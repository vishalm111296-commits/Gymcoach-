package com.gymcoach.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcoach.app.presentation.home.components.TodayWorkoutCard
import com.gymcoach.app.presentation.home.components.VtaperFocusCard
import com.gymcoach.app.presentation.home.components.TrainingInsightCard
import com.gymcoach.app.presentation.home.components.ReadinessCard
import com.gymcoach.app.ui.GymCoachBottomNav
import com.gymcoach.app.ui.theme.AccentBlue
import com.gymcoach.app.ui.theme.DarkBackground
import com.gymcoach.app.ui.theme.DarkSurface
import com.gymcoach.app.ui.theme.TextSecondary
import com.gymcoach.app.ui.theme.TextTertiary
import com.gymcoach.app.ui.theme.WarmWhite
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
            Spacer(Modifier.height(24.dp))
            GreetingHeader()
            Spacer(Modifier.height(20.dp))

            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp)) {
                        CircularProgressIndicator(color = AccentBlue, modifier = Modifier.align(Alignment.Center))
                    }
                }

                state.todayWorkout == null -> EmptyProgramCard(onViewProgram)

                else -> TodayWorkoutCard(
                    workoutName = state.todayWorkout?.name ?: "",
                    targetMuscles = state.todayWorkout?.targetMuscles ?: emptyList(),
                    exerciseCount = state.todayWorkout?.exerciseCount ?: 0,
                    estimatedDuration = state.todayWorkout?.estimatedDurationMin ?: 0,
                    onStartClick = onStartWorkout
                )
            }

            Spacer(Modifier.height(16.dp))
            ReadinessCard(
                isLoggedToday = state.readiness.isLoggedToday,
                recommendation = state.readiness.recommendation,
                onClick = onNavigateToReadiness
            )

            Spacer(Modifier.height(16.dp))
            TrainingInsightCard(state.trainingInsight)

            if (state.vtaperBars.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                VtaperFocusCard(muscleData = state.vtaperBars)
            } else {
                Spacer(Modifier.height(16.dp))
                EmptyVtaperCard()
            }

            Spacer(Modifier.height(24.dp))
            WeekSummaryRow(state.workoutsThisWeek, state.targetWorkouts, state.prCount, state.totalWorkouts)

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun GreetingHeader() {
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
            letterSpacing = 2.sp
        )
        Text(
            text = "Ready to train?",
            style = MaterialTheme.typography.headlineMedium,
            color = WarmWhite,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Action-oriented empty state for workout. */
@Composable
private fun EmptyProgramCard(onSetUpPlan: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "NO WORKOUT SCHEDULED",
                style = MaterialTheme.typography.labelSmall,
                color = AccentBlue,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Let's build your plan",
                style = MaterialTheme.typography.headlineSmall,
                color = WarmWhite,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Generate a personalized V-taper program around your goal, schedule, and equipment.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Button(
                onClick = onSetUpPlan,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = WarmWhite),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(56.dp)
            ) {
                Text("SET UP PLAN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun EmptyVtaperCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "V-TAPER FOCUS",
                style = MaterialTheme.typography.labelSmall,
                color = AccentBlue,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Set up a plan to see your V-taper progression targets.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun WeekSummaryRow(workoutsThisWeek: Int, targetWorkouts: Int, prCount: Int, totalWorkouts: Int) {
    if (totalWorkouts == 0) {
        Text(
            text = "No workout history yet. Complete your first session to see progress.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(vertical = 12.dp)
        )
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(48.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "$workoutsThisWeek/$targetWorkouts",
                    style = MaterialTheme.typography.titleLarge,
                    color = AccentBlue,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "workouts this week",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (prCount == 0) "No PRs yet" else "$prCount",
                    style = MaterialTheme.typography.titleLarge,
                    color = WarmWhite,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "personal records",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
