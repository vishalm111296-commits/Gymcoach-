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

    enum class PRType { WEIGHT, REP, ESTIMATED_1RM, VOLUME }

    fun detectPRs(
        exerciseId: Long,
        exerciseName: String,
        currentSets: List<WorkoutSetEntity>,
        existingPRs: List<PersonalRecord>,
        workoutId: Long
    ): List<PersonalRecord> {
        val detected = mutableListOf<PersonalRecord>()
        val now = Instant.now()
        val normalSets = currentSets.filter { it.completed && it.setType == 0 }
        if (normalSets.isEmpty()) return emptyList()

        // Weight PR
        val maxWeight = normalSets.maxOf { it.weight }
        val eWPR = existingPRs.filter { it.type == PRType.WEIGHT }.maxByOrNull { it.value }
        if (maxWeight > (eWPR?.value ?: 0.0)) {
            detected.add(PersonalRecord(exerciseId, exerciseName, PRType.WEIGHT, maxWeight, "${maxWeight}kg lifted", now, workoutId))
        }

        // Rep PR (most reps at any weight)
        val bestReps = normalSets.maxBy { it.reps }
        val eRPR = existingPRs.filter { it.type == PRType.REP }.maxByOrNull { it.value }
        if (bestReps.reps > (eRPR?.value?.toInt() ?: 0)) {
            detected.add(PersonalRecord(exerciseId, exerciseName, PRType.REP, bestReps.reps.toDouble(), "${bestReps.reps} reps at ${bestReps.weight}kg", now, workoutId))
        }

        // Estimated 1RM PR (Epley formula, capped at 12 reps)
        val bestE1RM = normalSets.maxOf { calculateEstimated1RM(it.weight, it.reps) }
        val e1PR = existingPRs.filter { it.type == PRType.ESTIMATED_1RM }.maxByOrNull { it.value }
        if (bestE1RM > (e1PR?.value ?: 0.0)) {
            detected.add(PersonalRecord(exerciseId, exerciseName, PRType.ESTIMATED_1RM, bestE1RM, "e1RM: ${String.format("%.1f", bestE1RM)}kg", now, workoutId))
        }

        // Volume PR (session volume for this exercise)
        val volume = calculateVolume(normalSets)
        val eVPR = existingPRs.filter { it.type == PRType.VOLUME }.maxByOrNull { it.value }
        if (volume > (eVPR?.value ?: 0.0)) {
            detected.add(PersonalRecord(exerciseId, exerciseName, PRType.VOLUME, volume, "Volume: ${String.format("%.0f", volume)}kg", now, workoutId))
        }

        // Bodyweight exercises: rep-based e1RM and volume
        if (normalSets.first().weight == 0.0) {
            val bodyweightReps = normalSets.maxBy { it.reps }.reps
            val bodyweightE1RM = bodyweightReps.toDouble() * 1.5 // Simple bodyweight strength proxy
            val bwE1PR = existingPRs.filter { it.type == PRType.ESTIMATED_1RM }.maxByOrNull { it.value }
            if (bodyweightE1RM > (bwE1PR?.value ?: 0.0)) {
                detected.add(PersonalRecord(exerciseId, exerciseName, PRType.ESTIMATED_1RM, bodyweightE1RM, "Bodyweight e1RM: ${String.format("%.1f", bodyweightE1RM)}kg", now, workoutId))
            }
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