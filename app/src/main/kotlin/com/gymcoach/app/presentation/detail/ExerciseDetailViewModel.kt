package com.gymcoach.app.presentation.detail
import com.gymcoach.app.domain.model.HistoricalSet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.exercise.SubstitutionEngine

import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.repository.ExerciseRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel

class ExerciseDetailViewModel @Inject constructor(
    private val repository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val substitutionEngine: SubstitutionEngine
) : ViewModel() {

    private val _exercise = MutableStateFlow<Exercise?>(null)
    val exercise: StateFlow<Exercise?> = _exercise.asStateFlow()

    private val _substitutes = MutableStateFlow<List<SubstitutionEngine.SubstitutionResult>>(emptyList())
    val substitutes: StateFlow<List<SubstitutionEngine.SubstitutionResult>> = _substitutes.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _previousSets = MutableStateFlow<List<HistoricalSet>>(emptyList())
    val previousSets: StateFlow<List<HistoricalSet>> = _previousSets.asStateFlow()

    fun loadExercise(id: Long) {
        viewModelScope.launch {
            repository.getExerciseById(id).collect { ex ->
                _exercise.value = ex
                ex?.let {
                    _isFavorite.value = it.isFavorite
                    loadSubstitutes(it)
                    loadPreviousPerformance(it.id)
                }
            }
        }
    }

    private suspend fun loadSubstitutes(exercise: Exercise) {
        try {
            val results = substitutionEngine.findSubstitutes(
                exerciseId = exercise.id,
                equipmentType = exercise.equipment,
                maxResults = 5
            )
            _substitutes.value = results
        } catch (e: Exception) {
            _substitutes.value = emptyList()
        }
    }

    private suspend fun loadPreviousPerformance(exerciseId: Long) {
        try {
            val sets = workoutRepository.getLastSetsForExercise(exerciseId)
            _previousSets.value = sets
        } catch (e: Exception) {
            _previousSets.value = emptyList()
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val ex = _exercise.value ?: return@launch
            val updated = ex.copy(isFavorite = !ex.isFavorite)
            repository.updateExercise(updated)
            _isFavorite.value = updated.isFavorite
        }
    }
}
