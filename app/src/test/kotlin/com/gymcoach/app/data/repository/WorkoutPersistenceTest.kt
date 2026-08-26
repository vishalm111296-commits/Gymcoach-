package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.entity.WorkoutEntity
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.SetType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * WorkoutPersistenceTest - real tests using WorkoutRepositoryImpl with mocked DAOs.
 *
 * Tests the complete workout lifecycle: create → add exercise → add set → complete → resume.
 * Verifies that status transitions are correct and data flows through mappers properly.
 */
class WorkoutPersistenceTest {

    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var repository: WorkoutRepositoryImpl

    @Before
    fun setup() {
        workoutDao = mockk<WorkoutDao>(relaxed = true)
        exerciseDao = mockk<ExerciseDao>(relaxed = true)
        repository = WorkoutRepositoryImpl(workoutDao, exerciseDao)
    }

    @Test
    fun `createWorkout calls insertWorkout on DAO`() = runTest {
        val workout = Workout(
            id = 0L,
            date = Instant.now(),
            startTime = Instant.now(),
            endTime = Instant.EPOCH,
            duration = 0L,
            notes = "Test workout",
            completed = false,
            status = "NOT_STARTED"
        )

        coEvery { workoutDao.insertWorkout(any()) } returns 1L

        val id = repository.createWorkout(workout)

        coVerify { workoutDao.insertWorkout(any()) }
        assertEquals("Should return inserted ID", 1L, id)
    }

    @Test
    fun `getLatestIncompleteWorkout returns ACTIVE workout`() = runTest {
        val now = Instant.now()
        val entity = WorkoutEntity(
            id = 1L,
            date = now.toEpochMilli(),
            startTime = now.toEpochMilli(),
            endTime = 0L,
            duration = 0L,
            notes = "Active workout",
            completed = false,
            status = "ACTIVE"
        )

        coEvery { workoutDao.getLatestIncompleteWorkout() } returns entity

        val workout = repository.getLatestIncompleteWorkout()

        assertNotNull("Should return a workout", workout)
        assertEquals("Workout should have status ACTIVE", "ACTIVE", workout!!.status)
        assertEquals("Workout ID should match", 1L, workout.id)
    }

    @Test
    fun `getLatestIncompleteWorkout returns null when no ACTIVE`() = runTest {
        coEvery { workoutDao.getLatestIncompleteWorkout() } returns null

        val workout = repository.getLatestIncompleteWorkout()

        assertNull("Should return null when no active workout", workout)
    }

    @Test
    fun `updateWorkout preserves status through entity mapping`() = runTest {
        val workout = Workout(
            id = 1L,
            date = Instant.now(),
            startTime = Instant.now(),
            endTime = Instant.now(),
            duration = 3600000L,
            notes = "Completed workout",
            completed = true,
            status = "COMPLETED",
        )

        coEvery { workoutDao.updateWorkout(any()) } returns Unit

        repository.updateWorkout(workout)

        coVerify { workoutDao.updateWorkout(match { entity ->
            entity.status == "COMPLETED" && entity.completed == true
        }) }
    }

    @Test
    fun `updateWorkout with ACTIVE status preserves it`() = runTest {
        val workout = Workout(
            id = 1L,
            date = Instant.now(),
            startTime = Instant.now(),
            endTime = Instant.EPOCH,
            duration = 0L,
            notes = "",
            completed = false,
            status = "ACTIVE"
        )

        coEvery { workoutDao.updateWorkout(any()) } returns Unit

        repository.updateWorkout(workout)

        coVerify { workoutDao.updateWorkout(match { entity ->
            entity.status == "ACTIVE" && entity.completed == false
        }) }
    }

    @Test
    fun `updateWorkout with ABANDONED status preserves it`() = runTest {
        val workout = Workout(
            id = 1L,
            date = Instant.now(),
            startTime = Instant.now(),
            endTime = Instant.EPOCH,
            duration = 0L,
            notes = "",
            completed = false,
            status = "ABANDONED"
        )

        coEvery { workoutDao.updateWorkout(any()) } returns Unit

        repository.updateWorkout(workout)

        coVerify { workoutDao.updateWorkout(match { entity ->
            entity.status == "ABANDONED"
        }) }
    }

