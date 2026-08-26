package com.gymcoach.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gymcoach.app.data.local.entity.*
import com.gymcoach.app.data.local.dao.*


@Database(
    entities = [
        ExerciseEntity::class,
        ExerciseFtsEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class,
        BodyMeasurementEntity::class,
        EquipmentEntity::class,
        ExerciseAliasEntity::class,
        ExerciseEquipmentEntity::class,
        ExerciseMuscleEntity::class,
        ExerciseSubstitutionEntity::class,
        FavoriteExerciseEntity::class,
        MuscleEntity::class,
        PersonalRecordEntity::class,
        ProgramDayEntity::class,
        ProgramEntity::class,
        ProgramExerciseEntity::class,
        UserProfileEntity::class,
        ReadinessEntity::class
    ],
    version = 11,
    exportSchema = true
)
abstract class GymCoachDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun programDao(): ProgramDao
    abstract fun programDayDao(): ProgramDayDao
    abstract fun programExerciseDao(): ProgramExerciseDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun personalRecordDao(): PersonalRecordDao
    abstract fun bodyMeasurementDao(): BodyMeasurementDao
    abstract fun favoriteExerciseDao(): FavoriteExerciseDao
    abstract fun exerciseSubstitutionDao(): ExerciseSubstitutionDao
    abstract fun muscleDao(): MuscleDao
    abstract fun equipmentDao(): EquipmentDao
    abstract fun exerciseMuscleDao(): ExerciseMuscleDao
    abstract fun exerciseEquipmentDao(): ExerciseEquipmentDao
    abstract fun exerciseAliasDao(): ExerciseAliasDao
    abstract fun readinessDao(): ReadinessDao

    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `workouts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` INTEGER NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `duration` INTEGER NOT NULL, `notes` TEXT NOT NULL, `completed` INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `workout_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workoutId` INTEGER NOT NULL, `exerciseId` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `workout_sets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workoutExerciseId` INTEGER NOT NULL, `setNumber` INTEGER NOT NULL, `weight` REAL NOT NULL, `reps` INTEGER NOT NULL, `rpe` REAL NOT NULL, `restSeconds` INTEGER NOT NULL, `completed` INTEGER NOT NULL)")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `exercises` ADD COLUMN `vtaper_lat` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `exercises` ADD COLUMN `vtaper_lateral_delt` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `exercises` ADD COLUMN `vtaper_upper_chest` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `exercises` ADD COLUMN `vtaper_rear_delt` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `exercises` ADD COLUMN `movement_pattern` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `exercises` ADD COLUMN `image_url` TEXT")
                database.execSQL("ALTER TABLE `exercises` ADD COLUMN `video_url` TEXT")
                database.execSQL("ALTER TABLE `exercises` ADD COLUMN `animation_url` TEXT")
                database.execSQL("ALTER TABLE `exercises` ADD COLUMN `setup_instructions` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `exercises` ADD COLUMN `execution_instructions` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `exercises` ADD COLUMN `breathing_instructions` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `exercises` ADD COLUMN `tempo_guidance` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `exercises` ADD COLUMN `beginner_variant_id` INTEGER")
                database.execSQL("ALTER TABLE `exercises` ADD COLUMN `advanced_variant_id` INTEGER")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_profiles` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `goal` TEXT NOT NULL DEFAULT '',
                        `experience` TEXT NOT NULL DEFAULT '',
                        `age` INTEGER NOT NULL DEFAULT 0,
                        `sex` TEXT NOT NULL DEFAULT '',
                        `height_cm` REAL NOT NULL DEFAULT 0.0,
                        `weight_kg` REAL NOT NULL DEFAULT 0.0,
                        `training_days_per_week` INTEGER NOT NULL DEFAULT 4,
                        `session_length_minutes` INTEGER NOT NULL DEFAULT 60,
                        `equipment_type` TEXT NOT NULL DEFAULT 'gym',
                        `preferred_exercises` TEXT NOT NULL DEFAULT '',
                        `exercises_to_avoid` TEXT NOT NULL DEFAULT '',
                        `created_at` INTEGER NOT NULL DEFAULT 0
                    )
                """)

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `programs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `user_id` INTEGER NOT NULL DEFAULT 1,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `split_type` TEXT NOT NULL DEFAULT '',
                        `duration_weeks` INTEGER NOT NULL DEFAULT 0,
                        `days_per_week` INTEGER NOT NULL DEFAULT 0,
                        `difficulty` TEXT NOT NULL DEFAULT '',
                        `goal` TEXT NOT NULL DEFAULT '',
                        `is_active` INTEGER NOT NULL DEFAULT 0,
                        `created_at` INTEGER NOT NULL
                    )
                """)

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `program_days` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `program_id` INTEGER NOT NULL,
                        `day_number` INTEGER NOT NULL,
                        `name` TEXT NOT NULL DEFAULT '',
                        `focus` TEXT NOT NULL DEFAULT '',
                        `is_rest_day` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`program_id`) REFERENCES `programs`(`id`) ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_program_days_program_id` ON `program_days`(`program_id`)")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `program_exercises` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `program_day_id` INTEGER NOT NULL,
                        `exercise_id` INTEGER NOT NULL,
                        `order_index` INTEGER NOT NULL DEFAULT 0,
                        `sets` INTEGER NOT NULL DEFAULT 3,
                        `target_reps` TEXT NOT NULL DEFAULT '',
                        `target_weight_kg` REAL NOT NULL DEFAULT 0.0,
                        `rest_seconds` INTEGER NOT NULL DEFAULT 90,
                        `notes` TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(`program_day_id`) REFERENCES `program_days`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_program_exercises_program_day_id` ON `program_exercises`(`program_day_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_program_exercises_exercise_id` ON `program_exercises`(`exercise_id`)")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `personal_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `exercise_id` INTEGER NOT NULL,
                        `user_id` INTEGER NOT NULL DEFAULT 1,
                        `weight_kg` REAL NOT NULL DEFAULT 0.0,
                        `reps` INTEGER NOT NULL DEFAULT 0,
                        `one_rep_max_kg` REAL NOT NULL DEFAULT 0.0,
                        `achieved_at` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_personal_records_exercise_id` ON `personal_records`(`exercise_id`)")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `body_measurements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `user_id` INTEGER NOT NULL DEFAULT 1,
                        `recorded_at` INTEGER NOT NULL,
                        `weight_kg` REAL NOT NULL DEFAULT 0.0,
                        `body_fat_pct` REAL,
                        `chest_cm` REAL,
                        `waist_cm` REAL,
                        `shoulders_cm` REAL,
                        `left_arm_cm` REAL,
                        `right_arm_cm` REAL,
                        `left_thigh_cm` REAL,
                        `right_thigh_cm` REAL,
                        `left_calf_cm` REAL,
                        `right_calf_cm` REAL,
                        `notes` TEXT NOT NULL
                    )
                """)

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `favorite_exercises` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `exercise_id` INTEGER NOT NULL,
                        `user_id` INTEGER NOT NULL DEFAULT 1,
                        `added_at` INTEGER NOT NULL,
                        FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_favorite_exercises_exercise_id` ON `favorite_exercises`(`exercise_id`)")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `exercise_substitutions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `original_exercise_id` INTEGER NOT NULL,
                        `substitute_exercise_id` INTEGER NOT NULL,
                        `reason` TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(`original_exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`substitute_exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_substitutions_original_exercise_id` ON `exercise_substitutions`(`original_exercise_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_substitutions_substitute_exercise_id` ON `exercise_substitutions`(`substitute_exercise_id`)")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `muscles` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `display_name` TEXT NOT NULL,
                        `parent_muscle_id` INTEGER,
                        `body_region` TEXT NOT NULL DEFAULT ''
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_muscles_parent_muscle_id` ON `muscles`(`parent_muscle_id`)")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `equipment` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `display_name` TEXT NOT NULL,
                        `category` TEXT NOT NULL DEFAULT ''
                    )
                """)

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `exercise_muscles` (
                        `exercise_id` INTEGER NOT NULL,
                        `muscle_id` INTEGER NOT NULL,
                        `role` TEXT NOT NULL,
                        PRIMARY KEY(`exercise_id`, `muscle_id`),
                        FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`muscle_id`) REFERENCES `muscles`(`id`) ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_muscles_exercise_id` ON `exercise_muscles`(`exercise_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_muscles_muscle_id` ON `exercise_muscles`(`muscle_id`)")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `exercise_equipment` (
                        `exercise_id` INTEGER NOT NULL,
                        `equipment_id` INTEGER NOT NULL,
                        `role` TEXT NOT NULL DEFAULT 'required',
                        PRIMARY KEY(`exercise_id`, `equipment_id`),
                        FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`equipment_id`) REFERENCES `equipment`(`id`) ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_equipment_exercise_id` ON `exercise_equipment`(`exercise_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_equipment_equipment_id` ON `exercise_equipment`(`equipment_id`)")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `exercise_aliases` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `exercise_id` INTEGER NOT NULL,
                        `alias` TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_aliases_exercise_id` ON `exercise_aliases`(`exercise_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_aliases_alias` ON `exercise_aliases`(`alias`)")
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `workout_sets` ADD COLUMN `setType` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // No-op: all tables and columns established in MIGRATION_2_3 matching version 8 entities.
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // v6: UserProfileEntity registered at v5 creation; no structural delta.
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // v7 added external-content FTS4 index over exercises for search
                database.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `exercise_fts` USING FTS4(`name` TEXT NOT NULL, `description` TEXT NOT NULL, `muscleGroup` TEXT NOT NULL, `equipment` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `category` TEXT NOT NULL, content=`exercises`)")
                database.execSQL("INSERT INTO exercise_fts(exercise_fts) VALUES('rebuild')")

                // Sync triggers to keep exercise_fts consistent with exercises table
                database.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS exercises_ai AFTER INSERT ON exercises BEGIN
                        INSERT INTO exercise_fts(rowid, name, description, muscleGroup, equipment, difficulty, category)
                        VALUES (new.id, new.name, new.description, new.muscleGroup, new.equipment, new.difficulty, new.category);
                    END
                """)
                database.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS exercises_au AFTER UPDATE ON exercises BEGIN
                        UPDATE exercise_fts SET
                            name = new.name,
                            description = new.description,
                            muscleGroup = new.muscleGroup,
                            equipment = new.equipment,
                            difficulty = new.difficulty,
                            category = new.category
                        WHERE rowid = new.id;
                    END
                """)
                database.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS exercises_ad AFTER DELETE ON exercises BEGIN
                        DELETE FROM exercise_fts WHERE rowid = old.id;
                    END
                """)
            }
        }

        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `workouts` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'NOT_STARTED'")
                database.execSQL("UPDATE workouts SET status = 'COMPLETED' WHERE completed = 1")
                database.execSQL(
                    "UPDATE workouts SET status = 'ACTIVE' WHERE completed = 0 " +
                        "AND id IN (SELECT workoutId FROM workout_exercises)"
                )
                database.execSQL(
                    "UPDATE workouts SET status = 'ABANDONED' WHERE completed = 0 AND status = 'NOT_STARTED'"
                )
            }
        }

        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Bench Press: moderate upper chest, moderate triceps
                database.execSQL("UPDATE exercises SET vtaper_lat=0, vtaper_lateral_delt=2, vtaper_upper_chest=7, vtaper_rear_delt=1 WHERE name='Bench Press'")
                // Squat: lower body, minimal V-taper
                database.execSQL("UPDATE exercises SET vtaper_lat=0, vtaper_lateral_delt=0, vtaper_upper_chest=0, vtaper_rear_delt=0 WHERE name='Squat'")
                // Push-up: moderate upper chest
                database.execSQL("UPDATE exercises SET vtaper_lat=0, vtaper_lateral_delt=1, vtaper_upper_chest=5, vtaper_rear_delt=1 WHERE name='Push-up'")
                // Shoulder Press: high lateral delt
                database.execSQL("UPDATE exercises SET vtaper_lat=0, vtaper_lateral_delt=8, vtaper_upper_chest=3, vtaper_rear_delt=1 WHERE name='Shoulder Press'")
                // Lateral Raise: primary lateral delt builder
                database.execSQL("UPDATE exercises SET vtaper_lat=1, vtaper_lateral_delt=10, vtaper_upper_chest=0, vtaper_rear_delt=2 WHERE name='Lateral Raise'")
                // Bent-over Row: high lat builder
                database.execSQL("UPDATE exercises SET vtaper_lat=9, vtaper_lateral_delt=0, vtaper_upper_chest=0, vtaper_rear_delt=5 WHERE name='Bent-over Row'")
                // Plank: core, minimal V-taper
                database.execSQL("UPDATE exercises SET vtaper_lat=0, vtaper_lateral_delt=0, vtaper_upper_chest=0, vtaper_rear_delt=0 WHERE name='Plank'")
                // Deadlift: high lat, rear delt
                database.execSQL("UPDATE exercises SET vtaper_lat=8, vtaper_lateral_delt=0, vtaper_upper_chest=0, vtaper_rear_delt=6 WHERE name='Deadlift'")
                // Bicep Curl: minimal V-taper
                database.execSQL("UPDATE exercises SET vtaper_lat=1, vtaper_lateral_delt=0, vtaper_upper_chest=0, vtaper_rear_delt=0 WHERE name='Bicep Curl'")
                // Pull-up: primary lat builder
                database.execSQL("UPDATE exercises SET vtaper_lat=10, vtaper_lateral_delt=1, vtaper_upper_chest=1, vtaper_rear_delt=4 WHERE name='Pull-up'")
                // Leg Press: lower body
                database.execSQL("UPDATE exercises SET vtaper_lat=0, vtaper_lateral_delt=0, vtaper_upper_chest=0, vtaper_rear_delt=0 WHERE name='Leg Press'")
                // Tricep Extension: minimal V-taper
                database.execSQL("UPDATE exercises SET vtaper_lat=0, vtaper_lateral_delt=0, vtaper_upper_chest=1, vtaper_rear_delt=0 WHERE name='Tricep Extension'")
                // Lunge: lower body
                database.execSQL("UPDATE exercises SET vtaper_lat=0, vtaper_lateral_delt=0, vtaper_upper_chest=0, vtaper_rear_delt=0 WHERE name='Lunge'")
                // Crunch: core
                database.execSQL("UPDATE exercises SET vtaper_lat=0, vtaper_lateral_delt=0, vtaper_upper_chest=0, vtaper_rear_delt=0 WHERE name='Crunch'")
                // Calf Raise: lower body
                database.execSQL("UPDATE exercises SET vtaper_lat=0, vtaper_lateral_delt=0, vtaper_upper_chest=0, vtaper_rear_delt=0 WHERE name='Calf Raise'")
                // Lat Pulldown: high lat builder
                database.execSQL("UPDATE exercises SET vtaper_lat=9, vtaper_lateral_delt=0, vtaper_upper_chest=0, vtaper_rear_delt=3 WHERE name='Lat Pulldown'")
                // Leg Curl: lower body
                database.execSQL("UPDATE exercises SET vtaper_lat=0, vtaper_lateral_delt=0, vtaper_upper_chest=0, vtaper_rear_delt=0 WHERE name='Leg Curl'")
                // Leg Extension: lower body
                database.execSQL("UPDATE exercises SET vtaper_lat=0, vtaper_lateral_delt=0, vtaper_upper_chest=0, vtaper_rear_delt=0 WHERE name='Leg Extension'")
                // Dumbbell Fly: upper chest, lateral delt
                database.execSQL("UPDATE exercises SET vtaper_lat=0, vtaper_lateral_delt=3, vtaper_upper_chest=6, vtaper_rear_delt=1 WHERE name='Dumbbell Fly'")
                // Barbell Curl: minimal V-taper
                database.execSQL("UPDATE exercises SET vtaper_lat=1, vtaper_lateral_delt=0, vtaper_upper_chest=0, vtaper_rear_delt=0 WHERE name='Barbell Curl'")
            }
        }

        /**
         * 9 -> 10: Add readiness table for daily recovery tracking.
         */
        val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `readiness` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `user_id` INTEGER NOT NULL DEFAULT 1,
                        `recorded_at` INTEGER NOT NULL,
                        `sleep_quality` INTEGER NOT NULL DEFAULT 3,
                        `soreness` INTEGER NOT NULL DEFAULT 3,
                        `energy` INTEGER NOT NULL DEFAULT 3,
                        `motivation` INTEGER NOT NULL DEFAULT 3,
                        `notes` TEXT NOT NULL DEFAULT ''
                    )
                """)
            }
        }

        /**
         * 10 -> 11: Add preferred_schedule and limitations_preferences to user_profiles table.
         */
        val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `user_profiles` ADD COLUMN `preferred_schedule` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `user_profiles` ADD COLUMN `limitations_preferences` TEXT NOT NULL DEFAULT ''")
            }
        }

        fun create(context: Context): GymCoachDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                GymCoachDatabase::class.java,
                "gymcoach.db"
            )
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                    MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                    MIGRATION_9_10, MIGRATION_10_11
                )
                .build()
        }
    }
}
