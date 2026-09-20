package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.NutritionLogDao
import com.gymcoach.app.data.local.entity.NutritionLogEntity
import com.gymcoach.app.domain.repository.NutritionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NutritionRepositoryImpl @Inject constructor(
    private val nutritionLogDao: NutritionLogDao
) : NutritionRepository {
    override fun getLogsForDay(startOfDay: Long, endOfDay: Long) =
        nutritionLogDao.getLogsForDay(startOfDay, endOfDay)

    override fun getRecentLogs() = nutritionLogDao.getRecentLogs()

    override suspend fun addLog(log: NutritionLogEntity) { nutritionLogDao.insert(log) }

    override suspend fun updateLog(log: NutritionLogEntity) { nutritionLogDao.update(log) }

    override suspend fun deleteLog(log: NutritionLogEntity) { nutritionLogDao.delete(log) }

    override suspend fun deleteLogById(id: Long) { nutritionLogDao.deleteById(id) }
}
