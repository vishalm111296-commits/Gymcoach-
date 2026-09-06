package com.gymcoach.app.domain.repository

import com.gymcoach.app.core.program.VolumeCalculator.MuscleAssignment
import com.gymcoach.app.domain.model.Exercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun getAllExercises(): Flow<List<Exercise>>
    fun getFilteredExercises(muscle: String?, difficulty: String?, equipment: String?): Flow<List<Exercise>>
    fun searchExercises(query: String): Flow<List<Exercise>>
    fun getExerciseById(id: Long): Flow<Exercise?>
    suspend fun addExercise(exercise: Exercise)
    suspend fun updateExercise(exercise: Exercise)
    suspend fun deleteExercise(exercise: Exercise)

    /**
     * Returns a map from exerciseId to its muscle assignments with roles
     * (PRIMARY/SECONDARY/STABILIZER) from the exercise_muscles table.
     * Used by VolumeCalculator for weighted volume attribution.
     */
    suspend fun getMuscleAssignmentsWithRoles(): Map<Long, List<MuscleAssignment>>
}
