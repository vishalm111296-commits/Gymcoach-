package com.gymcoach.app.presentation.program

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.program.DeloadEngine
import com.gymcoach.app.core.program.DeloadStatus
import com.gymcoach.app.domain.repository.ProgramRepository
import com.gymcoach.app.domain.repository.ReadinessRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeloadPeriodizationUiState(
    val isLoading: Boolean = true,
    val currentBlockWeek: Int = 1,
    val totalBlockWeeks: Int = 4,
    val programName: String = "",
    val readinessAvg: Int = 75,
    val deloadStatus: DeloadStatus? = null,
    val isDeloadActive: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DeloadPeriodizationViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val readinessRepository: ReadinessRepository,
    private val programRepository: ProgramRepository,
    private val deloadEngine: DeloadEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeloadPeriodizationUiState())
    val uiState: StateFlow<DeloadPeriodizationUiState> = _uiState.asStateFlow()

    init {
        loadPeriodizationData()
    }

    fun loadPeriodizationData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // 1. Fetch active program
                val activeProgram = programRepository.getActiveProgram().firstOrNull()
                val programName = activeProgram?.name ?: "Current Program"

                // 2. Determine current week (simplified for now: infer from workouts or hardcode to 4 for demo purposes to trigger deload)
                val workouts = workoutRepository.getCompletedWorkouts().firstOrNull() ?: emptyList()
                val workoutHistory = workouts.mapNotNull {
                    workoutRepository.getWorkoutWithDetails(it.id).firstOrNull()
                }

                // Simplified week calculation based on completed workouts (assuming 3-4 workouts per week)
                val currentWeek = maxOf(1, (workouts.size / 4) + 1)

                // 3. Fetch readiness scores. ReadinessEntity uses readinessScore property (1.0-5.0 scale), convert to 0-100 scale
                val readinessList = readinessRepository.getAllReadiness().firstOrNull() ?: emptyList()
                // Convert 1.0-5.0 score to 0-100 for the engine
                val readinessScores = readinessList.map { (it.readinessScore * 20).toInt() }
                val avgReadiness = if (readinessScores.isNotEmpty()) {
                    readinessScores.takeLast(3).average().toInt()
                } else 75

                // 4. Evaluate Deload Need
                val status = deloadEngine.evaluateDeloadNeed(
                    workoutHistory = workoutHistory,
                    readinessScores = readinessScores,
                    currentWeekInBlock = currentWeek
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentBlockWeek = currentWeek,
                        totalBlockWeeks = 4,
                        programName = programName,
                        readinessAvg = avgReadiness,
                        deloadStatus = status,
                        isDeloadActive = false
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load periodization data: ${e.message}"
                    )
                }
            }
        }
    }

    fun toggleDeloadProtocol() {
        _uiState.update { it.copy(isDeloadActive = !it.isDeloadActive) }
    }
}
