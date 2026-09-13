package com.gymcoach.app.core.timer

import android.content.Context
import com.gymcoach.app.core.notification.RestTimerNotificationService
import com.gymcoach.app.core.notification.RestTimerPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class RestTimerState(
    val timeRemaining: Int = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val totalDuration: Int = 0
)

/** Common rest duration presets (seconds). */
object RestPresets {
    val SHORT = 30       // Isolation exercises
    val MEDIUM = 60      // Moderate compounds
    val STANDARD = 90    // Heavy compounds
    val LONG = 120       // Squats, deadlifts
    val VERY_LONG = 180  // Max effort

    /** Returns the recommended rest time for a given exercise set type and RPE. */
    fun recommended(setType: com.gymcoach.app.domain.model.SetType, rpe: Double): Int {
        return when (setType) {
            com.gymcoach.app.domain.model.SetType.WARMUP -> 60
            com.gymcoach.app.domain.model.SetType.DROP -> 30
            com.gymcoach.app.domain.model.SetType.FAILURE -> 120
            com.gymcoach.app.domain.model.SetType.NORMAL -> {
                when {
                    rpe >= 9.0 -> 180     // Near failure → long rest
                    rpe >= 7.5 -> 120     // Heavy → long rest
                    rpe >= 6.0 -> 90      // Moderate → standard rest
                    else -> 60            // Light → short rest
                }
            }
        }
    }
}

@Singleton
class RestTimerManager @Inject constructor(
    @ApplicationContext private val context: Context? = null
) {

    private val _state = MutableStateFlow(RestTimerState())
    val state: StateFlow<RestTimerState> = _state.asStateFlow()

    private var tickJob: Job? = null

    init {
        restoreFromPreferences()
        if (context != null) {
            CoroutineScope(Dispatchers.Main.immediate).launch {
                combine(
                    RestTimerNotificationService.remainingSeconds,
                    RestTimerNotificationService.isPaused,
                    RestTimerNotificationService.isRunning,
                    RestTimerNotificationService.totalDurationSeconds
                ) { rem, paused, running, total ->
                    RestTimerState(
                        timeRemaining = rem,
                        isPaused = paused,
                        isRunning = running,
                        totalDuration = total
                    )
                }.collect { serviceState ->
                    if (serviceState.isRunning || _state.value.isRunning) {
                        _state.value = serviceState
                    }
                }
            }
        }
    }

    fun restoreFromPreferences() {
        if (context != null) {
            val durable = RestTimerPreferences.load(context)
            if (durable.isRunning) {
                val rem = durable.calculateRemainingSeconds()
                if (rem > 0) {
                    _state.value = RestTimerState(
                        timeRemaining = rem,
                        isRunning = true,
                        isPaused = durable.isPaused,
                        totalDuration = durable.totalDurationSeconds
                    )
                } else {
                    RestTimerPreferences.clear(context)
                }
            }
        }
    }

    fun start(
        seconds: Int,
        scope: CoroutineScope? = null,
        nextSet: String = "",
        workoutId: Long = -1L
    ) {
        if (context != null) {
            RestTimerNotificationService.start(context, seconds, nextSet, workoutId)
            _state.value = RestTimerState(
                timeRemaining = seconds,
                isRunning = true,
                isPaused = false,
                totalDuration = seconds
            )
        } else {
            // JVM Unit test fallback without Android Context
            tickJob?.cancel()
            _state.value = RestTimerState(timeRemaining = seconds, isRunning = true, totalDuration = seconds)
            if (scope != null) {
                startTicking(scope)
            }
        }
    }

    /** Restart the timer with a new duration (e.g., when user taps a preset). */
    fun restart(seconds: Int, scope: CoroutineScope? = null) {
        start(seconds, scope)
    }

    private fun startTicking(scope: CoroutineScope) {
        tickJob = scope.launch {
            while (_state.value.timeRemaining > 0) {
                delay(1000L)
                if (!_state.value.isPaused) {
                    _state.value = _state.value.copy(
                        timeRemaining = _state.value.timeRemaining - 1
                    )
                }
            }
            _state.value = _state.value.copy(isRunning = false, isPaused = false)
        }
    }

    fun pause() {
        if (context != null) {
            RestTimerNotificationService.pause(context)
        }
        if (_state.value.isRunning) {
            _state.value = _state.value.copy(isPaused = true)
        }
    }

    fun resume() {
        if (context != null) {
            RestTimerNotificationService.resume(context)
        }
        if (_state.value.isRunning && _state.value.isPaused) {
            _state.value = _state.value.copy(isPaused = false)
        }
    }

    fun adjust(deltaSeconds: Int) {
        if (context != null) {
            RestTimerNotificationService.adjust(context, deltaSeconds)
        } else {
            val newRem = (_state.value.timeRemaining + deltaSeconds).coerceAtLeast(0)
            if (newRem == 0) {
                stop()
            } else {
                _state.value = _state.value.copy(
                    timeRemaining = newRem,
                    totalDuration = maxOf(_state.value.totalDuration, newRem)
                )
            }
        }
    }

    fun skip() {
        stop()
    }

    fun stop() {
        if (context != null) {
            RestTimerNotificationService.cancel(context)
        }
        tickJob?.cancel()
        tickJob = null
        _state.value = RestTimerState()
    }
}
