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
import com.gymcoach.app.domain.model.CanonicalMuscleTaxonomy
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
    fun `hostileVolumeAttributionTest verifies strict canonical attribution and excludes invalid sets`() = runTest {
        // Insert 8 distinct muscles in canonical taxonomy
        val latId = muscleDao.insert(MuscleEntity(name = "latissimus_dorsi", displayName = "Lats", bodyRegion = "Back"))
        val bicepId = muscleDao.insert(MuscleEntity(name = "biceps", displayName = "Biceps", bodyRegion = "Arms"))
        val upperBackId = muscleDao.insert(MuscleEntity(name = "upper_back", displayName = "Upper Back", bodyRegion = "Back"))
        val latDeltId = muscleDao.insert(MuscleEntity(name = "lateral_deltoid", displayName = "Side Delt", bodyRegion = "Shoulders"))
        val quadId = muscleDao.insert(MuscleEntity(name = "quadriceps", displayName = "Quads", bodyRegion = "Legs"))
        val hamId = muscleDao.insert(MuscleEntity(name = "hamstrings", displayName = "Hamstrings", bodyRegion = "Legs"))
        val gluteId = muscleDao.insert(MuscleEntity(name = "glutes", displayName = "Glutes", bodyRegion = "Legs"))
        val calfId = muscleDao.insert(MuscleEntity(name = "calves", displayName = "Calves", bodyRegion = "Legs"))

        // Insert Exercises A-H
        val exA = exerciseDao.insert(ExerciseEntity(name = "Lat Pulldown", description = "", muscleGroup = "Back", equipment = "cable", difficulty = "Beginner"))
        val exB = exerciseDao.insert(ExerciseEntity(name = "Underhand Pullup", description = "", muscleGroup = "Biceps", equipment = "bodyweight", difficulty = "Intermediate"))
        val exC = exerciseDao.insert(ExerciseEntity(name = "Barbell Row", description = "", muscleGroup = "Back", equipment = "barbell", difficulty = "Intermediate"))
        val exD = exerciseDao.insert(ExerciseEntity(name = "Lateral Raise", description = "", muscleGroup = "Shoulders", equipment = "dumbbell", difficulty = "Beginner"))
        val exE = exerciseDao.insert(ExerciseEntity(name = "Squat", description = "", muscleGroup = "Legs", equipment = "barbell", difficulty = "Advanced"))
        val exF = exerciseDao.insert(ExerciseEntity(name = "RDL", description = "", muscleGroup = "Legs", equipment = "barbell", difficulty = "Intermediate"))
        val exG = exerciseDao.insert(ExerciseEntity(name = "Hip Thrust", description = "", muscleGroup = "Legs", equipment = "barbell", difficulty = "Intermediate"))
        val exH = exerciseDao.insert(ExerciseEntity(name = "Calf Raise", description = "", muscleGroup = "Legs", equipment = "machine", difficulty = "Beginner"))

        // Relations:
        // Ex A: primary latissimus_dorsi
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exA, muscleId = latId, role = "primary"))
        // Ex B: primary biceps, secondary latissimus_dorsi
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exB, muscleId = bicepId, role = "primary"))
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exB, muscleId = latId, role = "secondary"))
        // Ex C: primary upper_back
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exC, muscleId = upperBackId, role = "primary"))
        // Ex D: primary lateral_deltoid
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exD, muscleId = latDeltId, role = "primary"))
        // Ex E: primary quadriceps
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exE, muscleId = quadId, role = "primary"))
        // Ex F: primary hamstrings
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exF, muscleId = hamId, role = "primary"))
        // Ex G: primary glutes
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exG, muscleId = gluteId, role = "primary"))
        // Ex H: primary calves
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exH, muscleId = calfId, role = "primary"))

        // Create completed Workout
        val now = System.currentTimeMillis()
        val workoutId = workoutDao.insertWorkout(WorkoutEntity(date = now, startTime = now - 3600000, endTime = now, duration = 3600, notes = "Full Body", completed = true, status = "COMPLETED"))

        val weA = workoutDao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exA, orderIndex = 0))
        val weB = workoutDao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exB, orderIndex = 1))
        val weC = workoutDao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exC, orderIndex = 2))
        val weD = workoutDao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exD, orderIndex = 3))
        val weE = workoutDao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exE, orderIndex = 4))

        // Ex A: 3 completed sets
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 101L, workoutExerciseId = weA, setNumber = 1, weight = 60.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 102L, workoutExerciseId = weA, setNumber = 2, weight = 60.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 103L, workoutExerciseId = weA, setNumber = 3, weight = 60.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0))

        // Ex B: 2 completed sets + 1 warmup set (setType=1, ignored)
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 201L, workoutExerciseId = weB, setNumber = 1, weight = 0.0, reps = 8, rpe = 8.0, restSeconds = 90, completed = true, setType = 1)) // WARMUP
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 202L, workoutExerciseId = weB, setNumber = 2, weight = 0.0, reps = 8, rpe = 8.0, restSeconds = 90, completed = true, setType = 0))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 203L, workoutExerciseId = weB, setNumber = 3, weight = 0.0, reps = 8, rpe = 8.0, restSeconds = 90, completed = true, setType = 0))

        // Ex C: 2 completed sets
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 301L, workoutExerciseId = weC, setNumber = 1, weight = 70.0, reps = 8, rpe = 8.0, restSeconds = 90, completed = true, setType = 0))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 302L, workoutExerciseId = weC, setNumber = 2, weight = 70.0, reps = 8, rpe = 8.0, restSeconds = 90, completed = true, setType = 0))

        // Ex D: 3 completed sets
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 401L, workoutExerciseId = weD, setNumber = 1, weight = 12.0, reps = 12, rpe = 8.0, restSeconds = 60, completed = true, setType = 0))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 402L, workoutExerciseId = weD, setNumber = 2, weight = 12.0, reps = 12, rpe = 8.0, restSeconds = 60, completed = true, setType = 0))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 403L, workoutExerciseId = weD, setNumber = 3, weight = 12.0, reps = 12, rpe = 8.0, restSeconds = 60, completed = true, setType = 0))

        // Ex E: 4 completed sets + 1 incomplete set (completed=false, ignored)
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 501L, workoutExerciseId = weE, setNumber = 1, weight = 100.0, reps = 5, rpe = 8.0, restSeconds = 180, completed = true, setType = 0))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 502L, workoutExerciseId = weE, setNumber = 2, weight = 100.0, reps = 5, rpe = 8.0, restSeconds = 180, completed = true, setType = 0))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 503L, workoutExerciseId = weE, setNumber = 3, weight = 100.0, reps = 5, rpe = 8.0, restSeconds = 180, completed = true, setType = 0))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 504L, workoutExerciseId = weE, setNumber = 4, weight = 100.0, reps = 5, rpe = 8.0, restSeconds = 180, completed = true, setType = 0))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 505L, workoutExerciseId = weE, setNumber = 5, weight = 100.0, reps = 5, rpe = 8.0, restSeconds = 180, completed = false, setType = 0)) // INCOMPLETE

        // Query real sets and muscle mappings
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
                VolumeCalculator.MuscleAssignment(CanonicalMuscleTaxonomy.mapToCategory(rel.muscleName), role)
            }
        }

        val balance = volumeCalculator.calculateWeeklyVolume(completedSets, muscleAssignments)

        // Strict assertions:
        assertEquals(3, balance.latVolume.directSets) // Ex A primary
        assertEquals(2, balance.latVolume.indirectSets) // Ex B secondary
        assertEquals(5, balance.latVolume.weeklySets) // 3 + 2

        assertEquals(2, balance.bicepsVolume.directSets) // Ex B primary
        assertEquals(0, balance.bicepsVolume.indirectSets)
        assertEquals(2, balance.bicepsVolume.weeklySets) // Biceps is NEVER lats

        assertEquals(2, balance.upperBackVolume.directSets) // Ex C primary
        assertEquals(0, balance.upperBackVolume.indirectSets) // Upper Back is NEVER lats

        assertEquals(3, balance.lateralDeltVolume.directSets) // Ex D primary (Lateral Delts distinct)
        assertEquals(4, balance.quadricepsVolume.directSets) // Ex E primary (Quads distinct)
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

        // Verify Workout A pre-existing active session was abandoned per Policy C and C is ACTIVE
        val workoutCInDb = workoutDao.getWorkoutById(workoutCId!!).first()
        assertNotNull(workoutCInDb)
        assertEquals("ACTIVE", workoutCInDb!!.status)

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
