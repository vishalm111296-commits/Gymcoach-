package com.gymcoach.app.domain.model

/**
 * Explicit lifecycle states for a workout session.
 *
 * Transitions (enforced by WorkoutLoggingViewModel):
 *
 *   NOT_STARTED --> ACTIVE      user taps "Start" (row created eagerly at start)
 *   ACTIVE      --> COMPLETED   completeWorkout()
 *   ACTIVE      --> ABANDONED   discardWorkout() with content present
 *   ACTIVE      --> (deleted)   discardWorkout() with zero content - row removed entirely
 *   PAUSED      <-> ACTIVE      reserved for a future workout-level pause feature;
 *                               rest-timer pauses remain session-scoped in-memory
 *                               state inside RestTimerManager and do NOT touch the DB.
 *   NOT_STARTED                 never persisted; exists so domain code can represent
 *                               "no workout yet" without nulls.
 *
 * Persisted as TEXT in workouts.status (migration 7 -> 8).
 * The legacy Boolean `completed` column is kept in lockstep
 * (COMPLETED == true) because all analytics queries filter on it.
 */
enum class WorkoutStatus {
    NOT_STARTED,
    ACTIVE,
    PAUSED,
    COMPLETED,
    ABANDONED;

    companion object {
        fun fromString(raw: String?, completed: Boolean): WorkoutStatus =
            entries.firstOrNull { it.name == raw }
                ?: if (completed) COMPLETED else ACTIVE
    }
}
