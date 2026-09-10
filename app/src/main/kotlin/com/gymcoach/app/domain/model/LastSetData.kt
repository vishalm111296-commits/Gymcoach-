package com.gymcoach.app.domain.model

data class LastSetData(
    val setNumber: Int = 0,
    val reps: Int,
    val weight: Double,
    val rpe: Float?,
    val restSeconds: Int = 60,
    val setType: Int = 0
) {
    val weightKg: Double get() = weight
}
