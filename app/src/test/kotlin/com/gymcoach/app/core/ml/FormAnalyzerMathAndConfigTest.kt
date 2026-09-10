package com.gymcoach.app.core.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Structural tests for FormAnalyzer joint-angle math, visibility/confidence
 * semantics, and production default configs for all nine exercise types.
 *
 * ExampleEthic: every expectation mirrors the committed implementation;
 * these tests lock the CURRENT behavior as the regression baseline.
 */
class FormAnalyzerMathAndConfigTest {

    /**
     * Builds a pose where right-arm vector [shoulder->elbow->wrist] forms the
     * exact requested anatomical angle, with optional per-landmark visibility.
     */
    private fun armPose(
        shoulder: NormalizedLandmark,
        elbow: NormalizedLandmark,
        wrist: NormalizedLandmark,
        visibility: List<Float> = List(33) { 1.0f }
    ): Pose {
        val landmarks = List(33) { NormalizedLandmark(0f, 0f, 0f) }.toMutableList()
        landmarks[12] = shoulder
        landmarks[14] = elbow
        landmarks[16] = wrist
        return Pose(landmarks, visibility)
    }

    /** Feeds identical frames (so the 5-frame average converges to the raw angle). */
    private fun analyzeToConverged(
        analyzer: FormAnalyzer,
        pose: Pose,
        frames: Int = 5
    ): AnalysisResult {
        var last: AnalysisResult? = null
        repeat(frames) { last = analyzer.analyze(pose) }
        return last!!
    }

    // ── Angle math: pure vector geometry through the smoothed pipeline ─────

    @Test
    fun elbowAngle_rightAngle_measures90Degrees() {
        val analyzer = FormAnalyzer(ExerciseType.BICEP_CURL, FormAnalyzer.defaultFor(ExerciseType.BICEP_CURL))
        val result = analyzeToConverged(
            analyzer,
            armPose(
                shoulder = NormalizedLandmark(0f, 0f, 0f),
                elbow = NormalizedLandmark(0f, 1f, 0f),
                wrist = NormalizedLandmark(1f, 1f, 0f)
            )
        )
        assertEquals(90.0, result.angle, 1e-6)
    }

    @Test
    fun elbowAngle_straightLine_measures180Degrees() {
        val analyzer = FormAnalyzer(ExerciseType.BICEP_CURL, FormAnalyzer.defaultFor(ExerciseType.BICEP_CURL))
        val result = analyzeToConverged(
            analyzer,
            armPose(
                shoulder = NormalizedLandmark(0f, 0f, 0f),
                elbow = NormalizedLandmark(0f, 1f, 0f),
                wrist = NormalizedLandmark(0f, 2f, 0f)
            )
        )
        assertEquals(180.0, result.angle, 1e-6)
    }

    @Test
    fun elbowAngle_acuteAngle_measures45Degrees() {
        val analyzer = FormAnalyzer(ExerciseType.BICEP_CURL, FormAnalyzer.defaultFor(ExerciseType.BICEP_CURL))
        val result = analyzeToConverged(
            analyzer,
            armPose(
                shoulder = NormalizedLandmark(0f, 0f, 0f),
                elbow = NormalizedLandmark(0f, 1f, 0f),
                wrist = NormalizedLandmark(1f, 0f, 0f)
            )
        )
        assertEquals(45.0, result.angle, 1e-6)
    }

    @Test
    fun degenerateVectors_measureMinusOne_invalidFeedback() {
        val analyzer = FormAnalyzer(ExerciseType.BICEP_CURL, FormAnalyzer.defaultFor(ExerciseType.BICEP_CURL))
        // shoulder == elbow == wrist  -> zero-length vectors
        val result = analyzer.analyze(
            armPose(
                shoulder = NormalizedLandmark(0f, 1f, 0f),
                elbow = NormalizedLandmark(0f, 1f, 0f),
                wrist = NormalizedLandmark(0f, 1f, 0f)
            )
        )
        assertNotNull(result)
        assertEquals(-1.0, result!!.angle, 1e-9)
        assertEquals("Landmarks not detected", result.formFeedback)
    }

    @Test
    fun squatKneeAngle_rightAngle_measures90Degrees() {
        val analyzer = FormAnalyzer(ExerciseType.SQUAT, FormAnalyzer.defaultFor(ExerciseType.SQUAT))
        // hip(24)=(0,0), knee(26)=(0,1), ankle(28)=(1,1) -> right angle
        val landmarks = List(33) { NormalizedLandmark(0f, 0f, 0f) }.toMutableList()
        landmarks[24] = NormalizedLandmark(0f, 0f, 0f)
        landmarks[26] = NormalizedLandmark(0f, 1f, 0f)
        landmarks[28] = NormalizedLandmark(1f, 1f, 0f)
        val result = analyzeToConverged(analyzer, Pose(landmarks, List(33) { 1.0f }))
        assertEquals(90.0, result.angle, 1e-6)
    }

    // ── Visibility / confidence semantics ──────────────────────────────────

    @Test
    fun emptyVisibility_defaultsToConfidenceOne() {
        val analyzer = FormAnalyzer(ExerciseType.BICEP_CURL, FormAnalyzer.defaultFor(ExerciseType.BICEP_CURL))
        val result = analyzer.analyze(
            armPose(
                shoulder = NormalizedLandmark(0f, 0f, 0f),
                elbow = NormalizedLandmark(0f, 1f, 0f),
                wrist = NormalizedLandmark(1f, 1f, 0f),  // true 90 deg
                visibility = emptyList()
            )
        )
        assertNotNull(result)
        assertEquals(1.0, result!!.confidence, 1e-9)
    }

