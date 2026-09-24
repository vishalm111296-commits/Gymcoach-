package com.gymcoach.app.presentation.standards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.core.standards.LiftStandard
import com.gymcoach.app.core.standards.OverallStrengthProfile
import com.gymcoach.app.core.standards.StrengthTier
import com.gymcoach.app.domain.repository.PersonalRecord
import com.gymcoach.app.ui.theme.GymCoachBorders
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes
import com.gymcoach.app.ui.theme.GymCoachSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrengthStandardsScreen(
    onBackClick: () -> Unit,
    viewModel: StrengthStandardsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Strength Standards") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state is StrengthStandardsUiState.Success) {
                        val prCount = (state as StrengthStandardsUiState.Success).allPrs.size
                        Badge {
                            Text("$prCount PRs")
                        }
                        Spacer(modifier = Modifier.width(GymCoachSpacing.md))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GymCoachColors.PureDark,
                    titleContentColor = GymCoachColors.TextPrimary,
                    navigationIconContentColor = GymCoachColors.TextPrimary,
                    actionIconContentColor = GymCoachColors.Primary
                )
            )
        },
        containerColor = GymCoachColors.PureDark
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val currentState = state) {
                is StrengthStandardsUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = GymCoachColors.Primary
                    )
                }
                is StrengthStandardsUiState.Empty -> {
                    EmptyState()
                }
                is StrengthStandardsUiState.Success -> {
                    StrengthStandardsContent(
                        profile = currentState.profile,
                        prs = currentState.allPrs
                    )
                }
            }
        }
    }
}

@Composable
private fun StrengthStandardsContent(
    profile: OverallStrengthProfile,
    prs: List<PersonalRecord>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(GymCoachSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.xl)
    ) {
        HeroCard(profile)

        Text(
            text = "Big 4 Compound Matrix",
            style = MaterialTheme.typography.titleLarge,
            color = GymCoachColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Big4Matrix(profile.liftStandards)

        Text(
            text = "PR Trophy Showcase",
            style = MaterialTheme.typography.titleLarge,
            color = GymCoachColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )

        PRShowcase(prs)
    }
}

