package com.gymcoach.app.presentation.measurement.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.domain.vshape.model.MeasurementRecord
import com.gymcoach.app.domain.vshape.model.MeasurementType
import com.gymcoach.app.domain.measurement.usecase.AddMeasurementUseCase
import com.gymcoach.app.domain.measurement.usecase.GetMeasurementsForUserUseCase
import com.gymcoach.app.domain.measurement.usecase.GetMeasurementTrendUseCase
import com.gymcoach.app.domain.measurement.usecase.UpdateMeasurementUseCase
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
    private val updateMeasurementUseCase: UpdateMeasurementUseCase
) : ViewModel() {

    private val _measurements = MutableStateFlow<List<MeasurementRecord>>(emptyList())
    val measurements: StateFlow<List<MeasurementRecord>> = _measurements.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadMeasurements()
    }

    private fun loadMeasurements() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                getMeasurementsForUserUseCase("default_user").collect { records ->
                    _measurements.value = records
                }
            } catch (e: Exception) {
                _error.value = "Failed to load measurements"
            } finally {
                _isLoading.value = false
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

    fun updateMeasurement(record: MeasurementRecord) {
        viewModelScope.launch {
            updateMeasurementUseCase(record)
            loadMeasurements()
        }
    }
}
