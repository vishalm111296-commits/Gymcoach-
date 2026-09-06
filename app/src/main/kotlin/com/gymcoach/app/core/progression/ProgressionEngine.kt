package com.gymcoach.app.core.progression

import com.gymcoach.app.core.exercise.EquipmentAvailability
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
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
        require(targetRepsMin > 0 && targetRepsMax >= targetRepsMin) { "Invalid target rep range" }
        require(targetSets > 0) { "targetSets must be positive" }

        val normalCurrent = filterNormalSets(currentSets)
        if (normalCurrent.isEmpty()) {
            return recommendation(
                exerciseId,
                exerciseName,
                currentWeight = 0.0,
                currentReps = emptyList(),
                recommendedWeight = 0.0,
                recommendedReps = "$targetRepsMin-$targetRepsMax",
                recommendedSets = targetSets,
                reason = "No completed working sets yet.",
                confidence = 0.5,
                limited = false
            )
        }

        val currentWeight = normalCurrent.map { it.weight }.filter { it > 0 }.averageOrNull() ?: 0.0
        val currentReps = normalCurrent.map { it.reps }
        val averageRir = normalCurrent.mapNotNull { rirFromRpe(it.rpe) }.averageOrNull()
        val allHitTop = currentReps.all { it >= targetRepsMax }
        val allAtOrAboveMin = currentReps.all { it >= targetRepsMin }
        val anyBelowMin = currentReps.any { it < targetRepsMin }
        val limited = equipmentAvailability.isLimited(exerciseEquipment, equipmentType)
        val bodyweight = currentWeight <= 0.0

        // Do not progress an exercise that reached the top only by repeatedly
        // training to failure. Prefer stable reps with approximately 1-3 RIR.
        val recoverableEffort = averageRir == null || averageRir >= 1.0

        return when {
            allHitTop && recoverableEffort && !bodyweight && !limited -> {
                val newWeight = roundLoad(calculateIncrease(currentWeight))
                recommendation(
                    exerciseId, exerciseName, currentWeight, currentReps,
                    recommendedWeight = newWeight,
                    recommendedReps = "$targetRepsMin-$targetRepsMax",
                    recommendedSets = targetSets,
                    reason = "You reached the top of the rep range with a manageable effort. Add a small load and restart near the bottom of the range.",
                    confidence = 0.9,
                    limited = false
                )
            }
            allHitTop && recoverableEffort && (bodyweight || limited) -> {
                // With no practical load increase, first progress reps within a
                // capped range; only add a set after the rep ceiling is exceeded.
                val nextMax = targetRepsMax + 2
                val nextSets = if (targetRepsMax >= 20) targetSets + 1 else targetSets
                recommendation(
                    exerciseId, exerciseName, currentWeight, currentReps,
                    recommendedWeight = currentWeight,
                    recommendedReps = "$targetRepsMin-$nextMax",
                    recommendedSets = nextSets,
                    reason = if (bodyweight) {
                        "No external load recorded. Progress reps first; when the upper end becomes easy, use a harder variation or add a set."
                    } else {
                        "Available equipment limits a practical load increase. Progress reps first rather than forcing a large jump."
                    },
                    confidence = 0.8,
                    limited = limited
                )
            }
            anyBelowMin && isRegressing(previousSets, targetRepsMin) -> {
                val newWeight = if (!bodyweight) roundLoad(calculateDecrease(currentWeight)) else 0.0
                recommendation(
                    exerciseId, exerciseName, currentWeight, currentReps,
                    recommendedWeight = newWeight,
                    recommendedReps = "$targetRepsMin-$targetRepsMax",
                    recommendedSets = targetSets,
                    reason = if (bodyweight) {
                        "Performance is below the target range across consecutive sessions. Keep the variation, reduce difficulty, or use a shorter range of progression."
                    } else {
                        "Performance is below the target range across consecutive sessions. Reduce the load by about 10% and rebuild reps."
                    },
                    confidence = 0.82,
                    limited = limited
                )
            }
            allAtOrAboveMin -> {
                recommendation(
                    exerciseId, exerciseName, currentWeight, currentReps,
                    recommendedWeight = currentWeight,
                    recommendedReps = "$targetRepsMin-$targetRepsMax",
                    recommendedSets = targetSets,
                    reason = "Keep the current load and aim to add reps before increasing difficulty.",
                    confidence = 0.75,
                    limited = limited
                )
            }
            else -> {
                recommendation(
                    exerciseId, exerciseName, currentWeight, currentReps,
                    recommendedWeight = currentWeight,
                    recommendedReps = "$targetRepsMin-$targetRepsMax",
                    recommendedSets = targetSets,
                    reason = "Stay with the current setup until your working sets consistently reach the target range.",
                    confidence = 0.7,
                    limited = limited
                )
            }
        }
    }

    private fun recommendation(
        exerciseId: Long,
        exerciseName: String,
        currentWeight: Double,
        currentReps: List<Int>,
        recommendedWeight: Double,
        recommendedReps: String,
        recommendedSets: Int,
        reason: String,
        confidence: Double,
        limited: Boolean
    ) = ProgressionRecommendation(
        exerciseId = exerciseId,
        exerciseName = exerciseName,
        currentWeight = currentWeight,
        currentReps = currentReps,
        recommendedWeight = recommendedWeight,
        recommendedReps = recommendedReps,
        recommendedSets = recommendedSets,
        reason = reason,
        confidence = confidence,
        isEquipmentLimited = limited
    )

    private fun filterNormalSets(sets: List<WorkoutSetEntity>): List<WorkoutSetEntity> =
        sets.filter { it.completed && it.setType == 0 && it.reps > 0 }

    private fun calculateIncrease(currentWeight: Double): Double = when {
        currentWeight < 10.0 -> currentWeight + 1.0
        currentWeight < 20.0 -> currentWeight + 2.0
        currentWeight < 40.0 -> currentWeight + 2.0
        currentWeight < 60.0 -> currentWeight + 2.5
        currentWeight < 100.0 -> currentWeight + 5.0
        else -> currentWeight * 1.05
    }

    private fun calculateDecrease(currentWeight: Double): Double = currentWeight * 0.9

    private fun isRegressing(previousSets: List<WorkoutSetEntity>, targetMin: Int): Boolean {
        val normalPrev = filterNormalSets(previousSets)
        if (normalPrev.isEmpty()) return false
        return normalPrev.map { it.reps }.all { it < targetMin }
    }

    private fun rirFromRpe(rpe: Double): Double? = when {
        rpe <= 0.0 -> null
        rpe in 1.0..10.0 -> 10.0 - rpe
        else -> null
    }

    private fun roundLoad(value: Double): Double =
        (value * 2.0).toInt() / 2.0

    private fun Iterable<Double>.averageOrNull(): Double? =
        if (none()) null else average()
}
