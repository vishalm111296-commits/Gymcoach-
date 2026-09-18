package com.gymcoach.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcoach.app.data.local.entity.ReadinessEntity
import com.gymcoach.app.presentation.home.components.TodayWorkoutCard
import com.gymcoach.app.presentation.home.components.VtaperFocusCard
import com.gymcoach.app.ui.GymCoachBottomNav
import com.gymcoach.app.ui.theme.AccentBlue
import com.gymcoach.app.ui.theme.DarkBackground
import com.gymcoach.app.ui.theme.DarkSurface
import com.gymcoach.app.ui.theme.GymCoachBorders
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes
import com.gymcoach.app.ui.theme.TextPrimary
import com.gymcoach.app.ui.theme.TextSecondary
import com.gymcoach.app.ui.theme.TextTertiary
import com.gymcoach.app.ui.theme.WarmWhite
import java.time.LocalTime

@Composable
fun HomeDashboardScreen(
    onStartWorkout: (Long?) -> Unit,
    onViewProgram: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToReadiness: () -> Unit = {},
    onNavigateToExercises: () -> Unit = {},
    onNavigateToTemplates: () -> Unit = {},
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
                        "workout" -> onStartWorkout(null)
                        "exercise_list" -> onNavigateToExercises()
                        "program_detail" -> onViewProgram()
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GreetingHeader(state)

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentBlue)
                    }
                }

                state.todayWorkout == null -> {
                    EmptyProgramCard(onViewProgram)
                }

                else -> {
                    TodayWorkoutCard(
                        workoutName = state.todayWorkout?.name ?: "",
                        targetMuscles = state.todayWorkout?.targetMuscles ?: emptyList(),
                        exerciseCount = state.todayWorkout?.exerciseCount ?: 0,
                        estimatedDuration = state.todayWorkout?.estimatedDurationMin ?: 0,
                        onStartClick = { viewModel.startTodayWorkout { id -> onStartWorkout(id) } }
                    )
                }
            }

            // Recovery & Readiness Quick Card
            ReadinessDashboardCard(
                readiness = state.latestReadiness,
                onClick = onNavigateToReadiness
            )

            // Weekly Consistency & PR Summary
            WeeklyConsistencyCard(
                workoutsThisWeek = state.workoutsThisWeek,
                targetWorkouts = state.targetWorkouts,
                prCount = state.prCount
            )

            // Coach Insight Card
            if (state.coachInsight.isNotBlank()) {
                CoachInsightCard(state.coachInsight)
            }

            // V-Taper Focus if available
            if (state.vtaperBars.isNotEmpty()) {
                VtaperFocusCard(muscleData = state.vtaperBars)
            }

            // Quick Actions: Blank Workout, Templates, Exercise Library
            QuickActionsSection(
                onStartBlankWorkout = { onStartWorkout(null) },
                onNavigateToTemplates = onNavigateToTemplates,
                onNavigateToExercises = onNavigateToExercises
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GreetingHeader(state: HomeUiState) {
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 12 -> "GOOD MORNING"
        hour < 17 -> "GOOD AFTERNOON"
        else -> "GOOD EVENING"
    }

    // Dynamic alive status driven by real app state
    val dynamicStatus = when {
        state.todayWorkout != null && state.latestReadiness?.readinessScore ?: 0.0 >= 3.8 ->
            "Prime recovery — ready for peak performance."
        state.todayWorkout != null && state.latestReadiness?.readinessScore ?: 5.0 < 2.5 ->
            "Recovery is lower today — focus on form & control."
        state.todayWorkout != null ->
            "Ready for ${state.todayWorkout.name}?"
        state.hasProgram ->
            "Scheduled rest day — recovery fuels growth."
        else ->
            "Start your training journey today."
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 2.sp,
                fontSize = 11.sp
            ),
            color = TextTertiary
        )
        Text(
            text = dynamicStatus,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = TextPrimary
        )
    }
}

