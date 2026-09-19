package com.gymcoach.app.presentation.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.core.analytics.*
import com.gymcoach.app.data.local.entity.BodyMeasurementEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VTaperTransformationScreen(
    onBackClick: () -> Unit,
    viewModel: VTaperTransformationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("V-Taper & Body Ratios") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is VTaperUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is VTaperUiState.Empty -> {
                    Text(
                        text = "No measurements recorded yet.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is VTaperUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is VTaperUiState.Success -> {
                    VTaperReportContent(
                        report = state.report,
                        onDeleteMeasurement = viewModel::deleteMeasurement
                    )
                }
            }
        }
    }
}

@Composable
fun VTaperReportContent(
    report: VTaperReport,
    onDeleteMeasurement: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeroCard(adonisIndex = report.adonisIndex, latest = report.latestMeasurement)
        }

        if (report.recompDelta != null) {
            item {
                Text(
                    text = "${report.recompDelta.daysPeriod}-Day Delta Recomp",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                RecompDeltaSection(delta = report.recompDelta)
            }
        }

        if (report.limbSymmetries.isNotEmpty()) {
            item {
                Text(
                    text = "Bilateral Limb Symmetry",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            items(report.limbSymmetries) { symmetry ->
                LimbSymmetryCard(symmetry = symmetry)
            }
        }

        if (report.history.isNotEmpty()) {
            item {
                Text(
                    text = "Measurement History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            items(report.history) { measurement ->
                MeasurementHistoryCard(measurement = measurement, onDelete = { onDeleteMeasurement(measurement.id) })
            }
        }
    }
}

@Composable
fun HeroCard(adonisIndex: AdonisIndex, latest: BodyMeasurementEntity?) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Adonis Index",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Simple representation of the dial
            Box(
                modifier = Modifier
                    .size(120.dp)
            ) {
                CircularProgressIndicator(
                    progress = { adonisIndex.progressPct / 100f },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 12.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = String.format(Locale.US, "%.2f", adonisIndex.currentRatio),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "/ ${adonisIndex.targetRatio}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${adonisIndex.statusSummary} · ${adonisIndex.progressPct}% to Target",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            if (latest != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatBox("Shoulders", "${latest.shouldersCm}cm")
                    StatBox("Waist", "${latest.waistCm}cm")
                    StatBox("Chest", "${latest.chestCm}cm")
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RecompDeltaSection(delta: RecompDelta) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DeltaCard(
            modifier = Modifier.weight(1f),
            label = "Waist",
            deltaValue = delta.waistDeltaCm,
            unit = "cm",
            invertGood = true // negative is good for waist
        )
        DeltaCard(
            modifier = Modifier.weight(1f),
            label = "Shoulders",
            deltaValue = delta.shoulderDeltaCm,
            unit = "cm"
        )
        DeltaCard(
            modifier = Modifier.weight(1f),
            label = "Weight",
            deltaValue = delta.weightDeltaKg,
            unit = "kg"
        )
    }
}

@Composable
fun DeltaCard(
    modifier: Modifier = Modifier,
    label: String,
    deltaValue: Double,
    unit: String,
    invertGood: Boolean = false
) {
    val isGood = if (invertGood) deltaValue <= 0 else deltaValue >= 0
    val color = if (isGood) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val sign = if (deltaValue > 0) "+" else ""

    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$sign${String.format(Locale.US, "%.1f", deltaValue)}$unit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun LimbSymmetryCard(symmetry: LimbSymmetry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = symmetry.limbName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (symmetry.isBalanced) "Balanced" else "Imbalance (${String.format(Locale.US, "%.1f", symmetry.deltaCm)}cm)",
                    color = if (symmetry.isBalanced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("L: ${symmetry.leftCm}cm")
                Text("R: ${symmetry.rightCm}cm")
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (symmetry.symmetryPct / 100.0).toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun MeasurementHistoryCard(measurement: BodyMeasurementEntity, onDelete: () -> Unit) {
    val date = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(measurement.recordedAt))

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = date, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "W: ${measurement.weightKg}kg | Waist: ${measurement.waistCm}cm | Shoulders: ${measurement.shouldersCm}cm",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = onDelete) {
                Text("Delete")
            }
        }
    }
}
