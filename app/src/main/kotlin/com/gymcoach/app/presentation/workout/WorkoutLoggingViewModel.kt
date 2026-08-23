package com.gymcoach.app.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.timer.RestTimerManager
import com.gymcoach.app.core.timer.RestTimerState
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutExerciseWithSets
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutStatus
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

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private var workoutTimerJob: kotlinx.coroutines.Job? = null

    /** Single active DB observer; cancelled/replaced on every (re)load so collectors never stack. */
    private var workoutCollectionJob: kotlinx.coroutines.Job? = null

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
                    observeWorkout(workoutId)
                } else {
                    // v7 semantics: only genuinely-ACTIVE workouts are resumed.
                    // Empty/discarded rows are ABANDONED and never match,
                    // eliminating the phantom "Resume Workout" behavior.
                    val existing = workoutRepository.getLatestIncompleteWorkout()
                    if (existing != null && existing.status == WorkoutStatus.ACTIVE) {
                        observeWorkout(existing.id)
                    } else {
                        startNewWorkoutInternal()
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load workout"
            }
        }
    }

    private fun observeWorkout(workoutId: Long) {
        workoutCollectionJob?.cancel()
        workoutCollectionJob = viewModelScope.launch {
            workoutRepository.getWorkoutWithDetails(workoutId).collect {
                _currentWorkout.value = it
            }
        }
        startWorkoutTimer()
    }

    private fun startWorkoutTimer() {
        workoutTimerJob?.cancel()
        workoutTimerJob = viewModelScope.launch {
            while (true) {
                val start = _currentWorkout.value?.workout?.startTime
                if (start != null) {
                    _elapsedSeconds.value = Instant.now().epochSecond - start.epochSecond
                }
                kotlinx.coroutines.delay(1000L)
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
            completed = false,
            status = WorkoutStatus.ACTIVE
        )
        val id = workoutRepository.createWorkout(workout)
        observeWorkout(id)
    }

    /**
     * Explicit exit that is NOT completion.
     * - Zero exercises logged -> delete the row entirely (nothing was done).
     * - Any content present   -> mark ABANDONED (history shows it happened,
     *                            but it never auto-resumes).
     */
    fun discardWorkout() {
        val details = _currentWorkout.value ?: return
        workoutTimerJob?.cancel()
        restTimer.stop()
        viewModelScope.launch {
            try {
                if (details.exercises.isEmpty()) {
                    workoutRepository.deleteWorkout(details.workout.id)
                } else {
                    workoutRepository.updateWorkout(
                        details.workout.copy(status = WorkoutStatus.ABANDONED, completed = false)
                    )
                }
                clearSessionState()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to discard workout"
            }
        }
    }

    private fun clearSessionState() {
        workoutCollectionJob?.cancel()
        _currentWorkout.value = null
        _elapsedSeconds.value = 0L
        _completed.value = false
        _showExercisePicker.value = false
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
        updateSetField(exerciseIndex, setIndex) { it.copy(rpe = rpe.coerceIn(0.0, 10.0)) }
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
        updateSetField(exerciseIndex, setIndex) { it.copy(reps = reps.coerceAtLeast(0)) }
    }

    fun updateSetWeight(exerciseIndex: Int, setIndex: Int, weight: Double) {
        updateSetField(exerciseIndex, setIndex) { it.copy(weight = weight.coerceAtLeast(0.0)) }
    }

    fun updateSetRestSeconds(exerciseIndex: Int, setIndex: Int, restSeconds: Int) {
        updateSetField(exerciseIndex, setIndex) { it.copy(restSeconds = restSeconds.coerceAtLeast(0)) }
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
        workoutTimerJob?.cancel()
        restTimer.stop()
        val now = Instant.now()
        val duration = (now.epochSecond - workout.startTime.epochSecond).coerceAtLeast(0)
        val updated = workout.copy(
            endTime = now,
            duration = duration,
            completed = true,
            status = WorkoutStatus.COMPLETED
        )
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
        workoutTimerJob?.cancel()
        workoutCollectionJob?.cancel()
        restTimer.stop()
    }
}
