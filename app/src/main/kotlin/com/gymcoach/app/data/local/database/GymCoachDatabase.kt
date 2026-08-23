package com.gymcoach.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gymcoach.app.data.local.dao.BodyMeasurementDao
import com.gymcoach.app.data.local.dao.EquipmentDao
import com.gymcoach.app.data.local.dao.ExerciseAliasDao
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.ExerciseEquipmentDao
import com.gymcoach.app.data.local.dao.ExerciseMuscleDao
import com.gymcoach.app.data.local.dao.ExerciseSubstitutionDao
import com.gymcoach.app.data.local.dao.FavoriteExerciseDao
import com.gymcoach.app.data.local.dao.MuscleDao
import com.gymcoach.app.data.local.dao.PersonalRecordDao
import com.gymcoach.app.data.local.dao.ProgramDayDao
import com.gymcoach.app.data.local.dao.ProgramDao
import com.gymcoach.app.data.local.dao.ProgramExerciseDao
import com.gymcoach.app.data.local.dao.UserProfileDao
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.entity.BodyMeasurementEntity
import com.gymcoach.app.data.local.entity.EquipmentEntity
import com.gymcoach.app.data.local.entity.ExerciseAliasEntity
import com.gymcoach.app.data.local.entity.ExerciseEquipmentEntity
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.ExerciseFtsEntity
import com.gymcoach.app.data.local.entity.ExerciseMuscleEntity
import com.gymcoach.app.data.local.entity.ExerciseSubstitutionEntity
import com.gymcoach.app.data.local.entity.FavoriteExerciseEntity
import com.gymcoach.app.data.local.entity.MuscleEntity
import com.gymcoach.app.data.local.entity.PersonalRecordEntity
import com.gymcoach.app.data.local.entity.ProgramDayEntity
import com.gymcoach.app.data.local.entity.ProgramEntity
import com.gymcoach.app.data.local.entity.ProgramExerciseEntity
import com.gymcoach.app.data.local.entity.UserProfileEntity
import com.gymcoach.app.data.local.entity.WorkoutEntity
import com.gymcoach.app.data.local.entity.WorkoutExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutSetEntity

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
        UserProfileEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class GymCoachDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun programDao(): ProgramDao
    abstract fun programDayDao(): ProgramDayDao
    abstract fun programExerciseDao(): ProgramExerciseDao
    abstract fun personalRecordDao(): PersonalRecordDao
    abstract fun bodyMeasurementDao(): BodyMeasurementDao
    abstract fun muscleDao(): MuscleDao
    abstract fun equipmentDao(): EquipmentDao
    abstract fun favoriteExerciseDao(): FavoriteExerciseDao
    abstract fun exerciseAliasDao(): ExerciseAliasDao
    abstract fun exerciseMuscleDao(): ExerciseMuscleDao
    abstract fun exerciseEquipmentDao(): ExerciseEquipmentDao
    abstract fun exerciseSubstitutionDao(): ExerciseSubstitutionDao

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
                // No-op placeholder for version continuity (v3 added no tables).
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `workout_sets` ADD COLUMN `setType` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // New exercise metadata columns
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

                // Rebuild muscles/equipment to match current entity schemas
                database.execSQL("DROP TABLE IF EXISTS `muscles_legacy_v4`")
                database.execSQL("ALTER TABLE `muscles` RENAME TO `muscles_legacy_v4`")
                database.execSQL("CREATE TABLE IF NOT EXISTS `muscles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `display_name` TEXT NOT NULL, `parent_muscle_id` INTEGER, `body_region` TEXT NOT NULL DEFAULT '')")
                database.execSQL("INSERT INTO `muscles` (`id`, `name`, `display_name`, `body_region`) SELECT `id`, `name`, COALESCE(`name`, ''), '' FROM `muscles_legacy_v4`")
                database.execSQL("DROP TABLE IF EXISTS `muscles_legacy_v4`")

                database.execSQL("ALTER TABLE `equipment` RENAME TO `equipment_legacy_v4`")
                database.execSQL("CREATE TABLE IF NOT EXISTS `equipment` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `display_name` TEXT NOT NULL, `category` TEXT NOT NULL DEFAULT '')")
                database.execSQL("INSERT INTO `equipment` (`id`, `name`, `display_name`, `category`) SELECT `id`, `name`, COALESCE(`name`, ''), COALESCE(`category`, '') FROM `equipment_legacy_v4`")
                database.execSQL("DROP TABLE IF EXISTS `equipment_legacy_v4`")

                // Remaining new tables (schemas mirror entities exactly)
                database.execSQL("CREATE TABLE IF NOT EXISTS `user_profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `goal` TEXT NOT NULL DEFAULT '', `experience` TEXT NOT NULL DEFAULT '', `age` INTEGER NOT NULL DEFAULT 0, `sex` TEXT NOT NULL DEFAULT '', `height_cm` REAL NOT NULL DEFAULT 0.0, `weight_kg` REAL NOT NULL DEFAULT 0.0, `training_days_per_week` INTEGER NOT NULL DEFAULT 4, `session_length_minutes` INTEGER NOT NULL DEFAULT 60, `equipment_type` TEXT NOT NULL DEFAULT 'gym', `preferred_exercises` TEXT NOT NULL DEFAULT '', `exercises_to_avoid` TEXT NOT NULL DEFAULT '', `created_at` INTEGER NOT NULL DEFAULT 0)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `programs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL DEFAULT '', `description` TEXT NOT NULL DEFAULT '', `goal` TEXT NOT NULL DEFAULT '', `frequency` INTEGER NOT NULL DEFAULT 4, `is_active` INTEGER NOT NULL DEFAULT 0, `created_at` INTEGER NOT NULL DEFAULT 0)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `program_days` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `program_id` INTEGER NOT NULL, `day_number` INTEGER NOT NULL, `name` TEXT NOT NULL DEFAULT '', `target_muscles` TEXT NOT NULL DEFAULT '', FOREIGN KEY(`program_id`) REFERENCES `programs`(`id`) ON DELETE CASCADE)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `program_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `program_day_id` INTEGER NOT NULL, `exercise_id` INTEGER NOT NULL, `order_index` INTEGER NOT NULL DEFAULT 0, `target_sets` INTEGER NOT NULL DEFAULT 3, `target_reps_min` INTEGER NOT NULL DEFAULT 8, `target_reps_max` INTEGER NOT NULL DEFAULT 12, `target_rpe` REAL NOT NULL DEFAULT 7.0, `rest_seconds` INTEGER NOT NULL DEFAULT 90, FOREIGN KEY(`program_day_id`) REFERENCES `program_days`(`id`) ON DELETE CASCADE, FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `personal_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `exercise_id` INTEGER NOT NULL, `weight` REAL NOT NULL DEFAULT 0.0, `reps` INTEGER NOT NULL DEFAULT 0, `estimated_1rm` REAL NOT NULL DEFAULT 0.0, `volume` REAL NOT NULL DEFAULT 0.0, `workout_id` INTEGER NOT NULL, `created_at` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE, FOREIGN KEY(`workout_id`) REFERENCES `workouts`(`id`) ON DELETE CASCADE)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `body_measurements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` INTEGER NOT NULL DEFAULT 0, `weight_kg` REAL NOT NULL DEFAULT 0.0, `waist_cm` REAL NOT NULL DEFAULT 0.0, `chest_cm` REAL NOT NULL DEFAULT 0.0, `shoulders_cm` REAL NOT NULL DEFAULT 0.0, `arm_cm` REAL NOT NULL DEFAULT 0.0, `thigh_cm` REAL NOT NULL DEFAULT 0.0, `body_fat_estimate` REAL NOT NULL DEFAULT 0.0, `photo_url` TEXT NOT NULL DEFAULT '', `notes` TEXT NOT NULL DEFAULT '')")
                database.execSQL("CREATE TABLE IF NOT EXISTS `favorite_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `exercise_id` INTEGER NOT NULL, `added_at` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `exercise_substitutions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `original_exercise_id` INTEGER NOT NULL, `substitute_exercise_id` INTEGER NOT NULL, `reason` TEXT NOT NULL DEFAULT '', FOREIGN KEY(`original_exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE, FOREIGN KEY(`substitute_exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `exercise_muscles` (`exercise_id` INTEGER NOT NULL, `muscle_id` INTEGER NOT NULL, `role` TEXT NOT NULL DEFAULT 'primary', PRIMARY KEY(`exercise_id`, `muscle_id`), FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE, FOREIGN KEY(`muscle_id`) REFERENCES `muscles`(`id`) ON DELETE CASCADE)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `exercise_equipment` (`exercise_id` INTEGER NOT NULL, `equipment_id` INTEGER NOT NULL, `role` TEXT NOT NULL DEFAULT 'required', PRIMARY KEY(`exercise_id`, `equipment_id`), FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE, FOREIGN KEY(`equipment_id`) REFERENCES `equipment`(`id`) ON DELETE CASCADE)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `exercise_aliases` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `exercise_id` INTEGER NOT NULL, `alias` TEXT NOT NULL DEFAULT '', FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE)")

                // Indexes required by @Index declarations on entities
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_muscles_parent_muscle_id` ON `muscles` (`parent_muscle_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_muscles_exercise_id` ON `exercise_muscles` (`exercise_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_muscles_muscle_id` ON `exercise_muscles` (`muscle_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_equipment_exercise_id` ON `exercise_equipment` (`exercise_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_equipment_equipment_id` ON `exercise_equipment` (`equipment_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_substitutions_original_exercise_id` ON `exercise_substitutions` (`original_exercise_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_substitutions_substitute_exercise_id` ON `exercise_substitutions` (`substitute_exercise_id`)")
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
            }
        }

        fun create(context: Context): GymCoachDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                GymCoachDatabase::class.java,
                "gymcoach.db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .build()
            // NOTE: fallbackToDestructiveMigration() removed (security audit P0).
            // Seeding is handled by ExerciseSeeder.seedIfNeeded() at first app start.
        }
    }
}
