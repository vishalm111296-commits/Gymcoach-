package com.gymcoach.app.presentation.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcoach.app.ui.theme.*

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(GymCoachColors.PureDark),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.lg)
            ) {
                // Summary stats row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(GymCoachSpacing.md)
                    ) {
                        StatCard(
                            label = "PEAK E1RM",
                            value = "%.1f kg".format(state.peakE1RM),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "RECENT E1RM",
                            value = "%.1f kg".format(state.recentE1RM),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "SESSIONS",
                            value = "${state.totalSessions}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // E1RM Trend section
                if (state.e1rmTrend.isNotEmpty()) {
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
                    items(state.prHistory) { pr ->
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
        }
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

@Composable
private fun E1RMChart(dataPoints: List<E1RMDataPoint>) {
    // Simple bar chart representation using Canvas/BoxWithConstraints
    val maxE1RM = dataPoints.maxOfOrNull { it.e1rm }?.takeIf { it > 0 } ?: 1.0
    val formatter = DateTimeFormatter.ofPattern("M/d", Locale.getDefault())
    val zone = ZoneId.systemDefault()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = GymCoachShapes.lg,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(GymCoachSpacing.lg)) {
            // Show last 10 sessions as bar segments
            val displayed = dataPoints.takeLast(10)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                displayed.forEach { point ->
                    val fraction = (point.e1rm / maxE1RM).toFloat().coerceIn(0.05f, 1f)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
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
            Text(
                text = "Best e1RM: %.1f kg (%.0f kg × %d reps)".format(
                    dataPoints.lastOrNull()?.e1rm ?: 0.0,
                    dataPoints.lastOrNull()?.weight ?: 0.0,
                    dataPoints.lastOrNull()?.reps ?: 0
                ),
                style = MaterialTheme.typography.bodySmall,
                color = GymCoachColors.TextSecondary
            )
        }
    }
}

@Composable
private fun PREntryCard(pr: PREntry) {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    val zone = ZoneId.systemDefault()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = GymCoachShapes.md,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCardElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GymCoachSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "%.1f kg e1RM".format(pr.e1rm),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "%.0f kg × %d reps".format(pr.weight, pr.reps),
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

@Composable
private fun WeeklyVolumeChart(entries: List<WeeklyVolumeEntry>) {
    val maxVol = entries.maxOfOrNull { it.volumeKg }?.takeIf { it > 0 } ?: 1.0
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
                    val fraction = (entry.volumeKg / maxVol).toFloat().coerceIn(0.05f, 1f)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(Color(0xFF4CAF50))
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Total volume last week: %.0f kg".format(entries.lastOrNull()?.volumeKg ?: 0.0),
                style = MaterialTheme.typography.bodySmall,
                color = GymCoachColors.TextSecondary
            )
        }
    }
}
