package com.gymcoach.app.domain.vshape.usecase.impl

import com.gymcoach.app.data.local.entity.VShapeChallengeCompletion
import com.gymcoach.app.domain.vshape.model.MeasurementRecord
import com.gymcoach.app.domain.vshape.model.ChallengeDayType
import com.gymcoach.app.domain.vshape.model.DailyTarget
import com.gymcoach.app.domain.vshape.model.ValidationResult
import com.gymcoach.app.domain.vshape.usecase.VShapeUseCase
import javax.inject.Inject

class VShapeUseCaseImpl @Inject constructor() : VShapeUseCase {
    override fun calculateShoulderToWaistRatio(measurement: MeasurementRecord): Float {
        return (measurement.value / 2.54).toFloat() // Convert cm to inches for ratio calculation
    }

    override fun calculateWeeklyVolume(day: Int, sets: Int, reps: Int, weight: Float, exerciseType: String): Float {
        // Apply V-Shape specific volume targets based on exercise type
        val volumeMultiplier = when (exerciseType) {
            "lats" -> 1.2f
            "side delts" -> 1.0f
            "rear delts" -> 0.8f
            "upper back" -> 1.1f
            "core" -> 0.9f
            else -> 1.0f
        }
        return (sets.toFloat() * reps.toFloat() * weight) * volumeMultiplier
    }

    override fun getCurrentDayInChallenge(): Int {
        val startDate = System.currentTimeMillis() - (29 * 24 * 60 * 60 * 1000L)
        val daysSinceStart = ((System.currentTimeMillis() - startDate) / (24 * 60 * 60 * 1000L)).toInt()
        return (daysSinceStart % 30) + 1
    }

    override fun calculateStreak(completions: List<VShapeChallengeCompletion>): Int {
        if (completions.isEmpty()) return 0
        return completions.filter { it.completed }.maxOfOrNull { it.day } ?: 0
    }

    override fun isRestDay(day: Int): Boolean {
        return day % 7 == 0
    }

    override fun calculateVShapeIndex(shoulder: Float, waist: Float): Float {
        return shoulder / waist
    }

    override fun getDailyTarget(day: Int): DailyTarget {
        val dayType = getChallengeDayType(day)
        return when (dayType) {
            ChallengeDayType.WORKOUT -> DailyTarget(
                dayType = ChallengeDayType.WORKOUT,
                targetVolume = 1500.toFloat(),
                targetSets = 8,
                targetReps = 10,
                targetWeight = 135f,
                targetRpe = 8.5f,
                restTimeSeconds = 120
            )
            ChallengeDayType.MEASUREMENT -> DailyTarget(
                dayType = ChallengeDayType.MEASUREMENT,
                targetVolume = 0f,
                targetSets = 0,
                targetReps = 0,
                targetWeight = 0f,
                targetRpe = 0f,
                restTimeSeconds = 0
            )
            else -> DailyTarget(
                dayType = dayType,
                targetVolume = 300f,
                targetSets = 0,
                targetReps = 0,
                targetWeight = 0f,
                targetRpe = 0f,
                restTimeSeconds = 0
            )
        }
    }

    override fun getRecommendedExercisesForDay(day: Int, difficulty: String): List<String> {
        val dayType = getChallengeDayType(day)
        return when (dayType) {
            ChallengeDayType.WORKOUT -> when (difficulty) {
                "BEGINNER" -> listOf("Lat Pulldown", "Dumbbell Lateral Raise", "Face Pull", "Plank")
                "INTERMEDIATE" -> listOf("Pull-ups", "Cable Lateral Raise", "Bent-over Row", "Hanging Leg Raise")
                "ADVANCED" -> listOf("Weighted Pull-ups", "Cable Face Pulls", "Arnold Press", "Hanging Leg Raise", "Deadlift")
                else -> listOf("Lat Pulldown", "Lateral Raise", "Face Pull", "Plank")
            }
            ChallengeDayType.MEASUREMENT -> listOf("Measurement Input")
            ChallengeDayType.MOBILITY -> listOf("Wall Slides", "Thoracic Mobility Drills", "Shoulder Dislocations")
            else -> listOf()
        }
    }

