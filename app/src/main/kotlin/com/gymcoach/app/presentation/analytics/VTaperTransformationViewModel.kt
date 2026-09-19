package com.gymcoach.app.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.analytics.VTaperReport
import com.gymcoach.app.core.analytics.VTaperTransformationEngine
import com.gymcoach.app.data.local.dao.BodyMeasurementDao
import com.gymcoach.app.data.local.entity.BodyMeasurementEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class VTaperUiState {
    object Loading : VTaperUiState()
    object Empty : VTaperUiState()
    data class Success(val report: VTaperReport) : VTaperUiState()
    data class Error(val message: String) : VTaperUiState()
}

@HiltViewModel
class VTaperTransformationViewModel @Inject constructor(
    private val measurementDao: BodyMeasurementDao,
    private val transformationEngine: VTaperTransformationEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow<VTaperUiState>(VTaperUiState.Loading)
    val uiState: StateFlow<VTaperUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            measurementDao.getAll()
                .catch { e ->
                    _uiState.value = VTaperUiState.Error(e.message ?: "Failed to load measurements")
                }
                .collect { measurements ->
                    if (measurements.isEmpty()) {
                        _uiState.value = VTaperUiState.Empty
                    } else {
                        val report = transformationEngine.calculateReport(measurements)
                        _uiState.value = VTaperUiState.Success(report)
                    }
                }
        }
    }

    fun saveMeasurement(measurement: BodyMeasurementEntity) {
        viewModelScope.launch {
            try {
                if (measurement.id == 0L) {
                    measurementDao.insert(measurement)
                } else {
                    measurementDao.update(measurement)
                }
            } catch (e: Exception) {
                // In a real app we might show a one-off event/snackbar
            }
        }
    }

    fun deleteMeasurement(id: Long) {
        viewModelScope.launch {
            try {
                measurementDao.deleteById(id)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
