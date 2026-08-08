package com.gymcoach.app.domain.measurement.usecase

import com.gymcoach.app.domain.repository.MeasurementRepository
import com.gymcoach.app.domain.repository.MeasurementTrend
import com.gymcoach.app.domain.vshape.model.MeasurementType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMeasurementTrendUseCase @Inject constructor(
    private val repository: MeasurementRepository
) {
    operator fun invoke(userId: String, type: MeasurementType): Flow<MeasurementTrend> {
        return repository.getTrendForType(userId, type)
    }
}
