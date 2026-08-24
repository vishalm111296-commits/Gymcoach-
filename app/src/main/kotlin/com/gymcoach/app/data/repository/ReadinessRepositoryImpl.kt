package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.ReadinessDao
import com.gymcoach.app.data.local.entity.ReadinessEntity
import com.gymcoach.app.domain.repository.ReadinessRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ReadinessRepositoryImpl @Inject constructor(
    private val readinessDao: ReadinessDao
) : ReadinessRepository {

    override fun getAllReadiness(): Flow<List<ReadinessEntity>> {
        return readinessDao.getAll()
    }

    override fun getLatestReadiness(): Flow<ReadinessEntity?> {
        return readinessDao.getLatest()
    }

    override fun getReadinessInRange(startTime: Long, endTime: Long): Flow<List<ReadinessEntity>> {
        return readinessDao.getInRange(startTime, endTime)
    }

    override fun getRecentReadiness(since: Long): Flow<List<ReadinessEntity>> {
        return readinessDao.getRecent(since)
    }

    override suspend fun saveReadiness(readiness: ReadinessEntity): Long {
        return readinessDao.insert(readiness)
    }

    override suspend fun updateReadiness(readiness: ReadinessEntity) {
        readinessDao.update(readiness)
    }

    override suspend fun deleteReadiness(id: Long) {
        readinessDao.deleteById(id)
    }
}
