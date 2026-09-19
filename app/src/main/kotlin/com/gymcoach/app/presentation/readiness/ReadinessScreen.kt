package com.gymcoach.app.presentation.readiness

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import com.gymcoach.app.data.local.entity.ReadinessEntity
import com.gymcoach.app.ui.theme.*

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadinessScreen(
    onBackClick: () -> Unit = {},
    viewModel: ReadinessViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Recovery & Readiness", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showLogDialog() },
                containerColor = AccentBlue,
                contentColor = androidx.compose.ui.graphics.Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log Readiness")
            }
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(8.dp))

                    // Latest readiness score
                    state.latestReadiness?.let { latest ->
                        ReadinessScoreCard(latest)
                    } ?: run {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard)
                        ) {
                            Column(modifier = Modifier.padding(GymCoachSpacing.xxl)) {
                                Text(
                                    text = "NO READINESS DATA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GymCoachColors.Primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Tap + to log how you're feeling today. This helps determine your training readiness.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Training recommendation
                    state.latestReadiness?.let { latest ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard)
                        ) {
                            Column(modifier = Modifier.padding(GymCoachSpacing.lg)) {
                                Text(
                                    text = "TRAINING RECOMMENDATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GymCoachColors.Primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = latest.trainingRecommendation,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = WarmWhite,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (latest.isRestDayRecommended) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Consider a rest day or very light activity.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    // Recent history
                    if (state.recentReadiness.isNotEmpty()) {
                        Text(
                            text = "LAST 7 DAYS",
                            style = MaterialTheme.typography.labelSmall,
                            color = GymCoachColors.Primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        state.recentReadiness.forEach { entry ->
                            ReadinessHistoryItem(entry)
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    Spacer(Modifier.height(80.dp)) // Space for FAB
                }
            }
        }
    }

    // Log dialog
    if (state.showDialog) {
        ReadinessLogDialog(
            state = state,
            onSleepQualityChange = viewModel::setSleepQuality,
            onSorenessChange = viewModel::setSoreness,
            onEnergyChange = viewModel::setEnergy,
            onMotivationChange = viewModel::setMotivation,
            onNotesChange = viewModel::setNotes,
            onSave = viewModel::saveReadiness,
            onDismiss = viewModel::hideLogDialog
        )
    }
}

@Composable
private fun ReadinessScoreCard(readiness: ReadinessEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GymCoachSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BIOMETRIC READINESS",
                    style = MaterialTheme.typography.labelSmall,
                    color = GymCoachColors.Primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                BiometricBeacon(score = readiness.readinessScore)
            }

            Spacer(Modifier.height(16.dp))

            // Recovery gauge sweep with biometric breathing pulse
            ReadinessGauge(
                score = readiness.readinessScore,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Metric breakdown with animated progress bars
            MetricRow(icon = Icons.Default.Bedtime, label = "Sleep Quality", value = readiness.sleepQuality, delayIndex = 0)
            MetricRow(icon = Icons.Default.FitnessCenter, label = "Muscle Recovery", value = readiness.soreness, delayIndex = 1)
            MetricRow(icon = Icons.Default.Bolt, label = "Energy Level", value = readiness.energy, delayIndex = 2)
            MetricRow(icon = Icons.Default.LocalFireDepartment, label = "Neural Drive", value = readiness.motivation, delayIndex = 3)
        }
    }
}

@Composable
private fun ReadinessGauge(
    score: Double,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(score) {
        animatedProgress.animateTo(
            targetValue = (score / 5.0).toFloat().coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "biometricBreathing")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1.02f,
        targetValue = 1.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, delayMillis = 200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringScale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, delayMillis = 200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringAlpha"
    )

    val (gaugeColorStart, gaugeColorEnd) = when {
        score >= 4.0 -> GymCoachColors.CyanAccent to GymCoachColors.Success
        score >= 3.0 -> GymCoachColors.Primary to GymCoachColors.CyanAccent
        score >= 2.0 -> GymCoachColors.Warning to GymCoachColors.GoldAccent
        else -> GymCoachColors.Danger to Color(0xFFFF6B6B)
    }

    val statusLabel = when {
        score >= 4.0 -> "OPTIMAL RECOVERY"
        score >= 3.0 -> "GOOD RECOVERY"
        score >= 2.0 -> "MODERATE FATIGUE"
        else -> "REST RECOMMENDED"
    }

    val sweepColors = remember(gaugeColorStart, gaugeColorEnd) {
        listOf(gaugeColorStart, gaugeColorEnd, gaugeColorStart)
    }

    Box(
        modifier = modifier.size(210.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val arcPadding = 26.dp.toPx()
            val diameter = size.minDimension - (arcPadding * 2)
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val radius = diameter / 2f

            // Biometric breathing pulse outer aura
            drawCircle(
                color = gaugeColorStart.copy(alpha = ringAlpha),
                radius = radius * ringScale,
                center = centerOffset,
                style = Stroke(width = 3.dp.toPx())
            )

            // Biometric breathing pulse inner glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        gaugeColorStart.copy(alpha = pulseAlpha),
                        gaugeColorEnd.copy(alpha = pulseAlpha * 0.25f),
                        Color.Transparent
                    ),
                    center = centerOffset,
                    radius = (radius * pulseScale).coerceAtLeast(1f)
                ),
                radius = radius * pulseScale,
                center = centerOffset
            )

            // Background Track Arc (240 degrees from 150° to 390°)
            val startAngle = 150f
            val maxSweepAngle = 240f
            drawArc(
                color = GymCoachColors.SurfaceCardElevated,
                startAngle = startAngle,
                sweepAngle = maxSweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Foreground Sweep Progress Arc
            val currentSweep = maxSweepAngle * animatedProgress.value
            if (currentSweep > 0.5f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = sweepColors,
                        center = centerOffset
                    ),
                    startAngle = startAngle,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Center Biometric Score & Status text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val displayScore = score * animatedProgress.value
            Text(
                text = "%.1f".format(displayScore),
                style = MaterialTheme.typography.displayMedium,
                color = WarmWhite,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "OUT OF 5.0",
                style = MaterialTheme.typography.labelSmall,
                color = GymCoachColors.TextSecondary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(gaugeColorStart.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = gaugeColorStart,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun BiometricBeacon(score: Double) {
    val infiniteTransition = rememberInfiniteTransition(label = "beaconPulse")
    val beaconScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconScale"
    )
    val beaconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconAlpha"
    )

    val color = when {
        score >= 4.0 -> GymCoachColors.Success
        score >= 3.0 -> GymCoachColors.CyanAccent
        score >= 2.0 -> GymCoachColors.Warning
        else -> GymCoachColors.Danger
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(16.dp)) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer {
                        scaleX = beaconScale
                        scaleY = beaconScale
                        alpha = beaconAlpha
                    }
                    .clip(CircleShape)
                    .background(color)
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = "HRV / REST ACTIVE",
            style = MaterialTheme.typography.labelSmall,
            color = GymCoachColors.TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MetricRow(
    icon: ImageVector,
    label: String,
    value: Int,
    delayIndex: Int = 0
) {
    val animatedFill by animateFloatAsState(
        targetValue = (value / 5f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, delayMillis = delayIndex * 100, easing = FastOutSlowInEasing),
        label = "metric_fill_$label"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(GymCoachColors.SurfaceCardElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = GymCoachColors.Primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$value / 5",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = WarmWhite
                )
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(GymCoachColors.SurfaceCardElevated)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFill)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(GymCoachColors.Primary, GymCoachColors.CyanAccent)
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun ReadinessHistoryItem(readiness: ReadinessEntity) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GymCoachSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateFormat.format(Date(readiness.recordedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = readiness.trainingRecommendation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmWhite
                )
            }
            Text(
                text = "%.1f".format(readiness.readinessScore),
                style = MaterialTheme.typography.titleLarge,
                color = GymCoachColors.Primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ReadinessLogDialog(
    state: ReadinessUiState,
    onSleepQualityChange: (Int) -> Unit,
    onSorenessChange: (Int) -> Unit,
    onEnergyChange: (Int) -> Unit,
    onMotivationChange: (Int) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How are you feeling?") },
        text = {
            Column {
                SliderMetric(label = "Sleep Quality", value = state.sleepQuality, onValueChange = onSleepQualityChange)
                SliderMetric(label = "Soreness (5 = no soreness)", value = state.soreness, onValueChange = onSorenessChange)
                SliderMetric(label = "Energy Level", value = state.energy, onValueChange = onEnergyChange)
                SliderMetric(label = "Motivation", value = state.motivation, onValueChange = onMotivationChange)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = onNotesChange,
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("SAVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}

@Composable
private fun SliderMetric(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "$label: $value / 5",
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 1f..5f,
            steps = 3,
            colors = SliderDefaults.colors(
                thumbColor = GymCoachColors.Primary,
                activeTrackColor = GymCoachColors.Primary
            )
        )
    }
}
