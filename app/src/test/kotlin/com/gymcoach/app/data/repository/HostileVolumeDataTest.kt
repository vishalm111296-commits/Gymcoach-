package com.gymcoach.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.ExerciseMuscleDao
import com.gymcoach.app.data.local.dao.MuscleDao
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.database.GymCoachDatabase
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.ExerciseMuscleEntity
import com.gymcoach.app.data.local.entity.MuscleEntity
import com.gymcoach.app.data.local.entity.WorkoutEntity
import com.gymcoach.app.data.local.entity.WorkoutExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import com.gymcoach.app.domain.model.CanonicalMuscle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HostileVolumeDataTest {

    private lateinit var db: GymCoachDatabase
    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var muscleDao: MuscleDao
    private lateinit var exerciseMuscleDao: ExerciseMuscleDao
    private lateinit var repository: WorkoutRepositoryImpl
    private lateinit var volumeCalculator: VolumeCalculator

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        workoutDao = db.workoutDao()
        exerciseDao = db.exerciseDao()
        muscleDao = db.muscleDao()
        exerciseMuscleDao = db.exerciseMuscleDao()

        repository = WorkoutRepositoryImpl(workoutDao, exerciseDao)
        volumeCalculator = VolumeCalculator()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `hostileVolumeDataTest proves zero cross-bucket volume leakage across similar muscle names`() = runTest {
        // 1. Seed canonical muscles into DB
        val latId = muscleDao.insert(MuscleEntity(name = "latissimus_dorsi", displayName = "Lats", bodyRegion = "Back"))
        val latDeltId = muscleDao.insert(MuscleEntity(name = "lateral_deltoid", displayName = "Lateral Deltoid", bodyRegion = "Shoulders"))
        val rearDeltId = muscleDao.insert(MuscleEntity(name = "rear_deltoid", displayName = "Rear Deltoid", bodyRegion = "Shoulders"))
        val upperBackId = muscleDao.insert(MuscleEntity(name = "upper_back", displayName = "Upper Back", bodyRegion = "Back"))
        val upperChestId = muscleDao.insert(MuscleEntity(name = "upper_chest", displayName = "Upper Chest", bodyRegion = "Chest"))
        val chestId = muscleDao.insert(MuscleEntity(name = "chest", displayName = "Chest", bodyRegion = "Chest"))
        val quadId = muscleDao.insert(MuscleEntity(name = "quadriceps", displayName = "Quadriceps", bodyRegion = "Legs"))
        val hamId = muscleDao.insert(MuscleEntity(name = "hamstrings", displayName = "Hamstrings", bodyRegion = "Legs"))

        // 2. Insert exercises A through H
        val exA = exerciseDao.insert(ExerciseEntity(name = "Lat Pulldown", description = "", muscleGroup = "Lats", equipment = "cable", difficulty = "Intermediate"))
        val exB = exerciseDao.insert(ExerciseEntity(name = "Lateral Raise", description = "", muscleGroup = "Shoulders", equipment = "dumbbell", difficulty = "Beginner"))
        val exC = exerciseDao.insert(ExerciseEntity(name = "Barbell Row", description = "", muscleGroup = "Back", equipment = "barbell", difficulty = "Intermediate"))
        val exD = exerciseDao.insert(ExerciseEntity(name = "Face Pull", description = "", muscleGroup = "Shoulders", equipment = "cable", difficulty = "Beginner"))
        val exE = exerciseDao.insert(ExerciseEntity(name = "Incline Bench", description = "", muscleGroup = "Chest", equipment = "barbell", difficulty = "Intermediate"))
        val exF = exerciseDao.insert(ExerciseEntity(name = "Flat Bench", description = "", muscleGroup = "Chest", equipment = "barbell", difficulty = "Intermediate"))
        val exG = exerciseDao.insert(ExerciseEntity(name = "Leg Extension", description = "", muscleGroup = "Legs", equipment = "machine", difficulty = "Beginner"))
        val exH = exerciseDao.insert(ExerciseEntity(name = "Romanian Deadlift", description = "", muscleGroup = "Legs", equipment = "barbell", difficulty = "Intermediate"))

        // 3. Link exercise_muscles (primary and secondary)
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exA, muscleId = latId, role = "primary")) // Ex A -> Lats Primary (1.0)
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exB, muscleId = latDeltId, role = "primary")) // Ex B -> Lateral Delt Primary (1.0)
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exC, muscleId = upperBackId, role = "primary")) // Ex C -> Upper Back Primary (1.0)
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exC, muscleId = rearDeltId, role = "secondary")) // Ex C -> Rear Delt Secondary (0.5)
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exD, muscleId = rearDeltId, role = "primary")) // Ex D -> Rear Delt Primary (1.0)
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exE, muscleId = upperChestId, role = "primary")) // Ex E -> Upper Chest Primary (1.0)
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exF, muscleId = chestId, role = "primary")) // Ex F -> Chest Primary (1.0)
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exG, muscleId = quadId, role = "primary")) // Ex G -> Quads Primary (1.0)
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exH, muscleId = hamId, role = "primary")) // Ex H -> Hamstrings Primary (1.0)

        // 4. Record completed workout with 2 sets of Ex B (Lateral Raise) and 2 sets of Ex C (Barbell Row)
        val now = System.currentTimeMillis()
        val workoutId = workoutDao.insertWorkout(WorkoutEntity(date = now, startTime = now - 3600000, endTime = now, duration = 3600, notes = "", completed = true, status = "COMPLETED"))

        val weB = workoutDao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exB, orderIndex = 0))
        val weC = workoutDao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exC, orderIndex = 1))

        workoutDao.insertWorkoutSet(WorkoutSetEntity(workoutExerciseId = weB, setNumber = 1, weight = 12.0, reps = 15, rpe = 8.0, restSeconds = 60, completed = true, setType = 0))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(workoutExerciseId = weB, setNumber = 2, weight = 12.0, reps = 15, rpe = 8.0, restSeconds = 60, completed = true, setType = 0))

        workoutDao.insertWorkoutSet(WorkoutSetEntity(workoutExerciseId = weC, setNumber = 1, weight = 70.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(workoutExerciseId = weC, setNumber = 2, weight = 70.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0))

        // 5. Query and compute volume via real DB and repository path
        val completedSets = repository.getCompletedSetsWithContext().first()
        val muscleDetails = exerciseMuscleDao.getAllWithDetails().first()

        val muscleAssignments = muscleDetails.groupBy { it.exerciseId }.mapValues { (_, rels) ->
            rels.map { rel ->
                val role = when (rel.role.lowercase()) {
                    "primary" -> VolumeCalculator.MuscleRole.PRIMARY
                    "secondary" -> VolumeCalculator.MuscleRole.SECONDARY
                    "stabilizer" -> VolumeCalculator.MuscleRole.STABILIZER
                    else -> VolumeCalculator.MuscleRole.PRIMARY
                }
                val canonical = CanonicalMuscle.fromIdOrName(rel.muscleName)
                val muscleName = canonical?.displayName ?: rel.muscleName
                VolumeCalculator.MuscleAssignment(muscleName, role)
            }
        }

        val balance = volumeCalculator.calculateWeeklyVolume(completedSets, muscleAssignments)

        // 6. Assert exact bucket volumes (ZERO CROSS-BUCKET LEAKAGE):
        // Lateral Deltoid: 2 primary sets = 2.0 effective sets
        // Lats: 0 sets (Lateral Delt must NOT leak into Lats)
        // Upper Back: 2 primary sets = 2.0 effective sets
        // Rear Deltoid: 2 secondary sets = 1.0 effective set (2 * 0.5)
        assertEquals(2.0, balance.lateralDeltVolume.weeklyEffectiveSets, 0.001)
        assertEquals(0.0, balance.latVolume.weeklyEffectiveSets, 0.001)
        assertEquals(2.0, balance.upperBackVolume.weeklyEffectiveSets, 0.001)
        assertEquals(1.0, balance.rearDeltVolume.weeklyEffectiveSets, 0.001)
    }
}
