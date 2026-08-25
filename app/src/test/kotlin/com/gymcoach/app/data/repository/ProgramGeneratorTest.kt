package com.gymcoach.app.data.repository

import com.gymcoach.app.core.exercise.EquipmentAvailability
import com.gymcoach.app.core.program.ProgramGenerator
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ProgramGeneratorTest - real tests using the actual ProgramGenerator.
 *
 * Verifies program generation across frequencies, equipment restrictions,
 * muscle distribution, exercise selection, and V-taper prioritization.
 * Equipment names match ExerciseEntity.equipment values in the DB seed data.
 */
class ProgramGeneratorTest {

    private lateinit var generator: ProgramGenerator
    private lateinit var mockExerciseDao: ExerciseDao
    private lateinit var mockEquipmentAvailability: EquipmentAvailability

    // Test exercises covering all major muscle groups and equipment types
    // Equipment names MUST match DB seed data: "Barbell", "Dumbbell", "Bodyweight", "Cable", "Machine"
    private val testExercises = listOf(
        // Chest exercises
        exercise(1, "Bench Press", "Chest", "Barbell", 5, 3, 4, 2),
        exercise(2, "Dumbbell Press", "Chest", "Dumbbell", 3, 2, 3, 1),
        exercise(3, "Push-up", "Chest", "Bodyweight", 2, 1, 2, 0),
        exercise(4, "Incline Bench Press", "Chest", "Barbell", 4, 2, 3, 1),
        // Back exercises
        exercise(5, "Barbell Row", "Back", "Barbell", 4, 3, 3, 2),
        exercise(6, "Dumbbell Row", "Back", "Dumbbell", 3, 2, 3, 1),
        exercise(7, "Pull-up", "Back", "Bodyweight", 3, 1, 3, 1),
        exercise(8, "Lat Pulldown", "Back", "Cable", 2, 1, 2, 1),
        // Shoulder exercises
        exercise(9, "Overhead Press", "Lateral Deltoid", "Barbell", 4, 3, 4, 2),
        exercise(10, "Lateral Raise", "Lateral Deltoid", "Dumbbell", 2, 1, 2, 1),
        exercise(11, "Rear Delt Fly", "Rear Deltoid", "Dumbbell", 1, 1, 2, 1),
        exercise(12, "Face Pull", "Rear Deltoid", "Cable", 1, 1, 1, 1),
        // Leg exercises
        exercise(13, "Squat", "Quadriceps", "Barbell", 5, 3, 4, 3),
        exercise(14, "Leg Press", "Quadriceps", "Machine", 3, 2, 3, 2),
        exercise(15, "Romanian Deadlift", "Hamstrings", "Barbell", 4, 3, 3, 2),
        exercise(16, "Leg Curl", "Hamstrings", "Machine", 2, 1, 2, 1),
        exercise(17, "Bulgarian Split Squat", "Glutes", "Dumbbell", 3, 2, 3, 2),
        exercise(18, "Calf Raise", "Calves", "Machine", 2, 1, 2, 1),
        // Arm exercises
        exercise(19, "Barbell Curl", "Biceps", "Barbell", 3, 2, 3, 1),
        exercise(20, "Dumbbell Curl", "Biceps", "Dumbbell", 2, 1, 2, 1),
        exercise(21, "Tricep Pushdown", "Triceps", "Cable", 2, 1, 2, 1),
        exercise(22, "Skull Crusher", "Triceps", "Barbell", 2, 2, 3, 1),
        // Core exercises
        exercise(23, "Plank", "Core", "Bodyweight", 1, 1, 1, 0),
        exercise(24, "Cable Crunch", "Core", "Cable", 1, 1, 1, 1),
    )

    @Before
    fun setup() {
        mockExerciseDao = mockk<ExerciseDao>()
        mockEquipmentAvailability = mockk<EquipmentAvailability>()

        every { mockExerciseDao.getAll() } returns flowOf(testExercises)
        every { mockEquipmentAvailability.getAvailableEquipment(any()) } returns setOf(
            "Barbell", "Dumbbell", "Bodyweight", "Flat Bench",
            "Cable", "Machine"
        )

        generator = ProgramGenerator(mockExerciseDao, mockEquipmentAvailability)
    }

