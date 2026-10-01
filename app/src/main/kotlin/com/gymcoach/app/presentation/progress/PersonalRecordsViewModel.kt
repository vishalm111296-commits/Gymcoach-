package com.gymcoach.app.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.preferences.AppPreferences
import com.gymcoach.app.core.preferences.WeightUnit
import com.gymcoach.app.data.local.dao.PersonalRecordDao
import com.gymcoach.app.data.local.entity.PersonalRecordWithExercise
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject

sealed class PersonalRecordsUiState {
    object Loading : PersonalRecordsUiState()
    data class Success(
        val records: List<PersonalRecordWithExercise>,
        val sortBy: SortBy = SortBy.RECENT
    ) : PersonalRecordsUiState()
    object Empty : PersonalRecordsUiState()
    data class Error(val message: String) : PersonalRecordsUiState()
}

enum class SortBy { RECENT, WEIGHT, EXERCISE_NAME }

@HiltViewModel
class PersonalRecordsViewModel @Inject constructor(
    private val personalRecordDao: PersonalRecordDao,
    private val appPreferences: AppPreferences? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<PersonalRecordsUiState>(PersonalRecordsUiState.Loading)
    val uiState: StateFlow<PersonalRecordsUiState> = _uiState.asStateFlow()

    private val _sortBy = MutableStateFlow(SortBy.RECENT)
    val sortBy: StateFlow<SortBy> = _sortBy.asStateFlow()

    val weightUnit: StateFlow<WeightUnit> = appPreferences?.preferencesState
        ?.let { flow -> MutableStateFlow(flow.value.weightUnit).also { mutable ->
            viewModelScope.launch { flow.map { it.weightUnit }.collect { mutable.value = it } }
        } }
        ?: MutableStateFlow(WeightUnit.KG)

    init {
        loadRecords()
    }

    fun loadRecords() {
        viewModelScope.launch {
            _uiState.value = PersonalRecordsUiState.Loading
            personalRecordDao.getAllWithExerciseName()
                .combine(_sortBy) { records, sort ->
                    val sorted = when (sort) {
                        SortBy.RECENT -> records.sortedByDescending { it.achievedAt }
                        SortBy.WEIGHT -> records.sortedByDescending { it.weightKg }
                        SortBy.EXERCISE_NAME -> records.sortedBy { it.exerciseName.lowercase() }
                    }
                    Pair(sorted, sort)
                }
                .catch { e ->
                    if (e is CancellationException) throw e
                    _uiState.value = PersonalRecordsUiState.Error(e.message ?: "Unknown error")
                }
                .collect { (records, sort) ->
                    _uiState.value = if (records.isEmpty()) {
                        PersonalRecordsUiState.Empty
                    } else {
                        PersonalRecordsUiState.Success(records, sort)
                    }
                }
        }
    }

    fun setSortBy(sort: SortBy) {
        _sortBy.value = sort
    }
}
