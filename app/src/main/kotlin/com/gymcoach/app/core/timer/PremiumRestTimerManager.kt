package com.gymcoach.app.core.timer

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class PremiumRestTimerState(
    val timeRemaining: Int = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val totalDuration: Int = 0
) {
    val progress: Float get() = if (totalDuration > 0) timeRemaining.toFloat() / totalDuration else 0f
    val color: Color get() = when {
        progress > 0.5f -> Color.Green
        progress > 0.2f -> Color.Yellow
        else -> Color.Red
    }
}

@Singleton
class PremiumRestTimerManager @Inject constructor(
    private val vibrator: Vibrator?
) {
    private val _state = MutableStateFlow(PremiumRestTimerState())
    val state: StateFlow<PremiumRestTimerState> = _state.asStateFlow()

    private var tickJob: Job? = null

    fun start(seconds: Int, scope: CoroutineScope) {
        tickJob?.cancel()
        _state.value = PremiumRestTimerState(timeRemaining = seconds, isRunning = true, totalDuration = seconds)
        startTicking(scope)
    }

    private fun startTicking(scope: CoroutineScope) {
        tickJob = scope.launch {
            while (_state.value.timeRemaining > 0) {
                delay(1000L)
                if (!_state.value.isPaused) {
                    val nextTime = _state.value.timeRemaining - 1
                    _state.value = _state.value.copy(timeRemaining = nextTime)
                    if (nextTime == 0) vibrate()
                }
            }
            _state.value = _state.value.copy(isRunning = false, isPaused = false)
        }
    }

    fun pause() { if (_state.value.isRunning) _state.value = _state.value.copy(isPaused = true) }
    fun resume() { if (_state.value.isRunning && _state.value.isPaused) _state.value = _state.value.copy(isPaused = false) }
    fun skip() { stop() }
    fun stop() {
        tickJob?.cancel()
        _state.value = PremiumRestTimerState()
    }
    fun addTime(seconds: Int) {
        val newTime = (_state.value.timeRemaining + seconds).coerceAtLeast(0)
        _state.value = _state.value.copy(timeRemaining = newTime, totalDuration = maxOf(_state.value.totalDuration, newTime))
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(500)
        }
    }
}
