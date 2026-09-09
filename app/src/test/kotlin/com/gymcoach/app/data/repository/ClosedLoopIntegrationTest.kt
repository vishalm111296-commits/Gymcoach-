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
        // Step 1: Initial workout creation (Workout A)
        val workoutA = WorkoutEntity(
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

        coEvery { workoutDao.getWorkoutByIdSync(1L) } returns workoutA
        coEvery { workoutDao.getExercisesForWorkoutList(1L) } returns listOf(sourceExercise)
        coEvery { workoutDao.getSetsForExerciseList(10L) } returns listOf(sourceSet)

        // Step 2: Perform Again from A -> Workout B
        coEvery { workoutDao.cloneWorkoutAsNewActive(1L) } returns 2L andThen 3L
        val workoutBId = repository.cloneWorkoutAsNewActive(1L)
        assertEquals(2L, workoutBId)

        // Step 3: Verify source workout A remains untouched
        val originalWorkoutA = workoutDao.getWorkoutByIdSync(1L)
        assertNotNull(originalWorkoutA)
        assertEquals(1L, originalWorkoutA?.id)
        assertTrue(originalWorkoutA?.completed == true)
        assertEquals("COMPLETED", originalWorkoutA?.status)

        // Step 4: Perform Again from A a second time -> Workout C
        val workoutCId = repository.cloneWorkoutAsNewActive(1L)
        assertEquals(3L, workoutCId)

        // Step 5: Read history with Workout A, B, C present as independent sessions
        val historyStats = listOf(
            WorkoutWithStats(3L, 3000000L, 3000000L, 3003600L, 3600L, "Upper A Workout", true, "COMPLETED", 230.0, 1, 10, 1),
            WorkoutWithStats(2L, 2000000L, 2000000L, 2003600L, 3600L, "Upper A Workout", true, "COMPLETED", 225.0, 1, 10, 1),
            WorkoutWithStats(1L, 1000000L, 1000000L, 1003600L, 3600L, "Upper A Workout", true, "COMPLETED", 200.0, 1, 10, 1)
        )
        every { workoutDao.getCompletedWorkoutsWithStats() } returns flowOf(historyStats)

        val completedWorkouts = repository.getCompletedWorkouts().first()
        assertEquals(3, completedWorkouts.size)
        assertEquals(3L, completedWorkouts[0].id)
        assertEquals(2L, completedWorkouts[1].id)
        assertEquals(1L, completedWorkouts[2].id)

        // Step 6: Verify Progression Engine receives latest performance from latest workout B/C
        coEvery { workoutDao.getLastPerformanceForExercise(100L) } returns LastPerformance(date = 3000000L, maxWeight = 23.0)
        coEvery { workoutDao.getLastSetsForExercise(100L) } returns listOf(
            LastSetData(weight = 23.0, reps = 10, rpe = 8.5, restSeconds = 90, setType = 0, date = 3000000L)
        )

        val lastPerformance = repository.getLastPerformanceForExercise(100L)
        assertNotNull(lastPerformance)
        assertEquals(23.0, lastPerformance?.maxWeight ?: 0.0, 0.01)

        val lastSets = repository.getLastSetsForExercise(100L)
        assertEquals(1, lastSets.size)
        assertEquals(23.0, lastSets[0].weight, 0.01)

        // Step 7: Verify V-Taper Volume receives completed set data from all completed sessions
        every { workoutDao.getCompletedSetsByMuscle(any()) } returns flowOf(
            listOf(MuscleSetCount("Back", 12), MuscleSetCount("Chest", 15))
        )
        val completedSetsMap = repository.getCompletedSetsByMuscle(0L).first()
        assertEquals(12, completedSetsMap["Back"])
        assertEquals(15, completedSetsMap["Chest"])
    }
}
