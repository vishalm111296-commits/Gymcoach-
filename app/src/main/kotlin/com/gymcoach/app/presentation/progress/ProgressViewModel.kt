package com.gymcoach.app.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.data.local.dao.BodyMeasurementDao
import com.gymcoach.app.data.local.entity.BodyMeasurementEntity
import com.gymcoach.app.domain.model.WorkoutWithStats
import com.gymcoach.app.domain.repository.AnalyticsRepository
import com.gymcoach.app.domain.repository.MuscleGroupStats
import com.gymcoach.app.domain.repository.PersonalRecord
import com.gymcoach.app.domain.repository.WorkoutCounts
import com.gymcoach.app.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TARGET_SESSIONS_PER_WEEK = 4
private const val DEFAULT_MUSCLE_MIN_SETS = 10
private const val DEFAULT_MUSCLE_MAX_SETS = 20

data class ProgressUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    // Legacy dashboard fields
    val volumeHistory: List<Pair<Date, Double>> = emptyList(),
    val weeklySummary: List<Pair<Date, Double>> = emptyList(),
    val monthlySummary: List<Pair<Date, Double>> = emptyList(),
    val personalRecords: List<PersonalRecord> = emptyList(),
    val muscleGroupDistribution: List<MuscleGroupStats> = emptyList(),
    val totalWorkouts: Int = 0,
    val totalSets: Int = 0,
    val totalReps: Int = 0,
    val totalExercises: Int = 0,
    val totalVolume: Double = 0.0,
    val totalTrainingTimeMinutes: Long = 0,
    val averageWorkoutVolume: Double = 0.0,
    val averageWorkoutDurationMinutes: Long = 0,
    val weeklyTrend: Double = 0.0,
    val workoutFrequency: Int = 0,
    val workoutCounts: WorkoutCounts = WorkoutCounts(0, 0, 0, 0),
    val longestWorkout: WorkoutWithStats? = null,
    val shortestWorkout: WorkoutWithStats? = null,
    // Enhanced progress experience
    val dateRange: ProgressDateRange = ProgressDateRange.EIGHT_WEEKS,
    val workoutsThisWeek: Int = 0,
    val adherence: Float = 0f,
    val muscleVolume: List<MuscleVolumeData> = emptyList(),
    val selectedExercise: String? = null,
    val strengthPoints: List<ProgressPoint> = emptyList(),
    val recentPRs: List<PersonalRecordItem> = emptyList(),
    val bodyweightTrend: List<TrendPoint> = emptyList(),
    val waistTrend: List<TrendPoint> = emptyList(),
    val bodyweightDirection: TrendDirection = TrendDirection.STABLE,
    val waistDirection: TrendDirection = TrendDirection.STABLE,
    val workoutDays: Set<LocalDate> = emptySet(),
    // Body measurements
    val latestWeight: Double? = null,
    val latestWaist: Double? = null,
    val latestChest: Double? = null,
    val latestBodyFat: Double? = null,
    val showMeasurementDialog: Boolean = false
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val workoutRepository: WorkoutRepository,
    private val bodyMeasurementDao: BodyMeasurementDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

    fun selectDateRange(range: ProgressDateRange) {
        _uiState.update { it.copy(dateRange = range) }
        load()
    }

    fun selectExercise(name: String) {
        _uiState.update { it.copy(selectedExercise = name) }
        load()
    }

    fun showMeasurementDialog() {
        _uiState.update { it.copy(showMeasurementDialog = true) }
    }

    fun hideMeasurementDialog() {
        _uiState.update { it.copy(showMeasurementDialog = false) }
    }

    fun saveMeasurement(weightKg: Double, waistCm: Double?, chestCm: Double?, bodyFatPct: Double?, notes: String) {
        viewModelScope.launch {
            bodyMeasurementDao.insert(
                BodyMeasurementEntity(
                    weightKg = weightKg,
                    waistCm = waistCm,
                    chestCm = chestCm,
                    bodyFatPct = bodyFatPct,
                    notes = notes
                )
            )
            _uiState.update { it.copy(showMeasurementDialog = false) }
            load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val zone = ZoneId.systemDefault()
                val today = LocalDate.now()
                val weekStart = today.with(DayOfWeek.MONDAY)

                val completed = workoutRepository.getCompletedWorkouts()
                    .first()
                    .filter { it.completed }
                    .sortedBy { it.date }

                val workoutsThisWeek = completed.count { it.date.toLocalDate(zone) >= weekStart }
                val adherence = (workoutsThisWeek.toFloat() / TARGET_SESSIONS_PER_WEEK).coerceIn(0f, 1f)

                val windowStart = today.minusWeeks(HEATMAP_WEEKS.toLong())
                val muscleSets = linkedMapOf<String, Int>()
                val workoutDays = sortedSetOf<LocalDate>()
                val bestByExerciseDate = mutableMapOf<String, MutableMap<LocalDate, Double>>()
                val prByExercise = mutableMapOf<String, Triple<Double, Int, LocalDate>>()

                for (workout in completed) {
                    val day = workout.date.toLocalDate(zone)
                    if (day.isBefore(windowStart)) continue
                    workoutDays += day
                    val details = workoutRepository.getWorkoutWithDetails(workout.id).first() ?: continue
                    for (entry in details.exercises) {
                        val doneSets = entry.sets.filter { it.completed }
                        if (doneSets.isEmpty()) continue
                        val muscle = entry.exercise.muscleGroup.uppercase()
                        muscleSets[muscle] = (muscleSets[muscle] ?: 0) + doneSets.size
                        val bestSet = doneSets.maxBy { it.weight }
                        val previous = prByExercise[entry.exercise.name]
                        if (previous == null || bestSet.weight > previous.first) {
                            prByExercise[entry.exercise.name] = Triple(bestSet.weight, bestSet.reps, day)
                        }
                        val series = bestByExerciseDate.getOrPut(entry.exercise.name) { mutableMapOf() }
                        series[day] = maxOf(series[day] ?: 0.0, bestSet.weight)
                    }
                }

                val selectedExercise = _uiState.value.selectedExercise
                    ?: prByExercise.entries.maxByOrNull { it.value.first }?.key
                val rangeStart = today.minusWeeks(_uiState.value.dateRange.weeks.toLong())
                val strengthPoints = bestByExerciseDate[selectedExercise]
                    ?.map { (day, value) -> ProgressPoint(day, value) }
                    ?.filter { it.date >= rangeStart }
                    ?.sortedBy { it.date }
                    ?: emptyList()

                // --- Body Measurements from DAO ---
                val measurements = bodyMeasurementDao.getAll().first()
                val zoneId = ZoneId.systemDefault()

                val bodyweightTrend = measurements
                    .filter { it.weightKg > 0 }
                    .sortedBy { it.recordedAt }
                    .map { measurement ->
                        TrendPoint(
                            date = Instant.ofEpochMilli(measurement.recordedAt).atZone(zoneId).toLocalDate(),
                            value = measurement.weightKg
                        )
                    }

                val waistTrend = measurements
                    .filter { it.waistCm != null && it.waistCm > 0 }
                    .sortedBy { it.recordedAt }
                    .map { measurement ->
                        TrendPoint(
                            date = Instant.ofEpochMilli(measurement.recordedAt).atZone(zoneId).toLocalDate(),
                            value = measurement.waistCm!!
                        )
                    }

                val latest = measurements.firstOrNull()

                val volumeHistory = analyticsRepository.getVolumeHistory()
                val weekly = analyticsRepository.getWeeklySummary()
                val state = ProgressUiState(
                    isLoading = false,
                    dateRange = _uiState.value.dateRange,
                    selectedExercise = selectedExercise,
                    workoutsThisWeek = workoutsThisWeek,
                    adherence = adherence,
                    muscleVolume = muscleSets.entries
                        .sortedByDescending { it.value }
                        .map {
                            MuscleVolumeData(
                                muscleName = it.key,
                                currentSets = it.value,
                                targetMin = DEFAULT_MUSCLE_MIN_SETS,
                                targetMax = DEFAULT_MUSCLE_MAX_SETS
                            )
                        },
                    strengthPoints = strengthPoints,
                    recentPRs = prByExercise.map { (name, pr) ->
                        PersonalRecordItem(
                            exerciseName = name,
                            achievement = "%,.0f kg \u00d7 %d".format(pr.first, pr.second),
                            date = pr.third
                        )
                    }.sortedByDescending { it.date },
                    bodyweightTrend = bodyweightTrend,
                    waistTrend = waistTrend,
                    workoutDays = workoutDays,
                    volumeHistory = volumeHistory,
                    weeklySummary = weekly,
                    monthlySummary = analyticsRepository.getMonthlyVolumes(),
                    personalRecords = analyticsRepository.getAllPersonalRecords(),
                    muscleGroupDistribution = analyticsRepository.getMuscleGroupDistribution(),
                    totalWorkouts = analyticsRepository.getTotalWorkouts(),
                    totalSets = analyticsRepository.getTotalSets(),
                    totalReps = analyticsRepository.getTotalReps(),
                    totalExercises = analyticsRepository.getTotalExercises(),
                    totalVolume = analyticsRepository.getTotalVolume(),
                    totalTrainingTimeMinutes = analyticsRepository.getTotalTrainingTimeMinutes(),
                    averageWorkoutVolume = analyticsRepository.getAverageWorkoutVolume(),
                    averageWorkoutDurationMinutes = analyticsRepository.getAverageWorkoutDurationMinutes(),
                    weeklyTrend = calculateWeeklyTrend(weekly),
                    workoutFrequency = weekly.size,
                    workoutCounts = analyticsRepository.getWorkoutCounts(),
                    longestWorkout = analyticsRepository.getLongestWorkout(),
                    shortestWorkout = analyticsRepository.getShortestWorkout(),
                    latestWeight = latest?.weightKg,
                    latestWaist = latest?.waistCm,
                    latestChest = latest?.chestCm,
                    latestBodyFat = latest?.bodyFatPct
                )
                _uiState.value = state.copy(
                    bodyweightDirection = trendDirection(state.bodyweightTrend),
                    waistDirection = trendDirection(state.waistTrend)
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load progress data")
                }
            }
        }
    }

    private fun calculateWeeklyTrend(weekly: List<Pair<Date, Double>>): Double {
        if (weekly.size < 2) return 0.0
        val recent = weekly.takeLast(2)
        val prev = recent[0].second
        val curr = recent[1].second
        return if (prev == 0.0) 0.0 else ((curr - prev) / prev) * 100
    }

    private fun trendDirection(points: List<TrendPoint>): TrendDirection {
        if (points.size < 2) return TrendDirection.STABLE
        val first = points.first().value
        val last = points.last().value
        return when {
            last > first * 1.01 -> TrendDirection.UP
            last < first * 0.99 -> TrendDirection.DOWN
            else -> TrendDirection.STABLE
        }
    }

    private fun Instant.toLocalDate(zone: ZoneId): LocalDate = atZone(zone).toLocalDate()

    private companion object {
        const val HEATMAP_WEEKS = 12
    }
}
