package com.gymcoach.app.domain.repository

import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.model.ExerciseMuscleAssignment
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun getAllExercises(): Flow<List<Exercise>>
    fun getFilteredExercises(muscle: String?, difficulty: String?, equipment: String?): Flow<List<Exercise>>
    fun searchExercises(query: String): Flow<List<Exercise>>
    fun getExerciseById(id: Long): Flow<Exercise?>
    fun getAllExerciseMuscleDetails(): Flow<List<ExerciseMuscleAssignment>>
    suspend fun addExercise(exercise: Exercise)
    suspend fun updateExercise(exercise: Exercise)
    suspend fun deleteExercise(exercise: Exercise)
}
