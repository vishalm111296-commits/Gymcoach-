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
import com.gymcoach.app.domain.model.SupersetGroup
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutExerciseWithSets
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutWithDetails
import com.gymcoach.app.domain.repository.ExerciseRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import com.gymcoach.app.data.local.entity.ReadinessEntity
import com.gymcoach.app.domain.repository.ReadinessRepository
import com.gymcoach.app.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class WorkoutLoggingViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val restTimer: RestTimerManager,
    private val progressionEngine: ProgressionEngine,
    private val userProfileRepository: UserProfileRepository,
    private val readinessRepository: ReadinessRepository,
    private val personalRecordDao: com.gymcoach.app.data.local.dao.PersonalRecordDao,
    private val prDetector: com.gymcoach.app.core.progression.PRDetector,
    private val substitutionEngine: com.gymcoach.app.core.exercise.SubstitutionEngine? = null
) : ViewModel() {

    // Test backward compatibility constructor
    constructor(
        workoutRepository: WorkoutRepository,
        exerciseRepository: ExerciseRepository,
        restTimer: RestTimerManager,
        progressionEngine: ProgressionEngine,
        userProfileRepository: UserProfileRepository
    ) : this(
        workoutRepository,
        exerciseRepository,
        restTimer,
        progressionEngine,
        userProfileRepository,
        object : ReadinessRepository {
            override fun getAllReadiness() = kotlinx.coroutines.flow.emptyFlow<List<ReadinessEntity>>()
            override fun getLatestReadiness() = kotlinx.coroutines.flow.flowOf(null)
            override fun getReadinessInRange(startTime: Long, endTime: Long) = kotlinx.coroutines.flow.emptyFlow<List<ReadinessEntity>>()
            override fun getRecentReadiness(since: Long) = kotlinx.coroutines.flow.emptyFlow<List<ReadinessEntity>>()
            override suspend fun saveReadiness(readiness: ReadinessEntity) = 0L
            override suspend fun updateReadiness(readiness: ReadinessEntity) {}
            override suspend fun deleteReadiness(id: Long) {}
        },
        object : com.gymcoach.app.data.local.dao.PersonalRecordDao {
            override suspend fun insert(record: com.gymcoach.app.data.local.entity.PersonalRecordEntity): Long = 0L
            override suspend fun insertAll(records: List<com.gymcoach.app.data.local.entity.PersonalRecordEntity>) {}
            override suspend fun update(record: com.gymcoach.app.data.local.entity.PersonalRecordEntity): Int = 0
            override fun getByExerciseId(exerciseId: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.gymcoach.app.data.local.entity.PersonalRecordEntity>())
            override fun getAll() = kotlinx.coroutines.flow.flowOf(emptyList<com.gymcoach.app.data.local.entity.PersonalRecordEntity>())
            override fun getAllWithExerciseName() = kotlinx.coroutines.flow.flowOf(emptyList<com.gymcoach.app.data.local.entity.PersonalRecordWithExercise>())
            override suspend fun getById(id: Long): com.gymcoach.app.data.local.entity.PersonalRecordEntity? = null
            override suspend fun deleteById(id: Long): Int = 0
        },
        com.gymcoach.app.core.progression.PRDetector(),
        null
    )

    val latestReadiness: StateFlow<ReadinessEntity?> = readinessRepository.getLatestReadiness()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    data class WorkoutSummary(
        val workoutId: Long,
        val workoutName: String,
        val durationSeconds: Long,
        val totalVolumeKg: Double,
        val completedSetsCount: Int,
        val totalSetsCount: Int,
        val exercisesCompletedCount: Int,
        val newPRs: List<com.gymcoach.app.core.progression.PRDetector.PersonalRecord> = emptyList()
    )

    private val _workoutSummary = MutableStateFlow<WorkoutSummary?>(null)
    val workoutSummary: StateFlow<WorkoutSummary?> = _workoutSummary.asStateFlow()

    private var defaultRestSeconds = 90

    val allExercises = exerciseRepository.getAllExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentWorkout = MutableStateFlow<WorkoutWithDetails?>(null)
    val currentWorkout: StateFlow<WorkoutWithDetails?> = _currentWorkout.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private var workoutTimerJob: Job? = null

    /**
     * F-WORKOUT-5 fix: track the active Flow collector so we cancel it before
     * launching a replacement. Without this guard, calling loadOrStartWorkout
     * multiple times (e.g. on configuration change or back-navigation re-entry)
     * accumulated stale collectors each updating _currentWorkout and firing
     * redundant loadPreviousPerformanceForExercises calls.
     */
    private var workoutCollectorJob: Job? = null

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

    // Substitution state
    private val _substitutes = MutableStateFlow<List<com.gymcoach.app.core.exercise.SubstitutionEngine.SubstitutionResult>>(emptyList())
    val substitutes: StateFlow<List<com.gymcoach.app.core.exercise.SubstitutionEngine.SubstitutionResult>> = _substitutes.asStateFlow()

    private val _isSubstitutionLoading = MutableStateFlow(false)
    val isSubstitutionLoading: StateFlow<Boolean> = _isSubstitutionLoading.asStateFlow()

    // Accumulated volume for the current workout session
    private val _sessionVolume = MutableStateFlow(0.0)
    val sessionVolume: StateFlow<Double> = _sessionVolume.asStateFlow()

    private val _supersetGroups = MutableStateFlow<List<SupersetGroup>>(emptyList())
    val supersetGroups: StateFlow<List<SupersetGroup>> = _supersetGroups.asStateFlow()

    /**
     * F-WORKOUT-1 fix: serialise addSet calls so that nextSetNumber is always
     * computed atomically relative to the previous insert. Without this mutex,
     * two rapid taps launch two coroutines that both read the same in-memory
     * maxSetNumber before either DB write returns, creating duplicate setNumbers.
     *
     * F-WORKOUT-3: the same Mutex is used for addExerciseToWorkout to serialise
     * orderIndex computation for the same reason.
     */
    private val addSetMutex = Mutex()

    fun dismissError() {
        _error.value = null
    }

    fun loadOrStartWorkout(workoutId: Long? = null) {
        // F-WORKOUT-5: cancel any existing collector before launching a new one
        workoutCollectorJob?.cancel()
        workoutCollectorJob = viewModelScope.launch {
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

    @androidx.annotation.VisibleForTesting
    var enableWorkoutTimer = true

    private fun startWorkoutTimer() {
        if (!enableWorkoutTimer) return
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
        // Cancel any previous collector (F-WORKOUT-5) before collecting the new workout
        workoutCollectorJob?.cancel()
        workoutCollectorJob = viewModelScope.launch {
            workoutRepository.getWorkoutWithDetails(id).collect {
                _currentWorkout.value = it
                loadPreviousPerformanceForExercises(it?.exercises ?: emptyList())
                calculateSessionVolume(it)
                startWorkoutTimer()
            }
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

    /**
     * Add an exercise to the current workout.
     *
     * F-WORKOUT-3 fix: nextOrder is now computed inside the coroutine from the
     * latest in-memory state at the time of execution, not from a snapshot
     * captured before the coroutine launches.
     *
     * Previously `nextOrder` was computed on the calling thread then captured by
     * the lambda. Two rapid addExercise calls could both read the same
     * maxOrderIndex before either DB insert completed, producing duplicate
     * orderIndex values that cause exercises to render in non-deterministic order.
     *
     * The serialisation via addSetMutex ensures the two DB inserts happen
     * sequentially and each sees the updated in-memory maxOrderIndex left by
     * the preceding insert's Flow update.
     */
    fun addExerciseToWorkout(exercise: Exercise) {
        viewModelScope.launch {
            try {
                addSetMutex.withLock {
                    // Re-read inside the lock for the same reason as addSet
                    val workout = _currentWorkout.value ?: return@withLock
                    val nextOrder = (workout.exercises.maxOfOrNull { it.workoutExercise.orderIndex } ?: -1) + 1
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
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add exercise"
            }
        }
    }

    fun createAndAddCustomExercise(
        name: String,
        muscleGroup: String,
        equipment: String,
        difficulty: String = "Intermediate",
        notes: String = ""
    ) {
        viewModelScope.launch {
            try {
                val exerciseId = exerciseRepository.createCustomExercise(
                    name = name,
                    muscleGroup = muscleGroup,
                    equipment = equipment,
                    difficulty = difficulty,
                    notes = notes
                )
                val created = exerciseRepository.getExerciseById(exerciseId).firstOrNull()
                if (created != null) {
                    addExerciseToWorkout(created)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to create and add custom exercise"
            }
        }
    }

    /**
     * Add a new set, serialised via [addSetMutex] to prevent duplicate setNumber
     * assignment when two taps arrive before the first DB write completes
     * (F-WORKOUT-1).
     */
    fun addSet(exerciseIndex: Int) {
        val workout = _currentWorkout.value ?: return
        if (exerciseIndex !in workout.exercises.indices) return
        val we = workout.exercises[exerciseIndex]
        val exerciseId = we.exercise.id

        viewModelScope.launch {
            try {
                addSetMutex.withLock {
                    // Re-read currentWorkout inside the lock so we see any sets added
                    // by a concurrent tap that acquired the lock before us.
                    val latestWorkout = _currentWorkout.value ?: return@withLock
                    val latestWe = latestWorkout.exercises.getOrNull(exerciseIndex) ?: return@withLock
                    val nextSetNumber = (latestWe.sets.maxOfOrNull { it.setNumber } ?: 0) + 1

                    // Auto-populate from previous set in current session, or previous session if available
                    val precedingSet = latestWe.sets.lastOrNull()
                    val lastSets = _previousPerformance.value[exerciseId]
                    val prefilledWeight: Double
                    val prefilledReps: Int
                    val prefilledRest: Int

                    if (precedingSet != null && precedingSet.weight > 0) {
                        prefilledWeight = precedingSet.weight
                        prefilledReps = precedingSet.reps
                        prefilledRest = precedingSet.restSeconds.takeIf { it > 0 } ?: defaultRestSeconds
                    } else if (lastSets != null && lastSets.isNotEmpty()) {
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
                        workoutExerciseId = latestWe.workoutExercise.id,
                        setNumber = nextSetNumber,
                        weight = prefilledWeight,
                        reps = prefilledReps,
                        rpe = 0.0,
                        restSeconds = prefilledRest,
                        completed = false
                    )
                    workoutRepository.addSetToExercise(latestWe.workoutExercise.id, newSet)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add set"
            }
        }
    }

    /**
     * Batch insert scientific warm-up sets for an exercise.
     */
    fun addWarmupSets(
        exerciseIndex: Int,
        warmupSets: List<com.gymcoach.app.core.progression.WarmupCalculator.WarmupSetProtocol>
    ) {
        val workout = _currentWorkout.value ?: return
        if (exerciseIndex !in workout.exercises.indices || warmupSets.isEmpty()) return

        viewModelScope.launch {
            try {
                addSetMutex.withLock {
                    val latestWorkout = _currentWorkout.value ?: return@withLock
                    val latestWe = latestWorkout.exercises.getOrNull(exerciseIndex) ?: return@withLock
                    var currentMaxSetNumber = latestWe.sets.maxOfOrNull { it.setNumber } ?: 0

                    for (ws in warmupSets) {
                        currentMaxSetNumber++
                        val newSet = WorkoutSet(
                            workoutExerciseId = latestWe.workoutExercise.id,
                            setNumber = currentMaxSetNumber,
                            weight = ws.weight,
                            reps = ws.reps,
                            rpe = 0.0,
                            restSeconds = ws.restSeconds,
                            completed = false,
                            setType = SetType.WARMUP
                        )
                        workoutRepository.addSetToExercise(latestWe.workoutExercise.id, newSet)
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add warm-up sets"
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

    fun applyCameraReps(exerciseIndex: Int, reps: Int, setIndex: Int? = null) {
        val workout = _currentWorkout.value ?: return
        if (exerciseIndex !in workout.exercises.indices) return
        val we = workout.exercises[exerciseIndex]
        if (we.sets.isEmpty()) return
        val targetSetIndex = setIndex ?: we.sets.indexOfFirst { !it.completed }.let { if (it == -1) we.sets.lastIndex else it }
        if (targetSetIndex !in we.sets.indices) return
        val set = we.sets[targetSetIndex]
        val updated = set.copy(reps = reps, completed = true)
        viewModelScope.launch {
            try {
                workoutRepository.updateSet(updated)
                val refreshed = _currentWorkout.value
                calculateSessionVolume(refreshed)
                if (refreshed != null) {
                    calculateProgressionRecommendations(refreshed.exercises)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to apply camera reps"
            }
        }
        val recommendedRest = RestPresets.recommended(set.setType, set.rpe)
        val restSeconds = if (set.restSeconds > 0) set.restSeconds else recommendedRest
        val nextSet = calculateNextSetLabel(workout, exerciseIndex, targetSetIndex)
        restTimer.start(restSeconds, viewModelScope, nextSet = nextSet, workoutId = workout.workout.id)
    }

    fun removeSet(exerciseIndex: Int, setIndex: Int) {
        val workout = _currentWorkout.value ?: return
        if (exerciseIndex !in workout.exercises.indices) return
        val we = workout.exercises[exerciseIndex]
        if (setIndex !in we.sets.indices) return
        val set = we.sets[setIndex]
        viewModelScope.launch {
            try {
                workoutRepository.deleteSet(set.id)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to remove set"
            }
        }
    }

    fun removeExercise(exerciseIndex: Int) {
        val workout = _currentWorkout.value ?: return
        if (exerciseIndex !in workout.exercises.indices) return
        viewModelScope.launch {
            try {
                val we = workout.exercises[exerciseIndex]
                workoutRepository.removeExerciseFromWorkout(we.workoutExercise.id)
                val updated = _progressionRecommendations.value - we.exercise.id
                _progressionRecommendations.value = updated
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to remove exercise"
            }
        }
    }

    fun loadSubstitutesForExercise(exerciseId: Long) {
        viewModelScope.launch {
            _isSubstitutionLoading.value = true
            val engine = substitutionEngine
            if (engine == null) {
                _substitutes.value = emptyList()
                _isSubstitutionLoading.value = false
                return@launch
            }
            val userProfile = userProfileRepository.getLatestProfile().firstOrNull()
            val equipmentType = userProfile?.equipmentType ?: "gym"
            try {
                _substitutes.value = engine.findSubstitutes(exerciseId, equipmentType, maxResults = 5)
            } catch (_: Exception) {
                _substitutes.value = emptyList()
            } finally {
                _isSubstitutionLoading.value = false
            }
        }
    }

    suspend fun getSubstitutesForExercise(exerciseId: Long): List<com.gymcoach.app.core.exercise.SubstitutionEngine.SubstitutionResult> {
        val engine = substitutionEngine ?: return emptyList()
        val userProfile = userProfileRepository.getLatestProfile().firstOrNull()
        val equipmentType = userProfile?.equipmentType ?: "gym"
        return try {
            engine.findSubstitutes(exerciseId, equipmentType, maxResults = 5)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun swapExercise(exerciseIndex: Int, newExerciseId: Long) {
        val workout = _currentWorkout.value ?: return
        if (exerciseIndex !in workout.exercises.indices) return
        val we = workout.exercises[exerciseIndex]

        viewModelScope.launch {
            try {
                addSetMutex.withLock {
                    workoutRepository.swapExercise(we.workoutExercise.id, newExerciseId)

                    // Refresh previous performance for the newly swapped exercise
                    val lastSets = workoutRepository.getLastSetsForExercise(newExerciseId)
                    if (lastSets.isNotEmpty()) {
                        _previousPerformance.value = _previousPerformance.value + (newExerciseId to lastSets)
                    }
                    val lastPerf = workoutRepository.getLastPerformanceForExercise(newExerciseId)
                    if (lastPerf != null) {
                        _lastPerformanceSummary.value = _lastPerformanceSummary.value + (newExerciseId to lastPerf)
                    }

                    // Recalculate progression for updated workout
                    val refreshed = _currentWorkout.value
                    if (refreshed != null) {
                        calculateProgressionRecommendations(refreshed.exercises)
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to substitute exercise"
            }
        }
    }

    fun linkExercisesAsSuperset(exerciseIndexA: Int, exerciseIndexB: Int) {
        val remaining = _supersetGroups.value.filter {
            exerciseIndexA !in it.exerciseIndices && exerciseIndexB !in it.exerciseIndices
        }
        val newGroup = SupersetGroup(
            id = "SS_${UUID.randomUUID().toString().take(8)}",
            label = "Superset ${'A' + remaining.size}",
            exerciseIndices = listOf(exerciseIndexA, exerciseIndexB).sorted()
        )
        _supersetGroups.value = remaining + newGroup
    }

    fun unlinkSuperset(exerciseIndex: Int) {
        _supersetGroups.value = _supersetGroups.value.filter { exerciseIndex !in it.exerciseIndices }
    }

    fun getSupersetForExercise(exerciseIndex: Int): SupersetGroup? {
        return _supersetGroups.value.firstOrNull { exerciseIndex in it.exerciseIndices }
    }

    fun logSet(exerciseIndex: Int, setIndex: Int) {
        val workout = _currentWorkout.value ?: return
        if (exerciseIndex !in workout.exercises.indices) return
        val we = workout.exercises[exerciseIndex]
        if (setIndex !in we.sets.indices) return
        val set = we.sets[setIndex]
        val updated = set.copy(completed = true)
        viewModelScope.launch {
            try {
                workoutRepository.updateSet(updated)
                val refreshed = _currentWorkout.value
                calculateSessionVolume(refreshed)
                if (refreshed != null) {
                    calculateProgressionRecommendations(refreshed.exercises)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to log set"
            }
        }
        val supersetGroup = getSupersetForExercise(exerciseIndex)
        val restSeconds = supersetGroup?.getRecommendedRestSeconds(exerciseIndex)
            ?: if (set.restSeconds > 0) set.restSeconds else RestPresets.recommended(set.setType, set.rpe)
        val nextSet = calculateNextSetLabel(workout, exerciseIndex, setIndex)
        restTimer.start(restSeconds, viewModelScope, nextSet = nextSet, workoutId = workout.workout.id)
    }

    fun logSet(exerciseIndex: Int) {
        val workout = _currentWorkout.value ?: return
        val we = workout.exercises.getOrNull(exerciseIndex) ?: return
        val targetIndex = we.sets.indexOfFirst { !it.completed }.let { if (it == -1) we.sets.lastIndex else it }
        if (targetIndex in we.sets.indices) {
            logSet(exerciseIndex, targetIndex)
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
            try {
                workoutRepository.updateSet(updated)
                val refreshed = _currentWorkout.value
                calculateSessionVolume(refreshed)
                if (refreshed != null) {
                    calculateProgressionRecommendations(refreshed.exercises)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update set"
            }
        }
        if (updated.completed) {
            val supersetGroup = getSupersetForExercise(exerciseIndex)
            val restSeconds = supersetGroup?.getRecommendedRestSeconds(exerciseIndex)
                ?: if (set.restSeconds > 0) set.restSeconds else RestPresets.recommended(set.setType, set.rpe)
            val nextSet = calculateNextSetLabel(workout, exerciseIndex, setIndex)
            restTimer.start(restSeconds, viewModelScope, nextSet = nextSet, workoutId = workout.workout.id)
        } else {
            restTimer.stop()
        }
    }

    fun pauseRestTimer() { restTimer.pause() }
    fun resumeRestTimer() { restTimer.resume() }
    fun stopRestTimer() { restTimer.stop() }
    fun adjustRestTimer(deltaSeconds: Int) { restTimer.adjust(deltaSeconds) }

    fun changeRestTimerDuration(seconds: Int) {
        val workout = _currentWorkout.value
        val workoutId = workout?.workout?.id ?: -1L
        restTimer.start(seconds, viewModelScope, workoutId = workoutId)
    }

    private fun calculateNextSetLabel(
        workout: WorkoutWithDetails,
        exerciseIndex: Int,
        setIndex: Int
    ): String {
        val superset = getSupersetForExercise(exerciseIndex)
        if (superset != null) {
            val nextExIdx = superset.getNextExerciseIndex(exerciseIndex)
            if (nextExIdx != null) {
                val nextWe = workout.exercises.getOrNull(nextExIdx)
                if (nextWe != null) {
                    val nextIncompleteSet = nextWe.sets.firstOrNull { !it.completed }
                    if (nextIncompleteSet != null) {
                        return "${nextWe.exercise.name} Set ${nextIncompleteSet.setNumber}"
                    }
                }
            }
        }
        val currentWe = workout.exercises.getOrNull(exerciseIndex) ?: return ""
        val nextSetInSameExercise = currentWe.sets.getOrNull(setIndex + 1)
        if (nextSetInSameExercise != null) {
            return "${currentWe.exercise.name} Set ${nextSetInSameExercise.setNumber}"
        }
        val nextWe = workout.exercises.getOrNull(exerciseIndex + 1)
        if (nextWe != null) {
            return "${nextWe.exercise.name} Set 1"
        }
        return "Workout Complete"
    }

    fun completeWorkout() {
        val workoutDetails = _currentWorkout.value ?: return
        val workout = workoutDetails.workout
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
            try {
                workoutRepository.updateWorkout(updated)

                var totalCompletedSets = 0
                var totalSets = 0
                var totalVolume = 0.0
                var completedExercises = 0
                val allNewPRs = mutableListOf<com.gymcoach.app.core.progression.PRDetector.PersonalRecord>()

                for (we in workoutDetails.exercises) {
                    val exerciseCompletedSets = we.sets.filter { it.completed }
                    totalSets += we.sets.size
                    totalCompletedSets += exerciseCompletedSets.size

                    if (exerciseCompletedSets.isNotEmpty()) {
                        completedExercises++
                        totalVolume += exerciseCompletedSets.sumOf { it.weight * it.reps }

                        val existingEntityPRs = personalRecordDao.getByExerciseId(we.exercise.id).firstOrNull() ?: emptyList()
                        val existingPRs = existingEntityPRs.map { entity ->
                            com.gymcoach.app.core.progression.PRDetector.PersonalRecord(
                                exerciseId = entity.exerciseId,
                                exerciseName = we.exercise.name,
                                type = if (entity.reps > 0) com.gymcoach.app.core.progression.PRDetector.PRType.REP
                                       else if (entity.oneRepMaxKg > 0) com.gymcoach.app.core.progression.PRDetector.PRType.ESTIMATED_1RM
                                       else if (entity.notes.startsWith("Volume")) com.gymcoach.app.core.progression.PRDetector.PRType.VOLUME
                                       else com.gymcoach.app.core.progression.PRDetector.PRType.WEIGHT,
                                value = if (entity.oneRepMaxKg > 0) entity.oneRepMaxKg else if (entity.reps > 0) entity.reps.toDouble() else entity.weightKg,
                                details = entity.notes,
                                date = Instant.ofEpochMilli(entity.achievedAt),
                                workoutId = workout.id
                            )
                        }

                        val currentSetEntities = exerciseCompletedSets.map { it.toEntity() }
                        val newPRs = prDetector.detectPRs(we.exercise.id, we.exercise.name, currentSetEntities, existingPRs, workout.id)

                        for (pr in newPRs) {
                            personalRecordDao.insert(
                                com.gymcoach.app.data.local.entity.PersonalRecordEntity(
                                    exerciseId = pr.exerciseId,
                                    weightKg = if (pr.type == com.gymcoach.app.core.progression.PRDetector.PRType.WEIGHT) pr.value else 0.0,
                                    reps = if (pr.type == com.gymcoach.app.core.progression.PRDetector.PRType.REP) pr.value.toInt() else 0,
                                    oneRepMaxKg = if (pr.type == com.gymcoach.app.core.progression.PRDetector.PRType.ESTIMATED_1RM) pr.value else 0.0,
                                    achievedAt = pr.date.toEpochMilli(),
                                    notes = pr.details
                                )
                            )
                        }

                        allNewPRs.addAll(newPRs)
                    }
                }

                _workoutSummary.value = WorkoutSummary(
                    workoutId = workout.id,
                    workoutName = "Workout",
                    durationSeconds = duration,
                    totalVolumeKg = totalVolume,
                    completedSetsCount = totalCompletedSets,
                    totalSetsCount = totalSets,
                    exercisesCompletedCount = completedExercises,
                    newPRs = allNewPRs
                )

                _completed.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to complete workout"
            }
        }
    }

    private fun calculateProgressionRecommendations(exercises: List<WorkoutExerciseWithSets>) {
        viewModelScope.launch {
            val profile = userProfileRepository.getLatestProfile().firstOrNull()
            val equipmentType = profile?.equipmentType ?: "home"
            val currentReadiness = latestReadiness.value?.readinessScore
            val recommendations = mutableMapOf<Long, ProgressionRecommendation>()
            for (we in exercises) {
                val exercise = we.exercise
                val normalSets = we.sets.filter { it.completed && it.setType == SetType.NORMAL }
                if (normalSets.isNotEmpty()) {
                    val lastSets = _previousPerformance.value[exercise.id] ?: emptyList()
                    val recommendation = progressionEngine.calculateProgressionForExercise(
                        exercise = exercise,
                        previousSets = lastSets.map {
                            WorkoutSetEntity(
                                workoutExerciseId = 0, setNumber = 0,
                                weight = it.weight, reps = it.reps, rpe = it.rpe,
                                restSeconds = it.restSeconds, completed = true,
                                setType = it.setType
                            )
                        },
                        currentSets = normalSets.map { it.toEntity() },
                        equipmentType = equipmentType,
                        readinessScore = currentReadiness
                    )
                    recommendations[exercise.id] = recommendation
                }
            }
            _progressionRecommendations.value = recommendations
        }
    }

    private fun updateSetField(
        exerciseIndex: Int,
        setIndex: Int,
        transform: (WorkoutSet) -> WorkoutSet
    ) {
        val workout = _currentWorkout.value ?: return
        if (exerciseIndex !in workout.exercises.indices) return
        val we = workout.exercises[exerciseIndex]
        if (setIndex !in we.sets.indices) return
        val updated = transform(we.sets[setIndex])
        viewModelScope.launch {
            try {
                workoutRepository.updateSet(updated)
                calculateSessionVolume(_currentWorkout.value)
                val refreshed = _currentWorkout.value
                if (refreshed != null) {
                    calculateProgressionRecommendations(refreshed.exercises)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update set"
            }
        }
    }

    @androidx.annotation.VisibleForTesting
    fun clearForTest() {
        workoutCollectorJob?.cancel()
        workoutTimerJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        workoutCollectorJob?.cancel()
        workoutTimerJob?.cancel()
    }
}
