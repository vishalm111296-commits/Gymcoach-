package com.gymcoach.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.timer.RestTimerManager
import com.gymcoach.app.core.export.WorkoutDataExporter
import com.gymcoach.app.core.export.WorkoutDataImporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutWithDetails
import com.gymcoach.app.domain.model.WorkoutWithStats
import com.gymcoach.app.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ExportFormat { CSV_SPREADSHEET, CSV_STRONG, JSON }

data class ExportResult(
    val content: String,
    val filename: String,
    val mimeType: String
)

data class ImportUiState(
    val isImporting: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class WorkoutHistoryViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val restTimer: RestTimerManager,
    private val workoutDataExporter: WorkoutDataExporter,
    private val workoutDataImporter: WorkoutDataImporter = WorkoutDataImporter()
) : ViewModel() {

    // Test backward compatibility constructor
    constructor(
        workoutRepository: WorkoutRepository,
        restTimer: RestTimerManager
    ) : this(
        workoutRepository,
        restTimer,
        WorkoutDataExporter(),
        WorkoutDataImporter()
    )

    private val _exportResult = MutableStateFlow<ExportResult?>(null)
    val exportResult: StateFlow<ExportResult?> = _exportResult.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _importUiState = MutableStateFlow(ImportUiState())
    val importUiState: StateFlow<ImportUiState> = _importUiState.asStateFlow()

    enum class SortOption { NEWEST, OLDEST, VOLUME_DESC, VOLUME_ASC, DURATION_DESC, DURATION_ASC }
    enum class FilterOption { ALL, TODAY, THIS_WEEK, THIS_MONTH, CUSTOM }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterOption = MutableStateFlow(FilterOption.ALL)
    val filterOption: StateFlow<FilterOption> = _filterOption.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val _sortOption = MutableStateFlow(SortOption.NEWEST)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _customStartDate = MutableStateFlow<Long?>(null)
    val customStartDate: StateFlow<Long?> = _customStartDate.asStateFlow()

    private val _customEndDate = MutableStateFlow<Long?>(null)
    val customEndDate: StateFlow<Long?> = _customEndDate.asStateFlow()

    private val _selectedWorkout = MutableStateFlow<Long?>(null)

    private val _incompleteWorkout = MutableStateFlow<Workout?>(null)
    val incompleteWorkout: StateFlow<Workout?> = _incompleteWorkout.asStateFlow()

    private val _deleteTarget = MutableStateFlow<Long?>(null)
    val deleteTarget: StateFlow<Long?> = _deleteTarget.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _workouts = MutableStateFlow<List<WorkoutWithStats>>(emptyList())
    val workouts: StateFlow<List<WorkoutWithStats>> = _workouts.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        observeWorkouts()
        loadIncompleteWorkout()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeWorkouts() {
        viewModelScope.launch {
            try {
                val filtersFlow = combine(
                    searchQuery,
                    filterOption,
                    sortOption,
                    _customStartDate,
                    _customEndDate
                ) { query, filter, sort, customStart, customEnd ->
                    FilterState(query, filter, sort, customStart, customEnd)
                }

                filtersFlow
                    .flatMapLatest { filters ->
                        if (filters.query.isNotBlank()) {
                            kotlinx.coroutines.flow.flow {
                                emit(Pair(workoutRepository.searchWorkouts(filters.query), filters))
                            }
                        } else {
                            workoutRepository.getCompletedWorkouts().map { Pair(it, filters) }
                        }
                    }
                    .map { (workouts, filters) ->
                        var filtered = workouts

                        // Apply filter
                    filtered = when (filters.filter) {
                        FilterOption.ALL -> filtered
                        FilterOption.TODAY -> {
                            val todayStart = java.util.Calendar.getInstance().apply {
                                set(java.util.Calendar.HOUR_OF_DAY, 0)
                                set(java.util.Calendar.MINUTE, 0)
                                set(java.util.Calendar.SECOND, 0)
                                set(java.util.Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            filtered.filter { it.date.toEpochMilli() >= todayStart }
                        }
                        FilterOption.THIS_WEEK -> {
                            val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
                            filtered.filter { it.date.toEpochMilli() >= weekAgo }
                        }
                        FilterOption.THIS_MONTH -> {
                            val monthAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                            filtered.filter { it.date.toEpochMilli() >= monthAgo }
                        }
                        FilterOption.CUSTOM -> {
                            filtered.filter { it ->
                                val date = it.date.toEpochMilli()
                                (filters.customStart == null || date >= filters.customStart) &&
                                (filters.customEnd == null || date <= filters.customEnd)
                            }
                        }
                    }

                    // Apply sorting
                    val sorted = when (filters.sort) {
                        SortOption.NEWEST -> filtered.sortedByDescending { it.date.toEpochMilli() }
                        SortOption.OLDEST -> filtered.sortedBy { it.date.toEpochMilli() }
                        SortOption.VOLUME_DESC -> filtered.sortedByDescending { it.volume }
                        SortOption.VOLUME_ASC -> filtered.sortedBy { it.volume }
                        SortOption.DURATION_DESC -> filtered.sortedByDescending { it.duration }
                        SortOption.DURATION_ASC -> filtered.sortedBy { it.duration }
                    }

                    sorted
                }
                    .distinctUntilChanged()
                    .collect { workouts ->
                        _workouts.value = workouts
                        _isLoading.value = false
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Failed to load workouts: ${e.message}"
            }
        }
    }

    private data class FilterState(
        val query: String,
        val filter: FilterOption,
        val sort: SortOption,
        val customStart: Long?,
        val customEnd: Long?
    )

    private fun loadIncompleteWorkout() {
        viewModelScope.launch {
            try {
                _incompleteWorkout.value = workoutRepository.getIncompleteWorkout()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Non-critical — silently ignore; incomplete workout banner is optional
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChange(filter: FilterOption) {
        _filterOption.value = filter
    }

    fun onSortChange(sort: SortOption) {
        _sortOption.value = sort
    }

    fun onCustomDateRangeChange(start: Long?, end: Long?) {
        _customStartDate.value = start
        _customEndDate.value = end
    }

    fun onWorkoutClick(workoutId: Long) {
        _selectedWorkout.value = workoutId
    }

    fun onDeleteClick(workoutId: Long) {
        _deleteTarget.value = workoutId
    }

    fun confirmDelete() {
        _deleteTarget.value?.let { workoutId ->
            _deleteTarget.value = null
            viewModelScope.launch {
                try {
                    workoutRepository.deleteWorkout(workoutId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _error.value = "Failed to delete workout: ${e.message}"
                }
            }
        }
    }

    fun cancelDelete() {
        _deleteTarget.value = null
    }

    fun getIncompleteWorkout(): Workout? {
        return _incompleteWorkout.value
    }

    fun exportData(format: ExportFormat) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val completed = workoutRepository.getCompletedWorkouts().first()
                val detailsList = completed.mapNotNull {
                    workoutRepository.getWorkoutWithDetails(it.id).first()
                }
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val result = when (format) {
                    ExportFormat.CSV_SPREADSHEET -> ExportResult(
                        content = workoutDataExporter.exportToCsv(detailsList),
                        filename = "gymcoach_workouts_$timestamp.csv",
                        mimeType = "text/csv"
                    )
                    ExportFormat.CSV_STRONG -> ExportResult(
                        content = workoutDataExporter.exportToStrongCsv(detailsList),
                        filename = "strong_workouts_$timestamp.csv",
                        mimeType = "text/csv"
                    )
                    ExportFormat.JSON -> ExportResult(
                        content = workoutDataExporter.exportToJson(detailsList),
                        filename = "gymcoach_workouts_$timestamp.json",
                        mimeType = "application/json"
                    )
                }
                _exportResult.value = result
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _exportResult.value = null
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun clearExportResult() {
        _exportResult.value = null
    }

    fun importWorkoutsFromJson(jsonString: String) {
        viewModelScope.launch {
            _importUiState.value = ImportUiState(isImporting = true)
            try {
                val parseResult = workoutDataImporter.parseJson(jsonString)
                if (parseResult.isFailure) {
                    _importUiState.value = ImportUiState(
                        isImporting = false,
                        error = parseResult.exceptionOrNull()?.message ?: "Invalid or corrupt JSON format"
                    )
                    return@launch
                }
                val importedData = parseResult.getOrThrow()
                val importResult = workoutRepository.importWorkouts(importedData.workouts)
                if (importResult.isFailure) {
                    _importUiState.value = ImportUiState(
                        isImporting = false,
                        error = importResult.exceptionOrNull()?.message ?: "Failed to save imported workouts"
                    )
                    return@launch
                }
                val stats = importResult.getOrThrow()
                val msg = StringBuilder("Successfully imported ${stats.workoutsImported} workout(s) (${stats.setsImported} sets).")
                if (stats.workoutsSkipped > 0) {
                    msg.append(" Skipped ${stats.workoutsSkipped} duplicate(s).")
                }
                _importUiState.value = ImportUiState(
                    isImporting = false,
                    message = msg.toString()
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _importUiState.value = ImportUiState(
                    isImporting = false,
                    error = e.message ?: "Failed to import workouts"
                )
            }
        }
    }

    fun clearImportUiState() {
        _importUiState.value = ImportUiState()
    }
}
