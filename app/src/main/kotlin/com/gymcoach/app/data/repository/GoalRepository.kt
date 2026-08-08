package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.GoalDao
import com.gymcoach.app.data.local.entity.GoalEntity
import com.gymcoach.app.domain.vshape.model.Goal
import com.gymcoach.app.domain.vshape.model.GoalStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GoalRepository @Inject constructor(
    private val goalDao: GoalDao
) {
    fun getGoalsForUser(userId: String): Flow<List<Goal>> {
        return goalDao.getGoalsForUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getGoalsByStatus(userId: String, status: GoalStatus): Flow<List<Goal>> {
        return goalDao.getGoalsByStatus(userId, status.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getGoalsByType(userId: String, goalType: com.gymcoach.app.domain.vshape.model.GoalType): Flow<List<Goal>> {
        return goalDao.getGoalsByType(userId, goalType.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getActiveGoalsCount(userId: String): Flow<Int> {
        return goalDao.getActiveGoalsCount(userId)
    }

    suspend fun insertGoal(goal: Goal) {
        val entity = goal.toEntity()
        goalDao.insertGoal(entity)
    }

    suspend fun updateGoal(goal: Goal) {
        val entity = goal.toEntity()
        goalDao.updateGoal(entity)
    }

    suspend fun deleteCompletedGoals(userId: String) {
        goalDao.deleteCompletedGoals(userId)
    }

    private fun GoalEntity.toDomain(): Goal {
        return Goal(
            id = id,
            userId = userId,
            goalType = com.gymcoach.app.domain.vshape.model.GoalType.valueOf(goalType),
            targetValue = targetValue,
            currentValue = currentValue,
            unit = unit,
            startDate = java.time.Instant.ofEpochMilli(startDate),
            targetDate = java.time.Instant.ofEpochMilli(targetDate),
            status = GoalStatus.valueOf(status),
            createdAt = java.time.Instant.ofEpochMilli(createdAt),
            priority = com.gymcoach.app.domain.vshape.model.GoalPriority.valueOf(priority),
            notes = notes
        )
    }

    private fun Goal.toEntity(): GoalEntity {
        return GoalEntity(
            id = id,
            userId = userId,
            goalType = goalType.name,
            targetValue = targetValue,
            currentValue = currentValue,
            unit = unit,
            startDate = startDate.toEpochMilli(),
            targetDate = targetDate.toEpochMilli(),
            status = status.name,
            createdAt = createdAt.toEpochMilli(),
            priority = priority.name,
            notes = notes
        )
    }
}