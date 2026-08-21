package com.gymcoach.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.dao.UserProfileDao
import com.gymcoach.app.data.local.dao.MeasurementDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutEntity
import com.gymcoach.app.data.local.entity.WorkoutExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import com.gymcoach.app.data.local.entity.UserProfileEntity
import com.gymcoach.app.data.local.entity.MeasurementRecordEntity

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class,
        UserProfileEntity::class,
        MeasurementRecordEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class GymCoachDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun measurementDao(): MeasurementDao

    companion object {
        @Volatile
        private var INSTANCE: GymCoachDatabase? = null

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `user_profile` (`id` INTEGER NOT NULL DEFAULT 1, `name` TEXT NOT NULL DEFAULT '', `age` INTEGER NOT NULL DEFAULT 0, `gender` TEXT NOT NULL DEFAULT '', `height` REAL NOT NULL DEFAULT 0.0, `weight` REAL NOT NULL DEFAULT 0.0, `goalWeight` REAL NOT NULL DEFAULT 0.0, `currentGoal` TEXT NOT NULL DEFAULT '', `experience` TEXT NOT NULL DEFAULT '', `trainingStyle` TEXT NOT NULL DEFAULT '', `preferredSplit` TEXT NOT NULL DEFAULT '', `activityLevel` TEXT NOT NULL DEFAULT '', `weeklyWorkoutGoal` INTEGER NOT NULL DEFAULT 0, `proteinGoal` REAL NOT NULL DEFAULT 0.0, `caloriesGoal` INTEGER NOT NULL DEFAULT 0, `units` TEXT NOT NULL DEFAULT 'metric', `avatarUrl` TEXT NOT NULL DEFAULT '', `leanBodyMass` REAL NOT NULL DEFAULT 0.0, `maintenanceCalories` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `measurement_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` TEXT NOT NULL, `measurementType` TEXT NOT NULL, `value` REAL NOT NULL, `unit` TEXT NOT NULL, `date` INTEGER NOT NULL, `notes` TEXT, `createdAt` INTEGER NOT NULL)")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columns = mutableSetOf<String>()
                db.query("PRAGMA table_info(`workouts`)").use { cursor ->
                    while (cursor.moveToNext()) {
                        columns.add(cursor.getString(1))
                    }
                }
                if ("mood" !in columns) db.execSQL("ALTER TABLE `workouts` ADD COLUMN `mood` INTEGER")
                if ("energy" !in columns) db.execSQL("ALTER TABLE `workouts` ADD COLUMN `energy` INTEGER")
                if ("pain" !in columns) db.execSQL("ALTER TABLE `workouts` ADD COLUMN `pain` INTEGER")
                if ("isTemplate" !in columns) db.execSQL("ALTER TABLE `workouts` ADD COLUMN `isTemplate` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No schema changes, just data seeding updates which are handled in onCreate.
                // We add an empty migration to avoid destructive migration when version bumps.
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `equipment` TEXT NOT NULL DEFAULT 'Dumbbell,Flat Bench,Bodyweight'")
            }
        }

        fun getDatabase(context: Context): GymCoachDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GymCoachDatabase::class.java,
                    "gymcoach_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .addCallback(SeedCallback)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private object SeedCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                seedExercises(db)
            }
        }

        private fun seedExercises(db: SupportSQLiteDatabase) {
            val seedData = listOf(
                arrayOf("Dumbbell Bench Press", "Lie on a flat bench and press dumbbells from chest level to full arm extension.", "Chest", "Dumbbell", "Beginner", "Triceps, Shoulders", "1. Lie flat on a bench holding dumbbells at chest level. 2. Press dumbbells up. 3. Lower with control.", "Keep feet flat on floor.", "Bouncing/colliding dumbbells.", "Ensure firm grip.", "8-12", "90s", 12, "Resistance", "Push, Upper Body", 0, 0L),
                arrayOf("One-Arm Dumbbell Row", "Support knee and hand on a bench, pull dumbbell to hip.", "Back", "Dumbbell", "Beginner", "Biceps, Shoulders", "1. Place one knee and hand on bench. 2. Pull dumbbell up to hip. 3. Lower with control.", "Keep back flat.", "Rounding spine.", "Do not twist torso.", "8-12", "90s", 12, "Resistance", "Pull, Upper Body", 0, 0L),
                arrayOf("Dumbbell Pullover", "Lie across a bench, lower a dumbbell behind head, pull back up.", "Back", "Dumbbell", "Intermediate", "Chest, Triceps", "1. Lie on bench holding a dumbbell overhead. 2. Lower dumbbell behind head. 3. Pull back over chest.", "Keep core engaged.", "Bending elbows too much.", "Use secure grip.", "10-12", "90s", 10, "Resistance", "Pull, Upper Body", 0, 0L),
                arrayOf("Dumbbell Shoulder Press", "Press the dumbbells overhead until your arms are fully extended.", "Shoulders", "Dumbbell", "Intermediate", "Triceps", "1. Sit or stand with dumbbells at shoulder height. 2. Press weights overhead. 3. Lower with control.", "Don't arch your back.", "Using momentum.", "Control the weight on the way down.", "8-15", "90s", 12, "Resistance", "Push, Upper Body", 0, 0L),
                arrayOf("Dumbbell Lateral Raise", "Raise dumbbells out to the sides until your arms are parallel with the floor.", "Shoulders", "Dumbbell", "Beginner", "Traps", "1. Stand with dumbbells at sides. 2. Raise arms out to sides. 3. Lower slowly.", "Slight bend in elbows.", "Swinging torso.", "Use light weight.", "12-20", "60s", 8, "Resistance", "Isolation, Upper Body", 0, 0L),
                arrayOf("Bent-Over Rear Delt Fly", "Hinge at hips, raise dumbbells out to sides.", "Shoulders", "Dumbbell", "Beginner", "Upper Back", "1. Hinge forward at hips. 2. Raise dumbbells out to sides. 3. Lower with control.", "Keep neck neutral.", "Using momentum.", "Keep spine flat.", "12-15", "60s", 8, "Resistance", "Isolation, Upper Body", 0, 0L),
                arrayOf("Concentration Curl", "Sit on bench, support elbow against thigh, curl dumbbell.", "Arms", "Dumbbell", "Beginner", "Forearms", "1. Sit on bench. 2. Rest elbow against inner thigh. 3. Curl dumbbell up.", "Keep arm isolated.", "Moving thigh.", "Squeeze bicep at top.", "10-15", "60s", 8, "Resistance", "Isolation, Arms", 0, 0L),
                arrayOf("Dumbbell Bicep Curl", "Curl the dumbbells toward your shoulders with your elbows pinned to your sides.", "Arms", "Dumbbell", "Beginner", "Forearms", "1. Stand with dumbbells at sides. 2. Curl weights up. 3. Lower slowly.", "Keep elbows stationary.", "Using back momentum.", "Control the eccentric phase.", "10-15", "60s", 8, "Resistance", "Isolation, Arms", 0, 0L),
                arrayOf("Lying Dumbbell Tricep Extension", "Lie on bench, lower dumbbells beside head, extend arms.", "Arms", "Dumbbell", "Intermediate", "Shoulders", "1. Lie flat on bench holding dumbbells up. 2. Lower dumbbells beside ears. 3. Extend arms up.", "Keep elbows pointing up.", "Flaring elbows.", "Control the dumbbells.", "10-15", "90s", 8, "Resistance", "Isolation, Arms", 0, 0L),
                arrayOf("Goblet Squat", "Hold dumbbell at chest, perform a squat.", "Legs", "Dumbbell", "Beginner", "Glutes, Core", "1. Hold dumbbell at chest level. 2. Squat down. 3. Drive up to stand.", "Keep elbows inside knees.", "Rounding back.", "Keep weight close.", "10-15", "90s", 15, "Resistance", "Lower Body", 0, 0L),
                arrayOf("Dumbbell Romanian Deadlift", "Hinge at hips lowering dumbbells down shins.", "Legs", "Dumbbell", "Intermediate", "Hamstrings, Glutes, Core", "1. Stand holding dumbbells. 2. Hinge at hips keeping back flat. 3. Return to stand.", "Keep dumbbells close to legs.", "Rounding back.", "Engage hamstrings.", "8-12", "90s", 15, "Resistance", "Lower Body", 0, 0L),
                arrayOf("Bulgarian Split Squat", "Elevate rear foot, lower hips with dumbbells.", "Legs", "Dumbbell", "Intermediate", "Quads, Glutes", "1. Place rear foot on bench. 2. Lower front thigh to parallel. 3. Drive back up.", "Keep front knee aligned.", "Knee passing toe too far.", "Maintain balance.", "8-12", "90s", 15, "Resistance", "Lower Body", 0, 0L),
                arrayOf("Push-up", "Lower your chest toward the floor while keeping your body in a straight line.", "Chest", "Bodyweight", "Beginner", "Triceps, Shoulders, Core", "1. Start in plank position. 2. Lower body until chest touches floor. 3. Push back up.", "Keep core engaged.", "Sagging hips.", "Do not flare elbows too much.", "10-20", "60s", 10, "Bodyweight", "Push, Home", 0, 0L),
                arrayOf("Plank", "Hold a straight-body position supported on your forearms and toes.", "Core", "Bodyweight", "Beginner", "Shoulders, Glutes", "1. Rest on forearms and toes. 2. Keep body in a straight line. 3. Hold position.", "Breathe steadily.", "Hips too high or too low.", "Stop if lower back hurts.", "30-60s", "60s", 5, "Bodyweight", "Core, Home", 0, 0L),
                arrayOf("Crunch", "Curl your shoulders toward your pelvis while lying on your back.", "Core", "Bodyweight", "Beginner", "Obliques", "1. Lie on back. 2. Curl shoulders up. 3. Lower back down.", "Don't pull neck.", "Using momentum.", "Perform on a mat.", "15-25", "45s", 5, "Bodyweight", "Core, Home", 0, 0L),
                arrayOf("Bench Press", "Lie on a bench and press the barbell from your chest to full arm extension.", "Chest", "Barbell", "Intermediate", "Triceps, Shoulders", "1. Lie flat on bench. 2. Grip bar slightly wider than shoulder width. 3. Lower bar to mid-chest. 4. Press bar up.", "Keep feet flat on floor.", "Bouncing bar off chest.", "Use a spotter.", "8-12", "90s", 15, "Powerlifting", "Push, Upper Body", 0, 0L),
                arrayOf("Squat", "Lower your hips until your thighs are parallel to the floor, then stand back up.", "Legs", "Barbell", "Beginner", "Glutes, Core", "1. Stand with feet shoulder-width apart. 2. Rest bar on upper back. 3. Bend knees and drop hips. 4. Push through heels to stand.", "Keep chest up.", "Knees caving in.", "Use a squat rack.", "5-10", "120s", 20, "Powerlifting", "Lower Body", 0, 0L),
                arrayOf("Bent-over Row", "Hinge at the hips and pull the barbell toward your torso.", "Back", "Barbell", "Intermediate", "Biceps, Core", "1. Hinge forward at hips. 2. Keep back straight. 3. Pull bar to stomach. 4. Lower bar.", "Squeeze shoulder blades together.", "Rounding the lower back.", "Keep spine neutral.", "8-12", "90s", 15, "Resistance", "Pull, Upper Body", 0, 0L),
                arrayOf("Deadlift", "Hinge at the hips and lift the loaded barbell to a standing position.", "Back", "Barbell", "Advanced", "Glutes, Hamstrings, Core", "1. Stand with mid-foot under bar. 2. Bend and grab bar. 3. Lift chest and straighten back. 4. Stand up with the weight.", "Keep bar close to legs.", "Rounding the back.", "Keep spine neutral to avoid injury.", "3-8", "180s", 25, "Powerlifting", "Pull, Full Body", 0, 0L),
                arrayOf("Pull-up", "Pull your body up to a bar until your chin is over it.", "Back", "Bodyweight", "Intermediate", "Biceps, Core", "1. Hang from bar. 2. Pull body up until chin clears bar. 3. Lower with control.", "Engage lats first.", "Kicking legs.", "Use assistance if needed.", "5-15", "120s", 15, "Bodyweight", "Pull, Upper Body", 0, 0L),
                arrayOf("Leg Press", "Press weight away from you using your legs on a machine.", "Legs", "Machine", "Beginner", "Glutes, Calves", "1. Sit in machine. 2. Place feet on sled. 3. Lower sled. 4. Press sled up.", "Don't lock knees.", "Lifting hips off pad.", "Use safety stops.", "10-15", "90s", 15, "Machine", "Lower Body", 0, 0L),
                arrayOf("Tricep Extension", "Extend your arms against resistance to work the triceps.", "Arms", "Cable", "Beginner", "Shoulders", "1. Grip cable attachment. 2. Keep elbows tucked. 3. Extend arms down. 4. Return slowly.", "Squeeze at bottom.", "Moving elbows.", "Don't use too much weight.", "12-15", "60s", 8, "Cable", "Isolation, Arms", 0, 0L),
                arrayOf("Lunge", "Step forward and lower your hips until both knees are bent at 90 degrees.", "Legs", "Bodyweight", "Beginner", "Glutes, Core", "1. Step forward. 2. Lower hips. 3. Push back up.", "Keep torso upright.", "Knee going past toes.", "Maintain balance.", "10-20", "60s", 12, "Bodyweight", "Lower Body, Home", 0, 0L),
                arrayOf("Calf Raise", "Raise your heels off the ground to work the calves.", "Legs", "Machine", "Beginner", "None", "1. Stand on edge of step. 2. Lower heels. 3. Raise heels high.", "Full stretch at bottom.", "Bouncing.", "Use support for balance.", "15-20", "60s", 5, "Machine", "Isolation, Lower Body", 0, 0L),
                arrayOf("Lat Pulldown", "Pull the bar down to your chest while seated.", "Back", "Cable", "Beginner", "Biceps", "1. Sit at machine. 2. Grip bar wide. 3. Pull bar to upper chest. 4. Return slowly.", "Pull with lats.", "Leaning back too far.", "Control the weight.", "8-12", "90s", 12, "Cable", "Pull, Upper Body", 0, 0L),
                arrayOf("Leg Curl", "Curl your legs against resistance to work the hamstrings.", "Legs", "Machine", "Beginner", "Calves", "1. Lie on machine. 2. Curl weight up. 3. Lower slowly.", "Keep hips down.", "Using momentum.", "Adjust machine to fit.", "10-15", "60s", 10, "Machine", "Isolation, Lower Body", 0, 0L),
                arrayOf("Leg Extension", "Extend your legs against resistance to work the quads.", "Legs", "Machine", "Beginner", "None", "1. Sit on machine. 2. Extend legs. 3. Lower slowly.", "Squeeze at top.", "Swinging the weight.", "Don't hyperextend knees.", "10-15", "60s", 10, "Machine", "Isolation, Lower Body", 0, 0L),
                arrayOf("Dumbbell Fly", "Raise dumbbells to the side to work the shoulders.", "Shoulders", "Dumbbell", "Beginner", "Traps", "1. Stand with dumbbells. 2. Raise to sides. 3. Lower slowly.", "Slight elbow bend.", "Shrugging shoulders.", "Use light weights.", "12-15", "60s", 8, "Resistance", "Isolation, Upper Body", 0, 0L),
                arrayOf("Barbell Curl", "Curl a barbell to work the biceps.", "Arms", "Barbell", "Intermediate", "Forearms", "1. Stand holding bar. 2. Curl bar up. 3. Lower slowly.", "Keep elbows tucked.", "Swinging back.", "Control the descent.", "8-12", "90s", 10, "Resistance", "Isolation, Arms", 0, 0L)
            )
            seedData.forEach { row ->
                db.execSQL(
                    "INSERT INTO exercises (name, description, muscleGroup, equipment, difficulty, secondaryMuscles, instructions, tips, commonMistakes, safetyNotes, recommendedRepRange, recommendedRestTime, estimatedCalories, category, tags, isFavorite, lastViewed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    row
                )
            }
        }
    }
}
