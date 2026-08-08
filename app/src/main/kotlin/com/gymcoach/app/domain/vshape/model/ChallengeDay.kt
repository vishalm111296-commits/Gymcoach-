package com.gymcoach.app.domain.vshape.model

import java.time.DayOfWeek

data class ChallengeDay(
    val dayNumber: Int,
    val dayOfWeek: DayOfWeek,
    val isRestDay: Boolean = false,
    val isMeasurementDay: Boolean = false,
    val isNutritionFocus: Boolean = false,
    val isRecoveryDay: Boolean = false,
    val isPostureDay: Boolean = false,
    val isHydrationDay: Boolean = false,
    val isSleepDay: Boolean = false,
    val exerciseRequirements: List<ExerciseRequirement> = emptyList(),
    val nutritionRequirements: List<NutritionRequirement> = emptyList(),
    val recoveryRequirements: List<RecoveryRequirement> = emptyList(),
    val measurementRequirements: List<MeasurementRequirement> = emptyList(),
    val points: Int = 0,
    val bonusPoints: Int = 0,
    val completed: Boolean = false,
    val completionDate: java.time.Instant? = null
)

data class ExerciseRequirement(
    val exerciseType: String,
    val targetSets: Int,
    val targetReps: Int,
    val targetWeight: Double? = null,
    val targetRpe: Double? = null,
    val restTimeSeconds: Int? = null
)

data class NutritionRequirement(
    val mealType: MealType,
    val targetProteinGrams: Int,
    val targetCalories: Int,
    val hydrationLiters: Double,
    val timing: NutritionTiming
)

enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK,
    PRE_WORKOUT,
    POST_WORKOUT
}

enum class NutritionTiming {
    WITHIN_30_MINUTES,
    WITHIN_1_HOUR,
    WITHIN_2_HOURS,
    BEFORE_SLEEP,
    REGULAR_TIMING
}

data class RecoveryRequirement(
    val sleepHours: Double,
    val sorenessLimit: Int,
    val fatigueLimit: Int,
    val motivationLevel: Int
)

data class MeasurementRequirement(
    val measurementType: String,
    val targetValue: Double? = null,
    val notes: String? = null
)