package com.gymcoach.app.core.program

import com.gymcoach.app.core.exercise.EquipmentAvailability
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.domain.repository.ReadinessRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates constrained, repeatable hypertrophy programs rather than selecting
 * the top two exercises for every muscle slot.
 *
 * Design rules:
 * - V-taper priorities (lats, lateral delts, rear delts, upper chest) receive
 *   more attention than small supporting muscles.
 * - Each muscle is exposed repeatedly across the week where the split permits.
 * - Sessions are capped at seven exercises to keep the plan executable.
 * - Exercise metadata supplies rep ranges and rest when available.
 * - Readiness changes set count conservatively; it never turns every exercise
 *   into a four-set exercise.
 * - Equipment filtering is strict for compound requirements.
 */
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

    private data class SlotSpec(
        val muscle: String,
        val sets: Int,
        val priority: Int
    )

    suspend fun generateProgram(
        frequency: Int,
        equipmentType: String,
        goal: String
    ): GeneratedProgram {
        val latestReadiness = readinessRepository.getLatestReadiness().first()
        val availableEquipment = equipmentAvailability.getAvailableEquipment(equipmentType)
        val allExercises = exerciseDao.getAll().first()
        val filteredExercises = filterByEquipment(allExercises, availableEquipment)
        val readiness = latestReadiness?.readinessScore ?: 3.0

        val days = when (frequency) {
            3 -> generateFullBody(filteredExercises, readiness)
            4 -> generateUpperLower(filteredExercises, readiness)
            5 -> generatePPLUpperLower(filteredExercises, readiness)
            6 -> generatePPLDouble(filteredExercises, readiness)
            else -> generateUpperLower(filteredExercises, readiness)
        }

        return GeneratedProgram(
            name = "V-Taper $frequency-Day Program",
            description = "Personalized $frequency-day training program for $goal. Readiness-adjusted volume: ${"%.1f".format(readiness)}.",
            goal = goal,
            frequency = frequency,
            days = days
        )
    }

    private fun filterByEquipment(
        exercises: List<ExerciseEntity>,
        availableEquipment: Set<String>
    ): List<ExerciseEntity> = exercises.filter { ex ->
        val equipmentTokens = ex.equipment
            .replace("+", ",")
            .split(",")
            .map { it.trim().lowercase().replace("_", " ") }
            .filter { it.isNotBlank() }

        if (equipmentTokens.isEmpty() || equipmentTokens == listOf("bodyweight")) {
            true
        } else {
            equipmentTokens.all { it == "bodyweight" || availableEquipment.contains(it) }
        }
    }

    private fun generateUpperLower(exercises: List<ExerciseEntity>, readiness: Double): List<ProgramDay> = listOf(
        buildDay(1, "Upper A", listOf(
            SlotSpec("Back", 4, 4), SlotSpec("Chest", 3, 3),
            SlotSpec("Lateral Deltoid", 3, 4), SlotSpec("Rear Deltoid", 2, 3),
            SlotSpec("Biceps", 2, 1), SlotSpec("Triceps", 2, 1)
        ), exercises, readiness),
        buildDay(2, "Lower A", listOf(
            SlotSpec("Quadriceps", 3, 2), SlotSpec("Hamstrings", 3, 2),
            SlotSpec("Glutes", 3, 2), SlotSpec("Calves", 2, 1)
        ), exercises, readiness),
        buildDay(3, "Upper B", listOf(
            SlotSpec("Back", 4, 4), SlotSpec("Chest", 3, 3),
            SlotSpec("Lateral Deltoid", 3, 4), SlotSpec("Rear Deltoid", 2, 3),
            SlotSpec("Biceps", 2, 1), SlotSpec("Triceps", 2, 1)
        ), exercises, readiness),
        buildDay(4, "Lower B", listOf(
            SlotSpec("Hamstrings", 3, 2), SlotSpec("Quadriceps", 3, 2),
            SlotSpec("Glutes", 3, 2), SlotSpec("Core", 2, 1), SlotSpec("Calves", 2, 1)
        ), exercises, readiness)
    )

    private fun generateFullBody(exercises: List<ExerciseEntity>, readiness: Double): List<ProgramDay> = listOf(
        buildDay(1, "Full Body A", listOf(
            SlotSpec("Back", 3, 4), SlotSpec("Chest", 3, 3), SlotSpec("Quadriceps", 3, 2),
            SlotSpec("Lateral Deltoid", 3, 4), SlotSpec("Core", 2, 1)
        ), exercises, readiness),
        buildDay(2, "Full Body B", listOf(
            SlotSpec("Back", 3, 4), SlotSpec("Chest", 3, 3), SlotSpec("Hamstrings", 3, 2),
            SlotSpec("Rear Deltoid", 3, 3), SlotSpec("Biceps", 2, 1)
        ), exercises, readiness),
        buildDay(3, "Full Body C", listOf(
            SlotSpec("Back", 3, 4), SlotSpec("Chest", 3, 3), SlotSpec("Glutes", 3, 2),
            SlotSpec("Lateral Deltoid", 3, 4), SlotSpec("Triceps", 2, 1)
        ), exercises, readiness)
    )

    private fun generatePPLUpperLower(exercises: List<ExerciseEntity>, readiness: Double): List<ProgramDay> = listOf(
        buildDay(1, "Push", listOf(
            SlotSpec("Chest", 4, 3), SlotSpec("Lateral Deltoid", 4, 4), SlotSpec("Triceps", 3, 1)
        ), exercises, readiness),
        buildDay(2, "Pull", listOf(
            SlotSpec("Back", 5, 4), SlotSpec("Rear Deltoid", 3, 3), SlotSpec("Biceps", 3, 1)
        ), exercises, readiness),
        buildDay(3, "Legs", listOf(
            SlotSpec("Quadriceps", 4, 2), SlotSpec("Hamstrings", 4, 2),
            SlotSpec("Glutes", 3, 2), SlotSpec("Calves", 2, 1)
        ), exercises, readiness),
        buildDay(4, "Upper", listOf(
            SlotSpec("Back", 4, 4), SlotSpec("Chest", 3, 3), SlotSpec("Lateral Deltoid", 3, 4),
            SlotSpec("Rear Deltoid", 2, 3), SlotSpec("Biceps", 2, 1), SlotSpec("Triceps", 2, 1)
        ), exercises, readiness),
        buildDay(5, "Lower", listOf(
            SlotSpec("Quadriceps", 3, 2), SlotSpec("Hamstrings", 3, 2),
            SlotSpec("Glutes", 3, 2), SlotSpec("Core", 2, 1), SlotSpec("Calves", 2, 1)
        ), exercises, readiness)
    )

    private fun generatePPLDouble(exercises: List<ExerciseEntity>, readiness: Double): List<ProgramDay> = listOf(
        buildDay(1, "Push A", listOf(SlotSpec("Chest", 4, 3), SlotSpec("Lateral Deltoid", 4, 4), SlotSpec("Triceps", 3, 1)), exercises, readiness),
        buildDay(2, "Pull A", listOf(SlotSpec("Back", 5, 4), SlotSpec("Rear Deltoid", 3, 3), SlotSpec("Biceps", 3, 1)), exercises, readiness),
        buildDay(3, "Legs A", listOf(SlotSpec("Quadriceps", 4, 2), SlotSpec("Hamstrings", 4, 2), SlotSpec("Glutes", 3, 2), SlotSpec("Calves", 2, 1)), exercises, readiness),
        buildDay(4, "Push B", listOf(SlotSpec("Chest", 3, 3), SlotSpec("Lateral Deltoid", 4, 4), SlotSpec("Triceps", 3, 1)), exercises, readiness),
        buildDay(5, "Pull B", listOf(SlotSpec("Back", 4, 4), SlotSpec("Rear Deltoid", 3, 3), SlotSpec("Biceps", 3, 1)), exercises, readiness),
        buildDay(6, "Legs B", listOf(SlotSpec("Quadriceps", 3, 2), SlotSpec("Hamstrings", 3, 2), SlotSpec("Glutes", 3, 2), SlotSpec("Core", 2, 1), SlotSpec("Calves", 2, 1)), exercises, readiness)
    )

    private fun buildDay(
        dayNum: Int,
        name: String,
        slots: List<SlotSpec>,
        allExercises: List<ExerciseEntity>,
        readiness: Double
    ): ProgramDay {
        val selected = mutableListOf<ProgramExercise>()
        val usedExerciseIds = mutableSetOf<Long>()
        val usedMovementPatterns = mutableSetOf<String>()

        // First pass guarantees one sensible exercise for each requested muscle.
        slots.sortedByDescending { it.priority }.forEach { slot ->
            if (selected.size >= MAX_EXERCISES_PER_SESSION) return@forEach
            chooseCandidate(slot.muscle, allExercises, usedExerciseIds, usedMovementPatterns)
                ?.let { exercise ->
                    usedExerciseIds += exercise.id
                    if (exercise.movementPattern.isNotBlank()) usedMovementPatterns += exercise.movementPattern.lowercase()
                    selected += toProgramExercise(exercise, slot.sets, readiness)
                }
        }

        // Only high-priority V-taper slots receive a second movement, and only
        // when that adds a distinct pattern. This prevents the old "two per
        // muscle" explosion while still allocating more work where the goal is.
        slots.filter { it.priority >= 4 }
            .sortedByDescending { it.priority }
            .forEach { slot ->
                if (selected.size >= MAX_EXERCISES_PER_SESSION) return@forEach
                chooseCandidate(slot.muscle, allExercises, usedExerciseIds, usedMovementPatterns)
                    ?.let { exercise ->
                        usedExerciseIds += exercise.id
                        if (exercise.movementPattern.isNotBlank()) usedMovementPatterns += exercise.movementPattern.lowercase()
                        selected += toProgramExercise(exercise, maxOf(2, slot.sets - 1), readiness)
                    }
            }

        return ProgramDay(
            dayNumber = dayNum,
            name = name,
            targetMuscles = slots.map { it.muscle },
            exercises = selected
        )
    }

    private fun chooseCandidate(
        muscle: String,
        exercises: List<ExerciseEntity>,
        usedIds: Set<Long>,
        usedMovementPatterns: Set<String>
    ): ExerciseEntity? {
        val candidates = exercises
            .asSequence()
            .filter { it.id !in usedIds }
            .filter { it.muscleGroup.equals(muscle, ignoreCase = true) || containsMuscle(it.secondaryMuscles, muscle) }
            .sortedWith(
                compareByDescending<ExerciseEntity> { if (it.muscleGroup.equals(muscle, ignoreCase = true)) 1 else 0 }
                    .thenByDescending { relevantVtaperScore(it, muscle) }
                    .thenBy { if (it.movementPattern.isBlank() || it.movementPattern.lowercase() !in usedMovementPatterns) 0 else 1 }
                    .thenBy { difficultyOrder(it.difficulty) }
                    .thenBy { it.name }
            )
            .toList()

        return candidates.firstOrNull { candidate ->
            candidate.movementPattern.isBlank() || candidate.movementPattern.lowercase() !in usedMovementPatterns
        } ?: candidates.firstOrNull()
    }

    private fun containsMuscle(secondaryMuscles: String, muscle: String): Boolean =
        secondaryMuscles.split(",", "/", ";").any { it.trim().equals(muscle, ignoreCase = true) }

    private fun toProgramExercise(
        exercise: ExerciseEntity,
        baseSets: Int,
        readiness: Double
    ): ProgramExercise {
        val readinessSetAdjustment = when {
            readiness < 2.5 -> -1
            readiness >= 4.0 -> 0
            else -> 0
        }
        val targetSets = (baseSets + readinessSetAdjustment).coerceIn(1, 4)
        val (repMin, repMax) = parseRepRange(exercise.recommendedRepRange)
        val restSeconds = parseRestSeconds(exercise.recommendedRestTime)
        val targetRpe = when {
            readiness < 2.5 -> 7.0
            readiness >= 4.0 -> 8.0
            else -> 7.5
        }
        return ProgramExercise(
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            targetSets = targetSets,
            targetRepsMin = repMin,
            targetRepsMax = repMax,
            targetRpe = targetRpe,
            restSeconds = restSeconds
        )
    }

    private fun parseRepRange(value: String): Pair<Int, Int> {
        val match = REP_RANGE_REGEX.find(value)
        return if (match != null) {
            val min = match.groupValues[1].toIntOrNull()?.coerceIn(1, 30) ?: DEFAULT_REP_MIN
            val max = match.groupValues[2].toIntOrNull()?.coerceIn(min, 40) ?: DEFAULT_REP_MAX
            min to max
        } else {
            DEFAULT_REP_MIN to DEFAULT_REP_MAX
        }
    }

    private fun parseRestSeconds(value: String): Int {
        val match = REST_REGEX.find(value.lowercase()) ?: return DEFAULT_REST_SECONDS
        val amount = match.groupValues[1].toIntOrNull() ?: return DEFAULT_REST_SECONDS
        return when {
            value.lowercase().contains("min") -> (amount * 60).coerceIn(30, 300)
            else -> amount.coerceIn(30, 300)
        }
    }

    private fun relevantVtaperScore(exercise: ExerciseEntity, muscle: String): Int = when {
        muscle.equals("Back", ignoreCase = true) -> exercise.vtaperLat
        muscle.equals("Lateral Deltoid", ignoreCase = true) -> exercise.vtaperLateralDelt
        muscle.equals("Chest", ignoreCase = true) -> exercise.vtaperUpperChest
        muscle.equals("Rear Deltoid", ignoreCase = true) -> exercise.vtaperRearDelt
        else -> exercise.vtaperLat + exercise.vtaperLateralDelt + exercise.vtaperUpperChest + exercise.vtaperRearDelt
    }

    private fun difficultyOrder(difficulty: String): Int = when {
        difficulty.contains("Beginner", ignoreCase = true) -> 0
        difficulty.contains("Intermediate", ignoreCase = true) -> 1
        difficulty.contains("Advanced", ignoreCase = true) -> 2
        else -> 1
    }

    private companion object {
        const val MAX_EXERCISES_PER_SESSION = 7
        const val DEFAULT_REP_MIN = 8
        const val DEFAULT_REP_MAX = 12
        const val DEFAULT_REST_SECONDS = 90
        val REP_RANGE_REGEX = Regex("(\\d+)\\s*[-–]\\s*(\\d+)")
        val REST_REGEX = Regex("(\\d+)")
    }
}
