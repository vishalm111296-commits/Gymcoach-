package com.gymcoach.app.domain.vshape.usecase.impl

import com.gymcoach.app.data.local.entity.VShapeChallengeCompletion
import com.gymcoach.app.data.local.entity.BodyMeasurement
import com.gymcoach.app.domain.vshape.model.ChallengeDay
import com.gymcoach.app.domain.vshape.model.ChallengeDayType
import com.gymcoach.app.domain.vshape.usecase.VShapeChallengeUseCase
import com.gymcoach.app.domain.vshape.model.ChallengeDay as DomainChallengeDay
import com.gymcoach.app.domain.vshape.model.DailyTarget
import com.gymcoach.app.domain.vshape.model.ExerciseRequirement
import com.gymcoach.app.domain.vshape.model.NutritionRequirement
import com.gymcoach.app.domain.vshape.model.RecoveryRequirement
import com.gymcoach.app.domain.vshape.model.MeasurementRequirement
import java.time.DayOfWeek
import javax.inject.Inject

class VShapeChallengeUseCaseImpl @Inject constructor() : VShapeChallengeUseCase {
    override fun getCurrentChallengeDay(): DomainChallengeDay {
        val dayNumber = getCurrentDayInChallenge()
        val dayOfWeek = DayOfWeek.of((dayNumber % 7) + 1)
        val dayType = when {
            isRestDay(dayNumber) -> ChallengeDayType.REST
            dayNumber % 3 == 0 -> ChallengeDayType.POSTURE
            dayNumber % 2 == 0 -> ChallengeDayType.WORKOUT
            else -> ChallengeDayType.MEASUREMENT
        }
        
        return DomainChallengeDay(
            dayNumber = dayNumber,
            dayOfWeek = dayOfWeek,
            isRestDay = isRestDay(dayNumber),
            isMeasurementDay = dayNumber % 2 == 0,
            isNutritionFocus = dayNumber % 5 == 0,
            isRecoveryDay = dayNumber % 7 == 0,
            isPostureDay = dayNumber % 3 == 0,
            isHydrationDay = false,
            isSleepDay = false,
            exerciseRequirements = getExerciseRequirements(dayType),
            nutritionRequirements = getNutritionRequirements(dayType),
            recoveryRequirements = getRecoveryRequirements(dayType),
            measurementRequirements = getMeasurementRequirements(dayType),
            points = calculateDayPoints(dayNumber, dayType),
            bonusPoints = calculateBonusPoints(dayNumber, dayType),
            completed = false,
            completionDate = null
        )
    }

    override fun getChallengeCompletion(completions: List<VShapeChallengeCompletion>, day: Int): VShapeChallengeCompletion? {
        return completions.find { it.day == day }
    }

    override fun calculateStreak(completions: List<VShapeChallengeCompletion>): Int {
        if (completions.isEmpty()) return 0
        return completions.filter { it.completed }.maxOfOrNull { it.day } ?: 0
    }

    override fun calculateProgress(completions: List<VShapeChallengeCompletion>): ChallengeProgress {
        val completedDays = completions.count { it.completed }
        val totalDays = 30
        val progress = if (totalDays > 0) (completedDays.toFloat() / totalDays.toFloat()) else 0f
        
        return ChallengeProgress(
            completedDays = completedDays,
            totalDays = totalDays,
            progress = progress,
            streak = calculateStreak(completions),
            currentDay = getCurrentDayInChallenge(),
            challengesCompletedToday = completions.any { it.day == getCurrentDayInChallenge() && it.completed },
            recoveryDaysTaken = completions.count { it.completed && isRestDay(it.day) },
            createdAt = java.time.Instant.now()
        )
    }

