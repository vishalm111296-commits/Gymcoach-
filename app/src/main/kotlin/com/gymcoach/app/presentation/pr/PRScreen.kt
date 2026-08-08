package com.gymcoach.app.presentation.pr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.presentation.components.EmptyState
import com.gymcoach.app.presentation.components.LoadingState
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PRScreen(
    onBackClick: () -> Unit,
    viewModel: PRViewModel = hiltViewModel()
) {
    val categories by viewModel.prCategories.collectAsState()
    val recentPrs by viewModel.recentPrs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personal Records") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val hasData = categories.highestWeight.value > 0.0

        if (!hasData) {
            EmptyState(
                message = "No personal records yet. Complete some workouts to see your PRs!",
                onPrimaryAction = onBackClick,
                primaryActionLabel = "Start Workout",
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Overall Records", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                }

                item {
                    PRCategoryCard(
                        title = "Heaviest Weight",
                        value = "${categories.highestWeight.value} kg",
                        subtitle = categories.highestWeight.exerciseName ?: "",
                        icon = Icons.Filled.FitnessCenter
                    )
                }
                item {
                    PRCategoryCard(
                        title = "Highest Volume",
                        value = "${categories.highestVolume.value.toInt()} kg",
                        subtitle = categories.highestVolume.exerciseName ?: "",
                        icon = Icons.Filled.TrendingUp
                    )
                }
                item {
                    PRCategoryCard(
                        title = "Most Reps",
                        value = "${categories.highestReps.value.toInt()} reps",
                        subtitle = categories.highestReps.exerciseName ?: "",
                        icon = Icons.Filled.EmojiEvents
                    )
                }
                item {
                    PRCategoryCard(
                        title = "Longest Workout",
                        value = "${(categories.longestWorkout.value / 60).toInt()} min",
                        subtitle = "",
                        icon = Icons.Filled.Timer
                    )
                }
                item {
                    PRCategoryCard(
                        title = "Most Exercises",
                        value = "${categories.mostExercises.value.toInt()}",
                        subtitle = "in a single workout",
                        icon = Icons.Filled.FitnessCenter
                    )
                }
                item {
                    PRCategoryCard(
                        title = "Most Sets",
                        value = "${categories.mostSets.value.toInt()}",
                        subtitle = "in a single workout",
                        icon = Icons.Filled.TrendingUp
                    )
                }

                if (recentPrs.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text("Recent PRs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                    }

                    items(recentPrs, key = { "${it.type}_${it.exerciseName}_${it.date}" }) { pr ->
                        val dateStr = DateTimeFormatter.ofPattern("MMM dd, yyyy")
                            .withZone(ZoneId.systemDefault())
                            .format(pr.date)
                        val typeLabel = when (pr.type) {
                            PRViewModel.PRType.HIGHEST_WEIGHT -> "Weight"
                            PRViewModel.PRType.HIGHEST_VOLUME -> "Volume"
                            PRViewModel.PRType.HIGHEST_REPS -> "Reps"
                            PRViewModel.PRType.LONGEST_WORKOUT -> "Duration"
                            PRViewModel.PRType.FASTEST_WORKOUT -> "Speed"
                            PRViewModel.PRType.MOST_EXERCISES -> "Exercises"
                            PRViewModel.PRType.MOST_SETS -> "Sets"
                            PRViewModel.PRType.MOST_CALORIES -> "Calories"
                        }
                        ListItem(
                            headlineContent = { Text("$typeLabel: ${pr.value}") },
                            supportingContent = { Text("${pr.exerciseName ?: "Workout"} · $dateStr") },
                            leadingContent = {
                                Icon(Icons.Filled.EmojiEvents, contentDescription = "PR", tint = MaterialTheme.colorScheme.primary)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PRCategoryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}