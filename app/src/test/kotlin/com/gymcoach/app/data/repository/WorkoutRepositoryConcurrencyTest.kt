package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import com.gymcoach.app.domain.model.WorkoutSet
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class WorkoutRepositoryConcurrencyTest {

    @Test
    fun `concurrent addSetToExercise calls produce sequential set numbers`() = runTest {
        val dao = mockk<WorkoutDao>()
        val exerciseDao = mockk<ExerciseDao>()
        val repository = WorkoutRepositoryImpl(dao, exerciseDao)

        val setNumberCounter = AtomicInteger(1)
        coEvery { dao.addSetToExerciseAtomic(eq(100L), any()) } answers {
            val assigned = setNumberCounter.getAndIncrement()
            assigned.toLong()
        }

        val dummySet = WorkoutSet(
            id = 0,
            workoutExerciseId = 100L,
            setNumber = 0,
            weight = 50.0,
            reps = 10,
            rpe = 8.0,
            restSeconds = 90,
            completed = false
        )

        val deferreds = (1..5).map {
            async {
                repository.addSetToExercise(100L, dummySet)
            }
        }
        val results = deferreds.awaitAll()

        assertEquals(5, results.size)
        assertEquals(6, setNumberCounter.get())
    }

    @Test
    fun `concurrent addExerciseToWorkout calls produce sequential order indices`() = runTest {
        val dao = mockk<WorkoutDao>()
        val exerciseDao = mockk<ExerciseDao>()
        val repository = WorkoutRepositoryImpl(dao, exerciseDao)

        val orderCounter = AtomicInteger(0)
        coEvery { dao.addExerciseToWorkoutAtomic(eq(1L), eq(10L)) } answers {
            val order = orderCounter.getAndIncrement()
            order.toLong() + 50L
        }

        val deferreds = (1..5).map {
            async {
                repository.addExerciseToWorkout(1L, 10L, 0)
            }
        }
        val results = deferreds.awaitAll()

        assertEquals(5, results.size)
        assertEquals(5, orderCounter.get())
    }
}
