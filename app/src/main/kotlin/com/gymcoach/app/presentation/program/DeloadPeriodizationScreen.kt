package com.gymcoach.app.presentation.program

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.core.program.DeloadReason
import com.gymcoach.app.core.program.DeloadStatus
import com.gymcoach.app.ui.theme.GymCoachBorders
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes
import com.gymcoach.app.ui.theme.GymCoachSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeloadPeriodizationScreen(
    onBackClick: () -> Unit,
    viewModel: DeloadPeriodizationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mesocycle & Periodization", color = GymCoachColors.TextPrimary) },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = GymCoachColors.TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier.semantics { contentDescription = "Open periodization science info" }
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = GymCoachColors.Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GymCoachColors.PureDark,
                    titleContentColor = GymCoachColors.TextPrimary
                )
            )
        },
        containerColor = GymCoachColors.PureDark
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GymCoachColors.Primary)
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(GymCoachSpacing.lg),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.md)
                ) {
                    Text(
                        text = uiState.error ?: "Failed to load periodization data",
                        style = MaterialTheme.typography.bodyLarge,
                        color = GymCoachColors.Danger,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { viewModel.loadPeriodizationData() },
                        colors = ButtonDefaults.buttonColors(containerColor = GymCoachColors.Primary),
                        shape = GymCoachShapes.md
                    ) {
                        Text("Retry", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = GymCoachSpacing.lg)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.lg)
            ) {
                ProgressionTracker(uiState.currentBlockWeek, uiState.totalBlockWeeks)
                ReadinessTelemetry(uiState.readinessAvg)

                uiState.deloadStatus?.let { status ->
                    DeloadProtocolCard(
                        status = status,
                        isDeloadActive = uiState.isDeloadActive,
                        onToggle = { viewModel.toggleDeloadProtocol() }
                    )
                }

                if (uiState.isDeloadActive) {
                    WorkoutScalingPreview(
                        volumeReduction = uiState.deloadStatus?.volumeReductionPercent ?: 50,
                        intensityReduction = uiState.deloadStatus?.intensityReductionPercent ?: 10
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }

        if (showInfoDialog) {
            PeriodizationInfoDialog(onDismiss = { showInfoDialog = false })
        }
    }
}

