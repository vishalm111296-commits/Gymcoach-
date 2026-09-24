package com.gymcoach.app.presentation.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.ui.theme.GymCoachColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingFrequencyScreen(
    onBackClick: () -> Unit,
    viewModel: TrainingFrequencyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Training Frequency") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GymCoachColors.PureDark
                )
            )
        },
        containerColor = GymCoachColors.PureDark
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GymCoachColors.Primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "This Week",
                        value = state.workoutsThisWeek.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "This Month",
                        value = state.workoutsThisMonth.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Best Streak",
                        value = "${state.bestStreak}d",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Heatmap
                CustomCalendarHeatmap(
                    heatmapData = state.heatmapData,
                    today = LocalDate.now()
                )

                // Monthly breakdown
                MonthlyBreakdownChart(state.monthlyData)

                // Day frequency
                DayDistributionChart(state.dayFrequency)
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = GymCoachColors.Primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = GymCoachColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CustomCalendarHeatmap(
    heatmapData: Map<LocalDate, Int>,
    modifier: Modifier = Modifier,
    today: LocalDate
) {
    val HEATMAP_WEEKS = 24 // ~6 months
    val CELL_SIZE_DP = 12

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "6-MONTH CONSISTENCY",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                color = GymCoachColors.TextSecondary
            )

            Spacer(Modifier.padding(top = 14.dp))

            val startMonday = today.with(DayOfWeek.MONDAY).minusWeeks((HEATMAP_WEEKS - 1).toLong())

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { dayOfWeek ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(HEATMAP_WEEKS) { week ->
                            val date = startMonday.plusWeeks(week.toLong()).plusDays(dayOfWeek.toLong())
                            val count = heatmapData[date] ?: 0
                            
                            val color = when {
                                count == 0 -> Color(0xFF2D3748) // dark grey
                                count == 1 -> Color(0xFF6EE7B7) // light green
                                count == 2 -> Color(0xFF10B981) // medium green
                                else -> Color(0xFF047857) // bright/dark green (3+)
                            }

                            Box(
                                modifier = Modifier
                                    .size(CELL_SIZE_DP.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyBreakdownChart(monthlyData: Map<Month, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "MONTHLY BREAKDOWN",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                color = GymCoachColors.TextSecondary
            )
            Spacer(Modifier.padding(top = 14.dp))
            
            val maxCount = monthlyData.values.maxOrNull()?.coerceAtLeast(1) ?: 1

            // Display last 6 months in order
            val today = LocalDate.now()
            val months = (0..5).map { today.minusMonths(it.toLong()).month }.reversed()

            months.forEach { month ->
                val count = monthlyData[month] ?: 0
                val ratio = count.toFloat() / maxCount

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = GymCoachColors.TextSecondary,
                        modifier = Modifier.width(40.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2D3748))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(ratio)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GymCoachColors.Primary)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(24.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun DayDistributionChart(dayFrequency: Map<DayOfWeek, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "MOST ACTIVE DAY",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                color = GymCoachColors.TextSecondary
            )
            Spacer(Modifier.padding(top = 14.dp))

            val maxCount = dayFrequency.values.maxOrNull()?.coerceAtLeast(1) ?: 1

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                DayOfWeek.values().forEach { day ->
                    val count = dayFrequency[day] ?: 0
                    val ratio = count.toFloat() / maxCount

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .fillMaxHeight(ratio.coerceAtLeast(0.01f))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(GymCoachColors.Success)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1),
                            style = MaterialTheme.typography.bodySmall,
                            color = GymCoachColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}
