package com.gymcoach.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val muscleGroup: String,
    val equipment: String,
    val difficulty: String,
    val secondaryMuscles: String = "",
    val instructions: String = "",
    val tips: String = "",
    val commonMistakes: String = "",
    val safetyNotes: String = "",
    val recommendedRepRange: String = "",
    val recommendedRestTime: String = "",
    val estimatedCalories: Int = 0,
    val category: String = "",
    val tags: String = "",
    val isFavorite: Boolean = false,
    val lastViewed: Long = 0L
)