package com.gymcoach.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.timer.RestTimerManager
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutWithDetails
import com.gymcoach.app.domain.model.WorkoutWithStats
import com.gymcoach.app.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutHistoryViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val restTimer: RestTimerManager
) : ViewModel() {

    enum class SortOption { NEWEST, OLDEST, VOLUME_DESC, VOLUME_ASC, DURATION_DESC, DURATION_ASC }
    enum class FilterOption { ALL, TODAY, THIS_WEEK, THIS_MONTH, CUSTOM }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _filterOption = MutableStateFlow(FilterOption.ALL)
    val filterOption: StateFlow<FilterOption> = _filterOption

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val _sortOption = MutableStateFlow(SortOption.NEWEST)
    val sortOption: StateFlow<SortOption> = _sortOption

    private val _customStartDate = MutableStateFlow<Long?>(null)
    val customStartDate: StateFlow<Long?> = _customStartDate

    private val _customEndDate = MutableStateFlow<Long?>(null)
    val customEndDate: StateFlow<Long?> = _customEndDate

    private val _selectedWorkout = MutableStateFlow<Long?>(null)

    private val _incompleteWorkout = MutableStateFlow<Workout?>(null)
    val incompleteWorkout: StateFlow<Workout?> = _incompleteWorkout

    private val _deleteTarget = MutableStateFlow<Long?>(null)
    val deleteTarget: StateFlow<Long?> = _deleteTarget

    private val _workouts = MutableStateFlow<List<WorkoutWithStats>>(emptyList())
    val workouts: StateFlow<List<WorkoutWithStats>> = _workouts

    init {
        observeWorkouts()
        loadIncompleteWorkout()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeWorkouts() {
        viewModelScope.launch {
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
            _incompleteWorkout.value = workoutRepository.getIncompleteWorkout()
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
            viewModelScope.launch {
                workoutRepository.deleteWorkout(workoutId)
            }
        }
    }

    fun cancelDelete() {
        _deleteTarget.value = null
    }

    fun getIncompleteWorkout(): Workout? {
        return _incompleteWorkout.value
    }

    override fun onCleared() {
        super.onCleared()
        restTimer.stop()
    }
}