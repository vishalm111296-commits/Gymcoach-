package com.gymcoach.app.presentation.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionScreen(
    onBackClick: () -> Unit,
    workoutId: Long? = null,
    viewModel: WorkoutLoggingViewModel = hiltViewModel()
) {
    val currentWorkout by viewModel.currentWorkout.collectAsState()
    val showPicker by viewModel.showExercisePicker.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState()
    val completed by viewModel.completed.collectAsState()
    val error by viewModel.error.collectAsState()
    val restTimerState by viewModel.restTimerState.collectAsState()

    val rememberRestTimer = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(restTimerState.isRunning) {
        rememberRestTimer.value = restTimerState.isRunning
    }

    LaunchedEffect(workoutId) {
        viewModel.loadOrStartWorkout(workoutId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.dismissError()
        }
    }

    if (error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text("Error") },
            text = { Text(error!!) },
            confirmButton = { Button(onClick = { viewModel.dismissError() }) { Text("OK") } }
        )
    }

    if (completed) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Workout Complete!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onBackClick) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Session") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                currentWorkout?.let { workout ->
                    if (restTimerState.isRunning) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 3.dp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        text = "${restTimerState.timeRemaining}s",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }

                    workout.exercises.let { exercises ->
                        itemsIndexed(exercises, key = { _, ex -> ex.workoutExercise.id }) { exIdx, we ->
                            ExerciseSetCard(
                                exerciseName = we.exercise.name,
                                muscleGroup = we.exercise.muscleGroup,
                                sets = we.sets,
                                onAddSet = { viewModel.addSet(exIdx) },
                                onRemoveSet = { setIdx -> viewModel.removeSet(exIdx, setIdx) },
                                onRemoveExercise = { viewModel.removeExercise(exIdx) },
                                onRepsChange = { setIdx, reps -> viewModel.updateSetReps(exIdx, setIdx, reps) },
                                onWeightChange = { setIdx, weight -> viewModel.updateSetWeight(exIdx, setIdx, weight) },
                                onRpeChange = { setIdx, rpe -> viewModel.updateSetRpe(exIdx, setIdx, rpe) },
                                onRestSecondsChange = { setIdx, rest -> viewModel.updateSetRestSeconds(exIdx, setIdx, rest) },
                                onToggleComplete = { setIdx -> viewModel.toggleSetCompletion(exIdx, setIdx) }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.showExercisePicker() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Exercise")
                }

                Button(
                    onClick = { viewModel.completeWorkout() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Complete Workout")
                }
            }
        }
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { viewModel.hideExercisePicker() },
            title = { Text("Add Exercise") },
            text = {
                LazyColumn {
                    items(items = allExercises, key = { it.id }) { exercise ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.addExerciseToWorkout(exercise) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = exercise.muscleGroup,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.hideExercisePicker() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ExerciseSetCard(
    exerciseName: String,
    muscleGroup: String,
    sets: List<com.gymcoach.app.domain.model.WorkoutSet>,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onRemoveExercise: () -> Unit,
    onRepsChange: (Int, Int) -> Unit,
    onWeightChange: (Int, Double) -> Unit,
    onRpeChange: (Int, Double) -> Unit,
    onRestSecondsChange: (Int, Int) -> Unit,
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
                SetRow(
                    index = index,
                    weight = set.weight,
                    reps = set.reps,
                    rpe = set.rpe,
                    restSeconds = set.restSeconds,
                    completed = set.completed,
                    onRepsChange = { reps -> onRepsChange(index, reps) },
                    onWeightChange = { weight -> onWeightChange(index, weight) },
                    onRpeChange = { rpe -> onRpeChange(index, rpe) },
                    onRestSecondsChange = { rest -> onRestSecondsChange(index, rest) },
                    onToggleComplete = { onToggleComplete(index) },
                    onRemoveSet = { onRemoveSet(index) }
                )
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
private fun SetRow(
    index: Int,
    weight: Double,
    reps: Int,
    rpe: Double,
    restSeconds: Int,
    completed: Boolean,
    onRepsChange: (Int) -> Unit,
    onWeightChange: (Double) -> Unit,
    onRpeChange: (Double) -> Unit,
    onRestSecondsChange: (Int) -> Unit,
    onToggleComplete: () -> Unit,
    onRemoveSet: () -> Unit
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
        Text(
            text = "${index + 1}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(20.dp),
            fontWeight = FontWeight.Medium
        )

        OutlinedTextField(
            value = weightText,
            onValueChange = { v ->
                weightText = v
                v.toDoubleOrNull()?.let { onWeightChange(it) }
            },
            modifier = Modifier.weight(0.2f),
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
            modifier = Modifier.weight(0.2f),
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
            modifier = Modifier.weight(0.15f),
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
            modifier = Modifier.weight(0.15f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        Row(
            modifier = Modifier.width(56.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = completed,
                onCheckedChange = { onToggleComplete() },
                modifier = Modifier.size(24.dp)
            )
            IconButton(
                onClick = onRemoveSet,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove Set",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}