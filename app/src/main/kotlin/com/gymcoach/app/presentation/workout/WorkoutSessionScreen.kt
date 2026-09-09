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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.core.timer.RestPresets
import com.gymcoach.app.data.local.dao.LastSetData
import com.gymcoach.app.presentation.history.formatDuration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionScreen(
    onBackClick: () -> Unit,
    workoutId: Long? = null,
    onViewHistory: () -> Unit = {},
    viewModel: WorkoutLoggingViewModel = hiltViewModel()
) {
    val sessionUiState by viewModel.sessionUiState.collectAsState()
    val currentWorkout by viewModel.currentWorkout.collectAsState()
    val showPicker by viewModel.showExercisePicker.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState()
    val completed by viewModel.completed.collectAsState()
    val error by viewModel.error.collectAsState()
    val restTimerState by viewModel.restTimerState.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val previousPerformance by viewModel.previousPerformance.collectAsState()
    val lastPerformanceSummary by viewModel.lastPerformanceSummary.collectAsState()
    val sessionVolume by viewModel.sessionVolume.collectAsState()
    val progressionRecommendations by viewModel.progressionRecommendations.collectAsState()
    val completionStats by viewModel.completionStats.collectAsState()
    var showFinishDialog by rememberSaveable { mutableStateOf(false) }
    var showLeaveDialog by rememberSaveable { mutableStateOf(false) }

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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = "Workout Complete!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(24.dp))

                // Completion statistics
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Session Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            CompletionStatItem(
                                label = "Duration",
                                value = formatDuration(completionStats.durationSeconds)
                            )
                            CompletionStatItem(
                                label = "Exercises",
                                value = "${completionStats.exerciseCount}"
                            )
                            CompletionStatItem(
                                label = "Sets",
                                value = "${completionStats.totalSets}"
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            CompletionStatItem(
                                label = "Reps",
                                value = "${completionStats.totalReps}"
                            )
                            CompletionStatItem(
                                label = "Volume",
                                value = "%.1f kg".format(completionStats.totalVolume)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(onClick = onBackClick, modifier = Modifier.heightIn(min = 48.dp).weight(1f)) {
                        Text("Go Back")
                    }
                    Button(onClick = onViewHistory, modifier = Modifier.heightIn(min = 48.dp).weight(1f)) {
                        Text("View History")
                    }
                }
            }
        }
        return
    }

    // Loading state
    if (sessionUiState is WorkoutLoggingViewModel.SessionUiState.Loading) {
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
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Loading workout...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    // Empty state
    if (sessionUiState is WorkoutLoggingViewModel.SessionUiState.Empty) {
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
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "No Active Workout",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Start a new workout from the home screen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = {
                        viewModel.startNewWorkout()
                    }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Start New Workout")
                    }
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
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = formatDuration(elapsedSeconds),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (sessionVolume > 0) {
                                Text(
                                    text = "${String.format("%.0f", sessionVolume)} kg·reps",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        val hasLoggedSets = currentWorkout?.exercises?.any { it.sets.isNotEmpty() } == true
                        if (hasLoggedSets) {
                            showLeaveDialog = true
                        } else {
                            onBackClick()
                        }
                    }) {
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
                    // Rest timer card with preset buttons
                    if (restTimerState.isRunning) {
                        item {
                            RestTimerCard(
                                timeRemaining = restTimerState.timeRemaining,
                                totalDuration = restTimerState.totalDuration,
                                isPaused = restTimerState.isPaused,
                                onPauseResume = {
                                    if (restTimerState.isPaused) viewModel.resumeRestTimer()
                                    else viewModel.pauseRestTimer()
                                },
                                onSkip = { viewModel.stopRestTimer() },
                                onPresetTap = { seconds -> viewModel.changeRestTimerDuration(seconds) }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }

                    workout.exercises.let { exercises ->
                        itemsIndexed(exercises, key = { _, ex -> ex.workoutExercise.id }) { exIdx, we ->
                            val lastSets = previousPerformance[we.exercise.id]
                            val lastPerf = lastPerformanceSummary[we.exercise.id]

                            ExerciseSetCard(
                                exerciseName = we.exercise.name,
                                muscleGroup = we.exercise.muscleGroup,
                                sets = we.sets,
                                previousSets = lastSets,
                                lastPerformance = lastPerf,
                                instructions = we.exercise.instructions,
                                recommendation = progressionRecommendations[we.exercise.id],
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
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
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
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
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

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Workout?") },
            text = { Text("Your progress is saved and you can resume this workout from History.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveDialog = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Leave Workout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Keep Training")
                }
            }
        )
    }

    if (showPicker) {
        val currentExerciseIds = currentWorkout?.exercises?.map { it.exercise.id }?.toSet() ?: emptySet()
        AlertDialog(
            onDismissRequest = { viewModel.hideExercisePicker() },
            title = { Text("Add Exercise") },
            text = {
                LazyColumn {
                    items(allExercises, key = { it.id }) { exercise ->
                        val isAlreadyAdded = exercise.id in currentExerciseIds
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (!isAlreadyAdded) viewModel.addExerciseToWorkout(exercise)
                            },
                            enabled = !isAlreadyAdded,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAlreadyAdded)
                                    MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f)
                                else
                                    MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = exercise.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (isAlreadyAdded) {
                                        Text(
                                            text = "Added",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
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

/**
 * Rest timer card with progress bar and quick-select preset buttons.
 */
@Composable
private fun RestTimerCard(
    timeRemaining: Int,
    totalDuration: Int,
    isPaused: Boolean,
    onPauseResume: () -> Unit,
    onSkip: () -> Unit,
    onPresetTap: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp, vertical = 8.dp),
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
                        text = "Rest: ${timeRemaining}s",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Row {
                    IconButton(onClick = onPauseResume) {
                        Icon(
                            if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Pause/Resume"
                        )
                    }
                    IconButton(onClick = onSkip) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Skip")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = {
                    if (totalDuration > 0) timeRemaining.toFloat() / totalDuration else 0f
                },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )

            // Quick-select rest duration presets
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Adjust rest:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(
                    "30s" to RestPresets.SHORT,
                    "60s" to RestPresets.MEDIUM,
                    "90s" to RestPresets.STANDARD,
                    "120s" to RestPresets.LONG,
                    "180s" to RestPresets.VERY_LONG
                )
                presets.forEach { (label, seconds) ->
                    FilterChip(
                        selected = totalDuration == seconds,
                        onClick = { onPresetTap(seconds) },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseSetCard(
    exerciseName: String,
    muscleGroup: String,
    sets: List<com.gymcoach.app.domain.model.WorkoutSet>,
    previousSets: List<LastSetData>?,
    lastPerformance: com.gymcoach.app.data.local.dao.LastPerformance?,
    instructions: String,
    recommendation: com.gymcoach.app.core.progression.ProgressionEngine.ProgressionRecommendation? = null,
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
    var showInstructions by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteSetIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var showRemoveExerciseDialog by rememberSaveable { mutableStateOf(false) }
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
                IconButton(onClick = { showRemoveExerciseDialog = true }) {
                    Icon(Icons.Default.Close, contentDescription = "Remove Exercise")
                }
            }

            // Progression recommendation banner
            if (recommendation != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Next session target",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${recommendation.recommendedWeight}kg × ${recommendation.recommendedReps} reps",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = recommendation.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Instructions
                        if (instructions.isNotEmpty()) {
                            TextButton(onClick = { showInstructions = !showInstructions }) {
                                Text(if (showInstructions) "Hide Instructions" else "Show Instructions")
                            }
                
                            if (showInstructions) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Instructions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(instructions, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
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
                            pendingDeleteSetIndex = index
                            false
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

    // Confirmation dialog for swipe-to-delete set
    if (pendingDeleteSetIndex != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteSetIndex = null },
            title = { Text("Delete Set") },
            text = { Text("Are you sure you want to delete this set? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveSet(pendingDeleteSetIndex!!)
                        pendingDeleteSetIndex = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteSetIndex = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation dialog for removing the whole exercise
    if (showRemoveExerciseDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveExerciseDialog = false },
            title = { Text("Remove Exercise") },
            text = { Text("Remove $exerciseName from this workout? All its sets will be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveExerciseDialog = false
                        onRemoveExercise()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveExerciseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
            com.gymcoach.app.domain.model.SetType.WARMUP -> MaterialTheme.colorScheme.tertiary
            com.gymcoach.app.domain.model.SetType.DROP -> MaterialTheme.colorScheme.secondary
            com.gymcoach.app.domain.model.SetType.FAILURE -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurface
        }
        // Set type chip (tappable to cycle: Normal → Warm → Drop → Fail → Normal)
        val setTypeLabel = when (setType) {
            com.gymcoach.app.domain.model.SetType.WARMUP -> "Warm"
            com.gymcoach.app.domain.model.SetType.DROP -> "Drop"
            com.gymcoach.app.domain.model.SetType.FAILURE -> "Fail"
            else -> "Set ${index + 1}"
        }
        val nextTypeLabel = when (setType) {
            com.gymcoach.app.domain.model.SetType.NORMAL -> "Warm"
            com.gymcoach.app.domain.model.SetType.WARMUP -> "Drop"
            com.gymcoach.app.domain.model.SetType.DROP -> "Fail"
            com.gymcoach.app.domain.model.SetType.FAILURE -> "Normal"
        }
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clickable {
                    val nextType = when (setType) {
                        com.gymcoach.app.domain.model.SetType.NORMAL -> com.gymcoach.app.domain.model.SetType.WARMUP
                        com.gymcoach.app.domain.model.SetType.WARMUP -> com.gymcoach.app.domain.model.SetType.DROP
                        com.gymcoach.app.domain.model.SetType.DROP -> com.gymcoach.app.domain.model.SetType.FAILURE
                        com.gymcoach.app.domain.model.SetType.FAILURE -> com.gymcoach.app.domain.model.SetType.NORMAL
                    }
                    onSetTypeChange(nextType)
                }
                .semantics {
                    role = Role.Button
                    stateDescription = "Set type: $setTypeLabel"
                    contentDescription = "Set type $setTypeLabel, tap to switch to $nextTypeLabel"
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(24.dp)
                    .border(
                        width = 1.dp,
                        color = setTypeColor.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = setTypeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = setTypeColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
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
                }
            )
        }
    }
}

@Composable
private fun CompletionStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
