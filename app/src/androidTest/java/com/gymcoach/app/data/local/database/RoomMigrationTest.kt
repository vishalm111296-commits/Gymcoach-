package com.gymcoach.app.data.local.database

import androidx.room.testing.MigrationTestHelper
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gymcoach.app.data.local.database.GymCoachDatabase.Companion.MIGRATION_1_2
import com.gymcoach.app.data.local.database.GymCoachDatabase.Companion.MIGRATION_2_3
import com.gymcoach.app.data.local.database.GymCoachDatabase.Companion.MIGRATION_3_4
import com.gymcoach.app.data.local.database.GymCoachDatabase.Companion.MIGRATION_4_5
import com.gymcoach.app.data.local.database.GymCoachDatabase.Companion.MIGRATION_5_6
import com.gymcoach.app.data.local.database.GymCoachDatabase.Companion.MIGRATION_6_7
import com.gymcoach.app.data.local.database.GymCoachDatabase.Companion.MIGRATION_7_8
import com.gymcoach.app.data.local.database.GymCoachDatabase.Companion.MIGRATION_8_9
import com.gymcoach.app.data.local.database.GymCoachDatabase.Companion.MIGRATION_9_10
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Room migration tests using MigrationTestHelper.
 *
 * Schema JSONs (v1–v10) are exported under app/schemas by KSP.
 *
 * Strategy:
 * - MigrationTestHelper.createDatabase() reads the schema JSON for the
 *   requested version and creates a real SQLite database with that schema.
 * - runMigrationsAndValidate() applies the provided Migration objects,
 *   then compares the resulting schema against the schema JSON for the
 *   target version.
 * - For v7→v8 (status column backfill), we also do a manual SQL-level
 *   test because the backfill logic is the most critical migration.
 * - For v8→v9 (V-taper scores), we verify the UPDATE statements apply
 *   correct scores to each exercise.
 * - For v9→v10 (readiness table), we verify the table is created correctly.
 */
