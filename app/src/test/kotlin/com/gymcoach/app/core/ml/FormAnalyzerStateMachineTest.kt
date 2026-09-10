package com.gymcoach.app.core.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec-lock tests for the FormAnalyzer rep-counting state machine using the
 * REAL production configs from [FormAnalyzer.defaultFor].
 *
 * Semantics locked (current behavior, documented for future changes):
 *  - Angle is 5-frame smoothed average; rep is counted ONLY on the
 *    DOWN -> UP phase transition (falls back to DOWN when undecided).
 *  - Phases: UP when smoothed < upThreshold, DOWN when smoothed > downThreshold.
 *  - Required landmarks invisible for 3+ consecutive frames resets the session.
 *  - INVALID angles (-1) for 10+ consecutive frames resets the session.
 */
class FormAnalyzerStateMachineTest {

    private fun poseAtAngle(angle: Double, visibilityValue: Float = 1.0f): Pose {
        // 12=shoulder(0,0), 14=elbow(0,1), 16=wrist -> joint angle == `angle`
        val rad = Math.toRadians(180 - angle)
        val landmarks = List(33) { NormalizedLandmark(0f, 0f, 0f) }.toMutableList()
        landmarks[12] = NormalizedLandmark(0f, 0f, 0f)
        landmarks[14] = NormalizedLandmark(0f, 1f, 0f)
        landmarks[16] = NormalizedLandmark(
            Math.sin(rad).toFloat(), 1f + Math.cos(rad).toFloat(), 0f
        )
        val visibility = List(33) { visibilityValue }
        return Pose(landmarks, visibility)
    }

    private fun analyzerFor(type: ExerciseType): FormAnalyzer {
        return FormAnalyzer(type, FormAnalyzer.defaultFor(type))
    }

    /** Plank measures shoulder(12)-hip(24)-ankle(28), unlike arm exercises. */
    private fun plankPose(angle: Double, visibilityValue: Float = 1.0f): Pose {
        val rad = Math.toRadians(180 - angle)
        val landmarks = List(33) { NormalizedLandmark(0f, 0f, 0f) }.toMutableList()
        landmarks[12] = NormalizedLandmark(0f, 0f, 0f)
        landmarks[24] = NormalizedLandmark(0f, 1f, 0f)
        landmarks[28] = NormalizedLandmark(
            Math.sin(rad).toFloat(), 1f + Math.cos(rad).toFloat(), 0f
        )
        return Pose(landmarks, List(33) { visibilityValue })
    }

    /** Squat measures hip(24)-knee(26)-ankle(28), NOT the arm joints. */
    private fun squatPose(angle: Double, visibilityValue: Float = 1.0f): Pose {
        val rad = Math.toRadians(180 - angle)
        val landmarks = List(33) { NormalizedLandmark(0f, 0f, 0f) }.toMutableList()
        landmarks[24] = NormalizedLandmark(0f, 0f, 0f)
        landmarks[26] = NormalizedLandmark(0f, 1f, 0f)
        landmarks[28] = NormalizedLandmark(
            Math.sin(rad).toFloat(), 1f + Math.cos(rad).toFloat(), 0f
        )
        return Pose(landmarks, List(33) { visibilityValue })
    }

    /** Feeds `frames` of the same angle and returns the last AnalysisResult. */
    private fun feed(analyzer: FormAnalyzer, angle: Double, frames: Int): AnalysisResult? {
        var result: AnalysisResult? = null
        repeat(frames) { result = analyzer.analyze(poseAtAngle(angle)) }
        return result
    }

    /** Feeds `frames` of a squat(hip-knee-ankle) pose and returns the last result. */
    private fun feedSquat(analyzer: FormAnalyzer, angle: Double, frames: Int): AnalysisResult? {
        var result: AnalysisResult? = null
        repeat(frames) { result = analyzer.analyze(squatPose(angle)) }
        return result
    }

    // ── Rep-cycle spec-lock (BICEP_CURL: down=150 / up=50) ────────────────

    @Test
    fun bicepCurl_cycle_extendsThenCurls_countsOneRep() {
        val analyzer = analyzerFor(ExerciseType.BICEP_CURL)
        // 5 frames extended (smoothed 160 > 150 -> DOWN)
        feed(analyzer, 160.0, 5)
        // 5 frames curled (smoothed crosses < 50 -> UP on 5th frame)
        val result = feed(analyzer, 30.0, 5)
        assertNotNull(result)
        assertEquals(1, result!!.repCount)
        assertEquals(RepPhase.UP, result.currentPhase)
    }

