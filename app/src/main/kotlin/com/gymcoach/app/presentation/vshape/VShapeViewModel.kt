package com.gymcoach.app.presentation.vshape

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.domain.Result
import com.gymcoach.app.domain.vshape.model.MeasurementRecord
import com.gymcoach.app.domain.vshape.model.MeasurementType
import com.gymcoach.app.domain.measurement.usecase.GetLatestMeasurementUseCase
import com.gymcoach.app.domain.measurement.usecase.SaveMeasurementUseCase
import com.gymcoach.app.domain.measurement.usecase.ValidateMeasurementUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class VShapeViewModel @Inject constructor(
    private val saveMeasurementUseCase: SaveMeasurementUseCase,
    private val getLatestMeasurementUseCase: GetLatestMeasurementUseCase,
    private val validateMeasurementUseCase: ValidateMeasurementUseCase
) : ViewModel() {

    private val _latestMeasurement = MutableStateFlow<MeasurementRecord?>(null)
    val latestMeasurement: StateFlow<MeasurementRecord?> = _latestMeasurement

    private val _userId = "current_user"

    init {
        loadLatestMeasurement()
    }

    private fun loadLatestMeasurement() {
        viewModelScope.launch {
            when (val result = getLatestMeasurementUseCase.execute(_userId, MeasurementType.WEIGHT)) {
                is Result.Success -> _latestMeasurement.value = result.value
                is Result.Failure -> _latestMeasurement.value = null
            }
        }
    }

    fun onRecordMeasurement(
        measurementType: MeasurementType,
        value: Double,
        unit: String,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val record = MeasurementRecord(
                id = 0,
                userId = _userId,
                measurementType = measurementType,
                value = value,
                unit = unit,
                date = Instant.now(),
                notes = notes
            )

            val validationResult = validateMeasurementUseCase.execute(record)
            if (!validationResult.isValid) return@launch

            when (val result = saveMeasurementUseCase.execute(record)) {
                is Result.Success -> loadLatestMeasurement()
                is Result.Failure -> { /* swallow for now; surface later via UI state */ }
            }
        }
    }
}
