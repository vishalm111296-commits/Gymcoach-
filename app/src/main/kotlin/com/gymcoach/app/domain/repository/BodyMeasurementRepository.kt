package com.gymcoach.app.domain.repository

import com.gymcoach.app.data.local.entity.BodyMeasurementEntity
import kotlinx.coroutines.flow.Flow

interface BodyMeasurementRepository {
    fun getAllMeasurements(): Flow<List<BodyMeasurementEntity>>
    fun getLatestMeasurement(): Flow<BodyMeasurementEntity?>
    suspend fun saveMeasurement(entity: BodyMeasurementEntity): Long
    suspend fun deleteMeasurement(id: Long): Int
}
