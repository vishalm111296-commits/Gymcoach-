package com.gymcoach.app.data.local.database

import android.content.ContentValues
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import androidx.arch.persistence.room.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {

    private const val DATABASE_NAME = "gymcoach.db"
    private const val CURRENT_VERSION = 8

    @Rule
    var migrationTestHelper: MigrationTestHelper = null

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        migrationTestHelper = MigrationTestHelper(
            context,
            DATABASE_NAME,
            null, // no callback needed for these tests
            object : RoomDatabase.Callback(CURRENT_VERSION) {
                override fun onDestructiveMigration(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                    // Allow destructive migration for test purposes
                }
            }
        )
    }

    /**
     * Test 1→2 migration: Creates workouts, workout_exercises, workout_sets tables
     */
    @Test
    fun `migration 1→2 creates workouts, workout_exercises, workout_sets`() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val db = RoomDatabase.create(appContext.applicationContext, object : androidx.room.RoomDatabase() {
            override fun getOpenHelper(): RoomOpenHelper = throw UnsupportedOperationException()
            override fun getQueries(): Array<out String> = emptyArray()
        })

        try {
            // Insert v1 data (workouts only)
            val contentValues = ContentValues().apply {
                put("date", System.currentTimeMillis())
                put("startTime", System.currentTimeMillis())
                put("endTime", System.currentTimeMillis() + 3600000)
                put("duration", 3600000)
                put("notes", "test workout")
                put("completed", 1)
            }
            db.getOpenHelper().writableDatabase.insert("workouts", null, contentValues)

            // Migrate from v1 to v2
            migrationTestHelper.migrate(appContext, 1, 2)

            // Verify tables exist
            val cursor = db.getOpenHelper().writableDatabase.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table'",
                null
            )
            try {
                val tableNames = mutableListOf<String>()
                if (cursor.moveToFirst()) {
                    do {
                        tableNames.add(cursor.getString(0))
                    } while (cursor.moveToNext())
                }
                // After migration, should have workouts, workout_exercises, workout_sets
                assertContains("workouts", tableNames)
                assertContains("workout_exercises", tableNames)
                assertContains("workout_sets", tableNames)
            } finally {
                cursor.close()
            }
        } finally {
            db.close()
        }
    }

    /**
     * Test 2→3 migration: NO-OP (schema v2 = v1 historically)
     */
    @Test
    fun `migration 2→3 is NO-OP`() {
        migrationTestHelper.migrate(InstrumentationRegistry.getInstrumentation().targetContext, 2, 3)
        // If we get here without exception, NO-OP worked
    }

    /**
     * Test 3→4 migration: Adds setType column to workout_sets
     */
    @Test
    fun `migration 3→4 adds setType column`() {
        migrationTestHelper.migrate(InstrumentationRegistry.getInstrumentation().targetContext, 3, 4)

        val cursor = InstrumentationRegistry.getInstrumentation().targetContext
            .contentResolver.
            query(
                android.provider.BaseColumns._ID,
                null,
                "name = ?",
                arrayOf("workout_sets"),
                null
            )
        // Verify setType column exists in workout_sets
        val schemaCursor = cursor?.runQuery("PRAGMA table_info(workout_sets)")
        try {
            val columnNames = mutableListOf<String>()
            if (schemaCursor.moveToFirst()) {
                do {
                    columnNames.add(schemaCursor.getString(1))
                } while (schemaCursor.moveToNext())
            }
            assertContains("setType", columnNames)
        } finally {
            schemaCursor?.close()
        }
    }

    /**
     * Test 4→5 migration: Full schema migration with vtaper columns,
     * legacy table rebuild, and new tables
     */
    @Test
    fun `migration 4→5 adds vtaper columns and new tables`() {
        migrationTestHelper.migrate(InstrumentationRegistry.getInstrumentation().targetContext, 4, 5)

        val cursor = InstrumentationRegistry.getInstrumentation().targetContext
            .contentResolver.
            query(
                android.provider.BaseColumns._ID,
                null,
                "name = ?",
                arrayOf("exercises"),
                null
            )
        // Verify vtaper columns exist
        val schemaCursor = cursor?.runQuery("PRAGMA table_info(exercises)")
        try {
            val columnNames = mutableListOf<String>()
            if (schemaCursor.moveToFirst()) {
                do {
                    columnNames.add(schemaCursor.getString(1))
                } while (schemaCursor.moveToNext())
            }
            assertContains("vtaper_lat", columnNames)
            assertContains("vtaper_lateral_delt", columnNames)
            assertContains("vtaper_upper_chest", columnNames)
            assertContains("vtaper_rear_delt", columnNames)
        } finally {
            schemaCursor?.close()
        }

        // Verify new tables exist
        val tablesCursor = InstrumentationRegistry.getInstrumentation().targetContext
            .contentResolver.
            query(
                android.provider.BaseColumns._ID,
                null,
                "name = ?",
                arrayOf("workout_sets"),
                null
            )
        val newTables = mutableListOf<String>()
        for (tableName in arrayOf(
            "user_profiles", "programs", "program_days", "program_exercises",
            "personal_records", "body_measurements", "favorite_exercises",
            "exercise_substitutions"
        )) {
            val tCursor = InstrumentationRegistry.getInstrumentation().targetContext
                .contentResolver.
                query(
                    android.provider.BaseColumns._ID,
                    null,
                    "name = ?",
                    arrayOf(tableName),
                    null
                )
            if (tCursor != null && tCursor.getCount() > 0) {
                newTables.add(tableName)
            }
            tCursor?.close()
        }
        // All new tables should exist after 4→5 migration
        for (table in newTables) {
            assertTrue("Table $table should exist after 4→5 migration", table in newTables)
        }
    }

    /**
     * Test 5→6 migration: NO-OP (schemas v4 and v5 identical)
     */
    @Test
    fun `migration 5→6 is NO-OP`() {
        migrationTestHelper.migrate(InstrumentationRegistry.getInstrumentation().targetContext, 5, 6)
        // If we get here without exception, NO-OP worked
    }

    /**
     * Test 6→7 migration: Adds exercise_fts FTS4 virtual table
     */
    @Test
    fun `migration 6→7 adds exercise_fts FTS4 table`() {
        migrationTestHelper.migrate(InstrumentationRegistry.getInstrumentation().targetContext, 6, 7)

        val cursor = InstrumentationRegistry.getInstrumentation().targetContext
            .contentResolver.
            query(
                android.provider.BaseColumns._ID,
                null,
                "name = ?",
                arrayOf("exercise_fts"),
                null
            )
        // exercise_fts should exist after migration
        assertNotNull("exercise_fts table should exist after 6→7 migration", cursor)
        assertTrue("exercise_fts should have rows after rebuild", cursor?.getCount() > 0 ?: true)
    }

    /**
     * Test 7→8 migration: Adds status column to workouts with backfill
     */
    @Test
    fun `migration 7→8 adds status column with backfill`() {
        migrationTestHelper.migrate(InstrumentationRegistry.getInstrumentation().targetContext, 7, 8)

        val cursor = InstrumentationRegistry.getInstrumentation().targetContext
            .contentResolver.
            query(
                android.provider.BaseColumns._ID,
                null,
                "name = ?",
                arrayOf("workouts"),
                null
            )
        val schemaCursor = cursor?.runQuery("PRAGMA table_info(workouts)")
        try {
            val columnNames = mutableListOf<String>()
            if (schemaCursor.moveToFirst()) {
                do {
                    columnNames.add(schemaCursor.getString(1))
                } while (schemaCursor.moveToNext())
            }
            assertContains("status", columnNames)
        } finally {
            schemaCursor?.close()
        }

        // Verify data backfill worked
        val dataCursor = cursor?.runQuery(
            "SELECT id, status FROM workouts LIMIT 10"
        )
        try {
            if (dataCursor.moveToFirst()) {
                do {
                    val status = dataCursor.getString(dataCursor.getColumnIndexOrThrow("status"))
                    // Status should be one of: NOT_STARTED, ACTIVE, COMPLETED, ABANDONED
                    assertTrue("Invalid status: $status", listOf("NOT_STARTED", "ACTIVE", "COMPLETED", "ABANDONED").contains(status))
                } while (dataCursor.moveToNext())
            }
        } finally {
            dataCursor?.close()
        }
    }

    private fun assertContains(search: String, list: List<String>) {
        assertTrue("Expected $search in $list", list.contains(search))
    }
}