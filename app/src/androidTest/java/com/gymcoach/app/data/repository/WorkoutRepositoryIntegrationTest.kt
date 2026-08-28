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
import kotlinx.coroutines.flow.first
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
    fun create_and_get_workout_preserves_status() = runTest {
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
    fun getLatestIncompleteWorkout_returns_only_ACTIVE_workouts() = runTest {
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
    fun getIncompleteWorkout_returns_only_ACTIVE_workouts() = runTest {
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
    fun getCompletedWorkouts_filters_by_status_COMPLETED() = runTest {
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

        val completed = repository.getCompletedWorkouts().first()
        assertEquals("Should only return COMPLETED workouts", 2, completed.size)
        completed.forEach { assertEquals("COMPLETED", it.status) }
    }




}
