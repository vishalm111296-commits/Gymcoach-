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
import com.gymcoach.app.ui.components.GymCoachCard
import com.gymcoach.app.ui.components.GymCoachButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
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
    onNavigateToHistory: () -> Unit = {},
    onNavigateToReadiness: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()



    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        GreetingHeader()
        Spacer(Modifier.height(20.dp))

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp)) {
                CircularProgressIndicator(color = AccentBlue, modifier = Modifier.align(Alignment.Center))
            }
        } else {
            // Profile & Setup State
            if (state.profile == null) {
                GymCoachCard(onClick = onNavigateToProfile) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "PROFILE INCOMPLETE",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Complete your profile to generate a tailored program.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(12.dp))
                        GymCoachButton(
                            text = "COMPLETE SETUP",
                            onClick = onNavigateToProfile,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Readiness State
            GymCoachCard(onClick = onNavigateToReadiness) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "RECOVERY & READINESS",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    if (state.readiness != null) {
                        val readinessScore = (state.readiness!!.sleepQuality + state.readiness!!.energy + state.readiness!!.motivation - state.readiness!!.soreness)
                        Text(
                            text = if (readinessScore > 0) "Ready to Train" else "Needs Recovery",
                            style = MaterialTheme.typography.headlineSmall,
                            color = WarmWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Score: $readinessScore",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    } else {
                        Text(
                            text = "Log how you're feeling today to track recovery.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(12.dp))
                        GymCoachButton(
                            text = "LOG READINESS",
                            onClick = onNavigateToReadiness,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Today's Workout
            if (!state.hasProgram || state.todayWorkout == null) {
                EmptyProgramCard(onViewProgram)
            } else {
                TodayWorkoutCard(
                    workoutName = state.todayWorkout?.name ?: "",
                    targetMuscles = state.todayWorkout?.targetMuscles ?: emptyList(),
                    exerciseCount = state.todayWorkout?.exerciseCount ?: 0,
                    estimatedDuration = state.todayWorkout?.estimatedDurationMin ?: 0,
                    onStartClick = onStartWorkout
                )
            }
            Spacer(Modifier.height(16.dp))

            // Completed Workout
            if (state.latestCompletedWorkout != null) {
                GymCoachCard(onClick = onNavigateToHistory) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "RECENT ACTIVITY",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (state.latestCompletedWorkout!!.notes.isNotBlank()) state.latestCompletedWorkout!!.notes else "Completed Workout",
                            style = MaterialTheme.typography.headlineSmall,
                            color = WarmWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Volume: ${state.latestCompletedWorkout!!.volume} kg",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            } else {
                GymCoachCard(onClick = onNavigateToHistory) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "RECENT ACTIVITY",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "No completed workouts yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Coach Insights (only real data)
            CoachInsightCard(state.coachInsight)
            Spacer(Modifier.height(16.dp))

            // Progress Summary
            GymCoachCard(onClick = onNavigateToProgress) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TRAINING PROGRESS",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    WeekSummaryRow(state.workoutsThisWeek, state.targetWorkouts, state.prCount)
                }
            }

            if (state.vtaperBars.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                VtaperFocusCard(muscleData = state.vtaperBars)
            }
        }
        Spacer(Modifier.height(32.dp))
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

/** Action-oriented empty state - never "No workouts yet". */
@Composable
private fun EmptyProgramCard(onSetUpPlan: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "YOUR FIRST SESSION IS READY",
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
                text = "Two minutes of setup and your first V-taper program is generated around your goal, schedule, and equipment.",
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
                Text("SET UP MY PLAN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun CoachInsightCard(insight: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "COACH",
                style = MaterialTheme.typography.labelSmall,
                color = AccentBlue,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = insight.ifBlank { "Log sessions to unlock volume insights." },
                style = MaterialTheme.typography.bodyMedium,
                color = WarmWhite
            )
        }
    }
}

@Composable
private fun WeekSummaryRow(workoutsThisWeek: Int, targetWorkouts: Int, prCount: Int) {
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
                text = "$prCount",
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
