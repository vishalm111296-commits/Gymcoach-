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
}
