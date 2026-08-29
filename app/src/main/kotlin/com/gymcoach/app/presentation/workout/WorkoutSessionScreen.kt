package com.gymcoach.app.presentation.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.core.timer.RestPresets
import com.gymcoach.app.data.local.dao.LastSetData
import com.gymcoach.app.presentation.components.FrictionlessNumericInput
import com.gymcoach.app.presentation.components.PrimaryActionButton
import com.gymcoach.app.presentation.workout.components.LastSessionData
import com.gymcoach.app.presentation.workout.components.PreviousPerformanceRow
import com.gymcoach.app.presentation.workout.components.SetData
import com.gymcoach.app.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.gymcoach.app.presentation.history.formatDuration

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
    val previousPerformance by viewModel.previousPerformance.collectAsState()
    val lastPerformanceSummary by viewModel.lastPerformanceSummary.collectAsState()
    val sessionVolume by viewModel.sessionVolume.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()

    var showFinishDialog by rememberSaveable { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(workoutId) {
        viewModel.loadOrStartWorkout(workoutId)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.dismissError() }
    }

    if (error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text("Error") },
            text = { Text(error!!) },
            confirmButton = { Button(onClick = { viewModel.dismissError() }) { Text("OK") } }
        )
    }

    val workout = currentWorkout
    if (completed) {
        WorkoutCompletionScreen(
            onDone = onBackClick,
            volume = sessionVolume,
            durationSeconds = elapsedSeconds,
            exerciseCount = workout?.exercises?.size ?: 0
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Session", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showFinishDialog = true }) {
                        Text("FINISH", fontWeight = FontWeight.Bold, color = AccentBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = DarkBackground,
        bottomBar = {
            if (restTimerState.isRunning) {
                PremiumRestTimerBar(
                    timeRemaining = restTimerState.timeRemaining,
                    totalDuration = restTimerState.totalDuration,
                    isPaused = restTimerState.isPaused,
                    onPauseResume = { if (restTimerState.isPaused) viewModel.resumeRestTimer() else viewModel.pauseRestTimer() },
                    onSkip = { viewModel.stopRestTimer() }
                )
            }
        }
    ) { padding ->
        if (workout == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 120.dp) // Space for timer/FABs
        ) {
            itemsIndexed(workout.exercises) { exIdx, we ->
                val lastPerf = lastPerformanceSummary[we.exercise.id]
                val lastSets = previousPerformance[we.exercise.id]

                PremiumExerciseCard(
                    exerciseName = we.exercise.name,
                    muscleGroup = we.exercise.muscleGroup,
                    sets = we.sets,
                    previousSets = lastSets,
                    lastPerformance = lastPerf,
                    onAddSet = { viewModel.addSet(exIdx) },
                    onRemoveSet = { setIdx -> viewModel.removeSet(exIdx, setIdx) },
                    onRemoveExercise = { viewModel.removeExercise(exIdx) },
                    onRepsChange = { setIdx, reps -> viewModel.updateSetReps(exIdx, setIdx, reps) },
                    onWeightChange = { setIdx, weight -> viewModel.updateSetWeight(exIdx, setIdx, weight) },
                    onRpeChange = { setIdx, rpe -> viewModel.updateSetRpe(exIdx, setIdx, rpe) },
                    onToggleComplete = { setIdx ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleSetCompletion(exIdx, setIdx)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                PrimaryActionButton(
                    text = "+ Add Exercise",
                    onClick = { viewModel.showExercisePicker() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = workout.workout.notes,
                    onValueChange = { viewModel.updateNotes(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    label = { Text("Workout Notes", color = TextSecondary) },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = DarkSurface,
                        focusedContainerColor = DarkSurface,
                        unfocusedTextColor = TextPrimary,
                        focusedTextColor = TextPrimary
                    ),
                    maxLines = 3
                )
            }
        }
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Finish Workout") },
            text = { Text("Are you sure you are done? All completed sets will be saved.") },
            containerColor = DarkSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            confirmButton = {
                Button(
                    onClick = {
                        showFinishDialog = false
                        viewModel.completeWorkout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Finish")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showPicker) {
        // Exercise picker logic
        AlertDialog(
            onDismissRequest = { viewModel.hideExercisePicker() },
            title = { Text("Add Exercise") },
            containerColor = DarkSurface,
            titleContentColor = TextPrimary,
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    itemsIndexed(allExercises) { _, exercise ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.addExerciseToWorkout(exercise) }
                                .padding(vertical = 12.dp)
                        ) {
                            Column {
                                Text(exercise.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(exercise.muscleGroup, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.hideExercisePicker() }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun PremiumExerciseCard(
    exerciseName: String,
    muscleGroup: String,
    sets: List<com.gymcoach.app.domain.model.WorkoutSet>,
    previousSets: List<LastSetData>?,
    lastPerformance: com.gymcoach.app.data.local.dao.LastPerformance?,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onRemoveExercise: () -> Unit,
    onRepsChange: (Int, Int) -> Unit,
    onWeightChange: (Int, Double) -> Unit,
    onRpeChange: (Int, Double) -> Unit,
    onToggleComplete: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = exerciseName.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = muscleGroup,
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentBlueLight,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onRemoveExercise) {
                    Icon(Icons.Default.Close, contentDescription = "Remove Exercise", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Previous Performance Context
            val lastSessionData = if (lastPerformance != null && previousSets != null) {
                val lastDate = Instant.ofEpochMilli(lastPerformance.date)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("MMM d"))
                val setData = previousSets.map { SetData(it.weight, it.reps) }
                LastSessionData(lastDate, setData)
            } else null

            PreviousPerformanceRow(lastSession = lastSessionData)

            Spacer(modifier = Modifier.height(16.dp))

            // Set Headers
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("SET", style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.weight(0.15f))
                Text("KG", style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.weight(0.25f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("REPS", style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.weight(0.25f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("RPE", style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.weight(0.2f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.weight(0.15f)) // Checkbox space
            }

            // Sets
            sets.sortedBy { it.setNumber }.forEachIndexed { index, set ->
                SetLoggingRow(
                    index = index,
                    weight = set.weight,
                    reps = set.reps,
                    rpe = set.rpe,
                    completed = set.completed,
                    onRepsChange = { onRepsChange(index, it) },
                    onWeightChange = { onWeightChange(index, it) },
                    onRpeChange = { onRpeChange(index, it) },
                    onToggleComplete = { onToggleComplete(index) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            TextButton(onClick = onAddSet, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("+ Add Set", color = TextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SetLoggingRow(
    index: Int,
    weight: Double,
    reps: Int,
    rpe: Double,
    completed: Boolean,
    onRepsChange: (Int) -> Unit,
    onWeightChange: (Double) -> Unit,
    onRpeChange: (Double) -> Unit,
    onToggleComplete: () -> Unit
) {
    val weightText = if (weight > 0) weight.toString() else ""
    val repsText = if (reps > 0) reps.toString() else ""
    val rpeText = if (rpe > 0) rpe.toString() else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (completed) SuccessGreen.copy(alpha = 0.1f) else DarkBackground)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set Number
        Box(modifier = Modifier.weight(0.15f), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (completed) SuccessGreen else TextSecondary
            )
        }

        // Inputs
        FrictionlessNumericInput(
            value = weightText,
            onValueChange = { it.toDoubleOrNull()?.let(onWeightChange) },
            modifier = Modifier.weight(0.25f),
            isHighlight = !completed
        )
        Spacer(modifier = Modifier.width(8.dp))
        FrictionlessNumericInput(
            value = repsText,
            onValueChange = { it.toIntOrNull()?.let(onRepsChange) },
            modifier = Modifier.weight(0.25f),
            isHighlight = !completed
        )
        Spacer(modifier = Modifier.width(8.dp))
        FrictionlessNumericInput(
            value = rpeText,
            onValueChange = { it.toDoubleOrNull()?.let(onRpeChange) },
            modifier = Modifier.weight(0.2f),
            placeholder = "RPE",
            isHighlight = false
        )

        // Checkbox Action
        Box(
            modifier = Modifier
                .weight(0.15f)
                .padding(start = 8.dp)
                .clickable { onToggleComplete() },
            contentAlignment = Alignment.Center
        ) {
            if (completed) {
                Icon(Icons.Default.Check, contentDescription = "Completed", tint = SuccessGreen)
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurfaceVariant)
                )
            }
        }
    }
}

@Composable
private fun PremiumRestTimerBar(
    timeRemaining: Int,
    totalDuration: Int,
    isPaused: Boolean,
    onPauseResume: () -> Unit,
    onSkip: () -> Unit
) {
    Surface(
        color = RestTimerBg,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            LinearProgressIndicator(
                progress = { if (totalDuration > 0) timeRemaining.toFloat() / totalDuration else 0f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = AccentBlue,
                trackColor = DarkSurface
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = "Rest", tint = AccentBlueLight, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${timeRemaining}s REST",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(
                        onClick = onPauseResume,
                        modifier = Modifier.background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Pause/Resume",
                            tint = TextPrimary
                        )
                    }
                    IconButton(
                        onClick = onSkip,
                        modifier = Modifier.background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Skip", tint = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutCompletionScreen(
    onDone: () -> Unit,
    volume: Double,
    durationSeconds: Long,
    exerciseCount: Int
) {
    Box(
        modifier = Modifier.fillMaxSize().background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp).fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.size(80.dp).background(SuccessGreen.copy(alpha = 0.2f), RoundedCornerShape(40.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "WORKOUT COMPLETE",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Great job! Your performance has been logged.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))

            // Summary Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStatNode("VOLUME", "${volume.toInt()} kg")
                SummaryStatNode("TIME", formatDuration(durationSeconds * 1000))
                SummaryStatNode("EXERCISES", "$exerciseCount")
            }

            Spacer(modifier = Modifier.height(48.dp))
            PrimaryActionButton(
                text = "View Dashboard",
                onClick = onDone,
                isSuccess = true
            )
        }
    }
}

@Composable
private fun SummaryStatNode(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = TextPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            fontWeight = FontWeight.Bold
        )
    }
}
