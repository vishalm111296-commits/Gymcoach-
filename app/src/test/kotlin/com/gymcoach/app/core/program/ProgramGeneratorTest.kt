package com.gymcoach.app.core.program

import com.gymcoach.app.core.exercise.EquipmentAvailability
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pins per-slot V-taper ranking and muscle matching in ProgramGenerator.buildDay.
 */
class ProgramGeneratorTest {

    private lateinit var dao: ExerciseDao
    private lateinit var generator: ProgramGenerator

    // P: Back specialist, aggregate-inflated (lat9 + delt6 + rear4 = 19)
    private val p = ExerciseEntity(
        id = 1, name = "Barbell Row", description = "", muscleGroup = "Back",
        equipment = "barbell", difficulty = "Intermediate",
        secondaryMuscles = "Lateral Deltoid",
        vtaperLat = 9, vtaperLateralDelt = 6, vtaperRearDelt = 4
    )
    // C: genuine chest builder
    private val c = ExerciseEntity(
        id = 3, name = "Incline DB Press", description = "", muscleGroup = "Chest",
        equipment = "dumbbell,bench", difficulty = "Beginner",
        vtaperUpperChest = 9
    )
    // D: bodyweight chest fallback
    private val d = ExerciseEntity(
        id = 4, name = "Push-up", description = "", muscleGroup = "Chest",
        equipment = "bodyweight", difficulty = "Beginner",
        vtaperUpperChest = 4
    )
    // H: lateral-delt candidate, aggregate 18 (lat6+delt7+rear5) but delt only 7
    private val h = ExerciseEntity(
        id = 5, name = "Upright Row", description = "", muscleGroup = "Lateral Deltoid",
        equipment = "barbell", difficulty = "Intermediate",
        vtaperLat = 6, vtaperLateralDelt = 7, vtaperRearDelt = 5
    )
    // Q: lateral-delt specialist, aggregate 11 but delt 10 — must outrank H per-slot
    private val q = ExerciseEntity(
        id = 2, name = "Lateral Raise", description = "", muscleGroup = "Lateral Deltoid",
        equipment = "dumbbell", difficulty = "Beginner",
        vtaperLateralDelt = 10, vtaperRearDelt = 1
    )
    // B: dumbbell back option for home-equipment coverage
    private val b = ExerciseEntity(
        id = 6, name = "Dumbbell Row", description = "", muscleGroup = "Back",
        equipment = "dumbbell", difficulty = "Intermediate",
        vtaperLat = 6
    )

    private fun all() = listOf(p, q, c, d, h, b)

    @Before
    fun setUp() {
        dao = mockk()
        generator = ProgramGenerator(dao, EquipmentAvailability())
    }

    private suspend fun generate(equipmentType: String): ProgramGenerator.GeneratedProgram {
        coEvery { dao.getAll() } returns flowOf(all())
        return generator.generateProgram(4, equipmentType, "vtaper")
    }

    @Test
    fun `lateral deltoid slot ranks specialists above aggregate-inflated candidates`() = runTest {
        val upperA = generate("gym").days.first { it.name == "Upper A" }
        val names = upperA.exercises.map { it.exerciseName }
        assertTrue("Lateral Raise expected in Upper A", "Lateral Raise" in names)
        assertTrue("Upright Row expected in Upper A", "Upright Row" in names)
        // Per-slot ordering: deltoid score decides, NOT aggregate sum
        assertTrue(
            "Lateral Raise (delt=10) must be picked before Upright Row (delt=7, agg=18)",
            names.indexOf("Lateral Raise") < names.indexOf("Upright Row")
        )
    }

    @Test
    fun `chest slot selects chest builders not back champion`() = runTest {
        val upperA = generate("gym").days.first { it.name == "Upper A" }
        val names = upperA.exercises.map { it.exerciseName }
        assertTrue("Incline DB Press expected in Upper A", "Incline DB Press" in names)
        assertTrue("Push-up expected in Upper A", "Push-up" in names)
        // Back slot consumed Barbell Row first; it must not reappear via chest matching
        assertEquals(1, names.count { it == "Barbell Row" })
    }

    @Test
    fun `home equipment excludes barbell keeps dumbbell and compound dumbbell+bench`() = runTest {
        val program = generate("home")
        val allNames = program.days.flatMap { it.exercises }.map { it.exerciseName }
        assertFalse("Barbell Row must be excluded at home", "Barbell Row" in allNames)
        assertFalse("Upright Row (barbell) must be excluded at home", "Upright Row" in allNames)
        assertTrue("Dumbbell Row expected at home", "Dumbbell Row" in allNames)
        assertTrue("dumbbell+bench compound satisfied at home", "Incline DB Press" in allNames)
        assertEquals(4, program.days.size)
    }

