package com.gymcoach.app.presentation.readiness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.data.local.entity.ReadinessEntity
import com.gymcoach.app.domain.repository.ReadinessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReadinessUiState(
    val latestReadiness: ReadinessEntity? = null,
    val recentReadiness: List<ReadinessEntity> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
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

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Load latest readiness
                val latest = readinessRepository.getLatestReadiness().first()
                
                // Load last 7 days
                val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                val recent = readinessRepository.getRecentReadiness(weekAgo).first()
                
                _uiState.update {
                    it.copy(
                        latestReadiness = latest,
                        recentReadiness = recent,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load readiness data"
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun showLogDialog() {
        val latest = _uiState.value.latestReadiness
        _uiState.update {
            it.copy(
                showDialog = true,
                sleepQuality = latest?.sleepQuality ?: 3,
                soreness = latest?.soreness ?: 3,
                energy = latest?.energy ?: 3,
                motivation = latest?.motivation ?: 3,
                notes = latest?.notes ?: ""
            )
        }
    }

    fun hideLogDialog() {
        _uiState.update { it.copy(showDialog = false) }
    }

    fun setSleepQuality(value: Int) {
        _uiState.update { it.copy(sleepQuality = value.coerceIn(1, 5)) }
    }

    fun setSoreness(value: Int) {
        _uiState.update { it.copy(soreness = value.coerceIn(1, 5)) }
    }

    fun setEnergy(value: Int) {
        _uiState.update { it.copy(energy = value.coerceIn(1, 5)) }
    }

    fun setMotivation(value: Int) {
        _uiState.update { it.copy(motivation = value.coerceIn(1, 5)) }
    }

    fun setNotes(value: String) {
        _uiState.update { it.copy(notes = value) }
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
            try {
                readinessRepository.saveReadiness(readiness)
                _uiState.update { it.copy(showDialog = false) }
                load() // Refresh data
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to save readiness")
                }
            }
        }
    }
}
