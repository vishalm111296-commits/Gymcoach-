package com.gymcoach.app.domain.repository

import com.gymcoach.app.domain.model.TemplateExercise
import com.gymcoach.app.domain.model.WorkoutTemplate
import kotlinx.coroutines.flow.Flow

interface WorkoutTemplateRepository {
    fun getActiveTemplates(): Flow<List<WorkoutTemplate>>
    fun getArchivedTemplates(): Flow<List<WorkoutTemplate>>
    fun getTemplateById(id: Long): Flow<WorkoutTemplate?>
    suspend fun saveTemplate(template: WorkoutTemplate, exercises: List<TemplateExercise>): Long
    suspend fun duplicateTemplate(templateId: Long): Long
    suspend fun archiveTemplate(templateId: Long)
    suspend fun unarchiveTemplate(templateId: Long)
    suspend fun deleteTemplate(templateId: Long)
    suspend fun startWorkoutFromTemplate(templateId: Long): Long
}
