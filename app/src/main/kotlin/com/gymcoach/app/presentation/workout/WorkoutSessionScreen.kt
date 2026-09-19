package com.gymcoach.app.presentation.workout

import com.gymcoach.app.presentation.workout.components.PlateCalculatorDialog
import com.gymcoach.app.presentation.workout.components.WarmupCalculatorDialog

import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.gymcoach.app.ui.theme.GymCoachBorders
import com.gymcoach.app.ui.theme.GymCoachShapes
import com.gymcoach.app.ui.theme.GymCoachSpacing
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.AccentBlue
import com.gymcoach.app.ui.theme.TextPrimary
import com.gymcoach.app.ui.theme.TextSecondary
import com.gymcoach.app.ui.theme.TextTertiary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import com.gymcoach.app.presentation.components.CreateCustomExerciseBottomSheet
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.util.Locale

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WorkoutSessionScreen(
    onBackClick: () -> Unit,
    workoutId: Long? = null,
    onViewHistoryDetail: (Long) -> Unit = {},
    onCameraClick: (com.gymcoach.app.core.ml.ExerciseType) -> Unit = {},
    appliedReps: Int? = null,
    onClearAppliedReps: () -> Unit = {},
    viewModel: WorkoutLoggingViewModel = hiltViewModel()
) {
    val currentWorkout by viewModel.currentWorkout.collectAsState()
    val showPicker by viewModel.showExercisePicker.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState()
    val completed by viewModel.completed.collectAsState()
    val workoutSummary by viewModel.workoutSummary.collectAsState()
    val error by viewModel.error.collectAsState()
    val restTimerState by viewModel.restTimerState.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val previousPerformance by viewModel.previousPerformance.collectAsState()
    val lastPerformanceSummary by viewModel.lastPerformanceSummary.collectAsState()
    val sessionVolume by viewModel.sessionVolume.collectAsState()
    val progressionRecommendations by viewModel.progressionRecommendations.collectAsState()
    val latestReadiness by viewModel.latestReadiness.collectAsState()
    var dismissReadinessAdvisory by rememberSaveable { mutableStateOf(false) }
    var showFinishDialog by rememberSaveable { mutableStateOf(false) }
    var plateCalcWeight by rememberSaveable { mutableStateOf<Double?>(null) }
    var warmupDialogData by rememberSaveable { mutableStateOf<Triple<Int, String, Double>?>(null) }
    var pickerSearchQuery by rememberSaveable { mutableStateOf("") }
    var pickerSelectedCategory by rememberSaveable { mutableStateOf("All") }
    var showCreateCustomExerciseInWorkout by rememberSaveable { mutableStateOf(false) }
    var activeCameraExerciseIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    val haptic = LocalHapticFeedback.current
    val rememberRestTimer = rememberSaveable { mutableStateOf(false) }
    var wasTimerRunning by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(restTimerState.isRunning, restTimerState.timeRemaining) {
        if (wasTimerRunning && !restTimerState.isRunning && restTimerState.timeRemaining == 0) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        wasTimerRunning = restTimerState.isRunning
        rememberRestTimer.value = restTimerState.isRunning
    }

    LaunchedEffect(workoutId) {
        viewModel.loadOrStartWorkout(workoutId)
    }

    LaunchedEffect(appliedReps) {
        val reps = appliedReps
        if (reps != null && reps > 0) {
            val targetIdx = activeCameraExerciseIndex ?: 0
            viewModel.applyCameraReps(targetIdx, reps)
            onClearAppliedReps()
        }
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
        if (workoutSummary != null) {
            WorkoutCompletionView(
                summary = workoutSummary!!,
                onDone = onBackClick,
                onViewHistoryDetail = onViewHistoryDetail
            )
        } else {
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
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (sessionVolume > 0) {
                                Text(
                                    text = "${String.format(Locale.US, "%.0f", sessionVolume)} kg·reps",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
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
                    // Recovery & Readiness advisory banner (only active if logged today)
                    val readiness = latestReadiness
                    if (readiness != null && readiness.isRecordedToday && readiness.readinessScore < 3.0 && !dismissReadinessAdvisory) {
                        item {
                            RecoveryAdvisoryBanner(
                                readiness = readiness,
                                onDismiss = { dismissReadinessAdvisory = true }
                            )
                        }
                    }

                    // Rest timer card with preset buttons
                    if (restTimerState.isRunning) {
                        item(key = "rest_timer_card") {
                            AnimatedVisibility(
                                visible = restTimerState.isRunning,
                                enter = fadeIn() + expandVertically(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ),
                                exit = fadeOut() + shrinkVertically(),
                                modifier = Modifier.animateItemPlacement()
                            ) {
                                RestTimerCard(
                                    timeRemaining = restTimerState.timeRemaining,
                                    totalDuration = restTimerState.totalDuration,
                                    isPaused = restTimerState.isPaused,
                                    onPauseResume = {
                                        if (restTimerState.isPaused) viewModel.resumeRestTimer()
                                        else viewModel.pauseRestTimer()
                                    },
                                    onSkip = { viewModel.stopRestTimer() },
                                    onAddFifteen = { viewModel.adjustRestTimer(15) },
                                    onSubtractFifteen = { viewModel.adjustRestTimer(-15) },
                                    onPresetTap = { seconds -> viewModel.changeRestTimerDuration(seconds) }
                                )
                            }
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }

                    workout.exercises.let { exercises ->
                        itemsIndexed(exercises, key = { _, ex -> ex.workoutExercise.id }) { exIdx, we ->
                            val lastSets = previousPerformance[we.exercise.id]
                            val lastPerf = lastPerformanceSummary[we.exercise.id]

                            ExerciseSetCard(
                               modifier = Modifier.animateItemPlacement(),
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
                               onToggleComplete = { setIdx -> viewModel.toggleSetCompletion(exIdx, setIdx) },
                               onOpenPlateCalculator = { w -> plateCalcWeight = w },
                               onOpenWarmupCalculator = { w -> warmupDialogData = Triple(exIdx, we.exercise.name, w) },
                               onCameraClick = { type ->
                                   activeCameraExerciseIndex = exIdx
                                   onCameraClick(type)
                               }
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

            androidx.compose.material3.Surface(
                color = GymCoachColors.SurfaceDeep,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.material3.HorizontalDivider(
                        thickness = 1.dp,
                        color = GymCoachColors.BorderSubtle
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.showExercisePicker() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = GymCoachShapes.md,
                            border = GymCoachBorders.subtle,
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = TextPrimary
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = AccentBlue)
                            Spacer(Modifier.width(6.dp))
                            Text("Add Exercise", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { showFinishDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentBlue,
                                contentColor = androidx.compose.ui.graphics.Color.White
                            ),
                            shape = GymCoachShapes.md,
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Finish Workout", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (plateCalcWeight != null) {
        PlateCalculatorDialog(
            targetWeight = plateCalcWeight!!,
            onDismiss = { plateCalcWeight = null }
        )
    }

    if (warmupDialogData != null) {
        val (exIdx, exName, targetWeight) = warmupDialogData!!
        WarmupCalculatorDialog(
            exerciseName = exName,
            targetWeight = targetWeight,
            onInsertWarmupSets = { warmupSets ->
                viewModel.addWarmupSets(exIdx, warmupSets)
            },
            onDismiss = { warmupDialogData = null }
        )
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
        val filteredExercises = allExercises.filter { exercise ->
            val matchesCategory = pickerSelectedCategory == "All" || exercise.muscleGroup.equals(pickerSelectedCategory, ignoreCase = true)
            val matchesQuery = pickerSearchQuery.isBlank() ||
                exercise.name.contains(pickerSearchQuery, ignoreCase = true) ||
                exercise.muscleGroup.contains(pickerSearchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }

        AlertDialog(
            onDismissRequest = { viewModel.hideExercisePicker() },
            title = { Text("Add Exercise") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = pickerSearchQuery,
                        onValueChange = { pickerSearchQuery = it },
                        placeholder = { Text("Search exercise...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("All", "Chest", "Back", "Legs", "Shoulders", "Arms", "Core").forEach { category ->
                            FilterChip(
                                selected = pickerSelectedCategory == category,
                                onClick = { pickerSelectedCategory = category },
                                label = { Text(category) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.hideExercisePicker()
                            showCreateCustomExerciseInWorkout = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create Custom Exercise")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredExercises, key = { it.id }) { exercise ->
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

    if (showCreateCustomExerciseInWorkout) {
        CreateCustomExerciseBottomSheet(
            onDismiss = { showCreateCustomExerciseInWorkout = false },
            onSave = { name, muscleGroup, equipment, difficulty, notes ->
                viewModel.createAndAddCustomExercise(
                    name = name,
                    muscleGroup = muscleGroup,
                    equipment = equipment,
                    difficulty = difficulty,
                    notes = notes
                )
                showCreateCustomExerciseInWorkout = false
            }
        )
    }
}

// Rest timer card with circular progress sweep, resting pulse, and quick-select preset buttons.
@Composable
private fun RestTimerCard(
    timeRemaining: Int,
    totalDuration: Int,
    isPaused: Boolean,
    onPauseResume: () -> Unit,
    onSkip: () -> Unit,
    onAddFifteen: () -> Unit,
    onSubtractFifteen: () -> Unit,
    onPresetTap: (Int) -> Unit
) {
    val targetProgress = if (totalDuration > 0) (timeRemaining.toFloat() / totalDuration).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 500, easing = LinearEasing),
        label = "restCircularProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "restingPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (!isPaused && timeRemaining > 0) 1.14f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = if (!isPaused && timeRemaining > 0) 0.45f else 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val cardBorderColor by animateColorAsState(
        targetValue = if (!isPaused && timeRemaining > 0) AccentBlue.copy(alpha = 0.65f)
                      else GymCoachColors.BorderSubtle,
        animationSpec = tween(durationMillis = 300),
        label = "restCardBorderColor"
    )

    val timerProgressBrush = remember {
        Brush.sweepGradient(
            listOf(AccentBlue, GymCoachColors.CyanAccent, AccentBlue)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = GymCoachShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = GymCoachColors.SurfaceCardElevated
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Circular countdown progress sweep with athletic resting pulse
                    Box(
                        modifier = Modifier.size(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Resting pulse halo
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                    alpha = pulseAlpha * 0.35f
                                }
                                .background(
                                    AccentBlue,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                        // Circular progress canvas
                        Canvas(modifier = Modifier.size(48.dp)) {
                            val strokeWidth = 3.5.dp.toPx()
                            val diameter = size.minDimension - strokeWidth
                            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                            val arcSize = Size(diameter, diameter)

                            // Background circular track
                            drawArc(
                                color = GymCoachColors.SurfaceDeep,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            // Animated progress sweep arc
                            drawArc(
                                brush = timerProgressBrush,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                        // Center Play/Pause button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(
                                    if (isPaused) GymCoachColors.SurfaceDeep
                                    else AccentBlue.copy(alpha = 0.2f)
                                )
                                .clickable { onPauseResume() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "Resume Rest" else "Pause Rest",
                                tint = AccentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isPaused) "REST PAUSED" else "REST INTERVAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            ),
                            color = if (isPaused) TextSecondary else AccentBlue
                        )
                        Text(
                            text = "${timeRemaining}s",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 26.sp
                            ),
                            color = TextPrimary
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = onSubtractFifteen,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = GymCoachShapes.xs,
                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                            containerColor = GymCoachColors.SurfaceDeep,
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("-15s", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                    FilledTonalButton(
                        onClick = onAddFifteen,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = GymCoachShapes.xs,
                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                            containerColor = GymCoachColors.SurfaceDeep,
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("+15s", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = onPauseResume) {
                        Icon(
                            if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Pause/Resume",
                            tint = TextPrimary
                        )
                    }
                    IconButton(onClick = onSkip) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Skip",
                            tint = TextSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(GymCoachShapes.pill),
                color = AccentBlue,
                trackColor = GymCoachColors.SurfaceDeep
            )

            // Quick-select rest duration presets
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                        shape = GymCoachShapes.pill,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = GymCoachColors.SurfaceDeep,
                            labelColor = TextSecondary,
                            selectedContainerColor = AccentBlue,
                            selectedLabelColor = androidx.compose.ui.graphics.Color.White
                        ),
                        border = if (totalDuration == seconds) null else GymCoachBorders.subtle,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExerciseSetCard(
    exerciseName: String,
    muscleGroup: String,
    sets: List<com.gymcoach.app.domain.model.WorkoutSet>,
    previousSets: List<LastSetData>?,
    lastPerformance: com.gymcoach.app.data.local.dao.LastPerformance?,
    instructions: String,
    recommendation: com.gymcoach.app.core.progression.ProgressionEngine.ProgressionRecommendation? = null,
    modifier: Modifier = Modifier,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onRemoveExercise: () -> Unit,
    onRepsChange: (Int, Int) -> Unit,
    onWeightChange: (Int, Double) -> Unit,
    onRpeChange: (Int, Double) -> Unit,
    onRestSecondsChange: (Int, Int) -> Unit,
    onSetTypeChange: (Int, com.gymcoach.app.domain.model.SetType) -> Unit,
    onToggleComplete: (Int) -> Unit,
    onOpenPlateCalculator: (Double) -> Unit = {},
    onOpenWarmupCalculator: (Double) -> Unit = {},
    onCameraClick: ((com.gymcoach.app.core.ml.ExerciseType) -> Unit)? = null
) {
    var showInstructions by rememberSaveable { mutableStateOf(false) }
    val isExerciseActive = sets.isNotEmpty() && sets.any { !it.completed }
    val isExerciseCompleted = sets.isNotEmpty() && sets.all { it.completed }

    val cardBorderColor by animateColorAsState(
        targetValue = when {
            isExerciseCompleted -> GymCoachColors.Success.copy(alpha = 0.55f)
            isExerciseActive -> AccentBlue.copy(alpha = 0.65f)
            else -> GymCoachColors.BorderSubtle
        },
        animationSpec = tween(durationMillis = 350),
        label = "exerciseCardBorderColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        shape = GymCoachShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = GymCoachColors.SurfaceCard
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Exercise Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(GymCoachShapes.xs)
                                .background(AccentBlue.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = muscleGroup.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = AccentBlue
                            )
                        }

                        AnimatedContent(
                            targetState = when {
                                isExerciseCompleted -> "COMPLETED"
                                isExerciseActive -> "ACTIVE"
                                else -> null
                            },
                            transitionSpec = {
                                (fadeIn(tween(200)) + scaleIn(initialScale = 0.85f))
                                    .togetherWith(fadeOut(tween(150)))
                            },
                            label = "exerciseStatusBadge"
                        ) { status ->
                            when (status) {
                                "COMPLETED" -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(GymCoachShapes.xs)
                                            .background(GymCoachColors.Success.copy(alpha = 0.15f))
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = GymCoachColors.Success,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = "COMPLETED",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp
                                                ),
                                                color = GymCoachColors.Success
                                            )
                                        }
                                    }
                                }
                                "ACTIVE" -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(GymCoachShapes.xs)
                                            .background(AccentBlue.copy(alpha = 0.18f))
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "CURRENT",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                letterSpacing = 0.5.sp
                                            ),
                                            color = AccentBlue
                                        )
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val exerciseType = com.gymcoach.app.core.ml.ExerciseType.fromExerciseName(exerciseName)
                    if (exerciseType != null && onCameraClick != null) {
                        IconButton(onClick = { onCameraClick(exerciseType) }) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Camera Form Coach",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = {
                        val maxWeight = sets.map { it.weight }.filter { it > 0 }.maxOrNull() ?: 20.0
                        onOpenPlateCalculator(maxWeight)
                    }) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = "Plate Calculator",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {
                        val maxWeight = sets.map { it.weight }.filter { it > 0 }.maxOrNull() ?: 20.0
                        onOpenWarmupCalculator(maxWeight)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = "Warm-Up Protocol",
                            tint = GymCoachColors.Primary
                        )
                    }
                    IconButton(onClick = onRemoveExercise) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove Exercise",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Adaptive Progression Recommendation banner ("WHY THIS WEIGHT?")
            if (recommendation != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = GymCoachShapes.md,
                    colors = CardDefaults.cardColors(
                        containerColor = GymCoachColors.SurfaceCardElevated
                    ),
                    border = GymCoachBorders.primary
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = AccentBlue
                                )
                                Text(
                                    text = "WHY THIS WEIGHT? • ADAPTIVE COACH",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        fontSize = 10.sp
                                    ),
                                    color = AccentBlue
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(GymCoachShapes.xs)
                                    .background(AccentBlue.copy(alpha = 0.15f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "AUTOREGULATED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    ),
                                    color = AccentBlue
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "Target for Today",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${recommendation.recommendedWeight} kg × ${recommendation.recommendedReps} reps",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = TextPrimary
                                )
                            }
                        }

                        // Coaching reason from progression engine
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = GymCoachColors.Success,
                                modifier = Modifier.size(14.dp).padding(top = 2.dp)
                            )
                            Text(
                                text = recommendation.reason,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    lineHeight = 18.sp,
                                    fontSize = 12.sp
                                ),
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Previous performance indicator (What did I lift last time?)
            if (lastPerformance != null) {
                val lastDate = Instant.ofEpochMilli(lastPerformance.date)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("MMM d"))
                val bestWeight = lastPerformance.maxWeight
                val lastSetSummary = previousSets?.joinToString("  •  ") { 
                    "${it.weight}kg × ${it.reps}" 
                } ?: ""

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = GymCoachShapes.sm,
                    colors = CardDefaults.cardColors(
                        containerColor = GymCoachColors.SurfaceCard
                    ),
                    border = GymCoachBorders.subtle
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PREVIOUS SESSION ($lastDate)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp,
                                    fontSize = 10.sp
                                ),
                                color = TextTertiary
                            )
                            Text(
                                text = "Session Best: ${bestWeight}kg",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = GymCoachColors.CyanAccent
                            )
                        }
                        if (lastSetSummary.isNotEmpty()) {
                            Text(
                                text = lastSetSummary,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = TextSecondary
                            )
                        }
                    }
                }
            }


            // Instructions expander with smooth expansion transition
            if (instructions.isNotEmpty()) {
                TextButton(
                    onClick = { showInstructions = !showInstructions },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        if (showInstructions) "Hide Instructions" else "View Instructions",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
    
                AnimatedVisibility(
                    visible = showInstructions,
                    enter = fadeIn() + expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = GymCoachShapes.sm,
                        colors = CardDefaults.cardColors(
                            containerColor = GymCoachColors.SurfaceCardElevated
                        ),
                        border = GymCoachBorders.subtle
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Instructions",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                instructions,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Set Column Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "SET",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.12f),
                    textAlign = TextAlign.Center
                )
                Text(
                    "KG",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.22f),
                    textAlign = TextAlign.Center
                )
                Text(
                    "REPS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.22f),
                    textAlign = TextAlign.Center
                )
                Text(
                    "RPE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.16f),
                    textAlign = TextAlign.Center
                )
                Text(
                    "REST",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.16f),
                    textAlign = TextAlign.Center
                )
                Text(
                    "DONE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(44.dp),
                    textAlign = TextAlign.Center
                )
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
                                .background(color, shape = RoundedCornerShape(8.dp))
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

            FilledTonalButton(
                onClick = onAddSet,
                modifier = Modifier.fillMaxWidth(),
                shape = GymCoachShapes.sm,
                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                    containerColor = GymCoachColors.SurfaceCardElevated,
                    contentColor = TextPrimary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add Set", fontWeight = FontWeight.SemiBold)
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
    onToggleComplete: () -> Unit
) {
    var weightText by rememberSaveable { mutableStateOf(if (weight > 0) weight.toString() else "") }
    var repsText by rememberSaveable { mutableStateOf(if (reps > 0) reps.toString() else "") }
    var rpeText by rememberSaveable { mutableStateOf(if (rpe > 0) rpe.toString() else "") }
    var restText by rememberSaveable { mutableStateOf(if (restSeconds > 0) restSeconds.toString() else "") }
    val haptic = LocalHapticFeedback.current

    val setTypeColor = when (setType) {
        com.gymcoach.app.domain.model.SetType.WARMUP -> androidx.compose.ui.graphics.Color(0xFFFFB74D)
        com.gymcoach.app.domain.model.SetType.DROP -> androidx.compose.ui.graphics.Color(0xFFBA68C8)
        com.gymcoach.app.domain.model.SetType.FAILURE -> androidx.compose.ui.graphics.Color(0xFFE57373)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val setTypeText = when (setType) {
        com.gymcoach.app.domain.model.SetType.WARMUP -> "W"
        com.gymcoach.app.domain.model.SetType.DROP -> "D"
        com.gymcoach.app.domain.model.SetType.FAILURE -> "F"
        else -> "${index + 1}"
    }

    // Tactile checkmark pop: spring bounce scale from 0.8f to 1.2f to 1.0f upon checking off a set
    val checkScale = androidx.compose.runtime.remember { Animatable(1.0f) }
    LaunchedEffect(completed) {
        if (completed) {
            checkScale.snapTo(0.8f)
            checkScale.animateTo(
                targetValue = 1.2f,
                animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing)
            )
            checkScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        } else {
            checkScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 150)
            )
        }
    }

    // Smooth color transitions upon checking off a set
    val rowBackgroundColor by animateColorAsState(
        targetValue = if (completed) GymCoachColors.Success.copy(alpha = 0.09f)
                      else GymCoachColors.SurfaceDeep,
        animationSpec = tween(durationMillis = 280),
        label = "rowBgColor"
    )
    val rowBorderColor by animateColorAsState(
        targetValue = if (completed) GymCoachColors.Success.copy(alpha = 0.65f)
                      else GymCoachColors.BorderSubtle,
        animationSpec = tween(durationMillis = 280),
        label = "rowBorderColor"
    )
    val checkButtonBgColor by animateColorAsState(
        targetValue = if (completed) GymCoachColors.Success
                      else GymCoachColors.SurfaceCardElevated,
        animationSpec = tween(durationMillis = 220),
        label = "checkButtonBgColor"
    )
    val checkIconColor by animateColorAsState(
        targetValue = if (completed) Color.White
                      else TextTertiary,
        animationSpec = tween(durationMillis = 220),
        label = "checkIconColor"
    )
    val checkBorderColor by animateColorAsState(
        targetValue = if (completed) GymCoachColors.Success
                      else GymCoachColors.BorderSubtle,
        animationSpec = tween(durationMillis = 220),
        label = "checkBorderColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GymCoachShapes.sm)
            .background(rowBackgroundColor)
            .border(
                1.dp,
                rowBorderColor,
                GymCoachShapes.sm
            )
            .padding(vertical = 4.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set Number / Type Pill button (tap to cycle)
        Box(
            modifier = Modifier
                .weight(0.12f)
                .height(38.dp)
                .clip(GymCoachShapes.xs)
                .background(setTypeColor.copy(alpha = 0.12f))
                .clickable {
                    val nextType = when (setType) {
                        com.gymcoach.app.domain.model.SetType.NORMAL -> com.gymcoach.app.domain.model.SetType.WARMUP
                        com.gymcoach.app.domain.model.SetType.WARMUP -> com.gymcoach.app.domain.model.SetType.DROP
                        com.gymcoach.app.domain.model.SetType.DROP -> com.gymcoach.app.domain.model.SetType.FAILURE
                        com.gymcoach.app.domain.model.SetType.FAILURE -> com.gymcoach.app.domain.model.SetType.NORMAL
                    }
                    onSetTypeChange(nextType)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = setTypeText,
                style = MaterialTheme.typography.bodyMedium,
                color = setTypeColor,
                fontWeight = FontWeight.Black
            )
        }

        OutlinedTextField(
            value = weightText,
            onValueChange = { v ->
                weightText = v
                v.toDoubleOrNull()?.let { onWeightChange(it) }
            },
            modifier = Modifier.weight(0.22f),
            singleLine = true,
            shape = GymCoachShapes.xs,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GymCoachColors.SurfaceInput,
                unfocusedContainerColor = GymCoachColors.SurfaceInput,
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = GymCoachColors.BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        )

        OutlinedTextField(
            value = repsText,
            onValueChange = { v ->
                repsText = v
                v.toIntOrNull()?.let { onRepsChange(it) }
            },
            modifier = Modifier.weight(0.22f),
            singleLine = true,
            shape = GymCoachShapes.xs,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GymCoachColors.SurfaceInput,
                unfocusedContainerColor = GymCoachColors.SurfaceInput,
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = GymCoachColors.BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        )

        OutlinedTextField(
            value = rpeText,
            onValueChange = { v ->
                rpeText = v
                v.toDoubleOrNull()?.let { onRpeChange(it) }
            },
            modifier = Modifier.weight(0.16f),
            singleLine = true,
            shape = GymCoachShapes.xs,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GymCoachColors.SurfaceInput,
                unfocusedContainerColor = GymCoachColors.SurfaceInput,
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = GymCoachColors.BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        )

        OutlinedTextField(
            value = restText,
            onValueChange = { v ->
                restText = v
                v.toIntOrNull()?.let { onRestSecondsChange(it) }
            },
            modifier = Modifier.weight(0.16f),
            singleLine = true,
            shape = GymCoachShapes.xs,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GymCoachColors.SurfaceInput,
                unfocusedContainerColor = GymCoachColors.SurfaceInput,
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = GymCoachColors.BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        )

        // Tactile set completion button
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(38.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .graphicsLayer {
                        scaleX = checkScale.value
                        scaleY = checkScale.value
                    }
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(checkButtonBgColor)
                    .border(
                        1.dp,
                        checkBorderColor,
                        androidx.compose.foundation.shape.CircleShape
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleComplete()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = if (completed) "Set Completed" else "Mark Set Complete",
                    tint = checkIconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}


/**
 * Recovery Advisory banner informing the lifter of low readiness/high fatigue.
 */
@Composable
private fun RecoveryAdvisoryBanner(
    readiness: com.gymcoach.app.data.local.entity.ReadinessEntity,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = GymCoachShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color(0xFF332014)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            androidx.compose.ui.graphics.Color(0xFFFFB74D).copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RECOVERY ADVISORY (Readiness: %.1f/5.0)".format(readiness.readinessScore),
                    style = MaterialTheme.typography.labelSmall,
                    color = androidx.compose.ui.graphics.Color(0xFFFFB74D),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${readiness.trainingRecommendation}. Autoregulation recommended: leave 1-2 reps in reserve (RIR 2) and avoid forced failure.",
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color(0xFFFFF3E0)
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = androidx.compose.ui.graphics.Color(0xFFFFB74D)
                )
            }
        }
    }
}

@Composable
internal fun WorkoutCompletionView(
    summary: WorkoutLoggingViewModel.WorkoutSummary,
    onDone: () -> Unit,
    onViewHistoryDetail: (Long) -> Unit
) {
    // Animated fade-in entrance
    val enterAlpha = androidx.compose.runtime.remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        enterAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 350)
        )
    }

    // Celebration sparkle / bounce animation
    val sparkleTransition = rememberInfiniteTransition(label = "sparkles")
    val sparkleScale by sparkleTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleScale"
    )
    val sparkleRotation by sparkleTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleRotation"
    )
    val celebrationRingAlpha by sparkleTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "celebrationRingAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .graphicsLayer {
                    alpha = enterAlpha.value
                }
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Animated celebration sparkle badge with radiant halo & spring bounce
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer radiant halo
                val celebrationHaloBrush = remember {
                    Brush.radialGradient(
                        listOf(
                            AccentBlue,
                            Color.Transparent
                        )
                    )
                }
                val celebrationCircleBrush = remember {
                    Brush.linearGradient(
                        listOf(AccentBlue, GymCoachColors.CyanAccent)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .graphicsLayer {
                            scaleX = sparkleScale * 1.12f
                            scaleY = sparkleScale * 1.12f
                            alpha = celebrationRingAlpha * 0.45f
                        }
                        .background(
                            brush = celebrationHaloBrush,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                // Central celebratory circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            scaleX = sparkleScale
                            scaleY = sparkleScale
                        }
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(
                            brush = celebrationCircleBrush
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Celebration Sparkles",
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                rotationZ = sparkleRotation
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Workout Crushed! 🔥",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${summary.workoutName} • ${java.time.LocalDate.now()}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 2x2 Grid using Rows and Columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total Volume",
                    value = "${String.format(Locale.US, "%.0f", summary.totalVolumeKg)}\nkg·reps",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Duration",
                    value = formatDuration(summary.durationSeconds),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Sets Completed",
                    value = "${summary.completedSetsCount} / ${summary.totalSetsCount}",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Personal Records",
                    value = if (summary.newPRs.isNotEmpty()) "${summary.newPRs.size} New PRs!" else "0",
                    modifier = Modifier.weight(1f),
                    highlight = summary.newPRs.isNotEmpty()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (summary.newPRs.isNotEmpty()) {
                Text(
                    text = "Personal Records Broken",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(10.dp))

                summary.newPRs.forEach { pr ->
                    PRCard(pr)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { onViewHistoryDetail(summary.workoutId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("View Detailed Breakdown", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Card(
        modifier = modifier.aspectRatio(1.15f),
        shape = GymCoachShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                             else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = if (highlight) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                 else GymCoachBorders.subtleBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PRCard(pr: com.gymcoach.app.core.progression.PRDetector.PersonalRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = GymCoachShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFFFD54F).copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        androidx.compose.ui.graphics.Color(0xFFFFD54F).copy(alpha = 0.15f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "PR Icon",
                    tint = androidx.compose.ui.graphics.Color(0xFFFFD54F),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pr.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = pr.details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .background(
                        androidx.compose.ui.graphics.Color(0xFFFFD54F).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "NEW PR",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = androidx.compose.ui.graphics.Color(0xFFFFD54F),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
