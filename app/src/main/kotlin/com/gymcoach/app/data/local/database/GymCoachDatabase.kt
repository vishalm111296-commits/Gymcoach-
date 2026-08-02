package com.gymcoach.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutEntity
import com.gymcoach.app.data.local.entity.WorkoutExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutSetEntity

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class GymCoachDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `workouts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` INTEGER NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `duration` INTEGER NOT NULL, `notes` TEXT NOT NULL, `completed` INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `workout_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workoutId` INTEGER NOT NULL, `exerciseId` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `workout_sets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workoutExerciseId` INTEGER NOT NULL, `setNumber` INTEGER NOT NULL, `weight` REAL NOT NULL, `reps` INTEGER NOT NULL, `rpe` REAL NOT NULL, `restSeconds` INTEGER NOT NULL, `completed` INTEGER NOT NULL)")
            }
        }

        fun create(context: Context): GymCoachDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                GymCoachDatabase::class.java,
                "gymcoach.db"
            )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seedExercises(db)
                    }
                })
                .build()
        }

        private fun seedExercises(db: SupportSQLiteDatabase) {
            val seedData = listOf(
                arrayOf<Any>("Bench Press", "Lie on a bench and press the barbell from your chest to full arm extension.", "Chest", "Barbell", "Intermediate"),
                arrayOf<Any>("Squat", "Lower your hips until your thighs are parallel to the floor, then stand back up.", "Legs", "Barbell", "Beginner"),
                arrayOf<Any>("Push-up", "Lower your chest toward the floor while keeping your body in a straight line.", "Chest", "Bodyweight", "Beginner"),
                arrayOf<Any>("Shoulder Press", "Press the dumbbells overhead until your arms are fully extended.", "Shoulders", "Dumbbell", "Intermediate"),
                arrayOf<Any>("Lateral Raise", "Raise dumbbells out to the sides until your arms are parallel with the floor.", "Shoulders", "Dumbbell", "Beginner"),
                arrayOf<Any>("Bent-over Row", "Hinge at the hips and pull the barbell toward your torso.", "Back", "Barbell", "Intermediate"),
                arrayOf<Any>("Plank", "Hold a straight-body position supported on your forearms and toes.", "Core", "Bodyweight", "Beginner"),
                arrayOf<Any>("Deadlift", "Hinge at the hips and lift the loaded barbell to a standing position.", "Back", "Barbell", "Advanced"),
                arrayOf<Any>("Bicep Curl", "Curl the dumbbells toward your shoulders with your elbows pinned to your sides.", "Arms", "Dumbbell", "Beginner")
            )
            seedData.forEach { row ->
                db.execSQL(
                    "INSERT INTO exercises (name, description, muscleGroup, equipment, difficulty) VALUES (?, ?, ?, ?, ?)",
                    row
                )
            }
        }
    }
}