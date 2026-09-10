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
import com.gymcoach.app.domain.model.CanonicalMuscle
import com.gymcoach.app.domain.model.SetType
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

@RunWith(RobolectricTestRunner::class)
class RoomDatabaseClosedLoopIntegrationTest {

    private lateinit var db: GymCoachDatabase
    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var muscleDao: MuscleDao
    private lateinit var exerciseMuscleDao: ExerciseMuscleDao
    private lateinit var repository: WorkoutRepositoryImpl
    private lateinit var volumeCalculator: VolumeCalculator
    private lateinit var progressionEngine: ProgressionEngine

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
        progressionEngine = ProgressionEngine(EquipmentAvailability())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `databaseBackedVolumeCalculationTest calculates exact weighted credits from real database rows`() = runTest {
        val latId = muscleDao.insert(MuscleEntity(name = "latissimus_dorsi", displayName = "Lats", bodyRegion = "Back"))
        val bicepId = muscleDao.insert(MuscleEntity(name = "biceps", displayName = "Biceps", bodyRegion = "Arms"))

        val exAId = exerciseDao.insert(
            ExerciseEntity(name = "Lat Pulldown", description = "Back exercise", muscleGroup = "Lats", equipment = "cable", difficulty = "Beginner")
        )
        val exBId = exerciseDao.insert(
            ExerciseEntity(name = "Bicep Curl", description = "Arm exercise", muscleGroup = "Biceps", equipment = "dumbbell", difficulty = "Beginner")
        )

        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exAId, muscleId = latId, role = "primary"))
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exAId, muscleId = bicepId, role = "secondary"))
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exBId, muscleId = bicepId, role = "primary"))

        val now = System.currentTimeMillis()
        val workoutId = workoutDao.insertWorkout(
            WorkoutEntity(date = now, startTime = now - 3600000, endTime = now, duration = 3600, notes = "Pull Day", completed = true, status = "COMPLETED")
        )

        val weAId = workoutDao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exAId, orderIndex = 0))
        val weBId = workoutDao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exBId, orderIndex = 1))

        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 101L, workoutExerciseId = weAId, setNumber = 1, weight = 60.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = SetType.NORMAL.ordinal))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 102L, workoutExerciseId = weAId, setNumber = 2, weight = 60.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = SetType.NORMAL.ordinal))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 103L, workoutExerciseId = weAId, setNumber = 3, weight = 60.0, reps = 10, rpe = 8.5, restSeconds = 90, completed = true, setType = SetType.NORMAL.ordinal))

        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 201L, workoutExerciseId = weBId, setNumber = 1, weight = 15.0, reps = 12, rpe = 8.0, restSeconds = 60, completed = true, setType = SetType.NORMAL.ordinal))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 202L, workoutExerciseId = weBId, setNumber = 2, weight = 15.0, reps = 12, rpe = 8.0, restSeconds = 60, completed = true, setType = SetType.NORMAL.ordinal))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 203L, workoutExerciseId = weBId, setNumber = 3, weight = 15.0, reps = 12, rpe = 8.0, restSeconds = 60, completed = false, setType = SetType.NORMAL.ordinal))

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

        assertEquals(3, balance.latVolume.rawDirectSets)
        assertEquals(2, balance.bicepsVolume.rawDirectSets)
        assertEquals(3, balance.bicepsVolume.rawIndirectSets)
        assertEquals(3.0, balance.latVolume.weeklyEffectiveSets, 0.001)
        assertEquals(3.5, balance.bicepsVolume.weeklyEffectiveSets, 0.001)
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
        workoutDao.insertWorkoutSet(WorkoutSetEntity(workoutExerciseId = weAId, setNumber = 1, weight = 100.0, reps = 5, rpe = 8.0, restSeconds = 180, completed = true, setType = SetType.NORMAL.ordinal))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(workoutExerciseId = weAId, setNumber = 2, weight = 100.0, reps = 5, rpe = 8.5, restSeconds = 180, completed = true, setType = SetType.NORMAL.ordinal))

        val workoutBId = repository.createWorkoutFromHistory(workoutAId)
        assertNotNull(workoutBId)
        assertNotEquals(workoutAId, workoutBId)

        val workoutAInDb = workoutDao.getWorkoutById(workoutAId).first()
        assertNotNull(workoutAInDb)
        assertEquals("COMPLETED", workoutAInDb!!.status)
        assertTrue(workoutAInDb.completed)
        assertEquals("Leg Day A", workoutAInDb.notes)

        val workoutBInDb = workoutDao.getWorkoutById(workoutBId!!).first()
        assertNotNull(workoutBInDb)
        assertEquals("ACTIVE", workoutBInDb!!.status)
        assertFalse(workoutBInDb.completed)
        assertEquals(0L, workoutBInDb.duration)

        val exercisesB = workoutDao.getExercisesForWorkout(workoutBId).first()
        assertEquals(1, exercisesB.size)
        val setsB = workoutDao.getSetsForExercise(exercisesB[0].id).first()
        assertEquals(2, setsB.size)
        assertTrue("Copied sets must be incomplete for new session", setsB.all { !it.completed })

        workoutDao.updateWorkout(workoutBInDb.copy(completed = true, status = "COMPLETED", duration = 3000))

        val workoutCId = repository.createWorkoutFromHistory(workoutAId)
        assertNotNull(workoutCId)
        assertNotEquals(workoutAId, workoutCId)
        assertNotEquals(workoutBId, workoutCId)

        val completedWorkouts = repository.getCompletedWorkouts().first()
        assertEquals(2, completedWorkouts.size)
    }

    @Test
    fun `progressionLoopIntegrationTestCalculatesWeightIncreaseFromPersistedPerformance`() = runTest {
        // 1. Create & Complete Workout A in real Room database
        val exId = exerciseDao.insert(
            ExerciseEntity(name = "Bench Press", description = "Chest", muscleGroup = "Chest", equipment = "barbell", difficulty = "Intermediate")
        )
        val now = System.currentTimeMillis()
        val workoutAId = workoutDao.insertWorkout(
            WorkoutEntity(date = now - 86400000, startTime = now - 86400000, endTime = now - 82800000, duration = 3600, notes = "Push Day A", completed = true, status = "COMPLETED")
        )
        val weAId = workoutDao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = workoutAId, exerciseId = exId, orderIndex = 0))
        val setA = WorkoutSetEntity(workoutExerciseId = weAId, setNumber = 1, weight = 80.0, reps = 8, rpe = 8.0, restSeconds = 120, completed = true, setType = SetType.NORMAL.ordinal)
        workoutDao.insertWorkoutSet(setA)

        // 2. Perform Again -> Creates Workout B in real Room DB
        val workoutBId = repository.createWorkoutFromHistory(workoutAId)!!

        // 3. Log completed set in B (80kg x 12 reps - top of range)
        val exercisesB = workoutDao.getExercisesForWorkout(workoutBId).first()
        val setsB = workoutDao.getSetsForExercise(exercisesB[0].id).first()
        val updatedSetB = setsB[0].copy(weight = 80.0, reps = 12, completed = true)
        workoutDao.updateWorkoutSet(updatedSetB)

        // Complete Workout B
        val workoutBEntity = workoutDao.getWorkoutById(workoutBId).first()!!
        workoutDao.updateWorkout(workoutBEntity.copy(completed = true, status = "COMPLETED", duration = 3200))

        // 4. Retrieve actual persisted sets from Room DB via WorkoutDao path
        val exercisesInA = workoutDao.getExercisesForWorkout(workoutAId).first()
        val exercisesInB = workoutDao.getExercisesForWorkout(workoutBId).first()

        val retrievedSetsA = workoutDao.getSetsForExercise(exercisesInA[0].id).first()
        val retrievedSetsB = workoutDao.getSetsForExercise(exercisesInB[0].id).first()

        // 5. Query ProgressionEngine with persisted sets retrieved from DAO path
        val recommendation = progressionEngine.calculateProgression(
            exerciseId = exId,
            exerciseName = "Bench Press",
            exerciseEquipment = "barbell",
            targetRepsMin = 8,
            targetRepsMax = 12,
            targetSets = 3,
            previousSets = retrievedSetsA,
            currentSets = retrievedSetsB,
            equipmentType = "gym"
        )

        // 6. Assert progressive weight increase calculated strictly from DB-persisted data
        assertEquals("Progression engine must recommend weight increase to 85.0kg", 85.0, recommendation.recommendedWeight, 0.01)
    }
}
