package com.gymcoach.app.data.local.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.gymcoach.app.test.ArchAwareRobolectricTestRunner
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(ArchAwareRobolectricTestRunner::class)
class DatabaseMigrationTest {

    private val TEST_DB_NAME = "migration-test"

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DB_NAME)
    }

    private fun createV3Database(): SupportSQLiteDatabase {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(TEST_DB_NAME)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `workouts` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `date` INTEGER NOT NULL,
                            `startTime` INTEGER NOT NULL,
                            `endTime` INTEGER NOT NULL,
                            `duration` INTEGER NOT NULL,
                            `notes` TEXT NOT NULL,
                            `completed` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `exercises` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `description` TEXT NOT NULL,
                            `muscleGroup` TEXT NOT NULL,
                            `equipment` TEXT NOT NULL,
                            `difficulty` TEXT NOT NULL,
                            `secondaryMuscles` TEXT NOT NULL DEFAULT '',
                            `instructions` TEXT NOT NULL DEFAULT '',
                            `tips` TEXT NOT NULL DEFAULT '',
                            `commonMistakes` TEXT NOT NULL DEFAULT '',
                            `safetyNotes` TEXT NOT NULL DEFAULT '',
                            `recommendedRepRange` TEXT NOT NULL DEFAULT '',
                            `recommendedRestTime` TEXT NOT NULL DEFAULT '',
                            `estimatedCalories` INTEGER NOT NULL DEFAULT 0,
                            `category` TEXT NOT NULL DEFAULT '',
                            `tags` TEXT NOT NULL DEFAULT '',
                            `isFavorite` INTEGER NOT NULL DEFAULT 0,
                            `lastViewed` INTEGER NOT NULL DEFAULT 0
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `workout_exercises` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `workoutId` INTEGER NOT NULL,
                            `exerciseId` INTEGER NOT NULL,
                            `orderIndex` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `workout_sets` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `workoutExerciseId` INTEGER NOT NULL,
                            `setNumber` INTEGER NOT NULL,
                            `weight` REAL NOT NULL,
                            `reps` INTEGER NOT NULL,
                            `rpe` REAL NOT NULL,
                            `restSeconds` INTEGER NOT NULL,
                            `completed` INTEGER NOT NULL,
                            `setType` INTEGER NOT NULL DEFAULT 0
                        )
                        """.trimIndent()
                    )
                    db.version = 3
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        return factory.create(config).writableDatabase
    }

    private fun tableNames(db: SupportSQLiteDatabase): Set<String> {
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table'")
        val result = mutableSetOf<String>()
        while (cursor.moveToNext()) {
            result.add(cursor.getString(0))
        }
        cursor.close()
        return result
    }

    private fun tableColumns(db: SupportSQLiteDatabase, table: String): Set<String> {
        val cursor = db.query("PRAGMA table_info('$table')")
        val result = mutableSetOf<String>()
        while (cursor.moveToNext()) {
            result.add(cursor.getString(1))
        }
        cursor.close()
        return result
    }

    @Test
    fun migration3to4_createsUserProfilesAndMeasurementRecordsTables() {
        createV3Database().use { db ->
            val v3Tables = tableNames(db)
            assertFalse("v3 should not have user_profile", v3Tables.contains("user_profile"))
            assertFalse("v3 should not have measurement_records", v3Tables.contains("measurement_records"))
            assertTrue("v3 should have workouts", v3Tables.contains("workouts"))
            assertTrue("v3 should have exercises", v3Tables.contains("exercises"))
            assertTrue("v3 should have workout_exercises", v3Tables.contains("workout_exercises"))
            assertTrue("v3 should have workout_sets", v3Tables.contains("workout_sets"))

            GymCoachDatabase.MIGRATION_3_4.migrate(db)
            val tables = tableNames(db)

            assertTrue("user_profile table should exist after migration 3->4", tables.contains("user_profile"))
            assertTrue("measurement_records table should exist after migration 3->4", tables.contains("measurement_records"))
            assertTrue("workouts table should still exist", tables.contains("workouts"))
            assertTrue("exercises table should still exist", tables.contains("exercises"))

            val profileColumns = tableColumns(db, "user_profile")
            assertTrue("user_profile should have id column", profileColumns.contains("id"))
            assertTrue("user_profile should have name column", profileColumns.contains("name"))
            assertTrue("user_profile should have weight column", profileColumns.contains("weight"))
            assertTrue("user_profile should have proteinGoal column", profileColumns.contains("proteinGoal"))
            assertTrue("user_profile should have caloriesGoal column", profileColumns.contains("caloriesGoal"))

            val measurementColumns = tableColumns(db, "measurement_records")
            assertTrue("measurement_records should have id column", measurementColumns.contains("id"))
            assertTrue("measurement_records should have userId column", measurementColumns.contains("userId"))
            assertTrue("measurement_records should have measurementType column", measurementColumns.contains("measurementType"))
            assertTrue("measurement_records should have value column", measurementColumns.contains("value"))
            assertTrue("measurement_records should have date column", measurementColumns.contains("date"))
        }
    }

    @Test
    fun migration4to5_addsMoodEnergyPainAndIsTemplateColumns() {
        createV3Database().use { db ->
            GymCoachDatabase.MIGRATION_3_4.migrate(db)

            val beforeColumns = tableColumns(db, "workouts")
            assertFalse("v4 workouts should not have mood column", beforeColumns.contains("mood"))
            assertFalse("v4 workouts should not have energy column", beforeColumns.contains("energy"))
            assertFalse("v4 workouts should not have pain column", beforeColumns.contains("pain"))
            assertFalse("v4 workouts should not have isTemplate column", beforeColumns.contains("isTemplate"))

            GymCoachDatabase.MIGRATION_4_5.migrate(db)
            val columns = tableColumns(db, "workouts")

            assertTrue("workouts should have mood column after migration 4->5", columns.contains("mood"))
            assertTrue("workouts should have energy column after migration 4->5", columns.contains("energy"))
            assertTrue("workouts should have pain column after migration 4->5", columns.contains("pain"))
            assertTrue("workouts should have isTemplate column after migration 4->5", columns.contains("isTemplate"))

            val tables = tableNames(db)
            assertTrue("user_profile should still exist after migration 4->5", tables.contains("user_profile"))
            assertTrue("measurement_records should still exist after migration 4->5", tables.contains("measurement_records"))
        }
    }

    @Test
    fun migration3to5_fullChain_createsAllTablesAndColumns() {
        createV3Database().use { db ->
            GymCoachDatabase.MIGRATION_3_4.migrate(db)
            GymCoachDatabase.MIGRATION_4_5.migrate(db)

            val tables = tableNames(db)
            assertTrue("exercises should exist", tables.contains("exercises"))
            assertTrue("workouts should exist", tables.contains("workouts"))
            assertTrue("workout_exercises should exist", tables.contains("workout_exercises"))
            assertTrue("workout_sets should exist", tables.contains("workout_sets"))
            assertTrue("user_profile should exist", tables.contains("user_profile"))
            assertTrue("measurement_records should exist", tables.contains("measurement_records"))

            val workoutColumns = tableColumns(db, "workouts")
            assertTrue("workouts should have mood", workoutColumns.contains("mood"))
            assertTrue("workouts should have energy", workoutColumns.contains("energy"))
            assertTrue("workouts should have pain", workoutColumns.contains("pain"))
            assertTrue("workouts should have isTemplate", workoutColumns.contains("isTemplate"))
        }
    }

    @Test
    fun migration5to6_doesNotCrash() {
        createV3Database().use { db ->
            GymCoachDatabase.MIGRATION_3_4.migrate(db)
            GymCoachDatabase.MIGRATION_4_5.migrate(db)
            GymCoachDatabase.MIGRATION_5_6.migrate(db)
            val tables = tableNames(db)
            assertTrue("exercises should exist", tables.contains("exercises"))
        }
    }

    @Test
    fun migration6to7_addsEquipmentColumnToUserProfile() {
        createV3Database().use { db ->
            GymCoachDatabase.MIGRATION_3_4.migrate(db)
            GymCoachDatabase.MIGRATION_4_5.migrate(db)
            GymCoachDatabase.MIGRATION_5_6.migrate(db)

            val beforeColumns = tableColumns(db, "user_profile")
            assertFalse("v6 user_profile should not have equipment column", beforeColumns.contains("equipment"))

            GymCoachDatabase.MIGRATION_6_7.migrate(db)
            val columns = tableColumns(db, "user_profile")

            assertTrue("user_profile should have equipment column after migration 6->7", columns.contains("equipment"))
        }
    }
}
