package com.gymcoach.app.presentation.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.core.analytics.BalanceStatus
import com.gymcoach.app.core.analytics.CorrectivePrescription
import com.gymcoach.app.core.analytics.MuscleBalanceReport
import com.gymcoach.app.core.analytics.MuscleRatio
import com.gymcoach.app.ui.theme.GymCoachBorders
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes
import com.gymcoach.app.ui.theme.GymCoachSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuscleBalanceScreen(
    onBackClick: () -> Unit,
    viewModel: MuscleBalanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Muscle Balance", color = GymCoachColors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GymCoachColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GymCoachColors.PureDark
                )
            )
        },
        containerColor = GymCoachColors.PureDark
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is MuscleBalanceUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = GymCoachColors.Primary
                    )
                }
                is MuscleBalanceUiState.Empty -> {
                    Text(
                        "No data available for the past 30 days.",
                        color = GymCoachColors.TextSecondary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is MuscleBalanceUiState.Success -> {
                    MuscleBalanceContent(report = state.report)
                }
            }
        }
    }
}

@Composable
fun MuscleBalanceContent(report: MuscleBalanceReport) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = GymCoachSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.lg),
        contentPadding = PaddingValues(vertical = GymCoachSpacing.lg)
    ) {
        item {
            OverallScoreCard(score = report.overallBalanceScore)
        }
        item {
            RatioCard(ratio = report.pushPullRatio)
        }
        item {
            RatioCard(ratio = report.quadHamstringRatio)
        }
        item {
            RatioCard(ratio = report.bicepsTricepsRatio)
        }
        if (report.correctivePrescriptions.isNotEmpty()) {
            item {
                Text(
                    text = "Corrective Prescriptions",
                    style = MaterialTheme.typography.titleMedium,
                    color = GymCoachColors.TextPrimary,
                    modifier = Modifier.padding(top = GymCoachSpacing.sm, bottom = GymCoachSpacing.xs)
                )
            }
            items(report.correctivePrescriptions) { prescription ->
                PrescriptionCard(prescription = prescription)
            }
        }
    }
}

@Composable
fun OverallScoreCard(score: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = GymCoachShapes.Card,
        color = GymCoachColors.SurfaceCardElevated,
        border = GymCoachBorders.subtleBorder()
    ) {
        Column(
            modifier = Modifier.padding(GymCoachSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Overall Balance Score",
                style = MaterialTheme.typography.titleMedium,
                color = GymCoachColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(GymCoachSpacing.md))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(GymCoachColors.SurfaceDeep)
                    .border(4.dp, if (score >= 80) GymCoachColors.Success else GymCoachColors.Warning, CircleShape)
            ) {
                Text(
                    text = "$score%",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = GymCoachColors.TextPrimary
                )
            }
        }
    }
}

@Composable
fun RatioCard(ratio: MuscleRatio) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = GymCoachShapes.Card,
        color = GymCoachColors.SurfaceCard,
        border = GymCoachBorders.subtleBorder()
    ) {
        Column(
            modifier = Modifier.padding(GymCoachSpacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ratio.ratioName,
                    style = MaterialTheme.typography.titleMedium,
                    color = GymCoachColors.TextPrimary
                )
                StatusBadge(status = ratio.status)
            }
            Spacer(modifier = Modifier.height(GymCoachSpacing.md))

            Text(
                text = "Ratio: ${"%.2f".format(ratio.ratio)}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = GymCoachColors.TextPrimary
            )
            Text(
                text = "Optimal Range: ${"%.2f".format(ratio.optimalRange.start)} - ${"%.2f".format(ratio.optimalRange.endInclusive)}",
                style = MaterialTheme.typography.bodySmall,
                color = GymCoachColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(GymCoachSpacing.sm))

            // Visual Bar
            val totalVol = ratio.agonistVolumeKg + ratio.antagonistVolumeKg
            val agRatio = if (totalVol > 0) (ratio.agonistVolumeKg / totalVol).toFloat() else 0.5f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(GymCoachShapes.pill),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(if (agRatio > 0) agRatio else 0.1f)
                        .fillMaxHeight()
                        .background(GymCoachColors.Primary)
                )
                Box(
                    modifier = Modifier
                        .weight(if (1f - agRatio > 0) 1f - agRatio else 0.1f)
                        .fillMaxHeight()
                        .background(GymCoachColors.CyanAccent)
                )
            }
            Spacer(modifier = Modifier.height(GymCoachSpacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${ratio.agonistName}: ${ratio.agonistVolumeKg.toInt()} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = GymCoachColors.TextSecondary
                )
                Text(
                    text = "${ratio.antagonistName}: ${ratio.antagonistVolumeKg.toInt()} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = GymCoachColors.TextSecondary
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: BalanceStatus) {
    val bgColor = when (status) {
        BalanceStatus.OPTIMAL -> GymCoachColors.SuccessBg
        BalanceStatus.MODERATE_IMBALANCE -> GymCoachColors.WarningBg
        BalanceStatus.SEVERE_IMBALANCE -> GymCoachColors.DangerBg
    }
    val textColor = when (status) {
        BalanceStatus.OPTIMAL -> GymCoachColors.Success
        BalanceStatus.MODERATE_IMBALANCE -> GymCoachColors.Warning
        BalanceStatus.SEVERE_IMBALANCE -> GymCoachColors.Danger
    }
    val text = when (status) {
        BalanceStatus.OPTIMAL -> "Optimal"
        BalanceStatus.MODERATE_IMBALANCE -> "Moderate"
        BalanceStatus.SEVERE_IMBALANCE -> "Severe"
    }

    Box(
        modifier = Modifier
            .clip(GymCoachShapes.pill)
            .background(bgColor)
            .padding(horizontal = GymCoachSpacing.sm, vertical = GymCoachSpacing.xxs)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor
        )
    }
}

@Composable
fun PrescriptionCard(prescription: CorrectivePrescription) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = GymCoachShapes.Card,
        color = GymCoachColors.SurfaceInput,
        border = GymCoachBorders.subtleBorder()
    ) {
        Column(
            modifier = Modifier.padding(GymCoachSpacing.md)
        ) {
            Text(
                text = prescription.exerciseName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = GymCoachColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(GymCoachSpacing.xxs))
            Text(
                text = "Target: ${prescription.targetMuscle} | ${prescription.sets} sets x ${prescription.reps}",
                style = MaterialTheme.typography.bodyMedium,
                color = GymCoachColors.CyanAccent
            )
            Spacer(modifier = Modifier.height(GymCoachSpacing.sm))
            Text(
                text = prescription.rationale,
                style = MaterialTheme.typography.bodySmall,
                color = GymCoachColors.TextSecondary
            )
        }
    }
}
