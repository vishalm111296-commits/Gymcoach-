package com.gymcoach.app.core.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class RestAudioCueEvaluatorTest {

    private lateinit var evaluator: RestAudioCueEvaluator

    @Before
    fun setUp() {
        evaluator = RestAudioCueEvaluator()
    }

    @Test
    fun `evaluateCue returns countdown cues at 3, 2, 1 seconds`() {
        assertEquals(AudioCueType.COUNTDOWN_3, evaluator.evaluateCue(remainingSeconds = 3))
        assertEquals(AudioCueType.COUNTDOWN_2, evaluator.evaluateCue(remainingSeconds = 2))
        assertEquals(AudioCueType.COUNTDOWN_1, evaluator.evaluateCue(remainingSeconds = 1))
    }

    @Test
    fun `evaluateCue returns TIMER_FINISHED at 0 seconds or when isCompleted is true`() {
        assertEquals(AudioCueType.TIMER_FINISHED, evaluator.evaluateCue(remainingSeconds = 0))
        assertEquals(AudioCueType.TIMER_FINISHED, evaluator.evaluateCue(remainingSeconds = 0, isCompleted = true))
        assertEquals(AudioCueType.TIMER_FINISHED, evaluator.evaluateCue(remainingSeconds = 10, isCompleted = true))
    }

    @Test
    fun `evaluateCue returns SUPERSET_SWITCH when isSupersetSwitch is true at 0 seconds or completed`() {
        assertEquals(
            AudioCueType.SUPERSET_SWITCH,
            evaluator.evaluateCue(remainingSeconds = 0, isSupersetSwitch = true)
        )
        assertEquals(
            AudioCueType.SUPERSET_SWITCH,
            evaluator.evaluateCue(remainingSeconds = 0, isCompleted = true, isSupersetSwitch = true)
        )
        assertEquals(
            AudioCueType.SUPERSET_SWITCH,
            evaluator.evaluateCue(remainingSeconds = 45, isCompleted = true, isSupersetSwitch = true)
        )
    }

    @Test
    fun `evaluateCue returns WARNING_15S when enabled and remaining is 15 with totalDuration greater than 20`() {
        assertEquals(
            AudioCueType.WARNING_15S,
            evaluator.evaluateCue(remainingSeconds = 15, totalDurationSeconds = 60, enable15sWarning = true)
        )
        assertNull(
            evaluator.evaluateCue(remainingSeconds = 15, totalDurationSeconds = 60, enable15sWarning = false)
        )
    }

    @Test
    fun `evaluateCue returns HALFWAY when enabled and remaining is half of total duration`() {
        assertEquals(
            AudioCueType.HALFWAY,
            evaluator.evaluateCue(remainingSeconds = 30, totalDurationSeconds = 60, enableHalfwayAlert = true)
        )
        assertNull(
            evaluator.evaluateCue(remainingSeconds = 30, totalDurationSeconds = 60, enableHalfwayAlert = false)
        )
    }

    @Test
    fun `evaluateCue returns null during normal ticking`() {
        assertNull(evaluator.evaluateCue(remainingSeconds = 60))
        assertNull(evaluator.evaluateCue(remainingSeconds = 30))
        assertNull(evaluator.evaluateCue(remainingSeconds = 15, totalDurationSeconds = 0, enable15sWarning = false))
        assertNull(evaluator.evaluateCue(remainingSeconds = 5))
        assertNull(evaluator.evaluateCue(remainingSeconds = 4))
    }
}
