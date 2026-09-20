package com.gymcoach.app.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.temporal.ChronoField
import javax.inject.Inject

data class TrainingFrequencyUiState(
    val isLoading: Boolean = true,
    val workoutsThisWeek: Int = 0,
    val workoutsThisMonth: Int = 0,
    val bestStreak: Int = 0,
    val dayFrequency: Map<DayOfWeek, Int> = emptyMap(),
    val heatmapData: Map<LocalDate, Int> = emptyMap(),
    val monthlyData: Map<Month, Int> = emptyMap()
)

@HiltViewModel
class TrainingFrequencyViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainingFrequencyUiState())
    val uiState: StateFlow<TrainingFrequencyUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            workoutRepository.getCompletedWorkouts().collect { workouts ->
                val today = LocalDate.now()
                val sixMonthsAgo = today.minusMonths(6).withDayOfMonth(1)
                val zoneId = ZoneId.systemDefault()
                
                val currentWeekNum = today.get(ChronoField.ALIGNED_WEEK_OF_YEAR)
                val currentYear = today.year
                
                var weekCount = 0
                var monthCount = 0
                val heatData = mutableMapOf<LocalDate, Int>()
                val dayFreq = mutableMapOf<DayOfWeek, Int>()
                val monthData = mutableMapOf<Month, Int>()

                val activeDates = mutableSetOf<LocalDate>()

                for (w in workouts) {
                    val date = w.date.atZone(zoneId).toLocalDate()
                    activeDates.add(date)

                    // week count
                    if (date.year == currentYear && date.get(ChronoField.ALIGNED_WEEK_OF_YEAR) == currentWeekNum) {
                        weekCount++
                    }
                    
                    // month count
                    if (date.year == currentYear && date.month == today.month) {
                        monthCount++
                    }

                    // For last 6 months
                    if (!date.isBefore(sixMonthsAgo)) {
                        heatData[date] = heatData.getOrDefault(date, 0) + 1
                        dayFreq[date.dayOfWeek] = dayFreq.getOrDefault(date.dayOfWeek, 0) + 1
                        monthData[date.month] = monthData.getOrDefault(date.month, 0) + 1
                    }
                }

                // calculate best streak
                var currentStreak = 0
                var maxStreak = 0
                var lastDate: LocalDate? = null

                val sortedDates = activeDates.sorted()
                for (date in sortedDates) {
                    if (lastDate == null) {
                        currentStreak = 1
                    } else {
                        if (lastDate.plusDays(1) == date) {
                            currentStreak++
                        } else {
                            currentStreak = 1
                        }
                    }
                    if (currentStreak > maxStreak) {
                        maxStreak = currentStreak
                    }
                    lastDate = date
                }

                _uiState.value = TrainingFrequencyUiState(
                    isLoading = false,
                    workoutsThisWeek = weekCount,
                    workoutsThisMonth = monthCount,
                    bestStreak = maxStreak,
                    dayFrequency = dayFreq,
                    heatmapData = heatData,
                    monthlyData = monthData
                )
            }
        }
    }
}
