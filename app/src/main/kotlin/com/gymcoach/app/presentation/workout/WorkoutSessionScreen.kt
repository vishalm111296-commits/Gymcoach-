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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.material3.LinearProgressIndicator
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
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    var showFinishDialog by rememberSaveable { mutableStateOf(false) }

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
                title = { 
                    Column {
                        Text("Workout Session")
                        Text(
                            text = com.gymcoach.app.presentation.history.formatDuration(elapsedSeconds),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
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
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Rest")
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "Rest: ${restTimerState.timeRemaining}s",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        Row {
                                            IconButton(onClick = { 
                                                if (restTimerState.isPaused) viewModel.resumeRestTimer() 
                                                else viewModel.pauseRestTimer() 
                                            }) {
                                                Icon(if (restTimerState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = "Pause/Resume")
                                            }
                                            IconButton(onClick = { viewModel.stopRestTimer() }) {
                                                Icon(Icons.Default.SkipNext, contentDescription = "Skip")
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { 
                                            if (restTimerState.totalDuration > 0) 
                                                restTimerState.timeRemaining.toFloat() / restTimerState.totalDuration 
                                            else 0f 
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
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
                                onSetTypeChange = { setIdx, type -> viewModel.updateSetType(exIdx, setIdx, type) },
                                onToggleComplete = { setIdx -> viewModel.toggleSetCompletion(exIdx, setIdx) }
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = workout.workout.notes,
                                onValueChange = { newText: String -> viewModel.updateNotes(newText) },
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                label = { Text("Workout Notes") },
                                maxLines = 4
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
                    onClick = { showFinishDialog = true },
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

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Finish Workout") },
            text = { Text("Are you sure you are done? All completed sets will be saved.") },
            confirmButton = {
                Button(onClick = {
                    showFinishDialog = false
                    viewModel.completeWorkout()
                }) {
                    Text("Finish")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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

@OptIn(ExperimentalMaterial3Api::class)
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
    onSetTypeChange: (Int, com.gymcoach.app.domain.model.SetType) -> Unit,
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
                        onToggleComplete = { onToggleComplete(index) },
                        onRemoveSet = { onRemoveSet(index) }
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
private fun SetRow(
    index: Int,
    weight: Double,
    reps: Int,
    rpe: Double,
    restSeconds: Int,
    completed: Boolean,
    setType: com.gymcoach.app.domain.model.SetType,
    onRepsChange: (Int) -> Unit,
    onWeightChange: (Double) -> Unit,
    onRpeChange: (Double) -> Unit,
    onRestSecondsChange: (Int) -> Unit,
    onSetTypeChange: (com.gymcoach.app.domain.model.SetType) -> Unit,
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
        // Set Number & Type Indicator
        val setTypeColor = when (setType) {
            com.gymcoach.app.domain.model.SetType.WARMUP -> MaterialTheme.colorScheme.tertiary
            com.gymcoach.app.domain.model.SetType.DROP -> MaterialTheme.colorScheme.secondary
            com.gymcoach.app.domain.model.SetType.FAILURE -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurface
        }
        val setTypeText = when (setType) {
            com.gymcoach.app.domain.model.SetType.WARMUP -> "W"
            com.gymcoach.app.domain.model.SetType.DROP -> "D"
            com.gymcoach.app.domain.model.SetType.FAILURE -> "F"
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
                    // Cycle through set types
                    val nextType = when (setType) {
                        com.gymcoach.app.domain.model.SetType.NORMAL -> com.gymcoach.app.domain.model.SetType.WARMUP
                        com.gymcoach.app.domain.model.SetType.WARMUP -> com.gymcoach.app.domain.model.SetType.DROP
                        com.gymcoach.app.domain.model.SetType.DROP -> com.gymcoach.app.domain.model.SetType.FAILURE
                        com.gymcoach.app.domain.model.SetType.FAILURE -> com.gymcoach.app.domain.model.SetType.NORMAL
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