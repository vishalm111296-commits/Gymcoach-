package com.gymcoach.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.database.GymCoachDatabase
import com.gymcoach.app.data.local.entity.WorkoutEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class WorkoutRepositoryTransactionTest {
    private lateinit var database: GymCoachDatabase
    private lateinit var workoutDao: WorkoutDao

    class TestTransactionException(message: String) : RuntimeException(message)

    @Before
    fun setup() {
        val osName = (System.getProperty("os.name") ?: "").lowercase()
        val osArch = (System.getProperty("os.arch") ?: "").lowercase()
        val isUnsupportedLinuxAarch64 = osName.contains("linux") && (osArch == "aarch64" || osArch == "arm64")
        org.junit.Assume.assumeFalse(
            "Robolectric SQLite runtime is not supported on Linux aarch64 by upstream Robolectric",
            isUnsupportedLinuxAarch64
        )

        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, GymCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        workoutDao = database.workoutDao()
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
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
        val exceptionCaught = try {
            database.withTransaction {
                id = workoutDao.insertWorkout(workout)
                if (id >= 0L) {
                    throw TestTransactionException("Transaction aborted")
                }
            }
            false
        } catch (e: TestTransactionException) {
            true
        }
        
        assertTrue("Transaction abort exception should have been caught", exceptionCaught)
        assertTrue("Insert should have generated an ID inside transaction before abort", id != -1L)
        val retrieved = workoutDao.getWorkoutById(id).first()
        assertNull("Rolled back entity must not exist in database", retrieved)
    }
}
