package com.gymcoach.app.domain.model

data class HistoricalSet(
    val weight: Double,
    val reps: Int,
    val rpe: Double,
    val restSeconds: Int,
    val setType: Int,
    val date: Long
)
