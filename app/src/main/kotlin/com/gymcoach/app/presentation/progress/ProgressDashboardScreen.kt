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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.domain.repository.AnalyticsRepository
import com.gymcoach.app.domain.repository.PersonalRecord
import com.gymcoach.app.domain.repository.WorkoutCounts
import com.gymcoach.app.domain.model.WorkoutWithStats
import com.gymcoach.app.presentation.history.formatDuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import com.gymcoach.app.domain.repository.MuscleGroupStats
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
    val error: String? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    private var allPersonalRecords: List<PersonalRecord> = emptyList()
    private var allMuscleGroupDistribution: List<MuscleGroupStats> = emptyList()

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
                val totalExercises = analyticsRepository.getTotalExercises()
                val averageWorkoutVolume = analyticsRepository.getAverageWorkoutVolume()
                val averageWorkoutDurationMinutes = analyticsRepository.getAverageWorkoutDurationMinutes()
                val weeklyTrend = calculateWeeklyTrend(weekly)
                val workoutFrequency = calculateWorkoutFrequency(weekly)
                val workoutCounts = analyticsRepository.getWorkoutCounts()
                val longestWorkout = analyticsRepository.getLongestWorkout()
                val shortestWorkout = analyticsRepository.getShortestWorkout()

                allPersonalRecords = prs
                allMuscleGroupDistribution = muscleGroupDistribution

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
                    totalExercises = totalExercises,
                    averageWorkoutVolume = averageWorkoutVolume,
                    averageWorkoutDurationMinutes = averageWorkoutDurationMinutes,
                    weeklyTrend = weeklyTrend,
                    workoutFrequency = workoutFrequency,
                    workoutCounts = workoutCounts,
                    longestWorkout = longestWorkout,
                    shortestWorkout = shortestWorkout
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

    fun onSearchQueryChange(query: String) {
        val filteredPRs = if (query.isBlank()) allPersonalRecords
            else allPersonalRecords.filter { it.exerciseName.contains(query, ignoreCase = true) }
        val filteredMG = if (query.isBlank()) allMuscleGroupDistribution
            else allMuscleGroupDistribution.filter { it.name.contains(query, ignoreCase = true) }
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            personalRecords = filteredPRs,
            muscleGroupDistribution = filteredMG
        )
    }
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
            ProgressTopAppBar(
                title = "Progress",
                onBackClick = onBackClick,
                onRefresh = { viewModel.refresh() },
                onSearch = { viewModel.onSearchQueryChange(it) }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                CircularProgressIndicatorScreen(padding)
            }
            state.error != null -> {
                ErrorScreen(state.error!!, padding)
            }
            else -> {
                ProgressDataContent(
                    state = state,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressTopAppBar(
    title: String,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
    onSearch: (String) -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                modifier = Modifier.semantics {
                    contentDescription = "$title progress dashboard"
                }
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick, modifier = Modifier.height(48.dp).width(48.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to exercise list"
                )
            }
        },
        actions = {
            IconButton(onClick = onRefresh, modifier = Modifier.height(48.dp).width(48.dp)) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Refresh data"
                )
            }
        }
    )
}

@Composable
private fun CircularProgressIndicatorScreen(padding: androidx.compose.foundation.layout.PaddingValues) {
    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorScreen(error: String, padding: androidx.compose.foundation.layout.PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Error: $error",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun ProgressDataContent(
    state: ProgressUiState,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        SearchField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )

        Spacer(Modifier.height(8.dp))

        StatsOverview(
            totalWorkouts = state.workoutCounts.total,
            todayWorkouts = state.workoutCounts.today,
            weekWorkouts = state.workoutCounts.week,
            monthWorkouts = state.workoutCounts.month,
            totalExercises = state.totalExercises,
            totalSets = state.totalSets,
            totalReps = state.totalReps,
            totalVolume = state.totalVolume,
            totalTrainingTimeMinutes = state.totalTrainingTimeMinutes
        )

        Spacer(Modifier.height(16.dp))

        SectionHeader("Workout Extremes")
        Spacer(Modifier.height(8.dp))
        workoutExtremesRow(
            longestWorkout = state.longestWorkout,
            shortestWorkout = state.shortestWorkout
        )

        Spacer(Modifier.height(16.dp))

        trainingTimeAndAverages(
            averageWorkoutDurationMinutes = state.averageWorkoutDurationMinutes,
            averageWorkoutVolume = state.averageWorkoutVolume
        )

        Spacer(Modifier.height(16.dp))

        workoutFrequencyAndTrend(
            workoutFrequency = state.workoutFrequency,
            weeklyTrend = state.weeklyTrend
        )

        Spacer(Modifier.height(24.dp))

        VolumeHistoryChartSection(
            volumeHistory = state.volumeHistory
        )

        Spacer(Modifier.height(24.dp))

        WeeklySummarySection(
            weeklySummary = state.weeklySummary
        )

        Spacer(Modifier.height(24.dp))

        MonthlySummarySection(
            monthlySummary = state.monthlySummary
        )

        Spacer(Modifier.height(24.dp))

        TopExercisesSection(
            muscleGroupDistribution = state.muscleGroupDistribution
        )

        Spacer(Modifier.height(24.dp))

        PersonalRecordsSection(
            personalRecords = state.personalRecords
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text("Search exercises...") },
        leadingIcon = {
            Icon(
                Icons.Default.FilterList,
                contentDescription = "Filter exercises",
                modifier = Modifier.size(24.dp)
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search,
            keyboardType = KeyboardType.Text
        )
    )
}

