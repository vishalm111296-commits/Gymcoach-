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
        difficulty = "Intermediate",
        isFavorite = true
    )

    private val entity2 = ExerciseEntity(
        id = 2L,
        name = "Squat",
        description = "Leg exercise",
        muscleGroup = "Legs",
        equipment = "Barbell",
        difficulty = "Advanced",
        isFavorite = false
    )

    @Before
    fun setup() {
        dao = mockk()
        repo = ExerciseRepositoryImpl(dao)
    }

    @Test
    fun `getAllExercises returns domain models`() = runTest {
        every { dao.getAll() } returns flowOf(listOf(entity1, entity2))
        val result = repo.getAllExercises().first()
        assertEquals(2, result.size)
        assertEquals("Bench Press", result[0].name)
    }

    @Test
    fun `getFilteredExercises delegates to dao with correct parameters`() = runTest {
        every { dao.getFilteredExercises("Chest", "Intermediate", "Barbell", true) } returns flowOf(listOf(entity1))
        val result = repo.getFilteredExercises("Chest", "Intermediate", "Barbell", true).first()
        assertEquals(1, result.size)
        assertEquals("Bench Press", result[0].name)
    }

    @Test
    fun `getExerciseById returns null if not found`() = runTest {
        every { dao.getById(99L) } returns flowOf(null)
        val result = repo.getExerciseById(99L).first()
        assertNull(result)
    }
}