    @Test
    fun bicepCurl_twoFullCycles_countsTwoReps() {
        val analyzer = analyzerFor(ExerciseType.BICEP_CURL)
        feed(analyzer, 160.0, 5)
        feed(analyzer, 30.0, 5)  // rep 1
        feed(analyzer, 160.0, 5)
        val result = feed(analyzer, 30.0, 5)  // rep 2
        assertNotNull(result)
        assertEquals(2, result!!.repCount)
    }

    @Test
    fun bicepCurl_extendedOnly_neverCountsRep() {
        val analyzer = analyzerFor(ExerciseType.BICEP_CURL)
        val result = feed(analyzer, 170.0, 25)
        assertNotNull(result)
        assertEquals(0, result!!.repCount)
        assertEquals(RepPhase.DOWN, result.currentPhase)
    }

    // ── Rep-cycle spec-lock (SQUAT: down=100 / up=160) ────────────────────

    @Test
    fun squat_standingThenDepth_countsRepAtBottom() {
        val analyzer = analyzerFor(ExerciseType.SQUAT)
        feedSquat(analyzer, 180.0, 5)   // standing, knee ~180 -> DOWN
        val result = feedSquat(analyzer, 80.0, 5)  // bottom -> UP transition
        assertNotNull(result)
        assertEquals(1, result!!.repCount)
    }

    // ── Rep-cycle spec-lock (PUSH_UP: down=90 / up=160) ───────────────────

    @Test
    fun pushUp_topThenBottom_countsRepAtChestDrop() {
        val analyzer = analyzerFor(ExerciseType.PUSH_UP)
        feed(analyzer, 170.0, 5)   // arms straight -> DOWN
        val result = feed(analyzer, 70.0, 5)  // chest down -> UP transition
        assertNotNull(result)
        assertEquals(1, result!!.repCount)
    }

    // ── Confidence degradation ─────────────────────────────────────────────

    @Test
    fun lowConfidenceThreeConsecutiveFrames_resetsRepSession() {
        val analyzer = analyzerFor(ExerciseType.BICEP_CURL)
        feed(analyzer, 160.0, 5)
        feed(analyzer, 30.0, 5)  // rep 1
        // One invisible frame: ignored, session continues
        assertNull(analyzer.analyze(poseAtAngle(30.0, visibilityValue = 0.2f)))
        // Two more invisible frames -> threshold 3 -> reset() called
        assertNull(analyzer.analyze(poseAtAngle(30.0, visibilityValue = 0.2f)))
        assertNull(analyzer.analyze(poseAtAngle(30.0, visibilityValue = 0.2f)))
        // Fresh session: one more full cycle must count exactly ONE rep,
        // proving the pre-reset rep (and phase/history) were cleared.
        feed(analyzer, 160.0, 5)
        val result = feed(analyzer, 30.0, 5)
        assertNotNull(result)
        assertEquals(1, result!!.repCount)
    }

    @Test
    fun lowConfidenceBelowThreshold_doesNotReset() {
        val analyzer = analyzerFor(ExerciseType.BICEP_CURL)
        feed(analyzer, 160.0, 5)
        feed(analyzer, 30.0, 5)  // rep 1
        // Two occluded frames only (below 3) -> no reset
        assertNull(analyzer.analyze(poseAtAngle(30.0, visibilityValue = 0.1f)))
        assertNull(analyzer.analyze(poseAtAngle(30.0, visibilityValue = 0.1f)))
        feed(analyzer, 160.0, 5)  // visibility recovers: DOWN again (history kept)
        val result = feed(analyzer, 30.0, 5)
        assertNotNull(result)
        assertEquals(2, result!!.repCount)  // session continued, not reset
    }

    // ── INVALID-angle degradation ──────────────────────────────────────────

