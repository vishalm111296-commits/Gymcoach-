package com.gymcoach.app.core.program

import com.gymcoach.app.domain.model.SetType
import com.gymcoach.app.domain.model.WorkoutWithDetails
import javax.inject.Inject
import javax.inject.Singleton

data class DeloadStatus(
    val isDeloadRecommended: Boolean,
    val reason: DeloadReason?,
    val accumulatedWeeks: Int,
    val volumeReductionPercent: Int = 50, // 50% set volume reduction
    val intensityReductionPercent: Int = 10, // 10% load reduction
    val coachingAdvice: String
)

enum class DeloadReason {
    PLANNED_MESOCYCLE_END,   // 4-6 weeks of progressive overload completed
    FATIGUE_ACCUMULATION,    // Low readiness (<55) sustained
    STAGNATION_PLATEAU,      // Plateau across multiple consecutive sessions
    RECOVERY_RESET           // User explicitly requested or missed training
}

@Singleton
class DeloadEngine @Inject constructor() {

    companion object {
        const val MESOCYCLE_DELOAD_WEEK_THRESHOLD = 4
        const val LOW_READINESS_THRESHOLD = 55
        const val DEFAULT_VOLUME_REDUCTION_PERCENT = 50
        const val DEFAULT_INTENSITY_REDUCTION_PERCENT = 10
    }

    /**
     * Evaluates whether a deload week is recommended based on:
     * 1. Explicit user recovery reset request
     * 2. Planned mesocycle duration (week >= 4)
     * 3. Sustained low readiness scores (< 55) indicating systemic fatigue accumulation
     * 4. Performance plateau across consecutive sessions
     * 5. Fallback: continue progressive overload
     */
    fun evaluateDeloadNeed(
        workoutHistory: List<WorkoutWithDetails>,
        readinessScores: List<Int> = emptyList(),
        currentWeekInBlock: Int = 1,
        userRequestedReset: Boolean = false
    ): DeloadStatus {
        // 1. Explicit user recovery reset request
        if (userRequestedReset) {
            return DeloadStatus(
                isDeloadRecommended = true,
                reason = DeloadReason.RECOVERY_RESET,
                accumulatedWeeks = currentWeekInBlock,
                volumeReductionPercent = DEFAULT_VOLUME_REDUCTION_PERCENT,
                intensityReductionPercent = DEFAULT_INTENSITY_REDUCTION_PERCENT,
                coachingAdvice = "A recovery reset deload is recommended to re-acclimate your muscular and connective tissues safely before resuming high-intensity overload."
            )
        }

        // 2. Planned mesocycle end (week >= 4)
        if (currentWeekInBlock >= MESOCYCLE_DELOAD_WEEK_THRESHOLD) {
            return DeloadStatus(
                isDeloadRecommended = true,
                reason = DeloadReason.PLANNED_MESOCYCLE_END,
                accumulatedWeeks = currentWeekInBlock,
                volumeReductionPercent = DEFAULT_VOLUME_REDUCTION_PERCENT,
                intensityReductionPercent = DEFAULT_INTENSITY_REDUCTION_PERCENT,
                coachingAdvice = "You've completed $currentWeekInBlock weeks of progressive overload. A planned deload is recommended to dissipate accumulated fatigue, restore volume sensitivity, and prepare for your next mesocycle."
            )
        }

        // 3. Sustained low readiness (< 55) -> Fatigue accumulation
        if (isLowReadinessSustained(readinessScores)) {
            val avgReadiness = if (readinessScores.isNotEmpty()) {
                val recent = readinessScores.takeLast(minOf(3, readinessScores.size))
                recent.average().toInt()
            } else 0
            return DeloadStatus(
                isDeloadRecommended = true,
                reason = DeloadReason.FATIGUE_ACCUMULATION,
                accumulatedWeeks = currentWeekInBlock,
                volumeReductionPercent = DEFAULT_VOLUME_REDUCTION_PERCENT,
                intensityReductionPercent = DEFAULT_INTENSITY_REDUCTION_PERCENT,
                coachingAdvice = "Sustained low readiness scores (recent average: $avgReadiness/100) indicate significant accumulated fatigue. A deload week is recommended to prevent overreaching and allow central nervous system recovery."
            )
        }

        // 4. Stagnation plateau across 3+ consecutive sessions
        if (hasStagnationPlateau(workoutHistory)) {
            return DeloadStatus(
                isDeloadRecommended = true,
                reason = DeloadReason.STAGNATION_PLATEAU,
                accumulatedWeeks = currentWeekInBlock,
                volumeReductionPercent = DEFAULT_VOLUME_REDUCTION_PERCENT,
                intensityReductionPercent = DEFAULT_INTENSITY_REDUCTION_PERCENT,
                coachingAdvice = "Performance has plateaued across consecutive sessions. A deload week will help clear systemic fatigue and prepare your body to break through the plateau."
            )
        }

        // 5. No deload needed - fresh / progressing lifter
        return DeloadStatus(
            isDeloadRecommended = false,
            reason = null,
            accumulatedWeeks = currentWeekInBlock,
            volumeReductionPercent = 0,
            intensityReductionPercent = 0,
            coachingAdvice = "Readiness and performance markers are optimal. Continue progressive overload for week $currentWeekInBlock."
        )
    }

