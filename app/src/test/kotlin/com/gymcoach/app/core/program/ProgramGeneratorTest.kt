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
    private lateinit var readinessRepository: ReadinessRepository
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
    fun `low readiness under 2_5 reduces sets to 2 and RPE to 7_0`() = runTest {
        val readiness = ReadinessEntity(sleepQuality = 2, soreness = 2, energy = 2, motivation = 2)
        val program = generate("gym", readiness)
        val sets = program.days.flatMap { it.exercises }.map { it.targetSets }
        val rpe = program.days.flatMap { it.exercises }.map { it.targetRpe }
        assertTrue("All sets should be 2 (3-1 reduced)", sets.all { it == 2 })
        assertTrue("All RPE should be 7.0 (7.5-0.5 reduced)", rpe.all { it == 7.0 })
    }

    @Test
    fun `high readiness 4_0 or above increases sets to 4 and RPE to 8_0`() = runTest {
        val readiness = ReadinessEntity(sleepQuality = 5, soreness = 4, energy = 5, motivation = 4)
        val program = generate("gym", readiness)
        val sets = program.days.flatMap { it.exercises }.map { it.targetSets }
        val rpe = program.days.flatMap { it.exercises }.map { it.targetRpe }
        assertTrue("All sets should be 4 (3+1 increased)", sets.all { it == 4 })
        assertTrue("All RPE should be 8.0 (7.5+0.5 increased)", rpe.all { it == 8.0 })
    }

    @Test
    fun `default readiness 3_0 keeps base sets 3 and RPE 7_5`() = runTest {
        val program = generate("gym")
        val sets = program.days.flatMap { it.exercises }.map { it.targetSets }
        val rpe = program.days.flatMap { it.exercises }.map { it.targetRpe }
        assertTrue("All sets should be 3 (default)", sets.all { it == 3 })
        assertTrue("All RPE should be 7.5 (default)", rpe.all { it == 7.5 })
    }

    @Test
    fun `null readiness falls back to 3_0 score with base sets and RPE`() = runTest {
        val program = generate("gym", null)
        val sets = program.days.flatMap { it.exercises }.map { it.targetSets }
        val rpe = program.days.flatMap { it.exercises }.map { it.targetRpe }
        assertTrue("All sets should be 3 (fallback)", sets.all { it == 3 })
        assertTrue("All RPE should be 7.5 (fallback)", rpe.all { it == 7.5 })
        assertTrue("Description should mention fallback", program.description.contains("3.0"))
    }

    private suspend fun generateWithFrequency(
        frequency: Int,
        equipmentType: String = "gym",
        readinessEntity: ReadinessEntity? = null
    ): ProgramGenerator.GeneratedProgram {
        coEvery { dao.getAll() } returns flowOf(all())
        coEvery { readinessRepository.getLatestReadiness() } returns flowOf(readinessEntity)
        return generator.generateProgram(frequency, equipmentType, "vtaper")
    }

    @Test
    fun `frequency 1 generates single full body day with max 8 exercise cap`() = runTest {
        val program = generateWithFrequency(1)
        assertEquals(1, program.days.size)
        assertEquals("Full Body A", program.days[0].name)
        assertTrue("Day should respect cap of 8", program.days[0].exercises.size <= 8)
    }

    @Test
    fun `frequency 2 generates two full body days`() = runTest {
        val program = generateWithFrequency(2)
        assertEquals(2, program.days.size)
        assertEquals("Full Body A", program.days[0].name)
        assertEquals("Full Body B", program.days[1].name)
    }

    @Test
    fun `frequency 5 generates PPL Upper Lower hybrid with max 5 exercise cap`() = runTest {
        val program = generateWithFrequency(5)
        assertEquals(5, program.days.size)
        assertEquals(listOf("Push", "Pull", "Legs", "Upper", "Lower"), program.days.map { it.name })
        assertTrue("All days should respect cap of 5", program.days.all { it.exercises.size <= 5 })
    }

    @Test
    fun `frequency 6 generates PPL double with max 4 exercise cap`() = runTest {
        val program = generateWithFrequency(6)
        assertEquals(6, program.days.size)
        assertEquals(listOf("Push", "Pull", "Legs", "Push", "Pull", "Legs"), program.days.map { it.name })
        assertTrue("All days should respect cap of 4", program.days.all { it.exercises.size <= 4 })
    }

    @Test
    fun `frequency 7 generates 7 days including day 7 Full Body finisher`() = runTest {
        val program = generateWithFrequency(7)
        assertEquals(7, program.days.size)
        assertEquals("Full Body", program.days[6].name)
    }

    @Test
    fun `frequency 3 generates three full body days with max 7 exercise cap`() = runTest {
        val program = generateWithFrequency(3)
        assertEquals(3, program.days.size)
        assertEquals(listOf("Full Body A", "Full Body B", "Full Body C"), program.days.map { it.name })
        assertTrue("All days should respect cap of 7", program.days.all { it.exercises.size <= 7 })
    }

    @Test
    fun `difficulty ordering breaks ties when vtaper score is identical`() = runTest {
        val advLateral = ExerciseEntity(
            id = 20, name = "Cable Lateral Raise", description = "", muscleGroup = "Lateral Deltoid",
            equipment = "cable", difficulty = "Advanced", vtaperLateralDelt = 8
        )
        val begLateral = ExerciseEntity(
            id = 21, name = "DB Lateral Raise", description = "", muscleGroup = "Lateral Deltoid",
            equipment = "dumbbell", difficulty = "Beginner", vtaperLateralDelt = 8
        )
        coEvery { dao.getAll() } returns flowOf(listOf(advLateral, begLateral))
        coEvery { readinessRepository.getLatestReadiness() } returns flowOf(null)

        val program = generator.generateProgram(4, "gym", "vtaper")
        val upperA = program.days.first { it.name == "Upper A" }
        val names = upperA.exercises.map { it.exerciseName }

        assertEquals(listOf("DB Lateral Raise", "Cable Lateral Raise"), names)
    }

    @Test
    fun `exercise is never duplicated in the same training day despite matching multiple target muscle slots`() = runTest {
        val multiSlotEx = ExerciseEntity(
            id = 30, name = "Incline DB Curl", description = "",
            muscleGroup = "Biceps", secondaryMuscles = "Lateral Deltoid",
            equipment = "dumbbell", difficulty = "Beginner",
            vtaperLateralDelt = 5
        )
        coEvery { dao.getAll() } returns flowOf(listOf(multiSlotEx))
        coEvery { readinessRepository.getLatestReadiness() } returns flowOf(null)

        val program = generator.generateProgram(4, "gym", "vtaper")
        val upperA = program.days.first { it.name == "Upper A" }
        val matchingOccurrences = upperA.exercises.count { it.exerciseId == 30L }

        assertEquals("Exercise must appear exactly once in the session", 1, matchingOccurrences)
    }

    @Test
    fun `matchesMuscle handles compound movement patterns correctly across legs arms and core`() = runTest {
        val squat = ExerciseEntity(id = 40, name = "Barbell Squat", description = "", muscleGroup = "legs", movementPattern = "squat", equipment = "barbell", difficulty = "Intermediate")
        val rdl = ExerciseEntity(id = 41, name = "Romanian Deadlift", description = "", muscleGroup = "legs", movementPattern = "hip_hinge", equipment = "barbell", difficulty = "Intermediate")
        val curl = ExerciseEntity(id = 42, name = "Hammer Curl", description = "", muscleGroup = "arms", movementPattern = "elbow_flexion", equipment = "dumbbell", difficulty = "Beginner")
        val tricep = ExerciseEntity(id = 43, name = "Tricep Extension", description = "", muscleGroup = "arms", movementPattern = "elbow_extension", equipment = "cable", difficulty = "Beginner")
        val plank = ExerciseEntity(id = 44, name = "Core Plank", description = "", muscleGroup = "core", movementPattern = "anti_extension", equipment = "bodyweight", difficulty = "Beginner")

        coEvery { dao.getAll() } returns flowOf(listOf(squat, rdl, curl, tricep, plank))
        coEvery { readinessRepository.getLatestReadiness() } returns flowOf(null)

        val program = generator.generateProgram(4, "gym", "vtaper")
        val lowerA = program.days.first { it.name == "Lower A" }
        val lowerExercises = lowerA.exercises.map { it.exerciseName }
        assertTrue("Barbell Squat expected in Lower A via legs/squat matching", "Barbell Squat" in lowerExercises)
        assertTrue("Romanian Deadlift expected in Lower A via legs/hip_hinge matching", "Romanian Deadlift" in lowerExercises)

        val upperA = program.days.first { it.name == "Upper A" }
        val upperExercises = upperA.exercises.map { it.exerciseName }
        assertTrue("Hammer Curl expected in Upper A via arms/elbow_flexion matching", "Hammer Curl" in upperExercises)
        assertTrue("Tricep Extension expected in Upper A via arms/elbow_extension matching", "Tricep Extension" in upperExercises)

        val lowerB = program.days.first { it.name == "Lower B" }
        val lowerBExercises = lowerB.exercises.map { it.exerciseName }
        assertTrue("Core Plank expected in Lower B via core matching", "Core Plank" in lowerBExercises)
    }

    @Test
    fun `session exercise budget cap strictly truncates surplus candidates`() = runTest {
        // Upper A has 6 slots: Back, Chest, Lateral Deltoid, Rear Deltoid, Biceps, Triceps
        // Create 2 unique exercises for each of the 6 slots = 12 total valid exercises
        val surplusExercises = (1..12).map { idx ->
            val slotMuscle = when (idx) {
                1, 2 -> "Back"
                3, 4 -> "Chest"
                5, 6 -> "Lateral Deltoid"
                7, 8 -> "Rear Deltoid"
                9, 10 -> "Biceps"
                else -> "Triceps"
            }
            ExerciseEntity(
                id = 100L + idx,
                name = "Exercise $idx",
                description = "",
                muscleGroup = slotMuscle,
                equipment = "dumbbell",
                difficulty = "Beginner"
            )
        }

        coEvery { dao.getAll() } returns flowOf(surplusExercises)
        coEvery { readinessRepository.getLatestReadiness() } returns flowOf(null)

        // Frequency 4 has cap = 6 exercises
        val programFreq4 = generator.generateProgram(4, "gym", "vtaper")
        val upperAFreq4 = programFreq4.days.first { it.name == "Upper A" }
        assertEquals("Upper A must be strictly capped at 6 exercises", 6, upperAFreq4.exercises.size)

        // Frequency 6 has cap = 4 exercises
        val programFreq6 = generator.generateProgram(6, "gym", "vtaper")
        val pushDayFreq6 = programFreq6.days.first { it.name == "Push" }
        assertEquals("Push day must be strictly capped at 4 exercises", 4, pushDayFreq6.exercises.size)
    }
}

