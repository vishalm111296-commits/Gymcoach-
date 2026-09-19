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
}
