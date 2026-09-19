package com.gymcoach.app.core.analytics

import com.gymcoach.app.data.local.entity.BodyMeasurementEntity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VTaperTransformationEngineTest {

    private lateinit var engine: VTaperTransformationEngine

    @Before
    fun setup() {
        engine = VTaperTransformationEngine()
    }

    @Test
    fun testAdonisIndex_Novice() {
        val measurement = BodyMeasurementEntity(
            waistCm = 80.0,
            shouldersCm = 100.0 // Ratio: 1.25 < 1.35
        )
        val report = engine.calculateReport(listOf(measurement))
        assertEquals(VTaperTier.NOVICE, report.adonisIndex.tier)
        assertEquals(1.25, report.adonisIndex.currentRatio, 0.01)
        assertEquals(77, report.adonisIndex.progressPct) // 1.25 / 1.618 * 100
    }

    @Test
    fun testAdonisIndex_Athletic() {
        val measurement = BodyMeasurementEntity(
            waistCm = 80.0,
            shouldersCm = 115.0 // Ratio: 1.4375 (1.35..1.50)
        )
        val report = engine.calculateReport(listOf(measurement))
        assertEquals(VTaperTier.ATHLETIC, report.adonisIndex.tier)
    }

    @Test
    fun testAdonisIndex_Prime() {
        val measurement = BodyMeasurementEntity(
            waistCm = 80.0,
            shouldersCm = 125.0 // Ratio: 1.5625 (1.50..1.618)
        )
        val report = engine.calculateReport(listOf(measurement))
        assertEquals(VTaperTier.PRIME, report.adonisIndex.tier)
    }

    @Test
    fun testAdonisIndex_Golden() {
        val measurement = BodyMeasurementEntity(
            waistCm = 80.0,
            shouldersCm = 135.0 // Ratio: 1.6875 (> 1.618)
        )
        val report = engine.calculateReport(listOf(measurement))
        assertEquals(VTaperTier.GOLDEN, report.adonisIndex.tier)
        assertEquals(100, report.adonisIndex.progressPct) // Capped at 100
    }

    @Test
    fun testEmptyOrZeroCircumference() {
        val emptyReport = engine.calculateReport(emptyList())
        assertEquals(VTaperTier.NOVICE, emptyReport.adonisIndex.tier)
        assertEquals(0.0, emptyReport.adonisIndex.currentRatio, 0.0)

        val zeroMeasurement = BodyMeasurementEntity(
            waistCm = 0.0,
            shouldersCm = 0.0
        )
        val zeroReport = engine.calculateReport(listOf(zeroMeasurement))
        assertEquals(VTaperTier.NOVICE, zeroReport.adonisIndex.tier)
        assertEquals(0.0, zeroReport.adonisIndex.currentRatio, 0.0)
    }

    @Test
    fun testLimbSymmetry_Balanced() {
        val measurement = BodyMeasurementEntity(
            leftArmCm = 35.0,
            rightArmCm = 35.2 // delta = 0.2 (<= 0.5)
        )
        val report = engine.calculateReport(listOf(measurement))

        val armSymmetry = report.limbSymmetries.find { it.limbName == "Arms" }
        assertNotNull(armSymmetry)
        assertTrue(armSymmetry!!.isBalanced)
        assertEquals(0.2, armSymmetry.deltaCm, 0.01)
        assertEquals(99.43, armSymmetry.symmetryPct, 0.01) // 35 / 35.2
    }

    @Test
    fun testLimbSymmetry_Imbalanced() {
        val measurement = BodyMeasurementEntity(
            leftThighCm = 60.0,
            rightThighCm = 61.5 // delta = 1.5 (> 0.5)
        )
        val report = engine.calculateReport(listOf(measurement))

        val thighSymmetry = report.limbSymmetries.find { it.limbName == "Thighs" }
        assertNotNull(thighSymmetry)
        assertFalse(thighSymmetry!!.isBalanced)
        assertEquals(1.5, thighSymmetry.deltaCm, 0.01)
    }

    @Test
    fun testLimbSymmetry_MissingData() {
        val measurement = BodyMeasurementEntity(
            leftCalfCm = 0.0,
            rightCalfCm = 35.0
        )
        val report = engine.calculateReport(listOf(measurement))

        val calfSymmetry = report.limbSymmetries.find { it.limbName == "Calves" }
        assertNull(calfSymmetry) // Should be filtered out if either is 0
    }

    @Test
    fun testRecompDelta() {
        val now = System.currentTimeMillis()
        val days90Millis = 90L * 24 * 60 * 60 * 1000
        val days45Millis = 45L * 24 * 60 * 60 * 1000

        val measurements = listOf(
            BodyMeasurementEntity(id = 1, recordedAt = now - days90Millis - 1000, waistCm = 90.0, shouldersCm = 110.0, chestCm = 100.0, weightKg = 85.0),
            BodyMeasurementEntity(id = 2, recordedAt = now - days45Millis, waistCm = 85.0, shouldersCm = 115.0, chestCm = 105.0, weightKg = 82.0),
            BodyMeasurementEntity(id = 3, recordedAt = now, waistCm = 80.0, shouldersCm = 120.0, chestCm = 110.0, weightKg = 80.0)
        )

        val report = engine.calculateReport(measurements, daysWindow = 90)
        assertNotNull(report.recompDelta)

        // Baseline should be the one at now - 90 days
        assertEquals(-10.0, report.recompDelta!!.waistDeltaCm, 0.01) // 80 - 90
        assertEquals(10.0, report.recompDelta!!.shoulderDeltaCm, 0.01) // 120 - 110
        assertEquals(10.0, report.recompDelta!!.chestDeltaCm, 0.01) // 110 - 100
        assertEquals(-5.0, report.recompDelta!!.weightDeltaKg, 0.01) // 80 - 85
        assertEquals(90, report.recompDelta!!.daysPeriod)
    }
}
