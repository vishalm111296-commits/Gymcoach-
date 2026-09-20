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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
    onNavigateToStreaks: () -> Unit = {},
    onNavigateToVTaper: () -> Unit = {},
    onNavigateToTrainingFrequency: () -> Unit = {},
    onNavigateToNutrition: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val weeklyWorkoutCount by viewModel.weeklyWorkoutCount.collectAsStateWithLifecycle()
    val latestReadiness by viewModel.latestReadiness.collectAsStateWithLifecycle()

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
            StaggeredDashboardItem(index = 0, isLoading = state.isLoading) {
                GreetingHeader(state)
            }

            StaggeredDashboardItem(index = 1, isLoading = state.isLoading) {
                QuickStatsRow(
                    weeklyWorkoutCount = weeklyWorkoutCount,
                    latestReadiness = latestReadiness,
                    onNavigateToTrainingFrequency = onNavigateToTrainingFrequency,
                    onNavigateToReadiness = onNavigateToReadiness,
                    onNavigateToNutrition = onNavigateToNutrition
                )
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else {
                StaggeredDashboardItem(index = 2, isLoading = state.isLoading) {
                    if (state.todayWorkout == null) {
                        EmptyProgramCard(onViewProgram)
                    } else {
                        // Subtle pulsing glow and gentle breathing pulse on active workout card
                        val infiniteTransition = rememberInfiniteTransition(label = "heroPulseTransition")
                        val heroScale by infiniteTransition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = 1.015f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "heroCardScale"
                        )
                        val heroBorderAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.35f,
                            targetValue = 0.85f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "heroBorderAlpha"
                        )

                        TodayWorkoutCard(
                            workoutName = state.todayWorkout?.name ?: "",
                            targetMuscles = state.todayWorkout?.targetMuscles ?: emptyList(),
                            exerciseCount = state.todayWorkout?.exerciseCount ?: 0,
                            estimatedDuration = state.todayWorkout?.estimatedDurationMin ?: 0,
                            onStartClick = { viewModel.startTodayWorkout { id -> onStartWorkout(id) } },
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = heroScale
                                    scaleY = heroScale
                                }
                                .drawWithContent {
                                    drawContent()
                                    drawRoundRect(
                                        color = AccentBlue.copy(alpha = heroBorderAlpha),
                                        cornerRadius = CornerRadius(18.dp.toPx()),
                                        style = Stroke(width = 1.dp.toPx())
                                    )
                                }
                        )
                    }
                }

                // Recovery & Readiness Quick Card
                StaggeredDashboardItem(index = 3, isLoading = state.isLoading) {
                    ReadinessDashboardCard(
                        readiness = state.latestReadiness,
                        onClick = onNavigateToReadiness
                    )
                }

                // Weekly Consistency & PR Summary
                StaggeredDashboardItem(index = 4, isLoading = state.isLoading) {
                    WeeklyConsistencyCard(
                        workoutsThisWeek = state.workoutsThisWeek,
                        targetWorkouts = state.targetWorkouts,
                        prCount = state.prCount,
                        onClick = onNavigateToStreaks
                    )
                }

                // Coach Insight Card
                if (state.coachInsight.isNotBlank()) {
                    StaggeredDashboardItem(index = 5, isLoading = state.isLoading) {
                        CoachInsightCard(state.coachInsight)
                    }
                }

                // V-Taper Focus if available
                if (state.vtaperBars.isNotEmpty()) {
                    StaggeredDashboardItem(index = 6, isLoading = state.isLoading) {
                        Box(modifier = Modifier.clickable(onClick = onNavigateToVTaper)) {
                            VtaperFocusCard(muscleData = state.vtaperBars)
                        }
                    }
                }

                // Quick Actions: Blank Workout, Templates, Exercise Library
                StaggeredDashboardItem(index = 7, isLoading = state.isLoading) {
                    QuickActionsSection(
                        onStartBlankWorkout = { onStartWorkout(null) },
                        onNavigateToTemplates = onNavigateToTemplates,
                        onNavigateToExercises = onNavigateToExercises
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickStatsRow(
    weeklyWorkoutCount: Int,
    latestReadiness: Int,
    onNavigateToTrainingFrequency: () -> Unit,
    onNavigateToReadiness: () -> Unit,
    onNavigateToNutrition: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickStatChip(
            icon = Icons.Filled.DateRange,
            text = "$weeklyWorkoutCount Workouts",
            onClick = onNavigateToTrainingFrequency,
            modifier = Modifier.weight(1f)
        )
        QuickStatChip(
            icon = Icons.Filled.Favorite,
            text = "$latestReadiness Readiness",
            onClick = onNavigateToReadiness,
            modifier = Modifier.weight(1f)
        )
        QuickStatChip(
            icon = Icons.Filled.LocalDining,
            text = "0 kcal today",
            onClick = onNavigateToNutrition,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickStatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        border = GymCoachBorders.subtleBorder()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GymCoachColors.Primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                fontSize = 10.sp
            )
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

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "readinessPressScale"
    )

    val (badgeColor, statusLabel) = when {
        !isToday || readiness == null -> Pair(TextTertiary, "Check-in Pending")
        score >= 4.0 -> Pair(GymCoachColors.Success, "Optimal Recovery")
        score >= 3.0 -> Pair(AccentBlue, "Good Readiness")
        score >= 2.0 -> Pair(GymCoachColors.Warning, "Moderate Fatigue")
        else -> Pair(GymCoachColors.Danger, "Rest Advised")
    }

    val targetProgress = (score / 5.0).toFloat().coerceIn(0.05f, 1.0f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "readinessProgressAnim"
    )

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
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
                        progress = { animatedProgress },
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
    prCount: Int,
    onClick: () -> Unit = {}
) {
    val maxTarget = targetWorkouts.coerceIn(1, 7)
    val weeklyFraction = (workoutsThisWeek.toFloat() / maxTarget).coerceIn(0f, 1f)
    val animatedWeeklyProgress by animateFloatAsState(
        targetValue = weeklyFraction,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "weeklyGoalProgressAnim"
    )

    // Subtle pulsing glow for streak / PR achievement
    val infiniteTransition = rememberInfiniteTransition(label = "weeklyStreakInfinite")
    val prPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "prPulseScale"
    )
    val prGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "prGlowAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GymCoachShapes.md)
            .border(GymCoachBorders.subtle, GymCoachShapes.md)
            .clickable(onClick = onClick),
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

                // Progress Indicator Dots with animated scale & color
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..maxTarget) {
                        val isDone = i <= workoutsThisWeek
                        val dotScale by animateFloatAsState(
                            targetValue = if (isDone) 1f else 0.85f,
                            animationSpec = tween(
                                durationMillis = 400,
                                delayMillis = (i - 1) * 70,
                                easing = FastOutSlowInEasing
                            ),
                            label = "dotScale_$i"
                        )
                        val dotColor by animateColorAsState(
                            targetValue = if (isDone) AccentBlue else DarkSurface,
                            animationSpec = tween(
                                durationMillis = 400,
                                delayMillis = (i - 1) * 70,
                                easing = FastOutSlowInEasing
                            ),
                            label = "dotColor_$i"
                        )
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = dotScale
                                    scaleY = dotScale
                                }
                                .size(width = 16.dp, height = 5.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { animatedWeeklyProgress },
                    modifier = Modifier
                        .width(130.dp)
                        .height(3.dp)
                        .clip(CircleShape),
                    color = AccentBlue,
                    trackColor = GymCoachColors.SurfaceInput
                )
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
                            modifier = Modifier
                                .size(17.dp)
                                .graphicsLayer {
                                    scaleX = prPulseScale
                                    scaleY = prPulseScale
                                    alpha = prGlowAlpha
                                }
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val tileScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "actionTileScale"
    )

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .graphicsLayer {
                scaleX = tileScale
                scaleY = tileScale
            }
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
    val btnInteractionSource = remember { MutableInteractionSource() }
    val isPressed by btnInteractionSource.collectIsPressedAsState()
    val btnScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "emptyProgramBtnScale"
    )

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
                interactionSource = btnInteractionSource,
                shape = GymCoachShapes.md,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .graphicsLayer {
                        scaleX = btnScale
                        scaleY = btnScale
                    }
            ) {
                Text("Generate or Build Program", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Staggered entrance animation wrapper for dashboard sections.
 * Combines a subtle vertical translation with fade-in based on item index.
 */
@Composable
private fun StaggeredDashboardItem(
    index: Int,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            visible = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 420,
            delayMillis = index * 55,
            easing = FastOutSlowInEasing
        ),
        label = "staggeredAlpha_$index"
    )

    val translateY by animateFloatAsState(
        targetValue = if (visible) 0f else 26f,
        animationSpec = tween(
            durationMillis = 450,
            delayMillis = index * 55,
            easing = FastOutSlowInEasing
        ),
        label = "staggeredTranslateY_$index"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = translateY
            }
    ) {
        content()
    }
}
