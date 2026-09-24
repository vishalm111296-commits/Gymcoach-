package com.gymcoach.app.core.audio

import android.media.AudioManager
import android.media.ToneGenerator
import javax.inject.Inject
import javax.inject.Singleton

enum class AudioCoachPreset(val displayName: String) {
    CLASSIC_BEEPS("Classic Beeps"),
    MELLOW_CHIMES("Mellow Chimes"),
    POWER_PULSE("Power Pulse")
}

@Singleton
class RestAudioCoach @Inject constructor(
    private val evaluator: RestAudioCueEvaluator
) {
    private var soundEnabled: Boolean = true
    private var preset: AudioCoachPreset = AudioCoachPreset.CLASSIC_BEEPS
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

    fun onTick(
        remainingSeconds: Int,
        totalDurationSeconds: Int = 0,
        enable15sWarning: Boolean = true,
        enableHalfwayAlert: Boolean = false
    ) {
        val cue = evaluator.evaluateCue(
            remainingSeconds = remainingSeconds,
            totalDurationSeconds = totalDurationSeconds,
            enable15sWarning = enable15sWarning,
            enableHalfwayAlert = enableHalfwayAlert
        )
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
            val (toneType, durationMs) = when (preset) {
                AudioCoachPreset.CLASSIC_BEEPS -> when (cue) {
                    AudioCueType.WARNING_15S -> ToneGenerator.TONE_PROP_PROMPT to 200
                    AudioCueType.HALFWAY -> ToneGenerator.TONE_PROP_BEEP to 100
                    AudioCueType.COUNTDOWN_3,
                    AudioCueType.COUNTDOWN_2,
                    AudioCueType.COUNTDOWN_1 -> ToneGenerator.TONE_PROP_BEEP to 150
                    AudioCueType.TIMER_FINISHED -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD to 500
                    AudioCueType.SUPERSET_SWITCH -> ToneGenerator.TONE_CDMA_HIGH_L to 300
                }
                AudioCoachPreset.MELLOW_CHIMES -> when (cue) {
                    AudioCueType.WARNING_15S -> ToneGenerator.TONE_PROP_ACK to 250
                    AudioCueType.HALFWAY -> ToneGenerator.TONE_PROP_ACK to 120
                    AudioCueType.COUNTDOWN_3,
                    AudioCueType.COUNTDOWN_2,
                    AudioCueType.COUNTDOWN_1 -> ToneGenerator.TONE_PROP_ACK to 120
                    AudioCueType.TIMER_FINISHED -> ToneGenerator.TONE_PROP_BEEP2 to 400
                    AudioCueType.SUPERSET_SWITCH -> ToneGenerator.TONE_PROP_BEEP2 to 350
                }
                AudioCoachPreset.POWER_PULSE -> when (cue) {
                    AudioCueType.WARNING_15S -> ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE to 220
                    AudioCueType.HALFWAY -> ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE to 150
                    AudioCueType.COUNTDOWN_3,
                    AudioCueType.COUNTDOWN_2,
                    AudioCueType.COUNTDOWN_1 -> ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE to 180
                    AudioCueType.TIMER_FINISHED -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD to 600
                    AudioCueType.SUPERSET_SWITCH -> ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE to 400
                }
            }
            toneGenerator?.startTone(toneType, durationMs)
        } catch (_: Throwable) {
            // Guarded against missing audio hardware or unmocked runtime
        }
    }

    fun getVibrationPattern(cue: AudioCueType): LongArray {
        return when (cue) {
            AudioCueType.WARNING_15S -> longArrayOf(0, 100, 80, 100)
            AudioCueType.HALFWAY -> longArrayOf(0, 80)
            AudioCueType.COUNTDOWN_3,
            AudioCueType.COUNTDOWN_2,
            AudioCueType.COUNTDOWN_1 -> longArrayOf(0, 60)
            AudioCueType.TIMER_FINISHED -> longArrayOf(0, 200, 100, 300)
            AudioCueType.SUPERSET_SWITCH -> longArrayOf(0, 120, 80, 120, 80, 240)
        }
    }

    fun setPreset(newPreset: AudioCoachPreset) {
        preset = newPreset
    }

    fun getPreset(): AudioCoachPreset = preset

    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }

    fun isSoundEnabled(): Boolean = soundEnabled
}
