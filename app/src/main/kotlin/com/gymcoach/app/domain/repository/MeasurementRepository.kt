package com.gymcoach.app.domain.repository

import com.gymcoach.app.domain.vshape.model.MeasurementRecord
import com.gymcoach.app.domain.vshape.model.MeasurementType
import kotlinx.coroutines.flow.Flow

interface MeasurementRepository {
    fun getMeasurementsForUser(userId: String): Flow<List<MeasurementRecord>>
    fun getMeasurementsByType(userId: String, type: MeasurementType): Flow<List<MeasurementRecord>>
    fun getLatestMeasurementForUser(userId: String): Flow<MeasurementRecord?>
    suspend fun insertMeasurement(measurement: MeasurementRecord)
    suspend fun updateMeasurement(measurement: MeasurementRecord)
    fun getTrendForType(userId: String, type: MeasurementType): Flow<MeasurementTrend>
    fun getMeasurementsSince(userId: String, startDate: Long): Flow<List<MeasurementRecord>>
}

data class MeasurementTrend(
    val absoluteChange: Double,
    val percentChange: Double
)