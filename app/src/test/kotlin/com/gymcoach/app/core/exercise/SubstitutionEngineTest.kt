package com.gymcoach.app.core.exercise

import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.ExerciseMuscleDao
import com.gymcoach.app.data.local.dao.ExerciseSubstitutionDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.ExerciseSubstitutionEntity
import io.mockk.coEvery
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

    private val original = ExerciseEntity(
        id = 1,
        name = "Barbell Bench Press",
        description = "Chest press with barbell",
        muscleGroup = "Chest",
        equipment = "barbell",
        difficulty = "Intermediate",
        category = "push",
        tags = "compound"
    )

    private val sub1 = ExerciseEntity(
        id = 2,
        name = "Dumbbell Bench Press",
        description = "Chest press with dumbbells",
        muscleGroup = "Chest",
        equipment = "dumbbell",
        difficulty = "Intermediate",
        category = "push",
        tags = "compound"
    )

    private val sub2 = ExerciseEntity(
        id = 3,
        name = "Push-up",
        description = "Bodyweight chest press",
        muscleGroup = "Chest",
        equipment = "bodyweight",
        difficulty = "Beginner",
        category = "push",
        tags = "compound"
    )

    @Before
    fun setUp() {
        exerciseDao = mockk()
        exerciseMuscleDao = mockk()
        exerciseSubstitutionDao = mockk()
        equipmentAvailability = EquipmentAvailability()
        engine = SubstitutionEngine(
            exerciseDao,
            exerciseMuscleDao,
            exerciseSubstitutionDao,
            equipmentAvailability
        )
    }

    @Test
    fun findSubstitutesUsesBatchQueryForPredefinedSubstitutes() = runTest {
        val exerciseSubstitutions = listOf(
            ExerciseSubstitutionEntity(originalExerciseId = 1, substituteExerciseId = 2),
            ExerciseSubstitutionEntity(originalExerciseId = 1, substituteExerciseId = 3)
        )

        coEvery { exerciseDao.getById(1) } returns flowOf(original)
        coEvery { exerciseSubstitutionDao.getByExerciseId(1) } returns flowOf(exerciseSubstitutions)
        coEvery { exerciseDao.getByIds(listOf(2L, 3L)) } returns flowOf(listOf(sub1, sub2))
        coEvery { exerciseDao.getAll() } returns flowOf(listOf(original, sub1, sub2))

        val results = engine.findSubstitutes(exerciseId = 1, equipmentType = "gym", maxResults = 5)

        assertEquals(2, results.size)
        assertEquals(2L, results[0].substitute.id)
        assertEquals(3L, results[1].substitute.id)
        assertEquals("Recommended substitute", results[0].reason)
    }

    @Test
    fun findSubstitutesHandlesEmptyPredefinedSubstitutesGracefully() = runTest {
        coEvery { exerciseDao.getById(1) } returns flowOf(original)
        coEvery { exerciseSubstitutionDao.getByExerciseId(1) } returns flowOf(emptyList())
        coEvery { exerciseDao.getByIds(emptyList()) } returns flowOf(emptyList())
        coEvery { exerciseDao.getAll() } returns flowOf(listOf(original, sub1, sub2))

        val results = engine.findSubstitutes(exerciseId = 1, equipmentType = "gym", maxResults = 5)

        assertEquals(2, results.size)
        assertTrue(results.all { it.reason == "Same muscle group" })
    }
}
