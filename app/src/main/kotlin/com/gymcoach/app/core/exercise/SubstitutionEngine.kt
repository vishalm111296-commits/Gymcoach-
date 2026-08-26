package com.gymcoach.app.core.exercise

import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.ExerciseMuscleDao
import com.gymcoach.app.data.local.dao.ExerciseSubstitutionDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubstitutionEngine @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val exerciseMuscleDao: ExerciseMuscleDao,
    private val exerciseSubstitutionDao: ExerciseSubstitutionDao,
    private val equipmentAvailability: EquipmentAvailability
) {

    data class SubstitutionResult(
        val substitute: ExerciseEntity,
        val preservationScore: Int,
        val reason: String
    )

    suspend fun findSubstitutes(
        exerciseId: Long,
        equipmentType: String,
        maxResults: Int = 5
    ): List<SubstitutionResult> {
        val original = exerciseDao.getById(exerciseId).first() ?: return emptyList()
        val substitutes = mutableListOf<SubstitutionResult>()

        // Check predefined substitutions first
        val existingSubs = exerciseSubstitutionDao.getByExerciseId(exerciseId).first()
        for (sub in existingSubs) {
            val substitute = exerciseDao.getById(sub.substituteExerciseId).first() ?: continue
            if (equipmentAvailability.isAvailable(substitute.equipment, equipmentType)) {
                val score = calculatePreservationScore(original, substitute)
                substitutes.add(SubstitutionResult(substitute, score, "Recommended substitute"))
            }
        }

        // Fallback: same muscle group exercises with available equipment
        if (substitutes.size < maxResults) {
            val allExercises = exerciseDao.getAll().first()
            val sameGroup = allExercises
                .filter { it.id != exerciseId && it.muscleGroup.equals(original.muscleGroup, ignoreCase = true) }
                .filter { equipmentAvailability.isAvailable(it.equipment, equipmentType) }
                .filter { sub -> substitutes.none { it.substitute.id == sub.id } }
                .take(maxResults - substitutes.size)

            for (ex in sameGroup) {
                val score = calculatePreservationScore(original, ex)
                substitutes.add(SubstitutionResult(ex, score, "Same muscle group"))
            }
        }

        return substitutes.sortedByDescending { it.preservationScore }.take(maxResults)
    }

    private fun calculatePreservationScore(original: ExerciseEntity, substitute: ExerciseEntity): Int {
        var score = 0
        if (original.muscleGroup.equals(substitute.muscleGroup, ignoreCase = true)) score += 40
        if (original.category == substitute.category) score += 20
        if (original.equipment == substitute.equipment) score += 15
        if (original.difficulty == substitute.difficulty) score += 10
        val origPattern = original.tags.lowercase()
        val subPattern = substitute.tags.lowercase()
        if (origPattern.contains("compound") && subPattern.contains("compound")) score += 10
        if (origPattern.contains("isolation") && subPattern.contains("isolation")) score += 10
        return score.coerceAtMost(100)
    }
}