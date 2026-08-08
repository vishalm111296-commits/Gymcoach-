package com.gymcoach.app.domain.model

data class UserProfile(
    val id: Long = 1,
    val name: String = "",
    val age: Int = 0,
    val gender: String = "",
    val height: Double = 0.0,
    val weight: Double = 0.0,
    val goalWeight: Double = 0.0,
    val currentGoal: String = "",
    val experience: String = "",
    val trainingStyle: String = "",
    val preferredSplit: String = "",
    val activityLevel: String = "",
    val weeklyWorkoutGoal: Int = 0,
    val proteinGoal: Double = 0.0,
    val caloriesGoal: Int = 0,
    val units: String = "metric",
    val avatarUrl: String = "",
    val leanBodyMass: Double = 0.0,
    val maintenanceCalories: Int = 0
)