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

    private fun createArmPose(angle: Double, visible: Boolean = true, visibilityScore: Float = 1.0f): Pose {
        val rad = Math.toRadians(180 - angle)
        val landmarks = List(33) { NormalizedLandmark(0f, 0f, 0f) }.toMutableList()
        landmarks[12] = NormalizedLandmark(0f, 0f, 0f)
        landmarks[14] = NormalizedLandmark(0f, 1f, 0f)
        landmarks[16] = NormalizedLandmark(Math.sin(rad).toFloat(), 1f + Math.cos(rad).toFloat(), 0f)
        val visibility = List(33) { if (visible) visibilityScore else 0.0f }
        return Pose(landmarks, visibility)
    }

    private fun createLegPose(angle: Double, visible: Boolean = true, visibilityScore: Float = 1.0f): Pose {
        val rad = Math.toRadians(180 - angle)
        val landmarks = List(33) { NormalizedLandmark(0f, 0f, 0f) }.toMutableList()
        landmarks[24] = NormalizedLandmark(0f, 0f, 0f)
        landmarks[26] = NormalizedLandmark(0f, 1f, 0f)
        landmarks[28] = NormalizedLandmark(Math.sin(rad).toFloat(), 1f + Math.cos(rad).toFloat(), 0f)
        val visibility = List(33) { if (visible) visibilityScore else 0.0f }
        return Pose(landmarks, visibility)
    }

    private fun createPlankPose(angle: Double, visible: Boolean = true, visibilityScore: Float = 1.0f): Pose {
        val rad = Math.toRadians(180 - angle)
        val landmarks = List(33) { NormalizedLandmark(0f, 0f, 0f) }.toMutableList()
        landmarks[12] = NormalizedLandmark(0f, 0f, 0f)
        landmarks[24] = NormalizedLandmark(0f, 1f, 0f)
        landmarks[28] = NormalizedLandmark(Math.sin(rad).toFloat(), 1f + Math.cos(rad).toFloat(), 0f)
        val visibility = List(33) { if (visible) visibilityScore else 0.0f }
        return Pose(landmarks, visibility)
    }

    @Test
    fun `detects complete rep`() {
        for (i in 0..4) analyzer.analyze(createArmPose(160.0))
        for (i in 0..4) analyzer.analyze(createArmPose(80.0))
        var result: AnalysisResult? = null
        for (i in 0..4) {
            result = analyzer.analyze(createArmPose(160.0))
        }
        assertNotNull(result)
        assertEquals(1, result!!.repCount)
        assertEquals(RepPhase.DOWN, result.currentPhase)
    }

    @Test
    fun `ignores low confidence frames and triggers reset after threshold`() {
        for (i in 0..4) analyzer.analyze(createArmPose(160.0))
        for (i in 0..4) analyzer.analyze(createArmPose(80.0))

        // 1st and 2nd low confidence frame return null without resetting state
        assertNull(analyzer.analyze(createArmPose(160.0, visible = false)))
        assertNull(analyzer.analyze(createArmPose(160.0, visible = false)))

        // 3rd low confidence frame triggers automatic reset
        assertNull(analyzer.analyze(createArmPose(160.0, visible = false)))

        // After reset, repCount should start back at 0 when reps are detected
        for (i in 0..4) analyzer.analyze(createArmPose(160.0))
        for (i in 0..4) analyzer.analyze(createArmPose(80.0))
        var result: AnalysisResult? = null
        for (i in 0..4) {
            result = analyzer.analyze(createArmPose(160.0))
        }
        assertNotNull(result)
        assertEquals(1, result!!.repCount)
    }

    @Test
    fun `handles invalid angle out of range and resets after 10 invalid frames`() {
        val customConfig = ExerciseConfig(
            downThreshold = 90.0,
            upThreshold = 150.0,
            validAngleRange = 30.0..170.0
        )
        val customAnalyzer = FormAnalyzer(ExerciseType.BICEP_CURL, customConfig)

        // Pose with angle out of valid range (e.g. 20.0)
        val invalidPose = createArmPose(20.0)
        val result = customAnalyzer.analyze(invalidPose)

        assertNotNull(result)
        assertEquals("Angle out of expected range", result!!.formFeedback)

        // Send invalid frames up to maxConsecutiveInvalidBeforeReset (10)
        for (i in 2..10) {
            customAnalyzer.analyze(invalidPose)
        }

        // Send a valid frame now and ensure analyzer was reset
        for (i in 0..4) customAnalyzer.analyze(createArmPose(160.0))
        for (i in 0..4) customAnalyzer.analyze(createArmPose(80.0))
        var validResult: AnalysisResult? = null
        for (i in 0..4) {
            validResult = customAnalyzer.analyze(createArmPose(160.0))
        }
        assertNotNull(validResult)
        assertEquals(1, validResult!!.repCount)
    }

    @Test
    fun `handles zero magnitude points returning negative angle`() {
        val zeroLandmarks = List(33) { NormalizedLandmark(0f, 0f, 0f) }
        val pose = Pose(zeroLandmarks, List(33) { 1.0f })

        val result = analyzer.analyze(pose)
        assertNotNull(result)
        assertEquals("Landmarks not detected", result!!.formFeedback)
        assertEquals(-1.0, result.angle, 0.001)
    }

    @Test
    fun `plank time based exercise tracks hold duration and completes`() {
        val plankConfig = FormAnalyzer.defaultFor(ExerciseType.PLANK).copy(holdDurationMs = 5000)
        val plankAnalyzer = FormAnalyzer(ExerciseType.PLANK, plankConfig)

        val inPosPose = createPlankPose(180.0)
        val outOfPosPose = createPlankPose(150.0)

        // Out of position initially
        val r0 = plankAnalyzer.analyze(outOfPosPose, currentTimeMs = 1000L)
        assertNotNull(r0)
        assertEquals("Get into plank position", r0!!.formFeedback)
        assertEquals(0, r0.repCount)

        // Move into position at t = 2000ms
        val r1 = plankAnalyzer.analyze(inPosPose, currentTimeMs = 2000L)
        assertNotNull(r1)
        assertEquals("Hold for 5s", r1!!.formFeedback)
        assertEquals(0, r1.repCount)

        // Elapsed 3 seconds (t = 5000ms)
        val r2 = plankAnalyzer.analyze(inPosPose, currentTimeMs = 5000L)
        assertNotNull(r2)
        assertEquals("Hold for 2s", r2!!.formFeedback)
        assertEquals(0, r2.repCount)

        // Elapsed 5 seconds total (t = 7000ms) - hold complete!
        val r3 = plankAnalyzer.analyze(inPosPose, currentTimeMs = 7000L)
        assertNotNull(r3)
        assertEquals("Plank hold complete", r3!!.formFeedback)
        assertEquals(1, r3.repCount)
    }

    @Test
    fun `provides exercise feedback for all exercise types`() {
        val bicepAnalyzer = FormAnalyzer(ExerciseType.BICEP_CURL, FormAnalyzer.defaultFor(ExerciseType.BICEP_CURL))
        assertEquals("Extend arm more", bicepAnalyzer.analyze(createArmPose(175.0))?.formFeedback)
        assertEquals("Full range of motion", bicepAnalyzer.analyze(createArmPose(25.0))?.formFeedback)
        assertEquals("Good rep", bicepAnalyzer.analyze(createArmPose(100.0))?.formFeedback)

        val squatAnalyzer = FormAnalyzer(ExerciseType.SQUAT, FormAnalyzer.defaultFor(ExerciseType.SQUAT))
        assertEquals("Good depth", squatAnalyzer.analyze(createLegPose(85.0))?.formFeedback)
        assertEquals("Go lower", squatAnalyzer.analyze(createLegPose(110.0))?.formFeedback)
        assertEquals("Start squat", squatAnalyzer.analyze(createLegPose(150.0))?.formFeedback)

        val pushUpAnalyzer = FormAnalyzer(ExerciseType.PUSH_UP, FormAnalyzer.defaultFor(ExerciseType.PUSH_UP))
        assertEquals("Good depth", pushUpAnalyzer.analyze(createArmPose(85.0))?.formFeedback)
        assertEquals("Lower your chest", pushUpAnalyzer.analyze(createArmPose(120.0))?.formFeedback)
        assertEquals("Keep body straight", pushUpAnalyzer.analyze(createArmPose(160.0))?.formFeedback)

        val shoulderPressAnalyzer = FormAnalyzer(ExerciseType.SHOULDER_PRESS, FormAnalyzer.defaultFor(ExerciseType.SHOULDER_PRESS))
        assertEquals("Press weight up", shoulderPressAnalyzer.analyze(createArmPose(85.0))?.formFeedback)
        assertEquals("Almost there", shoulderPressAnalyzer.analyze(createArmPose(120.0))?.formFeedback)
        assertEquals("Start press", shoulderPressAnalyzer.analyze(createArmPose(160.0))?.formFeedback)

        val lateralRaiseAnalyzer = FormAnalyzer(ExerciseType.LATERAL_RAISE, FormAnalyzer.defaultFor(ExerciseType.LATERAL_RAISE))
        assertEquals("Raise arms to shoulder height", lateralRaiseAnalyzer.analyze(createArmPose(85.0))?.formFeedback)
        assertEquals("Lower with control", lateralRaiseAnalyzer.analyze(createArmPose(120.0))?.formFeedback)
        assertEquals("Start lateral raise", lateralRaiseAnalyzer.analyze(createArmPose(160.0))?.formFeedback)

        val bentOverRowAnalyzer = FormAnalyzer(ExerciseType.BENT_OVER_ROW, FormAnalyzer.defaultFor(ExerciseType.BENT_OVER_ROW))
        assertEquals("Pull to your torso", bentOverRowAnalyzer.analyze(createArmPose(85.0))?.formFeedback)
        assertEquals("Extend arms forward", bentOverRowAnalyzer.analyze(createArmPose(120.0))?.formFeedback)
        assertEquals("Start bent-over row", bentOverRowAnalyzer.analyze(createArmPose(160.0))?.formFeedback)

        // Non-time-based Plank configuration to test angle feedback string generation
        val plankNonTimeConfig = FormAnalyzer.defaultFor(ExerciseType.PLANK).copy(isTimeBased = false)
        val plankAnalyzer = FormAnalyzer(ExerciseType.PLANK, plankNonTimeConfig)
        assertEquals("Good plank position", plankAnalyzer.analyze(createPlankPose(178.0))?.formFeedback)
        assertEquals("Squeeze core, keep straight", plankAnalyzer.analyze(createPlankPose(165.0))?.formFeedback)
        assertEquals("Lower hips or raise up", plankAnalyzer.analyze(createPlankPose(150.0))?.formFeedback)

        val deadliftAnalyzer = FormAnalyzer(ExerciseType.DEADLIFT, FormAnalyzer.defaultFor(ExerciseType.DEADLIFT))
        assertEquals("Good hinge position", deadliftAnalyzer.analyze(createLegPose(85.0))?.formFeedback)
        assertEquals("Push hips back", deadliftAnalyzer.analyze(createLegPose(110.0))?.formFeedback)
        assertEquals("Start deadlift", deadliftAnalyzer.analyze(createLegPose(150.0))?.formFeedback)

        val benchPressAnalyzer = FormAnalyzer(ExerciseType.BENCH_PRESS, FormAnalyzer.defaultFor(ExerciseType.BENCH_PRESS))
        assertEquals("Lower the bar", benchPressAnalyzer.analyze(createArmPose(85.0))?.formFeedback)
        assertEquals("Press up", benchPressAnalyzer.analyze(createArmPose(120.0))?.formFeedback)
        assertEquals("Start bench press", benchPressAnalyzer.analyze(createArmPose(160.0))?.formFeedback)
    }

    @Test
    fun `defaultFor returns valid configs for all exercise types`() {
        ExerciseType.values().forEach { type ->
            val config = FormAnalyzer.defaultFor(type)
            assertNotNull(config)
            if (type == ExerciseType.PLANK) {
                assertEquals(true, config.isTimeBased)
                assertEquals(30000L, config.holdDurationMs)
            } else {
                assertEquals(false, config.isTimeBased)
            }
        }
    }

    @Test
    fun `manual reset clears analyzer state`() {
        for (i in 0..4) analyzer.analyze(createArmPose(160.0))
        for (i in 0..4) analyzer.analyze(createArmPose(80.0))
        for (i in 0..4) analyzer.analyze(createArmPose(160.0))

        analyzer.reset()

        // After manual reset, analyzing pose starting fresh should have repCount 0
        val result = analyzer.analyze(createArmPose(160.0))
        assertNotNull(result)
        assertEquals(0, result!!.repCount)
    }

    @Test
    fun `close can be called safely`() {
        analyzer.close()
    }
}
