package com.gymcoach.app.core.progression

import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressionEngine @Inject constructor() {

    data class ProgressionRecommendation(
        val exerciseId: Long,
        val exerciseName: String,
        val currentWeight: Double,
        val currentReps: List<Int>,
        val recommendedWeight: Double,
        val recommendedReps: String,
        val reason: String,
        val confidence: Double
    )

    fun calculateProgression(
        exerciseId: Long,
        exerciseName: String,
        targetRepsMin: Int,
        targetRepsMax: Int,
        previousSets: List<WorkoutSetEntity>,
        currentSets: List<WorkoutSetEntity>
    ): ProgressionRecommendation {
        val currentWeight = currentSets.firstOrNull()?.weight ?: 0.0
        val currentReps = currentSets.map { it.reps }
        val allHitTop = currentReps.all { it >= targetRepsMax }
        val anyBelowMin = currentReps.any { it < targetRepsMin }

        return when {
            allHitTop -> {
                val newWeight = calculateIncrease(currentWeight)
                ProgressionRecommendation(
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    currentWeight = currentWeight,
                    currentReps = currentReps,
                    recommendedWeight = newWeight,
                    recommendedReps = "$targetRepsMin-$targetRepsMax",
                    reason = "All sets reached top of rep range ($targetRepsMax). Increase weight.",
                    confidence = 0.9
                )
            }
            anyBelowMin && isRegressing(previousSets, targetRepsMin) -> {
                val newWeight = calculateDecrease(currentWeight)
                ProgressionRecommendation(
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    currentWeight = currentWeight,
                    currentReps = currentReps,
                    recommendedWeight = newWeight,
                    recommendedReps = "$targetRepsMin-$targetRepsMax",
                    reason = "Reps below minimum for 2+ sessions. Reduce weight.",
                    confidence = 0.8
                )
            }
            else -> {
                ProgressionRecommendation(
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    currentWeight = currentWeight,
                    currentReps = currentReps,
                    recommendedWeight = currentWeight,
                    recommendedReps = "$targetRepsMin-$targetRepsMax",
                    reason = "Maintain current weight and focus on hitting target reps.",
                    confidence = 0.7
                )
            }
        }
    }

    private fun calculateIncrease(currentWeight: Double): Double {
        return when {
            currentWeight < 20 -> currentWeight + 2.0
            currentWeight < 50 -> currentWeight + 2.5
            currentWeight < 100 -> currentWeight + 5.0
            else -> currentWeight * 1.05
        }
    }

    private fun calculateDecrease(currentWeight: Double): Double {
        return currentWeight * 0.9
    }

    private fun isRegressing(previousSets: List<WorkoutSetEntity>, targetMin: Int): Boolean {
        if (previousSets.isEmpty()) return false
        val prevReps = previousSets.map { it.reps }
        return prevReps.all { it < targetMin }
    }
}
