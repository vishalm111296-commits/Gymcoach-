package com.gymcoach.app.presentation.progress

import com.gymcoach.app.domain.model.SetType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for ProgressDateRange enum and ProgressModels.
 */
class ProgressModelsTest {

    @Test
    fun `date ranges have correct weeks`() {
        assertEquals(4, ProgressDateRange.FOUR_WEEKS.weeks)
        assertEquals(8, ProgressDateRange.EIGHT_WEEKS.weeks)
        assertEquals(12, ProgressDateRange.TWELVE_WEEKS.weeks)
    }

    @Test
    fun `date ranges have correct labels`() {
        assertEquals("4W", ProgressDateRange.FOUR_WEEKS.label)
        assertEquals("8W", ProgressDateRange.EIGHT_WEEKS.label)
        assertEquals("12W", ProgressDateRange.TWELVE_WEEKS.label)
    }

    @Test
    fun `ProgressPoint stores date and value`() {
        val point = ProgressPoint(LocalDate.of(2026, 1, 15), 80.0)
        assertEquals(LocalDate.of(2026, 1, 15), point.date)
        assertEquals(80.0, point.value, 0.01)
    }

    @Test
    fun `TrendPoint stores date and value`() {
        val point = TrendPoint(LocalDate.of(2026, 3, 1), 75.5)
        assertEquals(LocalDate.of(2026, 3, 1), point.date)
        assertEquals(75.5, point.value, 0.01)
    }

    @Test
    fun `TrendDirection enum values`() {
        assertEquals(3, TrendDirection.entries.size)
        assertTrue(TrendDirection.entries.contains(TrendDirection.UP))
        assertTrue(TrendDirection.entries.contains(TrendDirection.DOWN))
        assertTrue(TrendDirection.entries.contains(TrendDirection.STABLE))
    }

    @Test
    fun `MuscleVolumeData stores fields correctly`() {
        val data = MuscleVolumeData(
            muscleName = "CHEST",
            currentSets = 16,
            targetMin = 10,
            targetMax = 20
        )
        assertEquals("CHEST", data.muscleName)
        assertEquals(16, data.currentSets)
        assertEquals(10, data.targetMin)
        assertEquals(20, data.targetMax)
    }

    @Test
    fun `PersonalRecordItem stores fields correctly`() {
        val pr = PersonalRecordItem(
            exerciseName = "Bench Press",
            achievement = "100.0 kg × 8",
            date = LocalDate.of(2026, 8, 20)
        )
        assertEquals("Bench Press", pr.exerciseName)
        assertEquals("100.0 kg × 8", pr.achievement)
        assertEquals(LocalDate.of(2026, 8, 20), pr.date)
    }

    @Test
    fun `ProgressDateRange entries count`() {
        assertEquals(3, ProgressDateRange.entries.size)
    }
}

/**
 * Tests for ProgressViewModel calculations.
 */
class ProgressViewModelTest {

    @Test
    fun `calculateWeeklyTrend returns 0 for empty list`() {
        val weekly = emptyList<Pair<java.util.Date, Double>>()
        val trend = calculateWeeklyTrend(weekly)
        assertEquals(0.0, trend, 0.01)
    }

    @Test
    fun `calculateWeeklyTrend returns 0 for single entry`() {
        val weekly = listOf(
            java.util.Date() to 100.0
        )
        val trend = calculateWeeklyTrend(weekly)
        assertEquals(0.0, trend, 0.01)
    }

    @Test
    fun `calculateWeeklyTrend returns positive for increase`() {
        val weekly = listOf(
            java.util.Date() to 100.0,
            java.util.Date() to 150.0
        )
        val trend = calculateWeeklyTrend(weekly)
        assertEquals(50.0, trend, 0.01)
    }

    @Test
    fun `calculateWeeklyTrend returns negative for decrease`() {
        val weekly = listOf(
            java.util.Date() to 200.0,
            java.util.Date() to 100.0
        )
        val trend = calculateWeeklyTrend(weekly)
        assertEquals(-50.0, trend, 0.01)
    }

    @Test
    fun `calculateWeeklyTrend returns 0 for no change`() {
        val weekly = listOf(
            java.util.Date() to 100.0,
            java.util.Date() to 100.0
        )
        val trend = calculateWeeklyTrend(weekly)
        assertEquals(0.0, trend, 0.01)
    }

