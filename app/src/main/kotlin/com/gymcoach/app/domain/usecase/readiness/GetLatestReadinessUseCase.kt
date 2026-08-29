package com.gymcoach.app.domain.usecase.readiness

import com.gymcoach.app.data.local.entity.ReadinessEntity
import com.gymcoach.app.domain.repository.ReadinessRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLatestReadinessUseCase @Inject constructor(
    private val readinessRepository: ReadinessRepository
) {
    operator fun invoke(): Flow<ReadinessEntity?> {
        return readinessRepository.getLatestReadiness()
    }
}
