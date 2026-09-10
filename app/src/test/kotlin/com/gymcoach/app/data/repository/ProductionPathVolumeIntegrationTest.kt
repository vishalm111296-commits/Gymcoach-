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
import com.gymcoach.app.domain.model.SetType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProductionPathVolumeIntegrationTest {

    private lateinit var db: GymCoachDatabase
    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var muscleDao: MuscleDao
    private lateinit var exerciseMuscleDao: ExerciseMuscleDao
    private lateinit var workoutRepository: WorkoutRepositoryImpl
    private lateinit var exerciseRepository: ExerciseRepositoryImpl
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

        workoutRepository = WorkoutRepositoryImpl(workoutDao, exerciseDao)
        exerciseRepository = ExerciseRepositoryImpl(exerciseDao, exerciseMuscleDao)
        volumeCalculator = VolumeCalculator()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `production path integration test exercises real Room through repository and volume calculator`() = runTest {
        // 1. Seed muscles in Room
        val latId = muscleDao.insert(MuscleEntity(name = "latissimus_dorsi", displayName = "Lats", bodyRegion = "Back"))
        val bicepId = muscleDao.insert(MuscleEntity(name = "biceps", displayName = "Biceps", bodyRegion = "Arms"))

        // 2. Seed exercises in Room
        val exId = exerciseDao.insert(
            ExerciseEntity(name = "Lat Pulldown", description = "Back", muscleGroup = "Lats", equipment = "cable", difficulty = "Intermediate")
        )

        // 3. Seed exercise muscle relationships in Room (Lats PRIMARY 1.0, Biceps SECONDARY 0.5)
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exId, muscleId = latId, role = "primary"))
        exerciseMuscleDao.insert(ExerciseMuscleEntity(exerciseId = exId, muscleId = bicepId, role = "secondary"))

        // 4. Seed completed workout in Room
        val now = System.currentTimeMillis()
        val workoutId = workoutDao.insertWorkout(
            WorkoutEntity(date = now, startTime = now - 3600000, endTime = now, duration = 3600, notes = "", completed = true, status = "COMPLETED")
        )
        val weId = workoutDao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exId, orderIndex = 0))

        // 3 completed sets for Lat Pulldown
        repeat(3) { i ->
            workoutDao.insertWorkoutSet(
                WorkoutSetEntity(workoutExerciseId = weId, setNumber = i + 1, weight = 70.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = SetType.NORMAL.ordinal)
            )
        }

        // 5. Execute production repository calls
        val completedSets = workoutRepository.getCompletedSetsWithContext().first()
        val allExercises = exerciseRepository.getAllExercises().first()
        val muscleDetails = exerciseRepository.getAllExerciseMuscleDetails().first()

        // 6. Map using production HomeViewModel mapping logic
        val detailsByExercise = muscleDetails.groupBy { it.exerciseId }
        val muscleAssignments = allExercises.associate { exercise ->
            val rels = detailsByExercise[exercise.id]
            val assignments = mutableListOf<VolumeCalculator.MuscleAssignment>()
            if (!rels.isNullOrEmpty()) {
                for (rel in rels) {
                    val role = when (rel.role.lowercase()) {
                        "primary" -> VolumeCalculator.MuscleRole.PRIMARY
                        "secondary" -> VolumeCalculator.MuscleRole.SECONDARY
                        "stabilizer" -> VolumeCalculator.MuscleRole.STABILIZER
                        else -> VolumeCalculator.MuscleRole.PRIMARY
                    }
                    val canonical = CanonicalMuscle.fromIdOrName(rel.muscleName)
                    val mappedName = canonical?.displayName ?: rel.muscleName
                    assignments.add(VolumeCalculator.MuscleAssignment(mappedName, role))
                }
            }
            exercise.id to assignments
        }

        // 7. Calculate volume
        val balance = volumeCalculator.calculateWeeklyVolume(completedSets, muscleAssignments)

        // 8. Verify exact results derived from production repository path
        assertEquals(3.0, balance.latVolume.weeklyEffectiveSets, 0.001)
        assertEquals(3, balance.latVolume.rawDirectSets)
        assertEquals(1.5, balance.bicepsVolume.weeklyEffectiveSets, 0.001)
        assertEquals(3, balance.bicepsVolume.rawIndirectSets)
    }
}
