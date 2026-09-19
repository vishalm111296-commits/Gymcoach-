package com.gymcoach.app.core.audio

import javax.inject.Inject
import javax.inject.Singleton

enum class AudioCueType {
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
        isCompleted: Boolean = false,
        isSupersetSwitch: Boolean = false
    ): AudioCueType? {
        if (isCompleted) {
            return if (isSupersetSwitch) AudioCueType.SUPERSET_SWITCH else AudioCueType.TIMER_FINISHED
        }
        return when (remainingSeconds) {
            3 -> AudioCueType.COUNTDOWN_3
            2 -> AudioCueType.COUNTDOWN_2
            1 -> AudioCueType.COUNTDOWN_1
            0 -> if (isSupersetSwitch) AudioCueType.SUPERSET_SWITCH else AudioCueType.TIMER_FINISHED
            else -> null
        }
    }
}
