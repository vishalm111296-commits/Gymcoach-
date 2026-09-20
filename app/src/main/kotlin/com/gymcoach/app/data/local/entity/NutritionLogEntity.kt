package com.gymcoach.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nutrition_logs")
data class NutritionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = System.currentTimeMillis(), // epoch millis
    val mealName: String = "Meal", // "Breakfast", "Lunch", "Dinner", "Snack", "Pre-workout", "Post-workout"
    val calories: Int = 0,
    val proteinGrams: Float = 0f,
    val carbsGrams: Float = 0f,
    val fatGrams: Float = 0f,
    val fiberGrams: Float = 0f,
    val waterMl: Int = 0,
    val notes: String = ""
)
