package com.gymcoach.app.presentation.progress.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymcoach.app.presentation.history.formatDuration
import com.gymcoach.app.presentation.progress.ProgressDateRange
import com.gymcoach.app.presentation.progress.ProgressUiState

@Composable
fun ProgressDashboardContent(
    state: ProgressUiState,
    padding: PaddingValues,
    onSelectDateRange: (ProgressDateRange) -> Unit,
    onSelectExercise: (String) -> Unit
) {
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
            onSelect = onSelectDateRange
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
            ExerciseSelector(
                selectedExercise = state.selectedExercise,
                onSelect = onSelectExercise
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
