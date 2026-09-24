package com.gymcoach.app.core.audio

import javax.inject.Inject
import javax.inject.Singleton

enum class AudioCueType {
    WARNING_15S,
    HALFWAY,
    COUNTDOWN_3,
    COUNTDOWN_2,
    COUNTDOWN_1,
    TIMER_FINISHED,
    SUPERSET_SWITCH
}

@Singleton
class RestAudioCueEvaluator @Inject constructor() {
    /**
     * Evaluates what cue (if any) should be played when the timer ticks.
     * Ensures cues are only triggered once on boundary transitions.
     */
    fun evaluateCue(
        remainingSeconds: Int,
        totalDurationSeconds: Int = 0,
        isCompleted: Boolean = false,
        isSupersetSwitch: Boolean = false,
        enable15sWarning: Boolean = true,
        enableHalfwayAlert: Boolean = false
    ): AudioCueType? {
        if (isCompleted) {
            return if (isSupersetSwitch) AudioCueType.SUPERSET_SWITCH else AudioCueType.TIMER_FINISHED
        }
        if (remainingSeconds == 0) {
            return if (isSupersetSwitch) AudioCueType.SUPERSET_SWITCH else AudioCueType.TIMER_FINISHED
        }
        if (enableHalfwayAlert && totalDurationSeconds >= 30 && remainingSeconds == totalDurationSeconds / 2) {
            return AudioCueType.HALFWAY
        }
        if (enable15sWarning && remainingSeconds == 15 && totalDurationSeconds > 20) {
            return AudioCueType.WARNING_15S
        }
        return when (remainingSeconds) {
            3 -> AudioCueType.COUNTDOWN_3
            2 -> AudioCueType.COUNTDOWN_2
            1 -> AudioCueType.COUNTDOWN_1
            else -> null
        }
    }
}
