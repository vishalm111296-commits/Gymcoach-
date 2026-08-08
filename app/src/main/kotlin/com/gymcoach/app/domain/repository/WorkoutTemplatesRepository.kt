package com.gymcoach.app.domain.repository

import com.gymcoach.app.domain.model.Workout
import kotlinx.coroutines.flow.Flow

interface WorkoutTemplatesRepository {
    fun getAllTemplates(): Flow<List<Workout>>
    suspend fun saveTemplate(template: Workout): Long
    suspend fun deleteTemplate(templateId: Long)
}
