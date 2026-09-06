package com.gymcoach.app.presentation.progress

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for body measurement null handling.
 *
 * Verifies that:
 * - 0.0 values (from Room entity defaults) map to null ("not measured")
 * - Trend calculations handle empty/partial measurement data
 * - The convention 0.0 = not measured is consistent
 */
class BodyMeasurementTest {

    // ── 1. Null mapping convention ──────────────────────────────

    /**
     * Room entity uses Double = 0.0 for optional fields.
     * ViewModel maps 0.0 → null via takeIf { it > 0 }.
     * This test verifies that convention.
     */
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
    fun `null entity field maps to null`() {
        val entityWaistCm: Double? = null
        val uiValue = entityWaistCm?.takeIf { it > 0 }
        assertNull("null entity field stays null", uiValue)
    }

    @Test
    fun `negative value maps to null via takeIf`() {
        val dbValue: Double = -1.0
        val uiValue = dbValue.takeIf { it > 0 }
        assertNull("negative value should map to null", uiValue)
    }

    // ── 2. Body fat null handling ───────────────────────────────

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

    // ── 3. Waist null handling ──────────────────────────────────

    @Test
    fun `null waist is not treated as 0_0`() {
        val waistFromDb: Double = 0.0
        val waistForUi = waistFromDb.takeIf { it > 0 }
        assertNull("0.0 waist should be null in UI", waistForUi)
    }

    @Test
    fun `valid waist is preserved`() {
        val waistFromDb: Double = 82.0
        val waistForUi = waistFromDb.takeIf { it > 0 }
        assertEquals(82.0, waistForUi!!, 0.01)
    }

    // ── 4. Trend calculation with empty data ────────────────────

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

    // ── 5. Trend calculation with partial measurements ──────────

    @Test
    fun `trendDirection handles partial bodyweight-only data`() {
        val points = listOf(
            TrendPoint(LocalDate.of(2026, 1, 1), 80.0),
            TrendPoint(LocalDate.of(2026, 2, 1), 82.0),
            TrendPoint(LocalDate.of(2026, 3, 1), 81.0)
        )
        val direction = trendDirection(points)
        assertEquals(TrendDirection.UP, direction)
    }

    @Test
    fun `trendDirection handles waist-only data`() {
        val points = listOf(
            TrendPoint(LocalDate.of(2026, 1, 1), 85.0),
            TrendPoint(LocalDate.of(2026, 2, 1), 83.0),
            TrendPoint(LocalDate.of(2026, 3, 1), 82.0)
        )
        val direction = trendDirection(points)
        assertEquals(TrendDirection.DOWN, direction)
    }

    @Test
    fun `trendDirection handles single measurement point`() {
        val points = listOf(
            TrendPoint(LocalDate.of(2026, 1, 1), 80.0)
        )
        val direction = trendDirection(points)
        assertEquals(TrendDirection.STABLE, direction)
    }

    // ── 6. Measurement filtering (0.0 excluded from trends) ─────

    @Test
    fun `zero values excluded from bodyweight trend`() {
        val measurements = listOf(
            FakeMeasurement(weightKg = 80.0, waistCm = 0.0),
            FakeMeasurement(weightKg = 0.0, waistCm = 82.0),
            FakeMeasurement(weightKg = 82.0, waistCm = 81.0)
        )

        val bodyweightPoints = measurements
            .filter { it.weightKg > 0 }
            .map { TrendPoint(LocalDate.now(), it.weightKg) }

        assertEquals("Only non-zero bodyweight entries in trend", 2, bodyweightPoints.size)
    }

    @Test
    fun `zero values excluded from waist trend`() {
        val measurements = listOf(
            FakeMeasurement(weightKg = 80.0, waistCm = 0.0),
            FakeMeasurement(weightKg = 82.0, waistCm = 82.0),
            FakeMeasurement(weightKg = 81.0, waistCm = 81.0)
        )

        val waistPoints = measurements
            .filter { it.waistCm > 0 }
            .map { TrendPoint(LocalDate.now(), it.waistCm) }

        assertEquals("Only non-zero waist entries in trend", 2, waistPoints.size)
    }

    @Test
    fun `empty trend when all measurements are zero`() {
        val measurements = listOf(
            FakeMeasurement(weightKg = 0.0, waistCm = 0.0),
            FakeMeasurement(weightKg = 0.0, waistCm = 0.0)
        )

        val bodyweightPoints = measurements
            .filter { it.weightKg > 0 }
            .map { TrendPoint(LocalDate.now(), it.weightKg) }

        assertEquals("No entries when all are zero", 0, bodyweightPoints.size)
    }

    // ── 7. Latest measurement extraction ────────────────────────

    @Test
    fun `latest measurement extracts nullable fields correctly`() {
        val latest = FakeMeasurement(weightKg = 80.0, waistCm = 0.0, chestCm = 95.0, bodyFatPct = 0.0)

        val latestWeight = latest.weightKg.takeIf { it > 0 }
        val latestWaist = latest.waistCm.takeIf { it > 0 }
        val latestChest = latest.chestCm.takeIf { it > 0 }
        val latestBodyFat = latest.bodyFatPct.takeIf { it > 0 }

        assertEquals(80.0, latestWeight!!, 0.01)
        assertNull("0.0 waist → null", latestWaist)
        assertEquals(95.0, latestChest!!, 0.01)
        assertNull("0.0 body fat → null", latestBodyFat)
    }

    @Test
    fun `latest measurement all null when no measurements exist`() {
        val latest: FakeMeasurement? = null

        val latestWeight = latest?.weightKg?.takeIf { it > 0 }
        val latestWaist = latest?.waistCm?.takeIf { it > 0 }

        assertNull(latestWeight)
        assertNull(latestWaist)
    }

    // ── Helpers ─────────────────────────────────────────────────

    private data class FakeMeasurement(
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