@Composable
private fun HeroCard(profile: OverallStrengthProfile) {
    val (tierName, tierColor) = getTierDisplayInfo(profile.overallTier)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
        shape = GymCoachShapes.Card,
        border = GymCoachBorders.subtleBorder()
    ) {
        Column(
            modifier = Modifier.padding(GymCoachSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.md)
        ) {
            Text(
                text = "Overall Tier",
                style = MaterialTheme.typography.labelLarge,
                color = GymCoachColors.TextSecondary
            )

            Text(
                text = "$tierName Lifter",
                style = MaterialTheme.typography.headlineMedium,
                color = tierColor,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Top ${100 - profile.percentile}% (Percentile: ${profile.percentile})",
                style = MaterialTheme.typography.bodyMedium,
                color = GymCoachColors.TextPrimary
            )

            HorizontalDivider(color = GymCoachColors.BorderSubtle, modifier = Modifier.padding(vertical = GymCoachSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Total Big 4",
                        style = MaterialTheme.typography.labelMedium,
                        color = GymCoachColors.TextSecondary
                    )
                    Text(
                        text = "${profile.totalBig4Kg.toInt()} kg",
                        style = MaterialTheme.typography.titleMedium,
                        color = GymCoachColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Bodyweight",
                        style = MaterialTheme.typography.labelMedium,
                        color = GymCoachColors.TextSecondary
                    )
                    Text(
                        text = "${profile.bodyweightKg.toInt()} kg",
                        style = MaterialTheme.typography.titleMedium,
                        color = GymCoachColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceDeep),
                shape = GymCoachShapes.sm,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(GymCoachSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Coaching Advice",
                        tint = GymCoachColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = profile.coachingRecommendation,
                        style = MaterialTheme.typography.bodySmall,
                        color = GymCoachColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun Big4Matrix(liftStandards: List<LiftStandard>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.md)
    ) {
        liftStandards.chunked(2).forEach { rowLifts ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GymCoachSpacing.md)
            ) {
                rowLifts.forEach { lift ->
                    LiftCard(
                        lift = lift,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowLifts.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LiftCard(lift: LiftStandard, modifier: Modifier = Modifier) {
    val (_, tierColor) = getTierDisplayInfo(lift.tier)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
        shape = GymCoachShapes.md,
        border = GymCoachBorders.subtleBorder()
    ) {
        Column(
            modifier = Modifier.padding(GymCoachSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm)
        ) {
            Text(
                text = lift.exerciseName,
                style = MaterialTheme.typography.titleSmall,
                color = GymCoachColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${lift.current1RmKg.toInt()} kg",
                    style = MaterialTheme.typography.titleLarge,
                    color = GymCoachColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${String.format("%.1f", lift.bodyweightRatio)}x BW",
                    style = MaterialTheme.typography.labelSmall,
                    color = GymCoachColors.TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .clip(GymCoachShapes.pill)
                    .background(tierColor.copy(alpha = 0.2f))
                    .padding(horizontal = GymCoachSpacing.sm, vertical = 2.dp)
            ) {
                Text(
                    text = lift.tier.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = tierColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(GymCoachSpacing.xxs))

            if (lift.nextTier != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Next: ${lift.nextTier.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GymCoachColors.TextSecondary,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize
                        )
                        Text(
                            text = "${lift.nextTierKg?.toInt() ?: 0} kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = GymCoachColors.TextSecondary,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize
                        )
                    }
                    LinearProgressIndicator(
                        progress = { lift.progressToNextTierPct / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(GymCoachShapes.pill),
                        color = tierColor,
                        trackColor = GymCoachColors.SurfaceDeep
                    )
                }
            } else {
                Text(
                    text = "Max Tier Reached",
                    style = MaterialTheme.typography.bodySmall,
                    color = GymCoachColors.Success,
                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PRShowcase(prs: List<PersonalRecord>) {
    if (prs.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
            shape = GymCoachShapes.md,
            border = GymCoachBorders.subtleBorder()
        ) {
            Text(
                text = "No PRs recorded yet. Keep lifting!",
                modifier = Modifier.padding(GymCoachSpacing.lg),
                color = GymCoachColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm)
        ) {
            prs.forEach { pr ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCardElevated),
                    shape = GymCoachShapes.md
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(GymCoachSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(GymCoachSpacing.md)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(GymCoachShapes.pill)
                                .background(GymCoachColors.GoldAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = "Trophy",
                                tint = GymCoachColors.GoldAccent
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pr.exerciseName,
                                style = MaterialTheme.typography.titleSmall,
                                color = GymCoachColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "${pr.maxWeight.toInt()} kg",
                            style = MaterialTheme.typography.titleMedium,
                            color = GymCoachColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(GymCoachSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = GymCoachColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(GymCoachSpacing.md))
        Text(
            text = "Not Enough Data",
            style = MaterialTheme.typography.titleLarge,
            color = GymCoachColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(GymCoachSpacing.sm))
        Text(
            text = "Please ensure you have recorded your bodyweight and completed at least one of the Big 4 lifts to view your strength standards.",
            style = MaterialTheme.typography.bodyMedium,
            color = GymCoachColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

private fun getTierDisplayInfo(tier: StrengthTier): Pair<String, Color> {
    return when (tier) {
        StrengthTier.UNTRAINED -> "Untrained" to GymCoachColors.TextMuted
        StrengthTier.NOVICE -> "Novice" to GymCoachColors.Success
        StrengthTier.INTERMEDIATE -> "Intermediate" to GymCoachColors.Primary
        StrengthTier.ADVANCED -> "Advanced" to GymCoachColors.CyanAccent
        StrengthTier.ELITE -> "Elite" to GymCoachColors.GoldAccent
    }
}
