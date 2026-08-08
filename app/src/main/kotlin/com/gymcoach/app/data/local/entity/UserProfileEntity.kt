package com.gymcoach.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Long = 1,
    val name: String = "",
    val age: Int = 0,
    val gender: String = "", // Could be enum but using string for simplicity
    val height: Double = 0.0, // in cm
    val weight: Double = 0.0, // in kg
    val goalWeight: Double = 0.0,
    val currentGoal: String = "",
    val experience: String = "", // beginner, intermediate, advanced
    val trainingStyle: String = "", // e.g., bodybuilding, powerlifting
    val preferredSplit: String = "", // e.g., push/pull/legs, upper/lower
    val activityLevel: String = "", // sedentary, lightly active, etc.
    val weeklyWorkoutGoal: Int = 0,
    val proteinGoal: Double = 0.0, // grams per day
    val caloriesGoal: Int = 0,
    val units: String = "metric", // metric or imperial
    val avatarUrl: String = "", // placeholder for now
    val leanBodyMass: Double = 0.0,
    val maintenanceCalories: Int = 0
)