    @Test
    fun invalidAngleTenConsecutive_resetsSession() {
        val analyzer = analyzerFor(ExerciseType.BICEP_CURL)
        feed(analyzer, 160.0, 5)
        feed(analyzer, 30.0, 5)  // rep 1 (lastPhase = UP)
        // Degenerate landmarks -> angle -1 -> INVALID, 10 consecutive -> reset
        val degenerate = Pose(
            List(33) { NormalizedLandmark(0f, 0f, 0f) },
            List(33) { 1.0f }
        )
        repeat(10) { assertNotNull(analyzer.analyze(degenerate)) }
        // Without reset, lastPhase=UP + 5xDOWN then 5xUP would have yielded rep 2.
        feed(analyzer, 160.0, 5)
        val result = feed(analyzer, 30.0, 5)
        assertNotNull(result)
        assertEquals(1, result!!.repCount)
    }

    @Test
    fun invalidAngle_returnsFeedback_andDoesNotAdvancePhase() {
        val analyzer = analyzerFor(ExerciseType.BICEP_CURL)
        feed(analyzer, 160.0, 5)  // DOWN
        val degenerate = Pose(
            List(33) { NormalizedLandmark(0f, 0f, 0f) },
            List(33) { 1.0f }
        )
        val result = analyzer.analyze(degenerate)
        assertNotNull(result)
        assertEquals("Landmarks not detected", result!!.formFeedback)
        assertEquals(-1.0, result.angle, 1e-9)
        assertEquals(RepPhase.DOWN, result.currentPhase)
    }

    // ── First-frame / empty-history safety ─────────────────────────────────

    @Test
    fun firstFrame_noHistory_returnsDownWithoutCounting() {
        val analyzer = analyzerFor(ExerciseType.BICEP_CURL)
        val result = analyzer.analyze(poseAtAngle(160.0))
        assertNotNull(result)
        assertEquals(0, result!!.repCount)
        assertEquals(RepPhase.DOWN, result.currentPhase)
    }

    @Test
    fun firstFrame_atUpAngle_doesNotCountPhantomRep() {
        // A session that starts already-curled lands in UP phase but must not
        // count a rep (the DOWN->UP transition requires a prior DOWN baseline).
        val analyzer = analyzerFor(ExerciseType.BICEP_CURL)
        val result = analyzer.analyze(poseAtAngle(20.0))
        assertNotNull(result)
        assertEquals(0, result!!.repCount)
        assertEquals(RepPhase.UP, result.currentPhase)
    }

    // ── reset() ────────────────────────────────────────────────────────────

    @Test
    fun explicitReset_clearsCountPhaseAndHistory() {
        val analyzer = analyzerFor(ExerciseType.BICEP_CURL)
        feed(analyzer, 160.0, 5)
        feed(analyzer, 30.0, 5)
        analyzer.reset()
        feed(analyzer, 160.0, 5)
        val result = feed(analyzer, 30.0, 5)
        assertNotNull(result)
        assertEquals(1, result!!.repCount)
    }

    // ── Plank time-based state machine (isTimeBased) ───────────────────────

    @Test
    fun plank_holdsForDuration_countsOneRep() {
        val config = ExerciseConfig(
            downThreshold = 170.0,
            upThreshold = 170.0,
            minConfidence = 0.4,
            isTimeBased = true,
            holdDurationMs = 1000
        )
        val analyzer = FormAnalyzer(ExerciseType.PLANK, config)
        val hold = plankPose(175.0)  // in-position plank line
        // Production feeds real epoch-millis timestamps; the 0L sentinel only
        // disambiguates correctly against non-zero currentTimeMs values.
        val base = 100_000L

        val r0 = analyzer.analyze(hold, currentTimeMs = base)!!
        assertEquals(0, r0.repCount)
        assertEquals("Hold for 1s", r0.formFeedback)
        assertEquals(FeedbackTone.NEUTRAL, r0.feedbackTone)

        val r1 = analyzer.analyze(hold, currentTimeMs = base + 600L)!!
        assertEquals(0, r1.repCount)
        assertEquals("Hold for 0s", r1.formFeedback)
        assertEquals(FeedbackTone.NEUTRAL, r1.feedbackTone)

        val r2 = analyzer.analyze(hold, currentTimeMs = base + 1000L)!!
        assertEquals(1, r2.repCount)
        assertEquals("Plank hold complete", r2.formFeedback)
        assertEquals(FeedbackTone.GOOD, r2.feedbackTone)

        // The completed count and cue persist on later frames (no flicker to 0).
        val r3 = analyzer.analyze(hold, currentTimeMs = base + 1500L)!!
        assertEquals(1, r3.repCount)
        assertEquals("Plank hold complete", r3.formFeedback)
        assertEquals(FeedbackTone.GOOD, r3.feedbackTone)
    }