@Composable
fun ProgressionTracker(currentWeek: Int, totalWeeks: Int) {
    Card(
        shape = GymCoachShapes.Card,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
        border = GymCoachBorders.subtleBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GymCoachSpacing.lg)
        ) {
            Text(
                "4-Week Progression Tracker",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GymCoachColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(GymCoachSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val labels = listOf("Accumulation", "Overload", "Peak Volume", "Deload")
                for (i in 1..totalWeeks) {
                    val isActive = i == currentWeek
                    val isPast = i < currentWeek
                    val color = when {
                        isActive -> GymCoachColors.Primary
                        isPast -> GymCoachColors.Success
                        else -> GymCoachColors.BorderLight
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isActive || isPast) color.copy(alpha = 0.2f) else Color.Transparent)
                                .background(if (isActive) color else Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "W$i",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) GymCoachColors.PureDark else (if (isPast) color else GymCoachColors.TextMuted)
                            )
                        }
                        Spacer(modifier = Modifier.height(GymCoachSpacing.xs))
                        Text(
                            text = labels.getOrElse(i - 1) { "" },
                            style = MaterialTheme.typography.bodySmall,
                            color = GymCoachColors.TextMuted,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                    if (i < totalWeeks) {
                        HorizontalDivider(
                            modifier = Modifier
                                .weight(0.5f)
                                .padding(horizontal = 4.dp),
                            color = if (isPast) GymCoachColors.Success else GymCoachColors.BorderLight,
                            thickness = 2.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReadinessTelemetry(readinessAvg: Int) {
    Card(
        shape = GymCoachShapes.Card,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
        border = GymCoachBorders.subtleBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GymCoachSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(GymCoachColors.SurfaceInput),
                contentAlignment = Alignment.Center
            ) {
                val color = if (readinessAvg >= 70) GymCoachColors.Success else if (readinessAvg >= 55) GymCoachColors.Warning else GymCoachColors.Danger
                CircularProgressIndicator(
                    progress = { readinessAvg / 100f },
                    color = color,
                    trackColor = GymCoachColors.BorderLight,
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    text = "$readinessAvg",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GymCoachColors.TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(GymCoachSpacing.lg))
            Column {
                Text(
                    text = "Neuro-Recovery & Readiness",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GymCoachColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(GymCoachSpacing.xs))
                val statusText = if (readinessAvg >= 70) "Optimal Readiness" else if (readinessAvg >= 55) "Moderate Fatigue" else "High Fatigue Accumulation"
                val statusColor = if (readinessAvg >= 70) GymCoachColors.Success else if (readinessAvg >= 55) GymCoachColors.Warning else GymCoachColors.Danger

                Surface(
                    shape = GymCoachShapes.pill,
                    color = statusColor.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DeloadProtocolCard(status: DeloadStatus, isDeloadActive: Boolean, onToggle: () -> Unit) {
    Card(
        shape = GymCoachShapes.Card,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCardElevated),
        border = if (status.isDeloadRecommended) BorderStroke(1.dp, GymCoachColors.Primary.copy(alpha = 0.5f)) else GymCoachBorders.subtleBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GymCoachSpacing.lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (status.isDeloadRecommended) Icons.Default.Warning else Icons.Default.Thermostat,
                    contentDescription = null,
                    tint = if (status.isDeloadRecommended) GymCoachColors.Warning else GymCoachColors.CyanAccent
                )
                Spacer(modifier = Modifier.width(GymCoachSpacing.sm))
                Text(
                    text = "Deload Recommendation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GymCoachColors.TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(GymCoachSpacing.md))
            Text(
                text = status.coachingAdvice,
                style = MaterialTheme.typography.bodyMedium,
                color = GymCoachColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(GymCoachSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm)
            ) {
                MicroCard("-${status.volumeReductionPercent}% Volume", "Sets", Modifier.weight(1f))
                MicroCard("-${status.intensityReductionPercent}% Load", "Weight", Modifier.weight(1f))
                MicroCard("Recovery", "Joints", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(GymCoachSpacing.lg))

            Button(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDeloadActive) GymCoachColors.PureDark else GymCoachColors.Primary,
                    contentColor = if (isDeloadActive) GymCoachColors.Success else GymCoachColors.TextPrimary
                ),
                border = if (isDeloadActive) BorderStroke(1.dp, GymCoachColors.Success) else null
            ) {
                Text(if (isDeloadActive) "Deload Protocol Active" else "Activate Deload Protocol")
            }
        }
    }
}

@Composable
fun MicroCard(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = GymCoachShapes.sm,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceInput)
    ) {
        Column(
            modifier = Modifier.padding(GymCoachSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = GymCoachColors.CyanAccent)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = GymCoachColors.TextMuted)
        }
    }
}

@Composable
fun WorkoutScalingPreview(
    volumeReduction: Int = 50,
    intensityReduction: Int = 10
) {
    Card(
        shape = GymCoachShapes.Card,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
        border = GymCoachBorders.subtleBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GymCoachSpacing.lg)
        ) {
            Text(
                "Workout Scaling Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GymCoachColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(GymCoachSpacing.sm))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Barbell Squat", color = GymCoachColors.TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("-$volumeReduction% Volume, -$intensityReduction% Load", color = GymCoachColors.TextMuted, style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("4 sets × 100kg", color = GymCoachColors.TextMuted, style = MaterialTheme.typography.bodySmall, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                    val scaledSets = (4 * (100 - volumeReduction) / 100).coerceAtLeast(1)
                    val scaledWeight = (100.0 * (100 - intensityReduction) / 100.0)
                    Text("$scaledSets sets × ${scaledWeight.toInt()}kg", color = GymCoachColors.Success, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PeriodizationInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GymCoachColors.SurfaceCard,
        titleContentColor = GymCoachColors.TextPrimary,
        icon = {
            Icon(Icons.Default.Science, contentDescription = null, tint = GymCoachColors.Primary)
        },
        title = {
            Text("Periodization & Deload Science", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "• Week 1 (Accumulation): Establish baseline motor patterns and progressive volume.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GymCoachColors.TextSecondary
                )
                Text(
                    "• Week 2 (Overload): Increase intensity and working loads progressively.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GymCoachColors.TextSecondary
                )
                Text(
                    "• Week 3 (Peak Volume): Maximum adaptive volume stimulus with high effort.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GymCoachColors.TextSecondary
                )
                Text(
                    "• Week 4 (Deload): 50% volume drop and 10% load reduction to clear fatigue, restore connective tissues, and prepare for the next mesocycle.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GymCoachColors.TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GymCoachColors.Primary)
            ) {
                Text("Got It")
            }
        }
    )
}
