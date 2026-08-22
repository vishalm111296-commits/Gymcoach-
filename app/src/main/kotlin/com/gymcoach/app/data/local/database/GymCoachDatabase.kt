package com.gymcoach.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gymcoach.app.data.local.dao.BodyMeasurementDao
import com.gymcoach.app.data.local.dao.EquipmentDao
import com.gymcoach.app.data.local.dao.ExerciseAliasDao
import com.gymcoach.app.data.local.dao.ExerciseEquipmentDao
import com.gymcoach.app.data.local.dao.ExerciseMuscleDao
import com.gymcoach.app.data.local.dao.ExerciseSubstitutionDao
import com.gymcoach.app.data.local.dao.ExerciseDao
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
    version = 6,
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
                // v2 had no new tables, v3 added workout_exercises and workout_sets via 1_2
                // This migration is a no-op placeholder for version continuity
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // v4 added setType column to workout_sets
                database.execSQL("ALTER TABLE `workout_sets` ADD COLUMN `setType` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add new columns to exercises table
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

                // Create all 13 new tables matching entity schemas
                database.execSQL("CREATE TABLE IF NOT EXISTS `user_profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `goal` TEXT NOT NULL DEFAULT '', `experience` TEXT NOT NULL DEFAULT '', `age` INTEGER NOT NULL DEFAULT 0, `sex` TEXT NOT NULL DEFAULT '', `height_cm` REAL NOT NULL DEFAULT 0.0, `weight_kg` REAL NOT NULL DEFAULT 0.0, `training_days_per_week` INTEGER NOT NULL DEFAULT 4, `session_length_minutes` INTEGER NOT NULL DEFAULT 60, `equipment_type` TEXT NOT NULL DEFAULT 'gym', `preferred_exercises` TEXT NOT NULL DEFAULT '', `exercises_to_avoid` TEXT NOT NULL DEFAULT '', `created_at` INTEGER NOT NULL DEFAULT 0)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `programs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL DEFAULT '', `description` TEXT NOT NULL DEFAULT '', `goal` TEXT NOT NULL DEFAULT '', `frequency` INTEGER NOT NULL DEFAULT 4, `is_active` INTEGER NOT NULL DEFAULT 0, `created_at` INTEGER NOT NULL DEFAULT 0)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `program_days` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `program_id` INTEGER NOT NULL, `day_number` INTEGER NOT NULL, `name` TEXT NOT NULL DEFAULT '', `target_muscles` TEXT NOT NULL DEFAULT '', FOREIGN KEY(`program_id`) REFERENCES `programs`(`id`) ON DELETE CASCADE)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `program_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `program_day_id` INTEGER NOT NULL, `exercise_id` INTEGER NOT NULL, `order_index` INTEGER NOT NULL DEFAULT 0, `target_sets` INTEGER NOT NULL DEFAULT 3, `target_reps_min` INTEGER NOT NULL DEFAULT 8, `target_reps_max` INTEGER NOT NULL DEFAULT 12, `target_rpe` REAL NOT NULL DEFAULT 7.0, `rest_seconds` INTEGER NOT NULL DEFAULT 90, FOREIGN KEY(`program_day_id`) REFERENCES `program_days`(`id`) ON DELETE CASCADE, FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `personal_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `exercise_id` INTEGER NOT NULL, `weight` REAL NOT NULL DEFAULT 0.0, `reps` INTEGER NOT NULL DEFAULT 0, `estimated_1rm` REAL NOT NULL DEFAULT 0.0, `volume` REAL NOT NULL DEFAULT 0.0, `workout_id` INTEGER NOT NULL, `created_at` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE, FOREIGN KEY(`workout_id`) REFERENCES `workouts`(`id`) ON DELETE CASCADE)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `body_measurements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` INTEGER NOT NULL DEFAULT 0, `weight_kg` REAL NOT NULL DEFAULT 0.0, `waist_cm` REAL NOT NULL DEFAULT 0.0, `chest_cm` REAL NOT NULL DEFAULT 0.0, `shoulders_cm` REAL NOT NULL DEFAULT 0.0, `arm_cm` REAL NOT NULL DEFAULT 0.0, `thigh_cm` REAL NOT NULL DEFAULT 0.0, `body_fat_estimate` REAL NOT NULL DEFAULT 0.0, `photo_url` TEXT NOT NULL DEFAULT '', `notes` TEXT NOT NULL DEFAULT '')")
                database.execSQL("CREATE TABLE IF NOT EXISTS `favorite_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `exercise_id` INTEGER NOT NULL, `added_at` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `exercise_substitutions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `exercise_id` INTEGER NOT NULL, `substitute_id` INTEGER NOT NULL, `preservation_score` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE, FOREIGN KEY(`substitute_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `muscles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NOT NULL DEFAULT '', `group_name` TEXT NOT NULL DEFAULT '', `vtaper_relevance` INTEGER NOT NULL DEFAULT 0)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `equipment` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL DEFAULT '', `category` TEXT NOT NULL DEFAULT '', `gym_available` INTEGER NOT NULL DEFAULT 1, `home_available` INTEGER NOT NULL DEFAULT 0)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `exercise_muscles` (`exercise_id` INTEGER NOT NULL, `muscle_id` INTEGER NOT NULL, `role` TEXT NOT NULL DEFAULT 'primary', PRIMARY KEY(`exercise_id`, `muscle_id`), FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE, FOREIGN KEY(`muscle_id`) REFERENCES `muscles`(`id`) ON DELETE CASCADE)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `exercise_equipment` (`exercise_id` INTEGER NOT NULL, `equipment_id` INTEGER NOT NULL, `role` TEXT NOT NULL DEFAULT 'primary', PRIMARY KEY(`exercise_id`, `equipment_id`), FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE, FOREIGN KEY(`equipment_id`) REFERENCES `equipment`(`id`) ON DELETE CASCADE)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `exercise_aliases` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `exercise_id` INTEGER NOT NULL, `alias` TEXT NOT NULL DEFAULT '', FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON DELETE CASCADE)")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // v6 schema update - ensure UserProfileEntity is properly migrated
            }
        }

        fun create(context: Context): GymCoachDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                GymCoachDatabase::class.java,
                "gymcoach.db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
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
                arrayOf<Any>("Bench Press", "Lie on a bench and press the barbell from your chest to full arm extension.", "Chest", "Barbell", "Intermediate", "Triceps, Shoulders", "1. Lie flat on bench. 2. Grip bar slightly wider than shoulder width. 3. Lower bar to mid-chest. 4. Press bar up.", "Keep feet flat on floor.", "Bouncing bar off chest.", "Use a spotter.", "8-12", "90s", 15, "Powerlifting", "Push, Upper Body", 0, 0L, 3, 3, 9, 7, "Push", "https://example.com/bench.jpg", null, null, "Lie on bench with feet flat", "Press bar to full extension", "", "2-0-2", 1, null),
                arrayOf<Any>("Squat", "Lower your hips until your thighs are parallel to the floor, then stand back up.", "Legs", "Barbell", "Beginner", "Glutes, Core", "1. Stand with feet shoulder-width apart. 2. Rest bar on upper back. 3. Bend knees and drop hips. 4. Push through heels to stand.", "Keep chest up.", "Knees caving in.", "Use a squat rack.", "5-10", "120s", 20, "Powerlifting", "Lower Body", 0, 0L, 9, 5, 0, 4, "Pull", "https://example.com/squat.jpg", null, null, "Stand with feet shoulder-width", "Squat until thighs parallel to floor", "", "3-0-1", 1, null),
                arrayOf<Any>("Push-up", "Lower your chest toward the floor while keeping your body in a straight line.", "Chest", "Bodyweight", "Beginner", "Triceps, Shoulders, Core", "1. Start in plank position. 2. Lower body until chest touches floor. 3. Push back up.", "Keep core engaged.", "Sagging hips.", "Do not flare elbows too much.", "10-20", "60s", 10, "Bodyweight", "Push, Home", 0, 0L, 9, 8, 10, 6, "Push", "https://example.com/pushup.jpg", null, null, "Start plank position", "Lower until chest touches floor", "", "2-0-2", 1, null),
                arrayOf<Any>("Shoulder Press", "Press the dumbbells overhead until your arms are fully extended.", "Shoulders", "Dumbbell", "Intermediate", "Triceps", "1. Sit or stand with dumbbells at shoulder height. 2. Press weights overhead. 3. Lower with control.", "Don't arch your back.", "Using momentum.", "Control the weight on the way down.", "8-15", "90s", 12, "Resistance", "Push, Upper Body", 0, 0L, 7, 9, 8, 5, "Push", "https://example.com/shoulderpress.jpg", null, null, "Start with dumbbells at shoulders", "Press overhead to lockout", "", "1-0-1", 1, null),
                arrayOf<Any>("Lateral Raise", "Raise dumbbells out to the sides until your arms are parallel with the floor.", "Shoulders", "Dumbbell", "Beginner", "Traps", "1. Stand with dumbbells at sides. 2. Raise arms out to sides. 3. Lower slowly.", "Slight bend in elbows.", "Swinging torso.", "Use light weight.", "12-20", "60s", 8, "Resistance", "Isolation, Upper Body", 0, 0L, 7, 10, 9, 6, "Pull", "https://example.com/lateralraise.jpg", null, null, "Start with hands down", "Raise to shoulder height", "", "1-1-2", 1, null),
                arrayOf<Any>("Bent-over Row", "Hinge at the hips and pull the barbell toward your torso.", "Back", "Barbell", "Intermediate", "Biceps, Core", "1. Hinge forward at hips. 2. Keep back straight. 3. Pull bar to stomach. 4. Lower bar.", "Squeeze shoulder blades together.", "Rounding the lower back.", "Keep spine neutral.", "8-12", "90s", 15, "Resistance", "Pull, Upper Body", 0, 0L, 0, 7, 5, 8, "Pull", "https://example.com/bentoverrow.jpg", null, null, "Hinge at hips, keep back flat", "Pull bar to stomach", "", "2-0-2", 1, null),
                arrayOf<Any>("Plank", "Hold a straight-body position supported on your forearms and toes.", "Core", "Bodyweight", "Beginner", "Shoulders, Glutes", "1. Rest on forearms and toes. 2. Keep body in a straight line. 3. Hold position.", "Breathe steadily.", "Hips too high or too low.", "Stop if lower back hurts.", "30-60s", "60s", 5, "Bodyweight", "Core, Home", 0, 0L, 0, 0, 0, 3, "Isolation", "https://example.com/plank.jpg", null, null, "Rest on forearms and feet", "Keep body straight, don't let hips sag", "", "3-0-0", 0, null),
                arrayOf<Any>("Deadlift", "Hinge at the hips and lift the loaded barbell to a standing position.", "Back", "Barbell", "Advanced", "Glutes, Hamstrings, Core", "1. Stand with mid-foot under bar. 2. Bend and grab bar. 3. Lift chest and straighten back. 4. Stand up with the weight.", "Keep bar close to legs.", "Rounding the back.", "Keep spine neutral to avoid injury.", "3-8", "180s", 25, "Powerlifting", "Pull, Full Body", 0, 0L, 0, 3, 4, 9, "Pull", "https://example.com/deadlift.jpg", null, null, "Stand with bar over mid-foot", "Stand up keeping back straight", "", "1-0-1", 1, null),
                arrayOf<Any>("Bicep Curl", "Curl the dumbbells toward your shoulders with your elbows pinned to your sides.", "Arms", "Dumbbell", "Beginner", "Forearms", "1. Stand with dumbbells at sides. 2. Curl weights up. 3. Lower slowly.", "Keep elbows stationary.", "Using back momentum.", "Control the eccentric phase.", "10-15", "60s", 8, "Resistance", "Isolation, Arms", 0, 0L, 0, 1, 0, 7, "Isolation", "https://example.com/bicepcurl.jpg", null, null, "Stand with hands at sides", "Curl 90 degrees, lower slowly", "", "2-2-2", 1, null),
                arrayOf<Any>("Pull-up", "Pull your body up to a bar until your chin is over it.", "Back", "Bodyweight", "Intermediate", "Biceps, Core", "1. Hang from bar. 2. Pull body up until chin clears bar. 3. Lower with control.", "Engage lats first.", "Kicking legs.", "Use assistance if needed.", "5-15", "120s", 15, "Bodyweight", "Pull, Upper Body", 0, 0L, 0, 6, 5, 8, "Pull", "https://example.com/pulldown.jpg", null, null, "Hang from bar with arms extended", "Pull up until chin over bar", "", "2-1-3", 1, null),
                arrayOf<Any>("Leg Press", "Press weight away from you using your legs on a machine.", "Legs", "Machine", "Beginner", "Glutes, Calves", "1. Sit in machine. 2. Place feet on sled. 3. Lower sled. 4. Press sled up.", "Don't lock knees.", "Lifting hips off pad.", "Use safety stops.", "10-15", "90s", 15, "Machine", "Lower Body", 0, 0L, 9, 5, 0, 4, "Push", "https://example.com/legpress.jpg", null, null, "Sit and place feet on sled", "Press to full extension", "", "2-1-1", 1, null),
                arrayOf<Any>("Tricep Extension", "Extend your arms against resistance to work the triceps.", "Arms", "Cable", "Beginner", "Shoulders", "1. Grip cable attachment. 2. Keep elbows tucked. 3. Extend arms down. 4. Return slowly.", "Squeeze at bottom.", "Moving elbows.", "Don't use too much weight.", "12-15", "60s", 8, "Cable", "Isolation, Arms", 0, 0L, 0, 1, 0, 7, "Isolation", "https://example.com/tricepext.jpg", null, null, "Grip attachment above head", "Extend arms down, return slowly", "", "1-2-3", 1, null),
                arrayOf<Any>("Lunge", "Step forward and lower your hips until both knees are bent at 90 degrees.", "Legs", "Bodyweight", "Beginner", "Glutes, Core", "1. Step forward. 2. Lower hips. 3. Push back up.", "Keep torso upright.", "Knee going past toes.", "Maintain balance.", "10-20", "60s", 12, "Bodyweight", "Lower Body, Home", 0, 0L, 8, 6, 0, 5, "Push", "https://example.com/lunge.jpg", null, null, "Step forward and lower", "Push back to starting position", "", "2-1-1", 1, null),
                arrayOf<Any>("Crunch", "Curl your shoulders toward your pelvis while lying on your back.", "Core", "Bodyweight", "Beginner", "Obliques", "1. Lie on back. 2. Curl shoulders up. 3. Lower back down.", "Don't pull neck.", "Using momentum.", "Perform on a mat.", "15-25", "45s", 5, "Bodyweight", "Core, Home", 0, 0L, 0, 0, 0, 2, "Isolation", "https://example.com/crunch.jpg", null, null, "Lie on back, knees bent", "Curl shoulders off floor", "", "2-1-2", 0, null),
                arrayOf<Any>("Calf Raise", "Raise your heels off the ground to work the calves.", "Legs", "Machine", "Beginner", "None", "1. Stand on edge of step. 2. Lower heels. 3. Raise heels high.", "Full stretch at bottom.", "Bouncing.", "Use support for balance.", "15-20", "60s", 5, "Machine", "Isolation, Lower Body", 0, 0L, 8, 5, 0, 3, "Isolation", "https://example.com/calfraise.jpg", null, null, "Stand on step, let heels drop", "Raise as high as possible", "", "2-2-2", 1, null),
                arrayOf<Any>("Lat Pulldown", "Pull the bar down to your chest while seated.", "Back", "Cable", "Beginner", "Biceps", "1. Sit at machine. 2. Grip bar wide. 3. Pull bar to upper chest. 4. Return slowly.", "Pull with lats.", "Leaning back too far.", "Control the weight.", "8-12", "90s", 12, "Cable", "Pull, Upper Body", 0, 0L, 0, 6, 5, 8, "Pull", "https://example.com/larpulldown.jpg", null, null, "Sit and grip bar wider than shoulders", "Pull down to chest", "", "2-1-2", 1, null),
                arrayOf<Any>("Leg Curl", "Curl your legs against resistance to work the hamstrings.", "Legs", "Machine", "Beginner", "Calves", "1. Lie on machine. 2. Curl weight up. 3. Lower slowly.", "Keep hips down.", "Using momentum.", "Adjust machine to fit.", "10-15", "60s", 10, "Machine", "Isolation, Lower Body", 0, 0L, 9, 3, 0, 8, "Isolation", "https://example.com/legcurl.jpg", null, null, "Lie face down, adjust position", "Curl legs toward buttocks", "", "2-2-2", 1, null),
                arrayOf<Any>("Leg Extension", "Extend your legs against resistance to work the quads.", "Legs", "Machine", "Beginner", "None", "1. Sit on machine. 2. Extend legs. 3. Lower slowly.", "Squeeze at top.", "Swinging the weight.", "Don't hyperextend knees.", "10-15", "60s", 10, "Machine", "Isolation, Lower Body", 0, 0L, 9, 5, 0, 4, "Push", "https://example.com/legext.jpg", null, null, "Sit and adjust machine position", "Extend legs to full extension", "", "2-0-2", 1, null),
                arrayOf<Any>("Dumbbell Fly", "Raise dumbbells to the side to work the shoulders.", "Shoulders", "Dumbbell", "Beginner", "Traps", "1. Stand with dumbbells. 2. Raise to sides. 3. Lower slowly.", "Slight elbow bend.", "Shrugging shoulders.", "Use light weights.", "12-15", "60s", 8, "Resistance", "Isolation, Upper Body", 0, 0L, 7, 10, 9, 6, "Isolation", "https://example.com/dumbbellfly.jpg", null, null, "Start with hands at sides", "Raise to shoulder height", "", "1-1-2", 1, null),
                arrayOf<Any>("Barbell Curl", "Curl a barbell to work the biceps.", "Arms", "Barbell", "Intermediate", "Forearms", "1. Stand holding bar. 2. Curl bar up. 3. Lower slowly.", "Keep elbows tucked.", "Swinging back.", "Control the descent.", "8-12", "90s", 10, "Resistance", "Isolation, Arms", 0, 0L, 0, 2, 0, 7, "Isolation", "https://example.com/barbellcurl.jpg", null, null, "Stand and grip bar at shoulders", "Curl bar to shoulders", "", "2-2-2", 1, null)
            )
            seedData.forEach { row ->
                db.execSQL(
                    "INSERT INTO exercises (name, description, muscleGroup, equipment, difficulty, secondaryMuscles, instructions, tips, commonMistakes, safetyNotes, recommendedRepRange, recommendedRestTime, estimatedCalories, category, tags, isFavorite, lastViewed, vtaper_lat, vtaper_lateral_delt, vtaper_upper_chest, vtaper_rear_delt, movement_pattern, image_url, video_url, animation_url, setup_instructions, execution_instructions, breathing_instructions, tempo_guidance, beginner_variant_id, advanced_variant_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    row
                )
            }
        }
    }
}