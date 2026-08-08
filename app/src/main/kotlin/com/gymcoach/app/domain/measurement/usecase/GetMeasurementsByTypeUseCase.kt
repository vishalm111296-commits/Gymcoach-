package com.gymcoach.app.domain.measurement.usecase

import com.gymcoach.app.domain.repository.MeasurementRepository
import com.gymcoach.app.domain.vshape.model.MeasurementRecord
import com.gymcoach.app.domain.vshape.model.MeasurementType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMeasurementsByTypeUseCase @Inject constructor(
    private val repository: MeasurementRepository
) {
    operator fun invoke(userId: String, type: MeasurementType): Flow<List<MeasurementRecord>> {
        return repository.getMeasurementsByType(userId, type)
    }
}
