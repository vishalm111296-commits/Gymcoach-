package com.gymcoach.app.core.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FormAnalyzerTest {

    private lateinit var analyzer: FormAnalyzer

    @Before
    fun setUp() {
        val config = ExerciseConfig(
            downThreshold = 90.0,
            upThreshold = 150.0,
            minConfidence = 0.5
        )
        analyzer = FormAnalyzer(ExerciseType.BICEP_CURL, config)
    }

    private fun createPose(angle: Double, visible: Boolean = true): Pose {
        val rad = Math.toRadians(180 - angle)
        val landmarks = List(33) { NormalizedLandmark(0f, 0f, 0f) }.toMutableList()
        landmarks[12] = NormalizedLandmark(0f, 0f, 0f)
        landmarks[14] = NormalizedLandmark(0f, 1f, 0f)
        landmarks[16] = NormalizedLandmark(Math.sin(rad).toFloat(), 1f + Math.cos(rad).toFloat(), 0f)
        val visibility = List(33) { if (visible) 1.0f else 0.0f }
        return Pose(landmarks, visibility)
    }

    @Test
    fun `detects complete rep`() {
        for(i in 0..4) analyzer.analyze(createPose(160.0))
        for(i in 0..4) analyzer.analyze(createPose(80.0))
        var result: AnalysisResult? = null
        for(i in 0..4) {
             result = analyzer.analyze(createPose(160.0))
        }
        assertNotNull(result)
        assertEquals(1, result!!.repCount)
    }

    @Test
    fun `ignores low confidence frames`() {
        for(i in 0..4) analyzer.analyze(createPose(160.0))
        val result = analyzer.analyze(createPose(80.0, visible = false))
        assertNull(result)
    }
}
