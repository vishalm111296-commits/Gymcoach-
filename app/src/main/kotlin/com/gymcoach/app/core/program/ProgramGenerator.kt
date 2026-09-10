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
            // F-PROG-2 fix: 1-2 day/week programs must use Full Body.
            // The previous code routed 1 and 2 to the `else` branch which called
            // generateUpperLower (a 4-day split). That produced 4 days of programming
            // regardless of how many days the user wanted to train, and surfaced
            // sessions that were structured for 4x/week recovery patterns.
            1 -> generateFullBody(filteredExercises, baseReadinessScore).take(1)
            2 -> generateFullBody(filteredExercises, baseReadinessScore).take(2)
            3 -> generateFullBody(filteredExercises, baseReadinessScore)
            4 -> generateUpperLower(filteredExercises, baseReadinessScore)
            5 -> generatePPLUpperLower(filteredExercises, baseReadinessScore)
            6 -> generatePPLDouble(filteredExercises, baseReadinessScore)
            // 7 days: add an extra full-body finisher day to the PPL double
            else -> generatePPLDouble(filteredExercises, baseReadinessScore) +
                listOf(buildDay(7, "Full Body", listOf("Back", "Chest", "Lateral Deltoid", "Quadriceps", "Core"), filteredExercises, baseReadinessScore, maxExercisesPerSession(frequency)))
        }
        return GeneratedProgram(
            name = "V-Taper $frequency-Day Program",
            description = "Personalized $frequency-day training program for $goal. Adjusted for readiness score: ${".1f".format(baseReadinessScore)}",
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

    /**
     * F-PROG-1 fix: compute a per-session exercise cap from the user's frequency.
     * Higher frequency = more recovery overlap = fewer exercises per session.
     * These values keep total weekly volume in a sensible range without access to
     * UserProfile.sessionLengthMinutes at this call site.
     * A future improvement can pass sessionLengthMinutes here for finer control.
     */
    private fun maxExercisesPerSession(frequency: Int): Int = when {
        frequency <= 2 -> 8   // Full-body 1–2×/wk: more per session
        frequency == 3 -> 7   // Full-body 3×/wk
        frequency == 4 -> 6   // Upper/Lower 4×/wk
        frequency == 5 -> 5   // PPL+Upper/Lower hybrid
        else -> 4             // PPL 6×/wk: more sessions, fewer exercises each
    }

    private fun generateUpperLower(exercises: List<ExerciseEntity>, baseReadinessScore: Double): List<ProgramDay> {
        val cap = maxExercisesPerSession(4)
        // F-PROG-4 fix: Upper A and Upper B had identical muscle lists.
        // Upper A is volume-focused (more lat/delt work); Upper B swaps Rear Delt in
        // for an extra Biceps slot so there is genuine programming variety.
        val upperA = listOf("Back", "Chest", "Lateral Deltoid", "Rear Deltoid", "Biceps", "Triceps")
        val lowerA = listOf("Quadriceps", "Hamstrings", "Glutes", "Calves")
        val upperB = listOf("Back", "Lateral Deltoid", "Chest", "Biceps", "Triceps", "Core")
        val lowerB = listOf("Hamstrings", "Quadriceps", "Glutes", "Core", "Calves")
        return listOf(
            buildDay(1, "Upper A", upperA, exercises, baseReadinessScore, cap),
            buildDay(2, "Lower A", lowerA, exercises, baseReadinessScore, cap),
            buildDay(3, "Upper B", upperB, exercises, baseReadinessScore, cap),
            buildDay(4, "Lower B", lowerB, exercises, baseReadinessScore, cap)
        )
    }

    private fun generateFullBody(exercises: List<ExerciseEntity>, baseReadinessScore: Double): List<ProgramDay> {
        val cap = maxExercisesPerSession(3)
        return listOf(
            buildDay(1, "Full Body A", listOf("Back", "Chest", "Quadriceps", "Lateral Deltoid", "Core"), exercises, baseReadinessScore, cap),
            buildDay(2, "Full Body B", listOf("Back", "Chest", "Hamstrings", "Rear Deltoid", "Biceps"), exercises, baseReadinessScore, cap),
            buildDay(3, "Full Body C", listOf("Back", "Chest", "Glutes", "Lateral Deltoid", "Triceps"), exercises, baseReadinessScore, cap)
        )
    }

    private fun generatePPLUpperLower(exercises: List<ExerciseEntity>, baseReadinessScore: Double): List<ProgramDay> {
        val cap = maxExercisesPerSession(5)
        return listOf(
            buildDay(1, "Push", listOf("Chest", "Lateral Deltoid", "Triceps"), exercises, baseReadinessScore, cap),
            buildDay(2, "Pull", listOf("Back", "Rear Deltoid", "Biceps"), exercises, baseReadinessScore, cap),
            buildDay(3, "Legs", listOf("Quadriceps", "Hamstrings", "Glutes", "Calves"), exercises, baseReadinessScore, cap),
            buildDay(4, "Upper", listOf("Back", "Chest", "Lateral Deltoid", "Rear Deltoid", "Biceps", "Triceps"), exercises, baseReadinessScore, cap),
            buildDay(5, "Lower", listOf("Quadriceps", "Hamstrings", "Glutes", "Core", "Calves"), exercises, baseReadinessScore, cap)
        )
    }

    private fun generatePPLDouble(exercises: List<ExerciseEntity>, baseReadinessScore: Double): List<ProgramDay> {
        val cap = maxExercisesPerSession(6)
        return listOf(
            buildDay(1, "Push", listOf("Chest", "Lateral Deltoid", "Triceps"), exercises, baseReadinessScore, cap),
            buildDay(2, "Pull", listOf("Back", "Rear Deltoid", "Biceps"), exercises, baseReadinessScore, cap),
            buildDay(3, "Legs", listOf("Quadriceps", "Hamstrings", "Glutes", "Calves"), exercises, baseReadinessScore, cap),
            buildDay(4, "Push", listOf("Chest", "Lateral Deltoid", "Triceps"), exercises, baseReadinessScore, cap),
            buildDay(5, "Pull", listOf("Back", "Rear Deltoid", "Biceps"), exercises, baseReadinessScore, cap),
            buildDay(6, "Legs", listOf("Quadriceps", "Hamstrings", "Glutes", "Calves"), exercises, baseReadinessScore, cap)
        )
    }

    /**
     * Build one training day.
     *
     * F-PROG-1 fix: [maxExercises] caps total exercises across all muscle slots.
     * Previously there was no budget guard and a 6-slot Upper day could generate
     * 12 exercises (6 slots × .take(2)) = 36–48 sets, far exceeding a 60-minute session.
     */
    private fun buildDay(
        dayNum: Int,
        name: String,
        muscles: List<String>,
        allExercises: List<ExerciseEntity>,
        baseReadinessScore: Double,
        maxExercises: Int = 8
    ): ProgramDay {
        val selected = mutableListOf<ProgramExercise>()
        val usedExerciseIds = mutableSetOf<Long>()

        for (muscle in muscles) {
            if (selected.size >= maxExercises) break

            val candidates = allExercises
                .filter { it.muscleGroup.equals(muscle, ignoreCase = true) || it.secondaryMuscles.contains(muscle, ignoreCase = true) }
                .filter { it.id !in usedExerciseIds }
                .sortedWith(
                    compareByDescending<ExerciseEntity> { relevantVtaperScore(it, muscle) }
                        .thenBy { difficultyOrder(it.difficulty) }
                )
                // Take at most 2 per slot, but never exceed the session cap
                .take(minOf(2, maxExercises - selected.size))

            for (ex in candidates) {
                if (ex.id !in usedExerciseIds && selected.size < maxExercises) {
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
        else -> exercise.vtaperLat + exercise.vtaperLateralDelt + exercise.vtaperUpperChest + exercise.vtaperRearDelt
    }

    private fun difficultyOrder(difficulty: String): Int = when {
        difficulty.contains("Beginner", ignoreCase = true) -> 0
        difficulty.contains("Intermediate", ignoreCase = true) -> 1
        difficulty.contains("Advanced", ignoreCase = true) -> 2
        else -> 1
    }
}
