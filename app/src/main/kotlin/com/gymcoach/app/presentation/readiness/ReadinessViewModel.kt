package com.gymcoach.app.presentation.readiness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.data.local.entity.ReadinessEntity
import com.gymcoach.app.domain.repository.ReadinessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReadinessUiState(
    val latestReadiness: ReadinessEntity? = null,
    val recentReadiness: List<ReadinessEntity> = emptyList(),
    val isLoading: Boolean = true,
    val showDialog: Boolean = false,
    // Form state for logging
    val sleepQuality: Int = 3,
    val soreness: Int = 3,
    val energy: Int = 3,
    val motivation: Int = 3,
    val notes: String = ""
)

@HiltViewModel
class ReadinessViewModel @Inject constructor(
    private val readinessRepository: ReadinessRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReadinessUiState())
    val uiState: StateFlow<ReadinessUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Load latest readiness
            val latest = readinessRepository.getLatestReadiness().first()
            _uiState.value = _uiState.value.copy(latestReadiness = latest)
            
            // Load last 7 days
            val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            val recent = readinessRepository.getRecentReadiness(weekAgo).first()
            _uiState.value = _uiState.value.copy(
                recentReadiness = recent,
                isLoading = false
            )
        }
    }

    fun showLogDialog() {
        // Pre-fill with latest values if available
        val latest = _uiState.value.latestReadiness
        _uiState.value = _uiState.value.copy(
            showDialog = true,
            sleepQuality = latest?.sleepQuality ?: 3,
            soreness = latest?.soreness ?: 3,
            energy = latest?.energy ?: 3,
            motivation = latest?.motivation ?: 3,
            notes = latest?.notes ?: ""
        )
    }

    fun hideLogDialog() {
        _uiState.value = _uiState.value.copy(showDialog = false)
    }

    fun setSleepQuality(value: Int) {
        _uiState.value = _uiState.value.copy(sleepQuality = value.coerceIn(1, 5))
    }

    fun setSoreness(value: Int) {
        _uiState.value = _uiState.value.copy(soreness = value.coerceIn(1, 5))
    }

    fun setEnergy(value: Int) {
        _uiState.value = _uiState.value.copy(energy = value.coerceIn(1, 5))
    }

    fun setMotivation(value: Int) {
        _uiState.value = _uiState.value.copy(motivation = value.coerceIn(1, 5))
    }

    fun setNotes(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    fun saveReadiness() {
        viewModelScope.launch {
            val state = _uiState.value
            val readiness = ReadinessEntity(
                sleepQuality = state.sleepQuality,
                soreness = state.soreness,
                energy = state.energy,
                motivation = state.motivation,
                notes = state.notes
            )
            readinessRepository.saveReadiness(readiness)
            _uiState.value = _uiState.value.copy(showDialog = false)
            load() // Refresh data
        }
    }
}
