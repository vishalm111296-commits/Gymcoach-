package com.gymcoach.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymcoach.app.core.exercise.EquipmentAvailability
import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.core.progression.ProgressionEngine
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RoomDatabaseClosedLoopIntegrationTest {

    private lateinit var db: GymCoachDatabase
    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var exerciseMuscleDao: ExerciseMuscleDao
    private lateinit var muscleDao: MuscleDao
    private lateinit var repository: WorkoutRepositoryImpl
    private lateinit var volumeCalculator: VolumeCalculator
    private lateinit var progressionEngine: ProgressionEngine

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        workoutDao = db.workoutDao()
        exerciseDao = db.exerciseDao()
        exerciseMuscleDao = db.exerciseMuscleDao()
        muscleDao = db.muscleDao()

        repository = WorkoutRepositoryImpl(workoutDao, exerciseDao)
        volumeCalculator = VolumeCalculator()
        progressionEngine = ProgressionEngine(EquipmentAvailability())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `databaseBackedVolumeCalculationTest calculates real completed sets with primary and secondary credits`() = runTest {
        // 1. Insert Muscles
        val latId = muscleDao.insert(MuscleEntity(name = "latissimus_dorsi", displayName = "Lats", bodyRegion = "Back"))
        val bicepId = muscleDao.insert(MuscleEntity(name = "biceps", displayName = "Biceps", bodyRegion = "Arms"))

        // 2. Insert Exercises
        val exAId = exerciseDao.insert(
            ExerciseEntity(name = "Lat Pulldown", description = "Back exercise", muscleGroup = "Back", equipment = "cable", difficulty = "Beginner")
        )
        val exBId = exerciseDao.insert(
            ExerciseEntity(name = "Bicep Curl", description = "Arm exercise", muscleGroup = "Biceps", equipment = "dumbbell", difficulty = "Beginner")
        )

        // 3. Insert ExerciseMuscle relations
        // Ex A: Lats = PRIMARY (1.0), Biceps = SECONDARY (0.5)
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exAId, muscleId = latId, role = "primary"))
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exAId, muscleId = bicepId, role = "secondary"))
        // Ex B: Biceps = PRIMARY (1.0)
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exBId, muscleId = bicepId, role = "primary"))

        // 4. Insert Workout A (COMPLETED) with 3 completed sets for Ex A and 2 completed sets for Ex B + 1 incomplete set
        val now = System.currentTimeMillis()
        val workoutId = workoutDao.insertWorkout(
            WorkoutEntity(date = now, startTime = now - 3600000, endTime = now, duration = 3600, notes = "Pull Day", completed = true, status = "COMPLETED")
        )

        val weAId = workoutDao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exAId, orderIndex = 0))
        val weBId = workoutDao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exBId, orderIndex = 1))

        // 3 completed sets for Ex A
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 101L, workoutExerciseId = weAId, setNumber = 1, weight = 60.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 102L, workoutExerciseId = weAId, setNumber = 2, weight = 60.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 103L, workoutExerciseId = weAId, setNumber = 3, weight = 60.0, reps = 10, rpe = 8.5, restSeconds = 90, completed = true, setType = 0))

        // 2 completed sets for Ex B + 1 incomplete set (uncompleted set should be ignored)
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 201L, workoutExerciseId = weBId, setNumber = 1, weight = 15.0, reps = 12, rpe = 8.0, restSeconds = 60, completed = true, setType = 0))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 202L, workoutExerciseId = weBId, setNumber = 2, weight = 15.0, reps = 12, rpe = 8.0, restSeconds = 60, completed = true, setType = 0))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 203L, workoutExerciseId = weBId, setNumber = 3, weight = 15.0, reps = 12, rpe = 8.0, restSeconds = 60, completed = false, setType = 0)) // INCOMPLETE

        // 5. Query real completed sets and exercise muscle details from DB
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
                val muscleName = when {
                    rel.muscleName.contains("lat", ignoreCase = true) -> "Lats"
                    rel.muscleName.contains("bicep", ignoreCase = true) -> "Biceps"
                    else -> rel.muscleName
                }
                VolumeCalculator.MuscleAssignment(muscleName, role)
            }
        }

        val balance = volumeCalculator.calculateWeeklyVolume(completedSets, muscleAssignments)

        // 6. Verify real volume credits
        assertEquals(3, balance.latVolume.directSets) // 3 primary sets
        assertEquals(2, balance.bicepsVolume.directSets) // 2 primary sets
        assertEquals(3, balance.bicepsVolume.indirectSets) // 3 secondary sets from Lat Pulldown
    }

    @Test
    fun `databaseBackedPerformAgainTest creates independent active workout without mutating source`() = runTest {
        val exId = exerciseDao.insert(
            ExerciseEntity(name = "Barbell Squat", description = "Legs", muscleGroup = "Legs", equipment = "barbell", difficulty = "Intermediate")
        )

        val now = System.currentTimeMillis()
        val workoutAId = workoutDao.insertWorkout(
            WorkoutEntity(date = now - 86400000, startTime = now - 86400000, endTime = now - 82800000, duration = 3600, notes = "Leg Day A", completed = true, status = "COMPLETED")
        )
        val weAId = workoutDao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = workoutAId, exerciseId = exId, orderIndex = 0))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(workoutExerciseId = weAId, setNumber = 1, weight = 100.0, reps = 5, rpe = 8.0, restSeconds = 180, completed = true, setType = 0))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(workoutExerciseId = weAId, setNumber = 2, weight = 100.0, reps = 5, rpe = 8.5, restSeconds = 180, completed = true, setType = 0))

        // Trigger Perform Again
        val workoutBId = repository.createWorkoutFromHistory(workoutAId)
        assertNotNull(workoutBId)
        assertNotEquals(workoutAId, workoutBId)

        // Verify Workout A remains untouched in DB
        val workoutAInDb = workoutDao.getWorkoutById(workoutAId).first()
        assertNotNull(workoutAInDb)
        assertEquals("COMPLETED", workoutAInDb!!.status)
        assertTrue(workoutAInDb.completed)
        assertEquals("Leg Day A", workoutAInDb.notes)

        // Verify Workout B created as ACTIVE in DB
        val workoutBInDb = workoutDao.getWorkoutById(workoutBId!!).first()
        assertNotNull(workoutBInDb)
        assertEquals("ACTIVE", workoutBInDb!!.status)
        assertFalse(workoutBInDb.completed)
        assertEquals(0L, workoutBInDb.duration)

        // Verify sets in Workout B are incomplete and assigned to Workout B's exercise entity
        val exercisesB = workoutDao.getExercisesForWorkout(workoutBId).first()
        assertEquals(1, exercisesB.size)
        val setsB = workoutDao.getSetsForExercise(exercisesB[0].id).first()
        assertEquals(2, setsB.size)
        assertTrue("Copied sets must be incomplete for new session", setsB.all { !it.completed })

        // Complete Workout B
        workoutDao.updateWorkout(workoutBInDb.copy(completed = true, status = "COMPLETED", duration = 3000))

        // Trigger Perform Again from Workout A a second time -> Workout C
        val workoutCId = repository.createWorkoutFromHistory(workoutAId)
        assertNotNull(workoutCId)
        assertNotEquals(workoutAId, workoutCId)
        assertNotEquals(workoutBId, workoutCId)

        // Verify all 3 workouts exist independently in DB
        val completedWorkouts = repository.getCompletedWorkouts().first()
        assertEquals(2, completedWorkouts.size) // A and B completed
    }

    @Test
    fun `databaseBackedClosedLoopTest verifies complete cycle Workout A to B to C`() = runTest {
        // 1. Create & Complete Workout A
        val exId = exerciseDao.insert(
            ExerciseEntity(name = "Bench Press", description = "Chest", muscleGroup = "Chest", equipment = "barbell", difficulty = "Intermediate")
        )
        val now = System.currentTimeMillis()
        val workoutAId = workoutDao.insertWorkout(
            WorkoutEntity(date = now - 86400000, startTime = now - 86400000, endTime = now - 82800000, duration = 3600, notes = "Push Day A", completed = true, status = "COMPLETED")
        )
        val weAId = workoutDao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = workoutAId, exerciseId = exId, orderIndex = 0))
        val setA = WorkoutSetEntity(workoutExerciseId = weAId, setNumber = 1, weight = 80.0, reps = 8, rpe = 8.0, restSeconds = 120, completed = true, setType = 0)
        workoutDao.insertWorkoutSet(setA)

        // 2. Perform Again -> Creates Workout B
        val workoutBId = repository.createWorkoutFromHistory(workoutAId)!!

        // 3. Log completed set in B (80kg x 12 reps - top of range)
        val exercisesB = workoutDao.getExercisesForWorkout(workoutBId).first()
        val setsB = workoutDao.getSetsForExercise(exercisesB[0].id).first()
        val updatedSetB = setsB[0].copy(weight = 80.0, reps = 12, completed = true)
        workoutDao.updateWorkoutSet(updatedSetB)

        // Complete Workout B
        val workoutBEntity = workoutDao.getWorkoutById(workoutBId).first()!!
        workoutDao.updateWorkout(workoutBEntity.copy(completed = true, status = "COMPLETED", duration = 3200))

        // 4. Query history -> contains A and B
        val completedList = repository.getCompletedWorkouts().first()
        assertEquals(2, completedList.size)

        // 5. Query ProgressionEngine with persisted sets from A and B for a gym environment
        val recommendation = progressionEngine.calculateProgression(
            exerciseId = exId,
            exerciseName = "Bench Press",
            exerciseEquipment = "barbell",
            targetRepsMin = 8,
            targetRepsMax = 12,
            targetSets = 3,
            previousSets = listOf(setA),
            currentSets = listOf(updatedSetB),
            equipmentType = "gym"
        )
        assertEquals(85.0, recommendation.recommendedWeight, 0.01) // 80kg + 5kg -> 85kg

        // 6. Perform Again from A second time -> Workout C
        val workoutCId = repository.createWorkoutFromHistory(workoutAId)!!
        assertNotEquals(workoutAId, workoutCId)
        assertNotEquals(workoutBId, workoutCId)

        // Verify Workout C is ACTIVE
        val workoutCEntity = workoutDao.getWorkoutById(workoutCId).first()!!
        assertEquals("ACTIVE", workoutCEntity.status)
        assertFalse(workoutCEntity.completed)
    }
}
