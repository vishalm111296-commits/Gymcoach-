package com.gymcoach.app.database

import androidx.room.RoomMigrationTest
import androidx.room.MigrationTestHelper
import com.gymcoach.app.data.local.database.GymCoachDatabase
import com.gymcoach.app.data.local.entity.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Forensic migration test verifies data preservation through the complete
 * migration chain: 1→2→3→4→5.
 *
 * Each migration step must be individually verifiable, and the complete chain
 * must preserve all data without destruction or fallbackToDestructiveMigration.
 */
class RoomMigrationTest {

    @Test
    fun `migration chain 1→2→3→4→5 preserves exercise data`() = runBlocking {
        // Arrange: Create database at version 1 with exercise data
        val helper = MigrationTestHelper(
            db = GymCoachDatabase::class.java,
            oldVersion = 1,
            newVersion = 5,
            migrationRuns = listOf(
                1 to 2,
                2 to 3,
                3 to 4,
                4 to 5
            )
        )

        // Act: Create the v1 database and insert realistic exercise data
        // (MIGRATION_1_2 already creates workouts, workout_exercises, workout_sets tables)
        // Version 1 has only exercises table with basic columns (before workout tables)
        helper.createDb()

        val exerciseDao = helper.db.exerciseDao()
        runBlocking {
            exerciseDao.insert(
                ExerciseEntity(
                    name = "Bench Press",
                    description = "Chest exercise",
                    muscleGroup = "Chest",
                    equipment = "Barbell",
                    difficulty = "Intermediate",
                    secondaryMuscles = "Triceps, Shoulders",
                    instructions = "Lie on a bench and press the barbell from your chest to full arm extension.",
                    tips = "Keep feet flat on floor.",
                    commonMistakes = "Bouncing bar off chest.",
                    safetyNotes = "Use a spotter.",
                    recommendedRepRange = "8-12",
                    recommendedRestTime = "90s",
                    estimatedCalories = 15,
                    category = "Powerlifting",
                    tags = "Push",
                    isFavorite = false,
                    lastViewed = 0L,
                    // vtaper columns are added by MIGRATION_2_3, set defaults for v1
                    vtaperLat = 0,
                    vtaperLateralDelt = 0,
                    vtaperUpperChest = 0,
                    vtaperRearDelt = 0,
                    movementPattern = "",
                    imageUrl = null,
                    videoUrl = null,
                    animationUrl = null,
                    setupInstructions = "",
                    executionInstructions = "",
                    breathingInstructions = "",
                    tempoGuidance = "",
                    beginnerVariantId = 0L,
                    advancedVariantId = 0L
                )
            )
        }

        // Close the database to simulate an upgrade scenario
        helper.closeDb()

        // Run migrations from v1 to v5
        helper.runMigrationsSync()

        // Assert: Schema export exists (exportSchema = true)
        val schemaFile = java.io.File(
            "${helper.db.context.getFilesDir().parentFile}/schemas/main/exercises.json"
        )
        assertTrue("Schema JSON should exist with exportSchema=true", schemaFile.exists())

        // Reopen the database at version 5 and verify data survived
        helper.createDb()

        // Read back the migrated exercise data
        val result = runBlocking {
            exerciseDao.getAll().first()
        }

        // Assert: All data preserved through the complete migration chain
        assertNotNull("Exercise data should survive migration 1→5", result)
        assertEquals("Bench Press", result.name)
        assertEquals("Chest", result.muscleGroup)
        assertEquals("Barbell", result.equipment)
        // V-taper scores should have default values (0) after migration
        assertEquals(0, result.vtaperLat)
        assertEquals(0, result.vtaperLateralDelt)
        assertEquals(0, result.vtaperUpperChest)
        assertEquals(0, result.vtaperRearDelt)
    }
}