package com.gymcoach.app.data.local.database

import androidx.room.migration.AutoMigrationSpec
import androidx.room.testing.MigrationTestHelper
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
import com.gymcoach.app.data.local.database.GymCoachDatabase.Companion.MIGRATION_10_11
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith

/**
 * Room migration tests using MigrationTestHelper.
 *
 * Schema JSONs (v1–v11) are exported under app/schemas by KSP.
 *
 * Strategy:
 * - MigrationTestHelper.createDatabase() reads the schema JSON for the
 *   requested version and creates a real SQLite database with that schema.
 * - runMigrationsAndValidate() applies the provided Migration objects,
 *   then compares the resulting schema against the schema JSON for the
 *   target version.
 * - The backfill test (7→8) inserts real rows BEFORE migration so the
 *   backfill UPDATE has data to operate on (F-DB-4 fix).
 */
@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    @JvmField
    val migrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GymCoachDatabase::class.java,
        emptyList<AutoMigrationSpec>(),
        FrameworkSQLiteOpenHelperFactory()
    )

    // ──────────────────────────────────────────────
    //  F-DB-1 fix: full chain v1 → v11
    // ──────────────────────────────────────────────

    @Test
    fun migrateFullChain1To11() {
        // Create at v1 and run every migration to the current version v11.
        migrationTestHelper.createDatabase(TEST_DB, 1).close()

        val db = migrationTestHelper.runMigrationsAndValidate(
            TEST_DB, 11, true,
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
            MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11
        )

        // Spot-check: preferred_schedule column present (added in v10→v11)
        val pragmaCursor = db.query("PRAGMA table_info(user_profiles)")
        val columns = mutableSetOf<String>()
        while (pragmaCursor.moveToNext()) {
            columns.add(pragmaCursor.getString(pragmaCursor.getColumnIndexOrThrow("name")))
        }
        pragmaCursor.close()
        assertTrue("preferred_schedule must exist after full chain migration", columns.contains("preferred_schedule"))
        assertTrue("limitations_preferences must exist", columns.contains("limitations_preferences"))

        // Spot-check: readiness table from v9→v10
        val readinessCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='readiness'")
        assertTrue("readiness table must exist after full chain", readinessCursor.moveToFirst())
        readinessCursor.close()

        db.close()
    }

    // ──────────────────────────────────────────────
    //  Full chain: v1 → v10 (schema JSONs exist)
    // ──────────────────────────────────────────────

    @Test
    fun migrate1To10() {
        migrationTestHelper.createDatabase(TEST_DB, 1).close()

        migrationTestHelper.runMigrationsAndValidate(
            TEST_DB, 10, true,
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
            MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10
        ).close()
    }

    @Test
    fun migrate1To9() {
        migrationTestHelper.createDatabase(TEST_DB, 1).close()

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
        migrationTestHelper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2).close()
    }

    @Test
    fun migrate2To3() {
        migrationTestHelper.createDatabase(TEST_DB, 2).close()
        migrationTestHelper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3).close()
    }

    @Test
    fun migrate3To4() {
        migrationTestHelper.createDatabase(TEST_DB, 3).close()
        migrationTestHelper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4).close()
    }

    @Test
    fun migrate6To7() {
        migrationTestHelper.createDatabase(TEST_DB, 6).close()

        val db = migrationTestHelper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7)
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='exercise_fts'")
        assertTrue("exercise_fts table should exist after 6→7 migration", cursor.moveToFirst())
        cursor.close()
        db.close()
    }

    // ──────────────────────────────────────────────
    //  v7→v8: status column + backfill
    // ──────────────────────────────────────────────

    @Test
    fun migrate7To8_addsStatusColumn() {
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

    /**
     * F-DB-4 fix: the original test ran the backfill against an empty workouts
     * table, then asserted `results.any { it.second == "ACTIVE" }` — which
     * trivially passed because an empty list satisfies no predicate and
     * `any` returns false, but the whole assert block would fail.
     *
     * More importantly, the test never actually exercised the backfill UPDATE
     * logic because there were no rows to update.
     *
     * Fix: insert representative rows in the v7 state (before the migration),
     * then assert each row received the correct backfilled status value.
     */
    @Test
    fun migration7To8_statusBackfillWithRealRows() {
        // Create at v7 and insert rows before migration runs
        val v7Db = migrationTestHelper.createDatabase(TEST_DB, 7)

        // Completed workout — should become COMPLETED
        v7Db.execSQL(
            "INSERT INTO workouts (date, startTime, endTime, duration, notes, completed) VALUES (1000, 1000, 2000, 1000, 'completed', 1)"
        )
        // Incomplete workout with exercises — should become ACTIVE
        v7Db.execSQL(
            "INSERT INTO workouts (date, startTime, endTime, duration, notes, completed) VALUES (2000, 2000, 3000, 1000, 'active', 0)"
        )
        val activeWorkoutId = v7Db.query("SELECT id FROM workouts WHERE notes='active'").let {
            it.moveToFirst(); it.getLong(0).also { _ -> it.close() }
        }
        // Insert an exercise so the ACTIVE backfill condition is satisfied
        v7Db.execSQL(
            "INSERT INTO workout_exercises (workoutId, exerciseId, orderIndex) VALUES ($activeWorkoutId, 1, 0)"
        )
        // Incomplete workout with no exercises — should become ABANDONED
        v7Db.execSQL(
            "INSERT INTO workouts (date, startTime, endTime, duration, notes, completed) VALUES (3000, 3000, 4000, 1000, 'abandoned', 0)"
        )
        v7Db.close()

        // Run migration and validate resulting status values
        val db = migrationTestHelper.runMigrationsAndValidate(
            TEST_DB, 8, true, MIGRATION_7_8
        )

        fun statusFor(notes: String): String {
            val c = db.query("SELECT status FROM workouts WHERE notes='$notes'")
            assertTrue("Row '$notes' should exist", c.moveToFirst())
            return c.getString(0).also { c.close() }
        }

        assertEquals("Completed row should be COMPLETED", "COMPLETED", statusFor("completed"))
        assertEquals("Active row (has exercises) should be ACTIVE", "ACTIVE", statusFor("active"))
        assertEquals("Abandoned row (no exercises) should be ABANDONED", "ABANDONED", statusFor("abandoned"))

        db.close()
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

        val cursor = db.query("SELECT status FROM workouts WHERE notes = 'new workout'", emptyArray())
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
        migrationTestHelper.createDatabase(TEST_DB, 8).close()

        val db = migrationTestHelper.runMigrationsAndValidate(
            TEST_DB, 9, true,
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
            MIGRATION_7_8, MIGRATION_8_9
        )

        val cursor = db.query(
            "SELECT vtaper_lat, vtaper_lateral_delt, vtaper_upper_chest, vtaper_rear_delt FROM exercises WHERE name='Lateral Raise'"
        )
        assertTrue("Lateral Raise should exist", cursor.moveToFirst())
        assertEquals("Lateral Raise vtaper_lat should be 1", 1, cursor.getInt(0))
        assertEquals("Lateral Raise vtaper_lateral_delt should be 10", 10, cursor.getInt(1))
        assertEquals("Lateral Raise vtaper_upper_chest should be 0", 0, cursor.getInt(2))
        assertEquals("Lateral Raise vtaper_rear_delt should be 2", 2, cursor.getInt(3))
        cursor.close()

        val cursor2 = db.query(
            "SELECT vtaper_lat, vtaper_lateral_delt, vtaper_upper_chest, vtaper_rear_delt FROM exercises WHERE name='Pull-up'"
        )
        assertTrue("Pull-up should exist", cursor2.moveToFirst())
        assertEquals("Pull-up vtaper_lat should be 10", 10, cursor2.getInt(0))
        cursor2.close()

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
            MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11
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

        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='readiness'")
        assertTrue("readiness table should exist after 9→10 migration", cursor.moveToFirst())
        cursor.close()

        val pragmaCursor = db.query("PRAGMA table_info(readiness)")
        val expectedColumns = setOf("id", "user_id", "recorded_at", "sleep_quality", "soreness", "energy", "motivation", "notes")
        val foundColumns = mutableSetOf<String>()
        while (pragmaCursor.moveToNext()) {
            foundColumns.add(pragmaCursor.getString(pragmaCursor.getColumnIndexOrThrow("name")))
        }
        pragmaCursor.close()
        assertTrue("readiness table should have all expected columns", expectedColumns.containsAll(foundColumns))

        db.execSQL("INSERT INTO readiness (recorded_at) VALUES (0)")
        val checkCursor = db.query("SELECT sleep_quality, soreness, energy, motivation, notes FROM readiness WHERE id = 1", emptyArray())
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
        migrationTestHelper.createDatabase(TEST_DB, 1).close()

        val db = migrationTestHelper.runMigrationsAndValidate(
            TEST_DB, 10, true,
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
            MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10
        )

        val cursor = db.query("SELECT COUNT(*) FROM readiness")
        assertTrue(cursor.moveToFirst())
        cursor.close()

        db.close()
    }
}
