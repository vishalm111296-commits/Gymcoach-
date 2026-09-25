package com.gymcoach.app.presentation.gamification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.gamification.StreakAndAchievementEngine
import com.gymcoach.app.core.gamification.StreakReport
import com.gymcoach.app.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class StreakUiState {
    object Loading : StreakUiState()
    data class Success(val report: StreakReport) : StreakUiState()
    object Empty : StreakUiState()
}

@HiltViewModel
class StreakAndAchievementViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val streakEngine: StreakAndAchievementEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow<StreakUiState>(StreakUiState.Loading)
    val uiState: StateFlow<StreakUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                combine(
                    workoutRepository.getCompletedWorkouts(),
                    workoutRepository.getCompletedSetsWithContext()
                ) { workouts, setsWithContext ->
                    if (workouts.isEmpty()) {
                        StreakUiState.Empty
                    } else {
                        val report = streakEngine.calculateReport(workouts, setsWithContext)
                        StreakUiState.Success(report)
                    }
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = StreakUiState.Empty
            }
        }
    }
}
