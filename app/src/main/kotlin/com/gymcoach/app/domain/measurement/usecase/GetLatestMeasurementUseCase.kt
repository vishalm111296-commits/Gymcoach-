package com.gymcoach.app.domain.measurement.usecase

import com.gymcoach.app.domain.repository.MeasurementRepository
import com.gymcoach.app.domain.vshape.model.MeasurementRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLatestMeasurementUseCase @Inject constructor(
    private val repository: MeasurementRepository
) {
    operator fun invoke(userId: String): Flow<MeasurementRecord?> {
        return repository.getLatestMeasurementForUser(userId)
    }
}
