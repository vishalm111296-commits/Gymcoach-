package com.gymcoach.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.database.GymCoachDatabase
import com.gymcoach.app.data.local.entity.WorkoutEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class WorkoutRepositoryTransactionTest {
    private lateinit var database: GymCoachDatabase
    private lateinit var workoutDao: WorkoutDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, GymCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        workoutDao = database.workoutDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun givenFailedTransaction_whenInserting_thenDatabaseStateReverted() = runTest {
        val workout = WorkoutEntity(
            date = Instant.now().toEpochMilli(),
            startTime = Instant.now().toEpochMilli(),
            endTime = Instant.now().plusSeconds(3600).toEpochMilli(),
            duration = 3600,
            notes = "Should fail",
            completed = true,
            status = "COMPLETED"
        )
        
        var id = -1L
        
        try {
            database.runInTransaction {
                kotlinx.coroutines.runBlocking { id = workoutDao.insertWorkout(workout) }
                throw Exception("Transaction aborted")
            }
        } catch (e: Exception) {
            // Expected
        }
        
        val retrieved = if (id != -1L) workoutDao.getWorkoutById(id).first() else null
        assertNull(retrieved)
    }
}
