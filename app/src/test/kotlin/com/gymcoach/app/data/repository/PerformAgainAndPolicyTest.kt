package com.gymcoach.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymcoach.app.core.exercise.EquipmentAvailability
import com.gymcoach.app.core.progression.ProgressionEngine
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.database.GymCoachDatabase
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutEntity
import com.gymcoach.app.data.local.entity.WorkoutExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PerformAgainAndPolicyTest {

    private lateinit var db: GymCoachDatabase
    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var repository: WorkoutRepositoryImpl
    private lateinit var progressionEngine: ProgressionEngine

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        workoutDao = db.workoutDao()
        exerciseDao = db.exerciseDao()
        repository = WorkoutRepositoryImpl(workoutDao, exerciseDao)
        progressionEngine = ProgressionEngine(EquipmentAvailability())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `performAgainPreservesSourceImmutabilityAndGeneratesNewActiveSession`() = runTest {
        val ex1 = exerciseDao.insert(ExerciseEntity(name = "Bench Press", description = "", muscleGroup = "Chest", equipment = "barbell", difficulty = "Intermediate"))
        val ex2 = exerciseDao.insert(ExerciseEntity(name = "Incline Fly", description = "", muscleGroup = "Chest", equipment = "dumbbell", difficulty = "Intermediate"))

        val now = System.currentTimeMillis()
        val workoutA = workoutDao.insertWorkout(
            WorkoutEntity(date = now - 86400000, startTime = now - 86400000, endTime = now - 82800000, duration = 3600, notes = "Original Push Day", completed = true, status = "COMPLETED")
        )

        val weA1 = workoutDao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = workoutA, exerciseId = ex1, orderIndex = 0))
        val weA2 = workoutDao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = workoutA, exerciseId = ex2, orderIndex = 1))

        val setA1 = WorkoutSetEntity(id = 1001L, workoutExerciseId = weA1, setNumber = 1, weight = 80.0, reps = 8, rpe = 8.0, restSeconds = 120, completed = true, setType = 0)
        val setA2 = WorkoutSetEntity(id = 1002L, workoutExerciseId = weA1, setNumber = 2, weight = 80.0, reps = 8, rpe = 8.5, restSeconds = 120, completed = true, setType = 0)
        val setA3 = WorkoutSetEntity(id = 1003L, workoutExerciseId = weA2, setNumber = 1, weight = 20.0, reps = 12, rpe = 8.0, restSeconds = 60, completed = true, setType = 2) // DROP set

        workoutDao.insertWorkoutSet(setA1)
        workoutDao.insertWorkoutSet(setA2)
        workoutDao.insertWorkoutSet(setA3)

        // 1. Trigger Perform Again -> Workout B
        val workoutBId = repository.createWorkoutFromHistory(workoutA)
        assertNotNull(workoutBId)
        assertNotEquals(workoutA, workoutBId)

        // Verify Workout A remains completely untouched in DB
        val fetchedA = workoutDao.getWorkoutById(workoutA).first()!!
        assertEquals("COMPLETED", fetchedA.status)
        assertTrue(fetchedA.completed)
        assertEquals("Original Push Day", fetchedA.notes)
        assertEquals(3600L, fetchedA.duration)

        // Verify Workout B created as ACTIVE
        val fetchedB = workoutDao.getWorkoutById(workoutBId!!).first()!!
        assertEquals("ACTIVE", fetchedB.status)
        assertFalse(fetchedB.completed)
        assertEquals(0L, fetchedB.duration)

        // Verify Workout B exercises and sets preserve order and set types, but reset completion flag and set new auto-generated IDs
        val exercisesB = workoutDao.getExercisesForWorkout(workoutBId).first()
        assertEquals(2, exercisesB.size)
        assertEquals(0, exercisesB[0].orderIndex)
        assertEquals(1, exercisesB[1].orderIndex)

        val setsB1 = workoutDao.getSetsForExercise(exercisesB[0].id).first()
        val setsB2 = workoutDao.getSetsForExercise(exercisesB[1].id).first()

        assertEquals(2, setsB1.size)
        assertEquals(1, setsB2.size)

        // Assert all copied sets are incomplete
        assertTrue("All copied sets in B must be incomplete", setsB1.all { !it.completed })
        assertTrue("All copied sets in B must be incomplete", setsB2.all { !it.completed })

        // Assert child set IDs are new (different from source IDs 1001, 1002, 1003)
        assertNotEquals(1001L, setsB1[0].id)
        assertNotEquals(1002L, setsB1[1].id)
        assertNotEquals(1003L, setsB2[0].id)

        // Assert set types preserved (e.g. DROP set type = 2)
        assertEquals(2, setsB2[0].setType)

        // 2. Modify B and complete B
        val updatedSetB1 = setsB1[0].copy(weight = 80.0, reps = 12, completed = true)
        workoutDao.updateWorkoutSet(updatedSetB1)
        workoutDao.updateWorkout(fetchedB.copy(completed = true, status = "COMPLETED", duration = 3200))

        // 3. Perform Again from A second time -> Workout C
        val workoutCId = repository.createWorkoutFromHistory(workoutA)
        assertNotNull(workoutCId)
        assertNotEquals(workoutA, workoutCId)
        assertNotEquals(workoutBId, workoutCId)

        // Assert A and B remain independent and completed, while C is ACTIVE
        assertEquals("COMPLETED", workoutDao.getWorkoutById(workoutA).first()!!.status)
        assertEquals("COMPLETED", workoutDao.getWorkoutById(workoutBId).first()!!.status)
        assertEquals("ACTIVE", workoutDao.getWorkoutById(workoutCId!!).first()!!.status)
    }

    @Test
    fun `oneActiveWorkoutPolicyAbandonsPreExistingActiveSessionWhenCreatingNewOne`() = runTest {
        val exId = exerciseDao.insert(ExerciseEntity(name = "Barbell Row", description = "", muscleGroup = "Back", equipment = "barbell", difficulty = "Intermediate"))

        val now = System.currentTimeMillis()
        val workoutA = workoutDao.insertWorkout(WorkoutEntity(date = now - 86400000, startTime = now - 86400000, endTime = now - 82800000, duration = 3600, notes = "Back Day A", completed = true, status = "COMPLETED"))
        val weA = workoutDao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = workoutA, exerciseId = exId, orderIndex = 0))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(workoutExerciseId = weA, setNumber = 1, weight = 70.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0))

        // Step 1: Perform Again A -> Workout B (ACTIVE)
        val workoutBId = repository.createWorkoutFromHistory(workoutA)!!
        val workoutBBefore = workoutDao.getWorkoutById(workoutBId).first()!!
        assertEquals("ACTIVE", workoutBBefore.status)

        // Step 2: Perform Again A while B is still ACTIVE -> Workout C created
        val workoutCId = repository.createWorkoutFromHistory(workoutA)!!
        assertNotEquals(workoutBId, workoutCId)

        // Step 3: Verify Policy C enforcement: Workout B is now ABANDONED in DB
        val workoutBAfter = workoutDao.getWorkoutById(workoutBId).first()!!
        assertEquals("ABANDONED", workoutBAfter.status)
        assertTrue(workoutBAfter.completed)

        // Step 4: Verify Workout C is the single ACTIVE session
        val workoutC = workoutDao.getWorkoutById(workoutCId).first()!!
        assertEquals("ACTIVE", workoutC.status)
        assertFalse(workoutC.completed)

        // Step 5: Resume flow `getIncompleteWorkout()` returns ONLY the ACTIVE session C (ignoring ABANDONED B)
        val incomplete = repository.getIncompleteWorkout()
        assertNotNull(incomplete)
        assertEquals(workoutCId, incomplete!!.id)
        assertEquals("ACTIVE", incomplete.status)
    }

    @Test
    fun `progressionLoopIntegrationTestCalculatesWeightIncreaseFromPersistedPerformance`() = runTest {
        val exId = exerciseDao.insert(ExerciseEntity(name = "Overhead Press", description = "", muscleGroup = "Shoulders", equipment = "barbell", difficulty = "Intermediate"))

        val now = System.currentTimeMillis()
        val workoutA = workoutDao.insertWorkout(WorkoutEntity(date = now - 86400000, startTime = now - 86400000, endTime = now - 82800000, duration = 3600, notes = "Shoulders A", completed = true, status = "COMPLETED"))
        val weA = workoutDao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = workoutA, exerciseId = exId, orderIndex = 0))
        val setA = WorkoutSetEntity(workoutExerciseId = weA, setNumber = 1, weight = 50.0, reps = 8, rpe = 8.0, restSeconds = 120, completed = true, setType = 0)
        workoutDao.insertWorkoutSet(setA)

        // Perform Again A -> B
        val workoutBId = repository.createWorkoutFromHistory(workoutA)!!
        val weB = workoutDao.getExercisesForWorkout(workoutBId).first()[0]
        val setB = workoutDao.getSetsForExercise(weB.id).first()[0]

        // Log B completed at top of range (50kg x 12 reps)
        val updatedSetB = setB.copy(weight = 50.0, reps = 12, completed = true)
        workoutDao.updateWorkoutSet(updatedSetB)
        workoutDao.updateWorkout(workoutDao.getWorkoutById(workoutBId).first()!!.copy(completed = true, status = "COMPLETED", duration = 3000))

        // Query progression logic with persisted performance
        val recommendation = progressionEngine.calculateProgression(
            exerciseId = exId,
            exerciseName = "Overhead Press",
            exerciseEquipment = "barbell",
            targetRepsMin = 8,
            targetRepsMax = 12,
            targetSets = 3,
            previousSets = listOf(setA),
            currentSets = listOf(updatedSetB),
            equipmentType = "gym"
        )

        // Top of range hit -> recommend 2.5kg increase for barbell overhead press
        assertEquals(55.0, recommendation.recommendedWeight, 0.01)
    }
}
