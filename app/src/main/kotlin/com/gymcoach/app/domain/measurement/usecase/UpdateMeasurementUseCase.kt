package com.gymcoach.app.domain.measurement.usecase

import com.gymcoach.app.domain.repository.MeasurementRepository
import com.gymcoach.app.domain.vshape.model.MeasurementRecord
import javax.inject.Inject

class UpdateMeasurementUseCase @Inject constructor(
    private val repository: MeasurementRepository
) {
    suspend operator fun invoke(measurement: MeasurementRecord): Result<Unit> {
        return try {
            if (measurement.value < 0) {
                return Result.failure(IllegalArgumentException("Value must be positive"))
            }
            
            repository.updateMeasurement(measurement)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