    /**
     * Scales down a workout for a deload session:
     * - Halves the number of sets per exercise (50% volume reduction), maintaining a minimum of 1-2 sets.
     * - Reduces working weight by 10% (90% load intensity), rounded to nearest 0.5kg or 2.5kg increment.
     */
    fun applyDeloadToWorkout(
        workout: WorkoutWithDetails,
        weightIncrement: Double = 0.5
    ): WorkoutWithDetails {
        val deloadExercises = workout.exercises.map { exerciseWithSets ->
            val originalSets = exerciseWithSets.sets
            val targetSetCount = if (originalSets.isEmpty()) {
                0
            } else {
                // Halve sets with minimum 1 set (e.g. 1 set -> 1, 2 sets -> 1, 3 sets -> 2, 4 sets -> 2, 5 sets -> 3, 6 sets -> 3)
                maxOf(1, Math.round(originalSets.size * 0.5).toInt())
            }

            val scaledSets = originalSets.take(targetSetCount).mapIndexed { index, set ->
                val scaledWeight = if (set.weight > 0.0) {
                    roundToNearestIncrement(set.weight * 0.90, weightIncrement)
                } else {
                    0.0
                }
                set.copy(
                    setNumber = index + 1,
                    weight = scaledWeight
                )
            }

            exerciseWithSets.copy(sets = scaledSets)
        }

        return workout.copy(exercises = deloadExercises)
    }

    fun roundToNearestIncrement(weight: Double, increment: Double = 0.5): Double {
        if (increment <= 0.0 || weight <= 0.0) return weight
        val quotient = weight / increment
        val rounded = Math.round(quotient) * increment
        return Math.round(rounded * 100.0) / 100.0
    }

    private fun isLowReadinessSustained(readinessScores: List<Int>): Boolean {
        if (readinessScores.isEmpty()) return false
        val recent = readinessScores.takeLast(minOf(3, readinessScores.size))
        if (recent.isNotEmpty() && recent.average() < LOW_READINESS_THRESHOLD.toDouble()) return true
        if (recent.size >= 2 && recent.all { it < LOW_READINESS_THRESHOLD }) return true
        return false
    }

    private fun hasStagnationPlateau(workoutHistory: List<WorkoutWithDetails>): Boolean {
        if (workoutHistory.size < 3) return false
        val sorted = workoutHistory.sortedBy { it.workout.date }
        val recentWorkouts = sorted.takeLast(3)
        val volumes = recentWorkouts.map { workout ->
            workout.exercises.flatMap { it.sets }
                .filter { it.completed && it.setType == SetType.NORMAL }
                .sumOf { it.weight * it.reps }
        }
        if (volumes.all { it > 0.0 }) {
            // Non-increasing volume across 3 consecutive sessions indicates plateau/stagnation
            return volumes[2] <= volumes[1] && volumes[1] <= volumes[0]
        }
        return false
    }
}
