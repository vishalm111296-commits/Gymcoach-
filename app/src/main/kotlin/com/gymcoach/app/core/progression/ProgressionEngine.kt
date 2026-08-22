package com.gymcoach.app.core.progression

import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import com.gymcoach.app.core.exercise.EquipmentAvailability
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressionEngine @Inject constructor(
    private val equipmentAvailability: EquipmentAvailability
) {

    data class ProgressionRecommendation(
        val exerciseId: Long,
        val exerciseName: String,
        val currentWeight: Double,
        val currentReps: List<Int>,
        val recommendedWeight: Double,
        val recommendedReps: String,
        val recommendedSets: Int?,
        val reason: String,
        val confidence: Double,
        val isEquipmentLimited: Boolean
    )

    fun calculateProgression(
        exerciseId: Long,
        exerciseName: String,
        exerciseEquipment: String,
        targetRepsMin: Int,
        targetRepsMax: Int,
        targetSets: Int,
        previousSets: List<WorkoutSetEntity>,
        currentSets: List<WorkoutSetEntity>,
        equipmentType: String = "home"
    ): ProgressionRecommendation {
        val normalSets = filterNormalSets(currentSets)
        if (normalSets.isEmpty()) {
            return ProgressionRecommendation(
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                currentWeight = 0.0,
                currentReps = emptyList(),
                recommendedWeight = 0.0,
                recommendedReps = "$targetRepsMin-$targetRepsMax",
                recommendedSets = targetSets,
                reason = "No completed working sets yet.",
                confidence = 0.5,
                isEquipmentLimited = false
            )
        }

        val currentWeight = normalSets.first().weight
        val isBodyweight = currentWeight == 0.0
        val currentReps = normalSets.map { it.reps }
        val allHitTop = currentReps.all { it >= targetRepsMax }
        val anyBelowMin = currentReps.any { it < targetRepsMin }
        val isEquipmentLimited = equipmentAvailability.isLimited(exerciseEquipment, equipmentType)

        return when {
            allHitTop && !isBodyweight && !isEquipmentLimited -> {
                val newWeight = calculateIncrease(currentWeight)
                ProgressionRecommendation(
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    currentWeight = currentWeight,
                    currentReps = currentReps,
                    recommendedWeight = newWeight,
                    recommendedReps = "$targetRepsMin-$targetRepsMax",
                    recommendedSets = targetSets,
                    reason = "All sets reached top of rep range ($targetRepsMax). Increase weight.",
                    confidence = 0.9,
                    isEquipmentLimited = false
                )
            }
            allHitTop && (isBodyweight || isEquipmentLimited) -> {
                val newSets = targetSets + 1
                val newReps = targetRepsMax + 2
                ProgressionRecommendation(
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    currentWeight = currentWeight,
                    currentReps = currentReps,
                    recommendedWeight = currentWeight,
                    recommendedReps = "$targetRepsMin-$newReps",
                    recommendedSets = newSets,
                    reason = "Equipment limited — add set/rep progression instead of weight.",
                    confidence = 0.75,
                    isEquipmentLimited = true
                )
            }
            anyBelowMin && isRegressing(previousSets, targetRepsMin) -> {
                val newWeight = if (!isBodyweight) calculateDecrease(currentWeight) else 0.0
                ProgressionRecommendation(
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    currentWeight = currentWeight,
                    currentReps = currentReps,
                    recommendedWeight = newWeight,
                    recommendedReps = "$targetRepsMin-$targetRepsMax",
                    recommendedSets = targetSets,
                    reason = "Reps below minimum for 2+ sessions. ${if (isBodyweight) "Focus on form and range of motion." else "Reduce weight."}",
                    confidence = 0.8,
                    isEquipmentLimited = false
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
                    recommendedSets = targetSets,
                    reason = "Maintain current weight and focus on hitting target reps.",
                    confidence = 0.7,
                    isEquipmentLimited = isEquipmentLimited
                )
            }
        }
    }

    private fun filterNormalSets(sets: List<WorkoutSetEntity>): List<WorkoutSetEntity> {
        return sets.filter { it.completed && it.setType == 0 } // 0 = NORMAL
    }

    private fun calculateIncrease(currentWeight: Double): Double {
        return when {
            currentWeight < 20 -> currentWeight + 2.0
            currentWeight < 50 -> currentWeight + 2.5
            currentWeight < 100 -> currentWeight + 5.0
            else -> (currentWeight * 1.05).coerceAtMost(currentWeight + 10.0) // ACSM 2-10%
        }
    }

    private fun calculateDecrease(currentWeight: Double): Double {
        return currentWeight * 0.9
    }

    private fun isRegressing(previousSets: List<WorkoutSetEntity>, targetMin: Int): Boolean {
        val normalPrev = filterNormalSets(previousSets)
        if (normalPrev.isEmpty()) return false
        val prevReps = normalPrev.map { it.reps }
        return prevReps.all { it < targetMin }
    }
}