package com.gymcoach.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcoach.app.presentation.components.PrimaryActionButton
import com.gymcoach.app.presentation.home.components.TodayWorkoutCard
import com.gymcoach.app.presentation.home.components.VtaperFocusCard
import com.gymcoach.app.ui.GymCoachBottomNav
import com.gymcoach.app.ui.theme.*
import java.time.LocalTime

@Composable
fun HomeDashboardScreen(
    onStartWorkout: () -> Unit,
    onViewProgram: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToReadiness: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            GymCoachBottomNav(
                currentRoute = "home",
                onNavigate = { route ->
                    when (route) {
                        "workout" -> onStartWorkout()
                        "program" -> onViewProgram()
                        "progress" -> onNavigateToProgress()
                        "profile" -> onNavigateToProfile()
                        else -> Unit
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(32.dp))
            PremiumGreetingHeader()
            Spacer(Modifier.height(24.dp))

            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp)) {
                        CircularProgressIndicator(color = AccentBlue, modifier = Modifier.align(Alignment.Center))
                    }
                }
                state.todayWorkout == null -> PremiumEmptyProgramCard(onViewProgram)
                else -> PremiumTodayWorkoutCard(
                    workoutName = state.todayWorkout?.name ?: "",
                    targetMuscles = state.todayWorkout?.targetMuscles ?: emptyList(),
                    exerciseCount = state.todayWorkout?.exerciseCount ?: 0,
                    estimatedDuration = state.todayWorkout?.estimatedDurationMin ?: 0,
                    onStartClick = onStartWorkout
                )
            }

            Spacer(Modifier.height(24.dp))

            // Context & Recovery Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PremiumSummaryMetric(
                    title = "WEEKLY SETS",
                    value = "${state.workoutsThisWeek}/${state.targetWorkouts}",
                    modifier = Modifier.weight(1f)
                )
                PremiumSummaryMetric(
                    title = "PRs HIT",
                    value = "${state.prCount}",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(24.dp))

            PremiumReadinessCard(onClick = onNavigateToReadiness)

            Spacer(Modifier.height(24.dp))

            if (state.vtaperBars.isNotEmpty()) {
                VtaperFocusCard(muscleData = state.vtaperBars)
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PremiumGreetingHeader() {
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 12 -> "GOOD MORNING"
        hour < 17 -> "GOOD AFTERNOON"
        else -> "GOOD EVENING"
    }
    Column {
        Text(
            text = greeting,
            style = MaterialTheme.typography.labelMedium,
            color = TextTertiary,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Ready to train?",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun PremiumEmptyProgramCard(onSetUpPlan: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "NO ACTIVE PROGRAM",
                style = MaterialTheme.typography.labelSmall,
                color = AccentNeonGreen,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Build your first V-Taper plan.",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Generate a personalized routine based on your goals, equipment, and schedule.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(24.dp))
            PrimaryActionButton(
                text = "Generate Program",
                onClick = onSetUpPlan
            )
        }
    }
}

@Composable
private fun PremiumTodayWorkoutCard(
    workoutName: String,
    targetMuscles: List<String>,
    exerciseCount: Int,
    estimatedDuration: Int,
    onStartClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AccentBlueDark)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "UP NEXT TODAY",
                style = MaterialTheme.typography.labelSmall,
                color = AccentBlueLight,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = workoutName.uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(16.dp))

            // Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                MetricItem("EXERCISES", exerciseCount.toString())
                MetricItem("DURATION", "~${estimatedDuration}m")
            }

            Spacer(Modifier.height(16.dp))

            // Muscle Tags
            if (targetMuscles.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    targetMuscles.take(3).forEach { muscle ->
                        Surface(
                            color = AccentBlue,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = muscle,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            PrimaryActionButton(
                text = "Start Workout",
                onClick = onStartClick,
                isSuccess = true
            )
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AccentBlueLight,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PremiumSummaryMetric(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumReadinessCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "RECOVERY & READINESS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Log today's readiness",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "->",
                style = MaterialTheme.typography.titleMedium,
                color = AccentBlue,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
