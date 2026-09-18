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
        val isEquipmentLimited: Boolean,
        val isPlateaued: Boolean = false,
        val isDeloadRecommended: Boolean = false
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
        equipmentType: String = "home",
        readinessScore: Double? = null,
        consecutiveSessionsAtSameWeight: Int = 0
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
                isEquipmentLimited = false,
                isPlateaued = false,
                isDeloadRecommended = false
            )
        }

        val currentWeight = normalSets.first().weight
        val isBodyweight = currentWeight == 0.0
        val currentReps = normalSets.map { it.reps }
        val allHitTop = currentReps.all { it >= targetRepsMax }
        val anyBelowMin = currentReps.any { it < targetRepsMin }
        val isEquipmentLimited = equipmentAvailability.isLimited(exerciseEquipment, equipmentType)

        // 1. Severe low readiness (< 2.0): Deload recommendation
        if (readinessScore != null && readinessScore < 2.0) {
            val newWeight = if (!isBodyweight) calculateDecrease(currentWeight) else 0.0
            val newSets = (targetSets - 1).coerceAtLeast(2)
            return ProgressionRecommendation(
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                currentWeight = currentWeight,
                currentReps = currentReps,
                recommendedWeight = newWeight,
                recommendedReps = "$targetRepsMin-$targetRepsMax",
                recommendedSets = newSets,
                reason = "Readiness is critically low (${String.format(java.util.Locale.US, "%.1f", readinessScore)}/5.0). Deload recommended to promote recovery.",
                confidence = 0.85,
                isEquipmentLimited = isEquipmentLimited,
                isPlateaued = false,
                isDeloadRecommended = true
            )
        }

        // 2. Reduced readiness (< 2.5): Hold load
        if (readinessScore != null && readinessScore < 2.5) {
            return ProgressionRecommendation(
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                currentWeight = currentWeight,
                currentReps = currentReps,
                recommendedWeight = currentWeight,
                recommendedReps = "$targetRepsMin-$targetRepsMax",
                recommendedSets = targetSets,
                reason = "Reduced readiness (${String.format(java.util.Locale.US, "%.1f", readinessScore)}/5.0). Hold current weight to allow recovery.",
                confidence = 0.8,
                isEquipmentLimited = isEquipmentLimited,
                isPlateaued = false,
                isDeloadRecommended = false
            )
        }

        // 3. Plateau detection: 3+ sessions at same weight without hitting top rep target
        val isPlateaued = consecutiveSessionsAtSameWeight >= 3 && !allHitTop
        if (isPlateaued) {
            val newWeight = if (!isBodyweight) calculateDecrease(currentWeight) else 0.0
            return ProgressionRecommendation(
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                currentWeight = currentWeight,
                currentReps = currentReps,
                recommendedWeight = newWeight,
                recommendedReps = "$targetRepsMin-$targetRepsMax",
                recommendedSets = targetSets,
                reason = "Plateau detected ($consecutiveSessionsAtSameWeight sessions without progression). Recommended temporary weight reset to build momentum.",
                confidence = 0.85,
                isEquipmentLimited = isEquipmentLimited,
                isPlateaued = true,
                isDeloadRecommended = true
            )
        }

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
                    isEquipmentLimited = false,
                    isPlateaued = false,
                    isDeloadRecommended = false
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
                    isEquipmentLimited = true,
                    isPlateaued = false,
                    isDeloadRecommended = false
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
                    isEquipmentLimited = false,
                    isPlateaued = false,
                    isDeloadRecommended = false
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
                    isEquipmentLimited = isEquipmentLimited,
                    isPlateaued = false,
                    isDeloadRecommended = false
                )
            }
        }
    }

    private fun filterNormalSets(sets: List<WorkoutSetEntity>): List<WorkoutSetEntity> {
        return sets.filter { it.completed && it.setType == 0 } // 0 = NORMAL
    }

    fun roundToIncrement(weight: Double, increment: Double = 2.5): Double {
        if (increment <= 0.0) return weight
        return Math.round(weight / increment) * increment
    }

    private fun calculateIncrease(currentWeight: Double, increment: Double = 2.5): Double {
        val inc = if (currentWeight < 20.0) 2.0 else increment
        val raw = when {
            currentWeight < 20.0 -> currentWeight + 2.0
            currentWeight < 50.0 -> currentWeight + 2.5
            currentWeight < 100.0 -> currentWeight + 5.0
            else -> (currentWeight * 1.05).coerceAtMost(currentWeight + 10.0) // ACSM 2-10%
        }
        return roundToIncrement(raw, inc)
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

    companion object {
        fun parseRepRange(repRangeStr: String, defaultMin: Int = 8, defaultMax: Int = 12): Pair<Int, Int> {
            if (repRangeStr.isBlank()) return Pair(defaultMin, defaultMax)
            val parts = repRangeStr.split("-", "–", "to")
                .map { it.trim().filter { c -> c.isDigit() } }
                .filter { it.isNotBlank() }
            return when (parts.size) {
                2 -> Pair(parts[0].toIntOrNull() ?: defaultMin, parts[1].toIntOrNull() ?: defaultMax)
                1 -> {
                    val v = parts[0].toIntOrNull() ?: defaultMin
                    Pair((v - 2).coerceAtLeast(1), v)
                }
                else -> Pair(defaultMin, defaultMax)
            }
        }
    }

    fun calculateProgressionForExercise(
        exercise: com.gymcoach.app.domain.model.Exercise,
        previousSets: List<WorkoutSetEntity>,
        currentSets: List<WorkoutSetEntity>,
        equipmentType: String = "home",
        readinessScore: Double? = null,
        consecutiveSessionsAtSameWeight: Int = 0
    ): ProgressionRecommendation {
        val (minReps, maxReps) = parseRepRange(exercise.recommendedRepRange, 8, 12)
        return calculateProgression(
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            exerciseEquipment = exercise.equipment,
            targetRepsMin = minReps,
            targetRepsMax = maxReps,
            targetSets = 3,
            previousSets = previousSets,
            currentSets = currentSets,
            equipmentType = equipmentType,
            readinessScore = readinessScore,
            consecutiveSessionsAtSameWeight = consecutiveSessionsAtSameWeight
        )
    }

}
