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
import com.gymcoach.app.data.local.dao.GoalDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutEntity
import com.gymcoach.app.data.local.entity.WorkoutExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import com.gymcoach.app.data.local.entity.UserProfileEntity
import com.gymcoach.app.data.local.entity.MeasurementRecordEntity
import com.gymcoach.app.data.local.entity.GoalEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class,
        UserProfileEntity::class,
        MeasurementRecordEntity::class,
        GoalEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class GymCoachDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile
        private var INSTANCE: GymCoachDatabase? = null

        fun getDatabase(context: Context): GymCoachDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GymCoachDatabase::class.java,
                    "gymcoach_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    seedExercises(database.exerciseDao())
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedExercises(exerciseDao: ExerciseDao) {
            if (exerciseDao.getExerciseCount() == 0) {
                val seedData = listOf(
                    ExerciseEntity(name = "Bench Press", description = "Lie on a bench and press the barbell from your chest to full arm extension.", muscleGroup = "Chest", equipment = "Barbell", difficulty = "Intermediate", secondaryMuscles = "Triceps, Shoulders", instructions = "1. Lie flat on bench. 2. Grip bar slightly wider than shoulder width. 3. Lower bar to mid-chest. 4. Press bar up.", tips = "Keep feet flat on floor.", commonMistakes = "Bouncing bar off chest.", safetyNotes = "Use a spotter.", recommendedRepRange = "8-12", recommendedRestTime = "90s", estimatedCalories = 15, category = "Powerlifting", tags = "Push, Upper Body", isFavorite = false, lastViewed = 0L),
                    ExerciseEntity(name = "Squat", description = "Lower your hips until your thighs are parallel to the floor, then stand back up.", muscleGroup = "Legs", equipment = "Barbell", difficulty = "Beginner", secondaryMuscles = "Glutes, Core", instructions = "1. Stand with feet shoulder-width apart. 2. Rest bar on upper back. 3. Bend knees and drop hips. 4. Push through heels to stand.", tips = "Keep chest up.", commonMistakes = "Knees caving in.", safetyNotes = "Use a squat rack.", recommendedRepRange = "5-10", recommendedRestTime = "120s", estimatedCalories = 20, category = "Powerlifting", tags = "Lower Body", isFavorite = false, lastViewed = 0L),
                    ExerciseEntity(name = "Push-up", description = "Lower your chest toward the floor while keeping your body in a straight line.", muscleGroup = "Chest", equipment = "Bodyweight", difficulty = "Beginner", secondaryMuscles = "Triceps, Shoulders, Core", instructions = "1. Start in plank position. 2. Lower body until chest touches floor. 3. Push back up.", tips = "Keep core engaged.", commonMistakes = "Sagging hips.", safetyNotes = "Do not flare elbows too much.", recommendedRepRange = "10-20", recommendedRestTime = "60s", estimatedCalories = 10, category = "Bodyweight", tags = "Push, Home", isFavorite = false, lastViewed = 0L),
                    ExerciseEntity(name = "Shoulder Press", description = "Press the dumbbells overhead until your arms are fully extended.", muscleGroup = "Shoulders", equipment = "Dumbbell", difficulty = "Intermediate", secondaryMuscles = "Triceps", instructions = "1. Sit or stand with dumbbells at shoulder height. 2. Press weights overhead. 3. Lower with control.", tips = "Don't arch your back.", commonMistakes = "Using momentum.", safetyNotes = "Control the weight on the way down.", recommendedRepRange = "8-15", recommendedRestTime = "90s", estimatedCalories = 12, category = "Resistance", tags = "Push, Upper Body", isFavorite = false, lastViewed = 0L),
                    ExerciseEntity(name = "Lateral Raise", description = "Raise dumbbells out to the sides until your arms are parallel with the floor.", muscleGroup = "Shoulders", equipment = "Dumbbell", difficulty = "Beginner", secondaryMuscles = "Traps", instructions = "1. Stand with dumbbells at sides. 2. Raise arms out to sides. 3. Lower slowly.", tips = "Slight bend in elbows.", commonMistakes = "Swinging torso.", safetyNotes = "Use light weight.", recommendedRepRange = "12-20", recommendedRestTime = "60s", estimatedCalories = 8, category = "Resistance", tags = "Isolation, Upper Body", isFavorite = false, lastViewed = 0L),
                    ExerciseEntity(name = "Bent-over Row", description = "Hinge at the hips and pull the barbell toward your torso.", muscleGroup = "Back", equipment = "Barbell", difficulty = "Intermediate", secondaryMuscles = "Biceps, Core", instructions = "1. Hinge forward at hips. 2. Keep back straight. 3. Pull bar to stomach. 4. Lower bar.", tips = "Squeeze shoulder blades together.", commonMistakes = "Rounding the lower back.", safetyNotes = "Keep spine neutral.", recommendedRepRange = "8-12", recommendedRestTime = "90s", estimatedCalories = 15, category = "Resistance", tags = "Pull, Upper Body", isFavorite = false, lastViewed = 0L),
                    ExerciseEntity(name = "Plank", description = "Hold a straight-body position supported on your forearms and toes.", muscleGroup = "Core", equipment = "Bodyweight", difficulty = "Beginner", secondaryMuscles = "Shoulders, Glutes", instructions = "1. Rest on forearms and toes. 2. Keep body in a straight line. 3. Hold position.", tips = "Breathe steadily.", commonMistakes = "Hips too high or too low.", safetyNotes = "Stop if lower back hurts.", recommendedRepRange = "30-60s", recommendedRestTime = "60s", estimatedCalories = 5, category = "Bodyweight", tags = "Core, Home", isFavorite = false, lastViewed = 0L),
                    ExerciseEntity(name = "Deadlift", description = "Hinge at the hips and lift the loaded barbell to a standing position.", muscleGroup = "Back", equipment = "Barbell", difficulty = "Advanced", secondaryMuscles = "Glutes, Hamstrings, Core", instructions = "1. Stand with mid-foot under bar. 2. Bend and grab bar. 3. Lift chest and straighten back. 4. Stand up with the weight.", tips = "Keep bar close to legs.", commonMistakes = "Rounding the back.", safetyNotes = "Keep spine neutral to avoid injury.", recommendedRepRange = "3-8", recommendedRestTime = "180s", estimatedCalories = 25, category = "Powerlifting", tags = "Pull, Full Body", isFavorite = false, lastViewed = 0L),
                    ExerciseEntity(name = "Bicep Curl", description = "Curl the dumbbells toward your shoulders with your elbows pinned to your sides.", muscleGroup = "Arms", equipment = "Dumbbell", difficulty = "Beginner", secondaryMuscles = "Forearms", instructions = "1. Stand with dumbbells at sides. 2. Curl weights up. 3. Lower slowly.", tips = "Keep elbows stationary.", commonMistakes = "Using back momentum.", safetyNotes = "Control the eccentric phase.", recommendedRepRange = "10-15", recommendedRestTime = "60s", estimatedCalories = 8, category = "Resistance", tags = "Isolation, Arms", isFavorite = false, lastViewed = 0L),
                    ExerciseEntity(name = "Pull-up", description = "Pull your body up to a bar until your chin is over it.", muscleGroup = "Back", equipment = "Bodyweight", difficulty = "Intermediate", secondaryMuscles = "Biceps, Core", instructions = "1. Hang from bar. 2. Pull body up until chin clears bar. 3. Lower with control.", tips = "Engage lats first.", commonMistakes = "Kicking legs.", safetyNotes = "Use assistance if needed.", recommendedRepRange = "5-15", recommendedRestTime = "120s", estimatedCalories = 15, category = "Bodyweight", tags = "Pull, Upper Body", isFavorite = false, lastViewed = 0L),
                    ExerciseEntity(name = "Leg Press", description = "Press weight away from you using your legs on a machine.", muscleGroup = "Legs", equipment = "Machine", difficulty = "Beginner", secondaryMuscles = "Glutes, Calves", instructions = "1. Sit in machine. 2. Place feet on sled. 3. Lower sled. 4. Press sled up.", tips = "Don't lock knees.", commonMistakes = "Lifting hips off pad.", safetyNotes = "Use safety stops.", recommendedRepRange = "10-15", recommendedRestTime = "90s", estimatedCalories = 15, category = "Machine", tags = "Lower Body", isFavorite = false, lastViewed = 0L),
                    ExerciseEntity(name = "Tricep Extension", description = "Extend your arms against resistance to work the triceps.", muscleGroup = "Arms", equipment = "Cable", difficulty = "Beginner", secondaryMuscles = "Shoulders", instructions = "1. Grip cable attachment. 2. Keep elbows tucked. 3. Extend arms down. 4. Return slowly.", tips = "Squeeze at bottom.", commonMistakes = "Moving elbows.", safetyNotes = "Don't use too much weight.", recommendedRepRange = "12-15", recommendedRestTime = "60s", estimatedCalories = 8, category = "Cable", tags = "Isolation, Arms", isFavorite = false, lastViewed = 0L),
                    ExerciseEntity(name = "Lunge", description = "Step forward and lower your hips until both knees are bent at 90 degrees.", muscleGroup = "Legs", equipment = "Bodyweight", difficulty = "Beginner", secondaryMuscles = "Glutes, Core", instructions = "1. Step forward. 2. Lower hips. 3. Push back up.", tips = "Keep torso upright.", commonMistakes = "Knee going past toes.", safetyNotes = "Maintain balance.", recommendedRepRange = "10-20", recommendedRestTime = "60s", estimatedCalories = 12, category = "Bodyweight", tags = "Lower Body, Home", isFavorite = false, lastViewed = 0L),
                    ExerciseEntity(name = "Crunch", description = "Curl your shoulders toward your pelvis while lying on your back.", muscleGroup = "Core", equipment = "Bodyweight", difficulty = "Beginner", secondaryMuscles = "Obliques", instructions = "1. Lie on back. 2. Curl shoulders up. 3. Lower back down.", tips = "Don't pull neck.", commonMistakes = "Using momentum.", safetyNotes = "Perform on a mat.", recommendedRepRange = "15-25", recommendedRestTime = "45s", estimatedCalories = 5, category = "Bodyweight", tags = "Core, Home", isFavorite = false, lastViewed = 0L),
                    ExerciseEntity(name = "Calf Raise", description = "Raise your heels off the ground to work the calves.", muscleGroup = "Legs", equipment = "Machine", difficulty = "Beginner", secondaryMuscles = "None", instructions = "1. Stand on edge of step. 2. Lower heels. 3. Raise heels high.", tips = "Full stretch at bottom.", commonMistakes = "Bouncing.", safetyNotes = "Use support for balance.", recommendedRepRange = "15-20", recommendedRestTime = "60s", estimatedCalories = 5, category = "Machine", tags = "Isolation, Lower Body", isFavorite = false, lastViewed = 0L),
                    ExerciseEntity(name = "Lat Pulldown", description = "Pull the bar down to your chest while seated.", muscleGroup = "Back", equipment = "Cable", difficulty = "Beginner", secondaryMuscles = "Biceps", instructions = "1. Sit at machine. 2. Grip bar wide. 3. Pull bar to upper chest. 4. Return slowly.", tips = "Pull with lats.", commonMistakes = "Leaning back too far.", safetyNotes = "Control the weight.", recommendedRepRange = "8-12", recommendedRestTime = "90s", estimatedCalories = 12, category = "Cable", tags = "Pull, Upper Body", isFavorite = false, lastViewed = 0L),
                    ExerciseEntity(name = "Leg Curl", description = "Curl your legs against resistance to work the hamstrings.", muscleGroup = "Legs", equipment = "Machine", difficulty = "Beginner", secondaryMuscles = "Calves", instructions = "1. Lie on machine. 2. Curl weight up. 3. Lower slowly.", tips = "Keep hips down.", commonMistakes = "Using momentum.", safetyNotes = "Adjust machine to fit.", recommendedRepRange = "10-15", recommendedRestTime = "60s", estimatedCalories = 10, category = "Machine", tags = "Isolation, Lower Body", isFavorite = false, lastViewed = 0L),
                    ExerciseEntity(name = "Leg Extension", description = "Extend your legs against resistance to work the quads.", muscleGroup = "Legs", equipment = "Machine", difficulty = "Beginner", secondaryMuscles = "None", instructions = "1. Sit on machine. 2. Extend legs. 3. Lower slowly.", tips = "Squeeze at top.", commonMistakes = "Swinging the weight.", safetyNotes = "Don't hyperextend knees.", recommendedRepRange = "10-15", recommendedRestTime = "60s", estimatedCalories = 10, category = "Machine", tags = "Isolation, Lower Body", isFavorite = false, lastViewed = 0L),
                    ExerciseEntity(name = "Dumbbell Fly", description = "Raise dumbbells to the side to work the shoulders.", muscleGroup = "Shoulders", equipment = "Dumbbell", difficulty = "Beginner", secondaryMuscles = "Traps", instructions = "1. Stand with dumbbells. 2. Raise to sides. 3. Lower slowly.", tips = "Slight elbow bend.", commonMistakes = "Shrugging shoulders.", safetyNotes = "Use light weights.", recommendedRepRange = "12-15", recommendedRestTime = "60s", estimatedCalories = 8, category = "Resistance", tags = "Isolation, Upper Body", isFavorite = false, lastViewed = 0L),
                    ExerciseEntity(name = "Barbell Curl", description = "Curl a barbell to work the biceps.", muscleGroup = "Arms", equipment = "Barbell", difficulty = "Intermediate", secondaryMuscles = "Forearms", instructions = "1. Stand holding bar. 2. Curl bar up. 3. Lower slowly.", tips = "Keep elbows tucked.", commonMistakes = "Swinging back.", safetyNotes = "Control the descent.", recommendedRepRange = "8-12", recommendedRestTime = "90s", estimatedCalories = 10, category = "Resistance", tags = "Isolation, Arms", isFavorite = false, lastViewed = 0L)
                )
                exerciseDao.insertAllExercises(seedData)
            }
        }
    }
}
