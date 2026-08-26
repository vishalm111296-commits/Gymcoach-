package com.gymcoach.app.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymcoach.app.core.program.ProgramGenerator
import com.gymcoach.app.core.exercise.EquipmentAvailability
import com.gymcoach.app.data.local.database.GymCoachDatabase
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.ProgramDao
import com.gymcoach.app.data.local.dao.ProgramDayDao
import com.gymcoach.app.data.local.dao.ProgramExerciseDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.ProgramEntity
import com.gymcoach.app.data.local.entity.ProgramDayEntity
import com.gymcoach.app.data.local.entity.ProgramExerciseEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for ProgramRepository and ProgramGenerator using real Room database.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ProgramRepositoryIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: GymCoachDatabase
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var programDao: ProgramDao
    private lateinit var programDayDao: ProgramDayDao
    private lateinit var programExerciseDao: ProgramExerciseDao
    private lateinit var equipmentAvailability: EquipmentAvailability
    private lateinit var programGenerator: ProgramGenerator
    private lateinit var programRepository: ProgramRepositoryImpl

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GymCoachDatabase::class.java
        ).allowMainThreadQueries().build()
        exerciseDao = db.exerciseDao()
        programDao = db.programDao()
        programDayDao = db.programDayDao()
        programExerciseDao = db.programExerciseDao()
        equipmentAvailability = EquipmentAvailability()
        programGenerator = ProgramGenerator(exerciseDao, equipmentAvailability)
        programRepository = ProgramRepositoryImpl(programDao, programDayDao, programExerciseDao)
    }

    @Test
    fun `ProgramGenerator creates 4-day program with home equipment`() = runTest {
        // Insert test exercises matching home equipment
        val exercises = listOf(
            exercise(1, "DB Bench Press", "Chest", "dumbbell,bench", "Intermediate",
                vtaperLat = 0, vtaperLateralDelt = 2, vtaperUpperChest = 6, vtaperRearDelt = 1),
            exercise(2, "DB Row", "Back", "dumbbell,bench", "Intermediate",
                vtaperLat = 7, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 4),
            exercise(3, "Push-up", "Chest", "bodyweight", "Beginner",
                vtaperLat = 0, vtaperLateralDelt = 1, vtaperUpperChest = 5, vtaperRearDelt = 1),
            exercise(4, "Pull-up", "Back", "bodyweight", "Intermediate",
                vtaperLat = 10, vtaperLateralDelt = 1, vtaperUpperChest = 1, vtaperRearDelt = 4),
            exercise(5, "DB Lateral Raise", "Lateral Deltoid", "dumbbell", "Beginner",
                vtaperLat = 1, vtaperLateralDelt = 10, vtaperUpperChest = 0, vtaperRearDelt = 2),
            exercise(6, "DB Rear Delt Fly", "Rear Deltoid", "dumbbell", "Beginner",
                vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 8),
            exercise(7, "DB Bicep Curl", "Biceps", "dumbbell", "Beginner",
                vtaperLat = 1, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0),
            exercise(8, "DB Tricep Extension", "Triceps", "dumbbell", "Beginner",
                vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 1, vtaperRearDelt = 0),
            exercise(9, "Goblet Squat", "Quadriceps", "dumbbell", "Beginner",
                vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0),
            exercise(10, "DB RDL", "Hamstrings", "dumbbell", "Intermediate",
                vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0),
            exercise(11, "Plank", "Core", "bodyweight", "Beginner",
                vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0),
            exercise(12, "Calf Raise", "Calves", "dumbbell", "Beginner",
                vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0)
        )
        exercises.forEach { exerciseDao.insert(it) }

        val program = programGenerator.generateProgram(4, "home", "hypertrophy")

        assertEquals("Should have 4 days", 4, program.days.size)
        program.days.forEach { day ->
            assertTrue("Day ${day.dayNumber} should have exercises", day.exercises.isNotEmpty())
            day.exercises.forEach { ex ->
                val exerciseEntity = exercises.find { it.id == ex.exerciseId }
                assertNotNull("Exercise should exist", exerciseEntity)
                // Verify equipment is available for home
                val tokens = exerciseEntity!!.equipment.split(",").map { it.trim().lowercase() }
                tokens.forEach { token ->
                    assertTrue("Token $token should be available for home",
                        equipmentAvailability.isAvailable(token, "home") || token == "bodyweight")
                }
            }
        }
    }

    @Test
    fun `ProgramGenerator excludes barbell exercises for home equipment`() = runTest {
        val exercises = listOf(
            exercise(1, "DB Bench Press", "Chest", "dumbbell,bench", "Intermediate"),
            exercise(2, "Barbell Bench Press", "Chest", "barbell", "Intermediate"),
            exercise(3, "Push-up", "Chest", "bodyweight", "Beginner"),
            exercise(4, "DB Row", "Back", "dumbbell,bench", "Intermediate"),
            exercise(5, "Barbell Row", "Back", "barbell", "Intermediate"),
            exercise(6, "Pull-up", "Back", "bodyweight", "Intermediate"),
            exercise(7, "DB Lateral Raise", "Lateral Deltoid", "dumbbell", "Beginner"),
            exercise(8, "DB Rear Delt Fly", "Rear Deltoid", "dumbbell", "Beginner"),
            exercise(9, "DB Bicep Curl", "Biceps", "dumbbell", "Beginner"),
            exercise(10, "DB Tricep Extension", "Triceps", "dumbbell", "Beginner"),
            exercise(11, "Goblet Squat", "Quadriceps", "dumbbell", "Beginner"),
            exercise(12, "DB RDL", "Hamstrings", "dumbbell", "Intermediate"),
            exercise(13, "Plank", "Core", "bodyweight", "Beginner"),
            exercise(14, "Calf Raise", "Calves", "dumbbell", "Beginner")
        )
        exercises.forEach { exerciseDao.insert(it) }

        val program = programGenerator.generateProgram(4, "home", "intermediate", "hypertrophy")

        program.days.flatMap { it.exercises }.forEach { ex ->
            val entity = exercises.find { it.id == ex.exerciseId }
            assertNotNull(entity)
            assertTrue("Exercise ${entity!!.name} should not require barbell",
                !entity.equipment.contains("barbell"))
        }
    }

    @Test
    fun `ProgramRepository save and retrieve program`() = runTest {
        val program = ProgramEntity(
            userId = 1,
            name = "Test Program",
            description = "Test",
            splitType = "UpperLower",
            durationWeeks = 12,
            daysPerWeek = 4,
            difficulty = "Intermediate",
            goal = "Hypertrophy",
            isActive = 1,
            createdAt = System.currentTimeMillis()
        )
        val programId = programDao.insertProgram(program)

        val day1 = ProgramDayEntity(programId, 1, "Upper A", "Chest,Back,Shoulders,Arms", 0)
        val day2 = ProgramDayEntity(programId, 2, "Lower A", "Legs,Core", 0)
        val d1Id = programDayDao.insertProgramDay(day1)
        val d2Id = programDayDao.insertProgramDay(day2)

        programExerciseDao.insertProgramExercise(ProgramExerciseEntity(d1Id, 1, 0, 3, "8-12", 0.0, 90, ""))
        programExerciseDao.insertProgramExercise(ProgramExerciseEntity(d1Id, 2, 1, 3, "8-12", 0.0, 90, ""))

        val retrieved = programRepository.getActiveProgram().first()
        assertNotNull(retrieved)
        assertEquals("Test Program", retrieved!!.name)
        assertEquals(4, retrieved.daysPerWeek)
    }

    private fun exercise(
        id: Long,
        name: String,
        muscleGroup: String,
        equipment: String,
        difficulty: String,
        vtaperLat: Int = 0,
        vtaperLateralDelt: Int = 0,
        vtaperUpperChest: Int = 0,
        vtaperRearDelt: Int = 0
    ): ExerciseEntity {
        return ExerciseEntity(
            id = id,
            name = name,
            description = "Test",
            muscleGroup = muscleGroup,
            equipment = equipment,
            difficulty = difficulty,
            secondaryMuscles = "",
            instructions = "",
            tips = "",
            commonMistakes = "",
            safetyNotes = "",
            recommendedRepRange = "8-12",
            recommendedRestTime = "90",
            estimatedCalories = 10,
            category = "Resistance",
            tags = "compound",
            isFavorite = false,
            lastViewed = 0,
            vtaperLat = vtaperLat,
            vtaperLateralDelt = vtaperLateralDelt,
            vtaperUpperChest = vtaperUpperChest,
            vtaperRearDelt = vtaperRearDelt,
            movementPattern = "horizontal_push",
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
}