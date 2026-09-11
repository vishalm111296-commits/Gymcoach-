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

    private var _nextSetLabel: String = ""
    val nextSetLabel: String get() = _nextSetLabel

    fun start(seconds: Int, nextSet: String = "") {
        val safeSeconds = seconds.coerceAtLeast(0)
        if (safeSeconds == 0) {
            cancel()
            return
        }
        _remainingSeconds.value = safeSeconds
        _nextSetLabel = nextSet
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
        _isPaused.value = false
        _isRunning.value = false
        _nextSetLabel = ""
        onStateChanged?.invoke()
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
