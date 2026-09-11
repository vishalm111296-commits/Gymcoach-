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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
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

    @Rule
    @JvmField
    val migrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GymCoachDatabase::class.java,
        emptyList<AutoMigrationSpec>(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Before
    fun setUp() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(TEST_DB)
    }

    @After
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(TEST_DB)
    }

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
        // Insert an exercise so the ACTIVE backfill condition is satisfied and foreign key exists
        v7Db.execSQL(
            "INSERT INTO exercises (id, name, description, muscleGroup, equipment, difficulty, secondaryMuscles, instructions, tips, commonMistakes, safetyNotes, recommendedRepRange, recommendedRestTime, estimatedCalories, category, tags, isFavorite, lastViewed, vtaper_lat, vtaper_lateral_delt, vtaper_upper_chest, vtaper_rear_delt, movement_pattern, setup_instructions, execution_instructions, breathing_instructions, tempo_guidance) VALUES (1, 'Bench Press', '', 'Chest', 'Barbell', 'Intermediate', '', '', '', '', '', '8-12', '90', 10, 'Strength', '', 0, 0, 0, 0, 0, 0, 'horizontal_push', '', '', '', '')"
        )
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
        val v8Db = migrationTestHelper.createDatabase(TEST_DB, 8)
        v8Db.execSQL("INSERT INTO exercises (name, description, muscleGroup, equipment, difficulty, secondaryMuscles, instructions, tips, commonMistakes, safetyNotes, recommendedRepRange, recommendedRestTime, estimatedCalories, category, tags, isFavorite, lastViewed, vtaper_lat, vtaper_lateral_delt, vtaper_upper_chest, vtaper_rear_delt, movement_pattern, setup_instructions, execution_instructions, breathing_instructions, tempo_guidance) VALUES ('Lateral Raise', '', 'Shoulders', 'Dumbbell', 'Beginner', '', '', '', '', '', '8-12', '60', 10, 'Strength', '', 0, 0, 0, 0, 0, 0, '', '', '', '', '')")
        v8Db.execSQL("INSERT INTO exercises (name, description, muscleGroup, equipment, difficulty, secondaryMuscles, instructions, tips, commonMistakes, safetyNotes, recommendedRepRange, recommendedRestTime, estimatedCalories, category, tags, isFavorite, lastViewed, vtaper_lat, vtaper_lateral_delt, vtaper_upper_chest, vtaper_rear_delt, movement_pattern, setup_instructions, execution_instructions, breathing_instructions, tempo_guidance) VALUES ('Pull-up', '', 'Back', 'Bodyweight', 'Intermediate', '', '', '', '', '', '8-12', '90', 15, 'Strength', '', 0, 0, 0, 0, 0, 0, '', '', '', '', '')")
        v8Db.close()

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
    fun migrateFullChain1To12_withReadinessAndIndices() {
        migrationTestHelper.createDatabase(TEST_DB, 1).close()

        val db = migrationTestHelper.runMigrationsAndValidate(
            TEST_DB, 12, true,
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
            MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
            MIGRATION_10_11, GymCoachDatabase.MIGRATION_11_12
        )

        val cursor = db.query("SELECT COUNT(*) FROM readiness")
        assertTrue(cursor.moveToFirst())
        cursor.close()

        db.close()
    }

    @Test
    fun migrate11To12_addsUniqueIndices() {
        var db = migrationTestHelper.createDatabase(TEST_DB, 11)
        // Pre-populate with duplicate rows to verify deduplication handling
        db.execSQL("INSERT INTO workout_sets (id, workoutExerciseId, setNumber, weight, reps, rpe, restSeconds, completed, setType) VALUES (1, 10, 1, 50.0, 10, 8.0, 90, 1, 0)")
        db.execSQL("INSERT INTO workout_sets (id, workoutExerciseId, setNumber, weight, reps, rpe, restSeconds, completed, setType) VALUES (2, 10, 1, 55.0, 10, 8.0, 90, 1, 0)")
        db.execSQL("INSERT INTO workout_exercises (id, workoutId, exerciseId, orderIndex) VALUES (1, 5, 100, 0)")
        db.execSQL("INSERT INTO workout_exercises (id, workoutId, exerciseId, orderIndex) VALUES (2, 5, 101, 0)")
        db.close()

        db = migrationTestHelper.runMigrationsAndValidate(
            TEST_DB, 12, true,
            GymCoachDatabase.MIGRATION_11_12
        )

        val setIndexCursor = db.query("PRAGMA index_list('workout_sets')")
        var hasSetUniqueIndex = false
        while (setIndexCursor.moveToNext()) {
            val name = setIndexCursor.getString(setIndexCursor.getColumnIndexOrThrow("name"))
            val unique = setIndexCursor.getInt(setIndexCursor.getColumnIndexOrThrow("unique"))
            if (name == "index_workout_sets_workoutExerciseId_setNumber" && unique == 1) {
                hasSetUniqueIndex = true
            }
        }
        setIndexCursor.close()

        val exIndexCursor = db.query("PRAGMA index_list('workout_exercises')")
        var hasExUniqueIndex = false
        while (exIndexCursor.moveToNext()) {
            val name = exIndexCursor.getString(exIndexCursor.getColumnIndexOrThrow("name"))
            val unique = exIndexCursor.getInt(exIndexCursor.getColumnIndexOrThrow("unique"))
            if (name == "index_workout_exercises_workoutId_orderIndex" && unique == 1) {
                hasExUniqueIndex = true
            }
        }
        exIndexCursor.close()

        assertTrue("workout_sets must have unique index on (workoutExerciseId, setNumber)", hasSetUniqueIndex)
        assertTrue("workout_exercises must have unique index on (workoutId, orderIndex)", hasExUniqueIndex)

        // Verify zero data loss: both exercises and both sets are preserved with renumbered non-colliding indices
        val exCountCursor = db.query("SELECT COUNT(*) FROM workout_exercises")
        assertTrue(exCountCursor.moveToFirst())
        assertEquals("Both workout_exercises must be preserved without cascade deletion", 2, exCountCursor.getInt(0))
        exCountCursor.close()

        val setCountCursor = db.query("SELECT COUNT(*) FROM workout_sets")
        assertTrue(setCountCursor.moveToFirst())
        assertEquals("Both workout_sets must be preserved", 2, setCountCursor.getInt(0))
        setCountCursor.close()

        db.close()
    }

    @Test
    fun migrate11To12_adversarialDuplicatesAcrossWorkouts_preservesAllDataDeterministically() {
        var db = migrationTestHelper.createDatabase(TEST_DB, 11)

        // Workout 1: 3 exercises with colliding orderIndices
        // Exercise 101: orderIndex = 0 (colliding)
        db.execSQL("INSERT INTO workout_exercises (id, workoutId, exerciseId, orderIndex) VALUES (101, 1, 10, 0)")
        // Exercise 102: orderIndex = 0 (colliding)
        db.execSQL("INSERT INTO workout_exercises (id, workoutId, exerciseId, orderIndex) VALUES (102, 1, 20, 0)")
        // Exercise 103: orderIndex = 1
        db.execSQL("INSERT INTO workout_exercises (id, workoutId, exerciseId, orderIndex) VALUES (103, 1, 30, 1)")

        // Workout 2: 2 exercises with colliding orderIndices
        // Exercise 201: orderIndex = 0 (colliding)
        db.execSQL("INSERT INTO workout_exercises (id, workoutId, exerciseId, orderIndex) VALUES (201, 2, 40, 0)")
        // Exercise 202: orderIndex = 0 (colliding)
        db.execSQL("INSERT INTO workout_exercises (id, workoutId, exerciseId, orderIndex) VALUES (202, 2, 50, 0)")

        // Exercise 101 child sets: colliding setNumbers (1, 1, 2)
        db.execSQL("INSERT INTO workout_sets (id, workoutExerciseId, setNumber, weight, reps, rpe, restSeconds, completed, setType) VALUES (1, 101, 1, 60.0, 8, 8.0, 90, 1, 0)")
        db.execSQL("INSERT INTO workout_sets (id, workoutExerciseId, setNumber, weight, reps, rpe, restSeconds, completed, setType) VALUES (2, 101, 1, 62.5, 8, 8.5, 90, 1, 0)")
        db.execSQL("INSERT INTO workout_sets (id, workoutExerciseId, setNumber, weight, reps, rpe, restSeconds, completed, setType) VALUES (3, 101, 2, 65.0, 7, 9.0, 90, 1, 0)")

        // Exercise 102 child sets: colliding setNumbers (1, 1, 1)
        db.execSQL("INSERT INTO workout_sets (id, workoutExerciseId, setNumber, weight, reps, rpe, restSeconds, completed, setType) VALUES (4, 102, 1, 80.0, 5, 8.0, 120, 1, 0)")
        db.execSQL("INSERT INTO workout_sets (id, workoutExerciseId, setNumber, weight, reps, rpe, restSeconds, completed, setType) VALUES (5, 102, 1, 80.0, 5, 8.5, 120, 1, 0)")
        db.execSQL("INSERT INTO workout_sets (id, workoutExerciseId, setNumber, weight, reps, rpe, restSeconds, completed, setType) VALUES (6, 102, 1, 80.0, 4, 9.5, 120, 1, 0)")

        // Exercise 201 child sets: colliding setNumbers (1, 2, 2)
        db.execSQL("INSERT INTO workout_sets (id, workoutExerciseId, setNumber, weight, reps, rpe, restSeconds, completed, setType) VALUES (7, 201, 1, 100.0, 5, 8.0, 180, 1, 0)")
        db.execSQL("INSERT INTO workout_sets (id, workoutExerciseId, setNumber, weight, reps, rpe, restSeconds, completed, setType) VALUES (8, 201, 2, 105.0, 4, 9.0, 180, 1, 0)")
        db.execSQL("INSERT INTO workout_sets (id, workoutExerciseId, setNumber, weight, reps, rpe, restSeconds, completed, setType) VALUES (9, 201, 2, 105.0, 3, 10.0, 180, 1, 0)")

        db.close()

        db = migrationTestHelper.runMigrationsAndValidate(
            TEST_DB, 12, true,
            GymCoachDatabase.MIGRATION_11_12
        )

        // 1. Verify 100% preservation of all 5 exercises
        val exCountCursor = db.query("SELECT COUNT(*) FROM workout_exercises")
        assertTrue(exCountCursor.moveToFirst())
        assertEquals("All 5 workout_exercises must survive migration", 5, exCountCursor.getInt(0))
        exCountCursor.close()

        // 2. Verify 100% preservation of all 9 sets
        val setCountCursor = db.query("SELECT COUNT(*) FROM workout_sets")
        assertTrue(setCountCursor.moveToFirst())
        assertEquals("All 9 workout_sets must survive migration", 9, setCountCursor.getInt(0))
        setCountCursor.close()

        // 3. Verify deterministic, sequential orderIndex for Workout 1 (exercises 101, 102, 103)
        val w1ExCursor = db.query("SELECT id, orderIndex FROM workout_exercises WHERE workoutId = 1 ORDER BY orderIndex ASC")
        val w1Orders = mutableListOf<Pair<Long, Int>>()
        while (w1ExCursor.moveToNext()) {
            w1Orders.add(Pair(w1ExCursor.getLong(0), w1ExCursor.getInt(1)))
        }
        w1ExCursor.close()
        assertEquals(3, w1Orders.size)
        assertEquals(listOf(101L to 0, 102L to 1, 103L to 2), w1Orders)

        // 4. Verify deterministic, sequential orderIndex for Workout 2 (exercises 201, 202)
        val w2ExCursor = db.query("SELECT id, orderIndex FROM workout_exercises WHERE workoutId = 2 ORDER BY orderIndex ASC")
        val w2Orders = mutableListOf<Pair<Long, Int>>()
        while (w2ExCursor.moveToNext()) {
            w2Orders.add(Pair(w2ExCursor.getLong(0), w2ExCursor.getInt(1)))
        }
        w2ExCursor.close()
        assertEquals(2, w2Orders.size)
        assertEquals(listOf(201L to 0, 202L to 1), w2Orders)

        // 5. Verify deterministic, sequential setNumber for Exercise 101 (sets 1, 2, 3)
        val ex101SetsCursor = db.query("SELECT id, setNumber, weight FROM workout_sets WHERE workoutExerciseId = 101 ORDER BY setNumber ASC")
        val ex101Sets = mutableListOf<Triple<Long, Int, Double>>()
        while (ex101SetsCursor.moveToNext()) {
            ex101Sets.add(Triple(ex101SetsCursor.getLong(0), ex101SetsCursor.getInt(1), ex101SetsCursor.getDouble(2)))
        }
        ex101SetsCursor.close()
        assertEquals(3, ex101Sets.size)
        assertEquals(listOf(Triple(1L, 1, 60.0), Triple(2L, 2, 62.5), Triple(3L, 3, 65.0)), ex101Sets)

        // 6. Verify deterministic, sequential setNumber for Exercise 102 (sets 4, 5, 6)
        val ex102SetsCursor = db.query("SELECT id, setNumber, reps FROM workout_sets WHERE workoutExerciseId = 102 ORDER BY setNumber ASC")
        val ex102Sets = mutableListOf<Triple<Long, Int, Int>>()
        while (ex102SetsCursor.moveToNext()) {
            ex102Sets.add(Triple(ex102SetsCursor.getLong(0), ex102SetsCursor.getInt(1), ex102SetsCursor.getInt(2)))
        }
        ex102SetsCursor.close()
        assertEquals(3, ex102Sets.size)
        assertEquals(listOf(Triple(4L, 1, 5), Triple(5L, 2, 5), Triple(6L, 3, 4)), ex102Sets)

        // 7. Verify deterministic, sequential setNumber for Exercise 201 (sets 7, 8, 9)
        val ex201SetsCursor = db.query("SELECT id, setNumber, weight FROM workout_sets WHERE workoutExerciseId = 201 ORDER BY setNumber ASC")
        val ex201Sets = mutableListOf<Triple<Long, Int, Double>>()
        while (ex201SetsCursor.moveToNext()) {
            ex201Sets.add(Triple(ex201SetsCursor.getLong(0), ex201SetsCursor.getInt(1), ex201SetsCursor.getDouble(2)))
        }
        ex201SetsCursor.close()
        assertEquals(3, ex201Sets.size)
        assertEquals(listOf(Triple(7L, 1, 100.0), Triple(8L, 2, 105.0), Triple(9L, 3, 105.0)), ex201Sets)

        db.close()
    }
}
