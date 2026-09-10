package com.gymcoach.app.core.program

import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.core.exercise.EquipmentAvailability
import com.gymcoach.app.domain.repository.ReadinessRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgramGenerator @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val equipmentAvailability: EquipmentAvailability,
    private val readinessRepository: ReadinessRepository
) {
    data class GeneratedProgram(
        val name: String,
        val description: String,
        val goal: String,
        val frequency: Int,
        val days: List<ProgramDay>
    )

    data class ProgramDay(
        val dayNumber: Int,
        val name: String,
        val targetMuscles: List<String>,
        val exercises: List<ProgramExercise>
    )

    data class ProgramExercise(
        val exerciseId: Long,
        val exerciseName: String,
        val targetSets: Int,
        val targetRepsMin: Int,
        val targetRepsMax: Int,
        val targetRpe: Double,
        val restSeconds: Int
    )

    companion object {
        const val WARMUP_COOLDOWN_BUFFER_MINUTES = 5.0
        const val SET_EXECUTION_MINUTES = 0.75
        const val EXERCISE_TRANSITION_MINUTES = 2.0

        fun estimateExerciseMinutes(targetSets: Int, restSeconds: Int): Double {
            return targetSets * (SET_EXECUTION_MINUTES + restSeconds / 60.0) + EXERCISE_TRANSITION_MINUTES
        }

        fun estimateDayDurationMinutes(exercises: List<ProgramExercise>): Int {
            if (exercises.isEmpty()) return 0
            val total = WARMUP_COOLDOWN_BUFFER_MINUTES + exercises.sumOf {
                estimateExerciseMinutes(it.targetSets, it.restSeconds)
            }
            return total.toInt()
        }
    }

    suspend fun generateProgram(
        frequency: Int,
        equipmentType: String,
        goal: String,
        sessionLengthMinutes: Int = 60
    ): GeneratedProgram {
        val latestReadiness = readinessRepository.getLatestReadiness().first()
        val availableEquipment = equipmentAvailability.getAvailableEquipment(equipmentType)
        val allExercises = exerciseDao.getAll().first()
        val filteredExercises = filterByEquipment(allExercises, availableEquipment)

        val baseReadinessScore = latestReadiness?.readinessScore ?: 3.0
        val days = when (frequency) {
            1 -> generateFullBody1Day(filteredExercises, baseReadinessScore, sessionLengthMinutes)
            2 -> generateFullBody2Day(filteredExercises, baseReadinessScore, sessionLengthMinutes)
            3 -> generateFullBody(filteredExercises, baseReadinessScore, sessionLengthMinutes)
            4 -> generateUpperLower(filteredExercises, baseReadinessScore, sessionLengthMinutes)
            5 -> generatePPLUpperLower(filteredExercises, baseReadinessScore, sessionLengthMinutes)
            6 -> generatePPLDouble(filteredExercises, baseReadinessScore, sessionLengthMinutes)
            else -> generateUpperLower(filteredExercises, baseReadinessScore, sessionLengthMinutes)
        }
        return GeneratedProgram(
            name = "V-Taper $frequency-Day Program",
            description = "Personalized $frequency-day training program for $goal. Target session length: $sessionLengthMinutes mins. Adjusted for readiness score: ${"%.1f".format(baseReadinessScore)}",
            goal = goal,
            frequency = frequency,
            days = days
        )
    }

    private fun filterByEquipment(
        exercises: List<ExerciseEntity>,
        availableEquipment: Set<String>
    ): List<ExerciseEntity> {
        return exercises.filter { ex ->
            if (ex.equipment == "bodyweight" || ex.equipment.isBlank()) return@filter true

            val equipmentTokens = ex.equipment.split(",").map { it.trim().lowercase() }
            if (equipmentTokens.size > 1) {
                equipmentTokens.all { token ->
                    availableEquipment.contains(token) || token == "bodyweight"
                }
            } else {
                availableEquipment.contains(ex.equipment.lowercase())
            }
        }
    }

    private fun generateFullBody1Day(
        exercises: List<ExerciseEntity>,
        baseReadinessScore: Double,
        sessionLengthMinutes: Int
    ): List<ProgramDay> {
        val dayA = listOf("Back", "Chest", "Quadriceps", "Lateral Deltoid", "Hamstrings", "Core")
        return listOf(
            buildDay(1, "Full Body A", dayA, exercises, baseReadinessScore, sessionLengthMinutes)
        )
    }

    private fun generateFullBody2Day(
        exercises: List<ExerciseEntity>,
        baseReadinessScore: Double,
        sessionLengthMinutes: Int
    ): List<ProgramDay> {
        val dayA = listOf("Back", "Chest", "Quadriceps", "Lateral Deltoid", "Hamstrings", "Core")
        val dayB = listOf("Back", "Chest", "Hamstrings", "Rear Deltoid", "Quadriceps", "Biceps", "Triceps")
        return listOf(
            buildDay(1, "Full Body A", dayA, exercises, baseReadinessScore, sessionLengthMinutes),
            buildDay(2, "Full Body B", dayB, exercises, baseReadinessScore, sessionLengthMinutes)
        )
    }

    private fun generateUpperLower(
        exercises: List<ExerciseEntity>,
        baseReadinessScore: Double,
        sessionLengthMinutes: Int
    ): List<ProgramDay> {
        // Upper A: Horizontal push/pull & Lat V-taper focus
        val upperA = listOf("Back", "Chest", "Lateral Deltoid", "Biceps", "Triceps")
        val lowerA = listOf("Quadriceps", "Hamstrings", "Glutes", "Calves")
        // Upper B: Vertical pull, Rear Delts & Upper Back focus
        val upperB = listOf("Back", "Lateral Deltoid", "Rear Deltoid", "Upper Back", "Chest", "Triceps")
        val lowerB = listOf("Hamstrings", "Quadriceps", "Glutes", "Core", "Calves")
        return listOf(
            buildDay(1, "Upper A", upperA, exercises, baseReadinessScore, sessionLengthMinutes),
            buildDay(2, "Lower A", lowerA, exercises, baseReadinessScore, sessionLengthMinutes),
            buildDay(3, "Upper B", upperB, exercises, baseReadinessScore, sessionLengthMinutes),
            buildDay(4, "Lower B", lowerB, exercises, baseReadinessScore, sessionLengthMinutes)
        )
    }

    private fun generateFullBody(
        exercises: List<ExerciseEntity>,
        baseReadinessScore: Double,
        sessionLengthMinutes: Int
    ): List<ProgramDay> {
        return listOf(
            buildDay(1, "Full Body A", listOf("Back", "Chest", "Quadriceps", "Lateral Deltoid", "Core"), exercises, baseReadinessScore, sessionLengthMinutes),
            buildDay(2, "Full Body B", listOf("Back", "Chest", "Hamstrings", "Rear Deltoid", "Biceps"), exercises, baseReadinessScore, sessionLengthMinutes),
            buildDay(3, "Full Body C", listOf("Back", "Chest", "Glutes", "Lateral Deltoid", "Triceps"), exercises, baseReadinessScore, sessionLengthMinutes)
        )
    }

    private fun generatePPLUpperLower(
        exercises: List<ExerciseEntity>,
        baseReadinessScore: Double,
        sessionLengthMinutes: Int
    ): List<ProgramDay> {
        return listOf(
            buildDay(1, "Push", listOf("Chest", "Lateral Deltoid", "Triceps"), exercises, baseReadinessScore, sessionLengthMinutes),
            buildDay(2, "Pull", listOf("Back", "Rear Deltoid", "Biceps"), exercises, baseReadinessScore, sessionLengthMinutes),
            buildDay(3, "Legs", listOf("Quadriceps", "Hamstrings", "Glutes", "Calves"), exercises, baseReadinessScore, sessionLengthMinutes),
            buildDay(4, "Upper", listOf("Back", "Chest", "Lateral Deltoid", "Rear Deltoid", "Biceps", "Triceps"), exercises, baseReadinessScore, sessionLengthMinutes),
            buildDay(5, "Lower", listOf("Quadriceps", "Hamstrings", "Glutes", "Core", "Calves"), exercises, baseReadinessScore, sessionLengthMinutes)
        )
    }

    private fun generatePPLDouble(
        exercises: List<ExerciseEntity>,
        baseReadinessScore: Double,
        sessionLengthMinutes: Int
    ): List<ProgramDay> {
        return listOf(
            buildDay(1, "Push", listOf("Chest", "Lateral Deltoid", "Triceps"), exercises, baseReadinessScore, sessionLengthMinutes),
            buildDay(2, "Pull", listOf("Back", "Rear Deltoid", "Biceps"), exercises, baseReadinessScore, sessionLengthMinutes),
            buildDay(3, "Legs", listOf("Quadriceps", "Hamstrings", "Glutes", "Calves"), exercises, baseReadinessScore, sessionLengthMinutes),
            buildDay(4, "Push", listOf("Chest", "Lateral Deltoid", "Triceps"), exercises, baseReadinessScore, sessionLengthMinutes),
            buildDay(5, "Pull", listOf("Back", "Rear Deltoid", "Biceps"), exercises, baseReadinessScore, sessionLengthMinutes),
            buildDay(6, "Legs", listOf("Quadriceps", "Hamstrings", "Glutes", "Calves"), exercises, baseReadinessScore, sessionLengthMinutes)
        )
    }

    private fun buildDay(
        dayNum: Int,
        name: String,
        muscles: List<String>,
        allExercises: List<ExerciseEntity>,
        baseReadinessScore: Double,
        sessionLengthMinutes: Int
    ): ProgramDay {
        val selected = mutableListOf<ProgramExercise>()
        val usedExerciseIds = mutableSetOf<Long>()

        // Priority ordering of muscles (V-taper critical muscles first)
        val prioritizedMuscles = muscles.sortedByDescending { musclePriorityScore(it) }

        val adjustedSets = when {
            baseReadinessScore < 2.5 -> maxOf(1, 3 - 1)
            baseReadinessScore >= 4.0 -> 4
            else -> 3
        }
        val adjustedRpe = when {
            baseReadinessScore < 2.5 -> maxOf(1.0, 7.5 - 0.5)
            baseReadinessScore >= 4.0 -> 8.0
            else -> 7.5
        }
        val restSeconds = 90
        val exerciseMinutes = estimateExerciseMinutes(adjustedSets, restSeconds)
        val maxDurationWithBuffer = sessionLengthMinutes + 5.0

        // Pass 1: Add 1 exercise for each muscle slot in priority order as long as time budget allows
        for (muscle in prioritizedMuscles) {
            val candidate = allExercises
                .filter { it.muscleGroup.equals(muscle, ignoreCase = true) || it.secondaryMuscles.contains(muscle, ignoreCase = true) }
                .filter { it.id !in usedExerciseIds }
                .sortedWith(
                    compareByDescending<ExerciseEntity> { relevantVtaperScore(it, muscle) }
                        .thenBy { difficultyOrder(it.difficulty) }
                )
                .firstOrNull()

            if (candidate != null) {
                val currentEst = WARMUP_COOLDOWN_BUFFER_MINUTES + (selected.size + 1) * exerciseMinutes
                if (selected.isEmpty() || currentEst <= maxDurationWithBuffer) {
                    usedExerciseIds.add(candidate.id)
                    selected.add(
                        ProgramExercise(
                            exerciseId = candidate.id,
                            exerciseName = candidate.name,
                            targetSets = adjustedSets,
                            targetRepsMin = 8,
                            targetRepsMax = 12,
                            targetRpe = adjustedRpe,
                            restSeconds = restSeconds
                        )
                    )
                }
            }
        }

        // Pass 2: If time budget remains, add a 2nd exercise for high priority V-taper slots
        for (muscle in prioritizedMuscles) {
            val currentEst = WARMUP_COOLDOWN_BUFFER_MINUTES + (selected.size + 1) * exerciseMinutes
            if (currentEst > maxDurationWithBuffer) break

            val candidate = allExercises
                .filter { it.muscleGroup.equals(muscle, ignoreCase = true) || it.secondaryMuscles.contains(muscle, ignoreCase = true) }
                .filter { it.id !in usedExerciseIds }
                .sortedWith(
                    compareByDescending<ExerciseEntity> { relevantVtaperScore(it, muscle) }
                        .thenBy { difficultyOrder(it.difficulty) }
                )
                .firstOrNull()

            if (candidate != null) {
                usedExerciseIds.add(candidate.id)
                selected.add(
                    ProgramExercise(
                        exerciseId = candidate.id,
                        exerciseName = candidate.name,
                        targetSets = adjustedSets,
                        targetRepsMin = 8,
                        targetRepsMax = 12,
                        targetRpe = adjustedRpe,
                        restSeconds = restSeconds
                    )
                )
            }
        }

        return ProgramDay(dayNum, name, muscles, selected)
    }

    private fun musclePriorityScore(muscle: String): Int = when {
        muscle.equals("Back", ignoreCase = true) -> 100
        muscle.equals("Lateral Deltoid", ignoreCase = true) -> 90
        muscle.equals("Chest", ignoreCase = true) -> 80
        muscle.equals("Rear Deltoid", ignoreCase = true) -> 70
        else -> 50
    }

    private fun relevantVtaperScore(exercise: ExerciseEntity, muscle: String): Int = when {
        muscle.equals("Back", ignoreCase = true) -> exercise.vtaperLat
        muscle.equals("Lateral Deltoid", ignoreCase = true) -> exercise.vtaperLateralDelt
        muscle.equals("Chest", ignoreCase = true) -> exercise.vtaperUpperChest
        muscle.equals("Rear Deltoid", ignoreCase = true) -> exercise.vtaperRearDelt
        // ponytail: For non-V-taper-critical muscles (e.g., Biceps, Triceps, Quads),
        // fall back to the exercise's aggregate V-taper relevance across all dimensions.
        // This surfaces exercises that contribute broadly to the V-taper aesthetic
        // even when the primary muscle slot isn't one of the four critical slots.
        else -> exercise.vtaperLat + exercise.vtaperLateralDelt + exercise.vtaperUpperChest + exercise.vtaperRearDelt
    }

    private fun difficultyOrder(difficulty: String): Int = when {
        difficulty.contains("Beginner", ignoreCase = true) -> 0
        difficulty.contains("Intermediate", ignoreCase = true) -> 1
        difficulty.contains("Advanced", ignoreCase = true) -> 2
        else -> 1
    }
}
