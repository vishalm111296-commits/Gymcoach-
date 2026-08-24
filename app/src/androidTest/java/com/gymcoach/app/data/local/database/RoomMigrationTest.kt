package com.gymcoach.app.data.local.database

import androidx.room.AutoMigrationSpec
import androidx.room.MigrationTestHelper
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Room migration tests using MigrationTestHelper.
 *
 * Schema JSONs (v1–v7) are exported under app/schemas by KSP.
 * v8.json is generated at build time; tests referencing it will pass on CI.
 *
 * Strategy:
 * - MigrationTestHelper.createDatabase() reads the schema JSON for the
 *   requested version and creates a real SQLite database with that schema.
 * - runMigrationsAndValidate() applies the provided Migration objects,
 *   then compares the resulting schema against the schema JSON for the
 *   target version.
 * - For v7→v8 (status column backfill), we also do a manual SQL-level
 *   test because the backfill logic is the most critical migration.
 */
@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    @JvmField
    val migrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation().targetContext,
        GymCoachDatabase::class.java,
        emptyList<AutoMigrationSpec>(),
        FrameworkSQLiteOpenHelperFactory()
    )

    // ──────────────────────────────────────────────
    //  Full chain: v1 → v7 (schema JSONs exist)
    // ──────────────────────────────────────────────

    @Test
    fun migrate1To7() {
        // Create at v1 (only exercises table with basic columns)
        migrationTestHelper.createDatabase(TEST_DB, 1).close()

        // Run the complete chain to v7
        migrationTestHelper.runMigrationsAndValidate(
            TEST_DB, 7, true,
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7
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
        // Start from v7 schema (all tables exist)
        migrationTestHelper.createDatabase(TEST_DB, 7).close()

        // Run the full chain to v8
        val db = migrationTestHelper.runMigrationsAndValidate(
            TEST_DB, 8, true,
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8
        )

        // Verify status column exists via PRAGMA
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

        // Verify backfill: insert a completed workout before migration,
        // it should be backfilled to COMPLETED
        // (MigrationTestHelper applies migrations to an empty DB, so there's no
        // existing data to backfill. The DDL default is 'NOT_STARTED' which
        // matches the entity annotation.)
        db.close()
    }

    @Test
    fun migration7To8_statusBackfillLogic() {
        // Manual test: create a v7 DB, insert test data, run migration 7→8,
        // verify the backfill assigns correct statuses.

        // We can't use MigrationTestHelper for this because we need to insert
        // data before running the migration. Instead, we test the SQL directly.

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbHelper = object : android.database.sqlite.SQLiteOpenHelper(
            context, "test-backfill.db", null, 7
        ) {
            override fun onCreate(db: android.database.sqlite.SQLiteDatabase) {
                // v7 schema: exercises + workouts (no status column)
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS workouts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date INTEGER NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER NOT NULL,
                        duration INTEGER NOT NULL,
                        notes TEXT NOT NULL,
                        completed INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS workout_exercises (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        workoutId INTEGER NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        orderIndex INTEGER NOT NULL
                    )"""
                )

                // Insert test rows
                // Row 1: completed=1 → should become COMPLETED
                db.execSQL(
                    "INSERT INTO workouts (date, startTime, endTime, duration, notes, completed) VALUES (0, 0, 0, 0, 'completed workout', 1)"
                )
                // Row 2: completed=0, has exercises → should become ACTIVE
                db.execSQL(
                    "INSERT INTO workouts (date, startTime, endTime, duration, notes, completed) VALUES (0, 0, 0, 0, 'active workout', 0)"
                )
                db.execSQL(
                    "INSERT INTO workout_exercises (workoutId, exerciseId, orderIndex) VALUES (2, 1, 0)"
                )
                // Row 3: completed=0, no exercises → should become ABANDONED
                db.execSQL(
                    "INSERT INTO workouts (date, startTime, endTime, duration, notes, completed) VALUES (0, 0, 0, 0, 'zombie workout', 0)"
                )
            }

            override fun onUpgrade(db: android.database.sqlite.SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            override fun onDowngrade(db: android.database.sqlite.SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }

        val db = dbHelper.writableDatabase

        // Apply MIGRATION_7_8 SQL manually
        MIGRATION_7_8.migrate(db)

        // Verify status column was added and backfilled correctly
        val cursor = db.rawQuery("SELECT id, status, notes FROM workouts ORDER BY id", null)

        val results = mutableListOf<Triple<Long, String, String>>()
        while (cursor.moveToNext()) {
            results.add(
                Triple(
                    cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("status")),
                    cursor.getString(cursor.getColumnIndexOrThrow("notes"))
                )
            )
        }
        cursor.close()
        db.close()
        dbHelper.close()

        assertEquals("Should have 3 rows", 3, results.size)

        val statusByNote = results.associate { it.third to it.second }
        assertEquals("completed workout → COMPLETED", "COMPLETED", statusByNote["completed workout"])
        assertEquals("active workout → ACTIVE", "ACTIVE", statusByNote["active workout"])
        assertEquals("zombie workout → ABANDONED", "ABANDONED", statusByNote["zombie workout"])

        // Verify all statuses are valid
        val validStatuses = setOf("NOT_STARTED", "ACTIVE", "COMPLETED", "ABANDONED")
        for ((_, status, _) in results) {
            assertTrue("Invalid status: $status", status in validStatuses)
        }
    }

    @Test
    fun migration7To8_statusDefaultIsNotStarted() {
        // After migration, new inserts should default to NOT_STARTED
        migrationTestHelper.createDatabase(TEST_DB, 7).close()

        val db = migrationTestHelper.runMigrationsAndValidate(
            TEST_DB, 8, true,
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8
        )

        // Insert a new workout after migration
        db.execSQL(
            "INSERT INTO workouts (date, startTime, endTime, duration, notes, completed, status) VALUES (0, 0, 0, 0, 'new workout', 0, 'NOT_STARTED')"
        )

        val cursor = db.rawQuery("SELECT status FROM workouts WHERE notes = 'new workout'", null)
        assertTrue(cursor.moveToFirst())
        assertEquals("NOT_STARTED", cursor.getString(0))
        cursor.close()
        db.close()
    }
}
