package com.gymcoach.app.presentation.standards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.standards.OverallStrengthProfile
import com.gymcoach.app.core.standards.StrengthStandardsEngine
import com.gymcoach.app.data.local.dao.BodyMeasurementDao
import com.gymcoach.app.domain.repository.AnalyticsRepository
import com.gymcoach.app.domain.repository.PersonalRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class StrengthStandardsUiState {
    object Loading : StrengthStandardsUiState()
    data class Success(
        val profile: OverallStrengthProfile,
        val allPrs: List<PersonalRecord>
    ) : StrengthStandardsUiState()
    object Empty : StrengthStandardsUiState()
}

@HiltViewModel
class StrengthStandardsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val bodyMeasurementDao: BodyMeasurementDao,
    private val strengthStandardsEngine: StrengthStandardsEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow<StrengthStandardsUiState>(StrengthStandardsUiState.Loading)
    val uiState: StateFlow<StrengthStandardsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                _uiState.value = StrengthStandardsUiState.Loading

                val allPrs = analyticsRepository.getAllPersonalRecords()

                // Get one rep maxes from PRs
                val oneRepMaxes = allPrs.associate { it.exerciseName to it.maxWeight }

                // Get latest bodyweight
                val latestMeasurement = bodyMeasurementDao.getLatest().firstOrNull()
                val bodyweightKg = latestMeasurement?.weightKg ?: 0.0

                if (bodyweightKg <= 0.0) {
                    _uiState.value = StrengthStandardsUiState.Empty
                    return@launch
                }

                val profile = strengthStandardsEngine.evaluateProfile(bodyweightKg, oneRepMaxes)

                _uiState.value = StrengthStandardsUiState.Success(profile, allPrs)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = StrengthStandardsUiState.Empty
            }
        }
    }
}
