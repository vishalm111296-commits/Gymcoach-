package com.gymcoach.app.presentation.workout

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSummaryScreen(
    workoutId: Long,
    viewModel: WorkoutSummaryViewModel = hiltViewModel(),
    onFinish: () -> Unit
) {
    LaunchedEffect(workoutId) { viewModel.setWorkoutId(workoutId) }
    val metrics by viewModel.workoutSummary.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Summary") },
                modifier = Modifier.semantics {
                    contentDescription = "Workout summary screen"
                }
            )
        },
        bottomBar = {
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp)
            ) {
                Text("Finish", style = MaterialTheme.typography.titleMedium)
            }
        }
    ) { padding ->
        metrics?.let { m ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    MetricCard(label = "Duration", value = "${m.duration / 60} minutes", category = "Time")
                }
                item {
                    MetricCard(label = "Exercises", value = "${m.exerciseCount}", category = "Count")
                }
                item {
                    MetricCard(label = "Sets", value = "${m.totalSets}", category = "Count")
                }
                item {
                    MetricCard(label = "Reps", value = "${m.totalReps}", category = "Count")
                }
                item {
                    MetricCard(label = "Volume", value = "${m.totalVolume.toInt()} kg", category = "Weight")
                }
                item {
                    MetricCard(label = "Calories", value = "${m.calories} kcal", category = "Energy")
                }
                item {
                    MetricCard(label = "Max Weight", value = "${m.maxWeight} kg", category = "Weight")
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    category: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
