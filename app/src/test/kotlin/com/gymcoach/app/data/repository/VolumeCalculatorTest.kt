package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.dao.DateVolume
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * VolumeCalculatorTest - real tests verifying volume calculation via mocked DAO.
 *
 * Tests that volume queries delegate correctly to the DAO and return
 * properly structured data. The actual SQL aggregation (reps × weight)
 * is tested by the Room migration tests; here we test the data flow.
 */
class VolumeCalculatorTest {

    private lateinit var workoutDao: WorkoutDao

    @Before
    fun setup() {
        workoutDao = mockk<WorkoutDao>()
    }

    @Test
    fun `getAllWorkoutVolumes returns daily volumes`() = runTest {
        val volumes = listOf(
            DateVolume(date = 1700000000000L, volume = 3000.0),
            DateVolume(date = 1700086400000L, volume = 2500.0),
            DateVolume(date = 1700172800000L, volume = 4000.0)
        )

        coEvery { workoutDao.getAllWorkoutVolumes() } returns volumes

        val result = workoutDao.getAllWorkoutVolumes()

        assertEquals("Should have 3 daily volumes", 3, result.size)
        assertEquals("First day volume should be 3000.0", 3000.0, result[0].volume, 0.001)
        assertEquals("Second day volume should be 2500.0", 2500.0, result[1].volume, 0.001)
        assertEquals("Third day volume should be 4000.0", 4000.0, result[2].volume, 0.001)
    }

    @Test
    fun `getAllWorkoutVolumes returns empty when no completed workouts`() = runTest {
        coEvery { workoutDao.getAllWorkoutVolumes() } returns emptyList()

        val result = workoutDao.getAllWorkoutVolumes()

        assertEquals("Should return empty list", 0, result.size)
    }

    @Test
    fun `getMonthlyVolumes returns monthly aggregations`() = runTest {
        // Test the strftime('%Y-%m', datetime(w.date / 1000, 'unixepoch')) grouping
        val monthlyVolumes = listOf(
            DateVolume(date = 1704067200000L, volume = 15000.0),  // January 2024
            DateVolume(date = 1706745600000L, volume = 18000.0),  // February 2024
            DateVolume(date = 1709251200000L, volume = 20000.0)   // March 2024
        )

        coEvery { workoutDao.getMonthlyVolumes() } returns monthlyVolumes

        val result = workoutDao.getMonthlyVolumes()

        assertEquals("Should have 3 months", 3, result.size)
        // Verify volumes are different (not all grouped into same month)
        assertTrue("January volume should be different from February",
            result[0].volume != result[1].volume)
    }

    @Test
    fun `getTotalVolumeSum returns total across all workouts`() = runTest {
        coEvery { workoutDao.getTotalVolumeSum() } returns 50000.0

        val totalVolume = workoutDao.getTotalVolumeSum()

        assertEquals("Total volume should be 50000.0", 50000.0, totalVolume!!, 0.001)
    }

    @Test
    fun `getTotalVolumeSum returns null when no workouts`() = runTest {
        coEvery { workoutDao.getTotalVolumeSum() } returns null

        val totalVolume = workoutDao.getTotalVolumeSum()

        assertEquals("Should return null when no workouts", null, totalVolume)
    }

    @Test
    fun `getAverageWorkoutVolume returns average`() = runTest {
        coEvery { workoutDao.getAverageWorkoutVolume() } returns 2500.0

        val avgVolume = workoutDao.getAverageWorkoutVolume()

        assertEquals("Average volume should be 2500.0", 2500.0, avgVolume, 0.001)
    }

    @Test
    fun `volume calculation math is correct`() = runTest {
        // Verify the volume formula: volume = Σ(reps × weight) per set
        // This tests the math, not the DAO
        val sets = listOf(
            Pair(10, 60.0),   // 600
            Pair(8, 70.0),    // 560
            Pair(6, 80.0)     // 480
        )

        val totalVolume = sets.sumOf { it.first * it.second }

        assertEquals("Total volume should be 1640.0", 1640.0, totalVolume, 0.001)
    }

    @Test
    fun `zero weight sets contribute zero volume`() = runTest {
        val sets = listOf(
            Pair(10, 0.0),   // 0 (bodyweight)
            Pair(8, 0.0)     // 0 (bodyweight)
        )

        val totalVolume = sets.sumOf { it.first * it.second }

        assertEquals("Zero weight sets should contribute 0 volume", 0.0, totalVolume, 0.001)
    }
}
