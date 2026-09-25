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

    @Test
    fun `calculatePreservationScore accurately calculates each isolated component score`() = runTest {
        val base = ExerciseEntity(
            id = 1L, name = "Base", description = "",
            muscleGroup = "Chest", category = "push", equipment = "barbell",
            difficulty = "intermediate", tags = "compound, isolation"
        )

        // Only muscle matches (+40)
        val matchMuscle = ExerciseEntity(
            id = 2L, name = "Muscle Only", description = "",
            muscleGroup = "Chest", category = "legs", equipment = "cable",
            difficulty = "beginner", tags = "none"
        )
        // Only category matches (+20)
        val matchCategory = ExerciseEntity(
            id = 3L, name = "Category Only", description = "",
            muscleGroup = "Back", category = "push", equipment = "machine",
            difficulty = "advanced", tags = "none"
        )
        // Only equipment matches (+15)
        val matchEquipment = ExerciseEntity(
            id = 4L, name = "Equipment Only", description = "",
            muscleGroup = "Legs", category = "pull", equipment = "barbell",
            difficulty = "expert", tags = "none"
        )
        // Only difficulty matches (+10)
        val matchDifficulty = ExerciseEntity(
            id = 5L, name = "Difficulty Only", description = "",
            muscleGroup = "Arms", category = "core", equipment = "dumbbell",
            difficulty = "intermediate", tags = "none"
        )
        // Only compound matches (+10)
        val matchCompound = ExerciseEntity(
            id = 6L, name = "Compound Only", description = "",
            muscleGroup = "Shoulders", category = "core", equipment = "kettlebell",
            difficulty = "beginner", tags = "compound"
        )
        // Only isolation matches (+10)
        val matchIsolation = ExerciseEntity(
            id = 7L, name = "Isolation Only", description = "",
            muscleGroup = "Abs", category = "legs", equipment = "bodyweight",
            difficulty = "beginner", tags = "isolation"
        )
        // Everything matches except isolation: 40 + 20 + 15 + 10 + 10 = 95
        val match95 = ExerciseEntity(
            id = 8L, name = "Almost Full Match", description = "",
            muscleGroup = "Chest", category = "push", equipment = "barbell",
            difficulty = "intermediate", tags = "compound"
        )

        every { exerciseDao.getById(1L) } returns flowOf(base)
        every { exerciseSubstitutionDao.getSubstituteExercises(1L) } returns flowOf(
            listOf(matchMuscle, matchCategory, matchEquipment, matchDifficulty, matchCompound, matchIsolation, match95)
        )
        every { equipmentAvailability.isAvailable(any(), any()) } returns true

        val results = engine.findSubstitutes(1L, "gym", maxResults = 10)

        val scoreMap = results.associate { it.substitute.name to it.preservationScore }
        assertEquals(40, scoreMap["Muscle Only"])
        assertEquals(20, scoreMap["Category Only"])
        assertEquals(15, scoreMap["Equipment Only"])
        assertEquals(10, scoreMap["Difficulty Only"])
        assertEquals(10, scoreMap["Compound Only"])
        assertEquals(10, scoreMap["Isolation Only"])
        assertEquals(95, scoreMap["Almost Full Match"])
    }

    @Test
    fun `findSubstitutes handles varied maxResults boundary limits including zero`() = runTest {
        val orig = ExerciseEntity(
            id = 1L, name = "Bench Press", description = "", muscleGroup = "Chest",
            equipment = "barbell", category = "push", difficulty = "intermediate"
        )
        val pool = (2L..10L).map { id ->
            ExerciseEntity(
                id = id, name = "Chest Sub $id", description = "", muscleGroup = "Chest",
                equipment = "dumbbell", category = "push", difficulty = "intermediate"
            )
        }

        every { exerciseDao.getById(1L) } returns flowOf(orig)
        every { exerciseSubstitutionDao.getSubstituteExercises(1L) } returns flowOf(emptyList())
        every { exerciseDao.getAll() } returns flowOf(listOf(orig) + pool)
        every { equipmentAvailability.isAvailable(any(), any()) } returns true

        // maxResults = 0
        val res0 = engine.findSubstitutes(1L, "gym", maxResults = 0)
        assertEquals(0, res0.size)

        // maxResults = 1
        val res1 = engine.findSubstitutes(1L, "gym", maxResults = 1)
        assertEquals(1, res1.size)

        // maxResults = 4
        val res4 = engine.findSubstitutes(1L, "gym", maxResults = 4)
        assertEquals(4, res4.size)

        // maxResults = 20 (more than pool size of 9)
        val res20 = engine.findSubstitutes(1L, "gym", maxResults = 20)
        assertEquals(9, res20.size)
    }

    @Test
    fun `findSubstitutes returns empty list when exerciseId is negative or missing`() = runTest {
        every { exerciseDao.getById(-1L) } returns flowOf(null)
        every { exerciseDao.getById(999999L) } returns flowOf(null)

        assertTrue(engine.findSubstitutes(-1L, "gym").isEmpty())
        assertTrue(engine.findSubstitutes(999999L, "gym").isEmpty())
    }

    @Test
    fun `findSubstitutes with real EquipmentAvailability filters home versus gym equipment accurately`() = runTest {
        val realEquipmentAvailability = EquipmentAvailability()
        val realEngine = SubstitutionEngine(
            exerciseDao = exerciseDao,
            exerciseMuscleDao = exerciseMuscleDao,
            exerciseSubstitutionDao = exerciseSubstitutionDao,
            equipmentAvailability = realEquipmentAvailability
        )

        val barbellSquat = ExerciseEntity(
            id = 100L, name = "Barbell Back Squat", description = "", muscleGroup = "Quadriceps",
            equipment = "barbell", category = "legs", difficulty = "intermediate", tags = "compound"
        )
        val dumbbellSquat = ExerciseEntity(
            id = 101L, name = "Goblet Squat", description = "", muscleGroup = "Quadriceps",
            equipment = "dumbbell", category = "legs", difficulty = "beginner", tags = "compound"
        )
        val bodyweightSquat = ExerciseEntity(
            id = 102L, name = "Air Squat", description = "", muscleGroup = "Quadriceps",
            equipment = "bodyweight", category = "legs", difficulty = "beginner", tags = "compound"
        )
        val legPress = ExerciseEntity(
            id = 103L, name = "Machine Leg Press", description = "", muscleGroup = "Quadriceps",
            equipment = "leg press", category = "legs", difficulty = "intermediate", tags = "compound"
        )

        every { exerciseDao.getById(100L) } returns flowOf(barbellSquat)
        every { exerciseSubstitutionDao.getSubstituteExercises(100L) } returns flowOf(
            listOf(dumbbellSquat, bodyweightSquat, legPress)
        )

        // In HOME tier: dumbbell & bodyweight are available; leg press is not
        val homeResults = realEngine.findSubstitutes(100L, "home", maxResults = 5)
        assertEquals(2, homeResults.size)
        assertTrue(homeResults.any { it.substitute.name == "Goblet Squat" })
        assertTrue(homeResults.any { it.substitute.name == "Air Squat" })
        assertTrue(homeResults.none { it.substitute.name == "Machine Leg Press" })

        // In GYM tier: dumbbell, bodyweight, and leg press are all available
        val gymResults = realEngine.findSubstitutes(100L, "gym", maxResults = 5)
        assertEquals(3, gymResults.size)
        assertTrue(gymResults.any { it.substitute.name == "Machine Leg Press" })

        // In CUSTOM tier: only bodyweight is available
        val customResults = realEngine.findSubstitutes(100L, "custom", maxResults = 5)
        assertEquals(1, customResults.size)
        assertEquals("Air Squat", customResults[0].substitute.name)
    }

    @Test
    fun `findSubstitutes does not include original exercise in fallback results`() = runTest {
        val orig = ExerciseEntity(
            id = 50L, name = "Pull Up", description = "", muscleGroup = "Back",
            equipment = "pull-up bar", category = "pull", difficulty = "intermediate", tags = "compound"
        )
        val chinUp = ExerciseEntity(
            id = 51L, name = "Chin Up", description = "", muscleGroup = "Back",
            equipment = "pull-up bar", category = "pull", difficulty = "intermediate", tags = "compound"
        )

        every { exerciseDao.getById(50L) } returns flowOf(orig)
        every { exerciseSubstitutionDao.getSubstituteExercises(50L) } returns flowOf(emptyList())
        // Pool explicitly contains orig and chinUp
        every { exerciseDao.getAll() } returns flowOf(listOf(orig, chinUp))
        every { equipmentAvailability.isAvailable(any(), any()) } returns true

        val results = engine.findSubstitutes(50L, "gym", maxResults = 5)

        assertEquals(1, results.size)
        assertEquals(51L, results[0].substitute.id)
        assertEquals("Chin Up", results[0].substitute.name)
    }
}