    @Test
    fun plank_breakingPosition_resetsHoldTimer() {
        val config = ExerciseConfig(
            downThreshold = 170.0,
            upThreshold = 170.0,
            minConfidence = 0.4,
            isTimeBased = true,
            holdDurationMs = 1000
        )
        val analyzer = FormAnalyzer(ExerciseType.PLANK, config)
        val hold = plankPose(175.0)
        val sag = plankPose(150.0)  // hips dropped out of position
        val base = 200_000L

        analyzer.analyze(hold, currentTimeMs = base)
        analyzer.analyze(hold, currentTimeMs = base + 600L)
        val broken = analyzer.analyze(sag, currentTimeMs = base + 700L)!!
        assertEquals("Get into plank position", broken.formFeedback)
        assertEquals(FeedbackTone.WARN, broken.feedbackTone)
        assertEquals(0, broken.repCount)

        // Full hold again from new start -> completes a rep.
        analyzer.analyze(hold, currentTimeMs = base + 1000L)
        val completed = analyzer.analyze(hold, currentTimeMs = base + 2000L)!!
        assertEquals(1, completed.repCount)
        assertEquals("Plank hold complete", completed.formFeedback)
        assertEquals(FeedbackTone.GOOD, completed.feedbackTone)
    }

    // ── Feedback strings (hard-coded per exercise) ─────────────────────────

    @Test
    fun bicepCurl_feedback_switchesOnAngle() {
        val analyzer = analyzerFor(ExerciseType.BICEP_CURL)
        feed(analyzer, 160.0, 5)          // extended
        val extended = feed(analyzer, 175.0, 5)!!
        assertTrue(extended.formFeedback.isNotEmpty())
        val curled = feed(analyzer, 20.0, 5)!!
        assertEquals("Full range of motion", curled.formFeedback)
    }

    // ── Feedback tone mapping (single source of truth in FormAnalyzer) ────

    @Test
    fun feedbackTone_goodWarnNeutral_mapsPerExercise() {
        // BICEP_CURL: WARN when over-extended, GOOD at full range / mid-cycle.
        val analyzer = analyzerFor(ExerciseType.BICEP_CURL)
        feed(analyzer, 160.0, 5)
        val over = feed(analyzer, 175.0, 5)!!
        assertEquals("Extend arm more", over.formFeedback)
        assertEquals(FeedbackTone.WARN, over.feedbackTone)
        val fullRange = feed(analyzer, 20.0, 5)!!
        assertEquals("Full range of motion", fullRange.formFeedback)
        assertEquals(FeedbackTone.GOOD, fullRange.feedbackTone)
        val midCycle = feed(analyzer, 90.0, 5)!!
        assertEquals("Good rep", midCycle.formFeedback)
        assertEquals(FeedbackTone.GOOD, midCycle.feedbackTone)

        // SQUAT: GOOD at depth, WARN mid-descent, NEUTRAL at start.
        val squat = analyzerFor(ExerciseType.SQUAT)
        feedSquat(squat, 180.0, 5)
        val depth = feedSquat(squat, 80.0, 5)!!
        assertEquals("Good depth", depth.formFeedback)
        assertEquals(FeedbackTone.GOOD, depth.feedbackTone)
        val mid = feedSquat(squat, 110.0, 5)!!
        assertEquals("Go lower", mid.formFeedback)
        assertEquals(FeedbackTone.WARN, mid.feedbackTone)
        val top = feedSquat(squat, 180.0, 5)!!
        assertEquals("Start squat", top.formFeedback)
        assertEquals(FeedbackTone.NEUTRAL, top.feedbackTone)

        // Invalid joints: neutral system cue (not an error color).
        val degenerate = Pose(
            List(33) { NormalizedLandmark(0f, 0f, 0f) },
            List(33) { 1.0f }
        )
        val invalid = analyzer.analyze(degenerate)!!
        assertEquals("Landmarks not detected", invalid.formFeedback)
        assertEquals(FeedbackTone.NEUTRAL, invalid.feedbackTone)
    }
}