    @Test
    fun `getCompletedWorkouts maps status through`() = runTest {
        val now = Instant.now()
        val stats = com.gymcoach.app.data.local.dao.WorkoutWithStats(
            id = 1L,
            date = now.toEpochMilli(),
            startTime = now.toEpochMilli(),
            endTime = now.toEpochMilli(),
            duration = 3600000L,
            notes = "Test",
            completed = true,
            status = "COMPLETED",
            volume = 5000.0,
            setCount = 10,
            repCount = 100,
            exerciseCount = 5
        )

        coEvery { workoutDao.getCompletedWorkoutsWithStats() } returns flowOf(listOf(stats))

        val workouts = repository.getCompletedWorkouts()

        workouts.collect { list ->
            assertEquals("Should have 1 workout", 1, list.size)
            assertEquals("Volume should be 5000.0", 5000.0, list[0].volume, 0.001)
            assertEquals("Set count should be 10", 10, list[0].setCount)
        }
    }

    @Test
    fun `addSetToExercise calls DAO correctly`() = runTest {
        val set = WorkoutSet(
            id = 0L,
            workoutExerciseId = 1L,
            setNumber = 1,
            weight = 60.0,
            reps = 10,
            rpe = 7.0,
            restSeconds = 90,
            completed = false,
            setType = SetType.NORMAL
        )

        coEvery { workoutDao.insertWorkoutSet(any()) } returns 1L

        val id = repository.addSetToExercise(1L, set)

        coVerify { workoutDao.insertWorkoutSet(match { entity ->
            entity.weight == 60.0 && entity.reps == 10 && entity.setNumber == 1
        }) }
        assertEquals("Should return inserted ID", 1L, id)
    }

    @Test
    fun `updateSet calls DAO with correct entity`() = runTest {
        val set = WorkoutSet(
            id = 5L,
            workoutExerciseId = 1L,
            setNumber = 3,
            weight = 80.0,
            reps = 8,
            rpe = 8.5,
            restSeconds = 120,
            completed = true,
            setType = SetType.NORMAL
        )

        coEvery { workoutDao.updateWorkoutSet(any()) } returns Unit

        repository.updateSet(set)

        coVerify { workoutDao.updateWorkoutSet(match { entity ->
            entity.id == 5L && entity.weight == 80.0 && entity.reps == 8 && entity.completed == true
        }) }
    }

    @Test
    fun `searchWorkouts delegates to DAO`() = runTest {
        val now = Instant.now()
        val stats = com.gymcoach.app.data.local.dao.WorkoutWithStats(
            id = 1L,
            date = now.toEpochMilli(),
            startTime = now.toEpochMilli(),
            endTime = now.toEpochMilli(),
            duration = 3600000L,
            notes = "Chest day",
            completed = true,
            status = "COMPLETED",
            volume = 3000.0,
            setCount = 8,
            repCount = 80,
            exerciseCount = 4
        )

        coEvery { workoutDao.searchWorkouts("chest") } returns listOf(stats)

        val results = repository.searchWorkouts("chest")

        assertEquals("Should return 1 result", 1, results.size)
        assertEquals("Notes should match", "Chest day", results[0].notes)
    }

    @Test
    fun `deleteWorkout calls DAO`() = runTest {
        val entity = WorkoutEntity(
            id = 1L,
            date = Instant.now().toEpochMilli(),
            startTime = Instant.now().toEpochMilli(),
            endTime = 0L,
            duration = 0L,
            notes = "",
            completed = false,
            status = "ACTIVE"
        )

        coEvery { workoutDao.getWorkoutById(1L) } returns flowOf(entity)
        coEvery { workoutDao.deleteWorkout(any()) } returns Unit

        repository.deleteWorkout(1L)

        coVerify { workoutDao.deleteWorkout(match { it.id == 1L }) }
    }
}
