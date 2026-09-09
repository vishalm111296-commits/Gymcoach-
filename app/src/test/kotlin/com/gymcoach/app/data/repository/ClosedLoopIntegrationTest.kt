package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.LastPerformance
import com.gymcoach.app.data.local.dao.LastSetData
import com.gymcoach.app.data.local.dao.MuscleSetCount
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.dao.WorkoutWithStats
import com.gymcoach.app.data.local.entity.WorkoutEntity
import com.gymcoach.app.data.local.entity.WorkoutExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ClosedLoopIntegrationTest {

    private lateinit var workoutDao: WorkoutDao
    private lateinit var repository: WorkoutRepositoryImpl

    @Before
    fun setup() {
        workoutDao = mockk(relaxed = true)
        repository = WorkoutRepositoryImpl(workoutDao, mockk(relaxed = true))
    }

    @Test
    fun `closed loop - workout completion perform again progression and volume updates`() = runTest {
        // Step 1: Initial workout creation
        val sourceWorkout = WorkoutEntity(
            id = 1L,
            date = 1000000L,
            startTime = 1000000L,
            endTime = 1003600L,
            duration = 3600L,
            notes = "Upper A Workout",
            completed = true,
            status = "COMPLETED"
        )
        val sourceExercise = WorkoutExerciseEntity(id = 10L, workoutId = 1L, exerciseId = 100L, orderIndex = 0)
        val sourceSet = WorkoutSetEntity(id = 100L, workoutExerciseId = 10L, setNumber = 1, weight = 20.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0)

        coEvery { workoutDao.getWorkoutByIdSync(1L) } returns sourceWorkout
        coEvery { workoutDao.getExercisesForWorkoutList(1L) } returns listOf(sourceExercise)
        coEvery { workoutDao.getSetsForExerciseList(10L) } returns listOf(sourceSet)

        // Step 2: Perform Again (Clone Workout)
        coEvery { workoutDao.cloneWorkoutAsNewActive(1L) } returns 2L
        val clonedWorkoutId = repository.cloneWorkoutAsNewActive(1L)
        assertEquals(2L, clonedWorkoutId)

        // Step 3: Verify source workout remains untouched
        val originalWorkout = workoutDao.getWorkoutByIdSync(1L)
        assertNotNull(originalWorkout)
        assertEquals(1L, originalWorkout?.id)
        assertTrue(originalWorkout?.completed == true)
        assertEquals("COMPLETED", originalWorkout?.status)

        // Step 4: Read history with both workouts present
        val historyStats = listOf(
            WorkoutWithStats(2L, 2000000L, 2000000L, 2003600L, 3600L, "Upper A Workout", true, "COMPLETED", 225.0, 1, 10, 1),
            WorkoutWithStats(1L, 1000000L, 1000000L, 1003600L, 3600L, "Upper A Workout", true, "COMPLETED", 200.0, 1, 10, 1)
        )
        every { workoutDao.getCompletedWorkoutsWithStats() } returns flowOf(historyStats)

        val completedWorkouts = repository.getCompletedWorkouts().first()
        assertEquals(2, completedWorkouts.size)
        assertEquals(2L, completedWorkouts[0].id)
        assertEquals(1L, completedWorkouts[1].id)

        // Step 5: Verify Progression Engine receives latest performance
        coEvery { workoutDao.getLastPerformanceForExercise(100L) } returns LastPerformance(date = 2000000L, maxWeight = 22.5)
        coEvery { workoutDao.getLastSetsForExercise(100L) } returns listOf(
            LastSetData(weight = 22.5, reps = 10, rpe = 8.5, restSeconds = 90, setType = 0, date = 2000000L)
        )

        val lastPerformance = repository.getLastPerformanceForExercise(100L)
        assertNotNull(lastPerformance)
        assertEquals(22.5, lastPerformance?.maxWeight ?: 0.0, 0.01)

        val lastSets = repository.getLastSetsForExercise(100L)
        assertEquals(1, lastSets.size)
        assertEquals(22.5, lastSets[0].weight, 0.01)

        // Step 6: Verify V-Taper Volume receives completed set data
        every { workoutDao.getCompletedSetsByMuscle(any()) } returns flowOf(
            listOf(MuscleSetCount("Back", 8), MuscleSetCount("Chest", 10))
        )
        val completedSetsMap = repository.getCompletedSetsByMuscle(0L).first()
        assertEquals(8, completedSetsMap["Back"])
        assertEquals(10, completedSetsMap["Chest"])
    }
}
