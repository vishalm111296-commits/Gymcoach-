package com.gymcoach.app.core.program

import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.core.exercise.EquipmentAvailability
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgramGenerator @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val equipmentAvailability: EquipmentAvailability
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
        experienceLevel: String,
        goal: String
    ): GeneratedProgram {
        val availableEquipment = equipmentAvailability.getAvailableEquipment(equipmentType)
        val allExercises = exerciseDao.getAll().first()
        val filteredExercises = filterByEquipment(allExercises, availableEquipment)
        val days = when (frequency) {
            3 -> generateFullBody(filteredExercises)
            4 -> generateUpperLower(filteredExercises)
            5 -> generatePPLUpperLower(filteredExercises)
            6 -> generatePPLDouble(filteredExercises)
            else -> generateUpperLower(filteredExercises)
        }
        return GeneratedProgram(
            name = "V-Taper $frequency-Day Program",
            description = "Personalized $frequency-day training program for $goal",
            goal = goal,
            frequency = frequency,
            days = days
        )
    }

    /**
     * Filters exercises by available equipment.
     * Bodyweight exercises are always available.
     * Compound equipment requirements (e.g. "dumbbell,bench") require all tokens.
     */
    private fun filterByEquipment(
        exercises: List<ExerciseEntity>,
        availableEquipment: Set<String>
    ): List<ExerciseEntity> {
        return exercises.filter { ex ->
            // Bodyweight exercises are always available
            if (ex.equipment == "bodyweight" || ex.equipment.isBlank()) return@filter true

            val equipmentTokens = ex.equipment.split(",").map { it.trim().lowercase() }
            if (equipmentTokens.size > 1) {
                // Compound equipment: all tokens must be available
                equipmentTokens.all { token ->
                    availableEquipment.contains(token) || token == "bodyweight"
                }
            } else {
                // Single equipment: check availability
                availableEquipment.contains(ex.equipment.lowercase())
            }
        }
    }

    private fun generateUpperLower(exercises: List<ExerciseEntity>): List<ProgramDay> {
        val upperA = listOf("Back", "Chest", "Lateral Deltoid", "Rear Deltoid", "Biceps", "Triceps")
        val lowerA = listOf("Quadriceps", "Hamstrings", "Glutes", "Calves")
        val upperB = listOf("Back", "Chest", "Lateral Deltoid", "Rear Deltoid", "Biceps", "Triceps")
        val lowerB = listOf("Hamstrings", "Quadriceps", "Glutes", "Core", "Calves")
        return listOf(
            buildDay(1, "Upper A", upperA, exercises),
            buildDay(2, "Lower A", lowerA, exercises),
            buildDay(3, "Upper B", upperB, exercises),
            buildDay(4, "Lower B", lowerB, exercises)
        )
    }

    private fun generateFullBody(exercises: List<ExerciseEntity>): List<ProgramDay> {
        return listOf(
            buildDay(1, "Full Body A", listOf("Back", "Chest", "Quadriceps", "Lateral Deltoid", "Core"), exercises),
            buildDay(2, "Full Body B", listOf("Back", "Chest", "Hamstrings", "Rear Deltoid", "Biceps"), exercises),
            buildDay(3, "Full Body C", listOf("Back", "Chest", "Glutes", "Lateral Deltoid", "Triceps"), exercises)
        )
    }

    private fun generatePPLUpperLower(exercises: List<ExerciseEntity>): List<ProgramDay> {
        return listOf(
            buildDay(1, "Push", listOf("Chest", "Lateral Deltoid", "Triceps"), exercises),
            buildDay(2, "Pull", listOf("Back", "Rear Deltoid", "Biceps"), exercises),
            buildDay(3, "Legs", listOf("Quadriceps", "Hamstrings", "Glutes", "Calves"), exercises),
            buildDay(4, "Upper", listOf("Back", "Chest", "Lateral Deltoid", "Rear Deltoid", "Biceps", "Triceps"), exercises),
            buildDay(5, "Lower", listOf("Quadriceps", "Hamstrings", "Glutes", "Core", "Calves"), exercises)
        )
    }

    private fun generatePPLDouble(exercises: List<ExerciseEntity>): List<ProgramDay> {
        return listOf(
            buildDay(1, "Push", listOf("Chest", "Lateral Deltoid", "Triceps"), exercises),
            buildDay(2, "Pull", listOf("Back", "Rear Deltoid", "Biceps"), exercises),
            buildDay(3, "Legs", listOf("Quadriceps", "Hamstrings", "Glutes", "Calves"), exercises),
            buildDay(4, "Push", listOf("Chest", "Lateral Deltoid", "Triceps"), exercises),
            buildDay(5, "Pull", listOf("Back", "Rear Deltoid", "Biceps"), exercises),
            buildDay(6, "Legs", listOf("Quadriceps", "Hamstrings", "Glutes", "Calves"), exercises)
        )
    }

    private fun buildDay(
        dayNum: Int,
        name: String,
        muscles: List<String>,
        allExercises: List<ExerciseEntity>
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
                    selected.add(ProgramExercise(
                        exerciseId = ex.id,
                        exerciseName = ex.name,
                        targetSets = 3,
                        targetRepsMin = 8,
                        targetRepsMax = 12,
                        targetRpe = 7.5,
                        restSeconds = 90
                    ))
                }
            }
        }

        return ProgramDay(dayNum, name, muscles, selected)
    }

    /**
     * V-taper relevance of an exercise *for a specific muscle slot*.
     *
     * Ranking per slot (instead of summing all four V-taper scores) prevents a
     * single high-lat back exercise from outranking genuine chest/delt builders
     * when filling those slots — each slot competes on its own relevant axis.
     */
    private fun relevantVtaperScore(exercise: ExerciseEntity, muscle: String): Int = when {
        muscle.equals("Back", ignoreCase = true) -> exercise.vtaperLat
        muscle.equals("Lateral Deltoid", ignoreCase = true) -> exercise.vtaperLateralDelt
        muscle.equals("Chest", ignoreCase = true) -> exercise.vtaperUpperChest
        muscle.equals("Rear Deltoid", ignoreCase = true) -> exercise.vtaperRearDelt
        else -> 0
    }

    /** Sort difficulty: Beginner < Intermediate < Advanced */
    private fun difficultyOrder(difficulty: String): Int = when {
        difficulty.contains("Beginner", ignoreCase = true) -> 0
        difficulty.contains("Intermediate", ignoreCase = true) -> 1
        difficulty.contains("Advanced", ignoreCase = true) -> 2
        else -> 1
    }
}
