package com.gymcoach.app.domain.vshape.usecase.impl

import com.gymcoach.app.domain.repository.MeasurementRepository
import com.gymcoach.app.domain.vshape.model.MeasurementRecord
import com.gymcoach.app.domain.vshape.model.MeasurementType
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.stdlib.functions.Functions.not

class GetMeasurementsSinceUseCaseImpl @Inject constructor(
    private val repository: MeasurementRepository
) : GetMeasurementsSinceUseCase {
    override suspend fun execute(userId: String, startDate: Long): Result<List<MeasurementRecord>> {
        return try {
            val measurements = repository.getMeasurementsSince(userId, startDate)
            val domainMeasurements = measurements.value // Convert Flow to List
            Result.Success(domainMeasurements)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}

class GetMeasurementsByTypeUseCaseImpl @Inject constructor(
    private val repository: MeasurementRepository
) : GetMeasurementsByTypeUseCase {
    override suspend fun execute(userId: String, type: MeasurementType): Result<List<MeasurementRecord>> {
        return try {
            val measurements = repository.getMeasurementsByType(userId, type).first()
            Result.Success(measurements)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}

class GetMeasurementsForUserUseCaseImpl @Inject constructor(
    private val repository: MeasurementRepository
) : GetMeasurementsForUserUseCase {
    override suspend fun execute(userId: String): Result<List<MeasurementRecord>> {
        return try {
            val measurements = repository.getMeasurementsForUser(userId).first()
            Result.Success(measurements)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}

class GetLatestMeasurementUseCaseImpl @Inject constructor(
    private val repository: MeasurementRepository
) : GetLatestMeasurementUseCase {
    override suspend fun execute(userId: String, type: MeasurementType): Result<MeasurementRecord?> {
        return try {
            val measurement = repository.getMeasurementsByType(userId, type).firstOrNull() // Get first or null
            Result.Success(measurement)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}

class UpdateMeasurementUseCaseImpl @Inject constructor(
    private val repository: MeasurementRepository
) : UpdateMeasurementUseCase {
    override suspend fun execute(measurement: MeasurementRecord): Result<Unit> {
        return try {
            repository.updateMeasurement(measurement)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}