    @Test
    fun partialVisibility_averagesRequiredLandmarks() {
        val analyzer = FormAnalyzer(ExerciseType.BICEP_CURL, FormAnalyzer.defaultFor(ExerciseType.BICEP_CURL))
        // Required for bicep curl: indices 12, 14, 16 only.
        val visibility = MutableList(33) { 0.2f }  // everything else below threshold
        visibility[12] = 1.0f
        visibility[14] = 0.8f
        visibility[16] = 0.6f
        val result = analyzer.analyze(
            armPose(
                shoulder = NormalizedLandmark(0f, 0f, 0f),
                elbow = NormalizedLandmark(0f, 1f, 0f),
                wrist = NormalizedLandmark(1f, 1f, 0f),
                visibility = visibility
            )
        )
        assertNotNull(result)
        assertEquals((1.0 + 0.8 + 0.6) / 3.0, result!!.confidence, 1e-6)
    }

    @Test
    fun visibilityAtThreshold_acceptsFrame() {
        val analyzer = FormAnalyzer(ExerciseType.BICEP_CURL, FormAnalyzer.defaultFor(ExerciseType.BICEP_CURL))
        // A landmark AT minConfidence (0.5) still counts as visible.
        val visibility = MutableList(33) { 1.0f }
        visibility[16] = 0.5f
        val result = analyzer.analyze(
            armPose(
                shoulder = NormalizedLandmark(0f, 0f, 0f),
                elbow = NormalizedLandmark(0f, 1f, 0f),
                wrist = NormalizedLandmark(0f, 2f, 0f),
                visibility = visibility
            )
        )
        assertNotNull(result)
    }

    @Test
    fun visibilityBelowThreshold_rejectsFrame() {
        val analyzer = FormAnalyzer(ExerciseType.BICEP_CURL, FormAnalyzer.defaultFor(ExerciseType.BICEP_CURL))
        val visibility = MutableList(33) { 1.0f }
        visibility[16] = 0.49f
        val result = analyzer.analyze(
            armPose(
                shoulder = NormalizedLandmark(0f, 0f, 0f),
                elbow = NormalizedLandmark(0f, 1f, 0f),
                wrist = NormalizedLandmark(0f, 2f, 0f),
                visibility = visibility
            )
        )
        assertNull(result)
    }

    // ── Phase dead-zone semantics (locked behavior) ────────────────────────

    @Test
    fun bicepCurl_deadZoneAngle_holdsLastPhase() {
        val analyzer = FormAnalyzer(ExerciseType.BICEP_CURL, FormAnalyzer.defaultFor(ExerciseType.BICEP_CURL))
        // down=150 / up=50; 100 sits in the dead zone.
        val baseline = analyzeToConverged(
            analyzer,
            armPose(
                shoulder = NormalizedLandmark(0f, 0f, 0f),
                elbow = NormalizedLandmark(0f, 1f, 0f),
                wrist = NormalizedLandmark(0f, 2f, 0f)  // 180 deg -> DOWN
            ),
            frames = 5
        )
        assertEquals(RepPhase.DOWN, baseline.currentPhase)
        val deadZone = analyzeToConverged(
            analyzer,
            armPose(
                shoulder = NormalizedLandmark(0f, 0f, 0f),
                elbow = NormalizedLandmark(0f, 1f, 0f),
                wrist = NormalizedLandmark(1f, 1f, 0f)  // 90 deg -> dead zone
            ),
            frames = 5
        )
        assertEquals(RepPhase.DOWN, deadZone.currentPhase)  // holds prior phase
        assertEquals(0, deadZone.repCount)  // no rep counted in dead zone
    }

    // ── Production default configs for all exercise types ──────────────────

    @Test
    fun defaultConfigs_allTypes_withinThresholdBounds() {
        ExerciseType.values().forEach { type ->
            val config = FormAnalyzer.defaultFor(type)
            val msg = "type=$type"
            assertTrue(msg, config.downThreshold in 0.0..180.0)
            assertTrue(msg, config.upThreshold in 0.0..180.0)
            assertTrue(msg, config.minConfidence in 0.0..1.0)
            assertEquals(msg, 0.0, config.validAngleRange.start, 1e-9)
            assertEquals(msg, 180.0, config.validAngleRange.endInclusive, 1e-9)
        }
    }

    @Test
    fun defaultConfigs_onlyPlankIsTimeBased() {
        ExerciseType.values().forEach { type ->
            val config = FormAnalyzer.defaultFor(type)
            if (type == ExerciseType.PLANK) {
                assertTrue("PLANK must be time-based", config.isTimeBased)
                assertTrue("PLANK needs a hold duration", config.holdDurationMs > 0)
            } else {
                assertFalse("$type must be angle-based", config.isTimeBased)
            }
        }
    }

    @Test
    fun defaultConfigs_bicepCurlThresholdsMatchFeedbackBoundaries() {
        val config = FormAnalyzer.defaultFor(ExerciseType.BICEP_CURL)
        // Lock: down=150 / up=50 (rep counted on crossing UP below 50 deg).
        assertEquals(150.0, config.downThreshold, 1e-9)
        assertEquals(50.0, config.upThreshold, 1e-9)
        assertEquals(0.5, config.minConfidence, 1e-9)
    }
}