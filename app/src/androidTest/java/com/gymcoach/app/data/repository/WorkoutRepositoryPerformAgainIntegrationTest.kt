package com.gymcoach.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymcoach.app.data.local.database.GymCoachDatabase
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutEntity
import com.gymcoach.app.data.local.entity.WorkoutExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class WorkoutRepositoryPerformAgainIntegrationTest {

    private lateinit var db: GymCoachDatabase
    private lateinit var workoutDao: WorkoutDao
    private lateinit var repository: WorkoutRepositoryImpl

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymCoachDatabase::class.java).build()
        workoutDao = db.workoutDao()
        repository = WorkoutRepositoryImpl(workoutDao, db.exerciseDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testPerformAgainCreatesNewSessionWithoutMutatingHistory() = runBlocking {
        // 1. Setup Historical Data
        val historicalWorkoutId = workoutDao.insertWorkout(
            WorkoutEntity(
                id = 0,
                date = Instant.now().minusSeconds(86400).toEpochMilli(),
                startTime = Instant.now().minusSeconds(86400).toEpochMilli(),
                endTime = Instant.now().minusSeconds(80000).toEpochMilli(),
                duration = 6400,
                notes = "Felt strong",
                completed = true,
                status = "COMPLETED"
            )
        )

        db.exerciseDao().insert(
            ExerciseEntity(id = 1, name = "Squat", description = "A squat", muscleGroup = "Legs", equipment = "Barbell", difficulty = "Beginner")
        )

        val historicalExerciseId = workoutDao.insertWorkoutExercise(
            WorkoutExerciseEntity(id = 0, workoutId = historicalWorkoutId, exerciseId = 1, orderIndex = 0)
        )

        workoutDao.insertWorkoutSet(
            WorkoutSetEntity(
                id = 0,
                workoutExerciseId = historicalExerciseId,
                setNumber = 1,
                weight = 100.0,
                reps = 10,
                rpe = 8.0,
                restSeconds = 120,
                completed = true,
                setType = 0
            )
        )

        // 2. Perform Again
        val newSessionId = repository.createSessionFromHistory(historicalWorkoutId)

        // 3. Verify New Session
        assertNotEquals(historicalWorkoutId, newSessionId)

        val newWorkout = workoutDao.getWorkoutById(newSessionId).first()
        requireNotNull(newWorkout)
        assertEquals("ACTIVE", newWorkout.status)
        assertEquals(false, newWorkout.completed)
        assertEquals(0, newWorkout.duration)
        assertEquals("", newWorkout.notes)

        val newExercises = workoutDao.getExercisesForWorkout(newSessionId).first()
        assertEquals(1, newExercises.size)
        val newExercise = newExercises.first()
        assertNotEquals(historicalExerciseId, newExercise.id)
        assertEquals(1, newExercise.exerciseId)
        assertEquals(0, newExercise.orderIndex)

        val newSets = workoutDao.getSetsForExercise(newExercise.id).first()
        assertEquals(1, newSets.size)
        val newSet = newSets.first()
        assertEquals(false, newSet.completed)
        assertEquals(0.0, newSet.weight, 0.01)
        assertEquals(0, newSet.reps)
        assertEquals(0.0, newSet.rpe, 0.01)
        assertEquals(120, newSet.restSeconds) // Structural value is copied

        // 4. Verify History Remains Immutable
        val history = workoutDao.getWorkoutById(historicalWorkoutId).first()
        requireNotNull(history)
        assertEquals("COMPLETED", history.status)
        assertEquals(true, history.completed)
        assertEquals(6400, history.duration)

        val historyExercises = workoutDao.getExercisesForWorkout(historicalWorkoutId).first()
        assertEquals(1, historyExercises.size)

        val historySets = workoutDao.getSetsForExercise(historyExercises.first().id).first()
        assertEquals(1, historySets.size)
        val historySet = historySets.first()
        assertEquals(100.0, historySet.weight, 0.01)
        assertEquals(10, historySet.reps)
        assertEquals(true, historySet.completed)
    }
}
