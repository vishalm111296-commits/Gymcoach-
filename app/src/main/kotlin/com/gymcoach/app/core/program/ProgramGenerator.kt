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
                .filter { matchesMuscleSlot(it, muscle) }
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

    private fun normalizeToken(token: String): String =
        token.replace("[^a-zA-Z0-9]".toRegex(), "").lowercase()

    /**
     * Normalized matching between requested program muscle slots (e.g. "Lateral Deltoid",
     * "Rear Deltoid", "Quadriceps") and seeded exercise metadata.
     */
    private fun matchesMuscleSlot(exercise: ExerciseEntity, slot: String): Boolean {
        val normSlot = normalizeToken(slot)
        val normMg = normalizeToken(exercise.muscleGroup)
        val normSec = normalizeToken(exercise.secondaryMuscles)
        val normName = normalizeToken(exercise.name)

        if (normMg == normSlot) return true
        if (normSec.contains(normSlot)) return true

        return when (normSlot) {
            "lateraldeltoid", "lateraldelt", "sidedelt" -> {
                exercise.vtaperLateralDelt > 0 || normSec.contains("lateraldeltoid") ||
                    (normName.contains("lateral") && normMg == "shoulders") ||
                    (normSec.contains("lateral") && normMg == "shoulders")
            }
            "reardeltoid", "reardelt" -> {
                exercise.vtaperRearDelt > 0 || normSec.contains("reardeltoid") ||
                    (normName.contains("rear") && (normMg == "shoulders" || normMg == "back")) ||
                    (normName.contains("reverse") && (normMg == "shoulders" || normMg == "back"))
            }
            "quadriceps", "quads" -> {
                normSec.contains("quadriceps") || normSec.contains("quads") ||
                    (normMg == "legs" && listOf("squat", "lunge", "stepup", "legextension", "quad").any { normName.contains(it) })
            }
            "hamstrings" -> {
                normSec.contains("hamstrings") ||
                    (normMg == "legs" && listOf("rdl", "deadlift", "legcurl", "goodmorning", "hamstring").any { normName.contains(it) })
            }
            "glutes" -> {
                normSec.contains("glutes") ||
                    (normMg == "legs" && listOf("hipthrust", "glutebridge", "kickback", "glute").any { normName.contains(it) })
            }
            "calves" -> {
                normSec.contains("calves") || (normMg == "legs" && normName.contains("calf"))
            }
            "back" -> {
                normMg == "back" || normSec.contains("latissimus") || normSec.contains("upperback") || normSec.contains("lowerback")
            }
            "chest" -> {
                normMg == "chest" || normSec.contains("upperchest") || normSec.contains("lowerchest") || normSec.contains("midchest")
            }
            "biceps" -> {
                (normMg == "arms" && (listOf("biceps", "bicep", "curl").any { normName.contains(it) } || normSec.contains("biceps"))) || normSec.contains("biceps")
            }
            "triceps" -> {
                (normMg == "arms" && (listOf("triceps", "tricep", "dip", "extension", "kickback").any { normName.contains(it) } || normSec.contains("triceps"))) || normSec.contains("triceps")
            }
            "core" -> {
                normMg == "core" || normSec.contains("abs") || normSec.contains("obliques") || normSec.contains("deepcore")
            }
            else -> false
        }
    }

    private fun relevantVtaperScore(exercise: ExerciseEntity, muscle: String): Int {
        val normSlot = normalizeToken(muscle)
        return when {
            normSlot == "back" || normSlot == "lat" -> exercise.vtaperLat
            normSlot == "lateraldeltoid" || normSlot == "lateraldelt" || normSlot == "sidedelt" -> exercise.vtaperLateralDelt
            normSlot == "chest" || normSlot == "upperchest" -> exercise.vtaperUpperChest
            normSlot == "reardeltoid" || normSlot == "reardelt" -> exercise.vtaperRearDelt
            else -> exercise.vtaperLat + exercise.vtaperLateralDelt + exercise.vtaperUpperChest + exercise.vtaperRearDelt
        }
    }

    private fun difficultyOrder(difficulty: String): Int = when {
        difficulty.contains("Beginner", ignoreCase = true) -> 0
        difficulty.contains("Intermediate", ignoreCase = true) -> 1
        difficulty.contains("Advanced", ignoreCase = true) -> 2
        else -> 1
    }
}
