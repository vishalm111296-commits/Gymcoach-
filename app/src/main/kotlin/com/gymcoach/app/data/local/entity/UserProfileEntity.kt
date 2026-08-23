package com.gymcoach.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-user profile captured during onboarding.
 * Columns mirror MIGRATION_4_5 SQL for the `user_profiles` table exactly.
 */
@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "goal", defaultValue = "") val goal: String = "",
    @ColumnInfo(name = "experience", defaultValue = "") val experience: String = "",
    @ColumnInfo(name = "age", defaultValue = "0") val age: Int = 0,
    @ColumnInfo(name = "sex", defaultValue = "") val sex: String = "",
    @ColumnInfo(name = "height_cm", defaultValue = "0.0") val heightCm: Double = 0.0,
    @ColumnInfo(name = "weight_kg", defaultValue = "0.0") val weightKg: Double = 0.0,
    @ColumnInfo(name = "training_days_per_week", defaultValue = "4") val trainingDaysPerWeek: Int = 4,
    @ColumnInfo(name = "session_length_minutes", defaultValue = "60") val sessionLengthMinutes: Int = 60,
    @ColumnInfo(name = "equipment_type", defaultValue = "gym") val equipmentType: String = "gym",
    @ColumnInfo(name = "preferred_exercises", defaultValue = "") val preferredExercises: String = "",
    @ColumnInfo(name = "exercises_to_avoid", defaultValue = "") val exercisesToAvoid: String = "",
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long = System.currentTimeMillis()
)
