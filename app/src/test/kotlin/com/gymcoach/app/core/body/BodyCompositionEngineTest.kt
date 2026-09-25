package com.gymcoach.app.core.body

import com.gymcoach.app.data.local.entity.BodyMeasurementEntity
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class BodyCompositionEngineTest {

    @Test
    fun testVTaperCalculationAndCategories() {
        val standard = BodyMeasurementEntity(shouldersCm = 100.0, waistCm = 80.0) // 1.25 -> Standard
        val athletic = BodyMeasurementEntity(shouldersCm = 120.0, waistCm = 80.0) // 1.5 -> Athletic
        val golden = BodyMeasurementEntity(shouldersCm = 128.0, waistCm = 80.0) // 1.6 -> Golden
        val heroic = BodyMeasurementEntity(shouldersCm = 136.0, waistCm = 80.0) // 1.7 -> Heroic

        val trendStandard = BodyCompositionEngine.calculateTrend(listOf(standard))
        assertEquals(1.25, trendStandard.vTaperRatio!!, 0.01)
        assertEquals("Standard Frame", trendStandard.vTaperCategory)

        val trendAthletic = BodyCompositionEngine.calculateTrend(listOf(athletic))
        assertEquals(1.5, trendAthletic.vTaperRatio!!, 0.01)
        assertEquals("Athletic Taper", trendAthletic.vTaperCategory)

        val trendGolden = BodyCompositionEngine.calculateTrend(listOf(golden))
        assertEquals(1.6, trendGolden.vTaperRatio!!, 0.01)
        assertEquals("Golden V-Taper", trendGolden.vTaperCategory)

        val trendHeroic = BodyCompositionEngine.calculateTrend(listOf(heroic))
        assertEquals(1.7, trendHeroic.vTaperRatio!!, 0.01)
        assertEquals("Heroic Frame", trendHeroic.vTaperCategory)
    }

    @Test
    fun testGoldenRatioProximityCalculation() {
        // formula: max(0f, 1f - abs(ratio - 1.618) / 0.4f)
        val entityExact = BodyMeasurementEntity(shouldersCm = 161.8, waistCm = 100.0) // ratio 1.618
        val trendExact = BodyCompositionEngine.calculateTrend(listOf(entityExact))
        assertEquals(1.0f, trendExact.goldenRatioProximityPct, 0.001f)

        val entityOff = BodyMeasurementEntity(shouldersCm = 121.8, waistCm = 100.0) // ratio 1.218 (0.4 off)
        val trendOff = BodyCompositionEngine.calculateTrend(listOf(entityOff))
        assertEquals(0.0f, trendOff.goldenRatioProximityPct, 0.001f)

        val nullEntity = BodyMeasurementEntity(shouldersCm = 0.0, waistCm = 0.0)
        val trendNull = BodyCompositionEngine.calculateTrend(listOf(nullEntity))
        assertEquals(0.0f, trendNull.goldenRatioProximityPct, 0.001f)
    }

    @Test
    fun testWeeklyChangeRateLogic() {
        val t2 = System.currentTimeMillis()
        val t1 = t2 - TimeUnit.DAYS.toMillis(14) // 2 weeks apart

        val oldMeasurement = BodyMeasurementEntity(recordedAt = t1, weightKg = 80.0)
        val newMeasurement = BodyMeasurementEntity(recordedAt = t2, weightKg = 82.0) // +2.0kg over 2 weeks = +1.0kg/wk

        val trend = BodyCompositionEngine.calculateTrend(listOf(newMeasurement, oldMeasurement))

        assertEquals(82.0, trend.currentWeightKg, 0.01)
        assertEquals(80.0, trend.previousWeightKg)
        assertEquals(1.0, trend.weeklyRateKg, 0.01)
    }

    @Test
    fun testCircumferenceDeltas() {
        val t2 = System.currentTimeMillis()
        val t1 = t2 - TimeUnit.DAYS.toMillis(1)

        val oldMeasurement = BodyMeasurementEntity(
            recordedAt = t1,
            chestCm = 100.0,
            leftArmCm = 35.0,
            rightArmCm = 35.0
        )
        val newMeasurement = BodyMeasurementEntity(
            recordedAt = t2,
            chestCm = 102.0,
            leftArmCm = 36.0,
            rightArmCm = 36.0
        )

        val trend = BodyCompositionEngine.calculateTrend(listOf(newMeasurement, oldMeasurement))

        val chestDelta = trend.circumferences[BodyPart.CHEST]
        assertNotNull(chestDelta)
        assertEquals(102.0, chestDelta!!.currentCm, 0.01)
        assertEquals(2.0, chestDelta.deltaCm, 0.01)

        val armsDelta = trend.circumferences[BodyPart.ARMS]
        assertNotNull(armsDelta)
        assertEquals(36.0, armsDelta!!.currentCm, 0.01)
        assertEquals(1.0, armsDelta.deltaCm, 0.01)

        val waistDelta = trend.circumferences[BodyPart.WAIST]
        assertNull(waistDelta) // Because it's 0.0 in both entities
    }

    @Test
    fun testEmptyHistoryReturnsBaselineTrend() {
        val trend = BodyCompositionEngine.calculateTrend(emptyList())
        assertEquals(0.0, trend.currentWeightKg, 0.001)
        assertNull(trend.previousWeightKg)
        assertEquals(0.0, trend.weeklyRateKg, 0.001)
        assertNull(trend.currentBodyFatPct)
        assertNull(trend.vTaperRatio)
        assertEquals("Unknown", trend.vTaperCategory)
        assertEquals(0.0f, trend.goldenRatioProximityPct, 0.001f)
        assertTrue(trend.circumferences.isEmpty())
        assertTrue(trend.history.isEmpty())
    }

    @Test
    fun testZeroOrNegativeTimeDifferenceYieldsZeroWeeklyRate() {
        val t = System.currentTimeMillis()
        val m1 = BodyMeasurementEntity(recordedAt = t, weightKg = 80.0)
        val m2 = BodyMeasurementEntity(recordedAt = t, weightKg = 82.0) // Identical timestamps

        val trend = BodyCompositionEngine.calculateTrend(listOf(m2, m1))
        assertEquals(0.0, trend.weeklyRateKg, 0.001)
    }

    @Test
    fun testAllCircumferenceBodyPartsDeltas() {
        val t = System.currentTimeMillis()
        val oldM = BodyMeasurementEntity(
            recordedAt = t - 86400000L,
            shouldersCm = 115.0,
            chestCm = 100.0,
            waistCm = 85.0,
            hipsCm = 95.0,
            leftArmCm = 35.0,
            rightArmCm = 35.0,
            leftThighCm = 58.0,
            rightThighCm = 58.0,
            leftCalfCm = 38.0,
            rightCalfCm = 38.0
        )
        val newM = BodyMeasurementEntity(
            recordedAt = t,
            shouldersCm = 117.0, // +2
            chestCm = 101.5,     // +1.5
            waistCm = 83.0,      // -2
            hipsCm = 94.0,       // -1
            leftArmCm = 36.0,    // +1
            rightArmCm = 36.0,
            leftThighCm = 59.0,  // +1
            rightThighCm = 59.0,
            leftCalfCm = 38.5,   // +0.5
            rightCalfCm = 38.5
        )

        val trend = BodyCompositionEngine.calculateTrend(listOf(newM, oldM))
        assertEquals(2.0, trend.circumferences[BodyPart.SHOULDERS]?.deltaCm ?: 0.0, 0.01)
        assertEquals(1.5, trend.circumferences[BodyPart.CHEST]?.deltaCm ?: 0.0, 0.01)
        assertEquals(-2.0, trend.circumferences[BodyPart.WAIST]?.deltaCm ?: 0.0, 0.01)
        assertEquals(-1.0, trend.circumferences[BodyPart.HIPS]?.deltaCm ?: 0.0, 0.01)
        assertEquals(1.0, trend.circumferences[BodyPart.ARMS]?.deltaCm ?: 0.0, 0.01)
        assertEquals(1.0, trend.circumferences[BodyPart.THIGHS]?.deltaCm ?: 0.0, 0.01)
        assertEquals(0.5, trend.circumferences[BodyPart.CALVES]?.deltaCm ?: 0.0, 0.01)
    }

    @Test
    fun testVTaperCategoryBoundaryTransitions() {
        val trend139 = BodyCompositionEngine.calculateTrend(listOf(BodyMeasurementEntity(shouldersCm = 139.0, waistCm = 100.0)))
        assertEquals("Standard Frame", trend139.vTaperCategory)

        val trend140 = BodyCompositionEngine.calculateTrend(listOf(BodyMeasurementEntity(shouldersCm = 140.0, waistCm = 100.0)))
        assertEquals("Athletic Taper", trend140.vTaperCategory)

        val trend154 = BodyCompositionEngine.calculateTrend(listOf(BodyMeasurementEntity(shouldersCm = 154.0, waistCm = 100.0)))
        assertEquals("Athletic Taper", trend154.vTaperCategory)

        val trend155 = BodyCompositionEngine.calculateTrend(listOf(BodyMeasurementEntity(shouldersCm = 155.0, waistCm = 100.0)))
        assertEquals("Golden V-Taper", trend155.vTaperCategory)

        val trend165 = BodyCompositionEngine.calculateTrend(listOf(BodyMeasurementEntity(shouldersCm = 165.0, waistCm = 100.0)))
        assertEquals("Golden V-Taper", trend165.vTaperCategory)

        val trend166 = BodyCompositionEngine.calculateTrend(listOf(BodyMeasurementEntity(shouldersCm = 166.0, waistCm = 100.0)))
        assertEquals("Heroic Frame", trend166.vTaperCategory)
    }

    @Test
    fun testAsymmetricSingleLimbAveragingResilience() {
        val t = System.currentTimeMillis()
        val oldM = BodyMeasurementEntity(
            recordedAt = t - 86400000L,
            leftArmCm = 37.0,
            rightArmCm = 0.0 // Only left recorded previously
        )
        val newM = BodyMeasurementEntity(
            recordedAt = t,
            leftArmCm = 38.0,
            rightArmCm = 38.0 // Both recorded currently
        )

        val trend = BodyCompositionEngine.calculateTrend(listOf(newM, oldM))
        val armsDelta = trend.circumferences[BodyPart.ARMS]
        assertNotNull(armsDelta)
        assertEquals(38.0, armsDelta!!.currentCm, 0.001)
        // delta = 38.0 - 37.0 = 1.0 (does not divide 37 by 2 to yield 18.5)
        assertEquals(1.0, armsDelta.deltaCm, 0.001)
    }

    @Test
    fun testGoldenRatioProximityBeyondQuarterBoundaryClampsToZero() {
        // Ratio = 2.10 (diff = 0.482 > 0.4) -> must clamp to 0.0f
        val distantRatio = BodyMeasurementEntity(shouldersCm = 210.0, waistCm = 100.0)
        val trend = BodyCompositionEngine.calculateTrend(listOf(distantRatio))
        assertEquals(0.0f, trend.goldenRatioProximityPct, 0.001f)

        // Ratio = 1.818 (diff = 0.200 = exactly half of 0.4) -> proximity = 0.5f
        val halfwayRatio = BodyMeasurementEntity(shouldersCm = 181.8, waistCm = 100.0)
        val trendHalf = BodyCompositionEngine.calculateTrend(listOf(halfwayRatio))
        assertEquals(0.5f, trendHalf.goldenRatioProximityPct, 0.01f)
    }

    @Test
    fun testUnsortedHistoryChronologicalOrdering() {
        val tBase = 1700000000000L
        val mOldest = BodyMeasurementEntity(recordedAt = tBase, weightKg = 75.0)
        val mMiddle = BodyMeasurementEntity(recordedAt = tBase + 86400000L * 7, weightKg = 76.0)
        val mNewest = BodyMeasurementEntity(recordedAt = tBase + 86400000L * 14, weightKg = 77.0)

        // Pass in scrambled chronological order
        val trend = BodyCompositionEngine.calculateTrend(listOf(mMiddle, mOldest, mNewest))

        assertEquals(77.0, trend.currentWeightKg, 0.001)
        assertEquals(76.0, trend.previousWeightKg!!, 0.001)
        assertEquals(1.0, trend.weeklyRateKg, 0.01)
        assertEquals(3, trend.history.size)
        assertEquals(tBase + 86400000L * 14, trend.history[0].recordedAt)
        assertEquals(tBase + 86400000L * 7, trend.history[1].recordedAt)
        assertEquals(tBase, trend.history[2].recordedAt)
    }

    @Test
    fun testBodyFatPercentageSanitization() {
        // Zero body fat percentage -> sanitized to null
        val zeroFat = BodyMeasurementEntity(weightKg = 80.0, bodyFatPct = 0.0)
        val trendZero = BodyCompositionEngine.calculateTrend(listOf(zeroFat))
        assertNull(trendZero.currentBodyFatPct)

        // Negative body fat percentage -> sanitized to null
        val negativeFat = BodyMeasurementEntity(weightKg = 80.0, bodyFatPct = -5.0)
        val trendNegative = BodyCompositionEngine.calculateTrend(listOf(negativeFat))
        assertNull(trendNegative.currentBodyFatPct)

        // Valid positive body fat percentage -> retained
        val validFat = BodyMeasurementEntity(weightKg = 80.0, bodyFatPct = 14.5)
        val trendValid = BodyCompositionEngine.calculateTrend(listOf(validFat))
        assertEquals(14.5, trendValid.currentBodyFatPct!!, 0.001)
    }

    @Test
    fun testWeightLossWeeklyRateCalculation() {
        val t2 = System.currentTimeMillis()
        val t1 = t2 - TimeUnit.DAYS.toMillis(21) // 3 weeks ago

        val previous = BodyMeasurementEntity(recordedAt = t1, weightKg = 85.0)
        val latest = BodyMeasurementEntity(recordedAt = t2, weightKg = 83.5) // -1.5kg over 3 weeks = -0.5kg/week

        val trend = BodyCompositionEngine.calculateTrend(listOf(latest, previous))
        assertEquals(83.5, trend.currentWeightKg, 0.001)
        assertEquals(85.0, trend.previousWeightKg!!, 0.001)
        assertEquals(-0.5, trend.weeklyRateKg, 0.01)
    }

    @Test
    fun testSubWeeklyIntervalWeeklyRateCalculation() {
        val t2 = System.currentTimeMillis()
        val t1 = t2 - (TimeUnit.DAYS.toMillis(7) / 2) // 3.5 days ago (0.5 weeks)

        val previous = BodyMeasurementEntity(recordedAt = t1, weightKg = 70.0)
        val latest = BodyMeasurementEntity(recordedAt = t2, weightKg = 70.5) // +0.5kg in 0.5 weeks = +1.0kg/week

        val trend = BodyCompositionEngine.calculateTrend(listOf(latest, previous))
        assertEquals(1.0, trend.weeklyRateKg, 0.01)
    }

    @Test
    fun testSingleMeasurementHistoryBaselineDeltas() {
        val t = System.currentTimeMillis()
        val single = BodyMeasurementEntity(
            recordedAt = t,
            weightKg = 78.0,
            chestCm = 102.0,
            shouldersCm = 120.0,
            waistCm = 80.0,
            leftArmCm = 36.0,
            rightArmCm = 36.0
        )

        val trend = BodyCompositionEngine.calculateTrend(listOf(single))
        assertEquals(78.0, trend.currentWeightKg, 0.001)
        assertNull(trend.previousWeightKg)
        assertEquals(0.0, trend.weeklyRateKg, 0.001)
        assertEquals(1, trend.history.size)

        // All present parts should have deltaCm = 0.0
        assertEquals(102.0, trend.circumferences[BodyPart.CHEST]?.currentCm ?: 0.0, 0.001)
        assertEquals(0.0, trend.circumferences[BodyPart.CHEST]?.deltaCm ?: -1.0, 0.001)
        assertEquals(120.0, trend.circumferences[BodyPart.SHOULDERS]?.currentCm ?: 0.0, 0.001)
        assertEquals(0.0, trend.circumferences[BodyPart.SHOULDERS]?.deltaCm ?: -1.0, 0.001)
        assertEquals(36.0, trend.circumferences[BodyPart.ARMS]?.currentCm ?: 0.0, 0.001)
        assertEquals(0.0, trend.circumferences[BodyPart.ARMS]?.deltaCm ?: -1.0, 0.001)
    }

    @Test
    fun testMissingOrZeroShouldersOrWaistVTaperHandling() {
        // Zero shoulders
        val zeroShoulders = BodyMeasurementEntity(shouldersCm = 0.0, waistCm = 80.0)
        val trendZeroShoulders = BodyCompositionEngine.calculateTrend(listOf(zeroShoulders))
        assertNull(trendZeroShoulders.vTaperRatio)
        assertEquals("Unknown", trendZeroShoulders.vTaperCategory)
        assertEquals(0.0f, trendZeroShoulders.goldenRatioProximityPct, 0.001f)

        // Zero waist
        val zeroWaist = BodyMeasurementEntity(shouldersCm = 120.0, waistCm = 0.0)
        val trendZeroWaist = BodyCompositionEngine.calculateTrend(listOf(zeroWaist))
        assertNull(trendZeroWaist.vTaperRatio)
        assertEquals("Unknown", trendZeroWaist.vTaperCategory)
        assertEquals(0.0f, trendZeroWaist.goldenRatioProximityPct, 0.001f)

        // Negative values
        val negativeDimensions = BodyMeasurementEntity(shouldersCm = -120.0, waistCm = 80.0)
        val trendNegative = BodyCompositionEngine.calculateTrend(listOf(negativeDimensions))
        assertNull(trendNegative.vTaperRatio)
        assertEquals("Unknown", trendNegative.vTaperCategory)
        assertEquals(0.0f, trendNegative.goldenRatioProximityPct, 0.001f)
    }

    @Test
    fun testBilateralLimbAveragingAcrossAllLimbPairs() {
        val t = System.currentTimeMillis()
        val oldM = BodyMeasurementEntity(
            recordedAt = t - TimeUnit.DAYS.toMillis(7),
            leftArmCm = 35.0,
            rightArmCm = 35.0,
            leftThighCm = 58.0,
            rightThighCm = 58.0,
            leftCalfCm = 0.0,
            rightCalfCm = 39.0 // Calf only right previously
        )
        val newM = BodyMeasurementEntity(
            recordedAt = t,
            leftArmCm = 36.0,
            rightArmCm = 38.0, // Arms: (36 + 38) / 2 = 37.0 -> delta = 37 - 35 = +2.0
            leftThighCm = 0.0,
            rightThighCm = 60.0, // Thigh: only right currently = 60.0 -> delta = 60 - 58 = +2.0
            leftCalfCm = 40.0,
            rightCalfCm = 0.0 // Calf: only left currently = 40.0 -> delta = 40 - 39 = +1.0
        )

        val trend = BodyCompositionEngine.calculateTrend(listOf(newM, oldM))

        val armDelta = trend.circumferences[BodyPart.ARMS]
        assertNotNull(armDelta)
        assertEquals(37.0, armDelta!!.currentCm, 0.001)
        assertEquals(2.0, armDelta.deltaCm, 0.001)

        val thighDelta = trend.circumferences[BodyPart.THIGHS]
        assertNotNull(thighDelta)
        assertEquals(60.0, thighDelta!!.currentCm, 0.001)
        assertEquals(2.0, thighDelta.deltaCm, 0.001)

        val calfDelta = trend.circumferences[BodyPart.CALVES]
        assertNotNull(calfDelta)
        assertEquals(40.0, calfDelta!!.currentCm, 0.001)
        assertEquals(1.0, calfDelta.deltaCm, 0.001)
    }
}

