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
 * Pins per-slot V-taper ranking in ProgramGenerator.buildDay.
 *
 * Regression context: candidate selection takes 1 top exercise per muscle slot (capping per-session volume).
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
        assertFalse("Upright Row (lower delt score) omitted when taking 1 per slot", "Upright Row" in names)
    }

    @Test
    fun `chest slot selects chest builders not back champion`() = runTest {
        val upperA = generate("gym").days.first { it.name == "Upper A" }
        val names = upperA.exercises.map { it.exerciseName }
        assertTrue("Incline DB Press expected in Upper A", "Incline DB Press" in names)
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
}