    @Test
    fun `3-day program generates 3 days`() = runTest {
        val program = generator.generateProgram(3, "gym", "intermediate", "hypertrophy")
        assertEquals("3-day program should have 3 days", 3, program.days.size)
    }

    @Test
    fun `4-day program generates 4 days`() = runTest {
        val program = generator.generateProgram(4, "gym", "intermediate", "hypertrophy")
        assertEquals("4-day program should have 4 days", 4, program.days.size)
    }

    @Test
    fun `5-day program generates 5 days`() = runTest {
        val program = generator.generateProgram(5, "gym", "intermediate", "hypertrophy")
        assertEquals("5-day program should have 5 days", 5, program.days.size)
    }

    @Test
    fun `6-day program generates 6 days`() = runTest {
        val program = generator.generateProgram(6, "gym", "intermediate", "hypertrophy")
        assertEquals("6-day program should have 6 days", 6, program.days.size)
    }

    @Test
    fun `each day has at least 1 exercise`() = runTest {
        val program = generator.generateProgram(4, "gym", "intermediate", "hypertrophy")
        for (day in program.days) {
            assertTrue(
                "Day ${day.dayNumber} (${day.name}) should have at least 1 exercise",
                day.exercises.isNotEmpty()
            )
        }
    }

    @Test
    fun `no duplicate exercises within a day`() = runTest {
        val program = generator.generateProgram(4, "gym", "intermediate", "hypertrophy")
        for (day in program.days) {
            val exerciseIds = day.exercises.map { it.exerciseId }
            assertEquals(
                "Day ${day.dayNumber} should have no duplicate exercises",
                exerciseIds.size,
                exerciseIds.toSet().size
            )
        }
    }

    @Test
    fun `program name contains frequency`() = runTest {
        val program = generator.generateProgram(5, "gym", "intermediate", "hypertrophy")
        assertTrue(
            "Program name should contain '5'",
            program.name.contains("5")
        )
    }

    @Test
    fun `program goal is passed through`() = runTest {
        val program = generator.generateProgram(4, "gym", "intermediate", "strength")
        assertEquals("Goal should be passed through", "strength", program.goal)
    }

    @Test
    fun `program frequency is passed through`() = runTest {
        val program = generator.generateProgram(6, "gym", "intermediate", "hypertrophy")
        assertEquals("Frequency should be 6", 6, program.frequency)
    }

    @Test
    fun `dumbbell-only equipment excludes barbell exercises`() = runTest {
        every { mockEquipmentAvailability.getAvailableEquipment("custom") } returns setOf(
            "Dumbbell", "Bodyweight", "Flat Bench"
        )

        val program = generator.generateProgram(4, "custom", "intermediate", "hypertrophy")

        for (day in program.days) {
            for (exercise in day.exercises) {
                val exerciseEntity = testExercises.find { it.id == exercise.exerciseId }
                assertNotNull("Exercise should exist in test data", exerciseEntity)
                assertTrue(
                    "Exercise ${exercise.exerciseName} should not require Barbell (equipment: ${exerciseEntity!!.equipment})",
                    !exerciseEntity.equipment.contains("Barbell")
                )
            }
        }
    }

    @Test
    fun `dumbbell-only equipment excludes cable exercises`() = runTest {
        every { mockEquipmentAvailability.getAvailableEquipment("custom") } returns setOf(
            "Dumbbell", "Bodyweight", "Flat Bench"
        )

        val program = generator.generateProgram(4, "custom", "intermediate", "hypertrophy")

        for (day in program.days) {
            for (exercise in day.exercises) {
                val exerciseEntity = testExercises.find { it.id == exercise.exerciseId }
                assertNotNull("Exercise should exist in test data", exerciseEntity)
                assertTrue(
                    "Exercise ${exercise.exerciseName} should not require Cable (equipment: ${exerciseEntity!!.equipment})",
                    !exerciseEntity.equipment.contains("Cable")
                )
            }
        }
    }

