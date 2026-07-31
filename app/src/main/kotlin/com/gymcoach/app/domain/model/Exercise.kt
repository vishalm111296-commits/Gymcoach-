package com.gymcoach.app.domain.model

data class Exercise(
    val id: Long = 0,
    val name: String,
    val description: String,
    val muscleGroup: String,
    val equipment: String,
    val difficulty: String
)