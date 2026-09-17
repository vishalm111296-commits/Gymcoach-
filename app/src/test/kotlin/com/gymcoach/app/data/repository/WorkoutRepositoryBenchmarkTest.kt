package com.gymcoach.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.database.GymCoachDatabase
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutEntity
import com.gymcoach.app.data.local.entity.WorkoutExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.system.measureNanoTime

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class WorkoutRepositoryBenchmarkTest {

    private lateinit var db: GymCoachDatabase
    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var repository: WorkoutRepositoryImpl
    private var sourceWorkoutId: Long = 0

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        workoutDao = db.workoutDao()
        exerciseDao = db.exerciseDao()
        repository = WorkoutRepositoryImpl(workoutDao, exerciseDao)

        // Seed DB with 1 exercise
        val exId = exerciseDao.insert(
            ExerciseEntity(
                name = "Bench Press",
                description = "Chest exercise",
                muscleGroup = "Chest",
                equipment = "Barbell",
                difficulty = "Intermediate"
            )
        )

        // Seed a historical workout with 30 exercises, each with 4 sets (120 sets total)
        val now = System.currentTimeMillis()
        sourceWorkoutId = workoutDao.insertWorkout(
            WorkoutEntity(
                date = now - 86400000,
                startTime = now - 86400000,
                endTime = now - 82800000,
                duration = 3600,
                notes = "Heavy Chest Workout",
                completed = true,
                status = "COMPLETED"
            )
        )

        val exerciseCount = 30
        val setsPerExercise = 4

        for (i in 0 until exerciseCount) {
            val weId = workoutDao.insertWorkoutExercise(
                WorkoutExerciseEntity(
                    workoutId = sourceWorkoutId,
                    exerciseId = exId,
                    orderIndex = i
                )
            )
            for (j in 1..setsPerExercise) {
                workoutDao.insertWorkoutSet(
                    WorkoutSetEntity(
                        workoutExerciseId = weId,
                        setNumber = j,
                        weight = 100.0,
                        reps = 10,
                        rpe = 8.0,
                        restSeconds = 90,
                        completed = true,
                        setType = 0
                    )
                )
            }
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun benchmarkCreateWorkoutFromHistory() = runTest {
        // Warmup runs
        repeat(5) {
            val newId = repository.createWorkoutFromHistory(sourceWorkoutId)
            assertNotNull(newId)
        }

        // Measured runs
        val iterations = 30
        val totalNanos = measureNanoTime {
            repeat(iterations) {
                val newId = repository.createWorkoutFromHistory(sourceWorkoutId)
                assertNotNull(newId)
            }
        }

        val avgMillis = (totalNanos / 1_000_000.0) / iterations
        println("BENCHMARK_RESULT: createWorkoutFromHistory average execution time over $iterations runs = ${String.format("%.3f", avgMillis)} ms")

        // Verify cloning accuracy
        val latestActive = workoutDao.getIncompleteWorkout()
        assertNotNull(latestActive)
        val clonedExercises = workoutDao.getExercisesForWorkout(latestActive!!.id).first()
        assertEquals(30, clonedExercises.size)
    }
}
