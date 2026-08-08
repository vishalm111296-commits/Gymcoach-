package com.gymcoach.app.domain.measurement.usecase

import com.gymcoach.app.domain.vshape.model.MeasurementRecord
import com.gymcoach.app.domain.vshape.model.MeasurementType
import com.gymcoach.app.domain.Result
import com.gymcoach.app.domain.measurement.model.ValidationResult

interface SaveMeasurementUseCase {
    suspend fun execute(measurement: MeasurementRecord): Result<Unit>
}

interface GetLatestMeasurementUseCase {
    suspend fun execute(userId: String, type: MeasurementType): Result<MeasurementRecord?>
}

interface GetMeasurementsUseCase {
    suspend fun execute(userId: String, type: MeasurementType): Result<List<MeasurementRecord>>
}

interface ValidateMeasurementUseCase {
    fun execute(measurement: MeasurementRecord): ValidationResult
}