    @Test
    fun `bodyweight exercises always included regardless of equipment`() = runTest {
        every { mockEquipmentAvailability.getAvailableEquipment("custom") } returns setOf("Bodyweight")

        val program = generator.generateProgram(4, "custom", "intermediate", "hypertrophy")

        val exerciseIds = program.days.flatMap { it.exercises }.map { it.exerciseId }.toSet()
        // Push-up (id=3) and Pull-up (id=7) are Bodyweight and should be included
        assertTrue("Push-up should be included", exerciseIds.contains(3))
        assertTrue("Pull-up should be included", exerciseIds.contains(7))
    }

    @Test
    fun `vtaper sorting prioritizes high vtaper exercises over low vtaper exercises`() = runTest {
        // Create exercises where one has higher vtaper scores for the same muscle
        val vtaperExercises = listOf(
            exercise(100, "Lateral Raise", "Lateral Deltoid", "Dumbbell",
                vtaperLat = 1, vtaperLateralDelt = 10, vtaperUpperChest = 0, vtaperRearDelt = 2),
            exercise(101, "Shoulder Shrug", "Lateral Deltoid", "Dumbbell",
                vtaperLat = 0, vtaperLateralDelt = 1, vtaperUpperChest = 0, vtaperRearDelt = 0),
            exercise(102, "Barbell Row", "Back", "Barbell",
                vtaperLat = 9, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 5),
            exercise(103, "Bicep Curl", "Back", "Dumbbell",
                vtaperLat = 1, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0),
        )

        every { mockExerciseDao.getAll() } returns flowOf(vtaperExercises)
        every { mockEquipmentAvailability.getAvailableEquipment("gym") } returns setOf(
            "Barbell", "Dumbbell", "Bodyweight"
        )

        val program = generator.generateProgram(4, "gym", "intermediate", "hypertrophy")

        // Find the Pull/Lateral Deltoid days and verify vtaper-prioritized exercises come first
        // NOTE: prior version inspected the day's absolute first exercise regardless of muscle,
        // demanding a single exercise high-Lat AND high-LD (unsatisfiable); checks are now
        // scoped per target muscle group; thresholds/messages unchanged.
        for (day in program.days) {
            val entitiesInDay = day.exercises.mapNotNull { pe -> vtaperExercises.find { it.id == pe.exerciseId } }
            if (day.targetMuscles.contains("Lateral Deltoid")) {
                val firstLdExercise = entitiesInDay.firstOrNull { it.muscleGroup == "Lateral Deltoid" }
                if (firstLdExercise != null) {
                    assertTrue(
                        "First Lateral Deltoid exercise should have high vtaper_lateral_delt (got ${firstLdExercise.vtaperLateralDelt})",
                        firstLdExercise.vtaperLateralDelt >= 5
                    )
                }
            }
            if (day.targetMuscles.contains("Back")) {
                val firstBackExercise = entitiesInDay.firstOrNull { it.muscleGroup == "Back" }
                if (firstBackExercise != null) {
                    assertTrue(
                        "First Back exercise should have high vtaper_lat (got ${firstBackExercise.vtaperLat})",
                        firstBackExercise.vtaperLat >= 5
                    )
                }
            }
        }
    }

    @Test
    fun `difficulty tiebreaker prefers beginner over advanced when vtaper equal`() = runTest {
        val exercises = listOf(
            exercise(200, "Easy Lateral", "Lateral Deltoid", "Dumbbell",
                vtaperLat = 0, vtaperLateralDelt = 5, vtaperUpperChest = 0, vtaperRearDelt = 0,
                difficulty = "Beginner"),
            exercise(201, "Hard Lateral", "Lateral Deltoid", "Dumbbell",
                vtaperLat = 0, vtaperLateralDelt = 5, vtaperUpperChest = 0, vtaperRearDelt = 0,
                difficulty = "Advanced"),
        )

        every { mockExerciseDao.getAll() } returns flowOf(exercises)
        every { mockEquipmentAvailability.getAvailableEquipment("gym") } returns setOf(
            "Barbell", "Dumbbell", "Bodyweight"
        )

        val program = generator.generateProgram(4, "gym", "intermediate", "hypertrophy")

        for (day in program.days) {
            if (day.targetMuscles.contains("Lateral Deltoid") && day.exercises.isNotEmpty()) {
                val firstExercise = day.exercises.first()
                assertEquals("Beginner exercise should come first when vtaper scores equal", 200L, firstExercise.exerciseId)
            }
        }
    }

