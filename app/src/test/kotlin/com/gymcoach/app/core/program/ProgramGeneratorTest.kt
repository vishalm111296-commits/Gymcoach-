package com.gymcoach.app.core.program

import com.gymcoach.app.core.exercise.EquipmentAvailability
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.ReadinessEntity
import com.gymcoach.app.domain.repository.ReadinessRepository
import io.mockk.coEvery
import io.mockk.every
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
 * Regression context: ranking previously summed ALL four vtaper scores, so a
 * candidate whose aggregate was inflated by irrelevant axes (e.g. lat score on
 * a lateral-deltoid candidate) outranked the true specialist for that slot.
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
        readinessRepository = mockk()
        generator = ProgramGenerator(dao, EquipmentAvailability(), readinessRepository)
    }

    private suspend fun generate(equipmentType: String, readinessEntity: ReadinessEntity? = null): ProgramGenerator.GeneratedProgram {
        coEvery { dao.getAll() } returns flowOf(all())
        coEvery { readinessRepository.getLatestReadiness() } returns flowOf(readinessEntity)
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
    fun `low readiness (< 2.5) reduces sets to 2 and RPE to 7.0`() = runTest {
        val readiness = ReadinessEntity(sleepQuality = 2, soreness = 2, energy = 2, motivation = 2)
        val program = generate("gym", readiness)
        val sets = program.days.flatMap { it.exercises }.map { it.targetSets }
        val rpe = program.days.flatMap { it.exercises }.map { it.targetRpe }
        assertTrue("All sets should be 2 (3-1 reduced)", sets.all { it == 2 })
        assertTrue("All RPE should be 7.0 (7.5-0.5 reduced)", rpe.all { it == 7.0 })
    }

    @Test
    fun `high readiness (>= 4.0) increases sets to 4 and RPE to 8.0`() = runTest {
        val readiness = ReadinessEntity(sleepQuality = 5, soreness = 4, energy = 5, motivation = 4)
        val program = generate("gym", readiness)
        val sets = program.days.flatMap { it.exercises }.map { it.targetSets }
        val rpe = program.days.flatMap { it.exercises }.map { it.targetRpe }
        assertTrue("All sets should be 4 (3+1 increased)", sets.all { it == 4 })
        assertTrue("All RPE should be 8.0 (7.5+0.5 increased)", rpe.all { it == 8.0 })
    }

    @Test
    fun `default readiness (3.0) keeps base sets (3) and RPE (7.5)`() = runTest {
        val program = generate("gym")
        val sets = program.days.flatMap { it.exercises }.map { it.targetSets }
        val rpe = program.days.flatMap { it.exercises }.map { it.targetRpe }
        assertTrue("All sets should be 3 (default)", sets.all { it == 3 })
        assertTrue("All RPE should be 7.5 (default)", rpe.all { it == 7.5 })
    }

    @Test
    fun `null readiness falls back to 3.0 score with base sets and RPE`() = runTest {
        val program = generate("gym", null)
        val sets = program.days.flatMap { it.exercises }.map { it.targetSets }
        val rpe = program.days.flatMap { it.exercises }.map { it.targetRpe }
        assertTrue("All sets should be 3 (fallback)", sets.all { it == 3 })
        assertTrue("All RPE should be 7.5 (fallback)", rpe.all { it == 7.5 })
        assertTrue("Description should mention fallback", program.description.contains("3.0"))
    }
}
