package com.gymcoach.app.core.notification

import android.content.Context
import androidx.core.content.edit

/**
 * Authoritative, durable state for the workout rest interval timer.
 * Enables deterministic process-death recovery, activity recreation recovery,
 * and seamless background timing using wall-clock epoch timestamps.
 */
data class DurableTimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val restEndEpochMillis: Long = 0L,
    val totalDurationSeconds: Int = 0,
    val pausedRemainingSeconds: Int = 0,
    val nextSetLabel: String = "",
    val workoutId: Long = -1L
) {
    fun calculateRemainingSeconds(nowMillis: Long = System.currentTimeMillis()): Int {
        if (!isRunning) return 0
        if (isPaused) return pausedRemainingSeconds.coerceAtLeast(0)
        val remainingMillis = restEndEpochMillis - nowMillis
        return if (remainingMillis <= 0) 0 else ((remainingMillis + 999) / 1000).toInt()
    }
}

object RestTimerPreferences {
    private const val PREFS_NAME = "gymcoach_rest_timer_prefs"
    private const val KEY_RUNNING = "is_running"
    private const val KEY_PAUSED = "is_paused"
    private const val KEY_END_MILLIS = "rest_end_epoch_millis"
    private const val KEY_TOTAL_DURATION = "total_duration_seconds"
    private const val KEY_PAUSED_REMAINING = "paused_remaining_seconds"
    private const val KEY_NEXT_SET = "next_set_label"
    private const val KEY_WORKOUT_ID = "workout_id"

    fun load(context: Context): DurableTimerState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return DurableTimerState(
            isRunning = prefs.getBoolean(KEY_RUNNING, false),
            isPaused = prefs.getBoolean(KEY_PAUSED, false),
            restEndEpochMillis = prefs.getLong(KEY_END_MILLIS, 0L),
            totalDurationSeconds = prefs.getInt(KEY_TOTAL_DURATION, 0),
            pausedRemainingSeconds = prefs.getInt(KEY_PAUSED_REMAINING, 0),
            nextSetLabel = prefs.getString(KEY_NEXT_SET, "") ?: "",
            workoutId = prefs.getLong(KEY_WORKOUT_ID, -1L)
        )
    }

    fun save(context: Context, state: DurableTimerState) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(KEY_RUNNING, state.isRunning)
            putBoolean(KEY_PAUSED, state.isPaused)
            putLong(KEY_END_MILLIS, state.restEndEpochMillis)
            putInt(KEY_TOTAL_DURATION, state.totalDurationSeconds)
            putInt(KEY_PAUSED_REMAINING, state.pausedRemainingSeconds)
            putString(KEY_NEXT_SET, state.nextSetLabel)
            putLong(KEY_WORKOUT_ID, state.workoutId)
        }
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { clear() }
    }
}
