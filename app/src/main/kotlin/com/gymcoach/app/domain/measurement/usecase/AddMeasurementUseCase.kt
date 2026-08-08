package com.gymcoach.app.domain.measurement.usecase

import com.gymcoach.app.domain.repository.MeasurementRepository
import com.gymcoach.app.domain.vshape.model.MeasurementRecord
import com.gymcoach.app.domain.vshape.model.MeasurementType
import java.time.Instant
import javax.inject.Inject

class AddMeasurementUseCase @Inject constructor(
    private val repository: MeasurementRepository
) {
    suspend operator fun invoke(
        userId: String,
        type: MeasurementType,
        value: Double,
        notes: String? = null
    ): Result<Unit> {
        return try {
            if (value < 0) {
                return Result.failure(IllegalArgumentException("Value must be positive"))
            }
            
            when (type) {
                MeasurementType.BODY_FAT -> {
                    if (value > 100.0) {
                        return Result.failure(IllegalArgumentException("Body fat percentage cannot exceed 100%"))
                    }
                }
                MeasurementType.WEIGHT -> {
                    if (value > 500.0) {
                        return Result.failure(IllegalArgumentException("Weight value unrealistic"))
                    }
                }
                else -> {
                    if (value > 500.0) {
                        return Result.failure(IllegalArgumentException("Measurement value unrealistic"))
                    }
                }
            }
            
            val measurement = MeasurementRecord(
                userId = userId,
                measurementType = type,
                value = value,
                unit = type.unit,
                date = Instant.now(),
                notes = notes,
                createdAt = Instant.now()
            )
            
            repository.insertMeasurement(measurement)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
