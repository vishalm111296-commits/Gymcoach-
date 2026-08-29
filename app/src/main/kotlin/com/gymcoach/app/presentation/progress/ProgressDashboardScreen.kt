package com.gymcoach.app.presentation.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.presentation.history.formatDuration
import com.gymcoach.app.presentation.progress.components.BodyMeasurementTrend
import com.gymcoach.app.presentation.progress.components.MeasurementLogDialog
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressDashboardScreen(
    onBackClick: () -> Unit,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Measurement dialog
    if (state.showMeasurementDialog) {
        MeasurementLogDialog(
            latestWeight = state.latestWeight,
            latestWaist = state.latestWaist,
            latestChest = state.latestChest,
            latestBodyFat = state.latestBodyFat,
            onDismiss = { viewModel.hideMeasurementDialog() },
            onSave = { weight, waist, chest, bodyFat, notes ->
                viewModel.saveMeasurement(weight, waist, chest, bodyFat, notes)
            }
        )
    }

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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showMeasurementDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Log Measurement",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
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
                        text = state.error ?: "Unknown error",
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

                    // Date Range Selector
                    DateRangeSelector(
                        selected = state.dateRange,
                        onSelect = { viewModel.selectDateRange(it) }
                    )

                    Spacer(Modifier.height(16.dp))

                    // Weekly Adherence
                    WorkoutAdherenceCard(
                        workoutsThisWeek = state.workoutsThisWeek,
                        targetSessionsPerWeek = 4,
                        adherence = state.adherence
                    )

                    Spacer(Modifier.height(16.dp))

                    // Body Measurements
                    SectionHeader("Body Measurements")
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BodyMeasurementTrend(
                            label = "Bodyweight",
                            currentValue = state.latestWeight,
                            unit = "kg",
                            trend = state.bodyweightDirection,
                            dataPoints = state.bodyweightTrend,
                            modifier = Modifier.weight(1f),
                            goodWhenDown = false
                        )
                        BodyMeasurementTrend(
                            label = "Waist",
                            currentValue = state.latestWaist,
                            unit = "cm",
                            trend = state.waistDirection,
                            dataPoints = state.waistTrend,
                            modifier = Modifier.weight(1f),
                            goodWhenDown = true
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Stats Overview
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

                    // Workout Extremes
                    SectionHeader("Workout Extremes")
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.longestWorkout?.let {
                            StatCard(
                                label = "Longest Workout",
                                value = formatDuration(it.duration),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        state.shortestWorkout?.let {
                            StatCard(
                                label = "Shortest Workout",
                                value = formatDuration(it.duration),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

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
                            state.weeklyTrend > 0 -> "\u25b2 +%.1f%%".format(state.weeklyTrend)
                            state.weeklyTrend < 0 -> "\u25bc %.1f%%".format(state.weeklyTrend)
                            else -> "--"
                        }
                        StatCard(
                            label = "Weekly Trend",
                            value = trendSymbol,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Strength Progression Chart
                    SectionHeader("Strength Progression")
                    Spacer(Modifier.height(8.dp))
                    state.selectedExercise?.let { exercise ->
                        ExerciseSelector(
                            selectedExercise = exercise,
                            onSelect = { viewModel.selectExercise(it) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    if (state.strengthPoints.isNotEmpty()) {
                        StrengthLineChart(
                            data = state.strengthPoints,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    } else {
                        EmptyPlaceholder("No strength data yet")
                    }

                    Spacer(Modifier.height(24.dp))

                    // Muscle Volume Heatmap
                    SectionHeader("Muscle Volume")
                    Spacer(Modifier.height(8.dp))
                    if (state.muscleVolume.isNotEmpty()) {
                        state.muscleVolume.forEach { muscle ->
                            MuscleVolumeBar(
                                muscleName = muscle.muscleName,
                                currentSets = muscle.currentSets,
                                targetMin = muscle.targetMin,
                                targetMax = muscle.targetMax
                            )
                        }
                    } else {
                        EmptyPlaceholder("No muscle volume data yet")
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

                    // Recent Personal Records
                    SectionHeader("Recent PRs")
                    Spacer(Modifier.height(8.dp))
                    if (state.recentPRs.isNotEmpty()) {
                        state.recentPRs.forEach { pr ->
                            PRCard(
                                exerciseName = pr.exerciseName,
                                achievement = pr.achievement,
                                date = pr.date
                            )
                        }
                    } else {
                        EmptyPlaceholder("No PRs recorded yet")
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

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

// --- Components ---

@Composable
private fun DateRangeSelector(
    selected: ProgressDateRange,
    onSelect: (ProgressDateRange) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProgressDateRange.entries.forEach { range ->
            FilterChip(
                selected = range == selected,
                onClick = { onSelect(range) },
                label = { Text(range.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun WorkoutAdherenceCard(
    workoutsThisWeek: Int,
    targetSessionsPerWeek: Int,
    adherence: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly Adherence",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$workoutsThisWeek/$targetSessionsPerWeek",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { adherence },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when {
                    adherence >= 0.8f -> Color(0xFF2E7D32)
                    adherence >= 0.5f -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.error
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${(adherence * 100).toInt()}% of goal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun StrengthLineChart(
    data: List<ProgressPoint>,
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

        val values = data.map { it.value }
        val minVal = values.min()
        val maxVal = values.max()
        val range = (maxVal - minVal).coerceAtLeast(1.0)

        for (i in 0..3) {
            val y = chartHeight - (chartHeight * i / 3f)
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        val path = Path()
        val stepX = chartWidth / (data.size - 1).toFloat()

        data.forEachIndexed { index, point ->
            val x = paddingLeft + index * stepX
            val y = chartHeight - ((point.value - minVal) / range * chartHeight).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        data.forEachIndexed { index, point ->
            val x = paddingLeft + index * stepX
            val y = chartHeight - ((point.value - minVal) / range * chartHeight).toFloat()
            drawCircle(color = lineColor, radius = 4f, center = Offset(x, y))
        }
    }
}

@Composable
private fun ExerciseSelector(
    selectedExercise: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedExercise,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "\u25bc",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listOf(
                "Bench Press", "Squat", "Deadlift", "Overhead Press",
                "Barbell Row", "Pull-Up", "Dumbbell Curl", "Tricep Pushdown"
            ).forEach { exercise ->
                DropdownMenuItem(
                    text = { Text(exercise) },
                    onClick = {
                        onSelect(exercise)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MuscleVolumeBar(
    muscleName: String,
    currentSets: Int,
    targetMin: Int,
    targetMax: Int
) {
    val progress = (currentSets.toFloat() / targetMax.toFloat()).coerceIn(0f, 1f)
    val inRange = currentSets in targetMin..targetMax
    val isOver = currentSets > targetMax

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = muscleName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$currentSets sets (target: $targetMin-$targetMax)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = when {
                    inRange -> Color(0xFF2E7D32)
                    isOver -> Color(0xFFF57F17)
                    else -> MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun PRCard(
    exerciseName: String,
    achievement: String,
    date: LocalDate
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "PR",
                tint = Color(0xFFF57F17),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = achievement,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }
            Text(
                text = date.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f)
            )
        }
    }
}

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

        for (i in 0..3) {
            val y = chartHeight - (chartHeight * i / 3f)
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }

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
