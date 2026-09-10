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
import com.gymcoach.app.domain.model.SetType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CompletedSetFilteringRoomTest {

    private lateinit var db: GymCoachDatabase
    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var repository: WorkoutRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        workoutDao = db.workoutDao()
        exerciseDao = db.exerciseDao()
        repository = WorkoutRepositoryImpl(workoutDao, exerciseDao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `verify exact completed set filtering in Room DB excludes WARMUP and incomplete sets`() = runTest {
        val exId = exerciseDao.insert(
            ExerciseEntity(name = "Dumbbell Press", description = "Chest", muscleGroup = "Chest", equipment = "dumbbell", difficulty = "Intermediate")
        )

        val now = System.currentTimeMillis()
        val workoutId = workoutDao.insertWorkout(
            WorkoutEntity(date = now, startTime = now - 3600000, endTime = now, duration = 3600, notes = "", completed = true, status = "COMPLETED")
        )
        val weId = workoutDao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exId, orderIndex = 0))

        // Insert set types in Room
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 101L, workoutExerciseId = weId, setNumber = 1, weight = 20.0, reps = 10, rpe = 5.0, restSeconds = 60, completed = true, setType = SetType.WARMUP.ordinal)) // WARMUP -> Excluded by DAO query
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 102L, workoutExerciseId = weId, setNumber = 2, weight = 40.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = SetType.NORMAL.ordinal)) // NORMAL completed -> Included
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 103L, workoutExerciseId = weId, setNumber = 3, weight = 35.0, reps = 10, rpe = 9.0, restSeconds = 90, completed = true, setType = SetType.DROP.ordinal)) // DROP completed -> Included
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 104L, workoutExerciseId = weId, setNumber = 4, weight = 40.0, reps = 6, rpe = 10.0, restSeconds = 90, completed = true, setType = SetType.FAILURE.ordinal)) // FAILURE completed -> Included
        workoutDao.insertWorkoutSet(WorkoutSetEntity(id = 105L, workoutExerciseId = weId, setNumber = 5, weight = 40.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = false, setType = SetType.NORMAL.ordinal)) // Incomplete -> Excluded by DAO query

        // Query Room DB via repository (getCompletedSetsWithContext filtering ws.completed = 1 AND ws.setType != 1)
        val setContexts = repository.getCompletedSetsWithContext().first()

        // 3 completed working hypertrophy sets returned (NORMAL, DROP, FAILURE)
        assertEquals(3, setContexts.size)
        assertTrue("Incomplete and WARMUP sets must be excluded by Room DAO query", setContexts.all { it.completed && it.domainSetType != SetType.WARMUP })

        // Verify SetTypes returned from Room
        val types = setContexts.map { it.domainSetType }
        assertTrue("NORMAL present in completed sets", types.contains(SetType.NORMAL))
        assertTrue("DROP present in completed sets", types.contains(SetType.DROP))
        assertTrue("FAILURE present in completed sets", types.contains(SetType.FAILURE))
    }
}
