package com.gymcoach.app.presentation.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.domain.repository.AnalyticsRepository
import com.gymcoach.app.domain.repository.MuscleGroupStats
import com.gymcoach.app.domain.repository.PersonalRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// --- ViewModel ---

data class ProgressUiState(
    val isLoading: Boolean = true,
    val volumeHistory: List<Pair<Date, Double>> = emptyList(),
    val weeklySummary: List<Pair<Date, Double>> = emptyList(),
    val monthlySummary: List<Pair<Date, Double>> = emptyList(),
    val personalRecords: List<PersonalRecord> = emptyList(),
    val muscleGroupDistribution: List<MuscleGroupStats> = emptyList(),
    val totalWorkouts: Int = 0,
    val totalSets: Int = 0,
    val totalReps: Int = 0,
    val totalVolume: Double = 0.0,
    val totalTrainingTimeMinutes: Long = 0,
    val averageWorkoutVolume: Double = 0.0,
    val averageWorkoutDurationMinutes: Long = 0,
    val weeklyTrend: Double = 0.0,
    val workoutFrequency: Int = 0,
    val error: String? = null
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = ProgressUiState(isLoading = true)
            try {
                val volume = analyticsRepository.getVolumeHistory()
                val weekly = analyticsRepository.getWeeklySummary()
                val monthly = analyticsRepository.getMonthlyVolumes()
                val prs = analyticsRepository.getAllPersonalRecords()
                val muscleGroupDistribution = analyticsRepository.getMuscleGroupDistribution()
                val totalWorkouts = analyticsRepository.getTotalWorkouts()
                val totalSets = analyticsRepository.getTotalSets()
                val totalReps = analyticsRepository.getTotalReps()
                val totalVolume = analyticsRepository.getTotalVolume()
                val totalTrainingTimeMinutes = analyticsRepository.getTotalTrainingTimeMinutes()
                val averageWorkoutVolume = analyticsRepository.getAverageWorkoutVolume()
                val averageWorkoutDurationMinutes = analyticsRepository.getAverageWorkoutDurationMinutes()
                val weeklyTrend = calculateWeeklyTrend(weekly)
                val workoutFrequency = calculateWorkoutFrequency(weekly)
                _uiState.value = ProgressUiState(
                    isLoading = false,
                    volumeHistory = volume,
                    weeklySummary = weekly,
                    monthlySummary = monthly,
                    personalRecords = prs,
                    muscleGroupDistribution = muscleGroupDistribution,
                    totalWorkouts = totalWorkouts,
                    totalSets = totalSets,
                    totalReps = totalReps,
                    totalVolume = totalVolume,
                    totalTrainingTimeMinutes = totalTrainingTimeMinutes,
                    averageWorkoutVolume = averageWorkoutVolume,
                    averageWorkoutDurationMinutes = averageWorkoutDurationMinutes,
                    weeklyTrend = weeklyTrend,
                    workoutFrequency = workoutFrequency
                )
            } catch (e: Exception) {
                _uiState.value = ProgressUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load progress data"
                )
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

    private fun calculateWorkoutFrequency(weekly: List<Pair<Date, Double>>): Int {
        return weekly.size
    }

    fun refresh() = loadData()
}

