package com.gymcoach.app.presentation.gamification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.core.gamification.*
import com.gymcoach.app.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakAndAchievementScreen(
    onBackClick: () -> Unit,
    viewModel: StreakAndAchievementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Streaks & Achievements", color = GymCoachColors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GymCoachColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GymCoachColors.PureDark
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
            when (val state = uiState) {
                is StreakUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = GymCoachColors.Primary
                    )
                }
                is StreakUiState.Empty -> {
                    Text(
                        text = "Complete your first workout to see streaks and achievements!",
                        color = GymCoachColors.TextSecondary,
                        modifier = Modifier.align(Alignment.Center).padding(GymCoachSpacing.lg),
                        textAlign = TextAlign.Center
                    )
                }
                is StreakUiState.Success -> {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(GymCoachSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.lg)
                    ) {
                        HeroCard(state.report)
                        LevelCard(state.report)
                        HeatMapSection(state.report)
                        AchievementsSection(state.report)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(report: StreakReport) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCardElevated),
        shape = GymCoachShapes.Card,
        modifier = Modifier.fillMaxWidth(),
        border = GymCoachBorders.subtleBorder()
    ) {
        Column(
            modifier = Modifier.padding(GymCoachSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm)
        ) {
            Text(
                text = "Current Streak",
                style = MaterialTheme.typography.titleMedium,
                color = GymCoachColors.TextSecondary
            )
            Text(
                text = "${report.currentWeeklyStreak} Weeks Active \uD83D\uDD25",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = GymCoachColors.TextPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Weekly Target: ${report.currentWeekWorkoutsCompleted}/${report.weeklyTarget}",
                    color = GymCoachColors.TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Longest: ${report.longestWeeklyStreak} Weeks",
                    color = GymCoachColors.TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun LevelCard(report: StreakReport) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
        shape = GymCoachShapes.Card,
        modifier = Modifier.fillMaxWidth(),
        border = GymCoachBorders.subtleBorder()
    ) {
        Column(
            modifier = Modifier.padding(GymCoachSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Level ${report.athleteLevel}: Athlete",
                    style = MaterialTheme.typography.titleLarge,
                    color = GymCoachColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${report.currentXp} / ${report.xpForNextLevel} XP",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GymCoachColors.PrimaryLight
                )
            }
            LinearProgressIndicator(
                progress = { report.currentXp.toFloat() / report.xpForNextLevel.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = GymCoachColors.Primary,
                trackColor = GymCoachColors.SurfaceInput,
            )
        }
    }
}

@Composable
private fun HeatMapSection(report: StreakReport) {
    Column(
        verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Consistency Heatmap",
            style = MaterialTheme.typography.titleMedium,
            color = GymCoachColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
            shape = GymCoachShapes.Card,
            modifier = Modifier.fillMaxWidth(),
            border = GymCoachBorders.subtleBorder()
        ) {
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(GymCoachSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Simplified heatmap rendering
                // Group by week, 7 days per column
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val today = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate()
                val oneYearAgo = today.minusDays(365)

                var current = oneYearAgo
                val days = mutableListOf<String>()
                while (!current.isAfter(today)) {
                    days.add(current.format(formatter))
                    current = current.plusDays(1)
                }

                val weeks = days.chunked(7)

                weeks.forEach { weekDays ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        weekDays.forEach { dayStr ->
                            val count = report.heatMapData[dayStr] ?: 0
                            val color = when {
                                count == 0 -> GymCoachColors.SurfaceInput
                                count == 1 -> Color(0xFF0F766E) // Dark Emerald
                                count == 2 -> Color(0xFF10B981) // Emerald
                                else -> GymCoachColors.CyanAccent
                            }
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(GymCoachShapes.xs)
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementsSection(report: StreakReport) {
    var selectedCategory by remember { mutableStateOf(BadgeCategory.ALL) }

    Column(
        verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.md),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Achievement Badges",
            style = MaterialTheme.typography.titleMedium,
            color = GymCoachColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )

        ScrollableTabRow(
            selectedTabIndex = BadgeCategory.entries.indexOf(selectedCategory),
            containerColor = Color.Transparent,
            contentColor = GymCoachColors.Primary,
            edgePadding = 0.dp,
            divider = {}
        ) {
            BadgeCategory.entries.forEach { category ->
                Tab(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    text = {
                        Text(
                            text = category.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (selectedCategory == category) GymCoachColors.TextPrimary else GymCoachColors.TextSecondary
                        )
                    }
                )
            }
        }

        val filteredBadges = report.badges.filter {
            selectedCategory == BadgeCategory.ALL || it.category == selectedCategory
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm),
            modifier = Modifier.heightIn(max = 1000.dp),
            userScrollEnabled = false
        ) {
            items(filteredBadges) { badge ->
                BadgeCard(badge)
            }
        }
    }
}

@Composable
private fun BadgeCard(badge: AchievementBadge) {
    val tierColor = when (badge.tier) {
        BadgeTier.BRONZE -> Color(0xFFCD7F32)
        BadgeTier.SILVER -> Color(0xFFC0C0C0)
        BadgeTier.GOLD -> Color(0xFFFFD700)
        BadgeTier.OBSIDIAN -> Color(0xFF8A2BE2)
    }

    val bgColor = if (badge.isUnlocked) GymCoachColors.SurfaceCardElevated else GymCoachColors.SurfaceCard
    val borderColor = if (badge.isUnlocked) tierColor.copy(alpha = 0.5f) else GymCoachColors.BorderSubtle
    val iconTint = if (badge.isUnlocked) tierColor else GymCoachColors.TextMuted

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = GymCoachShapes.Card,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, GymCoachShapes.Card)
    ) {
        Column(
            modifier = Modifier.padding(GymCoachSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm)
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = badge.title,
                tint = iconTint,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = badge.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (badge.isUnlocked) GymCoachColors.TextPrimary else GymCoachColors.TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = badge.description,
                style = MaterialTheme.typography.bodySmall,
                color = GymCoachColors.TextMuted,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            if (!badge.isUnlocked) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { if (badge.maxProgress > 0) badge.currentProgress.toFloat() / badge.maxProgress.toFloat() else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = GymCoachColors.Primary,
                    trackColor = GymCoachColors.SurfaceInput
                )
                Text(
                    text = "${badge.currentProgress}/${badge.maxProgress}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GymCoachColors.TextMuted
                )
            }
        }
    }
}
