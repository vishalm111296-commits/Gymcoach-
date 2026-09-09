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
import com.gymcoach.app.core.di.ApplicationScope
import com.gymcoach.app.domain.repository.ExerciseRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import com.gymcoach.app.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean



import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class WorkoutLoggingViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val restTimer: RestTimerManager,
    private val progressionEngine: ProgressionEngine,
    private val userProfileRepository: UserProfileRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ViewModel() {

    /** Sealed UI state for the workout session screen. */
    sealed interface SessionUiState {
        data object Loading : SessionUiState
        data object Empty : SessionUiState
        data class Active(val workout: WorkoutWithDetails) : SessionUiState
        data class Error(val message: String) : SessionUiState
    }

    private val _sessionUiState = MutableStateFlow<SessionUiState>(SessionUiState.Loading)
    val sessionUiState: StateFlow<SessionUiState> = _sessionUiState.asStateFlow()

    private var defaultRestSeconds = 90

    val allExercises = exerciseRepository.getAllExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    /** Serializes exercise additions to prevent duplicate inserts under concurrent taps. */
    private val exerciseAddMutex = Mutex()
    /** Serializes set additions to prevent duplicate set numbers under concurrent taps. */
    private val addSetMutex = Mutex()
    /** Serializes workout creation to prevent duplicate ACTIVE workouts under concurrent starts. */
    private val workoutCreationMutex = Mutex()
    /** Ensures completeWorkout() executes at most once (true atomicity via compareAndSet). */
    private val completionInProgress = AtomicBoolean(false)

    // Stats shown on the completion screen
    data class CompletionStats(
        val durationSeconds: Long = 0,
        val totalVolume: Double = 0.0,
        val totalSets: Int = 0,
        val totalReps: Int = 0,
        val exerciseCount: Int = 0
    )

    private val _completionStats = MutableStateFlow(CompletionStats())
    val completionStats: StateFlow<CompletionStats> = _completionStats.asStateFlow()

    fun dismissError() {
        _error.value = null
    }

    fun loadOrStartWorkout(workoutId: Long? = null) {
        _sessionUiState.value = SessionUiState.Loading
        viewModelScope.launch {
            try {
                if (workoutId != null) {
                    // Load the workout once to check its status
                    val snapshot = workoutRepository.getWorkoutWithDetails(workoutId).first()
                    if (snapshot != null && (snapshot.workout.completed || snapshot.workout.status == "COMPLETED")) {
                        // Workout is already completed — create a fresh copy with same exercises
                        performAgainInternal(snapshot)
                    } else {
                        // Workout is incomplete — resume it
                        startWorkoutTimer()
                        workoutRepository.getWorkoutWithDetails(workoutId).collect {
                            _currentWorkout.value = it
                            _sessionUiState.value = if (it != null) SessionUiState.Active(it) else SessionUiState.Empty
                            loadPreviousPerformanceForExercises(it?.exercises ?: emptyList())
                            calculateSessionVolume(it)
                        }
                    }
                } else {
                    // Serialize the creation decision (check-then-create) to enforce
                    // at-most-one ACTIVE workout invariant. Returns the workout ID
                    // (either existing or newly created). Mutex is released before
                    // the Room Flow collect, which suspends indefinitely.
                    val targetWorkoutId = workoutCreationMutex.withLock {
                        val existing = workoutRepository.getLatestIncompleteWorkout()
                        if (existing != null) {
                            existing.id
                        } else {
                            val now = Instant.now()
                            val workout = Workout(
                                date = now, startTime = now, endTime = now,
                                duration = 0, notes = "", completed = false, status = "ACTIVE"
                            )
                            workoutRepository.createWorkout(workout)
                        }
                    }
                    // Mutex released. Start timer and collect outside the lock.
                    startWorkoutTimer()
                    workoutRepository.getWorkoutWithDetails(targetWorkoutId).collect {
                        _currentWorkout.value = it
                        _sessionUiState.value = if (it != null) SessionUiState.Active(it) else SessionUiState.Empty
                        loadPreviousPerformanceForExercises(it?.exercises ?: emptyList())
                        calculateSessionVolume(it)
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load workout"
                _sessionUiState.value = SessionUiState.Error(e.message ?: "Failed to load workout")
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
        if (exercises.isEmpty()) return

        val exerciseIds = exercises.map { it.exercise.id }.distinct()

        val perfMap = try {
            workoutRepository.getLastSetsForExercises(exerciseIds)
        } catch (_: Exception) {
            emptyMap<Long, List<LastSetData>>()
        }

        val summaryMap = try {
            workoutRepository.getLastPerformancesForExercises(exerciseIds)
        } catch (_: Exception) {
            emptyMap<Long, LastPerformance>()
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
                // Explicit user intent to create a new ACTIVE workout.
                // The Mutex only guards the DB insert; the Room Flow collect
                // (which suspends indefinitely) runs outside the lock.
                val id = workoutCreationMutex.withLock {
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
                    workoutRepository.createWorkout(workout)
                }
                startWorkoutTimer()
                workoutRepository.getWorkoutWithDetails(id).collect {
                    _currentWorkout.value = it
                    _sessionUiState.value = if (it != null) SessionUiState.Active(it) else SessionUiState.Empty
                    loadPreviousPerformanceForExercises(it?.exercises ?: emptyList())
                    calculateSessionVolume(it)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to start workout"
            }
        }
    }

    /**
     * Create a fresh workout copying exercises from a completed workout.
     * Does NOT copy set data — the user starts from scratch.
     */
    private suspend fun performAgainInternal(originalWorkout: WorkoutWithDetails) {
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
        val newWorkoutId = workoutRepository.createWorkout(workout)

        // Copy exercises from original workout (but not sets)
        for (exercise in originalWorkout.exercises) {
            workoutRepository.addExerciseToWorkout(
                newWorkoutId,
                exercise.exercise.id,
                exercise.workoutExercise.orderIndex
            )
        }

        // Start the timer BEFORE collecting: Room flows never complete, so any
        // code after the collect below is unreachable. Starting the timer first
        // lets _elapsedSeconds tick while the collect feeds _currentWorkout.
        startWorkoutTimer()

        // Load the new workout
        workoutRepository.getWorkoutWithDetails(newWorkoutId).collect {
            _currentWorkout.value = it
            _sessionUiState.value = if (it != null) SessionUiState.Active(it) else SessionUiState.Empty
            loadPreviousPerformanceForExercises(it?.exercises ?: emptyList())
            calculateSessionVolume(it)
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
        // APP-016: Reject duplicate exercises — a workout session must not
        // accidentally contain the same exercise more than once.
        val alreadyPresent = workout.exercises.any { it.exercise.id == exercise.id }
        if (alreadyPresent) return
        val nextOrder = (workout.exercises.maxOfOrNull { it.workoutExercise.orderIndex } ?: -1) + 1
        viewModelScope.launch {
            // Serialize to prevent concurrent duplicate inserts (race-safe).
            exerciseAddMutex.withLock {
                // Re-read current workout inside the lock to catch mutations
                // that occurred between the outer read and acquiring the lock.
                val currentWorkout = _currentWorkout.value ?: return@withLock
                val duplicateInsideLock = currentWorkout.exercises.any { it.exercise.id == exercise.id }
                if (duplicateInsideLock) return@withLock
                workoutRepository.addExerciseToWorkout(currentWorkout.workout.id, exercise.id, nextOrder)
            }
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

        viewModelScope.launch {
            // Serialize set additions to prevent duplicate set numbers under concurrent taps.
            addSetMutex.withLock {
                // Re-read current workout inside the lock to get the latest set list.
                val currentWorkout = _currentWorkout.value ?: return@withLock
                val currentWe = currentWorkout.exercises.getOrNull(exerciseIndex) ?: return@withLock
                val nextSetNumber = (currentWe.sets.maxOfOrNull { it.setNumber } ?: 0) + 1
                val newSet = WorkoutSet(
                    workoutExerciseId = currentWe.workoutExercise.id,
                    setNumber = nextSetNumber,
                    weight = prefilledWeight,
                    reps = prefilledReps,
                    rpe = 0.0,
                    restSeconds = prefilledRest,
                    completed = false
                )
                workoutRepository.addSetToExercise(currentWe.workoutExercise.id, newSet)
            }
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
        // Capture stable DB identity at call site, not index.
        val we = workout.exercises[exerciseIndex]
        val workoutExerciseId = we.workoutExercise.id
        val exerciseId = we.exercise.id
        viewModelScope.launch {
            workoutRepository.removeExerciseFromWorkout(workoutExerciseId)
            // Remove progression recommendation for removed exercise
            val updated = _progressionRecommendations.value - exerciseId
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
        val workoutData = _currentWorkout.value ?: return
        val workout = workoutData.workout
        // Terminal-state guard: refuse to complete an already-completed,
        // abandoned, or otherwise terminal workout.
        if (workout.completed || workout.status == "COMPLETED" || workout.status == "ABANDONED") return
        // Atomic admission control: exactly one caller wins the compareAndSet.
        if (!completionInProgress.compareAndSet(false, true)) return
        workoutTimerJob?.cancel()
        restTimer.stop()

        // Capture completion statistics before finalizing
        var totalSets = 0
        var totalReps = 0
        var totalVolume = 0.0
        for (we in workoutData.exercises) {
            for (set in we.sets) {
                if (set.completed) {
                    totalSets++
                    totalReps += set.reps
                    totalVolume += set.weight * set.reps
                }
            }
        }
        _completionStats.value = CompletionStats(
            durationSeconds = _elapsedSeconds.value,
            totalVolume = totalVolume,
            totalSets = totalSets,
            totalReps = totalReps,
            exerciseCount = workoutData.exercises.size
        )

        val now = Instant.now()
        val duration = now.epochSecond - workout.startTime.epochSecond
        val updated = workout.copy(
            endTime = now,
            duration = duration,
            completed = true,
            status = "COMPLETED"
        )
        applicationScope.launch {
            try {
                workoutRepository.updateWorkout(updated)
                // Only set terminal UI state AFTER successful DB write.
                _completed.value = true
            } catch (e: Exception) {
                // If DB write fails, allow retry by resetting the guard.
                completionInProgress.set(false)
                _error.value = "Failed to save workout: ${e.message}"
            }
        }
    }

    /**
     * Calculate progression recommendations for all exercises in the workout.
     * Uses ProgressionEngine with double progression logic.
     */
    private fun calculateProgressionRecommendations(exercises: List<WorkoutExerciseWithSets>) {
        viewModelScope.launch {
            val profile = userProfileRepository.getLatestProfile().firstOrNull()
            val equipmentType = profile?.equipmentType ?: "home"
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
                        equipmentType = equipmentType
                    )
                    recommendations[exercise.id] = recommendation
                }
            }
            _progressionRecommendations.value = recommendations
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