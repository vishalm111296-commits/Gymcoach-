package com.gymcoach.app.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.data.local.entity.BodyMeasurementEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showMeasurementDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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

                    // Profile section
                    SectionHeader("User Profile")
                    Spacer(Modifier.height(8.dp))

                    state.profile?.let { profile ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                InfoRow("Goal", profile.goal)
                                InfoRow("Experience", profile.experience)
                                InfoRow("Age", "${profile.age}")
                                InfoRow("Height", "%.0f cm".format(profile.heightCm))
                                InfoRow("Weight", "%.1f kg".format(profile.weightKg))
                                InfoRow("Training Days", "${profile.trainingDaysPerWeek}/week")
                                InfoRow("Session Length", "${profile.sessionLengthMinutes} min")
                                InfoRow("Equipment", profile.equipmentType)
                            }
                        }
                    } ?: run {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = "No profile set up yet. Complete onboarding first.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Body measurements section
                    SectionHeader("Body Measurements")
                    Spacer(Modifier.height(8.dp))

                    state.latestMeasurement?.let { m ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                InfoRow("Weight", "%.1f kg".format(m.weightKg))
                                m.bodyFatPct?.let { InfoRow("Body Fat", "%.1f%%".format(it)) }
                                m.chestCm?.let { InfoRow("Chest", "%.1f cm".format(it)) }
                                m.waistCm?.let { InfoRow("Waist", "%.1f cm".format(it)) }
                                m.hipsCm?.let { InfoRow("Hips", "%.1f cm".format(it)) }
                                m.shouldersCm?.let { InfoRow("Shoulders", "%.1f cm".format(it)) }
                                m.leftArmCm?.let { InfoRow("Left Arm", "%.1f cm".format(it)) }
                                m.rightArmCm?.let { InfoRow("Right Arm", "%.1f cm".format(it)) }
                                m.leftThighCm?.let { InfoRow("Left Thigh", "%.1f cm".format(it)) }
                                m.rightThighCm?.let { InfoRow("Right Thigh", "%.1f cm".format(it)) }
                                m.leftCalfCm?.let { InfoRow("Left Calf", "%.1f cm".format(it)) }
                                m.rightCalfCm?.let { InfoRow("Right Calf", "%.1f cm".format(it)) }
                                if (m.notes.isNotBlank()) InfoRow("Notes", m.notes)
                            }
                        }
                    } ?: run {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = "No measurements recorded yet.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { showMeasurementDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Log New Measurement")
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }

    if (showMeasurementDialog) {
        MeasurementInputDialog(
            onDismiss = { showMeasurementDialog = false },
            onSave = { measurement ->
                viewModel.saveMeasurement(measurement)
                showMeasurementDialog = false
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MeasurementInputDialog(
    onDismiss: () -> Unit,
    onSave: (BodyMeasurementEntity) -> Unit
) {
    var weightText by remember { mutableStateOf("") }
    var bodyFatText by remember { mutableStateOf("") }
    var chestText by remember { mutableStateOf("") }
    var waistText by remember { mutableStateOf("") }
    var hipsText by remember { mutableStateOf("") }
    var shouldersText by remember { mutableStateOf("") }
    var leftArmText by remember { mutableStateOf("") }
    var rightArmText by remember { mutableStateOf("") }
    var leftThighText by remember { mutableStateOf("") }
    var rightThighText by remember { mutableStateOf("") }
    var leftCalfText by remember { mutableStateOf("") }
    var rightCalfText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Measurement") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Weight (kg) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bodyFatText,
                    onValueChange = { bodyFatText = it },
                    label = { Text("Body Fat %") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = chestText,
                    onValueChange = { chestText = it },
                    label = { Text("Chest (cm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = waistText,
                    onValueChange = { waistText = it },
                    label = { Text("Waist (cm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hipsText,
                    onValueChange = { hipsText = it },
                    label = { Text("Hips (cm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = shouldersText,
                    onValueChange = { shouldersText = it },
                    label = { Text("Shoulders (cm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = leftArmText,
                    onValueChange = { leftArmText = it },
                    label = { Text("Left Arm (cm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rightArmText,
                    onValueChange = { rightArmText = it },
                    label = { Text("Right Arm (cm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = leftThighText,
                    onValueChange = { leftThighText = it },
                    label = { Text("Left Thigh (cm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rightThighText,
                    onValueChange = { rightThighText = it },
                    label = { Text("Right Thigh (cm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = leftCalfText,
                    onValueChange = { leftCalfText = it },
                    label = { Text("Left Calf (cm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rightCalfText,
                    onValueChange = { rightCalfText = it },
                    label = { Text("Right Calf (cm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val weight = weightText.toDoubleOrNull() ?: return@Button
                    if (weight <= 0) return@Button
                    onSave(
                        BodyMeasurementEntity(
                            weightKg = weight,
                            bodyFatPct = bodyFatText.toDoubleOrNull(),
                            chestCm = chestText.toDoubleOrNull(),
                            waistCm = waistText.toDoubleOrNull(),
                            hipsCm = hipsText.toDoubleOrNull(),
                            shouldersCm = shouldersText.toDoubleOrNull(),
                            leftArmCm = leftArmText.toDoubleOrNull(),
                            rightArmCm = rightArmText.toDoubleOrNull(),
                            leftThighCm = leftThighText.toDoubleOrNull(),
                            rightThighCm = rightThighText.toDoubleOrNull(),
                            leftCalfCm = leftCalfText.toDoubleOrNull(),
                            rightCalfCm = rightCalfText.toDoubleOrNull(),
                            notes = notesText
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
