package com.gymcoach.app.presentation.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.presentation.components.PremiumEmptyState
import com.gymcoach.app.presentation.progress.components.MeasurementLogDialog
import com.gymcoach.app.ui.theme.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import com.gymcoach.app.presentation.progress.components.BodyMeasurementTrend
import com.gymcoach.app.presentation.progress.components.MuscleVolumeChart
import com.gymcoach.app.presentation.progress.components.StrengthProgressChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressDashboardScreen(
    onBackClick: () -> Unit,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

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
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("PROGRESS", fontWeight = FontWeight.Black, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showMeasurementDialog() },
                containerColor = AccentBlue,
                contentColor = TextPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log Measurement")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            // Time Range Selector
            PremiumDateRangeSelector(
                selected = state.dateRange,
                onSelect = { viewModel.selectDateRange(it) }
            )

            Spacer(Modifier.height(32.dp))

            // TRAINING OVERVIEW
            PremiumSectionHeader("TRAINING OVERVIEW")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PremiumStatCard(title = "WORKOUTS", value = "${state.totalWorkouts}", subtitle = "Selected Period", modifier = Modifier.weight(1f))
                PremiumStatCard(title = "VOLUME", value = "%.0f kg".format(state.totalVolume), subtitle = "Total Lifted", modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(32.dp))

            // STRENGTH PROGRESSION
            PremiumSectionHeader("STRENGTH PROGRESSION")
            if (state.personalRecords.isEmpty()) {
                PremiumEmptyState("NO PRs", "Keep logging workouts to see your strength records.")
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        state.recentPRs.take(3).forEach { pr ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = pr.exerciseName.uppercase(), style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                                    Text(text = formatDate(pr.date), style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                }
                                Text(text = pr.achievement, style = MaterialTheme.typography.bodyMedium, color = PRHighlight, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // BODY MEASUREMENTS
            PremiumSectionHeader("BODY MEASUREMENTS")
            if (state.bodyweightTrend.isEmpty() && state.latestWeight == null) {
                PremiumEmptyState("NO MEASUREMENTS", "Log your bodyweight and measurements to see trends.")
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PremiumStatCard(title = "CURRENT WEIGHT", value = state.latestWeight?.let { "%.1f kg".format(it) } ?: "--", subtitle = "Latest entry", modifier = Modifier.weight(1f))
                    PremiumStatCard(title = "CURRENT WAIST", value = state.latestWaist?.takeIf { it > 0 }?.let { "%.1f cm".format(it) } ?: "--", subtitle = "Latest entry", modifier = Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(32.dp))

            // V-TAPER SUMMARY
            PremiumSectionHeader("V-TAPER SNAPSHOT")
            if (state.muscleVolume.isEmpty()) {
                PremiumEmptyState("NO DATA", "Complete workouts to generate your V-Taper muscle volume snapshot.")
            } else {
                // Focus on Lats, Lateral Deltoids as per existing data structure
                val backVolume = state.muscleVolume.find { it.muscleName.equals("Back", ignoreCase = true) }?.currentSets ?: 0
                val deltVolume = state.muscleVolume.find { it.muscleName.equals("Lateral Deltoid", ignoreCase = true) }?.currentSets ?: 0
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PremiumStatCard(title = "BACK SETS", value = "$backVolume", subtitle = "This Week", modifier = Modifier.weight(1f))
                    PremiumStatCard(title = "DELT SETS", value = "$deltVolume", subtitle = "This Week", modifier = Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun PremiumSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = TextTertiary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun PremiumStatCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = AccentBlue, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        }
    }
}

@Composable
private fun PremiumDateRangeSelector(selected: ProgressDateRange, onSelect: (ProgressDateRange) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(DarkSurfaceVariant, RoundedCornerShape(16.dp)).padding(4.dp)) {
        val ranges = listOf(
            ProgressDateRange.FOUR_WEEKS to "4W",
            ProgressDateRange.EIGHT_WEEKS to "8W",
            ProgressDateRange.TWELVE_WEEKS to "12W",

        )
        ranges.forEach { (range, label) ->
            val isSelected = selected == range
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) AccentBlue else Color.Transparent)
                    .clickable { onSelect(range) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) TextPrimary else TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatDate(date: LocalDate): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")
    return date.format(formatter)
}

// private fun formatDate(dateMs: Long): String {
