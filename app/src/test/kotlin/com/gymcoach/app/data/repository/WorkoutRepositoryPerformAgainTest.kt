package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class WorkoutRepositoryPerformAgainTest {

    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var repository: WorkoutRepositoryImpl

    private val sourceWorkoutId = 100L
    private val newWorkoutId = 200L
    private val newWorkoutExerciseId = 300L

    private val sourceWorkoutEntity = WorkoutEntity(
        id = sourceWorkoutId,
        date = 1600000000000L,
        startTime = 1600000000000L,
        endTime = 1600003600000L,
        duration = 3600L,
        notes = "Leg Day Heavy",
        completed = true,
        status = "COMPLETED"
    )

    private val sourceWorkoutExerciseEntity = WorkoutExerciseEntity(
        id = 10L,
        workoutId = sourceWorkoutId,
        exerciseId = 1L,
        orderIndex = 0
    )

    private val exerciseEntity = ExerciseEntity(
        id = 1L,
        name = "Squat",
        description = "Leg exercise",
        muscleGroup = "Legs",
        equipment = "Barbell",
        difficulty = "Intermediate"
    )

    private val sourceSet1 = WorkoutSetEntity(
        id = 101L,
        workoutExerciseId = 10L,
        setNumber = 1,
        weight = 100.0,
        reps = 5,
        rpe = 8.0,
        restSeconds = 180,
        completed = true,
        setType = 0
    )

    private val sourceSet2 = WorkoutSetEntity(
        id = 102L,
        workoutExerciseId = 10L,
        setNumber = 2,
        weight = 105.0,
        reps = 5,
        rpe = 9.0,
        restSeconds = 180,
        completed = true,
        setType = 0
    )

    @Before
    fun setup() {
        workoutDao = mockk(relaxed = true)
        exerciseDao = mockk(relaxed = true)
        repository = WorkoutRepositoryImpl(workoutDao, exerciseDao)
    }

    @Test
    fun `createWorkoutFromHistory generates new ID and clones structure with fresh completion state`() = runTest {
        every { workoutDao.getWorkoutById(sourceWorkoutId) } returns flowOf(sourceWorkoutEntity)
        every { workoutDao.getExercisesForWorkout(sourceWorkoutId) } returns flowOf(listOf(sourceWorkoutExerciseEntity))
        every { exerciseDao.getById(1L) } returns flowOf(exerciseEntity)
        every { workoutDao.getSetsForExercise(10L) } returns flowOf(listOf(sourceSet1, sourceSet2))

        val capturedSourceWorkout = slot<WorkoutEntity>()
        val capturedSourceExercisesWithSets = slot<List<Pair<WorkoutExerciseEntity, List<WorkoutSetEntity>>>>()

        coEvery {
            workoutDao.createWorkoutFromHistoryTransaction(
                capture(capturedSourceWorkout),
                capture(capturedSourceExercisesWithSets)
            )
        } returns newWorkoutId

        val resultId = repository.createWorkoutFromHistory(sourceWorkoutId)

        assertEquals(newWorkoutId, resultId)
        assertNotEquals(sourceWorkoutId, resultId)

        // Verify transaction was invoked with correct source entity
        assertEquals(sourceWorkoutId, capturedSourceWorkout.captured.id)
        assertEquals("COMPLETED", capturedSourceWorkout.captured.status)
        assertEquals(1, capturedSourceExercisesWithSets.captured.size)

        val exercisePair = capturedSourceExercisesWithSets.captured[0]
        assertEquals(10L, exercisePair.first.id)
        assertEquals(2, exercisePair.second.size)
        assertEquals(101L, exercisePair.second[0].id)
        assertEquals(102L, exercisePair.second[1].id)

        // Verify source historical data remained untouched
        coVerify(exactly = 0) { workoutDao.updateWorkout(any()) }
        coVerify(exactly = 0) { workoutDao.deleteWorkout(any()) }
        coVerify(exactly = 0) { workoutDao.updateWorkoutExercise(any()) }
        coVerify(exactly = 0) { workoutDao.updateWorkoutSet(any()) }
    }

    @Test
    fun `createWorkoutFromHistory returns null when source workout is not found`() = runTest {
        every { workoutDao.getWorkoutById(999L) } returns flowOf(null)

        val resultId = repository.createWorkoutFromHistory(999L)

        assertNull(resultId)
        coVerify(exactly = 0) { workoutDao.createWorkoutFromHistoryTransaction(any(), any()) }
    }

    @Test
    fun `repeated createWorkoutFromHistory calls create independent sessions`() = runTest {
        every { workoutDao.getWorkoutById(sourceWorkoutId) } returns flowOf(sourceWorkoutEntity)
        every { workoutDao.getExercisesForWorkout(sourceWorkoutId) } returns flowOf(listOf(sourceWorkoutExerciseEntity))
        every { workoutDao.getSetsForExercise(10L) } returns flowOf(listOf(sourceSet1))

        coEvery { workoutDao.createWorkoutFromHistoryTransaction(any(), any()) } returnsMany listOf(201L, 202L)

        val firstNewId = repository.createWorkoutFromHistory(sourceWorkoutId)
        val secondNewId = repository.createWorkoutFromHistory(sourceWorkoutId)

        assertEquals(201L, firstNewId)
        assertEquals(202L, secondNewId)
        assertNotEquals(firstNewId, secondNewId)
        assertNotEquals(sourceWorkoutId, firstNewId)
        assertNotEquals(sourceWorkoutId, secondNewId)
    }
}
