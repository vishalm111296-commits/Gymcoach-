package com.gymcoach.app.domain.repository

import com.gymcoach.app.data.local.dao.ExerciseMuscleWithDetails
import com.gymcoach.app.domain.model.Exercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun getAllExercises(): Flow<List<Exercise>>
    fun getFilteredExercises(muscle: String?, difficulty: String?, equipment: String?): Flow<List<Exercise>>
    fun searchExercises(query: String): Flow<List<Exercise>>
    fun getExerciseById(id: Long): Flow<Exercise?>
    fun getCustomExercises(): Flow<List<Exercise>>
    fun getAllExerciseMuscleDetails(): Flow<List<ExerciseMuscleWithDetails>>
    suspend fun addExercise(exercise: Exercise): Long
    suspend fun createCustomExercise(
        name: String,
        muscleGroup: String,
        equipment: String,
        difficulty: String = "Intermediate",
        notes: String = ""
    ): Long
    suspend fun updateExercise(exercise: Exercise)
    suspend fun deleteExercise(exercise: Exercise)
    suspend fun deleteCustomExercise(id: Long): Boolean
}
