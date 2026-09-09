package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.MuscleSetCount
import com.gymcoach.app.data.local.dao.WorkoutDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PerformAgainTest {

    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var repository: WorkoutRepositoryImpl

    @Before
    fun setup() {
        workoutDao = mockk(relaxed = true)
        exerciseDao = mockk(relaxed = true)
        repository = WorkoutRepositoryImpl(workoutDao, exerciseDao)
    }

    @Test
    fun `cloneWorkoutAsNewActive delegates to workoutDao cloneWorkoutAsNewActive`() = runTest {
        val sourceWorkoutId = 10L
        val expectedNewWorkoutId = 42L
        coEvery { workoutDao.cloneWorkoutAsNewActive(sourceWorkoutId) } returns expectedNewWorkoutId

        val result = repository.cloneWorkoutAsNewActive(sourceWorkoutId)

        assertEquals(expectedNewWorkoutId, result)
        coVerify(exactly = 1) { workoutDao.cloneWorkoutAsNewActive(sourceWorkoutId) }
    }

    @Test
    fun `getCompletedSetsByMuscle maps MuscleSetCount list to muscle map`() = runTest {
        val startDate = 100000L
        val daoResult = listOf(
            MuscleSetCount("Back", 6),
            MuscleSetCount("Chest", 4)
        )
        every { workoutDao.getCompletedSetsByMuscle(startDate) } returns flowOf(daoResult)

        val resultMap = repository.getCompletedSetsByMuscle(startDate).first()

        assertEquals(2, resultMap.size)
        assertEquals(6, resultMap["Back"])
        assertEquals(4, resultMap["Chest"])
    }
}
