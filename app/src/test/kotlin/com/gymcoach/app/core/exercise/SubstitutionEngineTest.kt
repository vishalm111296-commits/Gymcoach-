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

    @Test
    fun `findSubstitutes sorts candidates by descending preservation score`() = runTest {
        val orig = ExerciseEntity(
            id = 10L, name = "Barbell Squat", description = "", muscleGroup = "Quadriceps",
            equipment = "barbell", category = "legs", difficulty = "intermediate", tags = "compound"
        )
        // Sub 1: Perfect match except equipment (dumbbell): 40 (muscle) + 20 (category) + 10 (difficulty) + 10 (compound) = 80
        val sub1 = ExerciseEntity(
            id = 11L, name = "Goblet Squat", description = "", muscleGroup = "Quadriceps",
            equipment = "dumbbell", category = "legs", difficulty = "intermediate", tags = "compound"
        )
        // Sub 2: Different category & difficulty: 40 (muscle) = 40
        val sub2 = ExerciseEntity(
            id = 12L, name = "Leg Extension", description = "", muscleGroup = "Quadriceps",
            equipment = "machine", category = "isolation", difficulty = "beginner", tags = "isolation"
        )

        every { exerciseDao.getById(10L) } returns flowOf(orig)
        every { exerciseSubstitutionDao.getSubstituteExercises(10L) } returns flowOf(listOf(sub2, sub1))
        every { equipmentAvailability.isAvailable(any(), any()) } returns true

        val results = engine.findSubstitutes(10L, "gym")

        assertEquals(2, results.size)
        // sub1 (score 80) must be sorted before sub2 (score 40)
        assertEquals("Goblet Squat", results[0].substitute.name)
        assertEquals(80, results[0].preservationScore)
        assertEquals("Leg Extension", results[1].substitute.name)
        assertEquals(40, results[1].preservationScore)
    }

    @Test
    fun `findSubstitutes skips predefined substitutes when equipment is unavailable`() = runTest {
        val orig = ExerciseEntity(
            id = 1L, name = "Barbell Bench Press", description = "", muscleGroup = "Chest",
            equipment = "barbell", category = "push", difficulty = "intermediate"
        )
        val cableFly = ExerciseEntity(
            id = 4L, name = "Cable Fly", description = "", muscleGroup = "Chest",
            equipment = "cable", category = "push", difficulty = "intermediate"
        )

        every { exerciseDao.getById(1L) } returns flowOf(orig)
        every { exerciseSubstitutionDao.getSubstituteExercises(1L) } returns flowOf(listOf(cableFly))
        // Cable is unavailable at home
        every { equipmentAvailability.isAvailable("cable", "home") } returns false
        every { exerciseDao.getAll() } returns flowOf(listOf(pushUp))
        every { equipmentAvailability.isAvailable("bodyweight", "home") } returns true

        val results = engine.findSubstitutes(1L, "home")

        assertEquals(1, results.size)
        assertEquals("Push Up", results[0].substitute.name)
    }

    @Test
    fun `calculatePreservationScore clamps at 100 when all score bonuses are met`() = runTest {
        val orig = ExerciseEntity(
            id = 1L, name = "Original", description = "", muscleGroup = "Chest",
            equipment = "barbell", category = "push", difficulty = "intermediate", tags = "compound, isolation"
        )
        val identical = ExerciseEntity(
            id = 2L, name = "Identical Attributes", description = "", muscleGroup = "Chest",
            equipment = "barbell", category = "push", difficulty = "intermediate", tags = "compound, isolation"
        )

        every { exerciseDao.getById(1L) } returns flowOf(orig)
        every { exerciseSubstitutionDao.getSubstituteExercises(1L) } returns flowOf(listOf(identical))
        every { equipmentAvailability.isAvailable(any(), any()) } returns true

        val results = engine.findSubstitutes(1L, "gym")

        assertEquals(1, results.size)
        // 40 + 20 + 15 + 10 + 10 + 10 = 105, capped at 100
        assertEquals(100, results[0].preservationScore)
    }

    @Test
    fun `findSubstitutes truncates candidate list strictly to maxResults`() = runTest {
        val orig = ExerciseEntity(
            id = 1L, name = "Barbell Bench Press", description = "", muscleGroup = "Chest",
            equipment = "barbell", category = "push", difficulty = "intermediate"
        )
        val candidates = (2L..10L).map { id ->
            ExerciseEntity(
                id = id, name = "Chest Exercise $id", description = "", muscleGroup = "Chest",
                equipment = "dumbbell", category = "push", difficulty = "intermediate"
            )
        }

        every { exerciseDao.getById(1L) } returns flowOf(orig)
        every { exerciseSubstitutionDao.getSubstituteExercises(1L) } returns flowOf(emptyList())
        every { exerciseDao.getAll() } returns flowOf(listOf(orig) + candidates)
        every { equipmentAvailability.isAvailable(any(), any()) } returns true

        val results = engine.findSubstitutes(1L, "gym", maxResults = 3)

        assertEquals(3, results.size)
        assertTrue(results.none { it.substitute.id == 1L })
    }

    @Test
    fun `findSubstitutes prevents duplicate exercises between predefined and same group fallback`() = runTest {
        val orig = ExerciseEntity(
            id = 1L, name = "Barbell Bench Press", description = "", muscleGroup = "Chest",
            equipment = "barbell", category = "push", difficulty = "intermediate"
        )
        val predefinedSub = ExerciseEntity(
            id = 2L, name = "Dumbbell Bench Press", description = "", muscleGroup = "Chest",
            equipment = "dumbbell", category = "push", difficulty = "intermediate"
        )
        val otherSub = ExerciseEntity(
            id = 3L, name = "Incline Dumbbell Press", description = "", muscleGroup = "Chest",
            equipment = "dumbbell", category = "push", difficulty = "intermediate"
        )

        every { exerciseDao.getById(1L) } returns flowOf(orig)
        every { exerciseSubstitutionDao.getSubstituteExercises(1L) } returns flowOf(listOf(predefinedSub))
        every { exerciseDao.getAll() } returns flowOf(listOf(orig, predefinedSub, otherSub))
        every { equipmentAvailability.isAvailable(any(), any()) } returns true

        val results = engine.findSubstitutes(1L, "gym", maxResults = 5)

        assertEquals(2, results.size)
        assertEquals(listOf(2L, 3L), results.map { it.substitute.id }.sorted())
        val dbBench = results.first { it.substitute.id == 2L }
        assertEquals("Recommended substitute", dbBench.reason)
        val incline = results.first { it.substitute.id == 3L }
        assertEquals("Same muscle group", incline.reason)
    }

    @Test
    fun `calculatePreservationScore yields zero when attributes are completely disjoint`() = runTest {
        val orig = ExerciseEntity(
            id = 1L, name = "Deadlift", description = "", muscleGroup = "Back",
            equipment = "barbell", category = "pull", difficulty = "advanced", tags = ""
        )
        val disjoint = ExerciseEntity(
            id = 2L, name = "Leg Extension", description = "", muscleGroup = "Quadriceps",
            equipment = "machine", category = "isolation", difficulty = "beginner", tags = ""
        )

        every { exerciseDao.getById(1L) } returns flowOf(orig)
        every { exerciseSubstitutionDao.getSubstituteExercises(1L) } returns flowOf(listOf(disjoint))
        every { equipmentAvailability.isAvailable(any(), any()) } returns true

        val results = engine.findSubstitutes(1L, "gym")

        assertEquals(1, results.size)
        assertEquals(0, results[0].preservationScore)
    }

    @Test
    fun `muscleGroup matching is case-insensitive for preservation score and fallback filtering`() = runTest {
        val orig = ExerciseEntity(
            id = 1L, name = "Upper Body Row", description = "", muscleGroup = "BACK",
            equipment = "cable", category = "pull", difficulty = "intermediate", tags = "compound"
        )
        val lowercaseSub = ExerciseEntity(
            id = 2L, name = "Dumbbell Row", description = "", muscleGroup = "back",
            equipment = "dumbbell", category = "pull", difficulty = "intermediate", tags = "compound"
        )

        every { exerciseDao.getById(1L) } returns flowOf(orig)
        every { exerciseSubstitutionDao.getSubstituteExercises(1L) } returns flowOf(emptyList())
        every { exerciseDao.getAll() } returns flowOf(listOf(orig, lowercaseSub))
        every { equipmentAvailability.isAvailable(any(), any()) } returns true

        val results = engine.findSubstitutes(1L, "gym")

        assertEquals(1, results.size)
        assertEquals("Dumbbell Row", results[0].substitute.name)
        assertEquals(80, results[0].preservationScore)
    }
}
