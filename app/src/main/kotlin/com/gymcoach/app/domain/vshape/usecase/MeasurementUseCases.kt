package com.gymcoach.app.domain.vshape.usecase

import com.gymcoach.app.domain.vshape.model.MeasurementRecord
import com.gymcoach.app.domain.vshape.model.MeasurementType

interface SaveMeasurementUseCase {
    suspend fun execute(measurement: MeasurementRecord): Result<Unit>
}

interface GetLatestMeasurementUseCase {
    suspend fun execute(userId: String, type: MeasurementType): Result<MeasurementRecord?>
}

interface GetMeasurementsUseCase {
    suspend fun execute(userId: String): Result<List<MeasurementRecord>>
}

interface ValidateMeasurementUseCase {
    fun execute(measurement: MeasurementRecord): ValidationResult
}

sealed class Result<out T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Failure(val throwable: Throwable) : Result<Nothing>()
}

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
)