package com.gymcoach.app.core.audio

import android.media.AudioManager
import android.media.ToneGenerator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestAudioCoach @Inject constructor(
    private val evaluator: RestAudioCueEvaluator
) {
    private var soundEnabled: Boolean = true
    private var toneGenerator: ToneGenerator? = null

    init {
        initToneGenerator()
    }

    private fun initToneGenerator() {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (_: Throwable) {
            toneGenerator = null
        }
    }

    fun onTick(remainingSeconds: Int) {
        val cue = evaluator.evaluateCue(remainingSeconds = remainingSeconds)
        cue?.let { playCue(it) }
    }

    fun onComplete(isSupersetSwitch: Boolean = false) {
        val cue = evaluator.evaluateCue(
            remainingSeconds = 0,
            isCompleted = true,
            isSupersetSwitch = isSupersetSwitch
        )
        cue?.let { playCue(it) }
    }

    fun playCue(cue: AudioCueType) {
        if (!soundEnabled) return
        try {
            if (toneGenerator == null) {
                initToneGenerator()
            }
            val (toneType, durationMs) = when (cue) {
                AudioCueType.COUNTDOWN_3,
                AudioCueType.COUNTDOWN_2,
                AudioCueType.COUNTDOWN_1 -> ToneGenerator.TONE_PROP_BEEP to 150
                AudioCueType.TIMER_FINISHED -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD to 500
                AudioCueType.SUPERSET_SWITCH -> ToneGenerator.TONE_CDMA_HIGH_L to 300
            }
            toneGenerator?.startTone(toneType, durationMs)
        } catch (_: Throwable) {
            // Guarded against missing audio hardware or unmocked runtime
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }

    fun isSoundEnabled(): Boolean = soundEnabled
}
