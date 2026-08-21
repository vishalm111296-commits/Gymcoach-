package com.gymcoach.app.presentation.measurement.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.domain.vshape.model.MeasurementRecord
import com.gymcoach.app.domain.vshape.model.MeasurementType
import com.gymcoach.app.domain.measurement.usecase.AddMeasurementUseCase
import com.gymcoach.app.domain.measurement.usecase.GetMeasurementsForUserUseCase
import com.gymcoach.app.domain.measurement.usecase.GetMeasurementTrendUseCase
import com.gymcoach.app.domain.measurement.usecase.UpdateMeasurementUseCase
import com.gymcoach.app.domain.repository.MeasurementRepository
import com.gymcoach.app.domain.repository.MeasurementTrend
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeasurementViewModel @Inject constructor(
    private val addMeasurementUseCase: AddMeasurementUseCase,
    private val getMeasurementsForUserUseCase: GetMeasurementsForUserUseCase,
    private val getMeasurementTrendUseCase: GetMeasurementTrendUseCase,
    private val updateMeasurementUseCase: UpdateMeasurementUseCase,
    private val measurementRepository: MeasurementRepository
) : ViewModel() {

    private val _measurements = MutableStateFlow<List<MeasurementRecord>>(emptyList())
    val measurements: StateFlow<List<MeasurementRecord>> = _measurements.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _trends = MutableStateFlow<Map<MeasurementType, MeasurementTrend>>(emptyMap())
    val trends: StateFlow<Map<MeasurementType, MeasurementTrend>> = _trends.asStateFlow()

    init {
        loadMeasurements()
    }

    fun refresh() {
        loadMeasurements()
    }

    private fun loadMeasurements() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                getMeasurementsForUserUseCase("default_user").collect { records ->
                    _measurements.value = records
                    loadTrends()
                }
            } catch (e: Exception) {
                _error.value = "Failed to load measurements"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadTrends() {
        for (type in MeasurementType.values()) {
            viewModelScope.launch {
                try {
                    getMeasurementTrendUseCase("default_user", type).collect { trend ->
                        _trends.value = _trends.value + (type to trend)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun addMeasurement(record: MeasurementRecord) {
        viewModelScope.launch {
            try {
                addMeasurementUseCase("default_user", record.measurementType, record.value, record.notes)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add measurement"
            }
            loadMeasurements()
        }
    }

    fun deleteMeasurement(record: MeasurementRecord) {
        viewModelScope.launch {
            try {
                measurementRepository.deleteMeasurement(record.id)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete measurement"
            }
            loadMeasurements()
        }
    }

    fun updateMeasurement(record: MeasurementRecord) {
        viewModelScope.launch {
            updateMeasurementUseCase(record)
            loadMeasurements()
        }
    }
}
