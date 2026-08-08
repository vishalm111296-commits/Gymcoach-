package com.gymcoach.app.domain.measurement.usecase.impl

import com.gymcoach.app.domain.Result
import com.gymcoach.app.domain.repository.MeasurementRepository
import com.gymcoach.app.domain.vshape.model.MeasurementRecord
import com.gymcoach.app.domain.vshape.model.MeasurementType
import com.gymcoach.app.domain.measurement.model.ValidationResult
import com.gymcoach.app.domain.measurement.usecase.SaveMeasurementUseCase
import com.gymcoach.app.domain.measurement.usecase.GetLatestMeasurementUseCase
import com.gymcoach.app.domain.measurement.usecase.GetMeasurementsUseCase
import com.gymcoach.app.domain.measurement.usecase.ValidateMeasurementUseCase
import javax.inject.Inject

class SaveMeasurementUseCaseImpl @Inject constructor(
    private val repository: MeasurementRepository
) : SaveMeasurementUseCase {
    override suspend fun execute(measurement: MeasurementRecord): Result<Unit> {
        try {
            repository.insertMeasurement(measurement)
            return Result.Success(Unit)
        } catch (e: Exception) {
            return Result.Failure(e)
        }
    }
}

class GetLatestMeasurementUseCaseImpl @Inject constructor(
    private val repository: MeasurementRepository
) : GetLatestMeasurementUseCase {
    override suspend fun execute(userId: String, type: MeasurementType): Result<MeasurementRecord?> {
        return try {
            val measurement = repository.getLatestMeasurement(userId, type)
            Result.Success(measurement)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}

class GetMeasurementsUseCaseImpl @Inject constructor(
    private val repository: MeasurementRepository
) : GetMeasurementsUseCase {
    override suspend fun execute(userId: String, type: MeasurementType): Result<List<MeasurementRecord>> {
        return try {
            val measurements = repository.getMeasurementsByType(userId, type)
            Result.success(measurements)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ValidateMeasurementUseCaseImpl @Inject constructor() : ValidateMeasurementUseCase {
    override fun execute(measurement: MeasurementRecord): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (measurement.value <= 0) {
            errors.add("Value must be positive")
        }
        
        when (measurement.measurementType) {
            MeasurementType.BODY_FAT -> {
                if (measurement.value > 100) {
                    errors.add("Body fat percentage cannot exceed 100%")
                }
            }
            MeasurementType.WEIGHT -> {
                if (measurement.value > 500) {
                    errors.add("Weight value is unrealistic")
                }
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
}