@Composable
private fun ReadinessDashboardCard(
    readiness: ReadinessEntity?,
    onClick: () -> Unit
) {
    val isToday = readiness?.isRecordedToday == true
    val score = readiness?.readinessScore ?: 0.0

    val (badgeColor, statusLabel) = when {
        !isToday || readiness == null -> Pair(TextTertiary, "Check-in Pending")
        score >= 4.0 -> Pair(GymCoachColors.Success, "Optimal Recovery")
        score >= 3.0 -> Pair(AccentBlue, "Good Readiness")
        score >= 2.0 -> Pair(GymCoachColors.Warning, "Moderate Fatigue")
        else -> Pair(GymCoachColors.Danger, "Rest Advised")
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(GymCoachShapes.md)
            .border(GymCoachBorders.subtle, GymCoachShapes.md),
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
        shape = GymCoachShapes.md
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "RECOVERY & READINESS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = AccentBlue
                    )
                    Box(
                        modifier = Modifier
                            .clip(GymCoachShapes.xs)
                            .background(badgeColor.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isToday && readiness != null) "%.1f / 5.0".format(score) else statusLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = badgeColor
                        )
                    }
                }

                Text(
                    text = if (isToday && readiness != null) {
                        readiness.trainingRecommendation
                    } else {
                        "Tap to log sleep, soreness & energy to calibrate workout loads"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2
                )

                if (isToday && readiness != null) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (score / 5.0).toFloat().coerceIn(0.05f, 1.0f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = badgeColor,
                        trackColor = GymCoachColors.SurfaceInput
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open Readiness",
                tint = TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun WeeklyConsistencyCard(
    workoutsThisWeek: Int,
    targetWorkouts: Int,
    prCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GymCoachShapes.md)
            .border(GymCoachBorders.subtle, GymCoachShapes.md),
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
        shape = GymCoachShapes.md
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Workouts adherence
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "THIS WEEK'S CONSISTENCY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = TextTertiary
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$workoutsThisWeek",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = AccentBlue
                    )
                    Text(
                        text = " / ${targetWorkouts.coerceAtLeast(1)} sessions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                    )
                }

                // Progress Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    val maxTarget = targetWorkouts.coerceIn(1, 7)
                    for (i in 1..maxTarget) {
                        val isDone = i <= workoutsThisWeek
                        Box(
                            modifier = Modifier
                                .size(width = 16.dp, height = 5.dp)
                                .clip(CircleShape)
                                .background(if (isDone) AccentBlue else DarkSurface)
                        )
                    }
                }
            }

            // PR Count if any
            if (prCount > 0) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = GymCoachColors.GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "$prCount PRs",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = GymCoachColors.GoldAccent
                        )
                    }
                    Text(
                        text = "new bests logged",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun CoachInsightCard(insight: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GymCoachShapes.md)
            .border(GymCoachBorders.subtle, GymCoachShapes.md),
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
        shape = GymCoachShapes.md
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AccentBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "ADAPTIVE COACH INSIGHT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = AccentBlue
                )
                Text(
                    text = insight,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    onStartBlankWorkout: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToExercises: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "QUICK ACTIONS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            ),
            color = TextTertiary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionTile(
                title = "Free Session",
                icon = Icons.Default.Add,
                onClick = onStartBlankWorkout,
                modifier = Modifier.weight(1f)
            )
            ActionTile(
                title = "Templates",
                icon = Icons.Default.Bookmark,
                onClick = onNavigateToTemplates,
                modifier = Modifier.weight(1f)
            )
            ActionTile(
                title = "Exercises",
                icon = Icons.Default.Search,
                onClick = onNavigateToExercises,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .clip(GymCoachShapes.sm)
            .border(GymCoachBorders.subtle, GymCoachShapes.sm),
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
        shape = GymCoachShapes.sm
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = AccentBlue,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EmptyProgramCard(onSetUpPlan: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GymCoachShapes.lg)
            .border(GymCoachBorders.primary, GymCoachShapes.lg),
        shape = GymCoachShapes.lg,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "TRAINING PROGRAM SETUP",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = AccentBlue
            )
            Text(
                text = "No Active Program",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Text(
                text = "Generate an adaptive multi-day routine calibrated to your goals, schedule, and available gym equipment.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Button(
                onClick = onSetUpPlan,
                shape = GymCoachShapes.md,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Generate or Build Program", fontWeight = FontWeight.Bold)
            }
        }
    }
}
