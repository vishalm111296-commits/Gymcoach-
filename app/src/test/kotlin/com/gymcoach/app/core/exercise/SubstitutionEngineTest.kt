package com.gymcoach.app.core.exercise

import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.ExerciseMuscleDao
import com.gymcoach.app.data.local.dao.ExerciseSubstitutionDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SubstitutionEngineTest {

    private lateinit var exerciseDao: ExerciseDao
    private lateinit var exerciseMuscleDao: ExerciseMuscleDao
    private lateinit var exerciseSubstitutionDao: ExerciseSubstitutionDao
    private lateinit var equipmentAvailability: EquipmentAvailability
    private lateinit var engine: SubstitutionEngine

    private val benchPress = ExerciseEntity(
        id = 1L,
        name = "Barbell Bench Press",
        description = "Chest press",
        muscleGroup = "Chest",
        equipment = "barbell",
        category = "push",
        difficulty = "intermediate"
    )

    private val dbBenchPress = ExerciseEntity(
        id = 2L,
        name = "Dumbbell Bench Press",
        description = "DB Chest press",
        muscleGroup = "Chest",
        equipment = "dumbbell",
        category = "push",
        difficulty = "intermediate"
    )

    private val pushUp = ExerciseEntity(
        id = 3L,
        name = "Push Up",
        description = "Bodyweight push up",
        muscleGroup = "Chest",
        equipment = "bodyweight",
        category = "push",
        difficulty = "beginner"
    )

    @Before
    fun setup() {
        exerciseDao = mockk(relaxed = true)
        exerciseMuscleDao = mockk(relaxed = true)
        exerciseSubstitutionDao = mockk(relaxed = true)
        equipmentAvailability = mockk(relaxed = true)

        engine = SubstitutionEngine(
            exerciseDao = exerciseDao,
            exerciseMuscleDao = exerciseMuscleDao,
            exerciseSubstitutionDao = exerciseSubstitutionDao,
            equipmentAvailability = equipmentAvailability
        )
        every { exerciseDao.getAll() } returns flowOf(emptyList())
    }

    @Test
    fun `findSubstitutes returns predefined substitute when equipment is available`() = runTest {
        every { exerciseDao.getById(1L) } returns flowOf(benchPress)
        every { exerciseSubstitutionDao.getSubstituteExercises(1L) } returns flowOf(listOf(dbBenchPress))
        every { equipmentAvailability.isAvailable("dumbbell", "dumbbell_only") } returns true

        val results = engine.findSubstitutes(1L, "dumbbell_only")

        assertEquals(1, results.size)
        assertEquals("Dumbbell Bench Press", results[0].substitute.name)
        assertEquals("Recommended substitute", results[0].reason)
        assertTrue(results[0].preservationScore > 0)
    }

    @Test
    fun `findSubstitutes falls back to same muscle group exercises when predefined list is insufficient`() = runTest {
        every { exerciseDao.getById(1L) } returns flowOf(benchPress)
        every { exerciseSubstitutionDao.getSubstituteExercises(1L) } returns flowOf(emptyList())
        every { exerciseDao.getAll() } returns flowOf(listOf(benchPress, dbBenchPress, pushUp))
        every { equipmentAvailability.isAvailable(any(), any()) } returns true

        val results = engine.findSubstitutes(1L, "gym", maxResults = 2)

        assertEquals(2, results.size)
        assertTrue(results.all { it.reason == "Same muscle group" })
    }

    @Test
    fun `findSubstitutes returns empty list when exercise is not found`() = runTest {
        every { exerciseDao.getById(999L) } returns flowOf(null)

        val results = engine.findSubstitutes(999L, "gym")

        assertTrue(results.isEmpty())
    }
}
