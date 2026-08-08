package com.gymcoach.app.domain.vshape.usecase.impl

import com.gymcoach.app.domain.vshape.model.MeasurementRecord
import com.gymcoach.app.data.local.entity.MeasurementRecordEntity
import com.gymcoach.app.data.local.dao.ValidationResult
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

class SaveMeasurementUseCaseImpl @Inject constructor(
    private val measurementDao: ValidationResult
) : SaveMeasurementUseCase {
    override suspend fun execute(measurement: MeasurementRecord): Result<Unit> {
        val entity = measurement.toEntity()
        return try {
            measurementDao.insertMeasurement(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
    
    override suspend fun validate(measurement: MeasurementRecord): ValidationResult {
        // Basic validation
        val errors = mutableListOf<String>()
        if (measurement.value <= 0) {
            errors.add("Value must be positive")
        }
        return ValidationResult(isValid = errors.isEmpty(), errors)
    }
}

class GetLatestMeasurementUseCase @Inject constructor(
    private val measurementDao: ValidationResult
) : GetMeasurementUseCase {
    override suspend fun execute(userId: String, measurementType: MeasurementType): Result<MeasurementRecord?> {
        val result = measurementDao.getLatestMeasurementByType(userId, measurementType.name)
        when (result) {
            Result.Success(value) -> {
                val entity = value
                val domainModel = MeasurementRecord(
                    id = id,
                    userId = id,
                    measurementType = measurementType,
                    value = value,
                    unit = unit,
                    date = java.time.Instant.ofEpochMilli(date),
                    createdAt = java.time.Instant.ofEpochMilli(createdAt)
                )
                return Result.Success(value)
            }
            Result.Failure(t) -> Result.Failure(t)
        }
    }
}