    @Test
    fun `every exercise targets the expected muscle groups`() = runTest {
        val program = generator.generateProgram(4, "gym", "intermediate", "hypertrophy")
        for (day in program.days) {
            for (exercise in day.exercises) {
                val exerciseEntity = testExercises.find { it.id == exercise.exerciseId }
                assertNotNull("Exercise ${exercise.exerciseName} should exist", exerciseEntity)
                val muscle = exerciseEntity!!.muscleGroup
                val matchesTarget = day.targetMuscles.any { target ->
                    target.equals(muscle, ignoreCase = true)
                } || exerciseEntity.secondaryMuscles.split(",").any { secondary ->
                    day.targetMuscles.any { target ->
                        target.trim().equals(secondary.trim(), ignoreCase = true)
                    }
                }
                assertTrue(
                    "Exercise ${exercise.exerciseName} (muscle: $muscle) should be in target muscles for ${day.name}: ${day.targetMuscles}",
                    matchesTarget
                )
            }
        }
    }

    @Test
    fun `all exercises have valid sets and reps`() = runTest {
        val program = generator.generateProgram(4, "gym", "intermediate", "hypertrophy")
        for (day in program.days) {
            for (exercise in day.exercises) {
                assertTrue(
                    "Exercise ${exercise.exerciseName} should have >0 sets",
                    exercise.targetSets > 0
                )
                assertTrue(
                    "Exercise ${exercise.exerciseName} should have repsMin <= repsMax",
                    exercise.targetRepsMin <= exercise.targetRepsMax
                )
                assertTrue(
                    "Exercise ${exercise.exerciseName} should have repsMin > 0",
                    exercise.targetRepsMin > 0
                )
                assertTrue(
                    "Exercise ${exercise.exerciseName} should have restSeconds > 0",
                    exercise.restSeconds > 0
                )
            }
        }
    }

    @Test
    fun `3-day full body covers major muscle groups`() = runTest {
        val program = generator.generateProgram(3, "gym", "intermediate", "hypertrophy")
        val allMuscles = program.days.flatMap { day ->
            day.exercises.map { exercise ->
                testExercises.find { it.id == exercise.exerciseId }?.muscleGroup ?: ""
            }
        }.toSet()

        assertTrue("Full body should cover Chest", allMuscles.contains("Chest"))
        assertTrue("Full body should cover Back", allMuscles.contains("Back"))
        assertTrue("Full body should cover Quadriceps", allMuscles.contains("Quadriceps"))
    }

    // Helper function to create test ExerciseEntity
    private fun exercise(
        id: Long,
        name: String,
        muscleGroup: String,
        equipment: String,
        vtaperLat: Int = 0,
        vtaperLateralDelt: Int = 0,
        vtaperUpperChest: Int = 0,
        vtaperRearDelt: Int = 0,
        secondaryMuscles: String = "",
        difficulty: String = "Intermediate"
    ) = ExerciseEntity(
        id = id,
        name = name,
        description = "Test exercise",
        muscleGroup = muscleGroup,
        equipment = equipment,
        difficulty = difficulty,
        secondaryMuscles = secondaryMuscles,
        instructions = "",
        tips = "",
        commonMistakes = "",
        safetyNotes = "",
        recommendedRepRange = "8-12",
        recommendedRestTime = "90",
        estimatedCalories = 10,
        category = "Resistance",
        tags = "",
        isFavorite = false,
        lastViewed = 0L,
        vtaperLat = vtaperLat,
        vtaperLateralDelt = vtaperLateralDelt,
        vtaperUpperChest = vtaperUpperChest,
        vtaperRearDelt = vtaperRearDelt,
        movementPattern = "",
        imageUrl = null,
        videoUrl = null,
        animationUrl = null,
        setupInstructions = "",
        executionInstructions = "",
        breathingInstructions = "",
        tempoGuidance = "",
        beginnerVariantId = null,
        advancedVariantId = null
    )
}
