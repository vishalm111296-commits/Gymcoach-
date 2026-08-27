package com.gymcoach.app.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymcoach.app.data.local.database.GymCoachDatabase
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.entity.WorkoutEntity
import com.gymcoach.app.data.local.entity.WorkoutExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import com.gymcoach.app.domain.model.Workout

import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.SetType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Integration tests for WorkoutRepositoryImpl using real Room database.
 * Tests verify status filtering, PR detection, and volume calculations work correctly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class WorkoutRepositoryIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: GymCoachDatabase
    private lateinit var workoutDao: WorkoutDao
    private lateinit var repository: WorkoutRepositoryImpl

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GymCoachDatabase::class.java
        ).allowMainThreadQueries().build()
        workoutDao = db.workoutDao()
        repository = WorkoutRepositoryImpl(workoutDao, db.exerciseDao())
    }

    @Test
    fun `create and get workout preserves status`() = runTest {
        val workout = Workout(
            date = Instant.now(),
            startTime = Instant.now(),
            endTime = Instant.now(),
            duration = 0,
            notes = "Test workout",
            completed = false,
            status = "ACTIVE"
        )
        val id = repository.createWorkout(workout)

        val retrieved = repository.getWorkoutWithDetails(id).first()
        assertNotNull("Workout should be retrieved", retrieved)
        assertEquals("ACTIVE", retrieved!!.workout.status)
    }

    @Test
    fun `getLatestIncompleteWorkout returns only ACTIVE workouts`() = runTest {
        // Create COMPLETED workout
        val completed = WorkoutEntity(
            date = Instant.now().toEpochMilli(),
            startTime = Instant.now().toEpochMilli(),
            endTime = Instant.now().toEpochMilli(),
            duration = 3600000,
            notes = "Completed",
            completed = true,
            status = "COMPLETED"
        )
        workoutDao.insertWorkout(completed)

        // Create ACTIVE workout
        val active = WorkoutEntity(
            date = Instant.now().toEpochMilli(),
            startTime = Instant.now().toEpochMilli(),
            endTime = 0,
            duration = 0,
            notes = "Active",
            completed = false,
            status = "ACTIVE"
        )
        workoutDao.insertWorkout(active)

        // Create ABANDONED workout
        val abandoned = WorkoutEntity(
            date = Instant.now().toEpochMilli(),
            startTime = Instant.now().toEpochMilli(),
            endTime = 0,
            duration = 0,
            notes = "Abandoned",
            completed = false,
            status = "ABANDONED"
        )
        workoutDao.insertWorkout(abandoned)

        val result = repository.getLatestIncompleteWorkout()
        assertNotNull("Should return ACTIVE workout", result)
        assertEquals("ACTIVE", result!!.status)
        assertEquals("Active", result.notes)
    }

    @Test
    fun `getIncompleteWorkout returns only ACTIVE workouts`() = runTest {
        val active = WorkoutEntity(
            date = Instant.now().toEpochMilli(),
            startTime = Instant.now().toEpochMilli(),
            endTime = 0,
            duration = 0,
            notes = "Active workout",
            completed = false,
            status = "ACTIVE"
        )
        workoutDao.insertWorkout(active)

        val result = repository.getIncompleteWorkout()
        assertNotNull("Should return ACTIVE workout", result)
        assertEquals("ACTIVE", result!!.status)
    }

    @Test
    fun `getCompletedWorkouts filters by status COMPLETED`() = runTest {
        // Insert workouts with different statuses
        val statuses = listOf("COMPLETED", "ACTIVE", "ABANDONED", "NOT_STARTED", "COMPLETED")
        statuses.forEach { status ->
            val workout = WorkoutEntity(
                date = Instant.now().toEpochMilli(),
                startTime = Instant.now().toEpochMilli(),
                endTime = Instant.now().toEpochMilli(),
                duration = 3600000,
                notes = "Workout $status",
                completed = status == "COMPLETED",
                status = status
            )
            workoutDao.insertWorkout(workout)
        }

        val completed = repository.getCompletedWorkouts().value
        assertEquals("Should only return COMPLETED workouts", 2, completed.size)
        completed.forEach { assertEquals("COMPLETED", it.status) }
    }

    @Test
    fun `getPersonalRecordMax only considers COMPLETED workouts`() = runTest {
        val exerciseId = 1L

        // COMPLETED workout with heavy weight
        val completedWorkout = WorkoutEntity(
            date = Instant.now().toEpochMilli(),
            startTime = Instant.now().toEpochMilli(),
            endTime = Instant.now().toEpochMilli(),
            duration = 3600000,
            notes = "Heavy session",
            completed = true,
            status = "COMPLETED"
        )
        val completedId = workoutDao.insertWorkout(completedWorkout)
        val exEntity = WorkoutExerciseEntity(workoutId = completedId, exerciseId = exerciseId, orderIndex = 0)
        val exId = workoutDao.insertWorkoutExercise(exEntity)
        workoutDao.insertWorkoutSet(WorkoutSetEntity(workoutExerciseId = exId, setNumber = 1, weight = 100.0, reps = 5, rpe = 8.0, restSeconds = 180, completed = true, setType = 0))

        // ACTIVE workout with heavier weight (should be ignored)
        val activeWorkout = WorkoutEntity(
            date = Instant.now().toEpochMilli(),
            startTime = Instant.now().toEpochMilli(),
            endTime = 0,
            duration = 0,
            notes = "Active session",
            completed = false,
            status = "ACTIVE"
        )
        val activeId = workoutDao.insertWorkout(activeWorkout)
        val activeExEntity = WorkoutExerciseEntity(workoutId = activeId, exerciseId = exerciseId, orderIndex = 0)
        val activeExId = workoutDao.insertWorkoutExercise(activeExEntity)
        workoutDao.insertWorkoutSet(WorkoutSetEntity(workoutExerciseId = activeExId, setNumber = 1, weight = 150.0, reps = 5, rpe = 8.0, restSeconds = 180, completed = true, setType = 0))

        val pr = repository.getPersonalRecordMax(exerciseId)
        assertEquals("PR should be from COMPLETED only", 100.0, pr!!, 0.001)
    }

    @Test
    fun `monthly volume groups by strftime`() = runTest {
        val now = Instant.now().toEpochMilli()

        // Create workouts in different months
        val months = listOf(
            now - 60 * 24 * 60 * 60 * 1000L, // ~2 months ago
            now - 30 * 24 * 60 * 60 * 1000L, // ~1 month ago
            now
        )

        months.forEach { date ->
            val workout = WorkoutEntity(
                date = date,
                startTime = date,
                endTime = date + 3600000,
                duration = 3600000,
                notes = "Month workout",
                completed = true,
                status = "COMPLETED"
            )
            val wId = workoutDao.insertWorkout(workout)
            val exEntity = WorkoutExerciseEntity(workoutId = wId, exerciseId = 1L, orderIndex = 0)
            val exId = workoutDao.insertWorkoutExercise(exEntity)
            workoutDao.insertWorkoutSet(WorkoutSetEntity(workoutExerciseId = exId, setNumber = 1, weight = 100.0, reps = 10, rpe = 8.0, restSeconds = 180, completed = true, setType = 0))
        }

        val monthly = repository.getMonthlyVolumes().value
        assertTrue("Should have at least 1 month of data", monthly.size >= 1)
    }

    @Test
    fun `analytics queries filter by COMPLETED status`() = runTest {
        // Insert mixed status workouts
        val workout1 = WorkoutEntity(
            date = Instant.now().toEpochMilli(),
            startTime = Instant.now().toEpochMilli(),
            endTime = Instant.now().toEpochMilli(),
            duration = 3600000,
            notes = "Completed 1",
            completed = true,
            status = "COMPLETED"
        )
        val wId1 = workoutDao.insertWorkout(workout1)
        val ex1 = WorkoutExerciseEntity(workoutId = wId1, exerciseId = 1L, orderIndex = 0)
        val exId1 = workoutDao.insertWorkoutExercise(ex1)
        workoutDao.insertWorkoutSet(WorkoutSetEntity(workoutExerciseId = exId1, setNumber = 1, weight = 100.0, reps = 10, rpe = 8.0, restSeconds = 180, completed = true, setType = 0))

        val workout2 = WorkoutEntity(
            date = Instant.now().toEpochMilli(),
            startTime = Instant.now().toEpochMilli(),
            endTime = 0,
            duration = 0,
            notes = "Active",
            completed = false,
            status = "ACTIVE"
        )
        val wId2 = workoutDao.insertWorkout(workout2)

        val totalVolume = repository.getTotalVolumeSum()
        assertEquals("Only COMPLETED workout volume counted", 1000.0, totalVolume!!, 0.001)

        val avgVolume = repository.getAverageWorkoutVolume()
        assertEquals("Average from COMPLETED only", 1000.0, avgVolume, 0.001)

        val totalWorkouts = repository.getTotalWorkoutsCount()
        assertEquals("Only COMPLETED workouts counted", 1, totalWorkouts)
    }
}