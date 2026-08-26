package com.gymcoach.app.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.progression.ProgressionEngine
import com.gymcoach.app.core.progression.ProgressionEngine.ProgressionRecommendation
import com.gymcoach.app.core.timer.RestPresets
import com.gymcoach.app.core.timer.RestTimerManager
import com.gymcoach.app.core.timer.RestTimerState
import com.gymcoach.app.data.local.dao.LastPerformance
import com.gymcoach.app.data.local.dao.LastSetData
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.model.SetType
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutExerciseWithSets
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutWithDetails
import com.gymcoach.app.domain.repository.ExerciseRepository
import com.gymcoach.app.domain.repository.UserProfileRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow



import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class WorkoutLoggingViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val userProfileRepository: UserProfileRepository,
    private val restTimer: RestTimerManager,
    private val progressionEngine: ProgressionEngine
) : ViewModel() {

    private var defaultRestSeconds = 90

    val allExercises = exerciseRepository.getAllExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile = userProfileRepository.getLatestProfile()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _currentWorkout = MutableStateFlow<WorkoutWithDetails?>(null)
    val currentWorkout: StateFlow<WorkoutWithDetails?> = _currentWorkout.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private var workoutTimerJob: kotlinx.coroutines.Job? = null

    private val _showExercisePicker = MutableStateFlow(false)
    val showExercisePicker: StateFlow<Boolean> = _showExercisePicker.asStateFlow()

    private val _completed = MutableStateFlow(false)
    val completed: StateFlow<Boolean> = _completed.asStateFlow()

    val restTimerState: StateFlow<RestTimerState> = restTimer.state

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Previous performance: exerciseId -> last sets data
    private val _previousPerformance = MutableStateFlow<Map<Long, List<LastSetData>>>(emptyMap())
    val previousPerformance: StateFlow<Map<Long, List<LastSetData>>> = _previousPerformance.asStateFlow()

    // Previous performance: exerciseId -> last performance summary
    private val _lastPerformanceSummary = MutableStateFlow<Map<Long, LastPerformance>>(emptyMap())
    val lastPerformanceSummary: StateFlow<Map<Long, LastPerformance>> = _lastPerformanceSummary.asStateFlow()

    // Progression recommendations: exerciseId -> recommendation
    private val _progressionRecommendations = MutableStateFlow<Map<Long, ProgressionRecommendation>>(emptyMap())
    val progressionRecommendations: StateFlow<Map<Long, ProgressionRecommendation>> = _progressionRecommendations.asStateFlow()

    // Accumulated volume for the current workout session
    private val _sessionVolume = MutableStateFlow(0.0)
    val sessionVolume: StateFlow<Double> = _sessionVolume.asStateFlow()

    fun dismissError() {
        _error.value = null
    }

    fun loadOrStartWorkout(workoutId: Long? = null) {
        viewModelScope.launch {
            try {
                if (workoutId != null) {
                    workoutRepository.getWorkoutWithDetails(workoutId).collect {
                        _currentWorkout.value = it
                        loadPreviousPerformanceForExercises(it?.exercises ?: emptyList())
                        calculateSessionVolume(it)
                        startWorkoutTimer()
                    }
                } else {
                    val existing = workoutRepository.getLatestIncompleteWorkout()
                    if (existing != null) {
                        workoutRepository.getWorkoutWithDetails(existing.id).collect {
                            _currentWorkout.value = it
                            loadPreviousPerformanceForExercises(it?.exercises ?: emptyList())
                            calculateSessionVolume(it)
                            startWorkoutTimer()
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

    /**
     * Load previous performance data for all exercises in the current workout.
     * Shows what the user did last time for each exercise.
     */
    private suspend fun loadPreviousPerformanceForExercises(
        exercises: List<WorkoutExerciseWithSets>
    ) {
        val perfMap = mutableMapOf<Long, List<LastSetData>>()
        val summaryMap = mutableMapOf<Long, LastPerformance>()

        for (we in exercises) {
            val exerciseId = we.exercise.id
            try {
                val lastSets = workoutRepository.getLastSetsForExercise(exerciseId)
                if (lastSets.isNotEmpty()) {
                    perfMap[exerciseId] = lastSets
                }
                val lastPerf = workoutRepository.getLastPerformanceForExercise(exerciseId)
                if (lastPerf != null) {
                    summaryMap[exerciseId] = lastPerf
                }
            } catch (_: Exception) {
                // Silently skip if query fails (e.g., no history yet)
            }
        }

        _previousPerformance.value = perfMap
        _lastPerformanceSummary.value = summaryMap

        // Calculate initial progression recommendations
        calculateProgressionRecommendations(exercises)
    }

    /** Calculate accumulated volume (weight × reps) for the current session. */
    private fun calculateSessionVolume(workout: WorkoutWithDetails?) {
        if (workout == null) {
            _sessionVolume.value = 0.0
            return
        }
        var volume = 0.0
        for (we in workout.exercises) {
            for (set in we.sets) {
                if (set.completed && set.weight > 0 && set.reps > 0) {
                    volume += set.weight * set.reps
                }
            }
        }
        _sessionVolume.value = volume
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
            status = "ACTIVE"
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
            // Load previous performance for newly added exercise
            val lastSets = workoutRepository.getLastSetsForExercise(exercise.id)
            if (lastSets.isNotEmpty()) {
                _previousPerformance.value = _previousPerformance.value + (exercise.id to lastSets)
            }
            val lastPerf = workoutRepository.getLastPerformanceForExercise(exercise.id)
            if (lastPerf != null) {
                _lastPerformanceSummary.value = _lastPerformanceSummary.value + (exercise.id to lastPerf)
            }
            // Calculate progression for new exercise
            val refreshed = _currentWorkout.value
            if (refreshed != null) {
                calculateProgressionRecommendations(refreshed.exercises)
            }
        }
    }

    /**
     * Add a new set. If previous performance exists, pre-fill weight and reps.
     * The first new set copies from the last completed set of the same exercise.
     */
    fun addSet(exerciseIndex: Int) {
        val workout = _currentWorkout.value ?: return
        if (exerciseIndex !in workout.exercises.indices) return
        val we = workout.exercises[exerciseIndex]
        val exerciseId = we.exercise.id
        val nextSetNumber = (we.sets.maxOfOrNull { it.setNumber } ?: 0) + 1

        // Auto-populate from previous session if available
        val lastSets = _previousPerformance.value[exerciseId]
        val prefilledWeight: Double
        val prefilledReps: Int
        val prefilledRest: Int

        if (lastSets != null && lastSets.isNotEmpty()) {
            // Use the last set's data as default
            val lastSet = lastSets.last()
            prefilledWeight = lastSet.weight
            prefilledReps = lastSet.reps
            prefilledRest = lastSet.restSeconds.takeIf { it > 0 } ?: defaultRestSeconds
        } else {
            prefilledWeight = 0.0
            prefilledReps = 0
            prefilledRest = defaultRestSeconds
        }

        val newSet = WorkoutSet(
            workoutExerciseId = we.workoutExercise.id,
            setNumber = nextSetNumber,
            weight = prefilledWeight,
            reps = prefilledReps,
            rpe = 0.0,
            restSeconds = prefilledRest,
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

    fun updateSetType(exerciseIndex: Int, setIndex: Int, setType: SetType) {
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
            // Remove progression recommendation for removed exercise
            val updated = _progressionRecommendations.value - we.exercise.id
            _progressionRecommendations.value = updated
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
            // Recalculate session volume
            val refreshed = _currentWorkout.value
            calculateSessionVolume(refreshed)
            // Recalculate progression recommendations after set completion change
            if (refreshed != null) {
                calculateProgressionRecommendations(refreshed.exercises)
            }
        }
        if (updated.completed) {
            // Use the recommended rest time based on RPE and set type
            val recommendedRest = RestPresets.recommended(set.setType, set.rpe)
            val restSeconds = if (set.restSeconds > 0) set.restSeconds else recommendedRest
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

    /** Change the rest timer duration while it's running (e.g., user taps a preset). */
    fun changeRestTimerDuration(seconds: Int) {
        restTimer.restart(seconds, viewModelScope)
    }

    fun completeWorkout() {
        val workout = _currentWorkout.value?.workout ?: return
        // Terminal-state guard: refuse to complete an already-completed,
        // abandoned, or otherwise terminal workout.
        if (workout.completed || workout.status == "COMPLETED" || workout.status == "ABANDONED") return
        workoutTimerJob?.cancel()
        restTimer.stop()
        val now = Instant.now()
        val duration = now.epochSecond - workout.startTime.epochSecond
        val updated = workout.copy(
            endTime = now,
            duration = duration,
            completed = true,
            status = "COMPLETED"
        )
        viewModelScope.launch {
            workoutRepository.updateWorkout(updated)
            _completed.value = true
        }
    }

    /**
     * Calculate progression recommendations for all exercises in the workout.
     * Uses ProgressionEngine with double progression logic.
     */
    private fun calculateProgressionRecommendations(exercises: List<WorkoutExerciseWithSets>) {
        val recommendations = mutableMapOf<Long, ProgressionRecommendation>()
        for (we in exercises) {
            val exercise = we.exercise
            val normalSets = we.sets.filter { it.completed && it.setType == SetType.NORMAL }
            if (normalSets.isNotEmpty()) {
                val lastSets = _previousPerformance.value[exercise.id] ?: emptyList()
                val recommendation = progressionEngine.calculateProgression(
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    exerciseEquipment = exercise.equipment,
                    targetRepsMin = 8,
                    targetRepsMax = 12,
                    targetSets = 3,
                    previousSets = lastSets.map { WorkoutSetEntity(workoutExerciseId = 0, setNumber = 0, weight = it.weight, reps = it.reps, rpe = it.rpe, restSeconds = it.restSeconds, completed = true, setType = it.setType) },
                    currentSets = normalSets.map { it.toEntity() },
                    equipmentType = userProfile.value?.equipmentType ?: "home"
                )
                recommendations[exercise.id] = recommendation
            }
        }
        _progressionRecommendations.value = recommendations
    }

    private fun updateSetField(exerciseIndex: Int, setIndex: Int, transform: (WorkoutSet) -> WorkoutSet) {
        val workout = _currentWorkout.value ?: return
        if (exerciseIndex !in workout.exercises.indices) return
        val we = workout.exercises[exerciseIndex]
        if (setIndex !in we.sets.indices) return
        val updated = transform(we.sets[setIndex])
        viewModelScope.launch {
            workoutRepository.updateSet(updated)
            // Recalculate session volume after any field update
            calculateSessionVolume(_currentWorkout.value)
            // Recalculate progression recommendations
            val refreshed = _currentWorkout.value
            if (refreshed != null) {
                calculateProgressionRecommendations(refreshed.exercises)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        workoutTimerJob?.cancel()
        restTimer.stop()
    }
}