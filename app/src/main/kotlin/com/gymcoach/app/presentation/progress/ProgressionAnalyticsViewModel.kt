package com.gymcoach.app.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.core.progression.PRDetector
import com.gymcoach.app.domain.repository.ExerciseRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.IsoFields
import javax.inject.Inject

data class E1RMDataPoint(
    val date: Instant,
    val e1rm: Double,
    val weight: Double,
    val reps: Int
)

data class PREntry(
    val date: Instant,
    val weight: Double,
    val reps: Int,
    val e1rm: Double
)

data class WeeklyVolumeEntry(
    val weekLabel: String,      // e.g. "2026-W38"
    val weekStart: Long,        // epoch ms of Monday
    val volumeKg: Double
)

data class ProgressionAnalyticsUiState(
    val exerciseId: Long = 0L,
    val exerciseName: String = "",
    val e1rmTrend: List<E1RMDataPoint> = emptyList(),
    val prHistory: List<PREntry> = emptyList(),
    val weeklyVolume: List<WeeklyVolumeEntry> = emptyList(),
    val peakE1RM: Double = 0.0,
    val recentE1RM: Double = 0.0,
    val totalSessions: Int = 0,
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProgressionAnalyticsViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val prDetector: PRDetector,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _exerciseId = MutableStateFlow(0L)
    private val _uiState = MutableStateFlow(ProgressionAnalyticsUiState())
    val uiState: StateFlow<ProgressionAnalyticsUiState> = _uiState.asStateFlow()

    fun loadExercise(exerciseId: Long, exerciseName: String) {
        _exerciseId.value = exerciseId
        _uiState.update { it.copy(exerciseId = exerciseId, exerciseName = exerciseName, isLoading = true) }
        viewModelScope.launch {
            val resolvedId = if (exerciseId <= 0L && exerciseName.isNotBlank()) {
                try {
                    val all = exerciseRepository.getAllExercises().first()
                    all.firstOrNull { it.name.equals(exerciseName, ignoreCase = true) }?.id ?: exerciseId
                } catch (e: Exception) {
                    exerciseId
                }
            } else {
                exerciseId
            }
            _exerciseId.value = resolvedId
            workoutRepository.getCompletedSetsWithContext()
                .map { allSets -> computeAnalytics(resolvedId, exerciseName, allSets) }
                .catch { e ->
                    android.util.Log.e("ProgressionAnalyticsVM", "Error loading progression data", e)
                    _uiState.update { it.copy(isLoading = false, isEmpty = true) }
                }
                .collect { state -> _uiState.value = state }
        }
    }

    private fun computeAnalytics(
        exerciseId: Long,
        exerciseName: String,
        allSets: List<VolumeCalculator.SetWithContext>
    ): ProgressionAnalyticsUiState {
        // Filter to this exercise's completed normal sets
        val exerciseSets = allSets.filter {
            it.exerciseId == exerciseId && it.set.completed && it.set.setType == 0
        }.sortedBy { it.workoutDate }

        if (exerciseSets.isEmpty()) {
            return ProgressionAnalyticsUiState(
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                isLoading = false,
                isEmpty = true
            )
        }

        // --- E1RM trend: best e1RM per workout session ---
        val e1rmBySession = exerciseSets
            .groupBy { it.workoutDate }
            .map { (date, sets) ->
                val bestSet = sets.maxByOrNull { prDetector.calculateEstimated1RM(it.set.weight, it.set.reps) }!!
                val e1rm = prDetector.calculateEstimated1RM(bestSet.set.weight, bestSet.set.reps)
                E1RMDataPoint(
                    date = Instant.ofEpochMilli(date),
                    e1rm = e1rm,
                    weight = bestSet.set.weight,
                    reps = bestSet.set.reps
                )
            }
            .sortedBy { it.date }

        // --- PR history: sessions where e1RM is a new all-time high ---
        val prHistory = mutableListOf<PREntry>()
        var runningMax = 0.0
        for (point in e1rmBySession) {
            if (point.e1rm > runningMax) {
                runningMax = point.e1rm
                prHistory.add(PREntry(
                    date = point.date,
                    weight = point.weight,
                    reps = point.reps,
                    e1rm = point.e1rm
                ))
            }
        }

        // --- Weekly volume ---
        val zoneId = ZoneId.systemDefault()
        val weeklyVolume = exerciseSets
            .groupBy { set ->
                val zdt = Instant.ofEpochMilli(set.workoutDate).atZone(zoneId)
                val year = zdt.get(IsoFields.WEEK_BASED_YEAR)
                val week = zdt.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                val weekStart = zdt.with(java.time.DayOfWeek.MONDAY)
                    .toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
                Triple(year, week, weekStart)
            }
            .map { (key, sets) ->
                val (year, week, weekStart) = key
                val volume = sets.sumOf { it.set.weight * it.set.reps }
                WeeklyVolumeEntry(
                    weekLabel = "$year-W${week.toString().padStart(2, '0')}",
                    weekStart = weekStart,
                    volumeKg = volume
                )
            }
            .sortedBy { it.weekStart }
            .takeLast(12) // Last 12 weeks

        val peakE1RM = e1rmBySession.maxOfOrNull { it.e1rm } ?: 0.0
        val recentE1RM = e1rmBySession.lastOrNull()?.e1rm ?: 0.0
        val totalSessions = e1rmBySession.size

        return ProgressionAnalyticsUiState(
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            e1rmTrend = e1rmBySession,
            prHistory = prHistory.reversed(), // Most recent first
            weeklyVolume = weeklyVolume,
            peakE1RM = peakE1RM,
            recentE1RM = recentE1RM,
            totalSessions = totalSessions,
            isLoading = false,
            isEmpty = false
        )
    }
}
