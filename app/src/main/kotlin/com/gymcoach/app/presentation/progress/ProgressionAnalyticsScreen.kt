package com.gymcoach.app.presentation.progress

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import com.gymcoach.app.core.progression.OneRepMaxCalculator
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcoach.app.ui.theme.*
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class AnalyticsTab(val title: String) {
    OVERVIEW("Overview"),
    E1RM("1RM Trend"),
    VOLUME("Volume"),
    RECORDS("PRs")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressionAnalyticsScreen(
    exerciseId: Long,
    exerciseName: String,
    onBackClick: () -> Unit,
    viewModel: ProgressionAnalyticsViewModel = hiltViewModel()
) {
    LaunchedEffect(exerciseId) {
        viewModel.loadExercise(exerciseId, exerciseName)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(AnalyticsTab.OVERVIEW) }

    Scaffold(
        containerColor = GymCoachColors.PureDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.exerciseName.ifBlank { exerciseName },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GymCoachColors.PureDark)
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GymCoachColors.Primary)
            }

            state.isEmpty -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No workout data yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = GymCoachColors.TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Complete some sets to see your progression",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GymCoachColors.TextSecondary.copy(alpha = 0.7f)
                    )
                }
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(GymCoachColors.PureDark)
            ) {
                // Animated Tab Navigation
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = GymCoachColors.PureDark,
                    contentColor = GymCoachColors.Primary,
                    indicator = { tabPositions ->
                        if (selectedTab.ordinal < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                                color = GymCoachColors.Primary,
                                height = 3.dp
                            )
                        }
                    },
                    divider = {
                        HorizontalDivider(color = GymCoachColors.BorderSubtle)
                    }
                ) {
                    AnalyticsTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == tab) GymCoachColors.Primary else GymCoachColors.TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                }

                // Crossfade between analytics tabs
                Crossfade(
                    targetState = selectedTab,
                    animationSpec = tween(durationMillis = 300),
                    label = "analytics_tab_crossfade"
                ) { currentTab ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(GymCoachColors.PureDark),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.lg)
                    ) {
                        when (currentTab) {
                            AnalyticsTab.OVERVIEW -> {
                                // Summary stats with animated counters
                                item {
                                    SummaryStatsRow(
                                        peakE1RM = state.peakE1RM,
                                        recentE1RM = state.recentE1RM,
                                        totalSessions = state.totalSessions
                                    )
                                }

                                // 1RM Gauge Fill
                                if (state.e1rmTrend.isNotEmpty()) {
                                    item {
                                        E1RMGaugeCard(
                                            recentE1RM = state.recentE1RM,
                                            peakE1RM = state.peakE1RM
                                        )
                                    }
                                    item {
                                        SectionHeader("ESTIMATED 1RM TREND")
                                    }
                                    item {
                                        E1RMChart(dataPoints = state.e1rmTrend)
                                    }
                                }

                                // PR History section
                                if (state.prHistory.isNotEmpty()) {
                                    item {
                                        SectionHeader("PERSONAL RECORDS")
                                    }
                                    items(state.prHistory.take(5)) { pr ->
                                        PREntryCard(pr)
                                    }
                                }

                                // Weekly Volume section
                                if (state.weeklyVolume.isNotEmpty()) {
                                    item {
                                        SectionHeader("WEEKLY VOLUME (last 12 weeks)")
                                    }
                                    item {
                                        WeeklyVolumeChart(entries = state.weeklyVolume)
                                    }
                                }

                                item { Spacer(Modifier.height(32.dp)) }
                            }

                            AnalyticsTab.E1RM -> {
                                item {
                                    SummaryStatsRow(
                                        peakE1RM = state.peakE1RM,
                                        recentE1RM = state.recentE1RM,
                                        totalSessions = state.totalSessions
                                    )
                                }
                                item {
                                    E1RMGaugeCard(
                                        recentE1RM = state.recentE1RM,
                                        peakE1RM = state.peakE1RM
                                    )
                                }
                                if (state.e1rmTrend.isNotEmpty()) {
                                    item {
                                        SectionHeader("ESTIMATED 1RM TREND")
                                    }
                                    item {
                                        E1RMChart(dataPoints = state.e1rmTrend)
                                    }
                                }
                                if (state.oneRepMaxProfile != null) {
                                    item {
                                        SectionHeader("1RM SCIENTIFIC FORMULA COMPARISON")
                                    }
                                    item {
                                        OneRepMaxFormulaComparisonCard(profile = state.oneRepMaxProfile!!)
                                    }
                                    item {
                                        SectionHeader("ESTIMATED TRAINING INTENSITY ZONES")
                                    }
                                    item {
                                        TrainingIntensityZonesCard(zones = state.oneRepMaxProfile!!.zones)
                                    }
                                }
                                item { Spacer(Modifier.height(32.dp)) }
                            }

                            AnalyticsTab.VOLUME -> {
                                if (state.weeklyVolume.isNotEmpty()) {
                                    item {
                                        VolumeSummaryRow(entries = state.weeklyVolume)
                                    }
                                    item {
                                        SectionHeader("WEEKLY VOLUME (last 12 weeks)")
                                    }
                                    item {
                                        WeeklyVolumeChart(entries = state.weeklyVolume)
                                    }
                                } else {
                                    item {
                                        Text(
                                            text = "No volume data recorded yet",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = GymCoachColors.TextSecondary
                                        )
                                    }
                                }
                                item { Spacer(Modifier.height(32.dp)) }
                            }

                            AnalyticsTab.RECORDS -> {
                                if (state.prHistory.isNotEmpty()) {
                                    item {
                                        PRStatsHeader(
                                            totalPRs = state.prHistory.size,
                                            peakE1RM = state.peakE1RM
                                        )
                                    }
                                    item {
                                        SectionHeader("ALL PERSONAL RECORDS (${state.prHistory.size})")
                                    }
                                    items(state.prHistory) { pr ->
                                        PREntryCard(pr)
                                    }
                                } else {
                                    item {
                                        Text(
                                            text = "No personal records recorded yet",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = GymCoachColors.TextSecondary
                                        )
                                    }
                                }
                                item { Spacer(Modifier.height(32.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatsRow(
    peakE1RM: Double,
    recentE1RM: Double,
    totalSessions: Int
) {
    val animatedPeak by animateFloatAsState(
        targetValue = peakE1RM.toFloat(),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "summary_peak_anim"
    )
    val animatedRecent by animateFloatAsState(
        targetValue = recentE1RM.toFloat(),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "summary_recent_anim"
    )
    val animatedSessions by animateIntAsState(
        targetValue = totalSessions,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "summary_sessions_anim"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GymCoachSpacing.md)
    ) {
        StatCard(
            label = "PEAK E1RM",
            value = "%.1f kg".format(Locale.getDefault(), animatedPeak),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "RECENT E1RM",
            value = "%.1f kg".format(Locale.getDefault(), animatedRecent),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "SESSIONS",
            value = "$animatedSessions",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun VolumeSummaryRow(entries: List<WeeklyVolumeEntry>) {
    val totalVol = entries.sumOf { it.volumeKg }
    val avgVol = if (entries.isNotEmpty()) totalVol / entries.size else 0.0
    val maxVol = entries.maxOfOrNull { it.volumeKg } ?: 0.0

    val animTotal by animateFloatAsState(
        targetValue = totalVol.toFloat(),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "vol_summary_total"
    )
    val animAvg by animateFloatAsState(
        targetValue = avgVol.toFloat(),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "vol_summary_avg"
    )
    val animMax by animateFloatAsState(
        targetValue = maxVol.toFloat(),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "vol_summary_max"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GymCoachSpacing.md)
    ) {
        StatCard(
            label = "TOTAL VOLUME",
            value = "%.0f kg".format(Locale.getDefault(), animTotal),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "AVG / WEEK",
            value = "%.0f kg".format(Locale.getDefault(), animAvg),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "PEAK WEEK",
            value = "%.0f kg".format(Locale.getDefault(), animMax),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PRStatsHeader(totalPRs: Int, peakE1RM: Double) {
    val animPRs by animateIntAsState(
        targetValue = totalPRs,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "pr_stats_count"
    )
    val animPeak by animateFloatAsState(
        targetValue = peakE1RM.toFloat(),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "pr_stats_peak"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GymCoachSpacing.md)
    ) {
        StatCard(
            label = "TOTAL RECORDS",
            value = "$animPRs",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "ALL-TIME BEST",
            value = "%.1f kg".format(Locale.getDefault(), animPeak),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = GymCoachShapes.lg,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard)
    ) {
        Column(
            modifier = Modifier.padding(GymCoachSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = GymCoachColors.Primary,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = GymCoachColors.Primary,
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.Bold
    )
}

/**
 * Smooth gauge fill card for 1RM estimations.
 * Renders an animated circular arc gauge displaying current estimated 1RM vs peak 1RM.
 */
@Composable
private fun E1RMGaugeCard(
    recentE1RM: Double,
    peakE1RM: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = GymCoachShapes.lg,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard)
    ) {
        Column(
            modifier = Modifier.padding(GymCoachSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ESTIMATED 1RM GAUGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = GymCoachColors.Primary,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
                val isNewPeak = recentE1RM >= peakE1RM && peakE1RM > 0.0
                if (isNewPeak) {
                    Box(
                        modifier = Modifier
                            .background(
                                Color(0xFFFFD700).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AT PEAK",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            color = Color(0xFFFFD700)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val ratio = if (peakE1RM > 0) (recentE1RM / peakE1RM).toFloat().coerceIn(0f, 1f) else 0f
            val animatedRatio by animateFloatAsState(
                targetValue = ratio,
                animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
                label = "gauge_ratio_anim"
            )
            val animatedRecent by animateFloatAsState(
                targetValue = recentE1RM.toFloat(),
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                label = "gauge_recent_anim"
            )

            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = GymCoachColors.Primary
                val trackColor = GymCoachColors.SurfaceCardElevated
                val goldColor = Color(0xFFFFD700)
                val gaugeColor = if (ratio >= 0.999f) goldColor else primaryColor

                Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    val strokeWidth = 14f
                    val startAngle = 140f
                    val sweepMaxAngle = 260f

                    // Background track
                    drawArc(
                        color = trackColor,
                        startAngle = startAngle,
                        sweepAngle = sweepMaxAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Smooth animated gauge fill
                    if (animatedRatio > 0f) {
                        drawArc(
                            color = gaugeColor,
                            startAngle = startAngle,
                            sweepAngle = sweepMaxAngle * animatedRatio,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "%.1f".format(Locale.getDefault(), animatedRecent),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "kg e1RM",
                        style = MaterialTheme.typography.labelSmall,
                        color = GymCoachColors.TextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${(animatedRatio * 100).toInt()}% of peak",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = gaugeColor
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Current: %.1f kg".format(Locale.getDefault(), recentE1RM),
                    style = MaterialTheme.typography.bodySmall,
                    color = GymCoachColors.TextSecondary
                )
                Text(
                    text = "Peak: %.1f kg".format(Locale.getDefault(), peakE1RM),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Animated bar chart for estimated 1RM sessions, with bars revealing and growing from 0.
 */
@Composable
private fun E1RMChart(dataPoints: List<E1RMDataPoint>) {
    val maxE1RM = dataPoints.maxOfOrNull { it.e1rm }?.takeIf { it > 0 } ?: 1.0
    val formatter = DateTimeFormatter.ofPattern("M/d", Locale.getDefault())
    val zone = ZoneId.systemDefault()

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "e1rm_chart_bars_reveal"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = GymCoachShapes.lg,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(GymCoachSpacing.lg)) {
            val displayed = dataPoints.takeLast(10)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                displayed.forEach { point ->
                    val rawFraction = (point.e1rm / maxE1RM).toFloat().coerceIn(0.05f, 1f)
                    val animatedFraction = (rawFraction * animProgress).coerceAtLeast(0.02f)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(animatedFraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(GymCoachColors.Primary)
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                dataPoints.takeLast(10).forEach { point ->
                    Text(
                        text = formatter.format(point.date.atZone(zone)),
                        style = MaterialTheme.typography.labelSmall,
                        color = GymCoachColors.TextSecondary,
                        fontSize = 8.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            val lastPoint = dataPoints.lastOrNull()
            val animatedBestE1RM by animateFloatAsState(
                targetValue = (lastPoint?.e1rm ?: 0.0).toFloat(),
                animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                label = "e1rm_best_anim"
            )
            Text(
                text = "Best e1RM: %.1f kg (%.0f kg × %d reps)".format(
                    Locale.getDefault(),
                    animatedBestE1RM,
                    lastPoint?.weight ?: 0.0,
                    lastPoint?.reps ?: 0
                ),
                style = MaterialTheme.typography.bodySmall,
                color = GymCoachColors.TextSecondary
            )
        }
    }
}

/**
 * Milestone PR entry card featuring celebratory spring scale and fade badge pop.
 */
@Composable
private fun PREntryCard(pr: PREntry) {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    val zone = ZoneId.systemDefault()

    val scaleAnim = remember { Animatable(0.2f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(pr) {
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
        modifier = Modifier.fillMaxWidth(),
        shape = GymCoachShapes.md,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCardElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GymCoachSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                        alpha = alphaAnim.value
                    }
                    .size(40.dp)
                    .background(
                        Color(0xFFFFD700).copy(alpha = 0.18f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = "Milestone PR",
                    tint = Color(0xFFFFD700),
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
                        text = "%.1f kg e1RM".format(Locale.getDefault(), pr.e1rm),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scaleAnim.value
                                scaleY = scaleAnim.value
                            }
                            .background(Color(0xFFFFD700).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PR",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            color = Color(0xFFFFD700)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "%.0f kg × %d reps".format(Locale.getDefault(), pr.weight, pr.reps),
                    style = MaterialTheme.typography.bodySmall,
                    color = GymCoachColors.TextSecondary
                )
            }
            Text(
                text = formatter.format(pr.date.atZone(zone)),
                style = MaterialTheme.typography.labelSmall,
                color = GymCoachColors.Primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Animated weekly volume chart with bars revealing from bottom to top and animated volume counter.
 */
@Composable
private fun WeeklyVolumeChart(entries: List<WeeklyVolumeEntry>) {
    val maxVol = entries.maxOfOrNull { it.volumeKg }?.takeIf { it > 0 } ?: 1.0

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "weekly_vol_bars_reveal"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = GymCoachShapes.lg,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(GymCoachSpacing.lg)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                entries.forEach { entry ->
                    val rawFraction = (entry.volumeKg / maxVol).toFloat().coerceIn(0.05f, 1f)
                    val animatedFraction = (rawFraction * animProgress).coerceAtLeast(0.02f)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(animatedFraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(Color(0xFF4CAF50))
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            val lastVol = entries.lastOrNull()?.volumeKg ?: 0.0
            val animLastVol by animateFloatAsState(
                targetValue = lastVol.toFloat(),
                animationSpec = tween(900, easing = FastOutSlowInEasing),
                label = "weekly_vol_last_anim"
            )
            Text(
                text = "Total volume last week: %.0f kg".format(Locale.getDefault(), animLastVol),
                style = MaterialTheme.typography.bodySmall,
                color = GymCoachColors.TextSecondary
            )
        }
    }
}

/**
 * Scientific 1RM formula comparison card displaying Epley, Brzycki, Lombardi, Mayhew, and Wathen values.
 */
@Composable
private fun OneRepMaxFormulaComparisonCard(profile: OneRepMaxCalculator.OneRepMaxProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = GymCoachShapes.lg,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
        border = GymCoachBorders.subtleBorder()
    ) {
        Column(modifier = Modifier.padding(GymCoachSpacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ESTIMATED 1RM COMPARISON",
                    style = MaterialTheme.typography.labelSmall,
                    color = GymCoachColors.Primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Surface(
                    shape = GymCoachShapes.pill,
                    color = GymCoachColors.PrimaryGlow
                ) {
                    Text(
                        text = "Basis: ${profile.weight.toInt()} kg × ${profile.reps} reps",
                        style = MaterialTheme.typography.labelSmall,
                        color = GymCoachColors.PrimaryLight,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Average Highlight Box
            Surface(
                shape = GymCoachShapes.md,
                color = GymCoachColors.SurfaceCardElevated,
                border = GymCoachBorders.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "COMPOSITE CONSENSUS AVERAGE",
                            style = MaterialTheme.typography.labelSmall,
                            color = GymCoachColors.TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "5-Formula Weighted Benchmark",
                            style = MaterialTheme.typography.bodySmall,
                            color = GymCoachColors.TextSecondary
                        )
                    }
                    Text(
                        text = "%.1f kg".format(Locale.getDefault(), profile.average1RM),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = GymCoachColors.CyanAccent
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Formula Matrix Grid
            val formulas = listOf(
                Triple("Epley", profile.epley1RM, "w × (1 + r/30)"),
                Triple("Brzycki", profile.brzycki1RM, "w × (36 / (37 - r))"),
                Triple("Lombardi", profile.lombardi1RM, "w × r^0.10"),
                Triple("Mayhew", profile.mayhew1RM, "100w / (52.2 + 41.9e^-0.055r)"),
                Triple("Wathen", profile.wathen1RM, "100w / (48.8 + 53.8e^-0.075r)")
            )

            formulas.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { (name, value, formulaStr) ->
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = GymCoachShapes.sm,
                            colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceDeep),
                            border = GymCoachBorders.subtleBorder()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GymCoachColors.TextPrimary
                                    )
                                    Text(
                                        text = "%.1f kg".format(Locale.getDefault(), value),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = GymCoachColors.PrimaryLight
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = formulaStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GymCoachColors.TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

/**
 * Periodized training intensity percentage zones card.
 */
@Composable
private fun TrainingIntensityZonesCard(zones: List<OneRepMaxCalculator.TrainingZone>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = GymCoachShapes.lg,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
        border = GymCoachBorders.subtleBorder()
    ) {
        Column(
            modifier = Modifier.padding(GymCoachSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "INTENSITY PERCENTAGE TARGETS",
                style = MaterialTheme.typography.labelSmall,
                color = GymCoachColors.Primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            zones.forEach { zone ->
                val barColor = when {
                    zone.percentage >= 90 -> GymCoachColors.Danger
                    zone.percentage >= 80 -> GymCoachColors.Warning
                    zone.percentage >= 70 -> GymCoachColors.Primary
                    else -> GymCoachColors.Success
                }

                Surface(
                    shape = GymCoachShapes.sm,
                    color = GymCoachColors.SurfaceDeep,
                    border = GymCoachBorders.subtleBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = GymCoachShapes.xs,
                                color = barColor.copy(alpha = 0.2f),
                                modifier = Modifier.widthIn(min = 44.dp)
                            ) {
                                Text(
                                    text = "${zone.percentage}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = barColor,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 3.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = zone.trainingGoal,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = GymCoachColors.TextPrimary
                                )
                                Text(
                                    text = "${zone.repRange} reps",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GymCoachColors.TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Text(
                            text = "%.1f kg".format(Locale.getDefault(), zone.weight),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
