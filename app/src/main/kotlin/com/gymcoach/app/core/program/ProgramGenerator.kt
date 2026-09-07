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

    suspend fun generateProgram(
        frequency: Int,
        equipmentType: String,
        goal: String
    ): GeneratedProgram {
        val latestReadiness = readinessRepository.getLatestReadiness().first()
        val availableEquipment = equipmentAvailability.getAvailableEquipment(equipmentType)
        val allExercises = exerciseDao.getAll().first()
        val filteredExercises = filterByEquipment(allExercises, availableEquipment)

        val baseReadinessScore = latestReadiness?.readinessScore ?: 3.0
        val days = when (frequency) {
            3 -> generateFullBody(filteredExercises, baseReadinessScore)
            4 -> generateUpperLower(filteredExercises, baseReadinessScore)
            5 -> generatePPLUpperLower(filteredExercises, baseReadinessScore)
            6 -> generatePPLDouble(filteredExercises, baseReadinessScore)
            else -> generateUpperLower(filteredExercises, baseReadinessScore)
        }
        return GeneratedProgram(
            name = "V-Taper $frequency-Day Program",
            description = "Personalized $frequency-day training program for $goal. Adjusted for readiness score: ${"%.1f".format(baseReadinessScore)}",
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

    private fun generateUpperLower(exercises: List<ExerciseEntity>, baseReadinessScore: Double): List<ProgramDay> {
        val upperA = listOf("Back", "Chest", "Lateral Deltoid", "Rear Deltoid", "Biceps", "Triceps")
        val lowerA = listOf("Quadriceps", "Hamstrings", "Glutes", "Calves")
        val upperB = listOf("Back", "Chest", "Lateral Deltoid", "Rear Deltoid", "Biceps", "Triceps")
        val lowerB = listOf("Hamstrings", "Quadriceps", "Glutes", "Core", "Calves")
        return listOf(
            buildDay(1, "Upper A", upperA, exercises, baseReadinessScore),
            buildDay(2, "Lower A", lowerA, exercises, baseReadinessScore),
            buildDay(3, "Upper B", upperB, exercises, baseReadinessScore),
            buildDay(4, "Lower B", lowerB, exercises, baseReadinessScore)
        )
    }

    private fun generateFullBody(exercises: List<ExerciseEntity>, baseReadinessScore: Double): List<ProgramDay> {
        return listOf(
            buildDay(1, "Full Body A", listOf("Back", "Chest", "Quadriceps", "Lateral Deltoid", "Core"), exercises, baseReadinessScore),
            buildDay(2, "Full Body B", listOf("Back", "Chest", "Hamstrings", "Rear Deltoid", "Biceps"), exercises, baseReadinessScore),
            buildDay(3, "Full Body C", listOf("Back", "Chest", "Glutes", "Lateral Deltoid", "Triceps"), exercises, baseReadinessScore)
        )
    }

    private fun generatePPLUpperLower(exercises: List<ExerciseEntity>, baseReadinessScore: Double): List<ProgramDay> {
        return listOf(
            buildDay(1, "Push", listOf("Chest", "Lateral Deltoid", "Triceps"), exercises, baseReadinessScore),
            buildDay(2, "Pull", listOf("Back", "Rear Deltoid", "Biceps"), exercises, baseReadinessScore),
            buildDay(3, "Legs", listOf("Quadriceps", "Hamstrings", "Glutes", "Calves"), exercises, baseReadinessScore),
            buildDay(4, "Upper", listOf("Back", "Chest", "Lateral Deltoid", "Rear Deltoid", "Biceps", "Triceps"), exercises, baseReadinessScore),
            buildDay(5, "Lower", listOf("Quadriceps", "Hamstrings", "Glutes", "Core", "Calves"), exercises, baseReadinessScore)
        )
    }

    private fun generatePPLDouble(exercises: List<ExerciseEntity>, baseReadinessScore: Double): List<ProgramDay> {
        return listOf(
            buildDay(1, "Push", listOf("Chest", "Lateral Deltoid", "Triceps"), exercises, baseReadinessScore),
            buildDay(2, "Pull", listOf("Back", "Rear Deltoid", "Biceps"), exercises, baseReadinessScore),
            buildDay(3, "Legs", listOf("Quadriceps", "Hamstrings", "Glutes", "Calves"), exercises, baseReadinessScore),
            buildDay(4, "Push", listOf("Chest", "Lateral Deltoid", "Triceps"), exercises, baseReadinessScore),
            buildDay(5, "Pull", listOf("Back", "Rear Deltoid", "Biceps"), exercises, baseReadinessScore),
            buildDay(6, "Legs", listOf("Quadriceps", "Hamstrings", "Glutes", "Calves"), exercises, baseReadinessScore)
        )
    }

    private fun buildDay(
        dayNum: Int,
        name: String,
        muscles: List<String>,
        allExercises: List<ExerciseEntity>,
        baseReadinessScore: Double
    ): ProgramDay {
        val selected = mutableListOf<ProgramExercise>()
        val usedExerciseIds = mutableSetOf<Long>()

        for (muscle in muscles) {
            val candidates = allExercises
                .filter { it.muscleGroup.equals(muscle, ignoreCase = true) || it.secondaryMuscles.contains(muscle, ignoreCase = true) }
                .filter { it.id !in usedExerciseIds }
                .sortedWith(
                    compareByDescending<ExerciseEntity> { relevantVtaperScore(it, muscle) }
                        .thenBy { difficultyOrder(it.difficulty) }
                )
                .take(2)

            for (ex in candidates) {
                if (ex.id !in usedExerciseIds) {
                    usedExerciseIds.add(ex.id)
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
                    selected.add(ProgramExercise(
                        exerciseId = ex.id,
                        exerciseName = ex.name,
                        targetSets = adjustedSets,
                        targetRepsMin = 8,
                        targetRepsMax = 12,
                        targetRpe = adjustedRpe,
                        restSeconds = 90
                    ))
                }
            }
        }

        return ProgramDay(dayNum, name, muscles, selected)
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
