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
    val isRunning: Boolean = false
)

@Singleton
class RestTimerManager @Inject constructor() {

    private val _state = MutableStateFlow(RestTimerState())
    val state: StateFlow<RestTimerState> = _state.asStateFlow()

    private var tickJob: Job? = null

    fun start(seconds: Int, scope: CoroutineScope) {
        tickJob?.cancel()
        _state.value = RestTimerState(timeRemaining = seconds, isRunning = true)

        tickJob = scope.launch {
            while (_state.value.timeRemaining > 0) {
                delay(1000L)
                _state.value = _state.value.copy(
                    timeRemaining = _state.value.timeRemaining - 1
                )
            }
            _state.value = _state.value.copy(isRunning = false)
        }
    }

    fun stop() {
        tickJob?.cancel()
        tickJob = null
        _state.value = _state.value.copy(timeRemaining = 0, isRunning = false)
    }
}