package com.gymcoach.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.data.local.entity.BodyMeasurementEntity
import com.gymcoach.app.data.local.entity.UserProfileEntity
import com.gymcoach.app.domain.repository.UserProfileRepository
import com.gymcoach.app.data.local.dao.BodyMeasurementDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: UserProfileEntity? = null,
    val latestMeasurement: BodyMeasurementEntity? = null,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val bodyMeasurementDao: BodyMeasurementDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userProfileRepository.getLatestProfile().collect { profile ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profile = profile
                )
            }
        }
        viewModelScope.launch {
            bodyMeasurementDao.getLatest().collect { measurement ->
                _uiState.value = _uiState.value.copy(
                    latestMeasurement = measurement
                )
            }
        }
    }

    fun saveMeasurement(measurement: BodyMeasurementEntity) {
        viewModelScope.launch {
            try {
                bodyMeasurementDao.insert(measurement)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
