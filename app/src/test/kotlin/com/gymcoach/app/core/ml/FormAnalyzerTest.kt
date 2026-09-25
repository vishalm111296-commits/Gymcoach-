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

    private fun createLateralRaisePose(angle: Double, visible: Boolean = true): Pose {
        val rad = Math.toRadians(angle)
        val landmarks = List(33) { NormalizedLandmark(0f, 0f, 0f) }.toMutableList()
        landmarks[24] = NormalizedLandmark(0f, 1f, 0f)
        landmarks[12] = NormalizedLandmark(0f, 0f, 0f)
        landmarks[14] = NormalizedLandmark(Math.sin(rad).toFloat(), Math.cos(rad).toFloat(), 0f)
        val visibility = List(33) { if (visible) 1.0f else 0.0f }
        return Pose(landmarks, visibility)
    }

    @Test
    fun `detects complete rep for lateral raise`() {
        val latAnalyzer = FormAnalyzer(ExerciseType.LATERAL_RAISE, FormAnalyzer.defaultFor(ExerciseType.LATERAL_RAISE))
        for (i in 0..4) latAnalyzer.analyze(createLateralRaisePose(20.0))
        for (i in 0..4) latAnalyzer.analyze(createLateralRaisePose(90.0))
        var result: AnalysisResult? = null
        for (i in 0..4) {
            result = latAnalyzer.analyze(createLateralRaisePose(20.0))
        }
        assertNotNull(result)
        assertEquals(1, result!!.repCount)
    }

    @Test
    fun `fromExerciseName matches expected recognized exercises`() {
        assertEquals(ExerciseType.SQUAT, ExerciseType.fromExerciseName("Barbell Squat"))
        assertEquals(ExerciseType.BICEP_CURL, ExerciseType.fromExerciseName("Dumbbell Bicep Curl"))
        assertEquals(ExerciseType.PUSH_UP, ExerciseType.fromExerciseName("Push-up"))
        assertEquals(ExerciseType.PLANK, ExerciseType.fromExerciseName("Standard Plank"))
        assertEquals(ExerciseType.DEADLIFT, ExerciseType.fromExerciseName("Romanian Deadlift"))
        assertEquals(ExerciseType.BENCH_PRESS, ExerciseType.fromExerciseName("Incline Bench Press"))
        assertEquals(ExerciseType.SHOULDER_PRESS, ExerciseType.fromExerciseName("Overhead Press"))
        assertEquals(ExerciseType.LATERAL_RAISE, ExerciseType.fromExerciseName("Cable Lateral Raise"))
        assertEquals(ExerciseType.BENT_OVER_ROW, ExerciseType.fromExerciseName("Bent-over Row"))
    }

    @Test
    fun `fromExerciseName returns null for unrecognized exercises`() {
        assertNull(ExerciseType.fromExerciseName("Tricep Extension"))
        assertNull(ExerciseType.fromExerciseName("Leg Press"))
        assertNull(ExerciseType.fromExerciseName("Calf Raise"))
        assertNull(ExerciseType.fromExerciseName("Unknown Exercise"))
    }

    @Test
    fun `resetTransientTracking preserves rep count but clears tracking state`() {
        // Perform 1 full rep
        for (i in 0..4) analyzer.analyze(createPose(160.0))
        for (i in 0..4) analyzer.analyze(createPose(80.0))
        var result: AnalysisResult? = null
        for (i in 0..4) {
            result = analyzer.analyze(createPose(160.0))
        }
        assertNotNull(result)
        assertEquals(1, result!!.repCount)
        assertEquals(1, analyzer.repCount)

        // Reset transient tracking (simulating temporary low confidence or occlusion)
        analyzer.resetTransientTracking()

        // Rep count must be preserved
        assertEquals(1, analyzer.repCount)
    }

    @Test
    fun `full reset clears rep count and state`() {
        for (i in 0..4) analyzer.analyze(createPose(160.0))
        for (i in 0..4) analyzer.analyze(createPose(80.0))
        for (i in 0..4) analyzer.analyze(createPose(160.0))
        assertEquals(1, analyzer.repCount)

        // Full reset
        analyzer.reset()

        assertEquals(0, analyzer.repCount)
    }

    private fun createLegPose(angle: Double, visible: Boolean = true): Pose {
        val rad = Math.toRadians(180 - angle)
        val landmarks = List(33) { NormalizedLandmark(0f, 0f, 0f) }.toMutableList()
        landmarks[24] = NormalizedLandmark(0f, 0f, 0f)
        landmarks[26] = NormalizedLandmark(0f, 1f, 0f)
        landmarks[28] = NormalizedLandmark(Math.sin(rad).toFloat(), 1f + Math.cos(rad).toFloat(), 0f)
        val visibility = List(33) { if (visible) 1.0f else 0.0f }
        return Pose(landmarks, visibility)
    }

    private fun createPlankPose(angle: Double, visible: Boolean = true): Pose {
        val rad = Math.toRadians(180 - angle)
        val landmarks = List(33) { NormalizedLandmark(0f, 0f, 0f) }.toMutableList()
        landmarks[12] = NormalizedLandmark(0f, 0f, 0f)
        landmarks[24] = NormalizedLandmark(0f, 1f, 0f)
        landmarks[28] = NormalizedLandmark(Math.sin(rad).toFloat(), 1f + Math.cos(rad).toFloat(), 0f)
        val visibility = List(33) { if (visible) 1.0f else 0.0f }
        return Pose(landmarks, visibility)
    }

    @Test
    fun `detects complete rep for squat`() {
        val squatAnalyzer = FormAnalyzer(ExerciseType.SQUAT, FormAnalyzer.defaultFor(ExerciseType.SQUAT))
        for (i in 0..4) squatAnalyzer.analyze(createLegPose(170.0))
        for (i in 0..4) squatAnalyzer.analyze(createLegPose(90.0))
        var result: AnalysisResult? = null
        for (i in 0..4) {
            result = squatAnalyzer.analyze(createLegPose(170.0))
        }
        assertNotNull(result)
        assertEquals(1, result!!.repCount)
    }

    @Test
    fun `detects complete rep for push up`() {
        val pushUpAnalyzer = FormAnalyzer(ExerciseType.PUSH_UP, FormAnalyzer.defaultFor(ExerciseType.PUSH_UP))
        for (i in 0..4) pushUpAnalyzer.analyze(createPose(170.0))
        for (i in 0..4) pushUpAnalyzer.analyze(createPose(80.0))
        var result: AnalysisResult? = null
        for (i in 0..4) {
            result = pushUpAnalyzer.analyze(createPose(170.0))
        }
        assertNotNull(result)
        assertEquals(1, result!!.repCount)
    }

    @Test
    fun `detects complete rep for bench press`() {
        val benchAnalyzer = FormAnalyzer(ExerciseType.BENCH_PRESS, FormAnalyzer.defaultFor(ExerciseType.BENCH_PRESS))
        for (i in 0..4) benchAnalyzer.analyze(createPose(170.0))
        for (i in 0..4) benchAnalyzer.analyze(createPose(75.0))
        var result: AnalysisResult? = null
        for (i in 0..4) {
            result = benchAnalyzer.analyze(createPose(170.0))
        }
        assertNotNull(result)
        assertEquals(1, result!!.repCount)
    }

    @Test
    fun `detects complete rep for shoulder press`() {
        val spAnalyzer = FormAnalyzer(ExerciseType.SHOULDER_PRESS, FormAnalyzer.defaultFor(ExerciseType.SHOULDER_PRESS))
        for (i in 0..4) spAnalyzer.analyze(createPose(70.0))
        for (i in 0..4) spAnalyzer.analyze(createPose(175.0))
        var result: AnalysisResult? = null
        for (i in 0..4) {
            result = spAnalyzer.analyze(createPose(70.0))
        }
        assertNotNull(result)
        assertEquals(1, result!!.repCount)
    }

    @Test
    fun `plank tracking detects hold and increments rep on completion`() {
        val config = ExerciseConfig(
            downThreshold = 170.0,
            upThreshold = 170.0,
            minConfidence = 0.5,
            isTimeBased = true,
            holdDurationMs = 5000L
        )
        val plankAnalyzer = FormAnalyzer(ExerciseType.PLANK, config)

        // Enter plank position at t=1000
        val r1 = plankAnalyzer.analyze(createPlankPose(175.0), currentTimeMs = 1000L)
        assertNotNull(r1)
        assertEquals(0, r1!!.repCount)
        assertEquals("Hold for 5s", r1.formFeedback)

        // Halfway through hold at t=3500
        val r2 = plankAnalyzer.analyze(createPlankPose(175.0), currentTimeMs = 3500L)
        assertEquals(0, r2!!.repCount)
        assertEquals("Hold for 2s", r2.formFeedback)

        // Hold complete at t=6100
        val r3 = plankAnalyzer.analyze(createPlankPose(175.0), currentTimeMs = 6100L)
        assertEquals(1, r3!!.repCount)
        assertEquals("Plank hold complete", r3.formFeedback)
    }

    @Test
    fun `plank tracking resets timer when breaking position`() {
        val config = ExerciseConfig(
            downThreshold = 170.0,
            upThreshold = 170.0,
            minConfidence = 0.5,
            isTimeBased = true,
            holdDurationMs = 5000L
        )
        val plankAnalyzer = FormAnalyzer(ExerciseType.PLANK, config)

        // Enter plank position at t=1000
        plankAnalyzer.analyze(createPlankPose(175.0), currentTimeMs = 1000L)

        // Drop out of position (hips sagging: angle 140)
        val rBreak = plankAnalyzer.analyze(createPlankPose(140.0), currentTimeMs = 3000L)
        assertEquals("Get into plank position", rBreak!!.formFeedback)
        assertEquals(0, rBreak.repCount)

        // Re-enter position: timer starts fresh from t=4000
        val rRestart = plankAnalyzer.analyze(createPlankPose(175.0), currentTimeMs = 4000L)
        assertEquals("Hold for 5s", rRestart!!.formFeedback)
    }

    @Test
    fun `coincident landmarks report invalid movement without throwing`() {
        val landmarks = List(33) { NormalizedLandmark(0f, 0f, 0f) }
        val pose = Pose(landmarks, List(33) { 1.0f })

        val result = analyzer.analyze(pose)
        assertNotNull(result)
        assertEquals("Landmarks not detected", result!!.formFeedback)
        assertEquals(0, result.repCount)
    }

    @Test
    fun `consecutive low confidence frames reset transient tracking`() {
        // Build up initial angle and state
        for (i in 0..4) analyzer.analyze(createPose(160.0))

        // Feed 3 consecutive low-confidence frames
        analyzer.analyze(createPose(160.0, visible = false))
        analyzer.analyze(createPose(160.0, visible = false))
        analyzer.analyze(createPose(160.0, visible = false))

        // Valid frame afterwards starts cleanly without residual invalid state
        val result = analyzer.analyze(createPose(160.0))
        assertNotNull(result)
        assertEquals(0, result!!.repCount)
    }

    @Test
    fun `detects complete rep for bent over row`() {
        val rowAnalyzer = FormAnalyzer(ExerciseType.BENT_OVER_ROW, FormAnalyzer.defaultFor(ExerciseType.BENT_OVER_ROW))
        // Down threshold = 90, Up threshold = 160
        for (i in 0..4) rowAnalyzer.analyze(createPose(170.0))
        for (i in 0..4) rowAnalyzer.analyze(createPose(80.0))
        var result: AnalysisResult? = null
        for (i in 0..4) {
            result = rowAnalyzer.analyze(createPose(170.0))
        }
        assertNotNull(result)
        assertEquals(1, result!!.repCount)
    }

    @Test
    fun `detects complete rep for deadlift`() {
        val dlAnalyzer = FormAnalyzer(ExerciseType.DEADLIFT, FormAnalyzer.defaultFor(ExerciseType.DEADLIFT))
        // Down threshold = 100, Up threshold = 160
        for (i in 0..4) dlAnalyzer.analyze(createLegPose(170.0))
        for (i in 0..4) dlAnalyzer.analyze(createLegPose(90.0))
        var result: AnalysisResult? = null
        for (i in 0..4) {
            result = dlAnalyzer.analyze(createLegPose(170.0))
        }
        assertNotNull(result)
        assertEquals(1, result!!.repCount)
    }

    @Test
    fun `ten consecutive invalid movement frames resets transient tracking`() {
        // Build up initial angle and state
        for (i in 0..4) analyzer.analyze(createPose(160.0))

        // Feed 10 consecutive invalid frames (coincident landmarks returning -1.0)
        val invalidPose = Pose(List(33) { NormalizedLandmark(0f, 0f, 0f) }, List(33) { 1.0f })
        for (i in 1..10) {
            val invalidResult = analyzer.analyze(invalidPose)
            assertNotNull(invalidResult)
            assertEquals("Landmarks not detected", invalidResult!!.formFeedback)
        }

        // 11th frame with valid pose starts from fresh tracking state
        val validResult = analyzer.analyze(createPose(160.0))
        assertNotNull(validResult)
        assertEquals(0, validResult!!.repCount)
    }

    @Test
    fun `feedback messages reflect exact form states across exercises`() {
        val curlAnalyzer = FormAnalyzer(ExerciseType.BICEP_CURL, FormAnalyzer.defaultFor(ExerciseType.BICEP_CURL))
        val rCurl1 = curlAnalyzer.analyze(createPose(175.0))
        assertEquals("Extend arm more", rCurl1!!.formFeedback)
        val rCurl2 = curlAnalyzer.analyze(createPose(25.0))
        assertEquals("Full range of motion", rCurl2!!.formFeedback)
        val rCurl3 = curlAnalyzer.analyze(createPose(100.0))
        assertEquals("Good rep", rCurl3!!.formFeedback)

        val squatAnalyzer = FormAnalyzer(ExerciseType.SQUAT, FormAnalyzer.defaultFor(ExerciseType.SQUAT))
        val rSquat1 = squatAnalyzer.analyze(createLegPose(85.0))
        assertEquals("Good depth", rSquat1!!.formFeedback)
        val rSquat2 = squatAnalyzer.analyze(createLegPose(110.0))
        assertEquals("Go lower", rSquat2!!.formFeedback)
        val rSquat3 = squatAnalyzer.analyze(createLegPose(150.0))
        assertEquals("Start squat", rSquat3!!.formFeedback)

        val benchAnalyzer = FormAnalyzer(ExerciseType.BENCH_PRESS, FormAnalyzer.defaultFor(ExerciseType.BENCH_PRESS))
        val rBench1 = benchAnalyzer.analyze(createPose(85.0))
        assertEquals("Lower the bar", rBench1!!.formFeedback)
        val rBench2 = benchAnalyzer.analyze(createPose(130.0))
        assertEquals("Press up", rBench2!!.formFeedback)
        val rBench3 = benchAnalyzer.analyze(createPose(165.0))
        assertEquals("Start bench press", rBench3!!.formFeedback)
    }
}

