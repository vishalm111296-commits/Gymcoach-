package com.gymcoach.app.core.exercise

import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.ExerciseMuscleDao
import com.gymcoach.app.data.local.dao.ExerciseSubstitutionDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubstitutionEngine @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val exerciseMuscleDao: ExerciseMuscleDao,
    private val exerciseSubstitutionDao: ExerciseSubstitutionDao
) {

    data class SubstitutionResult(
        val substitute: ExerciseEntity,
        val preservationScore: Int,
        val reason: String
    )

    suspend fun findSubstitutes(
        exerciseId: Long,
        availableEquipment: List<String>,
        maxResults: Int = 5
    ): List<SubstitutionResult> {
        val original = exerciseDao.getById(exerciseId) ?: return emptyList()
        val substitutes = mutableListOf<SubstitutionResult>()

        val existingSubs = exerciseSubstitutionDao.getByExerciseId(exerciseId)
        for (sub in existingSubs) {
            val substitute = exerciseDao.getById(sub.substituteId) ?: continue
            if (substitute.equipment in availableEquipment || substitute.equipment == "Bodyweight") {
                val score = calculatePreservationScore(original, substitute)
                substitutes.add(SubstitutionResult(substitute, score, "Recommended substitute"))
            }
        }

        if (substitutes.size < maxResults) {
            val sameGroup = exerciseDao.getAll()
                .filter { it.id != exerciseId && it.muscleGroup == original.muscleGroup }
                .filter { it.equipment in availableEquipment || it.equipment == "Bodyweight" }
                .filter { sub -> substitutes.none { it.substitute.id == sub.id } }
            for (ex in sameGroup.take(maxResults - substitutes.size)) {
                val score = calculatePreservationScore(original, ex)
                substitutes.add(SubstitutionResult(ex, score, "Same muscle group"))
            }
        }

        return substitutes.sortedByDescending { it.preservationScore }.take(maxResults)
    }

    private fun calculatePreservationScore(original: ExerciseEntity, substitute: ExerciseEntity): Int {
        var score = 0
        if (original.muscleGroup == substitute.muscleGroup) score += 40
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
