package com.gymcoach.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gymcoach.app.data.local.dao.*
import com.gymcoach.app.data.local.entity.*

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class,
        ProgramEntity::class,
        ProgramDayEntity::class,
        ProgramExerciseEntity::class,
        PersonalRecordEntity::class,
        BodyMeasurementEntity::class,
        FavoriteExerciseEntity::class,
        ExerciseSubstitutionEntity::class,
        MuscleEntity::class,
        EquipmentEntity::class,
        ExerciseMuscleEntity::class,
        ExerciseEquipmentEntity::class,
        ExerciseAliasEntity::class,
        UserProfileEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class GymCoachDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutExerciseDao(): WorkoutExerciseDao
    abstract fun workoutSetDao(): WorkoutSetDao
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
                database.execSQL("CREATE TABLE IF NOT EXISTS `programs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `userProfileId` INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `program_days` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `programId` INTEGER NOT NULL, `dayOfWeek` INTEGER NOT NULL, `dayName` TEXT NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `program_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `programDayId` INTEGER NOT NULL, `exerciseId` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `personal_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userProfileId` INTEGER NOT NULL, `exerciseId` INTEGER NOT NULL, `weight` REAL NOT NULL, `reps` INTEGER NOT NULL, `dateAchieved` INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `body_measurements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userProfileId` INTEGER NOT NULL, `recorded_at` INTEGER NOT NULL, `weight_kg` REAL NOT NULL DEFAULT 0.0, `body_fat_pct` REAL, `chest_cm` REAL, `waist_cm` REAL, `hips_cm` REAL, `shoulders_cm` REAL, `left_arm_cm` REAL, `right_arm_cm` REAL, `left_thigh_cm` REAL, `right_thigh_cm` REAL, `left_calf_cm` REAL, `right_calf_cm` REAL, `notes` TEXT NOT NULL DEFAULT '')")
                database.execSQL("CREATE TABLE IF NOT EXISTS `favorite_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userProfileId` INTEGER NOT NULL, `exerciseId` INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `exercise_substitutions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `original_exercise_id` INTEGER NOT NULL, `substitute_exercise_id` INTEGER NOT NULL, `reason` TEXT NOT NULL DEFAULT '')")
                database.execSQL("CREATE TABLE IF NOT EXISTS `muscles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `nameEn` TEXT NOT NULL, `isPrimary` INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `equipment` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `display_name` TEXT NOT NULL, `category` TEXT NOT NULL DEFAULT '')")
                database.execSQL("CREATE TABLE IF NOT EXISTS `exercise_muscles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `exerciseId` INTEGER NOT NULL, `muscleId` INTEGER NOT NULL, PRIMARY KEY(exercise_id, muscle_id), FOREIGN KEY (exercise_id) REFERENCES exercises(id) ON DELETE CASCADE, FOREIGN KEY (muscle_id) REFERENCES muscles(id) ON DELETE CASCADE)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `exercise_equipment` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `exerciseId` INTEGER NOT NULL, `equipmentId` INTEGER NOT NULL, PRIMARY KEY(exercise_id, equipment_id), FOREIGN KEY (exercise_id) REFERENCES exercises(id) ON DELETE CASCADE, FOREIGN KEY (equipment_id) REFERENCES equipment(id) ON DELETE CASCADE)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `exercise_aliases` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `primaryId` INTEGER NOT NULL, `aliasName` TEXT NOT NULL, `creationDate` INTEGER NOT NULL)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_program_days_program_id` ON program_days(program_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_program_exercises_program_day_id` ON program_exercises(program_day_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_program_exercises_exercise_id` ON program_exercises(exercise_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_personal_records_exercise_id` ON personal_records(exercise_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_body_measurements_user_profile_id` ON body_measurements(user_profile_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_favorite_exercises_exercise_id` ON favorite_exercises(exercise_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_substitutions_original_exercise_id` ON exercise_substitutions(original_exercise_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_substitutions_substitute_exercise_id` ON exercise_substitutions(substitute_exercise_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_muscles_primary` ON muscles(isPrimary)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_equipment_category` ON equipment(category)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_muscles_exercise_id` ON exercise_muscles(exercise_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_muscles_muscle_id` ON exercise_muscles(muscle_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_equipment_exercise_id` ON exercise_equipment(exercise_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_equipment_equipment_id` ON exercise_equipment(equipment_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_aliases_exercise_id` ON exercise_aliases(exercise_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_aliases_alias` ON exercise_aliases(alias)")
                // FTS4 virtual table for exercise search
                database.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS exercises_fts USING fts4(
                        content="exercises",
                        name,
                        description,
                        muscleGroup,
                        equipment,
                        tags
                    )
                """)
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Schema at version 3 (after MIGRATION_2_3) already matches entity definitions.
                // This migration is a no-op to satisfy Room's requirement for a complete
                # Complete the migration fix