    override fun getDailyTargets(dayNumber: Int): DailyTarget {
        val dayType = when {
            isRestDay(dayNumber) -> ChallengeDayType.REST
            dayNumber % 3 == 0 -> ChallengeDayType.POSTURE
            dayNumber % 2 == 0 -> ChallengeDayType.WORKOUT
            else -> ChallengeDayType.MEASUREMENT
        }
        
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

    override fun getExerciseRecommendations(dayType: ChallengeDayType, dayNumber: Int, muscleGroups: List<String>): List<String> {
        return when (dayType) {
            ChallengeDayType.WORKOUT -> when {
                muscleGroups.contains("lats") -> listOf("Pull-ups", "Lat Pulldown", "Bent-over Row")
                muscleGroups.contains("side delts") -> listOf("Dumbbell Lateral Raise", "Cable Lateral Raise")
                muscleGroups.contains("rear delts") -> listOf("Face Pulls", "Reverse Flyes")
                muscleGroups.contains("upper back") -> listOf("Pull-ups", "Row")
                else -> listOf("Pull-ups", "Lateral Raise", "Face Pull")
            }
            ChallengeDayType.POSTURE -> listOf("Wall Slides", "Thoracic Upward Rotation", "Scapular Retraction")
            else -> listOf()
        }
    }

    override fun calculateRecoveryScore(sleepHours: Int, soreness: Int, fatigue: Int, workoutCompleted: Boolean): Int {
        val sleepScore = if (sleepHours >= 7) 10 else if (sleepHours >= 6) 7 else 3
        val sorenessScore = if (soreness == 0) 10 else if (soreness <= 3) 7 else if (soreness <= 6) 4 else 1
        val fatigueScore = if (!workoutCompleted) 10 else if (fatigue <= 3) 7 else if (fatigue <= 6) 4 else 1
        
        return (sleepScore + sorenessScore + fatigueScore) / 3
    }

    override fun shouldTakeRestDay(dayNumber: Int, lastWeekPerformance: Double, recoveryScore: Int): Boolean {
        if (isRestDay(dayNumber)) return true
        if (lastWeekPerformance < 0.6) return true
        if (recoveryScore < 5) return true
        if (dayNumber % 5 == 0) return true
        return false
    }

    override fun getProgressiveOverloadTarget(
        previousWeight: Float,
        previousReps: Int,
        previousRpe: Float,
        dayNumber: Int,
        targetVolume: Float,
        achievedVolume: Float
    ): Float {
        val volumeDeficit = targetVolume - achievedVolume
        val percentageIncrease = if (volumeDeficit > 0) {
            val baseIncrease = 0.05f
            val bonusIncrease = if (previousRpe >= 8.5f) 0.03f else 0f
            baseIncrease + bonusIncrease
        } else {
            0f
        }
        return previousWeight * (1 + percentageIncrease)
    }

    override fun calculateDailyBonusPoints(dayNumber: Int, dayType: ChallengeDayType, streak: Int): Int {
        var bonus = 0
        if (dayType == ChallengeDayType.POSTURE) bonus += 5
        if (dayType == ChallengeDayType.MEASUREMENT) bonus += 3
        if (isRestDay(dayNumber)) bonus += 2
        if (streak >= 7) bonus += 20
        if (streak >= 14) bonus += 30
        if (streak >= 21) bonus += 50
        if (streak >= 30) bonus += 100
        return bonus
    }

    override fun validateChallengeCompletion(
        dayNumber: Int,
        dayType: ChallengeDayType,
        measurements: List<BodyMeasurement>,
        completions: List<VShapeChallengeCompletion>
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        if (!isDayCompleted(dayNumber, completions)) {
            errors.add("Day $dayNumber not completed")
        }
        
        if (dayType == ChallengeDayType.MEASUREMENT) {
            val measurement = measurements.find { it.date >= System.currentTimeMillis() - 86400000 } // Within last 24h
            if (measurement == null) {
                warnings.add("Measurement day without recorded measurements")
            }
        }
        
        if (dayType == ChallengeDayType.RECOVERY && !isRestDay(dayNumber)) {
            warnings.add("Recovery day should have rest or recovery activity")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    private fun getCurrentDayInChallenge(): Int {
        val startDate = System.currentTimeMillis() - (29 * 24 * 60 * 60 * 1000L)
        val daysSinceStart = ((System.currentTimeMillis() - startDate) / (24 * 60 * 60 * 1000L)).toInt()
        return (daysSinceStart % 30) + 1
    }

    private fun isRestDay(dayNumber: Int): Boolean {
        return dayNumber % 7 == 0
    }

    private fun isDayCompleted(dayNumber: Int, completions: List<VShapeChallengeCompletion>): Boolean {
        return completions.any { it.day == dayNumber && it.completed }
    }

    private fun calculateDayPoints(dayNumber: Int, dayType: ChallengeDayType): Int {
        return when (dayType) {
            ChallengeDayType.WORKOUT -> 10
            ChallengeDayType.MEASUREMENT -> 5
            ChallengeDayType.POSTURE -> 8
            ChallengeDayType.RECOVERY -> 3
            ChallengeDayType.HYDRATION -> 2
            ChallengeDayType.SLEEP -> 3
            ChallengeDayType.REST -> 1
            else -> 1
        }
    }

    private fun calculateBonusPoints(dayNumber: Int, dayType: ChallengeDayType): Int {
        var bonus = 0
        if (dayType == ChallengeDayType.POSTURE) bonus += 5
        if (dayType == ChallengeDayType.MEASUREMENT) bonus += 3
        if (isRestDay(dayNumber)) bonus += 2
        return bonus
    }

    private fun getExerciseRequirements(dayType: ChallengeDayType): List<ExerciseRequirement> {
        return when (dayType) {
            ChallengeDayType.WORKOUT -> listOf(
                ExerciseRequirement("compound", 4, 8, 135f, 8.5f, 120),
                ExerciseRequirement("isolation", 2, 12, 45f, 8f, 90)
            )
            ChallengeDayType.MEASUREMENT -> emptyList()
            ChallengeDayType.POSTURE -> listOf(
                ExerciseRequirement("stretching", 1, 10, 0f, null, 0),
                ExerciseRequirement("mobility", 1, 15, 0f, null, 0)
            )
            else -> emptyList()
        }
    }

    private fun getNutritionRequirements(dayType: ChallengeDayType): List<NutritionRequirement> {
        return when (dayType) {
            ChallengeDayType.NUTRITION -> listOf(
                NutritionRequirement(
                    MealType.BREAKFAST,
                    40,
                    600,
                    2.0,
                    NutritionTiming.WITHIN_1_HOUR
                ),
                NutritionRequirement(
                    MealType.LUNCH,
                    50,
                    800,
                    2.0,
                    NutritionTiming.REGULAR_TIMING
                ),
                NutritionRequirement(
                    MealType.DINNER,
                    50,
                    700,
                    2.0,
                    NutritionTiming.REGULAR_TIMING
                )
            )
            else -> emptyList()
        }
    }

    private fun getRecoveryRequirements(dayType: ChallengeDayType): List<RecoveryRequirement> {
        return when (dayType) {
            ChallengeDayType.WORKOUT -> listOf(
                RecoveryRequirement(8, 7, 5, 8),
                RecoveryRequirement(8, 5, 4, 7)
            )
            ChallengeDayType.RECOVERY -> listOf(
                RecoveryRequirement(9, 3, 2, 8),
                RecoveryRequirement(10, 2, 1, 9)
            )
            else -> emptyList()
        }
    }

    private fun getMeasurementRequirements(dayType: ChallengeDayType): List<MeasurementRequirement> {
        return when (dayType) {
            ChallengeDayType.MEASUREMENT -> listOf(
                MeasurementRequirement("shoulder circumference", null, "Shoulder width"),
                MeasurementRequirement("waist circumference", null, "Waist measurement")
            )
            else -> emptyList()
        }
    }
}