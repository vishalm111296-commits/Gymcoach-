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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class WorkoutDaoIntegrationTest {

    private lateinit var database: GymCoachDatabase
    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseDao: ExerciseDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, GymCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        workoutDao = database.workoutDao()
        exerciseDao = database.exerciseDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun givenNewWorkout_whenInsertAndGetById_thenReturnsSameWorkout() = runTest {
        val workout = WorkoutEntity(
            date = Instant.now().toEpochMilli(),
            startTime = Instant.now().toEpochMilli(),
            endTime = Instant.now().plusSeconds(3600).toEpochMilli(),
            duration = 3600,
            notes = "Test Note",
            completed = true,
            status = "COMPLETED"
        )
        val id = workoutDao.insertWorkout(workout)
        
        val retrieved = workoutDao.getWorkoutById(id).first()
        assertNotNull(retrieved)
        assertEquals(workout.notes, retrieved?.notes)
        assertEquals(workout.duration, retrieved?.duration)
        assertEquals(workout.status, retrieved?.status)
    }

    @Test
    fun givenWorkoutWithExercisesAndSets_whenDeleteWorkout_thenCascadesDeletes() = runTest {
        // Insert Exercise
        val exerciseId = exerciseDao.insert(ExerciseEntity(name = "Squat", description = "", muscleGroup = "Legs", equipment = "barbell", difficulty = "intermediate"))
        
        // Insert Workout
        val workout = WorkoutEntity(
            date = Instant.now().toEpochMilli(),
            startTime = Instant.now().toEpochMilli(),
            endTime = Instant.now().plusSeconds(3600).toEpochMilli(),
            duration = 3600,
            notes = "Cascade Test",
            completed = true,
            status = "COMPLETED"
        )
        val workoutId = workoutDao.insertWorkout(workout)
        
        // Insert WorkoutExercise
        val workoutExerciseId = workoutDao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exerciseId, orderIndex = 1))
        
        // Insert Set
        val setId = workoutDao.insertWorkoutSet(WorkoutSetEntity(workoutExerciseId = workoutExerciseId, setNumber = 1, weight = 100.0, reps = 10, rpe = 8.0, restSeconds = 60, completed = true, setType = 0))

        // Verify inserted
        assertNotNull(workoutDao.getWorkoutById(workoutId).first())
        assertNotNull(workoutDao.getWorkoutExerciseById(workoutExerciseId))
        assertNotNull(workoutDao.getWorkoutSetById(setId))
        
        // Delete Workout
        workoutDao.deleteWorkout(workoutDao.getWorkoutById(workoutId).first()!!)
        
        // Verify cascaded deletion
        assertNull(workoutDao.getWorkoutById(workoutId).first())
        assertNull(workoutDao.getWorkoutExerciseById(workoutExerciseId))
        assertNull(workoutDao.getWorkoutSetById(setId))
    }


    @Test
    fun givenWorkoutsInVariousDates_whenGetWorkoutsInDateRange_thenReturnsCorrectWorkouts() = runTest {
        val now = Instant.now().toEpochMilli()
        val oneDayAgo = now - 86400000L
        val twoDaysAgo = now - 2 * 86400000L
        val threeDaysAgo = now - 3 * 86400000L

        workoutDao.insertWorkout(WorkoutEntity(date = now, startTime = now, endTime = now, duration = 0, notes = "Now", completed = true, status = "COMPLETED"))
        workoutDao.insertWorkout(WorkoutEntity(date = oneDayAgo, startTime = oneDayAgo, endTime = oneDayAgo, duration = 0, notes = "One Day", completed = true, status = "COMPLETED"))
        workoutDao.insertWorkout(WorkoutEntity(date = twoDaysAgo, startTime = twoDaysAgo, endTime = twoDaysAgo, duration = 0, notes = "Two Days", completed = true, status = "COMPLETED"))
        workoutDao.insertWorkout(WorkoutEntity(date = threeDaysAgo, startTime = threeDaysAgo, endTime = threeDaysAgo, duration = 0, notes = "Three Days", completed = true, status = "COMPLETED"))

        val startDate = twoDaysAgo - 1000
        val endDate = oneDayAgo + 1000

        val workouts = workoutDao.getWorkoutsInDateRange(startDate, endDate).first()
        assertEquals(2, workouts.size)
        assertTrue(workouts.any { it.notes == "One Day" })
        assertTrue(workouts.any { it.notes == "Two Days" })
    }

    @Test
    fun givenMultipleSetsForExercise_whenGetLastSetsForExercises_thenReturnsMostRecentCompletedSet() = runTest {
        val exerciseId = exerciseDao.insert(ExerciseEntity(name = "Bench Press", description = "", muscleGroup = "Chest", equipment = "barbell", difficulty = "intermediate"))
        
        val olderWorkout = WorkoutEntity(date = 1000L, startTime = 1000L, endTime = 2000L, duration = 1000, notes = "Older", completed = true, status = "COMPLETED")
        val olderWorkoutId = workoutDao.insertWorkout(olderWorkout)
        val olderWeId = workoutDao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = olderWorkoutId, exerciseId = exerciseId, orderIndex = 1))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(workoutExerciseId = olderWeId, setNumber = 1, weight = 80.0, reps = 10, rpe = 8.0, restSeconds = 60, completed = true, setType = 0))

        val newerWorkout = WorkoutEntity(date = 5000L, startTime = 5000L, endTime = 6000L, duration = 1000, notes = "Newer", completed = true, status = "COMPLETED")
        val newerWorkoutId = workoutDao.insertWorkout(newerWorkout)
        val newerWeId = workoutDao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = newerWorkoutId, exerciseId = exerciseId, orderIndex = 1))
        workoutDao.insertWorkoutSet(WorkoutSetEntity(workoutExerciseId = newerWeId, setNumber = 1, weight = 100.0, reps = 5, rpe = 9.0, restSeconds = 60, completed = true, setType = 0))

        val lastSets = workoutDao.getLastSetsForExercises(listOf(exerciseId))
        
        assertEquals(1, lastSets.size)
        assertEquals(100.0, lastSets[0].weight, 0.001)
        assertEquals(5, lastSets[0].reps)
    }

}
