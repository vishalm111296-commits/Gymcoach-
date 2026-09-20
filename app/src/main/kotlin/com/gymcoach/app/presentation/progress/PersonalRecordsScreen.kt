package com.gymcoach.app.presentation.progress

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.data.local.entity.PersonalRecordWithExercise
import com.gymcoach.app.ui.theme.DarkBackground
import com.gymcoach.app.ui.theme.DarkSurface
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes
import com.gymcoach.app.ui.theme.TextPrimary
import com.gymcoach.app.ui.theme.TextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)

// Trophy medal colors
private val GoldColor = Color(0xFFFFB300)
private val SilverColor = Color(0xFFB0BEC5)
private val BronzeColor = Color(0xFFBF8640)
private val ElectricViolet = GymCoachColors.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalRecordsScreen(
    onBackClick: () -> Unit,
    viewModel: PersonalRecordsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Personal Records",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(Modifier.size(8.dp))
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = GoldColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Sort chips
            SortChipRow(
                selectedSort = sortBy,
                onSortSelected = viewModel::setSortBy,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when (val state = uiState) {
                is PersonalRecordsUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GoldColor)
                    }
                }

                is PersonalRecordsUiState.Empty -> {
                    EmptyState(modifier = Modifier.fillMaxSize())
                }

                is PersonalRecordsUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = state.message,
                            color = GymCoachColors.Danger,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                is PersonalRecordsUiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp,
                            top = 4.dp, bottom = 32.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(
                            items = state.records,
                            key = { _, record -> record.id }
                        ) { index, record ->
                            AnimatedPRCard(
                                record = record,
                                rank = index,
                                animationDelay = (index * 60).coerceAtMost(480)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortChipRow(
    selectedSort: SortBy,
    onSortSelected: (SortBy) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SortBy.values().forEach { sort ->
            val label = when (sort) {
                SortBy.RECENT -> "Recent"
                SortBy.WEIGHT -> "Heaviest"
                SortBy.EXERCISE_NAME -> "A-Z"
            }
            FilterChip(
                selected = selectedSort == sort,
                onClick = { onSortSelected(sort) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selectedSort == sort) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GoldColor.copy(alpha = 0.18f),
                    selectedLabelColor = GoldColor,
                    containerColor = DarkSurface,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedSort == sort,
                    selectedBorderColor = GoldColor.copy(alpha = 0.5f),
                    borderColor = GymCoachColors.BorderSubtle
                )
            )
        }
    }
}

@Composable
private fun AnimatedPRCard(
    record: PersonalRecordWithExercise,
    rank: Int,
    animationDelay: Int
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(record.id) {
        kotlinx.coroutines.delay(animationDelay.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(300)
        )
    ) {
        PRHallOfFameCard(record = record, rank = rank)
    }
}

@Composable
private fun PRHallOfFameCard(
    record: PersonalRecordWithExercise,
    rank: Int
) {
    val accentColor = when (rank) {
        0 -> GoldColor
        1 -> SilverColor
        2 -> BronzeColor
        else -> ElectricViolet
    }
    val trophyEmoji = when (rank) {
        0 -> "🥇"
        1 -> "🥈"
        2 -> "🥉"
        else -> "🏆"
    }

    val dateStr = remember(record.achievedAt) {
        Instant.ofEpochMilli(record.achievedAt)
            .atZone(ZoneId.systemDefault())
            .format(DATE_FORMAT)
    }

    // Use stored 1RM or calculate via Epley formula
    val displayOneRepMax = if (record.oneRepMaxKg > 0.0) {
        record.oneRepMaxKg
    } else {
        record.weightKg * (1.0 + record.reps.coerceAtMost(12) / 30.0)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = GymCoachShapes.md,
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = accentColor.copy(alpha = 0.30f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Trophy / medal badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = accentColor.copy(alpha = 0.12f),
                        shape = GymCoachShapes.sm
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = trophyEmoji,
                    fontSize = 22.sp
                )
            }

            // Exercise info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.exerciseName.ifBlank { "Unknown Exercise" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                val weightStr = if (record.weightKg == record.weightKg.toLong().toDouble()) {
                    "${record.weightKg.toInt()} kg"
                } else {
                    String.format(Locale.US, "%.1f kg", record.weightKg)
                }
                Text(
                    text = "$weightStr × ${record.reps} reps",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Est. 1RM: ${String.format(Locale.US, "%.1f", displayOneRepMax)} kg  •  $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = GoldColor.copy(alpha = 0.6f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Your Hall of Fame awaits!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Start lifting to set your first records!",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
