package com.gymcoach.app.data.repository

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.gymcoach.app.data.local.database.GymCoachDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RoomDatabaseMigrationTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `migration3to4 adds setType column to workout_sets table`() {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // In-memory database
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `workout_sets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workoutExerciseId` INTEGER NOT NULL, `setNumber` INTEGER NOT NULL, `weight` REAL NOT NULL, `reps` INTEGER NOT NULL, `rpe` REAL NOT NULL, `restSeconds` INTEGER NOT NULL, `completed` INTEGER NOT NULL)"
                    )
                }

                override fun onUpgrade(
                    db: androidx.sqlite.db.SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) {}
            })
            .build()

        val factory = FrameworkSQLiteOpenHelperFactory()
        val openHelper = factory.create(config)
        val db = openHelper.writableDatabase

        // Insert a row into version 3 schema without setType
        db.execSQL(
            "INSERT INTO `workout_sets` (`id`, `workoutExerciseId`, `setNumber`, `weight`, `reps`, `rpe`, `restSeconds`, `completed`) VALUES (1, 10, 1, 100.0, 10, 8.0, 90, 1)"
        )

        // Execute Migration 3 -> 4
        GymCoachDatabase.MIGRATION_3_4.migrate(db)

        // Verify setType column exists with default value 0
        val cursor = db.query("SELECT `setType` FROM `workout_sets` WHERE `id` = 1")
        assertNotNull(cursor)
        assertTrue(cursor.moveToFirst())
        val setTypeIndex = cursor.getColumnIndex("setType")
        assertTrue(setTypeIndex != -1)
        val setType = cursor.getInt(setTypeIndex)
        assertEquals(0, setType)
        cursor.close()

        openHelper.close()
    }
}
