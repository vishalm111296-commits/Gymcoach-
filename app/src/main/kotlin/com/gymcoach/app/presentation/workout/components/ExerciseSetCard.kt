package com.gymcoach.app.presentation.workout.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gymcoach.app.data.local.dao.LastPerformance
import com.gymcoach.app.data.local.dao.LastSetData
import com.gymcoach.app.domain.model.SetType
import com.gymcoach.app.domain.model.WorkoutSet
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSetCard(
    exerciseName: String,
    muscleGroup: String,
    sets: List<WorkoutSet>,
    previousSets: List<LastSetData>?,
    lastPerformance: LastPerformance?,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onRemoveExercise: () -> Unit,
    onRepsChange: (Int, Int) -> Unit,
    onWeightChange: (Int, Double) -> Unit,
    onRpeChange: (Int, Double) -> Unit,
    onRestSecondsChange: (Int, Int) -> Unit,
    onSetTypeChange: (Int, SetType) -> Unit,
    onToggleComplete: (Int) -> Unit
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = muscleGroup,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRemoveExercise) {
                    Icon(Icons.Default.Close, contentDescription = "Remove Exercise")
                }
            }

            // Previous performance indicator
            if (lastPerformance != null) {
                val lastDate = Instant.ofEpochMilli(lastPerformance.date)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("MMM d"))
                val bestWeight = lastPerformance.maxWeight
                val lastSetSummary = previousSets?.joinToString(", ") {
                    "${it.weight}kg x ${it.reps}"
                } ?: ""

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Last time ($lastDate)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        if (lastSetSummary.isNotEmpty()) {
                            Text(
                                text = lastSetSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Text(
                            text = "Best: ${bestWeight}kg",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Set labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Set", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.15f))
                Text("Weight", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.2f))
                Text("Reps", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.2f))
                Text("RPE", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.15f))
                Text("Rest(s)", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.15f))
                Spacer(modifier = Modifier.width(40.dp)) // Checkbox + Delete
            }

            sets.sortedBy { it.setNumber }.forEachIndexed { index, set ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = {
                        if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                            onRemoveSet(index)
                            true
                        } else false
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        val color = if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                ) {
                    SetRow(
                        index = index,
                        weight = set.weight,
                        reps = set.reps,
                        rpe = set.rpe,
                        restSeconds = set.restSeconds,
                        completed = set.completed,
                        setType = set.setType,
                        onRepsChange = { reps -> onRepsChange(index, reps) },
                        onWeightChange = { weight -> onWeightChange(index, weight) },
                        onRpeChange = { rpe -> onRpeChange(index, rpe) },
                        onRestSecondsChange = { rest -> onRestSecondsChange(index, rest) },
                        onSetTypeChange = { type -> onSetTypeChange(index, type) },
                        onToggleComplete = { onToggleComplete(index) }
                    )
                }
            }

            TextButton(onClick = onAddSet) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Set")
            }
        }
    }
}

@Composable
fun SetRow(
    index: Int,
    weight: Double,
    reps: Int,
    rpe: Double,
    restSeconds: Int,
    completed: Boolean,
    setType: SetType,
    onRepsChange: (Int) -> Unit,
    onWeightChange: (Double) -> Unit,
    onRpeChange: (Double) -> Unit,
    onRestSecondsChange: (Int) -> Unit,
    onSetTypeChange: (SetType) -> Unit,
    onToggleComplete: () -> Unit
) {
    var weightText by rememberSaveable { mutableStateOf(if (weight > 0) weight.toString() else "") }
    var repsText by rememberSaveable { mutableStateOf(if (reps > 0) reps.toString() else "") }
    var rpeText by rememberSaveable { mutableStateOf(if (rpe > 0) rpe.toString() else "") }
    var restText by rememberSaveable { mutableStateOf(if (restSeconds > 0) restSeconds.toString() else "") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set Number & Type Indicator
        val setTypeColor = when (setType) {
            SetType.WARMUP -> MaterialTheme.colorScheme.tertiary
            SetType.DROP -> MaterialTheme.colorScheme.secondary
            SetType.FAILURE -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurface
        }
        val setTypeText = when (setType) {
            SetType.WARMUP -> "W"
            SetType.DROP -> "D"
            SetType.FAILURE -> "F"
            else -> "${index + 1}"
        }
        Box(
            modifier = Modifier.width(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = setTypeText,
                style = MaterialTheme.typography.bodyMedium,
                color = setTypeColor,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedTextField(
            value = weightText,
            onValueChange = { v ->
                weightText = v
                v.toDoubleOrNull()?.let { onWeightChange(it) }
            },
            modifier = Modifier.weight(0.18f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = repsText,
            onValueChange = { v ->
                repsText = v
                v.toIntOrNull()?.let { onRepsChange(it) }
            },
            modifier = Modifier.weight(0.18f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = rpeText,
            onValueChange = { v ->
                rpeText = v
                v.toDoubleOrNull()?.let { onRpeChange(it) }
            },
            modifier = Modifier.weight(0.13f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = restText,
            onValueChange = { v ->
                restText = v
                v.toIntOrNull()?.let { onRestSecondsChange(it) }
            },
            modifier = Modifier.weight(0.13f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        Row(
            modifier = Modifier.width(56.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val haptic = LocalHapticFeedback.current
            Checkbox(
                checked = completed,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleComplete()
                },
                modifier = Modifier.size(24.dp)
            )
            IconButton(
                onClick = {
                    val nextType = when (setType) {
                        SetType.NORMAL -> SetType.WARMUP
                        SetType.WARMUP -> SetType.DROP
                        SetType.DROP -> SetType.FAILURE
                        SetType.FAILURE -> SetType.NORMAL
                    }
                    onSetTypeChange(nextType)
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Cycle Set Type",
                    modifier = Modifier.size(16.dp),
                    tint = setTypeColor
                )
            }
        }
    }
}
