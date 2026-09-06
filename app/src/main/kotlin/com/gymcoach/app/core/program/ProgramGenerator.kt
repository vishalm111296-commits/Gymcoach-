package com.gymcoach.app.core.program

import com.gymcoach.app.core.exercise.EquipmentAvailability
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
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

    /** Ranking boost when the slot's key taxonomy id is a PRIMARY muscle of the exercise. */
    private val PRIMARY_BOOST = 10

    /** Ranking boost when the slot's key taxonomy id only appears in SECONDARY muscles. */
    private val SECONDARY_BOOST = 4

    /** Taxonomy ids that count toward the "Back" slot. */
    private val BACK_MUSCLE_IDS = setOf("latissimus_dorsi", "upper_back", "rear_deltoid", "lower_back")

    /** Taxonomy ids that count toward the "Chest" slot. */
    private val CHEST_MUSCLE_IDS = setOf("upper_chest", "mid_chest", "lower_chest")

    /** Taxonomy ids that count toward the "Core" slot. */
    private val CORE_MUSCLE_IDS = setOf("abs", "obliques", "deep_core")

    /**
     * Key taxonomy id per muscle slot, used for the primary/secondary ranking boost.
     * Back / Lateral Deltoid / Rear Deltoid are intentionally ABSENT: the V-taper
     * relevance scores already cover those three slots, per the fix spec ("leave
     * Back/Lateral/Rear as-is since vtaper covers them").
     */
    private val SLOT_KEY_MUSCLE_IDS = mapOf(
        "Chest" to "mid_chest",
        "Biceps" to "biceps",
        "Triceps" to "triceps",
        "Quadriceps" to "quadriceps",
        "Hamstrings" to "hamstrings",
        "Glutes" to "glutes",
        "Calves" to "calves",
        "Core" to "abs"
    )

    suspend fun generateProgram(
        frequency: Int,
        equipmentType: String,
        goal: String
    ): GeneratedProgram {
        val latestReadiness = readinessRepository.getLatestReadiness().first()
        val availableEquipment = equipmentAvailability.getAvailableEquipment(equipmentType)
        val allExercises = exerciseDao.getAll().first()
        // Authoritative primary-muscle assignments from the seeded `exercise_muscles`
        // relation (role='primary'), keyed by exercise id. Consulted by matchesMuscle
        // and relevantVtaperScore so primary muscles (e.g. biceps on a curl, hamstrings
        // on a leg curl) attribute to the correct slot instead of being guessed from the
        // display-name slot vs the comma-joined taxonomy-id secondaryMuscles string.
        val primaryMusclesByExercise = exerciseDao.getPrimaryMusclesByExercise()
            .groupBy({ it.exerciseId }, { it.muscleName })
        val filteredExercises = filterByEquipment(allExercises, availableEquipment)

        val baseReadinessScore = latestReadiness?.readinessScore ?: 3.0
        val days = when (frequency) {
            3 -> generateFullBody(filteredExercises, baseReadinessScore, primaryMusclesByExercise)
            4 -> generateUpperLower(filteredExercises, baseReadinessScore, primaryMusclesByExercise)
            5 -> generatePPLUpperLower(filteredExercises, baseReadinessScore, primaryMusclesByExercise)
            6 -> generatePPLDouble(filteredExercises, baseReadinessScore, primaryMusclesByExercise)
            else -> generateUpperLower(filteredExercises, baseReadinessScore, primaryMusclesByExercise)
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

    private fun generateUpperLower(
        exercises: List<ExerciseEntity>,
        baseReadinessScore: Double,
        primaryMusclesByExercise: Map<Long, List<String>>
    ): List<ProgramDay> {
        val upperA = listOf("Back", "Chest", "Lateral Deltoid", "Rear Deltoid", "Biceps", "Triceps")
        val lowerA = listOf("Quadriceps", "Hamstrings", "Glutes", "Calves")
        val upperB = listOf("Back", "Chest", "Lateral Deltoid", "Rear Deltoid", "Biceps", "Triceps")
        val lowerB = listOf("Hamstrings", "Quadriceps", "Glutes", "Core", "Calves")
        return listOf(
            buildDay(1, "Upper A", upperA, exercises, baseReadinessScore, primaryMusclesByExercise),
            buildDay(2, "Lower A", lowerA, exercises, baseReadinessScore, primaryMusclesByExercise),
            buildDay(3, "Upper B", upperB, exercises, baseReadinessScore, primaryMusclesByExercise),
            buildDay(4, "Lower B", lowerB, exercises, baseReadinessScore, primaryMusclesByExercise)
        )
    }

    private fun generateFullBody(
        exercises: List<ExerciseEntity>,
        baseReadinessScore: Double,
        primaryMusclesByExercise: Map<Long, List<String>>
    ): List<ProgramDay> {
        return listOf(
            buildDay(1, "Full Body A", listOf("Back", "Chest", "Quadriceps", "Lateral Deltoid", "Core"), exercises, baseReadinessScore, primaryMusclesByExercise),
            buildDay(2, "Full Body B", listOf("Back", "Chest", "Hamstrings", "Rear Deltoid", "Biceps"), exercises, baseReadinessScore, primaryMusclesByExercise),
            buildDay(3, "Full Body C", listOf("Back", "Chest", "Glutes", "Lateral Deltoid", "Triceps"), exercises, baseReadinessScore, primaryMusclesByExercise)
        )
    }

    private fun generatePPLUpperLower(
        exercises: List<ExerciseEntity>,
        baseReadinessScore: Double,
        primaryMusclesByExercise: Map<Long, List<String>>
    ): List<ProgramDay> {
        return listOf(
            buildDay(1, "Push", listOf("Chest", "Lateral Deltoid", "Triceps"), exercises, baseReadinessScore, primaryMusclesByExercise),
            buildDay(2, "Pull", listOf("Back", "Rear Deltoid", "Biceps"), exercises, baseReadinessScore, primaryMusclesByExercise),
            buildDay(3, "Legs", listOf("Quadriceps", "Hamstrings", "Glutes", "Calves"), exercises, baseReadinessScore, primaryMusclesByExercise),
            buildDay(4, "Upper", listOf("Back", "Chest", "Lateral Deltoid", "Rear Deltoid", "Biceps", "Triceps"), exercises, baseReadinessScore, primaryMusclesByExercise),
            buildDay(5, "Lower", listOf("Quadriceps", "Hamstrings", "Glutes", "Core", "Calves"), exercises, baseReadinessScore, primaryMusclesByExercise)
        )
    }

    private fun generatePPLDouble(
        exercises: List<ExerciseEntity>,
        baseReadinessScore: Double,
        primaryMusclesByExercise: Map<Long, List<String>>
    ): List<ProgramDay> {
        return listOf(
            buildDay(1, "Push", listOf("Chest", "Lateral Deltoid", "Triceps"), exercises, baseReadinessScore, primaryMusclesByExercise),
            buildDay(2, "Pull", listOf("Back", "Rear Deltoid", "Biceps"), exercises, baseReadinessScore, primaryMusclesByExercise),
            buildDay(3, "Legs", listOf("Quadriceps", "Hamstrings", "Glutes", "Calves"), exercises, baseReadinessScore, primaryMusclesByExercise),
            buildDay(4, "Push", listOf("Chest", "Lateral Deltoid", "Triceps"), exercises, baseReadinessScore, primaryMusclesByExercise),
            buildDay(5, "Pull", listOf("Back", "Rear Deltoid", "Biceps"), exercises, baseReadinessScore, primaryMusclesByExercise),
            buildDay(6, "Legs", listOf("Quadriceps", "Hamstrings", "Glutes", "Calves"), exercises, baseReadinessScore, primaryMusclesByExercise)
        )
    }

    private fun buildDay(
        dayNum: Int,
        name: String,
        muscles: List<String>,
        allExercises: List<ExerciseEntity>,
        baseReadinessScore: Double,
        primaryMusclesByExercise: Map<Long, List<String>>
    ): ProgramDay {
        val selected = mutableListOf<ProgramExercise>()
        val usedExerciseIds = mutableSetOf<Long>()

        for (muscle in muscles) {
            val candidates = allExercises
                .filter { matchesMuscle(it, muscle, primaryMusclesByExercise[it.id].orEmpty()) }
                .filter { it.id !in usedExerciseIds }
                .sortedWith(
                    compareByDescending<ExerciseEntity> {
                        relevantVtaperScore(it, muscle, primaryMusclesByExercise[it.id].orEmpty())
                    }
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

    /**
     * Token-based slot matching. Slots are display names ("Lateral Deltoid",
     * "Biceps", ...) while [ExerciseEntity.secondaryMuscles] holds comma-joined
     * TAXONOMY ids ("lateral_deltoid", ...). Matching therefore compares exact
     * taxonomy-id tokens against both the authoritative primary assignments
     * (from `exercise_muscles` role='primary') and the secondary token list,
     * plus category equality and the V-taper relevance scores where defined.
     */
    private fun matchesMuscle(ex: ExerciseEntity, slot: String, primaryIds: List<String>): Boolean {
        val secondaryTokens = ex.secondaryMuscles
            .split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        val primaryTokens = primaryIds.map { it.lowercase() }

        fun hasAny(ids: Set<String>): Boolean =
            primaryTokens.any { it in ids } || secondaryTokens.any { it in ids }

        return when (slot) {
            "Back" -> ex.muscleGroup.equals("back", ignoreCase = true) || hasAny(BACK_MUSCLE_IDS)
            "Chest" -> ex.muscleGroup.equals("chest", ignoreCase = true) || hasAny(CHEST_MUSCLE_IDS)
            "Lateral Deltoid" -> ex.vtaperLateralDelt > 0 || hasAny(setOf("lateral_deltoid"))
            "Rear Deltoid" -> ex.vtaperRearDelt > 0 || hasAny(setOf("rear_deltoid"))
            "Biceps" -> hasAny(setOf("biceps"))
            "Triceps" -> hasAny(setOf("triceps"))
            "Quadriceps" -> hasAny(setOf("quadriceps"))
            "Hamstrings" -> hasAny(setOf("hamstrings"))
            "Glutes" -> hasAny(setOf("glutes"))
            "Calves" -> hasAny(setOf("calves"))
            "Core" -> ex.muscleGroup.equals("core", ignoreCase = true) || hasAny(CORE_MUSCLE_IDS)
            else -> ex.muscleGroup.equals(slot, ignoreCase = true) || secondaryTokens.contains(slot.lowercase())
        }
    }

    private fun relevantVtaperScore(exercise: ExerciseEntity, muscle: String, primaryIds: List<String>): Int {
        val base = when {
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

        val keyId = SLOT_KEY_MUSCLE_IDS[muscle] ?: return base
        val primary = primaryIds.any { it.equals(keyId, ignoreCase = true) }
        if (primary) return base + PRIMARY_BOOST
        val secondary = exercise.secondaryMuscles
            .split(',')
            .any { it.trim().equals(keyId, ignoreCase = true) }
        if (secondary) return base + SECONDARY_BOOST
        return base
    }

    private fun difficultyOrder(difficulty: String): Int = when {
        difficulty.contains("Beginner", ignoreCase = true) -> 0
        difficulty.contains("Intermediate", ignoreCase = true) -> 1
        difficulty.contains("Advanced", ignoreCase = true) -> 2
        else -> 1
    }
}