@Composable
private fun workoutExtremesRow(
    longestWorkout: WorkoutWithStats?,
    shortestWorkout: WorkoutWithStats?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        longestWorkout?.let {
            StatCard(
                label = "Longest Workout",
                value = formatDuration(it.duration),
                modifier = Modifier.weight(1f)
            )
        }
        shortestWorkout?.let {
            StatCard(
                label = "Shortest Workout",
                value = formatDuration(it.duration),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun trainingTimeAndAverages(
    averageWorkoutDurationMinutes: Long,
    averageWorkoutVolume: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            label = "Avg Duration",
            value = "${averageWorkoutDurationMinutes}m",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Avg Volume",
            value = "%.1f kg".format(averageWorkoutVolume),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun workoutFrequencyAndTrend(
    workoutFrequency: Int,
    weeklyTrend: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            label = "Weekly Workouts",
            value = "${workoutFrequency}",
            modifier = Modifier.weight(1f)
        )
        val trendSymbol = when {
            weeklyTrend > 0 -> "▲ +%.1f%%".format(weeklyTrend)
            weeklyTrend < 0 -> "▼ %.1f%%".format(weeklyTrend)
            else -> "• 0.0%%"
        }
        val trendColor = when {
            weeklyTrend > 0 -> MaterialTheme.colorScheme.primary
            weeklyTrend < 0 -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        StatCard(
            label = "Weekly Trend",
            value = trendSymbol,
            modifier = Modifier.weight(1f),
            valueColor = trendColor
        )
    }
}

@Composable
private fun VolumeHistoryChartSection(volumeHistory: List<Pair<Date, Double>>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionHeader("Volume History")
        Spacer(Modifier.height(8.dp))
        if (volumeHistory.isNotEmpty()) {
            VolumeLineChart(
                data = volumeHistory,
                title = "Weekly volume over time",
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )
        } else {
            EmptyPlaceholder("No volume data yet")
        }
    }
}

@Composable
private fun WeeklySummarySection(weeklySummary: List<Pair<Date, Double>>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionHeader("Weekly Summary")
        Spacer(Modifier.height(8.dp))
        if (weeklySummary.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(weeklySummary, key = { it.first.time }) { (date, volume) ->
                    SummaryRow(
                        label = weekLabel(date),
                        value = "%.0f kg".format(volume)
                    )
                }
            }
        } else {
            EmptyPlaceholder("No weekly data yet")
        }
    }
}

@Composable
private fun MonthlySummarySection(monthlySummary: List<Pair<Date, Double>>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionHeader("Monthly Summary")
        Spacer(Modifier.height(8.dp))
        if (monthlySummary.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(monthlySummary, key = { it.first.time }) { (date, volume) ->
                    SummaryRow(
                        label = monthLabel(date),
                        value = "%.0f kg".format(volume)
                    )
                }
            }
        } else {
            EmptyPlaceholder("No monthly data yet")
        }
    }
}

@Composable
private fun TopExercisesSection(muscleGroupDistribution: List<MuscleGroupStats>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionHeader("Top Exercises")
        Spacer(Modifier.height(8.dp))
        if (muscleGroupDistribution.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(muscleGroupDistribution, key = { it.name.lowercase() }) { stat ->
                    SummaryRow(
                        label = stat.name,
                        value = "${stat.totalReps} reps"
                    )
                }
            }
        } else {
            EmptyPlaceholder("No exercise data yet")
        }
    }
}

@Composable
private fun PersonalRecordsSection(personalRecords: List<PersonalRecord>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionHeader("Personal Records")
        Spacer(Modifier.height(8.dp))
        if (personalRecords.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(personalRecords, key = { it.exerciseName.lowercase() }) { pr ->
                    SummaryRow(
                        label = pr.exerciseName,
                        value = "%.1f kg".format(pr.maxWeight)
                    )
                }
            }
        } else {
            EmptyPlaceholder("No PRs recorded yet")
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
    title: String,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val maxVolume = if (data.isNotEmpty()) data.maxOf { it.second } else 0.0
    val maxVolumeLabel = remember(maxVolume) {
        if (maxVolume >= 1000) String.format("%.1fk", maxVolume / 1000.0) else String.format("%.0f", maxVolume)
    }
    val firstDateLabel = remember(data) {
        if (data.isNotEmpty()) dateFormat.format(data.first().first) else ""
    }
    val lastDateLabel = remember(data) {
        if (data.isNotEmpty()) dateFormat.format(data.last().first) else ""
    }

    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics {
                        contentDescription = "Volume history chart showing weekly volume over time"
                    }
            ) {
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

            // Y-axis labels
            Text(
                text = maxVolumeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 2.dp, top = 2.dp)
            )
            Text(
                text = "0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 2.dp, bottom = 2.dp)
            )

            // X-axis labels
            Text(
                text = firstDateLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 2.dp)
            )
            Text(
                text = lastDateLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp, bottom = 2.dp)
            )
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
    todayWorkouts: Int,
    weekWorkouts: Int,
    monthWorkouts: Int,
    totalExercises: Int,
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
            StatCard(label = "Workouts", value = "$totalWorkouts", modifier = Modifier.weight(1f))
            StatCard(label = "Today", value = "$todayWorkouts", modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(label = "Week", value = "$weekWorkouts", modifier = Modifier.weight(1f))
            StatCard(label = "Month", value = "$monthWorkouts", modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(label = "Exercises", value = "$totalExercises", modifier = Modifier.weight(1f))
            StatCard(label = "Sets", value = "$totalSets", modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(label = "Reps", value = "$totalReps", modifier = Modifier.weight(1f))
            StatCard(label = "Volume", value = "%.1f kg".format(totalVolume), modifier = Modifier.weight(1f))
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
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.primary
) {
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
                color = valueColor
            )
        }
    }
}