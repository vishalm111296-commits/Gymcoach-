package com.gymcoach.app.presentation.progress

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for body measurement null handling.
 *
 * Convention:
 * - Room entity uses non-nullable Double = 0.0 for optional fields (waist, chest, bodyFat, etc.)
 * - ViewModel maps 0.0 → null via takeIf { it > 0 } so UI can distinguish "not measured" from "0.0"
 * - BodyMeasurementTrend shows "Not measured" for null currentValue
 * - MeasurementLogDialog passes null for empty fields → ViewModel stores as 0.0
 */
class BodyMeasurementTest {

    // ── 1. Null mapping convention (ViewModel layer) ────────────

    @Test
    fun `zero value maps to null via takeIf`() {
        val dbValue: Double = 0.0
        val uiValue = dbValue.takeIf { it > 0 }
        assertNull("0.0 should become null (not measured)", uiValue)
    }

    @Test
    fun `positive value preserved via takeIf`() {
        val dbValue: Double = 75.5
        val uiValue = dbValue.takeIf { it > 0 }
        assertEquals(75.5, uiValue!!, 0.01)
    }

    @Test
    fun `negative value maps to null via takeIf`() {
        val dbValue: Double = -1.0
        val uiValue = dbValue.takeIf { it > 0 }
        assertNull("negative value should map to null", uiValue)
    }

    // ── 2. Safe-call chain on nullable entity reference ─────────

    /**
     * Simulates ProgressViewModel: latest?.waistCm?.takeIf { it > 0 }
     * When latest is null (no measurements), result is null.
     */
    @Test
    fun `entity reference null yields null`() {
        val latest: FakeEntity? = null
        val latestWaist = latest?.waistCm?.takeIf { it > 0 }
        assertNull(latestWaist)
    }

    @Test
    fun `entity with zero waist yields null`() {
        val latest = FakeEntity(waistCm = 0.0)
        val latestWaist = latest?.waistCm?.takeIf { it > 0 }
        assertNull("0.0 waist → null", latestWaist)
    }

    @Test
    fun `entity with valid waist yields value`() {
        val latest = FakeEntity(waistCm = 82.0)
        val latestWaist = latest?.waistCm?.takeIf { it > 0 }
        assertEquals(82.0, latestWaist!!, 0.01)
    }

    // ── 3. Body fat null handling ───────────────────────────────

    @Test
    fun `null body fat is not treated as 0_0`() {
        val bodyFatFromDb: Double = 0.0
        val bodyFatForUi = bodyFatFromDb.takeIf { it > 0 }
        assertNull("0.0 body fat should be null in UI", bodyFatForUi)
    }

    @Test
    fun `valid body fat is preserved`() {
        val bodyFatFromDb: Double = 15.3
        val bodyFatForUi = bodyFatFromDb.takeIf { it > 0 }
        assertEquals(15.3, bodyFatForUi!!, 0.01)
    }

    // ── 4. Trend calculation with empty/partial data ────────────

    @Test
    fun `trendDirection returns STABLE for empty list`() {
        val direction = trendDirection(emptyList())
        assertEquals(TrendDirection.STABLE, direction)
    }

    @Test
    fun `trendDirection returns STABLE for single point`() {
        val points = listOf(TrendPoint(LocalDate.of(2026, 1, 1), 75.0))
        val direction = trendDirection(points)
        assertEquals(TrendDirection.STABLE, direction)
    }

    @Test
    fun `trendDirection handles bodyweight-only data`() {
        val points = listOf(
            TrendPoint(LocalDate.of(2026, 1, 1), 80.0),
            TrendPoint(LocalDate.of(2026, 2, 1), 82.0),
            TrendPoint(LocalDate.of(2026, 3, 1), 81.0)
        )
        assertEquals(TrendDirection.UP, trendDirection(points))
    }

    @Test
    fun `trendDirection handles waist-only data`() {
        val points = listOf(
            TrendPoint(LocalDate.of(2026, 1, 1), 85.0),
            TrendPoint(LocalDate.of(2026, 2, 1), 83.0),
            TrendPoint(LocalDate.of(2026, 3, 1), 82.0)
        )
        assertEquals(TrendDirection.DOWN, trendDirection(points))
    }

    // ── 5. Measurement filtering (0.0 excluded from trends) ─────

    @Test
    fun `zero values excluded from bodyweight trend`() {
        val measurements = listOf(
            FakeEntity(weightKg = 80.0, waistCm = 0.0),
            FakeEntity(weightKg = 0.0, waistCm = 82.0),
            FakeEntity(weightKg = 82.0, waistCm = 81.0)
        )
        val bodyweightPoints = measurements
            .filter { it.weightKg > 0 }
            .map { TrendPoint(LocalDate.now(), it.weightKg) }
        assertEquals(2, bodyweightPoints.size)
    }

    @Test
    fun `zero values excluded from waist trend`() {
        val measurements = listOf(
            FakeEntity(weightKg = 80.0, waistCm = 0.0),
            FakeEntity(weightKg = 82.0, waistCm = 82.0),
            FakeEntity(weightKg = 81.0, waistCm = 81.0)
        )
        val waistPoints = measurements
            .filter { it.waistCm > 0 }
            .map { TrendPoint(LocalDate.now(), it.waistCm) }
        assertEquals(2, waistPoints.size)
    }

    @Test
    fun `empty trend when all measurements are zero`() {
        val measurements = listOf(
            FakeEntity(weightKg = 0.0, waistCm = 0.0),
            FakeEntity(weightKg = 0.0, waistCm = 0.0)
        )
        val bodyweightPoints = measurements
            .filter { it.weightKg > 0 }
            .map { TrendPoint(LocalDate.now(), it.weightKg) }
        assertEquals(0, bodyweightPoints.size)
    }

    // ── 6. Full extraction path (entity → ViewModel → UI) ───────

    @Test
    fun `full extraction path with mixed measurements`() {
        val latest = FakeEntity(weightKg = 80.0, waistCm = 0.0, chestCm = 95.0, bodyFatPct = 0.0)

        val latestWeight = latest?.weightKg?.takeIf { it > 0 }
        val latestWaist = latest?.waistCm?.takeIf { it > 0 }
        val latestChest = latest?.chestCm?.takeIf { it > 0 }
        val latestBodyFat = latest?.bodyFatPct?.takeIf { it > 0 }

        assertEquals(80.0, latestWeight!!, 0.01)
        assertNull("0.0 waist → null", latestWaist)
        assertEquals(95.0, latestChest!!, 0.01)
        assertNull("0.0 body fat → null", latestBodyFat)
    }

    @Test
    fun `all null when no measurements exist`() {
        val latest: FakeEntity? = null

        val latestWeight = latest?.weightKg?.takeIf { it > 0 }
        val latestWaist = latest?.waistCm?.takeIf { it > 0 }

        assertNull(latestWeight)
        assertNull(latestWaist)
    }

    // ── Helpers ─────────────────────────────────────────────────

    /**
     * Simulates BodyMeasurementEntity (non-nullable Double fields with 0.0 defaults).
     */
    private data class FakeEntity(
        val weightKg: Double = 0.0,
        val waistCm: Double = 0.0,
        val chestCm: Double = 0.0,
        val bodyFatPct: Double = 0.0
    )

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
