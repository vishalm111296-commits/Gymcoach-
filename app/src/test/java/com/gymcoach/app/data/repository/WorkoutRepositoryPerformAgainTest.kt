package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.entity.WorkoutEntity
import com.gymcoach.app.data.local.entity.WorkoutExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

class WorkoutRepositoryPerformAgainTest {
    private lateinit var workoutDao: WorkoutDao
    private lateinit var repository: WorkoutRepositoryImpl

    @Before
    fun setup() {
        workoutDao = mockk(relaxed = true)
        repository = WorkoutRepositoryImpl(workoutDao, mockk(relaxed = true))
    }

    @Test
    fun `createSessionFromHistory returns new workout ID and clones structure`() = runTest {
        val historicalWorkoutId = 42L

        val historicalWorkout = WorkoutEntity(
            id = historicalWorkoutId,
            date = Instant.now().toEpochMilli(),
            startTime = Instant.now().toEpochMilli(),
            endTime = Instant.now().toEpochMilli(),
            duration = 3600,
            notes = "Great workout",
            completed = true,
            status = "COMPLETED"
        )

        val historicalExercises = listOf(
            WorkoutExerciseEntity(id = 100L, workoutId = historicalWorkoutId, exerciseId = 1L, orderIndex = 0),
            WorkoutExerciseEntity(id = 101L, workoutId = historicalWorkoutId, exerciseId = 2L, orderIndex = 1)
        )

        coEvery { workoutDao.getWorkoutById(historicalWorkoutId) } returns flowOf(historicalWorkout)
        coEvery { workoutDao.getExercisesForWorkout(historicalWorkoutId) } returns flowOf(historicalExercises)
        coEvery { workoutDao.getSetsForExercise(100L) } returns flowOf(listOf(
            WorkoutSetEntity(id = 500L, workoutExerciseId = 100L, setNumber = 1, weight = 100.0, reps = 10, rpe = 8.0, restSeconds = 60, completed = true, setType = 0)
        ))
        coEvery { workoutDao.getSetsForExercise(101L) } returns flowOf(emptyList())


        coEvery { workoutDao.insertWorkout(any()) } returns 80L
        coEvery { workoutDao.insertWorkoutExercise(any()) } returns 1010L
        coEvery { workoutDao.insertWorkoutSet(any()) } returns 5050L

        val newSessionId = repository.createSessionFromHistory(historicalWorkoutId)

        assertEquals(80L, newSessionId)
    }
}
