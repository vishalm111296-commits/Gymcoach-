package com.gymcoach.app.domain.model

data class OverloadRecommendation(
    val recommendedWeight: Double,
    val recommendedReps: Int,
    val explanation: String
)

object CoachingEngine {
    fun recommendOverload(
        exerciseName: String,
        lastSets: List<WorkoutSet>,
        targetRepMin: Int,
        targetRepMax: Int,
        isWeighted: Boolean
    ): OverloadRecommendation {
        if (lastSets.isEmpty()) {
            return OverloadRecommendation(0.0, targetRepMin, "No previous data. Starting with baseline reps.")
        }
        val completedSets = lastSets.filter { it.completed }
        if (completedSets.isEmpty()) {
            return OverloadRecommendation(0.0, targetRepMin, "No completed sets. Starting with baseline reps.")
        }
        val allHitMax = completedSets.all { it.reps >= targetRepMax }
        return if (allHitMax) {
            if (isWeighted) {
                // If weight of the sets is different, we increase from the max weight used or last weight used.
                // Standard double progression: increment weight. We'll use the last completed set weight as baseline.
                val lastWeight = completedSets.last().weight
                OverloadRecommendation(
                    lastWeight + 2.5,
                    targetRepMin,
                    "Target reps hit on all sets. Recommending weight increase."
                )
            } else {
                OverloadRecommendation(
                    0.0,
                    targetRepMax + 2,
                    "Target reps hit on all bodyweight sets. Recommending rep increase."
                )
            }
        } else {
            val lastWeight = completedSets.last().weight
            OverloadRecommendation(
                lastWeight,
                targetRepMax,
                "Work on hitting target rep range of $targetRepMin-$targetRepMax across all sets with current weight."
            )
        }
    }

    fun calculateE1RM(weight: Double, reps: Int): Double {
        return weight * (1.0 + reps.toDouble() / 30.0)
    }

    fun filterByEquipment(exercises: List<Exercise>, allowedEquipment: List<String>): List<Exercise> {
        val allowedSet = allowedEquipment.map { it.lowercase() }.toSet()
        return exercises.filter { exercise ->
            val eq = exercise.equipment.lowercase()
            eq == "bodyweight" || allowedSet.contains(eq)
        }
    }

    fun getAlternatives(
        exercise: Exercise,
        allExercises: List<Exercise>,
        allowedEquipment: List<String>
    ): List<Exercise> {
        val filtered = filterByEquipment(allExercises, allowedEquipment)
        return filtered.filter {
            it.id != exercise.id &&
            it.name != exercise.name &&
            it.muscleGroup.equals(exercise.muscleGroup, ignoreCase = true)
        }
    }

    fun calculateMuscleSetVolumes(workouts: List<WorkoutWithDetails>): Map<String, Double> {
        val volumeMap = mutableMapOf<String, Double>()
        for (workout in workouts) {
            for (ewSets in workout.exercises) {
                val exercise = ewSets.exercise
                val completedCount = ewSets.sets.count { it.completed }
                if (completedCount == 0) continue

                val primary = exercise.muscleGroup.trim()
                if (primary.isNotEmpty()) {
                    volumeMap[primary] = volumeMap.getOrDefault(primary, 0.0) + (1.0 * completedCount)
                }

                if (exercise.secondaryMuscles.isNotEmpty()) {
                    val secondaries = exercise.secondaryMuscles.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    for (sec in secondaries) {
                        volumeMap[sec] = volumeMap.getOrDefault(sec, 0.0) + (0.5 * completedCount)
                    }
                }
            }
        }
        return volumeMap
    }
}
