package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.domain.model.Exercise
import io.mockk.every
import io.mockk.mockk
import io.mockk.coVerify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ExerciseRepositoryTest {

    private lateinit var dao: ExerciseDao
    private lateinit var repo: ExerciseRepositoryImpl

    private val entity1 = ExerciseEntity(
        id = 1L,
        name = "Bench Press",
        description = "Chest exercise",
        muscleGroup = "Chest",
        equipment = "Barbell",
        difficulty = "Intermediate"
    )

    private val entity2 = ExerciseEntity(
        id = 2L,
        name = "Squat",
        description = "Leg exercise",
        muscleGroup = "Legs",
        equipment = "Barbell",
        difficulty = "Advanced"
    )

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        repo = ExerciseRepositoryImpl(dao)
    }

    @Test
    fun `getAllExercises maps entities to domain models`() = runTest {
        every { dao.getAll() } returns flowOf(listOf(entity1, entity2))

        val result = repo.getAllExercises().first()

        assertEquals(2, result.size)
        assertEquals(
            Exercise(1L, "Bench Press", "Chest exercise", "Chest", "Barbell", "Intermediate"),
            result[0]
        )
        assertEquals(
            Exercise(2L, "Squat", "Leg exercise", "Legs", "Barbell", "Advanced"),
            result[1]
        )
    }

    @Test
    fun `getAllExercises emits empty list when dao returns empty`() = runTest {
        every { dao.getAll() } returns flowOf(emptyList())

        val result = repo.getAllExercises().first()

        assertEquals(emptyList<Exercise>(), result)
    }

    @Test
    fun `getExerciseById maps entity to domain model`() = runTest {
        every { dao.getById(1L) } returns flowOf(entity1)

        val result = repo.getExerciseById(1L).first()

        assertEquals(
            Exercise(1L, "Bench Press", "Chest exercise", "Chest", "Barbell", "Intermediate"),
            result
        )
    }

    @Test
    fun `getExerciseById returns null when not found`() = runTest {
        every { dao.getById(99L) } returns flowOf(null)

        val result = repo.getExerciseById(99L).first()

        assertNull(result)
    }

    @Test
    fun `addExercise converts domain to entity and inserts`() = runTest {
        val exercise = Exercise(0L, "Deadlift", "Back exercise", "Back", "Barbell", "Advanced")

        repo.addExercise(exercise)

        coVerify {
            dao.insert(
                ExerciseEntity(0L, "Deadlift", "Back exercise", "Back", "Barbell", "Advanced")
            )
        }
    }

    @Test
    fun `deleteExercise converts domain to entity and deletes`() = runTest {
        val exercise = Exercise(1L, "Bench Press", "Chest exercise", "Chest", "Barbell", "Intermediate")

        repo.deleteExercise(exercise)

        coVerify {
            dao.delete(
                ExerciseEntity(1L, "Bench Press", "Chest exercise", "Chest", "Barbell", "Intermediate")
            )
        }
    }
}
