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

/** Regression coverage for V-taper ranking, volume allocation and equipment filtering. */
class ProgramGeneratorTest {

    private lateinit var dao: ExerciseDao
    private lateinit var readinessRepository: ReadinessRepository
    private lateinit var generator: ProgramGenerator

    private val p = ExerciseEntity(
        id = 1, name = "Barbell Row", description = "", muscleGroup = "Back",
        equipment = "barbell", difficulty = "Intermediate", movementPattern = "horizontal_pull",
        secondaryMuscles = "Lateral Deltoid", recommendedRepRange = "8-12", recommendedRestTime = "120s",
        vtaperLat = 9, vtaperLateralDelt = 6, vtaperRearDelt = 4
    )
    private val c = ExerciseEntity(
        id = 3, name = "Incline DB Press", description = "", muscleGroup = "Chest",
        equipment = "dumbbell,bench", difficulty = "Beginner", movementPattern = "incline_push",
        recommendedRepRange = "6-10", recommendedRestTime = "2 min", vtaperUpperChest = 9
    )
    private val d = ExerciseEntity(
        id = 4, name = "Push-up", description = "", muscleGroup = "Chest",
        equipment = "bodyweight", difficulty = "Beginner", movementPattern = "horizontal_push",
        recommendedRepRange = "10-20", recommendedRestTime = "60s", vtaperUpperChest = 4
    )
    private val h = ExerciseEntity(
        id = 5, name = "Upright Row", description = "", muscleGroup = "Lateral Deltoid",
        equipment = "barbell", difficulty = "Intermediate", movementPattern = "vertical_pull",
        vtaperLat = 6, vtaperLateralDelt = 7, vtaperRearDelt = 5
    )
    private val q = ExerciseEntity(
        id = 2, name = "Lateral Raise", description = "", muscleGroup = "Lateral Deltoid",
        equipment = "dumbbell", difficulty = "Beginner", movementPattern = "lateral_raise",
        vtaperLateralDelt = 10, vtaperRearDelt = 1
    )
    private val b = ExerciseEntity(
        id = 6, name = "Dumbbell Row", description = "", muscleGroup = "Back",
        equipment = "dumbbell", difficulty = "Intermediate", movementPattern = "horizontal_pull",
        recommendedRepRange = "8-12", recommendedRestTime = "90s", vtaperLat = 6
    )

    private fun all() = listOf(p, q, c, d, h, b)

    @Before
    fun setUp() {
        dao = mockk()
        readinessRepository = mockk()
        generator = ProgramGenerator(dao, EquipmentAvailability(), readinessRepository)
    }

    private suspend fun generate(
        equipmentType: String,
        readinessEntity: ReadinessEntity? = null
    ): ProgramGenerator.GeneratedProgram {
        coEvery { dao.getAll() } returns flowOf(all())
        coEvery { readinessRepository.getLatestReadiness() } returns flowOf(readinessEntity)
        return generator.generateProgram(4, equipmentType, "vtaper")
    }

    @Test
    fun `lateral deltoid priority prefers specialist and adds distinct second movement`() = runTest {
        val upperA = generate("gym").days.first { it.name == "Upper A" }
        val names = upperA.exercises.map { it.exerciseName }
        assertTrue("Lateral Raise expected", "Lateral Raise" in names)
        assertTrue("Back movement expected", "Barbell Row" in names || "Dumbbell Row" in names)
        assertTrue("session cap must be respected", names.size <= 7)
    }

    @Test
    fun `equipment filtering excludes unsupported compound equipment`() = runTest {
        val program = generate("home")
        val allNames = program.days.flatMap { it.exercises }.map { it.exerciseName }
        assertFalse("Barbell Row must be excluded at home", "Barbell Row" in allNames)
        assertFalse("Upright Row must be excluded at home", "Upright Row" in allNames)
        assertTrue("Dumbbell Row expected at home", "Dumbbell Row" in allNames)
        assertTrue("Push-up expected at home", "Push-up" in allNames)
        assertFalse("Dumbbell+bench compound must be excluded at home", "Incline DB Press" in allNames)
        assertEquals(4, program.days.size)
    }

    @Test
    fun `strict dumbbell bodyweight profile excludes bench and bar equipment`() = runTest {
        val allNames = generate("dumbbell_bodyweight").days.flatMap { it.exercises }.map { it.exerciseName }
        assertTrue("Push-up expected", "Push-up" in allNames)
        assertTrue("Dumbbell Row expected", "Dumbbell Row" in allNames)
        assertTrue("Lateral Raise expected", "Lateral Raise" in allNames)
        assertFalse("Incline DB Press requires bench", "Incline DB Press" in allNames)
        assertFalse("Barbell Row requires barbell", "Barbell Row" in allNames)
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
    fun `low readiness reduces every selected exercise by one set but preserves slot budget`() = runTest {
        val readiness = ReadinessEntity(sleepQuality = 2, soreness = 2, energy = 2, motivation = 2)
        val program = generate("gym", readiness)
        val exercises = program.days.flatMap { it.exercises }
        assertTrue(exercises.all { it.targetSets in 1..3 })
        assertTrue(exercises.all { it.targetRpe == 7.0 })
    }

    @Test
    fun `high readiness does not blindly inflate every exercise`() = runTest {
        val readiness = ReadinessEntity(sleepQuality = 5, soreness = 4, energy = 5, motivation = 4)
        val program = generate("gym", readiness)
        val exercises = program.days.flatMap { it.exercises }
        assertTrue(exercises.all { it.targetSets in 1..4 })
        assertTrue(exercises.all { it.targetRpe == 8.0 })
        assertTrue("program must retain session cap", program.days.all { it.exercises.size <= 7 })
    }

    @Test
    fun `default readiness preserves explicit slot budgets`() = runTest {
        val upperA = generate("gym").days.first { it.name == "Upper A" }
        assertEquals(4, upperA.exercises.first { it.exerciseName == "Barbell Row" }.targetSets)
        assertEquals(3, upperA.exercises.first { it.exerciseName == "Incline DB Press" }.targetSets)
        assertEquals(3, upperA.exercises.first { it.exerciseName == "Lateral Raise" }.targetSets)
    }

    @Test
    fun `exercise metadata supplies rep range and rest`() = runTest {
        val upperA = generate("gym").days.first { it.name == "Upper A" }
        val press = upperA.exercises.first { it.exerciseName == "Incline DB Press" }
        assertEquals(6, press.targetRepsMin)
        assertEquals(10, press.targetRepsMax)
        assertEquals(120, press.restSeconds)
    }

    @Test
    fun `null readiness falls back to neutral score`() = runTest {
        val program = generate("gym", null)
        assertTrue(program.description.contains("3.0"))
        assertTrue(program.days.all { it.exercises.size <= 7 })
    }
}
