package com.gymcoach.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun RestTimerDisplay(
    timeRemaining: Int,
    totalDuration: Int,
    isPaused: Boolean,
    onPlayPause: () -> Unit,
    onSkip: () -> Unit,
    onAdd30: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${timeRemaining}s",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        if (isPaused) {
                            Icons.Default.PlayArrow
                        } else {
                            Icons.Default.Pause
                        },
                        contentDescription = if (isPaused) "Resume timer" else "Pause timer"
                    )
                }
                IconButton(
                    onClick = onAdd30,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add 30 seconds"
                    )
                }
                IconButton(
                    onClick = onSkip,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Skip rest"
                    )
                }
            }
        }
    }
}

@Composable
fun WorkoutNotesDialog(
    onDismiss: () -> Unit,
    onConfirm: (notes: String, mood: Int, energy: Int, pain: Int) -> Unit,
    initialNotes: String = "",
    initialMood: Int = 0,
    initialEnergy: Int = 0,
    initialPain: Int = 0
) {
    var notes by rememberSaveable { mutableStateOf(initialNotes) }
    var mood by rememberSaveable { mutableIntStateOf(initialMood) }
    var energy by rememberSaveable { mutableIntStateOf(initialEnergy) }
    var pain by rememberSaveable { mutableIntStateOf(initialPain) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Workout Notes") },
        text = {
            Column(Modifier.width(280.dp)) {
                OutlinedTextField(
                    label = { Text("Notes") },
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mood:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(8.dp))
                    Slider(
                        value = mood.toFloat(),
                        onValueChange = { mood = it.roundToInt() },
                        valueRange = 0f..5f,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${mood}/5", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Energy:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(8.dp))
                    Slider(
                        value = energy.toFloat(),
                        onValueChange = { energy = it.roundToInt() },
                        valueRange = 0f..5f,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${energy}/5", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pain:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(8.dp))
                    Slider(
                        value = pain.toFloat(),
                        onValueChange = { pain = it.roundToInt() },
                        valueRange = 0f..5f,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${pain}/5", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(notes, mood, energy, pain) },
                enabled = notes.isNotBlank()
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