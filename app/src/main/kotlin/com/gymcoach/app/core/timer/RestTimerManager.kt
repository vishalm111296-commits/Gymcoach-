package com.gymcoach.app.core.timer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class RestTimerState(
    val timeRemaining: Int = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val totalDuration: Int = 0
)

@Singleton
class RestTimerManager @Inject constructor() {

    private val _state = MutableStateFlow(RestTimerState())
    val state: StateFlow<RestTimerState> = _state.asStateFlow()

    private var tickJob: Job? = null
    fun start(seconds: Int, scope: CoroutineScope) {
        tickJob?.cancel()
        _state.value = RestTimerState(timeRemaining = seconds, isRunning = true, totalDuration = seconds)

        startTicking(scope)
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
        if (_state.value.isRunning) {
            _state.value = _state.value.copy(isPaused = true)
        }
    }

    fun resume() {
        if (_state.value.isRunning && _state.value.isPaused) {
            _state.value = _state.value.copy(isPaused = false)
        }
    }

    fun addTime(seconds: Int) {
        _state.value = _state.value.copy(
            timeRemaining = (_state.value.timeRemaining + seconds).coerceAtLeast(0),
            totalDuration = (_state.value.totalDuration + seconds).coerceAtLeast(0)
        )
    }

    fun stop() {
        tickJob?.cancel()
        tickJob = null
        _state.value = _state.value.copy(timeRemaining = 0, isRunning = false, isPaused = false, totalDuration = 0)
    }
}