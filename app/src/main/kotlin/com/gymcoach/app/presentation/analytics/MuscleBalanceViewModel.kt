package com.gymcoach.app.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.analytics.MuscleBalanceAnalyzer
import com.gymcoach.app.core.analytics.MuscleBalanceReport
import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.domain.repository.ExerciseRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

sealed class MuscleBalanceUiState {
    object Loading : MuscleBalanceUiState()
    data class Success(val report: MuscleBalanceReport) : MuscleBalanceUiState()
    object Empty : MuscleBalanceUiState()
}

@HiltViewModel
class MuscleBalanceViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val muscleBalanceAnalyzer: MuscleBalanceAnalyzer
) : ViewModel() {

    private val _uiState = MutableStateFlow<MuscleBalanceUiState>(MuscleBalanceUiState.Loading)
    val uiState: StateFlow<MuscleBalanceUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = MuscleBalanceUiState.Loading

            try {
                // Get past 30 days of completed sets
                val thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli()
                val sets = workoutRepository.getCompletedSetsWithContext().first()
                    .filter { it.workoutDate >= thirtyDaysAgo }

                if (sets.isEmpty()) {
                    _uiState.value = MuscleBalanceUiState.Empty
                    return@launch
                }

                val allExercises = exerciseRepository.getAllExercises().first()
                val exerciseMuscleMap = mutableMapOf<Long, List<VolumeCalculator.MuscleAssignment>>()

                for (exercise in allExercises) {
                    val assignments = mutableListOf<VolumeCalculator.MuscleAssignment>()
                    if (exercise.muscleGroup.isNotEmpty()) {
                        assignments.add(VolumeCalculator.MuscleAssignment(exercise.muscleGroup, VolumeCalculator.MuscleRole.PRIMARY))
                    }
                    if (exercise.secondaryMuscles.isNotEmpty()) {
                        exercise.secondaryMuscles.split(",").map { it.trim() }.forEach {
                            if (it.isNotEmpty()) {
                                assignments.add(VolumeCalculator.MuscleAssignment(it, VolumeCalculator.MuscleRole.SECONDARY))
                            }
                        }
                    }
                    exerciseMuscleMap[exercise.id] = assignments
                }

                val report = muscleBalanceAnalyzer.analyzeBalance(sets, exerciseMuscleMap)
                _uiState.value = MuscleBalanceUiState.Success(report)

            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = MuscleBalanceUiState.Empty
            }
        }
    }
}
