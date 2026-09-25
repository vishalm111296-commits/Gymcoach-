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

    @Test
    fun testSingleMeasurementRecompDeltaIsNull() {
        val measurement = BodyMeasurementEntity(
            id = 1,
            recordedAt = System.currentTimeMillis(),
            waistCm = 80.0,
            shouldersCm = 120.0
        )
        val report = engine.calculateReport(listOf(measurement))
        assertNull(report.recompDelta)
        assertNotNull(report.latestMeasurement)
    }

    @Test
    fun testChronologicalSortingOrder() {
        val now = System.currentTimeMillis()
        val mOld = BodyMeasurementEntity(id = 1, recordedAt = now - 100000L, waistCm = 95.0, shouldersCm = 110.0)
        val mNew = BodyMeasurementEntity(id = 2, recordedAt = now, waistCm = 80.0, shouldersCm = 125.0)

        // Pass out-of-order
        val report = engine.calculateReport(listOf(mNew, mOld))
        assertEquals(2, report.history.size)
        assertEquals(1L, report.history[0].id)
        assertEquals(2L, report.history[1].id)
        assertEquals(2L, report.latestMeasurement?.id)
        assertEquals(80.0, report.latestMeasurement?.waistCm ?: 0.0, 0.001)
    }

    @Test
    fun testPerfectLimbSymmetry() {
        val measurement = BodyMeasurementEntity(
            leftArmCm = 40.0,
            rightArmCm = 40.0,
            leftThighCm = 62.0,
            rightThighCm = 62.0
        )
        val report = engine.calculateReport(listOf(measurement))
        val armSym = report.limbSymmetries.find { it.limbName == "Arms" }!!
        assertEquals(0.0, armSym.deltaCm, 0.001)
        assertEquals(100.0, armSym.symmetryPct, 0.001)
        assertTrue(armSym.isBalanced)
    }

    @Test
    fun testAdonisIndexExactTierBoundaries() {
        // Ratio 1.34 -> Novice
        val rNovice = engine.calculateReport(listOf(BodyMeasurementEntity(waistCm = 100.0, shouldersCm = 134.0)))
        assertEquals(VTaperTier.NOVICE, rNovice.adonisIndex.tier)

        // Ratio 1.35 -> Athletic
        val rAthletic = engine.calculateReport(listOf(BodyMeasurementEntity(waistCm = 100.0, shouldersCm = 135.0)))
        assertEquals(VTaperTier.ATHLETIC, rAthletic.adonisIndex.tier)

        // Ratio 1.50 -> Prime
        val rPrime = engine.calculateReport(listOf(BodyMeasurementEntity(waistCm = 100.0, shouldersCm = 150.0)))
        assertEquals(VTaperTier.PRIME, rPrime.adonisIndex.tier)

        // Ratio 1.618 -> Golden
        val rGolden = engine.calculateReport(listOf(BodyMeasurementEntity(waistCm = 100.0, shouldersCm = 161.8)))
        assertEquals(VTaperTier.GOLDEN, rGolden.adonisIndex.tier)
    }

    @Test
    fun testLimbSymmetryExactHalfCentimeterBoundary() {
        // Delta = 0.50 -> Balanced
        val balancedMeasurement = BodyMeasurementEntity(
            leftArmCm = 35.0,
            rightArmCm = 35.5
        )
        val balancedReport = engine.calculateReport(listOf(balancedMeasurement))
        val armSym = balancedReport.limbSymmetries.find { it.limbName == "Arms" }
        assertNotNull(armSym)
        assertTrue(armSym!!.isBalanced)
        assertEquals(0.5, armSym.deltaCm, 0.001)

        // Delta = 0.51 -> Imbalanced
        val imbalancedMeasurement = BodyMeasurementEntity(
            leftArmCm = 35.0,
            rightArmCm = 35.51
        )
        val imbalancedReport = engine.calculateReport(listOf(imbalancedMeasurement))
        val armImb = imbalancedReport.limbSymmetries.find { it.limbName == "Arms" }
        assertNotNull(armImb)
        assertFalse(armImb!!.isBalanced)
        assertEquals(0.51, armImb.deltaCm, 0.001)
    }

    @Test
    fun testExtremeAdonisRatioClampedAt100Progress() {
        val superGolden = BodyMeasurementEntity(
            waistCm = 70.0,
            shouldersCm = 145.0 // Ratio: 2.07
        )
        val report = engine.calculateReport(listOf(superGolden))
        assertEquals(VTaperTier.GOLDEN, report.adonisIndex.tier)
        assertEquals(100, report.adonisIndex.progressPct)
        assertEquals("Golden Tier", report.adonisIndex.statusSummary)
    }

    @Test
    fun testRecompDeltaClosestBaselineSelectionAcrossMultipleEntries() {
        val now = System.currentTimeMillis()
        val day = 24L * 60L * 60L * 1000L

        // 4 measurements: 120 days ago, 91 days ago, 60 days ago, now
        val m120 = BodyMeasurementEntity(id = 1, recordedAt = now - (120 * day), waistCm = 95.0, shouldersCm = 105.0)
        val m91 = BodyMeasurementEntity(id = 2, recordedAt = now - (91 * day), waistCm = 90.0, shouldersCm = 110.0)
        val m60 = BodyMeasurementEntity(id = 3, recordedAt = now - (60 * day), waistCm = 85.0, shouldersCm = 115.0)
        val mNow = BodyMeasurementEntity(id = 4, recordedAt = now, waistCm = 80.0, shouldersCm = 120.0)

        // 90 day window: m91 is closest to now - 90*day
        val report = engine.calculateReport(listOf(mNow, m60, m91, m120), daysWindow = 90)
        assertNotNull(report.recompDelta)
        assertEquals(-10.0, report.recompDelta!!.waistDeltaCm, 0.001) // 80 - 90
        assertEquals(10.0, report.recompDelta!!.shoulderDeltaCm, 0.001) // 120 - 110
        assertEquals(91, report.recompDelta!!.daysPeriod)
    }

    @Test
    fun testAdonisIndexExactThresholdTransitions() {
        // 1.349 -> NOVICE vs 1.350 -> ATHLETIC
        val mNovice = BodyMeasurementEntity(waistCm = 100.0, shouldersCm = 134.9)
        assertEquals(VTaperTier.NOVICE, engine.calculateReport(listOf(mNovice)).adonisIndex.tier)
        val mAthletic = BodyMeasurementEntity(waistCm = 100.0, shouldersCm = 135.0)
        assertEquals(VTaperTier.ATHLETIC, engine.calculateReport(listOf(mAthletic)).adonisIndex.tier)

        // 1.499 -> ATHLETIC vs 1.500 -> PRIME
        val mAthleticUpper = BodyMeasurementEntity(waistCm = 100.0, shouldersCm = 149.9)
        assertEquals(VTaperTier.ATHLETIC, engine.calculateReport(listOf(mAthleticUpper)).adonisIndex.tier)
        val mPrime = BodyMeasurementEntity(waistCm = 100.0, shouldersCm = 150.0)
        assertEquals(VTaperTier.PRIME, engine.calculateReport(listOf(mPrime)).adonisIndex.tier)

        // 1.617 -> PRIME vs 1.618 -> GOLDEN
        val mPrimeUpper = BodyMeasurementEntity(waistCm = 1000.0, shouldersCm = 1617.0)
        assertEquals(VTaperTier.PRIME, engine.calculateReport(listOf(mPrimeUpper)).adonisIndex.tier)
        val mGolden = BodyMeasurementEntity(waistCm = 1000.0, shouldersCm = 1618.0)
        assertEquals(VTaperTier.GOLDEN, engine.calculateReport(listOf(mGolden)).adonisIndex.tier)
    }

    @Test
    fun testPartialLimbMeasurementExclusion() {
        val measurement = BodyMeasurementEntity(
            leftArmCm = 36.0,
            rightArmCm = 0.0, // missing right arm -> Arms excluded
            leftThighCm = 0.0,
            rightThighCm = 58.0, // missing left thigh -> Thighs excluded
            leftCalfCm = 38.0,
            rightCalfCm = 38.2 // valid both -> Calves included
        )
        val report = engine.calculateReport(listOf(measurement))
        assertEquals(1, report.limbSymmetries.size)
        assertEquals("Calves", report.limbSymmetries[0].limbName)
        assertTrue(report.limbSymmetries[0].isBalanced)
    }

    @Test
    fun testSingleMeasurementHasNullRecompDelta() {
        val single = BodyMeasurementEntity(id = 1, recordedAt = System.currentTimeMillis(), waistCm = 80.0, shouldersCm = 120.0)
        val report = engine.calculateReport(listOf(single))
        assertNull("Single measurement must have null recompDelta", report.recompDelta)
    }

    @Test
    fun testNegativeCircumferenceSafelyReturnsEmptyAdonisIndex() {
        val negativeWaist = BodyMeasurementEntity(waistCm = -80.0, shouldersCm = 120.0)
        val report1 = engine.calculateReport(listOf(negativeWaist))
        assertEquals(0.0, report1.adonisIndex.currentRatio, 0.0)
        assertEquals("No Data", report1.adonisIndex.statusSummary)

        val negativeShoulders = BodyMeasurementEntity(waistCm = 80.0, shouldersCm = -120.0)
        val report2 = engine.calculateReport(listOf(negativeShoulders))
        assertEquals(0.0, report2.adonisIndex.currentRatio, 0.0)
        assertEquals("No Data", report2.adonisIndex.statusSummary)
    }
}

