package com.gymcoach.app.presentation.body

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.core.body.BodyMetricTrend
import com.gymcoach.app.core.body.BodyPart
import com.gymcoach.app.data.local.entity.BodyMeasurementEntity
import com.gymcoach.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyCompositionScreen(
    onBackClick: () -> Unit,
    viewModel: BodyCompositionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = GymCoachColors.PureDark,
        topBar = {
            TopAppBar(
                title = { Text("Body Composition", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showLogDialog = true }) {
                        Text("+ Log", color = GymCoachColors.Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GymCoachColors.PureDark,
                    titleContentColor = GymCoachColors.TextPrimary,
                    navigationIconContentColor = GymCoachColors.TextPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showLogDialog = true },
                containerColor = GymCoachColors.Primary,
                contentColor = GymCoachColors.TextPrimary
            ) {
                Text("+ Log New Measurement")
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is BodyCompositionUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is BodyCompositionUiState.Empty -> {
                    Text(
                        "No measurements yet.\nTap '+ Log' to start tracking.",
                        color = GymCoachColors.TextSecondary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is BodyCompositionUiState.Success -> {
                    BodyCompositionContent(
                        trend = state.trend,
                        onDelete = { viewModel.deleteMeasurement(it) }
                    )
                }
            }
        }
    }

    if (showLogDialog) {
        LogMeasurementDialog(
            onDismiss = { showLogDialog = false },
            onSave = { weight, fat, chest, waist, shoulders, arms, thighs, calves, notes ->
                viewModel.saveMeasurement(weight, fat, chest, waist, shoulders, arms, thighs, calves, notes)
                showLogDialog = false
            }
        )
    }
}

@Composable
fun BodyCompositionContent(
    trend: BodyMetricTrend,
    onDelete: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(GymCoachSpacing.lg)
    ) {
        item {
            HeroCard(trend)
            Spacer(modifier = Modifier.height(GymCoachSpacing.xl))
        }

        item {
            Text(
                "CIRCUMFERENCES",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = GymCoachColors.Primary,
                modifier = Modifier.padding(bottom = GymCoachSpacing.sm)
            )
            CircumferenceGrid(trend)
            Spacer(modifier = Modifier.height(GymCoachSpacing.xl))
        }

        item {
            Text(
                "HISTORY",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = GymCoachColors.Primary,
                modifier = Modifier.padding(bottom = GymCoachSpacing.sm)
            )
        }

        items(trend.history) { entity ->
            HistoryCard(entity, onDelete)
            Spacer(modifier = Modifier.height(GymCoachSpacing.md))
        }

        item {
            Spacer(modifier = Modifier.height(80.dp)) // FAB spacing
        }
    }
}

@Composable
fun HeroCard(trend: BodyMetricTrend) {
    Card(
        shape = GymCoachShapes.Card,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCardElevated),
        border = GymCoachBorders.subtleBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(GymCoachSpacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Current Weight", color = GymCoachColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${trend.currentWeightKg}", color = GymCoachColors.TextPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(" kg", color = GymCoachColors.TextSecondary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 2.dp))
                    }
                }

                if (trend.weeklyRateKg != 0.0) {
                    val isGain = trend.weeklyRateKg > 0
                    val color = if (isGain) GymCoachColors.PrimaryLight else GymCoachColors.Success
                    val sign = if (isGain) "+" else ""
                    val formattedRate = String.format(Locale.US, "%.1f", trend.weeklyRateKg)
                    Surface(
                        color = color.copy(alpha = 0.2f),
                        shape = GymCoachShapes.pill,
                    ) {
                        Text("$sign$formattedRate kg/wk", color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(GymCoachSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (trend.currentBodyFatPct != null) {
                    Column {
                        Text("Body Fat", color = GymCoachColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text("${trend.currentBodyFatPct}%", color = GymCoachColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
                    }
                }

                if (trend.vTaperRatio != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("V-Taper", color = GymCoachColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(String.format(Locale.US, "%.2f", trend.vTaperRatio), color = GymCoachColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.width(4.dp))
                            Surface(
                                color = GymCoachColors.GoldAccent.copy(alpha = 0.2f),
                                shape = GymCoachShapes.pill
                            ) {
                                Text(trend.vTaperCategory, color = GymCoachColors.GoldAccent, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            if (trend.vTaperRatio != null) {
                Spacer(modifier = Modifier.height(GymCoachSpacing.md))
                Text("Golden Ratio Proximity", color = GymCoachColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { trend.goldenRatioProximityPct },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = GymCoachColors.GoldAccent,
                    trackColor = GymCoachColors.SurfaceInput
                )
            }
        }
    }
}

@Composable
fun CircumferenceGrid(trend: BodyMetricTrend) {
    val parts = listOf(BodyPart.CHEST, BodyPart.SHOULDERS, BodyPart.WAIST, BodyPart.ARMS, BodyPart.THIGHS, BodyPart.CALVES)

    Column(verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm)) {
        for (i in parts.indices step 2) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm)) {
                val p1 = parts[i]
                MetricCard(p1, trend.circumferences[p1], Modifier.weight(1f))

                if (i + 1 < parts.size) {
                    val p2 = parts[i + 1]
                    MetricCard(p2, trend.circumferences[p2], Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun MetricCard(part: BodyPart, delta: com.gymcoach.app.core.body.MetricDelta?, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = GymCoachShapes.md,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
        border = GymCoachBorders.subtleBorder()
    ) {
        Column(modifier = Modifier.padding(GymCoachSpacing.md)) {
            Text(part.name.lowercase(Locale.US).replaceFirstChar { it.titlecase(Locale.US) }, color = GymCoachColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val value = delta?.currentCm ?: 0.0
                Text(if (value > 0) String.format(Locale.US, "%.1f", value) else "--", color = GymCoachColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
                if (value > 0) {
                    Text(" cm", color = GymCoachColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.weight(1f))

                val d = delta?.deltaCm ?: 0.0
                if (d != 0.0) {
                    val isPos = d > 0
                    val color = if (part == BodyPart.WAIST) {
                         if (isPos) GymCoachColors.Warning else GymCoachColors.Success
                    } else {
                         if (isPos) GymCoachColors.Success else GymCoachColors.Warning
                    }
                    val sign = if (isPos) "+" else ""
                    Text(
                        "$sign${String.format(Locale.US, "%.1f", d)}",
                        color = color,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryCard(entity: BodyMeasurementEntity, onDelete: (Long) -> Unit) {
    val df = SimpleDateFormat("MMM d, yyyy", Locale.US)
    val dateStr = df.format(Date(entity.recordedAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = GymCoachShapes.sm,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
        border = GymCoachBorders.subtleBorder()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(GymCoachSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(dateStr, color = GymCoachColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("${entity.weightKg} kg", color = GymCoachColors.TextPrimary, style = MaterialTheme.typography.bodyLarge)
                    if (entity.bodyFatPct > 0) {
                        Text("${entity.bodyFatPct}% BF", color = GymCoachColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            IconButton(onClick = { onDelete(entity.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = GymCoachColors.Danger)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogMeasurementDialog(
    onDismiss: () -> Unit,
    onSave: (Double, Double, Double, Double, Double, Double, Double, Double, String) -> Unit
) {
    var weight by remember { mutableStateOf("") }
    var bf by remember { mutableStateOf("") }
    var chest by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    var shoulders by remember { mutableStateOf("") }
    var arms by remember { mutableStateOf("") }
    var thighs by remember { mutableStateOf("") }
    var calves by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GymCoachColors.SurfaceDeep
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text("Log Measurement", color = GymCoachColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight (kg)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = bf, onValueChange = { bf = it }, label = { Text("Body Fat (%)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = shoulders, onValueChange = { shoulders = it }, label = { Text("Shoulders (cm)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = chest, onValueChange = { chest = it }, label = { Text("Chest (cm)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = waist, onValueChange = { waist = it }, label = { Text("Waist (cm)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = arms, onValueChange = { arms = it }, label = { Text("Arms (cm)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = thighs, onValueChange = { thighs = it }, label = { Text("Thighs (cm)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = calves, onValueChange = { calves = it }, label = { Text("Calves (cm)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    onSave(
                        weight.toDoubleOrNull() ?: 0.0,
                        bf.toDoubleOrNull() ?: 0.0,
                        chest.toDoubleOrNull() ?: 0.0,
                        waist.toDoubleOrNull() ?: 0.0,
                        shoulders.toDoubleOrNull() ?: 0.0,
                        arms.toDoubleOrNull() ?: 0.0,
                        thighs.toDoubleOrNull() ?: 0.0,
                        calves.toDoubleOrNull() ?: 0.0,
                        notes
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GymCoachColors.Primary)
            ) {
                Text("Save Measurement")
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