    @Test
    fun `custom bodyweight-only keeps only bodyweight exercises`() = runTest {
        val allNames = generate("custom").days.flatMap { it.exercises }.map { it.exerciseName }
        assertTrue("Push-up expected", "Push-up" in allNames)
        assertFalse("Incline DB Press requires gear", "Incline DB Press" in allNames)
        assertFalse("Lateral Raise requires dumbbell", "Lateral Raise" in allNames)
        assertFalse("Dumbbell Row requires dumbbell", "Dumbbell Row" in allNames)
    }

    @Test
    fun `TEST 3 - Muscle Slot Normalization verifies underscore, space, and case normalization`() {
        val latDeltEx = ExerciseEntity(
            id = 10, name = "Cable Lateral Raise", description = "", muscleGroup = "shoulders",
            equipment = "cable", difficulty = "Beginner", secondaryMuscles = "lateral_deltoid"
        )
        val rearDeltEx = ExerciseEntity(
            id = 11, name = "Face Pull", description = "", muscleGroup = "shoulders",
            equipment = "cable", difficulty = "Beginner", secondaryMuscles = "rear_deltoid"
        )
        val quadEx = ExerciseEntity(
            id = 12, name = "Leg Extension", description = "", muscleGroup = "legs",
            equipment = "machine", difficulty = "Beginner", secondaryMuscles = "quadriceps"
        )

        assertTrue(
            "Lateral Deltoid slot matches secondaryMuscles lateral_deltoid",
            ProgramGenerator.matchesMuscleSlot(latDeltEx, "Lateral Deltoid")
        )
        assertTrue(
            "Rear Deltoid slot matches secondaryMuscles rear_deltoid",
            ProgramGenerator.matchesMuscleSlot(rearDeltEx, "Rear Deltoid")
        )
        assertTrue(
            "Quadriceps slot matches secondaryMuscles quadriceps",
            ProgramGenerator.matchesMuscleSlot(quadEx, "Quadriceps")
        )
    }

    @Test
    fun `TEST 4 - Exact Matching prevents false positive substring and intra-category matches`() {
        val chestEx = ExerciseEntity(
            id = 20, name = "Bench Press", description = "", muscleGroup = "chest",
            equipment = "barbell", difficulty = "Beginner", secondaryMuscles = "front_deltoid, triceps"
        )
        val hamsLegEx = ExerciseEntity(
            id = 21, name = "Lying Leg Curl", description = "", muscleGroup = "legs",
            equipment = "machine", difficulty = "Beginner", secondaryMuscles = "hamstrings"
        )

        assertFalse(
            "Chest exercise must not accidentally match Back slot",
            ProgramGenerator.matchesMuscleSlot(chestEx, "Back")
        )
        assertFalse(
            "Chest exercise must not accidentally match Lateral Deltoid slot",
            ProgramGenerator.matchesMuscleSlot(chestEx, "Lateral Deltoid")
        )
        assertFalse(
            "Hamstring exercise in 'legs' category must not match Quadriceps slot",
            ProgramGenerator.matchesMuscleSlot(hamsLegEx, "Quadriceps")
        )
    }

    @Test
    fun `TEST 7 - Equipment Regression verifies matching fix does not bypass equipment constraints`() = runTest {
        val barbellLatDelt = ExerciseEntity(
            id = 30, name = "Barbell Upright Row", description = "", muscleGroup = "shoulders",
            equipment = "barbell", difficulty = "Intermediate", secondaryMuscles = "lateral_deltoid"
        )
        val bodyweightPushUp = ExerciseEntity(
            id = 31, name = "Push-up", description = "", muscleGroup = "Chest",
            equipment = "bodyweight", difficulty = "Beginner", vtaperUpperChest = 5
        )

        coEvery { dao.getAll() } returns flowOf(listOf(barbellLatDelt, bodyweightPushUp))

        val bodyweightProgram = generator.generateProgram(4, "custom", "vtaper")
        val selectedNames = bodyweightProgram.days.flatMap { it.exercises }.map { it.exerciseName }

        assertFalse(
            "Barbell equipment must be excluded in bodyweight/custom mode despite muscle match",
            "Barbell Upright Row" in selectedNames
        )
        assertTrue(
            "Bodyweight exercise must be included in bodyweight/custom mode",
            "Push-up" in selectedNames
        )
    }
}
