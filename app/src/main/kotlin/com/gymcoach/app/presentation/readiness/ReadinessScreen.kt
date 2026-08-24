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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.data.local.entity.ReadinessEntity
import com.gymcoach.app.ui.theme.AccentBlue
import com.gymcoach.app.ui.theme.DarkBackground
import com.gymcoach.app.ui.theme.DarkSurface
import com.gymcoach.app.ui.theme.WarmWhite
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
        topBar = {
            TopAppBar(
                title = { Text("Recovery & Readiness") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showLogDialog() },
                containerColor = AccentBlue
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
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text(
                                    text = "NO READINESS DATA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentBlue,
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
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "TRAINING RECOMMENDATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentBlue,
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
                            color = AccentBlue,
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
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "READINESS SCORE",
                style = MaterialTheme.typography.labelSmall,
                color = AccentBlue,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "%.1f / 5.0".format(readiness.readinessScore),
                style = MaterialTheme.typography.displaySmall,
                color = WarmWhite,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            
            // Metric breakdown
            MetricRow(icon = Icons.Default.Bedtime, label = "Sleep", value = readiness.sleepQuality)
            MetricRow(icon = Icons.Default.FitnessCenter, label = "Soreness", value = readiness.soreness)
            MetricRow(icon = Icons.Default.Bolt, label = "Energy", value = readiness.energy)
            MetricRow(icon = Icons.Default.LocalFireDepartment, label = "Motivation", value = readiness.motivation)
        }
    }
}

@Composable
private fun MetricRow(icon: ImageVector, label: String, value: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = AccentBlue,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "$value / 5",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = WarmWhite
        )
    }
}

@Composable
private fun ReadinessHistoryItem(readiness: ReadinessEntity) {
    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                color = AccentBlue,
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
                thumbColor = AccentBlue,
                activeTrackColor = AccentBlue
            )
        )
    }
}
