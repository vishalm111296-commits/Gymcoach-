package com.gymcoach.app.presentation.progress.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Dialog for logging a body measurement entry.
 * Pre-fills with the latest values so the user only needs to update what changed.
 */
@Composable
fun MeasurementLogDialog(
    latestWeight: Double?,
    latestWaist: Double?,
    latestChest: Double?,
    latestBodyFat: Double?,
    onDismiss: () -> Unit,
    onSave: (weightKg: Double, waistCm: Double?, chestCm: Double?, bodyFatPct: Double?, notes: String) -> Unit
) {
    var weight by remember { mutableStateOf(latestWeight?.let { "%.1f".format(it) } ?: "") }
    var waist by remember { mutableStateOf(latestWaist?.let { "%.1f".format(it) } ?: "") }
    var chest by remember { mutableStateOf(latestChest?.let { "%.1f".format(it) } ?: "") }
    var bodyFat by remember { mutableStateOf(latestBodyFat?.let { "%.1f".format(it) } ?: "") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Measurement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Bodyweight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = waist,
                    onValueChange = { waist = it },
                    label = { Text("Waist (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = chest,
                    onValueChange = { chest = it },
                    label = { Text("Chest (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bodyFat,
                    onValueChange = { bodyFat = it },
                    label = { Text("Body Fat % (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val w = weight.toDoubleOrNull() ?: 0.0
                    val waistVal = waist.toDoubleOrNull()
                    val chestVal = chest.toDoubleOrNull()
                    val bf = bodyFat.toDoubleOrNull()
                    onSave(w, waistVal, chestVal, bf, notes)
                },
                enabled = weight.toDoubleOrNull() != null && (weight.toDoubleOrNull() ?: 0.0) > 0
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
