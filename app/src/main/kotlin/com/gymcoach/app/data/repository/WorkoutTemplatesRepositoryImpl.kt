package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.entity.WorkoutEntity
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.repository.WorkoutTemplatesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class WorkoutTemplatesRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao
) : WorkoutTemplatesRepository {
    override fun getAllTemplates(): Flow<List<Workout>> {
        return workoutDao.getAllWorkoutTemplates().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveTemplate(template: Workout): Long {
        return workoutDao.insertWorkout(template.toEntity().copy(isTemplate = true))
    }

    override suspend fun deleteTemplate(templateId: Long) {
        workoutDao.getWorkoutById(templateId).firstOrNull()?.let {
            workoutDao.deleteWorkout(it)
        }
    }
}

private fun WorkoutEntity.toDomain() = Workout(
    id = id,
    date = Instant.ofEpochMilli(date),
    startTime = Instant.ofEpochMilli(startTime),
    endTime = Instant.ofEpochMilli(endTime),
    duration = duration,
    notes = notes,
    completed = completed,
    isTemplate = isTemplate
)

private fun Workout.toEntity() = WorkoutEntity(
    id = id,
    date = date.toEpochMilli(),
    startTime = startTime.toEpochMilli(),
    endTime = endTime.toEpochMilli(),
    duration = duration,
    notes = notes,
    completed = completed,
    isTemplate = isTemplate
)
