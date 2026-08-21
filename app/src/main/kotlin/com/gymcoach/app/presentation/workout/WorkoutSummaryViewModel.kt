package com.gymcoach.app.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.domain.model.WorkoutWithDetails
import com.gymcoach.app.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface WorkoutSummaryUiState {
    data object Loading : WorkoutSummaryUiState
    data class Success(val summary: WorkoutSummary) : WorkoutSummaryUiState
    data class Error(val message: String) : WorkoutSummaryUiState
}

@HiltViewModel
class WorkoutSummaryViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _workoutId = MutableStateFlow<Long?>(null)

    val workoutSummary = _workoutId.map { id ->
        if (id == null) WorkoutSummaryUiState.Loading
        else {
            try {
                val details = workoutRepository.getWorkoutWithDetailsNow(id)
                if (details != null) {
                    WorkoutSummaryUiState.Success(calculateMetrics(details))
                } else {
                    WorkoutSummaryUiState.Error("Workout not found")
                }
            } catch (e: Exception) {
                WorkoutSummaryUiState.Error(e.message ?: "Failed to load workout summary")
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorkoutSummaryUiState.Loading)

    fun setWorkoutId(id: Long) {
        _workoutId.value = id
    }

    private fun calculateMetrics(details: WorkoutWithDetails): WorkoutSummary {
        var totalVolume = 0.0
        var totalReps = 0
        var totalSets = 0
        var maxWeight = 0.0

        details.exercises.forEach { we ->
            we.sets.filter { it.completed }.forEach { set ->
                totalVolume += set.weight * set.reps
                totalReps += set.reps
                totalSets++
                if (set.weight > maxWeight) maxWeight = set.weight
            }
        }

        return WorkoutSummary(
            duration = details.workout.duration,
            exerciseCount = details.exercises.size,
            totalSets = totalSets,
            totalReps = totalReps,
            totalVolume = totalVolume,
            calories = (totalVolume * 0.1).toInt(),
            maxWeight = maxWeight
        )
    }
}

data class WorkoutSummary(
    val duration: Long,
    val exerciseCount: Int,
    val totalSets: Int,
    val totalReps: Int,
    val totalVolume: Double,
    val calories: Int,
    val maxWeight: Double
)
