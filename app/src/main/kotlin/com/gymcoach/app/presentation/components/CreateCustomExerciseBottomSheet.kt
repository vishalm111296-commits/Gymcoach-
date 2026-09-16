package com.gymcoach.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val VoltAccent = Color(0xFFD4FF32)
private val DarkVoltText = Color(0xFF121316)

/**
 * Obsidian Volt styled Bottom Sheet for creating a custom exercise.
 * Designed to seamlessly blend into GymCoach's dark-mode-first aesthetic.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateCustomExerciseBottomSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, muscleGroup: String, equipment: String, difficulty: String, notes: String) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    initialMuscleGroup: String = "Chest",
    initialEquipment: String = "Dumbbell"
) {
    var name by rememberSaveable { mutableStateOf("") }
    var muscleGroup by rememberSaveable { mutableStateOf(initialMuscleGroup) }
    var equipment by rememberSaveable { mutableStateOf(initialEquipment) }
    var difficulty by rememberSaveable { mutableStateOf("Intermediate") }
    var notes by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val muscleOptions = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Full Body")
    val equipmentOptions = listOf("Dumbbell", "Barbell", "Cable", "Bodyweight", "Machine", "Kettlebell", "Band")
    val difficultyOptions = listOf("Beginner", "Intermediate", "Advanced")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Create Custom Exercise",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Add to your personal adaptive library",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Exercise Name Field
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (errorMessage != null) errorMessage = null
                },
                label = { Text("Exercise Name *") },
                placeholder = { Text("e.g., Swiss Ball Hex Press") },
                singleLine = true,
                isError = errorMessage != null,
                supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VoltAccent,
                    focusedLabelColor = VoltAccent
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Target Muscle Group
            Text(
                text = "Target Muscle Group",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                muscleOptions.forEach { option ->
                    val isSelected = muscleGroup.equals(option, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { muscleGroup = option },
                        label = { Text(option) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VoltAccent.copy(alpha = 0.2f),
                            selectedLabelColor = VoltAccent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Equipment Type
            Text(
                text = "Equipment",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                equipmentOptions.forEach { option ->
                    val isSelected = equipment.equals(option, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { equipment = option },
                        label = { Text(option) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VoltAccent.copy(alpha = 0.2f),
                            selectedLabelColor = VoltAccent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Difficulty Level
            Text(
                text = "Difficulty",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                difficultyOptions.forEach { option ->
                    val isSelected = difficulty.equals(option, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { difficulty = option },
                        label = { Text(option) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VoltAccent.copy(alpha = 0.2f),
                            selectedLabelColor = VoltAccent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes / Form Cues
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Setup Notes & Execution Cues (Optional)") },
                placeholder = { Text("e.g., Focus on 2-second eccentric phase and full lock out...") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VoltAccent,
                    focusedLabelColor = VoltAccent
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val trimmedName = name.trim()
                        if (trimmedName.isBlank()) {
                            errorMessage = "Please enter an exercise name"
                        } else {
                            onSave(trimmedName, muscleGroup, equipment, difficulty, notes.trim())
                        }
                    },
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VoltAccent,
                        contentColor = DarkVoltText
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "Save Exercise",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
