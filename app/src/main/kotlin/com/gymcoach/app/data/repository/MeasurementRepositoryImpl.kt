package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.MeasurementDao
import com.gymcoach.app.data.local.entity.MeasurementRecordEntity
import com.gymcoach.app.domain.repository.MeasurementRepository
import com.gymcoach.app.domain.repository.MeasurementTrend
import com.gymcoach.app.domain.vshape.model.MeasurementRecord
import com.gymcoach.app.domain.vshape.model.MeasurementType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MeasurementRepositoryImpl @Inject constructor(
    private val measurementDao: MeasurementDao
) : MeasurementRepository {

    override fun getMeasurementsForUser(userId: String): Flow<List<MeasurementRecord>> {
        return measurementDao.getMeasurementsForUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMeasurementsByType(userId: String, type: MeasurementType): Flow<List<MeasurementRecord>> {
        return measurementDao.getMeasurementsByType(userId, type.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getLatestMeasurementForUser(userId: String): Flow<MeasurementRecord?> {
        return measurementDao.getLatestMeasurementForUser(userId).map { it?.toDomain() }
    }

    override suspend fun insertMeasurement(measurement: MeasurementRecord) {
        measurementDao.insertMeasurement(measurement.toEntity())
    }

    override suspend fun updateMeasurement(measurement: MeasurementRecord) {
        measurementDao.updateMeasurement(measurement.toEntity())
    }

    override fun getTrendForType(userId: String, type: MeasurementType): Flow<MeasurementTrend> {
        return getMeasurementsByType(userId, type).map { measurements ->
            if (measurements.size < 2) return@map MeasurementTrend(0.0, 0.0)
            
            val sorted = measurements.sortedBy { it.date }
            val latest = sorted.last()
            val previous = sorted[sorted.size - 2]
            
            val diff = latest.value - previous.value
            val percentChange = if (previous.value != 0.0) (diff / previous.value) * 100 else 0.0
            
            MeasurementTrend(diff, percentChange)
        }
    }
    
    override fun getMeasurementsSince(userId: String, startDate: Long): Flow<List<MeasurementRecord>> {
        return measurementDao.getMeasurementsSince(startDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private fun MeasurementRecordEntity.toDomain(): MeasurementRecord {
        return MeasurementRecord(
            id = id,
            userId = userId,
            measurementType = MeasurementType.valueOf(measurementType),
            value = value,
            unit = unit,
            date = java.time.Instant.ofEpochMilli(date),
            notes = notes,
            createdAt = java.time.Instant.ofEpochMilli(createdAt)
        )
    }

    private fun MeasurementRecord.toEntity(): MeasurementRecordEntity {
        return MeasurementRecordEntity(
            id = id,
            userId = userId,
            measurementType = measurementType.name,
            value = value,
            unit = unit,
            date = date.toEpochMilli(),
            notes = notes,
            createdAt = createdAt.toEpochMilli()
        )
    }
}
