package com.gymcoach.app.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymcoach.app.data.local.database.GymCoachDatabase
import com.gymcoach.app.data.local.dao.ReadinessDao
import com.gymcoach.app.data.local.entity.ReadinessEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for ReadinessRepository using real Room database.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ReadinessRepositoryIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: GymCoachDatabase
    private lateinit var readinessDao: ReadinessDao
    private lateinit var repository: ReadinessRepositoryImpl

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GymCoachDatabase::class.java
        ).allowMainThreadQueries().build()
        readinessDao = db.readinessDao()
        repository = ReadinessRepositoryImpl(readinessDao)
    }

    @Test
    fun `save and get latest readiness`() = runTest {
        val readiness = ReadinessEntity(
            sleepQuality = 4,
            soreness = 3,
            energy = 5,
            motivation = 4,
            notes = "Feeling good"
        )
        repository.saveReadiness(readiness)

        val latest = repository.getLatestReadiness().first()
        assertNotNull("Should retrieve latest readiness", latest)
        assertEquals(4, latest!!.sleepQuality)
        assertEquals(3, latest.soreness)
        assertEquals(5, latest.energy)
        assertEquals(4, latest.motivation)
        assertEquals("Feeling good", latest.notes)
    }

    @Test
    fun `getRecentReadiness returns last 7 days`() = runTest {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L

        // Insert readiness entries for past 10 days
        for (i in 0..9) {
            val readiness = ReadinessEntity(
                sleepQuality = 3 + (i % 3),
                soreness = 3,
                energy = 3,
                motivation = 3,
                notes = "Day $i"
            ).also { it.recordedAt = now - i * dayMs }
            repository.saveReadiness(readiness)
        }

        val weekAgo = now - 7 * dayMs
        val recent = repository.getRecentReadiness(weekAgo).first()
        assertTrue("Should have at most 7 days", recent.size <= 7)
    }

    @Test
    fun `readiness score computed correctly`() = runTest {
        val readiness = ReadinessEntity(
            sleepQuality = 4,
            soreness = 2,
            energy = 5,
            motivation = 3
        )

        val score = readiness.readinessScore
        assertEquals("Score should be average of 4 metrics", 3.5, score, 0.001)
    }

    @Test
    fun `training recommendation based on score`() = runTest {
        // High readiness
        val high = ReadinessEntity(sleepQuality = 5, soreness = 5, energy = 5, motivation = 5)
        assertEquals("Full intensity", high.trainingRecommendation)

        // Moderate readiness
        val moderate = ReadinessEntity(sleepQuality = 3, soreness = 3, energy = 3, motivation = 3)
        assertEquals("Moderate session", moderate.trainingRecommendation)

        // Low readiness
        val low = ReadinessEntity(sleepQuality = 2, soreness = 2, energy = 2, motivation = 2)
        assertEquals("Light session", low.trainingRecommendation)

        // Very low readiness
        val veryLow = ReadinessEntity(sleepQuality = 1, soreness = 1, energy = 1, motivation = 1)
        assertEquals("Rest day", veryLow.trainingRecommendation)
    }

    @Test
    fun `isRestDayRecommended when score below 2.5`() = runTest {
        val low = ReadinessEntity(sleepQuality = 2, soreness = 2, energy = 2, motivation = 2)
        assertTrue(low.isRestDayRecommended)

        val ok = ReadinessEntity(sleepQuality = 3, soreness = 3, energy = 3, motivation = 3)
        assertTrue(!ok.isRestDayRecommended)
    }
}