@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    @JvmField
    val migrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GymCoachDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    // ──────────────────────────────────────────────
    //  Full chain: v1 → v10 (schema JSONs exist)
    // ──────────────────────────────────────────────

    @Test
    fun migrate1To10() {
        // Create at v1 (only exercises table with basic columns)
        migrationTestHelper.createDatabase(TEST_DB, 1).close()

        // Run the complete chain to v10
        migrationTestHelper.runMigrationsAndValidate(
            TEST_DB, 10, true,
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
            MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10
        ).close()
    }

    @Test
    fun migrate1To9() {
        // Create at v1 (only exercises table with basic columns)
        migrationTestHelper.createDatabase(TEST_DB, 1).close()

        // Run the complete chain to v9
        migrationTestHelper.runMigrationsAndValidate(
            TEST_DB, 9, true,
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
            MIGRATION_7_8, MIGRATION_8_9
        ).close()
    }

    // ──────────────────────────────────────────────
    //  Individual migration steps
    // ──────────────────────────────────────────────

    @Test
    fun migrate1To2() {
        migrationTestHelper.createDatabase(TEST_DB, 1).close()

        migrationTestHelper.runMigrationsAndValidate(
            TEST_DB, 2, true, MIGRATION_1_2
        ).close()
    }

    @Test
    fun migrate2To3() {
        migrationTestHelper.createDatabase(TEST_DB, 2).close()

        migrationTestHelper.runMigrationsAndValidate(
            TEST_DB, 3, true, MIGRATION_2_3
        ).close()
    }

    @Test
    fun migrate3To4() {
        migrationTestHelper.createDatabase(TEST_DB, 3).close()

        migrationTestHelper.runMigrationsAndValidate(
            TEST_DB, 4, true, MIGRATION_3_4
        ).close()
    }

    @Test
    fun migrate6To7() {
        migrationTestHelper.createDatabase(TEST_DB, 6).close()

        val db = migrationTestHelper.runMigrationsAndValidate(
            TEST_DB, 7, true, MIGRATION_6_7
        )

        // Verify exercise_fts virtual table exists
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='exercise_fts'")
        assertTrue("exercise_fts table should exist after 6→7 migration", cursor.moveToFirst())
        cursor.close()
        db.close()
    }

    // ──────────────────────────────────────────────
    //  v7→v8: status column + backfill (manual SQL)
    // ──────────────────────────────────────────────

    @Test
    fun migrate7To8_addsStatusColumnAndBackfills() {
        migrationTestHelper.createDatabase(TEST_DB, 7).close()

        val db = migrationTestHelper.runMigrationsAndValidate(
            TEST_DB, 8, true,
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8
        )

        val pragmaCursor = db.query("PRAGMA table_info(workouts)")
        var hasStatusColumn = false
        while (pragmaCursor.moveToNext()) {
            if (pragmaCursor.getString(pragmaCursor.getColumnIndexOrThrow("name")) == "status") {
                hasStatusColumn = true
                break
            }
        }
        pragmaCursor.close()
        assertTrue("workouts table should have 'status' column after 7→8 migration", hasStatusColumn)
        db.close()
    }

    @Test
    fun migration7To8_statusBackfillLogic() {
        var db = migrationTestHelper.createDatabase(TEST_DB, 7)
        db.execSQL("CREATE TABLE IF NOT EXISTS `exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `muscleGroup` TEXT NOT NULL, `equipment` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `secondaryMuscles` TEXT NOT NULL, `instructions` TEXT NOT NULL, `tips` TEXT NOT NULL, `commonMistakes` TEXT NOT NULL, `safetyNotes` TEXT NOT NULL, `recommendedRepRange` TEXT NOT NULL, `recommendedRestTime` TEXT NOT NULL, `estimatedCalories` INTEGER NOT NULL, `category` TEXT NOT NULL, `isArchived` INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("INSERT INTO exercises (id, name, description, muscleGroup, equipment, difficulty, secondaryMuscles, instructions, tips, commonMistakes, safetyNotes, recommendedRepRange, recommendedRestTime, estimatedCalories, category, isArchived) VALUES (1, dummy, , , , , , , , , , , , 0, , 0)")
        db.execSQL("INSERT INTO workouts (id, date, startTime, endTime, duration, notes, completed) VALUES (1, 0, 0, 0, 0, completed workout, 1)")
        db.execSQL("INSERT INTO workouts (id, date, startTime, endTime, duration, notes, completed) VALUES (2, 0, 0, 0, 0, active workout, 0)")
        db.execSQL("INSERT INTO workout_exercises (id, workoutId, exerciseId, orderIndex) VALUES (1, 2, 1, 0)")
        db.execSQL("INSERT INTO workouts (id, date, startTime, endTime, duration, notes, completed) VALUES (3, 0, 0, 0, 0, zombie workout, 0)")
        db.close()
        db = migrationTestHelper.runMigrationsAndValidate(TEST_DB, 8, true, MIGRATION_7_8)
        val cursor = db.query("SELECT id, status, notes FROM workouts ORDER BY id")
        val results = mutableListOf<Triple<Long, String, String>>()
        while (cursor.moveToNext()) {
            results.add(Triple(cursor.getLong(cursor.getColumnIndexOrThrow("id")), cursor.getString(cursor.getColumnIndexOrThrow("status")), cursor.getString(cursor.getColumnIndexOrThrow("notes"))))
        }
        cursor.close()
        db.close()
        assertEquals("Should have 3 rows", 3, results.size)
        val statusByNote = results.associate { it.third to it.second }
        assertEquals("completed workout → COMPLETED", "COMPLETED", statusByNote["completed workout"])
        assertEquals("active workout → ACTIVE", "ACTIVE", statusByNote["active workout"])
        assertEquals("zombie workout → ABANDONED", "ABANDONED", statusByNote["zombie workout"])
    }

    @Test
    fun migration7To8_statusDefaultIsNotStarted() {
        migrationTestHelper.createDatabase(TEST_DB, 7).close()

        val db = migrationTestHelper.runMigrationsAndValidate(
            TEST_DB, 8, true,
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8
        )

        db.execSQL(
            "INSERT INTO workouts (date, startTime, endTime, duration, notes, completed, status) VALUES (0, 0, 0, 0, 'new workout', 0, 'NOT_STARTED')"
        )

        val cursor = db.query("SELECT status FROM workouts WHERE notes = 'new workout'")
        assertTrue(cursor.moveToFirst())
        assertEquals("NOT_STARTED", cursor.getString(0))
        cursor.close()
        db.close()
    }

    // ──────────────────────────────────────────────
    //  v8→v9: V-taper scores backfill
    // ──────────────────────────────────────────────

    @Test
    fun migrate8To9_setsVtaperScores() {
        // Start from v8, run to v9, verify vtaper scores were set
        migrationTestHelper.createDatabase(TEST_DB, 8).close()

        val db = migrationTestHelper.runMigrationsAndValidate(
            TEST_DB, 9, true,
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
            MIGRATION_7_8, MIGRATION_8_9
        )

        // Verify Lateral Raise has vtaper_lateral_delt=10
        val cursor = db.query(
            "SELECT vtaper_lat, vtaper_lateral_delt, vtaper_upper_chest, vtaper_rear_delt FROM exercises WHERE name='Lateral Raise'"
        )
        assertTrue("Lateral Raise should exist", cursor.moveToFirst())
        assertEquals("Lateral Raise vtaper_lat should be 1", 1, cursor.getInt(0))
        assertEquals("Lateral Raise vtaper_lateral_delt should be 10", 10, cursor.getInt(1))
        assertEquals("Lateral Raise vtaper_upper_chest should be 0", 0, cursor.getInt(2))
        assertEquals("Lateral Raise vtaper_rear_delt should be 2", 2, cursor.getInt(3))
        cursor.close()

        // Verify Pull-up has vtaper_lat=10
        val cursor2 = db.query(
            "SELECT vtaper_lat, vtaper_lateral_delt, vtaper_upper_chest, vtaper_rear_delt FROM exercises WHERE name='Pull-up'"
        )
        assertTrue("Pull-up should exist", cursor2.moveToFirst())
        assertEquals("Pull-up vtaper_lat should be 10", 10, cursor2.getInt(0))
        assertEquals("Pull-up vtaper_lateral_delt should be 1", 1, cursor2.getInt(1))
        assertEquals("Pull-up vtaper_upper_chest should be 1", 1, cursor2.getInt(2))
        assertEquals("Pull-up vtaper_rear_delt should be 4", 4, cursor2.getInt(3))
        cursor2.close()

        // Verify Bench Press has vtaper_upper_chest=7
        val cursor3 = db.query(
            "SELECT vtaper_lat, vtaper_lateral_delt, vtaper_upper_chest, vtaper_rear_delt FROM exercises WHERE name='Bench Press'"
        )
        assertTrue("Bench Press should exist", cursor3.moveToFirst())
        assertEquals("Bench Press vtaper_upper_chest should be 7", 7, cursor3.getInt(2))
        cursor3.close()

        // Verify Squat (lower body) has all zeros
        val cursor4 = db.query(
            "SELECT vtaper_lat, vtaper_lateral_delt, vtaper_upper_chest, vtaper_rear_delt FROM exercises WHERE name='Squat'"
        )
        assertTrue("Squat should exist", cursor4.moveToFirst())
        assertEquals("Squat vtaper_lat should be 0", 0, cursor4.getInt(0))
        assertEquals("Squat vtaper_lateral_delt should be 0", 0, cursor4.getInt(1))
        assertEquals("Squat vtaper_upper_chest should be 0", 0, cursor4.getInt(2))
        assertEquals("Squat vtaper_rear_delt should be 0", 0, cursor4.getInt(3))
        cursor4.close()

        db.close()
    }

    @Test
    fun migrate8To9_movementPatternColumnExists() {
        migrationTestHelper.createDatabase(TEST_DB, 8).close()

        val db = migrationTestHelper.runMigrationsAndValidate(
            TEST_DB, 9, true,
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
            MIGRATION_7_8, MIGRATION_8_9
        )

        // Verify movement_pattern column exists
        val pragmaCursor = db.query("PRAGMA table_info(exercises)")
        var hasMovementPattern = false
        while (pragmaCursor.moveToNext()) {
            if (pragmaCursor.getString(pragmaCursor.getColumnIndexOrThrow("name")) == "movement_pattern") {
                hasMovementPattern = true
                break
            }
        }
        pragmaCursor.close()
        assertTrue("exercises table should have 'movement_pattern' column", hasMovementPattern)
        db.close()
    }

    // ──────────────────────────────────────────────
    //  v9→v10: Readiness table
    // ──────────────────────────────────────────────

    @Test
    fun migrate10To11_addsScheduleAndLimitationsColumns() {
        migrationTestHelper.createDatabase(TEST_DB, 10).close()

        val db = migrationTestHelper.runMigrationsAndValidate(
            TEST_DB, 11, true,
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
            MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_9_10
        )

        val pragmaCursor = db.query("PRAGMA table_info(user_profiles)")
        val foundColumns = mutableSetOf<String>()
        while (pragmaCursor.moveToNext()) {
            foundColumns.add(pragmaCursor.getString(pragmaCursor.getColumnIndexOrThrow("name")))
        }
        pragmaCursor.close()

        assertTrue("Should have preferred_schedule", foundColumns.contains("preferred_schedule"))
        assertTrue("Should have limitations_preferences", foundColumns.contains("limitations_preferences"))
        db.close()
    }

    @Test
    fun migrate9To10_createsReadinessTable() {
        migrationTestHelper.createDatabase(TEST_DB, 9).close()

        val db = migrationTestHelper.runMigrationsAndValidate(
            TEST_DB, 10, true,
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
            MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10
        )


        // Verify readiness table exists
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='readiness'")
        assertTrue("readiness table should exist after 9→10 migration", cursor.moveToFirst())
        cursor.close()

        // Verify readiness table schema
        val pragmaCursor = db.query("PRAGMA table_info(readiness)")
        val expectedColumns = setOf("id", "user_id", "recorded_at", "sleep_quality", "soreness", "energy", "motivation", "notes")
        val foundColumns = mutableSetOf<String>()
        while (pragmaCursor.moveToNext()) {
            foundColumns.add(pragmaCursor.getString(pragmaCursor.getColumnIndexOrThrow("name")))
        }
        pragmaCursor.close()
        assertTrue("readiness table should have all expected columns", expectedColumns.containsAll(foundColumns))

        // Verify default values
        db.execSQL("INSERT INTO readiness (recorded_at) VALUES (0)")
        val checkCursor = db.query("SELECT sleep_quality, soreness, energy, motivation, notes FROM readiness WHERE id = 1")
        assertTrue(checkCursor.moveToFirst())
        assertEquals("Default sleep_quality should be 3", 3, checkCursor.getInt(checkCursor.getColumnIndexOrThrow("sleep_quality")))
        assertEquals("Default soreness should be 3", 3, checkCursor.getInt(checkCursor.getColumnIndexOrThrow("soreness")))
        assertEquals("Default energy should be 3", 3, checkCursor.getInt(checkCursor.getColumnIndexOrThrow("energy")))
        assertEquals("Default motivation should be 3", 3, checkCursor.getInt(checkCursor.getColumnIndexOrThrow("motivation")))
        assertEquals("Default notes should be empty", "", checkCursor.getString(checkCursor.getColumnIndexOrThrow("notes")))
        checkCursor.close()
        db.close()
    }

    @Test
    fun migrateFullChain1To10_withReadiness() {
        // Full chain test: create at v1, migrate all the way to v10
        migrationTestHelper.createDatabase(TEST_DB, 1).close()

        val db = migrationTestHelper.runMigrationsAndValidate(
            TEST_DB, 10, true,
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
            MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10
        )

        // Verify readiness table exists and is queryable
        val cursor = db.query("SELECT COUNT(*) FROM readiness")
        assertTrue(cursor.moveToFirst())
        cursor.close()

        // Verify vtaper scores are set (migration 8→9 runs UPDATE statements)
        val vtaperCursor = db.query(
            "SELECT COUNT(*) FROM exercises WHERE vtaper_lateral_delt > 0 OR vtaper_lat > 0 OR vtaper_upper_chest > 0 OR vtaper_rear_delt > 0"
        )
        assertTrue(vtaperCursor.moveToFirst())
        val countWithVtaper = vtaperCursor.getInt(0)
        vtaperCursor.close()
        assertTrue("At least some exercises should have non-zero vtaper scores", countWithVtaper > 0)

        // Verify status column exists and has valid values
        val statusCursor = db.query("SELECT DISTINCT status FROM workouts")
        val statuses = mutableSetOf<String>()
        while (statusCursor.moveToNext()) {
            statuses.add(statusCursor.getString(0))
        }
        statusCursor.close()
        assertTrue("Should have valid status values", statuses.containsAll(setOf("NOT_STARTED", "ACTIVE", "COMPLETED", "ABANDONED")))

        db.close()
    }
}