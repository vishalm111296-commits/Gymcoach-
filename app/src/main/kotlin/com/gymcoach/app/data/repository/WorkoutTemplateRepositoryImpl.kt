package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.dao.WorkoutTemplateDao
import com.gymcoach.app.data.local.entity.TemplateExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutTemplateEntity
import com.gymcoach.app.domain.model.TemplateExercise
import com.gymcoach.app.domain.model.WorkoutTemplate
import com.gymcoach.app.domain.repository.WorkoutTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class WorkoutTemplateRepositoryImpl @Inject constructor(
    private val workoutTemplateDao: WorkoutTemplateDao,
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao
) : WorkoutTemplateRepository {

    override fun getActiveTemplates(): Flow<List<WorkoutTemplate>> {
        return workoutTemplateDao.getActiveTemplates().flatMapLatest { entities ->
            if (entities.isEmpty()) return@flatMapLatest flowOf(emptyList())
            val flows = entities.map { entity ->
                getTemplateWithExercisesFlow(entity)
            }
            combine(flows) { templates -> templates.toList() }
        }
    }

    override fun getArchivedTemplates(): Flow<List<WorkoutTemplate>> {
        return workoutTemplateDao.getArchivedTemplates().flatMapLatest { entities ->
            if (entities.isEmpty()) return@flatMapLatest flowOf(emptyList())
            val flows = entities.map { entity ->
                getTemplateWithExercisesFlow(entity)
            }
            combine(flows) { templates -> templates.toList() }
        }
    }

    override fun getTemplateById(id: Long): Flow<WorkoutTemplate?> {
        return workoutTemplateDao.getTemplateById(id).flatMapLatest { entity ->
            if (entity == null) flowOf(null)
            else getTemplateWithExercisesFlow(entity)
        }
    }

    private fun getTemplateWithExercisesFlow(templateEntity: WorkoutTemplateEntity): Flow<WorkoutTemplate> {
        return workoutTemplateDao.getTemplateExercises(templateEntity.id).map { exEntities ->
            val exerciseIds = exEntities.map { it.exerciseId }.distinct()
            val exerciseMap = if (exerciseIds.isNotEmpty()) {
                exerciseDao.getByIdsSync(exerciseIds).associateBy { it.id }
            } else {
                emptyMap()
            }
            val exercises = exEntities.map { exEntity ->
                val exerciseEntity = exerciseMap[exEntity.exerciseId]
                TemplateExercise(
                    id = exEntity.id,
                    templateId = exEntity.templateId,
                    exerciseId = exEntity.exerciseId,
                    exerciseName = exerciseEntity?.name ?: "Exercise #${exEntity.exerciseId}",
                    muscleGroup = exerciseEntity?.muscleGroup ?: "",
                    equipment = exerciseEntity?.equipment ?: "",
                    orderIndex = exEntity.orderIndex,
                    targetSets = exEntity.targetSets,
                    targetReps = exEntity.targetReps,
                    targetWeightKg = exEntity.targetWeightKg,
                    targetRpe = exEntity.targetRpe,
                    restSeconds = exEntity.restSeconds,
                    notes = exEntity.notes
                )
            }
            WorkoutTemplate(
                id = templateEntity.id,
                name = templateEntity.name,
                description = templateEntity.description,
                isArchived = templateEntity.isArchived,
                version = templateEntity.version,
                createdAt = templateEntity.createdAt,
                updatedAt = templateEntity.updatedAt,
                exercises = exercises
            )
        }
    }

    override suspend fun saveTemplate(
        template: WorkoutTemplate,
        exercises: List<TemplateExercise>
    ): Long {
        val now = System.currentTimeMillis()
        val isUpdate = template.id > 0L
        val entity = if (!isUpdate) {
            WorkoutTemplateEntity(
                name = template.name.trim(),
                description = template.description.trim(),
                isArchived = false,
                version = 1,
                createdAt = now,
                updatedAt = now
            )
        } else {
            WorkoutTemplateEntity(
                id = template.id,
                name = template.name.trim(),
                description = template.description.trim(),
                isArchived = template.isArchived,
                version = template.version + 1,
                createdAt = template.createdAt,
                updatedAt = now
            )
        }

        val exerciseEntities = exercises.mapIndexed { index, ex ->
            TemplateExerciseEntity(
                templateId = template.id,
                exerciseId = ex.exerciseId,
                orderIndex = index,
                targetSets = ex.targetSets,
                targetReps = ex.targetReps,
                targetWeightKg = ex.targetWeightKg,
                targetRpe = ex.targetRpe,
                restSeconds = ex.restSeconds,
                notes = ex.notes
            )
        }
        return workoutTemplateDao.saveTemplateAtomic(entity, exerciseEntities, isUpdate)
    }

    override suspend fun duplicateTemplate(templateId: Long): Long {
        return workoutTemplateDao.duplicateTemplateAtomic(templateId)
    }

    override suspend fun archiveTemplate(templateId: Long) {
        workoutTemplateDao.archiveTemplate(templateId)
    }

    override suspend fun unarchiveTemplate(templateId: Long) {
        workoutTemplateDao.unarchiveTemplate(templateId)
    }

    override suspend fun deleteTemplate(templateId: Long) {
        workoutTemplateDao.deleteTemplateById(templateId)
    }

    override suspend fun startWorkoutFromTemplate(templateId: Long): Long {
        val template = workoutTemplateDao.getTemplateByIdSync(templateId)
            ?: throw IllegalArgumentException("Template with id $templateId not found")
        val exercises = workoutTemplateDao.getTemplateExercisesSync(templateId)
        return workoutDao.createWorkoutFromTemplateTransaction(template, exercises)
    }
}
