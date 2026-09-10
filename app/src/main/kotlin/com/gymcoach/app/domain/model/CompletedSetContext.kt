package com.gymcoach.app.domain.model

data class CompletedSetContext(
    val setId: Long,
    val exerciseId: Long,
    val workoutDate: Long,
    val weightKg: Double,
    val reps: Int,
    val rpe: Float?,
    val completed: Boolean,
    val setType: Int
)
