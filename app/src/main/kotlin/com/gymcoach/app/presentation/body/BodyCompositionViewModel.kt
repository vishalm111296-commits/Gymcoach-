package com.gymcoach.app.presentation.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.body.BodyCompositionEngine
import com.gymcoach.app.core.body.BodyMetricTrend
import com.gymcoach.app.data.local.entity.BodyMeasurementEntity
import com.gymcoach.app.domain.repository.BodyMeasurementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BodyCompositionUiState {
    object Loading : BodyCompositionUiState
    data class Success(val trend: BodyMetricTrend) : BodyCompositionUiState
    object Empty : BodyCompositionUiState
}

@HiltViewModel
class BodyCompositionViewModel @Inject constructor(
    private val repository: BodyMeasurementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BodyCompositionUiState>(BodyCompositionUiState.Loading)
    val uiState: StateFlow<BodyCompositionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllMeasurements().collectLatest { measurements ->
                if (measurements.isEmpty()) {
                    _uiState.update { BodyCompositionUiState.Empty }
                } else {
                    val trend = BodyCompositionEngine.calculateTrend(measurements)
                    _uiState.update { BodyCompositionUiState.Success(trend) }
                }
            }
        }
    }

    fun saveMeasurement(
        weightKg: Double,
        bodyFatPct: Double,
        chestCm: Double,
        waistCm: Double,
        shouldersCm: Double,
        armCm: Double,
        thighCm: Double,
        calfCm: Double,
        notes: String
    ) {
        viewModelScope.launch {
            val entity = BodyMeasurementEntity(
                weightKg = weightKg,
                bodyFatPct = bodyFatPct,
                chestCm = chestCm,
                waistCm = waistCm,
                shouldersCm = shouldersCm,
                leftArmCm = armCm,
                rightArmCm = armCm,
                leftThighCm = thighCm,
                rightThighCm = thighCm,
                leftCalfCm = calfCm,
                rightCalfCm = calfCm,
                notes = notes,
                recordedAt = System.currentTimeMillis()
            )
            repository.saveMeasurement(entity)
        }
    }

    fun deleteMeasurement(id: Long) {
        viewModelScope.launch {
            repository.deleteMeasurement(id)
        }
    }
}
