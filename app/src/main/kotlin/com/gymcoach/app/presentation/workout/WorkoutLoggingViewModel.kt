package com.gymcoach.app.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.timer.RestTimerManager
import com.gymcoach.app.core.timer.RestTimerState
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutExerciseWithSets
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutWithDetails
import com.gymcoach.app.domain.repository.ExerciseRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class WorkoutLoggingViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val restTimer: RestTimerManager
) : ViewModel() {

    private var defaultRestSeconds = 90

    val allExercises = exerciseRepository.getAllExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentWorkout = MutableStateFlow<WorkoutWithDetails?>(null)
    val currentWorkout: StateFlow<WorkoutWithDetails?> = _currentWorkout.asStateFlow()

    private val _showExercisePicker = MutableStateFlow(false)
    val showExercisePicker: StateFlow<Boolean> = _showExercisePicker.asStateFlow()

    private val _completed = MutableStateFlow(false)
    val completed: StateFlow<Boolean> = _completed.asStateFlow()

    val restTimerState: StateFlow<RestTimerState> = restTimer.state

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun dismissError() {
        _error.value = null
    }

    fun loadOrStartWorkout(workoutId: Long? = null) {
        viewModelScope.launch {
            try {
                if (workoutId != null) {
                    workoutRepository.getWorkoutWithDetails(workoutId).collect {
                        _currentWorkout.value = it
                    }
                } else {
                    val existing = workoutRepository.getLatestIncompleteWorkout()
                    if (existing != null) {
                        workoutRepository.getWorkoutWithDetails(existing.id).collect {
                            _currentWorkout.value = it
                        }
                    } else {
                        startNewWorkoutInternal()
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load workout"
            }
        }
    }

    fun startNewWorkout() {
        viewModelScope.launch {
            try {
                startNewWorkoutInternal()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to start workout"
            }
        }
    }

    private suspend fun startNewWorkoutInternal() {
        val now = Instant.now()
        val workout = Workout(
            date = now,
            startTime = now,
            endTime = now,
            duration = 0,
            notes = "",
            completed = false
        )
        val id = workoutRepository.createWorkout(workout)
        workoutRepository.getWorkoutWithDetails(id).collect {
            _currentWorkout.value = it
        }
    }

    fun updateNotes(notes: String) {
        val current = _currentWorkout.value?.workout ?: return
        viewModelScope.launch {
            try {
                workoutRepository.updateWorkout(current.copy(notes = notes))
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update notes"
            }
        }
    }

    fun updateSetRpe(exerciseIndex: Int, setIndex: Int, rpe: Double) {
        updateSetField(exerciseIndex, setIndex) { it.copy(rpe = rpe) }
    }

    fun showExercisePicker() {
        _showExercisePicker.value = true
    }

    fun hideExercisePicker() {
        _showExercisePicker.value = false
    }

    fun addExerciseToWorkout(exercise: Exercise) {
        val workout = _currentWorkout.value ?: return
        val nextOrder = (workout.exercises.maxOfOrNull { it.workoutExercise.orderIndex } ?: -1) + 1
        viewModelScope.launch {
            workoutRepository.addExerciseToWorkout(workout.workout.id, exercise.id, nextOrder)
            _showExercisePicker.value = false
        }
    }

    fun addSet(exerciseIndex: Int) {
        val workout = _currentWorkout.value ?: return
        if (exerciseIndex !in workout.exercises.indices) return
        val we = workout.exercises[exerciseIndex]
        val nextSetNumber = (we.sets.maxOfOrNull { it.setNumber } ?: 0) + 1
        val newSet = WorkoutSet(
            workoutExerciseId = we.workoutExercise.id,
            setNumber = nextSetNumber,
            weight = 0.0,
            reps = 0,
            rpe = 0.0,
            restSeconds = 0,
            completed = false
        )
        viewModelScope.launch {
            workoutRepository.addSetToExercise(we.workoutExercise.id, newSet)
        }
    }

    fun updateSetReps(exerciseIndex: Int, setIndex: Int, reps: Int) {
        updateSetField(exerciseIndex, setIndex) { it.copy(reps = reps) }
    }

    fun updateSetWeight(exerciseIndex: Int, setIndex: Int, weight: Double) {
        updateSetField(exerciseIndex, setIndex) { it.copy(weight = weight) }
    }

    fun updateSetRestSeconds(exerciseIndex: Int, setIndex: Int, restSeconds: Int) {
        updateSetField(exerciseIndex, setIndex) { it.copy(restSeconds = restSeconds) }
    }

    fun updateSetType(exerciseIndex: Int, setIndex: Int, setType: com.gymcoach.app.domain.model.SetType) {
        updateSetField(exerciseIndex, setIndex) { it.copy(setType = setType) }
    }

    fun removeSet(exerciseIndex: Int, setIndex: Int) {
        val workout = _currentWorkout.value ?: return
        if (exerciseIndex !in workout.exercises.indices) return
        val we = workout.exercises[exerciseIndex]
        if (setIndex !in we.sets.indices) return
        val set = we.sets[setIndex]
        viewModelScope.launch {
            workoutRepository.deleteSet(set.id)
        }
    }

    fun removeExercise(exerciseIndex: Int) {
        val workout = _currentWorkout.value ?: return
        if (exerciseIndex !in workout.exercises.indices) return
        val we = workout.exercises[exerciseIndex]
        viewModelScope.launch {
            workoutRepository.removeExerciseFromWorkout(we.workoutExercise.id)
        }
    }

    fun toggleSetCompletion(exerciseIndex: Int, setIndex: Int) {
        val workout = _currentWorkout.value ?: return
        if (exerciseIndex !in workout.exercises.indices) return
        val we = workout.exercises[exerciseIndex]
        if (setIndex !in we.sets.indices) return
        val set = we.sets[setIndex]
        val updated = set.copy(completed = !set.completed)
        viewModelScope.launch {
            workoutRepository.updateSet(updated)
        }
        if (updated.completed) {
            val restSeconds = if (updated.restSeconds > 0) updated.restSeconds else defaultRestSeconds
            restTimer.start(restSeconds, viewModelScope)
        } else {
            restTimer.stop()
        }
    }

    fun pauseRestTimer() {
        restTimer.pause()
    }

    fun resumeRestTimer() {
        restTimer.resume()
    }

    fun stopRestTimer() {
        restTimer.stop()
    }

    fun completeWorkout() {
        val workout = _currentWorkout.value?.workout ?: return
        restTimer.stop()
        val now = Instant.now()
        val duration = now.epochSecond - workout.startTime.epochSecond
        val updated = workout.copy(endTime = now, duration = duration, completed = true)
        viewModelScope.launch {
            workoutRepository.updateWorkout(updated)
            _completed.value = true
        }
    }

    private fun updateSetField(exerciseIndex: Int, setIndex: Int, transform: (WorkoutSet) -> WorkoutSet) {
        val workout = _currentWorkout.value ?: return
        if (exerciseIndex !in workout.exercises.indices) return
        val we = workout.exercises[exerciseIndex]
        if (setIndex !in we.sets.indices) return
        val updated = transform(we.sets[setIndex])
        viewModelScope.launch {
            workoutRepository.updateSet(updated)
        }
    }

    override fun onCleared() {
        super.onCleared()
        restTimer.stop()
    }
}