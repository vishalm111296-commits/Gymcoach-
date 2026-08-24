package com.gymcoach.app.domain.repository

import com.gymcoach.app.data.local.entity.ReadinessEntity
import kotlinx.coroutines.flow.Flow

interface ReadinessRepository {
    fun getAllReadiness(): Flow<List<ReadinessEntity>>
    fun getLatestReadiness(): Flow<ReadinessEntity?>
    fun getReadinessInRange(startTime: Long, endTime: Long): Flow<List<ReadinessEntity>>
    fun getRecentReadiness(since: Long): Flow<List<ReadinessEntity>>
    suspend fun saveReadiness(readiness: ReadinessEntity): Long
    suspend fun updateReadiness(readiness: ReadinessEntity)
    suspend fun deleteReadiness(id: Long)
}
