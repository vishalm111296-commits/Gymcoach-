package com.gymcoach.app.data.repository

import com.gymcoach.app.core.exercise.EquipmentAvailability
import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.core.progression.ProgressionEngine
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.entity.WorkoutEntity
import com.gymcoach.app.data.local.entity.WorkoutExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class GymCoachClosedLoopIntegrationTest {

    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var workoutRepository: WorkoutRepositoryImpl
    private lateinit var progressionEngine: ProgressionEngine
    private lateinit var volumeCalculator: VolumeCalculator
    private lateinit var equipmentAvailability: EquipmentAvailability

    private val sourceWorkoutId = 100L
    private val clonedWorkoutId = 200L

    private val sourceWorkoutEntity = WorkoutEntity(
        id = sourceWorkoutId,
        date = 1600000000000L,
        startTime = 1600000000000L,
        endTime = 1600003600000L,
        duration = 3600L,
        notes = "V-Taper Push Day",
        completed = true,
        status = "COMPLETED"
    )

    private val sourceExerciseEntity = WorkoutExerciseEntity(
        id = 10L,
        workoutId = sourceWorkoutId,
        exerciseId = 100L,
        orderIndex = 0
    )

    private val sourceSet = WorkoutSetEntity(
        id = 1000L,
        workoutExerciseId = 10L,
        setNumber = 1,
        weight = 80.0,
        reps = 8,
        rpe = 8.0,
        restSeconds = 120,
        completed = true,
        setType = 0
    )

    @Before
    fun setup() {
        workoutDao = mockk(relaxed = true)
        exerciseDao = mockk(relaxed = true)
        equipmentAvailability = mockk(relaxed = true)
        workoutRepository = WorkoutRepositoryImpl(workoutDao, exerciseDao)
        progressionEngine = ProgressionEngine(equipmentAvailability)
        volumeCalculator = VolumeCalculator()
    }

    @Test
    fun `closed loop journey perform again produces new active workout and maintains historical immutability`() = runTest {
        // Step 1: Mock source historical workout details
        every { workoutDao.getWorkoutById(sourceWorkoutId) } returns flowOf(sourceWorkoutEntity)
        every { workoutDao.getExercisesForWorkout(sourceWorkoutId) } returns flowOf(listOf(sourceExerciseEntity))
        every { workoutDao.getSetsForExercise(10L) } returns flowOf(listOf(sourceSet))

        val capturedWorkout = slot<WorkoutEntity>()
        val capturedExercisesWithSets = slot<List<Pair<WorkoutExerciseEntity, List<WorkoutSetEntity>>>>()

        coEvery {
            workoutDao.createWorkoutFromHistoryTransaction(
                capture(capturedWorkout),
                capture(capturedExercisesWithSets)
            )
        } returns clonedWorkoutId

        // Step 2: Trigger Perform Again
        val newWorkoutId = workoutRepository.createWorkoutFromHistory(sourceWorkoutId)

        // Step 3: Assert new workout identity generated
        assertNotNull("newWorkoutId should not be null", newWorkoutId)
        if (newWorkoutId != null) {
            assertEquals(clonedWorkoutId, newWorkoutId)
            assertNotEquals(sourceWorkoutId, newWorkoutId)
        }

        // Step 4: Verify source historical records remain untouched
        coVerify(exactly = 0) { workoutDao.updateWorkout(any()) }
        coVerify(exactly = 0) { workoutDao.deleteWorkout(any()) }
        coVerify(exactly = 0) { workoutDao.updateWorkoutSet(any()) }

        // Step 5: Verify progression engine calculates recommendation from previous set
        val recommendation = progressionEngine.calculateProgression(
            exerciseId = 100L,
            exerciseName = "Barbell Bench Press",
            exerciseEquipment = "Barbell",
            targetRepsMin = 8,
            targetRepsMax = 10,
            targetSets = 3,
            previousSets = listOf(sourceSet),
            currentSets = listOf(sourceSet.copy(reps = 10, rpe = 8.0)) // Hit top of range
        )

        assertNotNull(recommendation)
        assertEquals(85.0, recommendation.recommendedWeight, 0.01) // 80kg + 5kg -> 85.0kg

        // Step 6: Verify volume calculator counts only completed sets with ACSM weighting
        val setContexts = listOf(
            VolumeCalculator.SetWithContext(
                set = WorkoutSetEntity(id = 2000L, workoutExerciseId = 20L, setNumber = 1, weight = 85.0, reps = 8, rpe = 8.5, restSeconds = 120, completed = true, setType = 0),
                exerciseId = 100L,
                workoutDate = System.currentTimeMillis()
            )
        )
        val muscleAssignments = mapOf(
            100L to listOf(
                VolumeCalculator.MuscleAssignment("Chest", VolumeCalculator.MuscleRole.PRIMARY),
                VolumeCalculator.MuscleAssignment("Triceps", VolumeCalculator.MuscleRole.SECONDARY)
            )
        )

        val balance = volumeCalculator.calculateWeeklyVolume(setContexts, muscleAssignments)
        assertEquals(1, balance.upperChestVolume.directSets)
        assertEquals(1, balance.tricepsVolume.indirectSets)
    }
}
