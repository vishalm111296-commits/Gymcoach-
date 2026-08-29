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
        val reasons: List<String>
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
                val reasons = mutableListOf("Recommended substitute")
                appendCommonReasons(original, substitute, reasons)
                substitutes.add(SubstitutionResult(substitute, reasons))
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
                val reasons = mutableListOf<String>()
                appendCommonReasons(original, ex, reasons)
                substitutes.add(SubstitutionResult(ex, reasons))
            }
        }

        // Sort by the number of matching reasons
        return substitutes.sortedByDescending { it.reasons.size }.take(maxResults)
    }

    private fun appendCommonReasons(original: ExerciseEntity, substitute: ExerciseEntity, reasons: MutableList<String>) {
        if (original.muscleGroup.equals(substitute.muscleGroup, ignoreCase = true)) {
            reasons.add("Same primary muscle")
        }
        if (original.equipment == substitute.equipment) {
            reasons.add("Same equipment")
        }
        val origPattern = original.movementPattern.lowercase()
        val subPattern = substitute.movementPattern.lowercase()
        if (origPattern.isNotBlank() && origPattern == subPattern) {
            reasons.add("Similar movement pattern")
        }
    }
}