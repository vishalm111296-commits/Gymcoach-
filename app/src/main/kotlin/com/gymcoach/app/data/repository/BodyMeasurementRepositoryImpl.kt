package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.BodyMeasurementDao
import com.gymcoach.app.data.local.entity.BodyMeasurementEntity
import com.gymcoach.app.domain.repository.BodyMeasurementRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BodyMeasurementRepositoryImpl @Inject constructor(
    private val dao: BodyMeasurementDao
) : BodyMeasurementRepository {

    override fun getAllMeasurements(): Flow<List<BodyMeasurementEntity>> {
        return dao.getAll()
    }

    override fun getLatestMeasurement(): Flow<BodyMeasurementEntity?> {
        return dao.getLatest()
    }

    override suspend fun saveMeasurement(entity: BodyMeasurementEntity): Long {
        return dao.insert(entity)
    }

    override suspend fun deleteMeasurement(id: Long): Int {
        return dao.deleteById(id)
    }
}
