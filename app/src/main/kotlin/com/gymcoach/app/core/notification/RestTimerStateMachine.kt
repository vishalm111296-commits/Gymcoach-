package com.gymcoach.app.core.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Encapsulates all state transitions for workout rest intervals.
 * Decouples timer business logic from Android Service and Notification lifecycles
 * to allow exhaustive, deterministic unit testing without mocks.
 */
class RestTimerStateMachine(
    private val onStateChanged: (() -> Unit)? = null,
    private val onComplete: (() -> Unit)? = null,
    private val onCancel: (() -> Unit)? = null
) {
    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _totalDurationSeconds = MutableStateFlow(0)
    val totalDurationSeconds: StateFlow<Int> = _totalDurationSeconds.asStateFlow()

    private var _nextSetLabel: String = ""
    val nextSetLabel: String get() = _nextSetLabel

    private var _workoutId: Long = -1L
    val workoutId: Long get() = _workoutId

    fun start(seconds: Int, nextSet: String = "", workoutId: Long = -1L) {
        val safeSeconds = seconds.coerceAtLeast(0)
        if (safeSeconds == 0) {
            cancel()
            return
        }
        _remainingSeconds.value = safeSeconds
        _totalDurationSeconds.value = safeSeconds
        _nextSetLabel = nextSet
        _workoutId = workoutId
        _isPaused.value = false
        _isRunning.value = true
        onStateChanged?.invoke()
    }

    fun tick(remaining: Int) {
        if (!_isRunning.value || _isPaused.value) return
        val safe = remaining.coerceAtLeast(0)
        _remainingSeconds.value = safe
        if (safe == 0) {
            complete()
        } else {
            onStateChanged?.invoke()
        }
    }

    fun pause() {
        if (!_isRunning.value || _isPaused.value) return
        _isPaused.value = true
        onStateChanged?.invoke()
    }

    fun resume() {
        if (!_isRunning.value || !_isPaused.value) return
        _isPaused.value = false
        onStateChanged?.invoke()
    }

    fun adjust(deltaSeconds: Int): Int {
        if (!_isRunning.value) return 0
        val newRemaining = _remainingSeconds.value + deltaSeconds
        if (newRemaining <= 0) {
            cancel()
            return 0
        }
        _remainingSeconds.value = newRemaining
        _totalDurationSeconds.value = maxOf(_totalDurationSeconds.value, newRemaining)
        onStateChanged?.invoke()
        return newRemaining
    }

    fun complete() {
        _remainingSeconds.value = 0
        _isPaused.value = false
        _isRunning.value = false
        onComplete?.invoke()
        onStateChanged?.invoke()
    }

    fun cancel() {
        _remainingSeconds.value = 0
        _isPaused.value = false
        _isRunning.value = false
        onCancel?.invoke()
        onStateChanged?.invoke()
    }

    fun reset() {
        _remainingSeconds.value = 0
        _totalDurationSeconds.value = 0
        _isPaused.value = false
        _isRunning.value = false
        _nextSetLabel = ""
        _workoutId = -1L
        onStateChanged?.invoke()
    }

    fun toDurableState(restEndEpochMillis: Long): DurableTimerState {
        return DurableTimerState(
            isRunning = _isRunning.value,
            isPaused = _isPaused.value,
            restEndEpochMillis = restEndEpochMillis,
            totalDurationSeconds = _totalDurationSeconds.value,
            pausedRemainingSeconds = _remainingSeconds.value,
            nextSetLabel = _nextSetLabel,
            workoutId = _workoutId
        )
    }

    fun restore(state: DurableTimerState, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!state.isRunning) {
            reset()
            return false
        }
        val remaining = state.calculateRemainingSeconds(nowMillis)
        if (remaining <= 0) {
            complete()
            return false
        }
        _remainingSeconds.value = remaining
        _totalDurationSeconds.value = maxOf(state.totalDurationSeconds, remaining)
        _nextSetLabel = state.nextSetLabel
        _workoutId = state.workoutId
        _isPaused.value = state.isPaused
        _isRunning.value = true
        onStateChanged?.invoke()
        return true
    }

    companion object {
        fun formatSeconds(totalSeconds: Int): String {
            val safeSec = totalSeconds.coerceAtLeast(0)
            val minutes = safeSec / 60
            val seconds = safeSec % 60
            return "%02d:%02d".format(minutes, seconds)
        }
    }
}
