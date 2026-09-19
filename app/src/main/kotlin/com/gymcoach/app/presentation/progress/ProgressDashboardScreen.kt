package com.gymcoach.app.presentation.progress

import com.gymcoach.app.presentation.progress.components.TrainingInsightsCard

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
import androidx.compose.material3.TextButton
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
import com.gymcoach.app.presentation.progress.components.MuscleDistributionPieChart
import com.gymcoach.app.presentation.progress.components.BodyMeasurementTrend
import com.gymcoach.app.presentation.progress.components.MeasurementLogDialog
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import com.gymcoach.app.ui.theme.GymCoachBorders
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.*
import androidx.compose.ui.unit.sp
import com.gymcoach.app.ui.theme.GymCoachShapes
import com.gymcoach.app.ui.theme.GymCoachSpacing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressDashboardScreen(
    onBackClick: () -> Unit,
    onNavigateToProgressionAnalytics: (Long, String) -> Unit = { _, _ -> },
    onNavigateToMuscleBalance: () -> Unit = {},
    onNavigateToStreaks: () -> Unit = {},
    onNavigateToVTaper: () -> Unit = {},
    onNavigateBottomBar: (String) -> Unit = {},
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Measurement dialog
    if (state.showMeasurementDialog) {
        MeasurementLogDialog(
            latestWeight = state.latestWeight,
            latestWaist = state.latestWaist,
            latestShoulders = state.latestShoulders,
            latestChest = state.latestChest,
            latestBodyFat = state.latestBodyFat,
            onDismiss = { viewModel.hideMeasurementDialog() },
            onSave = { weight, waist, shoulders, chest, bodyFat, notes ->
                viewModel.saveMeasurement(weight, waist, shoulders, chest, bodyFat, notes)
            }
        )
    }

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            com.gymcoach.app.ui.GymCoachBottomNav(currentRoute = "progress", onNavigate = onNavigateBottomBar)
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Progress & Analytics",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showMeasurementDialog() },
                containerColor = AccentBlue,
                contentColor = androidx.compose.ui.graphics.Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Log Measurement",
                    tint = androidx.compose.ui.graphics.Color.White
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

                    // Date Range Selector
                    DateRangeSelector(
                        selected = state.dateRange,
                        onSelect = { viewModel.selectDateRange(it) }
                    )

                    Spacer(Modifier.height(16.dp))

                    // Advanced Analytics & Telemetry Hub
                    AdvancedAnalyticsHub(
                        onNavigateToVTaper = onNavigateToVTaper,
                        onNavigateToMuscleBalance = onNavigateToMuscleBalance,
                        onNavigateToStreaks = onNavigateToStreaks
                    )

                    Spacer(Modifier.height(16.dp))

                    // Muscle Group Distribution
                    MuscleDistributionPieChart(stats = state.muscleGroupDistribution)

                    Spacer(Modifier.height(16.dp))

                    // Weekly Adherence
                    WorkoutAdherenceCard(
                        workoutsThisWeek = state.workoutsThisWeek,
                        targetSessionsPerWeek = state.targetSessionsPerWeek,
                        adherence = state.adherence
                    )

                    if (state.insights.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        TrainingInsightsCard(insights = state.insights)
                    }

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
                            currentValue = state.latestWeight ?: 0.0,
                            unit = "kg",
                            trend = state.bodyweightDirection,
                            dataPoints = state.bodyweightTrend,
                            modifier = Modifier.weight(1f),
                            goodWhenDown = false
                        )
                        BodyMeasurementTrend(
                            label = "Waist",
                            currentValue = state.latestWaist ?: 0.0,
                            unit = "cm",
                            trend = state.waistDirection,
                            dataPoints = state.waistTrend,
                            modifier = Modifier.weight(1f),
                            goodWhenDown = true
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BodyMeasurementTrend(
                            label = "Shoulders",
                            currentValue = state.latestShoulders ?: 0.0,
                            unit = "cm",
                            trend = state.shouldersDirection,
                            dataPoints = state.shouldersTrend,
                            modifier = Modifier.weight(1f),
                            goodWhenDown = false
                        )
                        BodyMeasurementTrend(
                            label = "Shoulder/Waist Ratio",
                            currentValue = state.shoulderToWaistTrend.lastOrNull()?.value ?: 0.0,
                            unit = "ratio",
                            trend = state.ratioDirection,
                            dataPoints = state.shoulderToWaistTrend,
                            modifier = Modifier.weight(1f),
                            goodWhenDown = false
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
                    val animAvgDuration by animateIntAsState(
                        targetValue = state.averageWorkoutDurationMinutes.toInt(),
                        animationSpec = tween(800, easing = FastOutSlowInEasing),
                        label = "avg_duration_anim"
                    )
                    val animAvgVolume by animateFloatAsState(
                        targetValue = state.averageWorkoutVolume.toFloat(),
                        animationSpec = tween(850, easing = FastOutSlowInEasing),
                        label = "avg_volume_anim"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            label = "Avg Duration",
                            value = "${animAvgDuration}m",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Avg Volume",
                            value = "%.1f kg".format(Locale.getDefault(), animAvgVolume),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Workout Frequency & Weekly Trend
                    val animWorkoutFreq by animateIntAsState(
                        targetValue = state.workoutFrequency,
                        animationSpec = tween(800, easing = FastOutSlowInEasing),
                        label = "workout_freq_anim"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            label = "Weekly Workouts",
                            value = "$animWorkoutFreq",
                            modifier = Modifier.weight(1f)
                        )
                        val trendSymbol = when {
                            state.weeklyTrend > 0 -> "\u25b2 +%.1f%%".format(Locale.getDefault(), state.weeklyTrend)
                            state.weeklyTrend < 0 -> "\u25bc %.1f%%".format(Locale.getDefault(), state.weeklyTrend)
                            else -> "\u2022 0.0%%"
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
                    if (state.selectedExercise != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExerciseSelector(
                                selectedExercise = state.selectedExercise!!,
                                onSelect = { viewModel.selectExercise(it) }
                            )
                            TextButton(
                                onClick = {
                                    state.selectedExercise?.let { exName ->
                                        onNavigateToProgressionAnalytics(0L, exName)
                                    }
                                }
                            ) {
                                Text("Detailed Analytics \u2192")
                            }
                        }
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
    val animatedAdherence by animateFloatAsState(
        targetValue = adherence.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "adherence_bar_anim"
    )
    val animatedPercent by animateIntAsState(
        targetValue = (adherence * 100).toInt(),
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "adherence_pct_anim"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = GymCoachShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = GymCoachBorders.subtleBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WEEKLY ADHERENCE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "$workoutsThisWeek of $targetSessionsPerWeek sessions completed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            if (adherence >= 0.8f) Color(0xFF10B981).copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$animatedPercent%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = if (adherence >= 0.8f) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { animatedAdherence },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when {
                    adherence >= 0.8f -> Color(0xFF10B981)
                    adherence >= 0.5f -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.error
                },
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        }
    }
}

@Composable
private fun StrengthLineChart(
    data: List<ProgressPoint>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = GymCoachShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = GymCoachBorders.subtleBorder()
    ) {
        val lineColor = MaterialTheme.colorScheme.primary
        val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

        val animProgress by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            label = "strength_chart_anim"
        )

        val (minVal, _, range) = remember(data) {
            val values = data.map { it.value }
            val min = values.minOrNull() ?: 0.0
            val max = values.maxOrNull() ?: 1.0
            Triple(min, max, (max - min).coerceAtLeast(1.0))
        }

        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (data.size < 2) return@Canvas

            val paddingLeft = 12f
            val paddingBottom = 12f
            val chartWidth = size.width - paddingLeft
            val chartHeight = size.height - paddingBottom

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
            val baselineY = chartHeight

            data.forEachIndexed { index, point ->
                val x = paddingLeft + index * stepX
                val targetY = chartHeight - ((point.value - minVal) / range * chartHeight).toFloat()
                val y = baselineY - (baselineY - targetY) * animProgress
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            // Area fill under line
            val fillPath = Path().apply {
                addPath(path)
                lineTo(paddingLeft + (data.size - 1) * stepX, chartHeight)
                lineTo(paddingLeft, chartHeight)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.22f * animProgress), Color.Transparent),
                    startY = 0f,
                    endY = chartHeight
                )
            )

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            data.forEachIndexed { index, point ->
                val x = paddingLeft + index * stepX
                val targetY = chartHeight - ((point.value - minVal) / range * chartHeight).toFloat()
                val y = baselineY - (baselineY - targetY) * animProgress
                drawCircle(color = lineColor, radius = 5f * animProgress, center = Offset(x, y))
                drawCircle(color = Color.White, radius = 2.5f * animProgress, center = Offset(x, y))
            }
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
                .clickable { expanded = true },
            shape = GymCoachShapes.sm,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            border = GymCoachBorders.subtleBorder()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedExercise,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "\u25bc",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
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
    val targetProgress = (currentSets.toFloat() / targetMax.toFloat()).coerceIn(0f, 1f)
    val inRange = currentSets in targetMin..targetMax
    val isOver = currentSets > targetMax

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "muscle_bar_anim"
    )
    val animatedSets by animateIntAsState(
        targetValue = currentSets,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "muscle_sets_anim"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = GymCoachShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = GymCoachBorders.subtleBorder()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = muscleName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "$animatedSets / $targetMax sets",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                when {
                                    inRange -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    isOver -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = when {
                                inRange -> "OPTIMAL"
                                isOver -> "OVER"
                                else -> "LOW"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            color = when {
                                inRange -> Color(0xFF10B981)
                                isOver -> Color(0xFFF59E0B)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = when {
                    inRange -> Color(0xFF10B981)
                    isOver -> Color(0xFFF59E0B)
                    else -> MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
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
    val scaleAnim = remember { Animatable(0.2f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(exerciseName, achievement) {
        launch {
            alphaAnim.animateTo(1f, animationSpec = tween(350))
        }
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = GymCoachShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                        alpha = alphaAnim.value
                    }
                    .size(42.dp)
                    .background(Color(0xFFFFD54F).copy(alpha = 0.2f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "PR",
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scaleAnim.value
                                scaleY = scaleAnim.value
                            }
                            .background(Color(0xFFFFD54F).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PR",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            color = Color(0xFFFFD54F)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = achievement,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = date.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp
    )
}

@Composable
private fun EmptyPlaceholder(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = GymCoachShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = GymCoachBorders.subtleBorder()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = GymCoachShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = GymCoachBorders.subtleBorder()
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
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
    Card(
        modifier = modifier,
        shape = GymCoachShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = GymCoachBorders.subtleBorder()
    ) {
        val lineColor = MaterialTheme.colorScheme.primary
        val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

        val animProgress by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            label = "volume_chart_anim"
        )

        val (minVal, _, range) = remember(data) {
            val values = data.map { it.second }
            val min = values.minOrNull() ?: 0.0
            val max = values.maxOrNull() ?: 1.0
            Triple(min, max, (max - min).coerceAtLeast(1.0))
        }

        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (data.size < 2) return@Canvas

            val paddingLeft = 12f
            val paddingBottom = 12f
            val chartWidth = size.width - paddingLeft
            val chartHeight = size.height - paddingBottom

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
            val baselineY = chartHeight

            data.forEachIndexed { index, point ->
                val x = paddingLeft + index * stepX
                val targetY = chartHeight - ((point.second - minVal) / range * chartHeight).toFloat()
                val y = baselineY - (baselineY - targetY) * animProgress
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            // Area gradient fill under line
            val fillPath = Path().apply {
                addPath(path)
                lineTo(paddingLeft + (data.size - 1) * stepX, chartHeight)
                lineTo(paddingLeft, chartHeight)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.22f * animProgress), Color.Transparent),
                    startY = 0f,
                    endY = chartHeight
                )
            )

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            data.forEachIndexed { index, point ->
                val x = paddingLeft + index * stepX
                val targetY = chartHeight - ((point.second - minVal) / range * chartHeight).toFloat()
                val y = baselineY - (baselineY - targetY) * animProgress
                drawCircle(color = lineColor, radius = 5f * animProgress, center = Offset(x, y))
                drawCircle(color = Color.White, radius = 2.5f * animProgress, center = Offset(x, y))
            }
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
    val animWorkouts by animateIntAsState(targetValue = totalWorkouts, animationSpec = tween(800, easing = FastOutSlowInEasing), label = "stats_total_workouts")
    val animToday by animateIntAsState(targetValue = todayWorkouts, animationSpec = tween(800, easing = FastOutSlowInEasing), label = "stats_today_workouts")
    val animWeek by animateIntAsState(targetValue = weekWorkouts, animationSpec = tween(800, easing = FastOutSlowInEasing), label = "stats_week_workouts")
    val animMonth by animateIntAsState(targetValue = monthWorkouts, animationSpec = tween(800, easing = FastOutSlowInEasing), label = "stats_month_workouts")
    val animExercises by animateIntAsState(targetValue = totalExercises, animationSpec = tween(800, easing = FastOutSlowInEasing), label = "stats_total_exercises")
    val animSets by animateIntAsState(targetValue = totalSets, animationSpec = tween(800, easing = FastOutSlowInEasing), label = "stats_total_sets")
    val animReps by animateIntAsState(targetValue = totalReps, animationSpec = tween(800, easing = FastOutSlowInEasing), label = "stats_total_reps")
    val animVolume by animateFloatAsState(targetValue = totalVolume.toFloat(), animationSpec = tween(950, easing = FastOutSlowInEasing), label = "stats_total_volume")
    val animTime by animateIntAsState(targetValue = totalTrainingTimeMinutes.toInt(), animationSpec = tween(800, easing = FastOutSlowInEasing), label = "stats_training_time")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(label = "Workouts", value = "$animWorkouts", modifier = Modifier.weight(1f))
            StatCard(label = "Today", value = "$animToday", modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(label = "Week", value = "$animWeek", modifier = Modifier.weight(1f))
            StatCard(label = "Month", value = "$animMonth", modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(label = "Exercises", value = "$animExercises", modifier = Modifier.weight(1f))
            StatCard(label = "Sets", value = "$animSets", modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(label = "Reps", value = "$animReps", modifier = Modifier.weight(1f))
            StatCard(label = "Volume", value = "%.1f kg".format(Locale.getDefault(), animVolume), modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = "Time",
                value = "${animTime}m",
                modifier = Modifier.weight(1f)
            )
            val avgSessionMinutes = if (animWorkouts > 0) animTime / animWorkouts else 0
            StatCard(
                label = "Avg. Duration",
                value = "${avgSessionMinutes}m",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = GymCoachShapes.sm,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = GymCoachBorders.subtleBorder()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AdvancedAnalyticsHub(
    onNavigateToVTaper: () -> Unit,
    onNavigateToMuscleBalance: () -> Unit,
    onNavigateToStreaks: () -> Unit
) {
    SectionHeader("Advanced Telemetry & Analytics")
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnalyticsFeatureCard(
            title = "V-Taper",
            subtitle = "Adonis Telemetry",
            icon = Icons.Default.LocalFireDepartment,
            accentColor = Color(0xFF6C63FF),
            onClick = onNavigateToVTaper,
            modifier = Modifier.weight(1f)
        )
        AnalyticsFeatureCard(
            title = "Balance",
            subtitle = "Antagonist Ratios",
            icon = Icons.Default.FitnessCenter,
            accentColor = Color(0xFF00F2FE),
            onClick = onNavigateToMuscleBalance,
            modifier = Modifier.weight(1f)
        )
        AnalyticsFeatureCard(
            title = "Streaks",
            subtitle = "Consistency Badges",
            icon = Icons.Default.EmojiEvents,
            accentColor = Color(0xFFFFB300),
            onClick = onNavigateToStreaks,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AnalyticsFeatureCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = GymCoachShapes.md,
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        border = GymCoachBorders.subtleBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                maxLines = 1
            )
        }
    }
}

