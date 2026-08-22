package com.gymcoach.app.core.progression

import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PRDetector @Inject constructor() {

    data class PersonalRecord(
        val exerciseId: Long,
        val exerciseName: String,
        val type: PRType,
        val value: Double,
        val details: String,
        val date: Instant,
        val workoutId: Long
    )

    enum class PRType { WEIGHT, REP, ESTIMATED_1RM, VOLUME, BEST_SET }

    fun detectPRs(
        exerciseId: Long,
        exerciseName: String,
        currentSets: List<WorkoutSetEntity>,
        existingPRs: List<PersonalRecord>,
        workoutId: Long
    ): List<PersonalRecord> {
        val detected = mutableListOf<PersonalRecord>()
        val now = Instant.now()
        val completed = currentSets.filter { it.completed }
        if (completed.isEmpty()) return emptyList()

        val maxWeight = completed.maxOf { it.weight }
        val eWPR = existingPRs.filter { it.type == PRType.WEIGHT }.maxByOrNull { it.value }
        if (maxWeight > (eWPR?.value ?: 0.0)) {
            detected.add(PersonalRecord(exerciseId, exerciseName, PRType.WEIGHT, maxWeight, "${maxWeight}kg lifted", now, workoutId))
        }

        val bestReps = completed.maxBy { it.reps }
        val eRPR = existingPRs.filter { it.type == PRType.REP }.maxByOrNull { it.value }
        if (bestReps.reps > (eRPR?.value?.toInt() ?: 0)) {
            detected.add(PersonalRecord(exerciseId, exerciseName, PRType.REP, bestReps.reps.toDouble(), "${bestReps.reps} reps at ${bestReps.weight}kg", now, workoutId))
        }

        val bestE1RM = completed.maxOf { calculateEstimated1RM(it.weight, it.reps) }
        val e1PR = existingPRs.filter { it.type == PRType.ESTIMATED_1RM }.maxByOrNull { it.value }
        if (bestE1RM > (e1PR?.value ?: 0.0)) {
            detected.add(PersonalRecord(exerciseId, exerciseName, PRType.ESTIMATED_1RM, bestE1RM, "e1RM: ${String.format("%.1f", bestE1RM)}kg", now, workoutId))
        }

        val volume = calculateVolume(completed)
        val eVPR = existingPRs.filter { it.type == PRType.VOLUME }.maxByOrNull { it.value }
        if (volume > (eVPR?.value ?: 0.0)) {
            detected.add(PersonalRecord(exerciseId, exerciseName, PRType.VOLUME, volume, "Volume: ${String.format("%.0f", volume)}kg", now, workoutId))
        }

        val bestSet = completed.maxBy { calculateEstimated1RM(it.weight, it.reps) }
        val bestSetE1RM = calculateEstimated1RM(bestSet.weight, bestSet.reps)
        val eBPR = existingPRs.filter { it.type == PRType.BEST_SET }.maxByOrNull { it.value }
        if (bestSetE1RM > (eBPR?.value ?: 0.0)) {
            detected.add(PersonalRecord(exerciseId, exerciseName, PRType.BEST_SET, bestSetE1RM, "Best: ${bestSet.weight}kg x ${bestSet.reps}", now, workoutId))
        }

        return detected
    }

    fun calculateEstimated1RM(weight: Double, reps: Int): Double {
        if (weight <= 0 || reps <= 0) return 0.0
        return weight * (1 + reps.coerceAtMost(12).toDouble() / 30.0)
    }

    fun calculateVolume(sets: List<WorkoutSetEntity>): Double {
        return sets.sumOf { it.weight * it.reps }
    }
}
