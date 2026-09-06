package com.gymcoach.app.core.program

import com.gymcoach.app.core.exercise.EquipmentAvailability
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.ReadinessEntity
import com.gymcoach.app.domain.repository.ReadinessRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProgramGeneratorTest {

    private lateinit var dao: ExerciseDao
    private lateinit var readinessRepository: ReadinessRepository
    private lateinit var generator: ProgramGenerator

    private val p = ExerciseEntity(
        id = 1, name = "Barbell Row", description = "", muscleGroup = "Back",
        equipment = "barbell", difficulty = "Intermediate",
        secondaryMuscles = "Lateral Deltoid",
        vtaperLat = 9, vtaperLateralDelt = 6, vtaperRearDelt = 4
    )
    private val c = ExerciseEntity(
        id = 3, name = "Incline DB Press", description = "", muscleGroup = "Chest",
        equipment = "dumbbell,bench", difficulty = "Beginner",
        vtaperUpperChest = 9
    )
    private val d = ExerciseEntity(
        id = 4, name = "Push-up", description = "", muscleGroup = "Chest",
        equipment = "bodyweight", difficulty = "Beginner",
        vtaperUpperChest = 4
    )
    private val h = ExerciseEntity(
        id = 5, name = "Upright Row", description = "", muscleGroup = "Lateral Deltoid",
        equipment = "barbell", difficulty = "Intermediate",
        vtaperLat = 6, vtaperLateralDelt = 7, vtaperRearDelt = 5
    )
    private val q = ExerciseEntity(
        id = 2, name = "Lateral Raise", description = "", muscleGroup = "Lateral Deltoid",
        equipment = "dumbbell", difficulty = "Beginner",
        vtaperLateralDelt = 10, vtaperRearDelt = 1
    )
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
    fun lateral_deltoid_slot_ranks_specialists_above_aggregate_inflated_candidates() = runTest {
        val upperA = generate("gym").days.first { it.name == "Upper A" }
        val names = upperA.exercises.map { it.exerciseName }
        assertTrue("Lateral Raise expected in Upper A", "Lateral Raise" in names)
        assertTrue("Upright Row expected in Upper A", "Upright Row" in names)
        assertTrue(
            "Lateral Raise (delt=10) must be picked before Upright Row (delt=7, agg=18)",
            names.indexOf("Lateral Raise") < names.indexOf("Upright Row")
        )
    }

    @Test
    fun chest_slot_selects_chest_builders_not_back_champion() = runTest {
        val upperA = generate("gym").days.first { it.name == "Upper A" }
        val names = upperA.exercises.map { it.exerciseName }
        assertTrue("Incline DB Press expected in Upper A", "Incline DB Press" in names)
        assertTrue("Push-up expected in Upper A", "Push-up" in names)
        assertEquals(1, names.count { it == "Barbell Row" })
    }

    @Test
    fun home_equipment_excludes_barbell_keeps_dumbbell_and_compound_dumbbell_bench() = runTest {
        val program = generate("home")
        val allNames = program.days.flatMap { it.exercises }.map { it.exerciseName }
        assertFalse("Barbell Row must be excluded at home", "Barbell Row" in allNames)
        assertFalse("Upright Row (barbell) must be excluded at home", "Upright Row" in allNames)
        assertTrue("Dumbbell Row expected at home", "Dumbbell Row" in allNames)
        assertTrue("dumbbell+bench compound satisfied at home", "Incline DB Press" in allNames)
        assertEquals(4, program.days.size)
    }

    @Test
    fun custom_bodyweight_only_keeps_only_bodyweight_exercises() = runTest {
        val allNames = generate("custom").days.flatMap { it.exercises }.map { it.exerciseName }
        assertTrue("Push-up expected", "Push-up" in allNames)
        assertFalse("Incline DB Press requires gear", "Incline DB Press" in allNames)
        assertFalse("Lateral Raise requires dumbbell", "Lateral Raise" in allNames)
        assertFalse("Dumbbell Row requires dumbbell", "Dumbbell Row" in allNames)
    }

    @Test
    fun low_readiness_reduces_sets_to_2_and_rpe_to_7_0() = runTest {
        val readiness = ReadinessEntity(sleepQuality = 2, soreness = 2, energy = 2, motivation = 2)
        val program = generate("gym", readiness)
        val sets = program.days.flatMap { it.exercises }.map { it.targetSets }
        val rpe = program.days.flatMap { it.exercises }.map { it.targetRpe }
        assertTrue("All sets should be 2 (3-1 reduced)", sets.all { it == 2 })
        assertTrue("All RPE should be 7.0 (7.5-0.5 reduced)", rpe.all { it == 7.0 })
    }

    @Test
    fun high_readiness_increases_sets_to_4_and_rpe_to_8_0() = runTest {
        val readiness = ReadinessEntity(sleepQuality = 5, soreness = 4, energy = 5, motivation = 4)
        val program = generate("gym", readiness)
        val sets = program.days.flatMap { it.exercises }.map { it.targetSets }
        val rpe = program.days.flatMap { it.exercises }.map { it.targetRpe }
        assertTrue("All sets should be 4 (3+1 increased)", sets.all { it == 4 })
        assertTrue("All RPE should be 8.0 (7.5+0.5 increased)", rpe.all { it == 8.0 })
    }

    @Test
    fun default_readiness_keeps_base_sets_3_and_rpe_7_5() = runTest {
        val program = generate("gym")
        val sets = program.days.flatMap { it.exercises }.map { it.targetSets }
        val rpe = program.days.flatMap { it.exercises }.map { it.targetRpe }
        assertTrue("All sets should be 3 (default)", sets.all { it == 3 })
        assertTrue("All RPE should be 7.5 (default)", rpe.all { it == 7.5 })
    }

    @Test
    fun null_readiness_falls_back_to_3_0_score_with_base_sets_and_rpe() = runTest {
        val program = generate("gym", null)
        val sets = program.days.flatMap { it.exercises }.map { it.targetSets }
        val rpe = program.days.flatMap { it.exercises }.map { it.targetRpe }
        assertTrue("All sets should be 3 (fallback)", sets.all { it == 3 })
        assertTrue("All RPE should be 7.5 (fallback)", rpe.all { it == 7.5 })
        assertTrue("Description should mention fallback", program.description.contains("3.0"))
    }
}