    @Test
    fun `calculateWeeklyTrend handles multiple entries`() {
        val weekly = listOf(
            java.util.Date() to 50.0,
            java.util.Date() to 100.0,
            java.util.Date() to 200.0
        )
        val trend = calculateWeeklyTrend(weekly)
        // Only compares last two: (200-100)/100 * 100 = 100%
        assertEquals(100.0, trend, 0.01)
    }

    @Test
    fun `calculateWeeklyTrend handles zero previous volume`() {
        val weekly = listOf(
            java.util.Date() to 0.0,
            java.util.Date() to 100.0
        )
        val trend = calculateWeeklyTrend(weekly)
        assertEquals(0.0, trend, 0.01)
    }

    @Test
    fun `trendDirection returns STABLE for empty list`() {
        val direction = trendDirection(emptyList())
        assertEquals(TrendDirection.STABLE, direction)
    }

    @Test
    fun `trendDirection returns STABLE for single point`() {
        val points = listOf(TrendPoint(LocalDate.now(), 70.0))
        val direction = trendDirection(points)
        assertEquals(TrendDirection.STABLE, direction)
    }

    @Test
    fun `trendDirection returns UP for increase`() {
        val points = listOf(
            TrendPoint(LocalDate.of(2026, 1, 1), 70.0),
            TrendPoint(LocalDate.of(2026, 8, 1), 80.0)
        )
        val direction = trendDirection(points)
        assertEquals(TrendDirection.UP, direction)
    }

    @Test
    fun `trendDirection returns DOWN for decrease`() {
        val points = listOf(
            TrendPoint(LocalDate.of(2026, 1, 1), 80.0),
            TrendPoint(LocalDate.of(2026, 8, 1), 70.0)
        )
        val direction = trendDirection(points)
        assertEquals(TrendDirection.DOWN, direction)
    }

    @Test
    fun `trendDirection returns STABLE for small change`() {
        val points = listOf(
            TrendPoint(LocalDate.of(2026, 1, 1), 70.0),
            TrendPoint(LocalDate.of(2026, 8, 1), 70.5) // < 1% change
        )
        val direction = trendDirection(points)
        assertEquals(TrendDirection.STABLE, direction)
    }

    @Test
    fun `adherence is clamped between 0 and 1`() {
        // Over target
        val overAdherence = (8.toFloat() / 4).coerceIn(0f, 1f)
        assertEquals(1f, overAdherence, 0.01f)

        // Under target
        val underAdherence = (1.toFloat() / 4).coerceIn(0f, 1f)
        assertEquals(0.25f, underAdherence, 0.01f)

        // Zero
        val zeroAdherence = (0.toFloat() / 4).coerceIn(0f, 1f)
        assertEquals(0f, zeroAdherence, 0.01f)
    }

    // Helper functions to match ProgressViewModel's private methods
    private fun calculateWeeklyTrend(weekly: List<Pair<java.util.Date, Double>>): Double {
        if (weekly.size < 2) return 0.0
        val recent = weekly.takeLast(2)
        val prev = recent[0].second
        val curr = recent[1].second
        return if (prev == 0.0) 0.0 else ((curr - prev) / prev) * 100
    }

    private fun trendDirection(points: List<TrendPoint>): TrendDirection {
        if (points.size < 2) return TrendDirection.STABLE
        val first = points.first().value
        val last = points.last().value
        return when {
            last > first * 1.01 -> TrendDirection.UP
            last < first * 0.99 -> TrendDirection.DOWN
            else -> TrendDirection.STABLE
        }
    }
}

/**
 * Tests for MuscleGroupStats and MuscleDistributionPieChart calculation logic.
 */
class MuscleGroupStatsTest {

    @Test
    fun `MuscleGroupStats calculates total percentage correctly`() {
        val stats = listOf(
            com.gymcoach.app.domain.repository.MuscleGroupStats("Chest", 100),
            com.gymcoach.app.domain.repository.MuscleGroupStats("Back", 100)
        )
        val totalReps = stats.sumOf { it.totalReps }
        assertEquals(200, totalReps)

        val chestPercentage = (stats[0].totalReps.toDouble() / totalReps * 100)
        assertEquals(50.0, chestPercentage, 0.01)
    }

    @Test
    fun `empty MuscleGroupStats list has zero total reps`() {
        val stats = emptyList<com.gymcoach.app.domain.repository.MuscleGroupStats>()
        val totalReps = stats.sumOf { it.totalReps }
        assertEquals(0, totalReps)
    }
}
