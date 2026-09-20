package com.gymcoach.app.domain.repository

import com.gymcoach.app.data.local.entity.NutritionLogEntity
import kotlinx.coroutines.flow.Flow

interface NutritionRepository {
    fun getLogsForDay(startOfDay: Long, endOfDay: Long): Flow<List<NutritionLogEntity>>
    fun getRecentLogs(): Flow<List<NutritionLogEntity>>
    suspend fun addLog(log: NutritionLogEntity)
    suspend fun updateLog(log: NutritionLogEntity)
    suspend fun deleteLog(log: NutritionLogEntity)
    suspend fun deleteLogById(id: Long)
}
