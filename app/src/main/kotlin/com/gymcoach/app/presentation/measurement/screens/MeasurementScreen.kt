package com.gymcoach.app.presentation.measurement.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.text.input.ImeAction
import com.gymcoach.app.domain.vshape.model.MeasurementRecord
import com.gymcoach.app.domain.vshape.model.MeasurementType
import com.gymcoach.app.presentation.components.LoadingState
import com.gymcoach.app.presentation.components.ErrorState
import com.gymcoach.app.presentation.components.EmptyState
import com.gymcoach.app.presentation.measurement.screens.MeasurementViewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementScreen(
    onBackClick: () -> Unit,
    viewModel: MeasurementViewModel = hiltViewModel()
) {
    val measurements by viewModel.measurements.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf<MeasurementType?>(null) }
    var valueText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Body Measurements") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Measurement")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) {
                LoadingState(modifier = Modifier.fillMaxSize())
            } else if (error != null) {
                ErrorState(message = error ?: "", onRetry = { /* viewModel.refresh() */ }, modifier = Modifier.fillMaxSize())
            } else if (measurements.isEmpty()) {
                EmptyState(
                    message = "No measurements yet. Start tracking your body metrics!",
                    onPrimaryAction = { showAddDialog = true },
                    primaryActionLabel = "Add Measurement",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Current measurements overview
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Current Measurements", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        items(MeasurementType.values()) { type ->
                            val latest = measurements
                                .filter { it.measurementType == type }
                                .maxByOrNull { it.date.toEpochMilli() }
                            if (latest != null) {
                                MeasurementCard(
                                    record = latest,
                                    trend = null,
                                    onDelete = {}
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Measurement Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Measurement") },
            text = {
                Column(modifier = Modifier.padding(16.dp)) {
                    androidx.compose.material3.ExposedDropdownMenuBox(
                        expanded = selectedType != null,
                        onExpandedChange = { },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val selectedTypeText = selectedType?.displayName ?: "Select Measurement"
                        OutlinedTextField(
                            value = selectedTypeText,
                            onValueChange = { },
                            label = { Text("Measurement Type") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = selectedType != null)
                            },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = selectedType != null,
                            onDismissRequest = { },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (type in MeasurementType.values()) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(type.displayName) },
                                    onClick = { selectedType = type }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = valueText,
                        onValueChange = { valueText = it },
                        label = { Text("Value (${selectedType?.unit ?: "kg"})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Notes (optional)") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedType != null && valueText.isNotBlank()) {
                            val record = MeasurementRecord(
                                userId = "default_user",
                                measurementType = selectedType!!,
                                value = valueText.toDoubleOrNull() ?: 0.0,
                                unit = selectedType!!.unit,
                                date = java.time.Instant.now(),
                                notes = notesText.ifBlank { null },
                                createdAt = java.time.Instant.now()
                            )
                            // viewModel.addMeasurement(record)
                            showAddDialog = false
                            valueText = ""
                            notesText = ""
                            selectedType = null
                        }
                    },
                    enabled = selectedType != null && valueText.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Helper composable functions for this screen
@Composable
fun MeasurementCard(
    record: MeasurementRecord,
    trend: com.gymcoach.app.domain.repository.MeasurementTrend?,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.measurementType.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${record.value} ${record.unit}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                trend?.let { t ->
                    Row {
                        Icon(
                            imageVector = if (t.absoluteChange >= 0) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                            contentDescription = "Trend",
                            tint = if (t.absoluteChange >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f%%", t.percentChange),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (t.absoluteChange >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Delete")
                }
            }
        }
    }
}