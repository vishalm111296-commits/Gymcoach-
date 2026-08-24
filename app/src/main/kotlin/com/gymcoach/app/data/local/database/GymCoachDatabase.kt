package com.gymcoach.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
import com.gymcoach.app.data.local.dao.ProgramDao
import com.gymcoach.app.data.local.dao.ProgramDayDao
import com.gymcoach.app.data.local.dao.ProgramExerciseDao
import com.gymcoach.app.data.local.dao.ReadinessDao
import com.gymcoach.app.data.local.dao.UserProfileDao
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.entity.BodyMeasurementEntity
import com.gymcoach.app.data.local.entity.EquipmentEntity
import com.gymcoach.app.data.local.entity.ExerciseAliasEntity
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.ExerciseEquipmentEntity
import com.gymcoach.app.data.local.entity.ExerciseFtsEntity
import com.gymcoach.app.data.local.entity.ExerciseMuscleEntity
import com.gymcoach.app.data.local.entity.ExerciseSubstitutionEntity
import com.gymcoach.app.data.local.entity.FavoriteExerciseEntity
import com.gymcoach.app.data.local.entity.MuscleEntity
import com.gymcoach.app.data.local.entity.PersonalRecordEntity
import com.gymcoach.app.data.local.entity.ProgramDayEntity
import com.gymcoach.app.data.local.entity.ProgramEntity
import com.gymcoach.app.data.local.entity.ProgramExerciseEntity
import com.gymcoach.app.data.local.entity.ReadinessEntity
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
        UserProfileEntity::class,
        ReadinessEntity::class
    ],
    version = 10,
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

        fun create(context: Context): GymCoachDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                GymCoachDatabase::class.java,
                "gymcoach.db"
            )
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                    MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                    MIGRATION_9_10
                )
                // Room 2.6.1 API: destructive fallback only when no migration path exists.
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
                // name, description, muscleGroup, equipment, difficulty, secondaryMuscles, instructions, tips, commonMistakes, safetyNotes, recommendedRepRange, recommendedRestTime, estimatedCalories, category, tags, isFavorite, lastViewed,
                // vtaper_lat, vtaper_lateral_delt, vtaper_upper_chest, vtaper_rear_delt, movement_pattern
                arrayOf<Any>("Bench Press", "Lie on a bench and press the barbell from your chest to full arm extension.", "Chest", "Barbell", "Intermediate", "Triceps, Shoulders", "1. Lie flat on bench. 2. Grip bar slightly wider than shoulder width. 3. Lower bar to mid-chest. 4. Press bar up.", "Keep feet flat on floor.", "Bouncing bar off chest.", "Use a spotter.", "8-12", "90s", 15, "Powerlifting", "Push, Upper Body", 0, 0L, 0, 2, 7, 1, "horizontal_push"),
                arrayOf<Any>("Squat", "Lower your hips until your thighs are parallel to the floor, then stand back up.", "Legs", "Barbell", "Beginner", "Glutes, Core", "1. Stand with feet shoulder-width apart. 2. Rest bar on upper back. 3. Bend knees and drop hips. 4. Push through heels to stand.", "Keep chest up.", "Knees caving in.", "Use a squat rack.", "5-10", "120s", 20, "Powerlifting", "Lower Body", 0, 0L, 0, 0, 0, 0, "squat"),
                arrayOf<Any>("Push-up", "Lower your chest toward the floor while keeping your body in a straight line.", "Chest", "Bodyweight", "Beginner", "Triceps, Shoulders, Core", "1. Start in plank position. 2. Lower body until chest touches floor. 3. Push back up.", "Keep core engaged.", "Sagging hips.", "Do not flare elbows too much.", "10-20", "60s", 10, "Bodyweight", "Push, Home", 0, 0L, 0, 1, 5, 1, "horizontal_push"),
                arrayOf<Any>("Shoulder Press", "Press the dumbbells overhead until your arms are fully extended.", "Shoulders", "Dumbbell", "Intermediate", "Triceps", "1. Sit or stand with dumbbells at shoulder height. 2. Press weights overhead. 3. Lower with control.", "Don't arch your back.", "Using momentum.", "Control the weight on the way down.", "8-15", "90s", 12, "Resistance", "Push, Upper Body", 0, 0L, 0, 8, 3, 1, "vertical_push"),
                arrayOf<Any>("Lateral Raise", "Raise dumbbells out to the sides until your arms are parallel with the floor.", "Shoulders", "Dumbbell", "Beginner", "Traps", "1. Stand with dumbbells at sides. 2. Raise arms out to sides. 3. Lower slowly.", "Slight bend in elbows.", "Swinging torso.", "Use light weight.", "12-20", "60s", 8, "Resistance", "Isolation, Upper Body", 0, 0L, 1, 10, 0, 2, "lateral_raise"),
                arrayOf<Any>("Bent-over Row", "Hinge at the hips and pull the barbell toward your torso.", "Back", "Barbell", "Intermediate", "Biceps, Core", "1. Hinge forward at hips. 2. Keep back straight. 3. Pull bar to stomach. 4. Lower bar.", "Squeeze shoulder blades together.", "Rounding the lower back.", "Keep spine neutral.", "8-12", "90s", 15, "Resistance", "Pull, Upper Body", 0, 0L, 9, 0, 0, 5, "horizontal_pull"),
                arrayOf<Any>("Plank", "Hold a straight-body position supported on your forearms and toes.", "Core", "Bodyweight", "Beginner", "Shoulders, Glutes", "1. Rest on forearms and toes. 2. Keep body in a straight line. 3. Hold position.", "Breathe steadily.", "Hips too high or too low.", "Stop if lower back hurts.", "30-60s", "60s", 5, "Bodyweight", "Core, Home", 0, 0L, 0, 0, 0, 0, "isometric"),
                arrayOf<Any>("Deadlift", "Hinge at the hips and lift the loaded barbell to a standing position.", "Back", "Barbell", "Advanced", "Glutes, Hamstrings, Core", "1. Stand with mid-foot under bar. 2. Bend and grab bar. 3. Lift chest and straighten back. 4. Stand up with the weight.", "Keep bar close to legs.", "Rounding the back.", "Keep spine neutral to avoid injury.", "3-8", "180s", 25, "Powerlifting", "Pull, Full Body", 0, 0L, 8, 0, 0, 6, "hip_hinge"),
                arrayOf<Any>("Bicep Curl", "Curl the dumbbells toward your shoulders with your elbows pinned to your sides.", "Arms", "Dumbbell", "Beginner", "Forearms", "1. Stand with dumbbells at sides. 2. Curl weights up. 3. Lower slowly.", "Keep elbows stationary.", "Using back momentum.", "Control the eccentric phase.", "10-15", "60s", 8, "Resistance", "Isolation, Arms", 0, 0L, 1, 0, 0, 0, "isolation"),
                arrayOf<Any>("Pull-up", "Pull your body up to a bar until your chin is over it.", "Back", "Bodyweight", "Intermediate", "Biceps, Core", "1. Hang from bar. 2. Pull body up until chin clears bar. 3. Lower with control.", "Engage lats first.", "Kicking legs.", "Use assistance if needed.", "5-15", "120s", 15, "Bodyweight", "Pull, Upper Body", 0, 0L, 10, 1, 1, 4, "vertical_pull"),
                arrayOf<Any>("Leg Press", "Press weight away from you using your legs on a machine.", "Legs", "Machine", "Beginner", "Glutes, Calves", "1. Sit in machine. 2. Place feet on sled. 3. Lower sled. 4. Press sled up.", "Don't lock knees.", "Lifting hips off pad.", "Use safety stops.", "10-15", "90s", 15, "Machine", "Lower Body", 0, 0L, 0, 0, 0, 0, "push"),
                arrayOf<Any>("Tricep Extension", "Extend your arms against resistance to work the triceps.", "Arms", "Cable", "Beginner", "Shoulders", "1. Grip cable attachment. 2. Keep elbows tucked. 3. Extend arms down. 4. Return slowly.", "Squeeze at bottom.", "Moving elbows.", "Don't use too much weight.", "12-15", "60s", 8, "Cable", "Isolation, Arms", 0, 0L, 0, 0, 1, 0, "isolation"),
                arrayOf<Any>("Lunge", "Step forward and lower your hips until both knees are bent at 90 degrees.", "Legs", "Bodyweight", "Beginner", "Glutes, Core", "1. Step forward. 2. Lower hips. 3. Push back up.", "Keep torso upright.", "Knee going past toes.", "Maintain balance.", "10-20", "60s", 12, "Bodyweight", "Lower Body, Home", 0, 0L, 0, 0, 0, 0, "lunge"),
                arrayOf<Any>("Crunch", "Curl your shoulders toward your pelvis while lying on your back.", "Core", "Bodyweight", "Beginner", "Obliques", "1. Lie on back. 2. Curl shoulders up. 3. Lower back down.", "Don't pull neck.", "Using momentum.", "Perform on a mat.", "15-25", "45s", 5, "Bodyweight", "Core, Home", 0, 0L, 0, 0, 0, 0, "isolation"),
                arrayOf<Any>("Calf Raise", "Raise your heels off the ground to work the calves.", "Legs", "Machine", "Beginner", "None", "1. Stand on edge of step. 2. Lower heels. 3. Raise heels high.", "Full stretch at bottom.", "Bouncing.", "Use support for balance.", "15-20", "60s", 5, "Machine", "Isolation, Lower Body", 0, 0L, 0, 0, 0, 0, "isolation"),
                arrayOf<Any>("Lat Pulldown", "Pull the bar down to your chest while seated.", "Back", "Cable", "Beginner", "Biceps", "1. Sit at machine. 2. Grip bar wide. 3. Pull bar to upper chest. 4. Return slowly.", "Pull with lats.", "Leaning back too far.", "Control the weight.", "8-12", "90s", 12, "Cable", "Pull, Upper Body", 0, 0L, 9, 0, 0, 3, "vertical_pull"),
                arrayOf<Any>("Leg Curl", "Curl your legs against resistance to work the hamstrings.", "Legs", "Machine", "Beginner", "Calves", "1. Lie on machine. 2. Curl weight up. 3. Lower slowly.", "Keep hips down.", "Using momentum.", "Adjust machine to fit.", "10-15", "90s", 10, "Machine", "Isolation, Lower Body", 0, 0L, 0, 0, 0, 0, "isolation"),
                arrayOf<Any>("Leg Extension", "Extend your legs against resistance to work the quads.", "Legs", "Machine", "Beginner", "None", "1. Sit on machine. 2. Extend legs. 3. Lower slowly.", "Squeeze at top.", "Swinging the weight.", "Don't hyperextend knees.", "10-15", "60s", 10, "Machine", "Isolation, Lower Body", 0, 0L, 0, 0, 0, 0, "isolation"),
                arrayOf<Any>("Dumbbell Fly", "Raise dumbbells to the side to work the shoulders.", "Shoulders", "Dumbbell", "Beginner", "Traps", "1. Stand with dumbbells. 2. Raise to sides. 3. Lower slowly.", "Slight elbow bend.", "Shrugging shoulders.", "Use light weights.", "12-15", "60s", 8, "Resistance", "Isolation, Upper Body", 0, 0L, 0, 3, 6, 1, "lateral_raise"),
                arrayOf<Any>("Barbell Curl", "Curl a barbell to work the biceps.", "Arms", "Barbell", "Intermediate", "Forearms", "1. Stand holding bar. 2. Curl bar up. 3. Lower slowly.", "Keep elbows tucked.", "Swinging back.", "Control the descent.", "8-12", "90s", 10, "Resistance", "Isolation, Arms", 0, 0L, 1, 0, 0, 0, "isolation")
            )
            seedData.forEach { row ->
                db.execSQL(
                    "INSERT INTO exercises (name, description, muscleGroup, equipment, difficulty, secondaryMuscles, instructions, tips, commonMistakes, safetyNotes, recommendedRepRange, recommendedRestTime, estimatedCalories, category, tags, isFavorite, lastViewed, vtaper_lat, vtaper_lateral_delt, vtaper_upper_chest, vtaper_rear_delt, movement_pattern) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    row
                )
            }
        }
    }
}