    override fun calculateRecoveryScore(sleepHours: Int, soreness: Int, fatigue: Int): Int {
        val sleepScore = (sleepHours * 10 / 8).coerceIn(0, 10).toInt()
        val sorenessScore = (10 - soreness).coerceIn(0, 10)
        val fatigueScore = (10 - fatigue).coerceIn(0, 10)
        return ((sleepScore + sorenessScore + fatigueScore) / 3).toInt()
    }

    override fun getChallengeDayType(day: Int): ChallengeDayType {
        return when {
            isRestDay(day) -> ChallengeDayType.REST
            day % 3 == 0 -> ChallengeDayType.MOBILITY
            day % 2 == 0 -> ChallengeDayType.WORKOUT
            else -> ChallengeDayType.MEASUREMENT
        }
    }

    override fun shouldTakeRestDay(day: Int, lastWeekPerformance: Double): Boolean {
        return if (isRestDay(day)) true
        else if (lastWeekPerformance < 0.6) true
        else if (day % 5 == 0) true
        else false
    }

    override fun getVolumeTargetForMuscleGroup(muscleGroup: String, day: Int): Float {
        return when (muscleGroup.lowercase()) {
            "lats" -> 1500.toFloat()
            "side delts" -> 1200.toFloat()
            "rear delts" -> 800.toFloat()
            "upper back" -> 1000.toFloat()
            "core" -> 600.toFloat()
            else -> 0f
        }
    }

    override fun calculateProgressiveOverload(weight: Float, reps: Int, rpe: Float, dayOfWeek: Int): Float {
        val percentageIncrease = when {
            rpe >= 9.0f -> 0.025f
            rpe >= 8.5f -> 0.015f
            rpe >= 8.0f -> 0.01f
            else -> 0.0f
        }
        return weight * (1 + percentageIncrease)
    }

    override fun getEstimatedTimeForWorkout(exercises: List<String>): Int {
        return exercises.size * 45 + 15
    }

    override fun validateMeasurementData(shoulder: Float, waist: Float, weight: Float?): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        if (shoulder <= 0) errors.add("Shoulder circumference must be positive")
        if (waist <= 0) errors.add("Waist circumference must be positive")
        if (waist >= shoulder * 0.9) warnings.add("Waist circumference too large relative to shoulder width")
        if (shoulder / waist < 1.0) errors.add("V-Taper ratio too low. Shoulder width should be greater than waist width")
        
        if (weight != null && weight <= 0) errors.add("Weight must be positive")
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    override fun getOptimalTrainingFrequency(trainingHistory: List<java.time.Instant>): Int {
        if (trainingHistory.size < 3) return 3
        val recentWeeks = trainingHistory.take(4)
        return if (recentWeeks.size == 4) 4 else 3
    }

    override fun generateWorkoutSuggestion(muscleGroups: List<String>, availableTime: Int, equipment: List<String>): List<String> {
        val suggestions = mutableListOf<String>()
        
        muscleGroups.forEach { muscleGroup ->
            when (muscleGroup.lowercase()) {
                "lats" -> suggestions.add("Pull-ups or Lat Pulldowns")
                "side delts" -> suggestions.add("Dumbbell Lateral Raises")
                "rear delts" -> suggestions.add("Face Pulls")
                "upper back" -> suggestions.add("Bent-over Rows")
                "core" -> suggestions.add("Planks or Russian Twists")
            }
        }
        
        if (suggestions.isEmpty()) suggestions.add("Generic full-body workout")
        
        return suggestions.take(availableTime / 45)
    }

    override fun calculateRestTimeBetweenSets(rpe: Float, sets: Int): Int {
        return when {
            rpe >= 9.0f -> 180
            rpe >= 8.5f -> 150
            rpe >= 8.0f -> 120
            else -> 90
        }
    }

    override fun getExerciseOrder(dayType: ChallengeDayType, muscleGroups: List<String>): List<String> {
        return when (dayType) {
            ChallengeDayType.WORKOUT -> listOf("compound", "isolation", "core")
            ChallengeDayType.MOBILITY -> listOf("mobility", "activation")
            else -> listOf()
        }
    }

    override fun shouldProgressLoad(weight: Float, reps: Int, day: Int): Boolean {
        return reps >= 8 && weight > 0
    }
}