// --- Screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressDashboardScreen(
    onBackClick: () -> Unit,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.error!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(8.dp))

                    // Stats Overview
                    StatsOverview(
                        totalWorkouts = state.totalWorkouts,
                        totalSets = state.totalSets,
                        totalReps = state.totalReps,
                        totalVolume = state.totalVolume,
                        totalTrainingTimeMinutes = state.totalTrainingTimeMinutes
                    )

                    Spacer(Modifier.height(16.dp))

                    // Training Time & Averages
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            label = "Avg Duration",
                            value = "${state.averageWorkoutDurationMinutes}m",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Avg Volume",
                            value = "%.1f kg".format(state.averageWorkoutVolume),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Workout Frequency & Weekly Trend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            label = "Weekly Workouts",
                            value = "${state.workoutFrequency}",
                            modifier = Modifier.weight(1f)
                        )
                        val trendSymbol = when {
                            state.weeklyTrend > 0 -> "▲ +%.1f%%".format(state.weeklyTrend)
                            state.weeklyTrend < 0 -> "▼ %.1f%%".format(state.weeklyTrend)
                            else -> "• 0.0%%"
                        }
                        StatCard(
                            label = "Weekly Trend",
                            value = trendSymbol,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Volume History Chart
                    SectionHeader("Volume History")
                    Spacer(Modifier.height(8.dp))
                    if (state.volumeHistory.isNotEmpty()) {
                        VolumeLineChart(
                            data = state.volumeHistory,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    } else {
                        EmptyPlaceholder("No volume data yet")
                    }

                    Spacer(Modifier.height(24.dp))

                    // Weekly Summary
                    SectionHeader("Weekly Summary")
                    Spacer(Modifier.height(8.dp))
                    if (state.weeklySummary.isNotEmpty()) {
                        state.weeklySummary.forEach { (date, volume) ->
                            SummaryRow(
                                label = weekLabel(date),
                                value = "%.0f kg".format(volume)
                            )
                        }
                    } else {
                        EmptyPlaceholder("No weekly data yet")
                    }

                    Spacer(Modifier.height(24.dp))

                    // Monthly Summary
                    SectionHeader("Monthly Summary")
                    Spacer(Modifier.height(8.dp))
                    if (state.monthlySummary.isNotEmpty()) {
                        state.monthlySummary.forEach { (date, volume) ->
                            SummaryRow(
                                label = monthLabel(date),
                                value = "%.0f kg".format(volume)
                            )
                        }
                    } else {
                        EmptyPlaceholder("No monthly data yet")
                    }

                    Spacer(Modifier.height(24.dp))

                    // Muscle Group Distribution
                    SectionHeader("Top Exercises")
                    Spacer(Modifier.height(8.dp))
                    if (state.muscleGroupDistribution.isNotEmpty()) {
                        state.muscleGroupDistribution.forEach { stat ->
                            SummaryRow(
                                label = stat.name,
                                value = "${stat.totalReps} reps"
                            )
                        }
                    } else {
                        EmptyPlaceholder("No exercise data yet")
                    }

                    Spacer(Modifier.height(24.dp))

                    // Personal Records
                    SectionHeader("Personal Records")
                    Spacer(Modifier.height(8.dp))
                    if (state.personalRecords.isNotEmpty()) {
                        state.personalRecords.forEach { pr ->
                            SummaryRow(
                                label = pr.exerciseName,
                                value = "%.1f kg".format(pr.maxWeight)
                            )
                        }
                    } else {
                        EmptyPlaceholder("No PRs recorded yet")
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

// --- Components ---

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun EmptyPlaceholder(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun VolumeLineChart(
    data: List<Pair<Date, Double>>,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas

        val paddingLeft = 8f
        val paddingBottom = 8f
        val chartWidth = size.width - paddingLeft
        val chartHeight = size.height - paddingBottom

        val values = data.map { it.second }
        val minVal = values.min()
        val maxVal = values.max()
        val range = (maxVal - minVal).coerceAtLeast(1.0)

        // Grid lines (3 horizontal)
        for (i in 0..3) {
            val y = chartHeight - (chartHeight * i / 3f)
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        // Line path
        val path = Path()
        val stepX = chartWidth / (data.size - 1).toFloat()

        data.forEachIndexed { index, (_, volume) ->
            val x = paddingLeft + index * stepX
            val y = chartHeight - ((volume - minVal) / range * chartHeight).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Data point dots
        data.forEachIndexed { index, (_, volume) ->
            val x = paddingLeft + index * stepX
            val y = chartHeight - ((volume - minVal) / range * chartHeight).toFloat()
            drawCircle(color = lineColor, radius = 4f, center = Offset(x, y))
        }
    }
}

private fun weekLabel(date: Date): String {
    val fmt = SimpleDateFormat("MMM dd", Locale.getDefault())
    return "Week of ${fmt.format(date)}"
}

private fun monthLabel(date: Date): String {
    val fmt = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    return fmt.format(date)
}

@Composable
private fun StatsOverview(
    totalWorkouts: Int,
    totalSets: Int,
    totalReps: Int,
    totalVolume: Double,
    totalTrainingTimeMinutes: Long
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = "Workouts",
                value = "$totalWorkouts",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Sets",
                value = "$totalSets",
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = "Reps",
                value = "$totalReps",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Volume",
                value = "%.1f kg".format(totalVolume),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = "Time",
                value = "${totalTrainingTimeMinutes}m",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Est. Calories",
                value = "%.0f".format(totalVolume * 0.05),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
