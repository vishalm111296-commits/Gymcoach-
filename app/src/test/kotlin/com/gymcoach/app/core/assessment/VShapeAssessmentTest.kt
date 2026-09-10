package com.gymcoach.app.core.assessment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class VShapeAssessmentTest {

    private val DELTA = 1e-9

    @Test
    fun testRatioMath() {
        val result = VShapeAssessmentCalculator.assess(60.0, 40.0, 90.0, 0.0, 0.0)
        assertEquals(1.5, result.morphology.shoulderWaistRatio!!, DELTA)
        assertEquals(40.0 / 90.0, result.morphology.waistHipRatio!!, DELTA)
        assertTrue(result.morphology.hasMeasurements)
    }

    @Test
    fun testZeroGuardsAllZero() {
        val result = VShapeAssessmentCalculator.assess(0.0, 0.0, 0.0, 0.0, 0.0)
        assertEquals(VShapeLevel.NOT_ENOUGH_DATA, result.level)
        assertNull(result.morphology.shoulderWaistRatio)
        assertNull(result.morphology.waistHipRatio)
        assertFalse(result.morphology.hasMeasurements)
        assertEquals(1, result.insights.size)
        assertEquals("Log shoulders and waist to assess your V-shape", result.insights[0])
    }

    @Test
    fun testZeroGuardsWaistZero() {
        val result = VShapeAssessmentCalculator.assess(60.0, 0.0, 90.0, 0.0, 0.0)
        assertEquals(VShapeLevel.NOT_ENOUGH_DATA, result.level)
        assertNull(result.morphology.shoulderWaistRatio)
        assertNull(result.morphology.waistHipRatio)
        assertFalse(result.morphology.hasMeasurements)
        assertEquals(1, result.insights.size)
        assertEquals("Log shoulders and waist to assess your V-shape", result.insights[0])
    }

    @Test
    fun testZeroGuardsShouldersZero() {
        val result = VShapeAssessmentCalculator.assess(0.0, 40.0, 90.0, 0.0, 0.0)
        assertEquals(VShapeLevel.NOT_ENOUGH_DATA, result.level)
        assertNull(result.morphology.shoulderWaistRatio)
        // waistHipRatio only requires waist and hips > 0, so it is still computed
        assertEquals(40.0 / 90.0, result.morphology.waistHipRatio!!, DELTA)
        assertFalse(result.morphology.hasMeasurements)
        assertEquals(1, result.insights.size)
        assertEquals("Log shoulders and waist to assess your V-shape", result.insights[0])
    }

    @Test
    fun testLevelBoundaryEarly() {
        val result = VShapeAssessmentCalculator.assess(100.0, 100.0 / 1.29, 90.0, 0.0, 0.0)
        assertEquals(VShapeLevel.EARLY, result.level)
    }

    @Test
    fun testLevelBoundaryBuildingLower() {
        val result = VShapeAssessmentCalculator.assess(100.0, 100.0 / 1.30, 90.0, 0.0, 0.0)
        assertEquals(VShapeLevel.BUILDING, result.level)
    }

    @Test
    fun testLevelBoundaryBuildingUpper() {
        val result = VShapeAssessmentCalculator.assess(100.0, 100.0 / 1.44, 90.0, 0.0, 0.0)
        assertEquals(VShapeLevel.BUILDING, result.level)
    }

    @Test
    fun testLevelBoundaryDevelopingLower() {
        val result = VShapeAssessmentCalculator.assess(100.0, 100.0 / 1.45, 90.0, 0.0, 0.0)
        assertEquals(VShapeLevel.DEVELOPING, result.level)
    }

    @Test
    fun testLevelBoundaryDevelopingUpper() {
        val result = VShapeAssessmentCalculator.assess(100.0, 100.0 / 1.59, 90.0, 0.0, 0.0)
        assertEquals(VShapeLevel.DEVELOPING, result.level)
    }

    @Test
    fun testLevelBoundaryStrong() {
        val result = VShapeAssessmentCalculator.assess(100.0, 100.0 / 1.60, 90.0, 0.0, 0.0)
        assertEquals(VShapeLevel.STRONG, result.level)
    }

    @Test
    fun testTrainingInsightPrimaryUnderloaded() {
        val result = VShapeAssessmentCalculator.assess(100.0, 50.0, 90.0, 1.0, 1.0)
        assertTrue(result.insights.contains("Add lateral delt and lat volume to drive the V-shape"))
    }

    @Test
    fun testTrainingInsightSecondaryUnderloaded() {
        val result = VShapeAssessmentCalculator.assess(100.0, 50.0, 90.0, 3.0, 1.0)
        assertTrue(result.insights.contains("Upper chest and rear delt volume is underloaded"))
    }

    @Test
    fun testTrainingInsightBothExcellent() {
        val result = VShapeAssessmentCalculator.assess(100.0, 50.0, 90.0, 3.0, 3.0)
        assertTrue(result.insights.contains("Excellent V-taper training balance"))
        assertFalse(result.insights.contains("Add lateral delt and lat volume to drive the V-shape"))
        assertFalse(result.insights.contains("Upper chest and rear delt volume is underloaded"))
    }

    @Test
    fun testTrainingInsightBalanced() {
        val result = VShapeAssessmentCalculator.assess(100.0, 50.0, 90.0, 2.5, 2.5)
        assertTrue(result.insights.contains("Keep training balanced across lats, lateral delts, and rear delts"))
    }

    @Test
    fun testCombinedInsightOrdering() {
        // Both underload branches fire (1.0 < 2.0 and 1.2 < 2.0); "Keep training
        // balanced" is mutually exclusive with the underload branches, so the
        // combined list has exactly two ordered insights.
        val result = VShapeAssessmentCalculator.assess(60.0, 40.0, 90.0, 1.0, 1.2)
        assertEquals(2, result.insights.size)
        assertEquals("Add lateral delt and lat volume to drive the V-shape", result.insights[0])
        assertEquals("Upper chest and rear delt volume is underloaded", result.insights[1])
    }

    @Test
    fun testCombinedInsightMiddleBandOrdering() {
        // Middle band (2.0 <= scores < 3.0): neither underload branch fires and
        // the excellent branch does not fire, so the single "keep balanced"
        // insight is the only one present.
        val result = VShapeAssessmentCalculator.assess(60.0, 40.0, 90.0, 2.5, 2.5)
        assertEquals(1, result.insights.size)
        assertEquals("Keep training balanced across lats, lateral delts, and rear delts", result.insights[0])
    }

    @Test
    fun testLevelLabelStrings() {
        assertEquals("Not enough data", VShapeLevel.NOT_ENOUGH_DATA.label)
        assertEquals("Early", VShapeLevel.EARLY.label)
        assertEquals("Building", VShapeLevel.BUILDING.label)
        assertEquals("Developing", VShapeLevel.DEVELOPING.label)
        assertEquals("Strong", VShapeLevel.STRONG.label)
    }

    @Test
    fun testFormatRatio() {
        assertEquals("1.50", VShapeAssessmentCalculator.formatRatio(1.5))
        assertEquals("1.33", VShapeAssessmentCalculator.formatRatio(1.333333))
        assertEquals("—", VShapeAssessmentCalculator.formatRatio(null))
    }
}