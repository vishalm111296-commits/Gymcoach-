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
        ExerciseAliasEntity::class
    ],
    version = 5,
    exportSchema = true
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
                database.execSQL("CREATE TABLE IF NOT EXISTS `body_measurements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userProfileId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `weight` REAL NOT NULL, `height` REAL, `bodyFatPercent` REAL, `muscleMass` REAL, `waistCircumference` REAL, `chestCircumference` REAL, `armCircumference` REAL, `legCircumference` REAL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `favorite_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userProfileId` INTEGER NOT NULL, `exerciseId` INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `exercise_substitutions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `primaryExerciseId` INTEGER NOT NULL, `substituteExerciseId` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `muscles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `nameEn` TEXT NOT NULL, `isPrimary` INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `equipment` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, ` imageUrl` TEXT)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `exercise_muscles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `exerciseId` INTEGER NOT NULL, `muscleId` INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `exercise_equipment` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `exerciseId` INTEGER NOT NULL, `equipmentId` INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `exercise_aliases` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `primaryId` INTEGER NOT NULL, `aliasName` TEXT NOT NULL, `creationDate` INTEGER NOT NULL)")

                database.execSQL("CREATE INDEX IF NOT EXISTS `index_workouts_user_id` ON `workouts` (`id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_exercises_workout_id` ON `workout_exercises` (`workoutId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_exercises_exercise_id` ON `workout_exercises` (`exerciseId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sets_workout_exercise_id` ON `workout_sets` (`workoutExerciseId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_programs_user_profile_id` ON `programs` (`userProfileId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_program_days_program_id` ON `program_days` (`programId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_program_exercises_program_day_id` ON `program_exercises` (`programDayId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_personal_records_user_profile_id` ON `personal_records` (`userProfileId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_personal_records_exercise_id` ON `personal_records` (`exerciseId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_body_measurements_user_profile_id` ON `body_measurements` (`userProfileId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_favorite_exercises_user_profile_id` ON `favorite_exercises` (`userProfileId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_favorite_exercises_exercise_id` ON `favorite_exercises` (`exerciseId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_substitutions_primary_exercise_id` ON `exercise_substitutions` (`primaryExerciseId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_substitutions_substitute_exercise_id` ON `exercise_substitutions` (`substituteExerciseId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_muscles_primary` ON `muscles` (`isPrimary`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_equipment_category` ON `equipment` (`category`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_muscles_exercise_id` ON `exercise_muscles` (`exerciseId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_muscles_muscle_id` ON `exercise_muscles` (`muscleId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_equipment_exercise_id` ON `exercise_equipment` (`exerciseId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_equipment_equipment_id` ON `exercise_equipment` (`equipmentId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_aliases_primary_id` ON `exercise_aliases` (`primaryId`)")
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // No schema changes needed — all tables/columns created in MIGRATION_2_3
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // No schema changes needed — all tables/columns created in MIGRATION_2_3
            }
        }

        fun create(context: Context): GymCoachDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                GymCoachDatabase::class.java,
                "gymcoach.db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration(false)
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
                arrayOf<Any>("Bench Press", "Lie on a bench and press the barbell from your chest to full arm extension.", "Chest", "Barbell", "Intermediate", "Triceps, Shoulders", "1. Lie flat on bench. 2. Grip bar slightly wider than shoulder width. 3. Lower bar to mid-chest. 4. Press bar up.", "Keep feet flat on floor.", "Bouncing bar off chest.", "Use a spotter.", "8-12", "90s", 15, "Powerlifting", "Push, Upper Body", 0, 0L),
                arrayOf<Any>("Squat", "Lower your hips until your thighs are parallel to the floor, then stand back up.", "Legs", "Barbell", "Beginner", "Glutes, Core", "1. Stand with feet shoulder-width apart. 2. Rest bar on upper back. 3. Bend knees and drop hips. 4. Push through heels to stand.", "Keep chest up.", "Knees caving in.", "Use a squat rack.", "5-10", "120s", 20, "Powerlifting", "Lower Body", 0, 0L),
                arrayOf<Any>("Push-up", "Lower your chest toward the floor while keeping your body in a straight line.", "Chest", "Bodyweight", "Beginner", "Triceps, Shoulders, Core", "1. Start in plank position. 2. Lower body until chest touches floor. 3. Push back up.", "Keep core engaged.", "Sagging hips.", "Do not flare elbows too much.", "10-20", "60s", 10, "Bodyweight", "Push, Home", 0, 0L),
                arrayOf<Any>("Shoulder Press", "Press the dumbbells overhead until your arms are fully extended.", "Shoulders", "Dumbbell", "Intermediate", "Triceps", "1. Sit or stand with dumbbells at shoulder height. 2. Press weights overhead. 3. Lower with control.", "Don't arch your back.", "Using momentum.", "Control the weight on the way down.", "8-15", "90s", 12, "Resistance", "Push, Upper Body", 0, 0L),
                arrayOf<Any>("Lateral Raise", "Raise dumbbells out to the sides until your arms are parallel with the floor.", "Shoulders", "Dumbbell", "Beginner", "Traps", "1. Stand with dumbbells at sides. 2. Raise arms out to sides. 3. Lower slowly.", "Slight bend in elbows.", "Swinging torso.", "Use light weight.", "12-20", "60s", 8, "Resistance", "Isolation, Upper Body", 0, 0L),
                arrayOf<Any>("Bent-over Row", "Hinge at the hips and pull the barbell toward your torso.", "Back", "Barbell", "Intermediate", "Biceps, Core", "1. Hinge forward at hips. 2. Keep back straight. 3. Pull bar to stomach. 4. Lower bar.", "Squeeze shoulder blades together.", "Rounding the lower back.", "Keep spine neutral.", "8-12", "90s", 15, "Resistance", "Pull, Upper Body", 0, 0L),
                arrayOf<Any>("Plank", "Hold a straight-body position supported on your forearms and toes.", "Core", "Bodyweight", "Beginner", "Shoulders, Glutes", "1. Rest on forearms and toes. 2. Keep body in a straight line. 3. Hold position.", "Breathe steadily.", "Hips too high or too low.", "Stop if lower back hurts.", "30-60s", "60s", 5, "Bodyweight", "Core, Home", 0, 0L),
                arrayOf<Any>("Deadlift", "Hinge at the hips and lift the loaded barbell to a standing position.", "Back", "Barbell", "Advanced", "Glutes, Hamstrings, Core", "1. Stand with mid-foot under bar. 2. Bend and grab bar. 3. Lift chest and straighten back. 4. Stand up with the weight.", "Keep bar close to legs.", "Rounding the back.", "Keep spine neutral to avoid injury.", "3-8", "180s", 25, "Powerlifting", "Pull, Full Body", 0, 0L),
                arrayOf<Any>("Bicep Curl", "Curl the dumbbells toward your shoulders with your elbows pinned to your sides.", "Arms", "Dumbbell", "Beginner", "Forearms", "1. Stand with dumbbells at sides. 2. Curl weights up. 3. Lower slowly.", "Keep elbows stationary.", "Using back momentum.", "Control the eccentric phase.", "10-15", "60s", 8, "Resistance", "Isolation, Arms", 0, 0L),
                arrayOf<Any>("Pull-up", "Pull your body up to a bar until your chin is over it.", "Back", "Bodyweight", "Intermediate", "Biceps, Core", "1. Hang from bar. 2. Pull body up until chin clears bar. 3. Lower with control.", "Engage lats first.", "Kicking legs.", "Use assistance if needed.", "5-15", "120s", 15, "Bodyweight", "Pull, Upper Body", 0, 0L),
                arrayOf<Any>("Leg Press", "Press weight away from you using your legs on a machine.", "Legs", "Machine", "Beginner", "Glutes, Calves", "1. Sit in machine. 2. Place feet on sled. 3. Lower sled. 4. Press sled up.", "Don't lock knees.", "Lifting hips off pad.", "Use safety stops.", "10-15", "90s", 15, "Machine", "Lower Body", 0, 0L),
                arrayOf<Any>("Tricep Extension", "Extend your arms against resistance to work the triceps.", "Arms", "Cable", "Beginner", "Shoulders", "1. Grip cable attachment. 2. Keep elbows tucked. 3. Extend arms down. 4. Return slowly.", "Squeeze at bottom.", "Moving elbows.", "Don't use too much weight.", "12-15", "60s", 8, "Cable", "Isolation, Arms", 0, 0L),
                arrayOf<Any>("Lunge", "Step forward and lower your hips until both knees are bent at 90 degrees.", "Legs", "Bodyweight", "Beginner", "Glutes, Core", "1. Step forward. 2. Lower hips. 3. Push back up.", "Keep torso upright.", "Knee going past toes.", "Maintain balance.", "10-20", "60s", 12, "Bodyweight", "Lower Body, Home", 0, 0L),
                arrayOf<Any>("Crunch", "Curl your shoulders toward your pelvis while lying on your back.", "Core", "Bodyweight", "Beginner", "Obliques", "1. Lie on back. 2. Curl shoulders up. 3. Lower back down.", "Don't pull neck.", "Using momentum.", "Perform on a mat.", "15-25", "45s", 5, "Bodyweight", "Core, Home", 0, 0L),
                arrayOf<Any>("Calf Raise", "Raise your heels off the ground to work the calves.", "Legs", "Machine", "Beginner", "None", "1. Stand on edge of step. 2. Lower heels. 3. Raise heels high.", "Full stretch at bottom.", "Bouncing.", "Use support for balance.", "15-20", "60s", 5, "Machine", "Isolation, Lower Body", 0, 0L),
                arrayOf<Any>("Lat Pulldown", "Pull the bar down to your chest while seated.", "Back", "Cable", "Beginner", "Biceps", "1. Sit at machine. 2. Grip bar wide. 3. Pull bar to upper chest. 4. Return slowly.", "Pull with lats.", "Leaning back too far.", "Control the weight.", "8-12", "90s", 12, "Cable", "Pull, Upper Body", 0, 0L),
                arrayOf<Any>("Leg Curl", "Curl your legs against resistance to work the hamstrings.", "Legs", "Machine", "Beginner", "Calves", "1. Lie on machine. 2. Curl weight up. 3. Lower slowly.", "Keep hips down.", "Using momentum.", "Adjust machine to fit.", "10-15", "60s", 10, "Machine", "Isolation, Lower Body", 0, 0L),
                arrayOf<Any>("Leg Extension", "Extend your legs against resistance to work the quads.", "Legs", "Machine", "Beginner", "None", "1. Sit on machine. 2. Extend legs. 3. Lower slowly.", "Squeeze at top.", "Swinging the weight.", "Don't hyperextend knees.", "10-15", "60s", 10, "Machine", "Isolation, Lower Body", 0, 0L),
                arrayOf<Any>("Dumbbell Fly", "Raise dumbbells to the side to work the shoulders.", "Shoulders", "Dumbbell", "Beginner", "Traps", "1. Stand with dumbbells. 2. Raise to sides. 3. Lower slowly.", "Slight elbow bend.", "Shrugging shoulders.", "Use light weights.", "12-15", "60s", 8, "Resistance", "Isolation, Upper Body", 0, 0L),
                arrayOf<Any>("Barbell Curl", "Curl a barbell to work the biceps.", "Arms", "Barbell", "Intermediate", "Forearms", "1. Stand holding bar. 2. Curl bar up. 3. Lower slowly.", "Keep elbows tucked.", "Swinging back.", "Control the descent.", "8-12", "90s", 10, "Resistance", "Isolation, Arms", 0, 0L)
            )
            seedData.forEach { row ->
                db.execSQL(
                    "INSERT INTO exercises (name, description, muscleGroup, equipment, difficulty, secondaryMuscles, instructions, tips, commonMistakes, safetyNotes, recommendedRepRange, recommendedRestTime, estimatedCalories, category, tags, isFavorite, lastViewed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    row
                )
            }
        }
    }
}