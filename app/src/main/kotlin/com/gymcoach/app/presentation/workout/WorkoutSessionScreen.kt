package com.gymcoach.app.presentation.workout

import com.gymcoach.app.core.preferences.WeightUnit
import com.gymcoach.app.presentation.workout.components.ActiveWorkoutSetRow
import com.gymcoach.app.presentation.workout.components.ExerciseSubstitutionDialog
import com.gymcoach.app.presentation.workout.components.PlateCalculatorDialog
import com.gymcoach.app.presentation.workout.components.SupersetLinkDialog
import com.gymcoach.app.presentation.workout.components.WarmupCalculatorDialog
import com.gymcoach.app.presentation.components.ExerciseTechniqueBottomSheet
import androidx.compose.material.icons.filled.PlayCircleOutline

import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.gymcoach.app.ui.theme.GymCoachBorders
import com.gymcoach.app.ui.theme.GymCoachShapes
import com.gymcoach.app.ui.theme.GymCoachSpacing
import com.gymcoach.app.ui.theme.GymCoachColors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.Surface
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

@kotlinx.parcelize.Parcelize
data class WarmupDialogState(val exerciseIndex: Int, val exerciseName: String, val targetWeight: Double) : android.os.Parcelable

@kotlinx.parcelize.Parcelize
data class SubstitutionDialogState(val exerciseIndex: Int, val exerciseId: Long, val exerciseName: String) : android.os.Parcelable

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
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showEmptyWorkoutDiscardDialog by rememberSaveable { mutableStateOf(false) }
    val discarded by viewModel.discarded.collectAsState()
    LaunchedEffect(discarded) {
        if (discarded) {
            onBackClick()
        }
    }
    var plateCalcWeight by rememberSaveable { mutableStateOf<Double?>(null) }
    var preferredBarWeight by rememberSaveable { mutableStateOf<Double?>(null) }
    var warmupDialogData by rememberSaveable { mutableStateOf<WarmupDialogState?>(null) }
    var substitutionDialogData by rememberSaveable { mutableStateOf<SubstitutionDialogState?>(null) }
    val substitutes by viewModel.substitutes.collectAsState()
    val isSubstitutionLoading by viewModel.isSubstitutionLoading.collectAsState()
    var pickerSearchQuery by rememberSaveable { mutableStateOf("") }
    var pickerSelectedCategory by rememberSaveable { mutableStateOf("All") }
    var showCreateCustomExerciseInWorkout by rememberSaveable { mutableStateOf(false) }
    var activeCameraExerciseIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val supersetGroups by viewModel.supersetGroups.collectAsState()
    var supersetDialogExerciseIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val animationRepository = remember { com.gymcoach.app.core.animation.AnimationRepository(context.applicationContext) }
    var selectedTechniqueExerciseId by rememberSaveable { mutableStateOf<Long?>(null) }
    val workoutListState = rememberLazyListState()
    val selectedTechniqueExercise = currentWorkout?.exercises?.firstOrNull { it.exercise.id == selectedTechniqueExerciseId }?.exercise
    val preferencesState by viewModel.preferencesState.collectAsState()
    val weightUnit = preferencesState.weightUnit

    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.prEvents.collect { pr ->
            snackbarHostState.showSnackbar(
                message = "🏆 NEW PR: ${pr.exerciseName} - ${pr.details}!",
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
        }
    }

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

    DisposableEffect(preferencesState.keepScreenOn) {
        val activity = context as? android.app.Activity
        if (preferencesState.keepScreenOn) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
        val summary = workoutSummary
        if (summary != null && summary.completedSetsCount > 0) {
            WorkoutCompletionView(
                summary = summary,
                onDone = onBackClick,
                onViewHistoryDetail = onViewHistoryDetail,
                weightUnit = weightUnit
            )
        } else {
            LaunchedEffect(Unit) {
                onBackClick()
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
                                color = GymCoachColors.Primary
                            )
                            if (sessionVolume > 0) {
                                val volFormatted = java.text.NumberFormat.getNumberInstance(Locale.US).format(sessionVolume)
                                Text(
                                    text = "$volFormatted ${weightUnit.code}·reps",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GymCoachColors.CyanAccent
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
                },
                actions = {
                    TextButton(onClick = { showDiscardDialog = true }) {
                        Text(
                            text = "Discard",
                            color = GymCoachColors.ErrorRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )
        },
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = workoutListState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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


                    item(key = "workout_header_spacer") { Spacer(Modifier.height(8.dp)) }

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
                               weightUnit = weightUnit,
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
                               onOpenWarmupCalculator = { w -> warmupDialogData = WarmupDialogState(exIdx, we.exercise.name, w) },
                               onSubstituteExercise = {
                                   substitutionDialogData = SubstitutionDialogState(exIdx, we.exercise.id, we.exercise.name)
                                   viewModel.loadSubstitutesForExercise(we.exercise.id)
                               },
                               onCameraClick = { type ->
                                   activeCameraExerciseIndex = exIdx
                                   onCameraClick(type)
                               },
                               supersetGroup = supersetGroups.firstOrNull { exIdx in it.exerciseIndices },
                               onOpenSupersetDialog = { supersetDialogExerciseIndex = exIdx },
                               onOpenTechniqueGuide = {
                                   selectedTechniqueExerciseId = we.exercise.id
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

                        item {
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { showDiscardDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = GymCoachColors.ErrorRed
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    GymCoachColors.ErrorRed.copy(alpha = 0.5f)
                                ),
                                shape = GymCoachShapes.md
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Discard Workout",
                                    modifier = Modifier.size(18.dp),
                                    tint = GymCoachColors.ErrorRed
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Discard Workout",
                                    fontWeight = FontWeight.SemiBold,
                                    color = GymCoachColors.ErrorRed
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }

            // Persistent Floating Rest Timer Dock
            AnimatedVisibility(
                visible = restTimerState.isRunning,
                enter = fadeIn() + expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
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
                                contentColor = GymCoachColors.TextPrimary
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add exercise to workout", modifier = Modifier.size(18.dp), tint = GymCoachColors.Primary)
                            Spacer(Modifier.width(6.dp))
                            Text("Add Exercise", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                val completedCount = currentWorkout?.exercises?.sumOf { we -> we.sets.count { it.completed } } ?: 0
                                if (completedCount == 0) {
                                    showEmptyWorkoutDiscardDialog = true
                                } else {
                                    showFinishDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GymCoachColors.Primary,
                                contentColor = androidx.compose.ui.graphics.Color.White
                            ),
                            shape = GymCoachShapes.md,
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Finish workout", modifier = Modifier.size(18.dp))
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
            barWeight = preferredBarWeight ?: (if (weightUnit == WeightUnit.LBS) 45.0 else 20.0),
            weightUnit = weightUnit,
            onDismiss = { plateCalcWeight = null },

            onBarWeightChange = { preferredBarWeight = it }
        )
    }

    if (warmupDialogData != null) {
        val (exIdx, exName, targetWeight) = warmupDialogData!!
        WarmupCalculatorDialog(
            exerciseName = exName,
            targetWeight = targetWeight,
            weightUnit = weightUnit,
            onInsertWarmupSets = { warmupSets ->
                viewModel.addWarmupSets(exIdx, warmupSets)
            },
            onDismiss = { warmupDialogData = null }
        )
    }

    if (substitutionDialogData != null) {
        val (exIdx, _, exName) = substitutionDialogData!!
        ExerciseSubstitutionDialog(
            currentExerciseName = exName,
            substitutes = substitutes,
            isLoading = isSubstitutionLoading,
            onSelectSubstitute = { newExerciseId ->
                viewModel.swapExercise(exIdx, newExerciseId)
                substitutionDialogData = null
            },
            onDismiss = { substitutionDialogData = null }
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

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Workout?") },
            text = { Text("Are you sure you want to discard this workout? All progress in this session will be permanently deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.discardWorkout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GymCoachColors.ErrorRed,
                        contentColor = GymCoachColors.TextPrimary
                    )
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEmptyWorkoutDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyWorkoutDiscardDialog = false },
            title = { Text("No Completed Sets") },
            text = { Text("You haven't completed any sets in this workout session. Would you like to discard it?") },
            confirmButton = {
                Button(
                    onClick = {
                        showEmptyWorkoutDiscardDialog = false
                        viewModel.discardWorkout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GymCoachColors.ErrorRed,
                        contentColor = GymCoachColors.TextPrimary
                    )
                ) {
                    Text("Discard Workout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyWorkoutDiscardDialog = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }

    if (showPicker) {
        val filteredExercises = remember(allExercises, pickerSelectedCategory, pickerSearchQuery) {
            allExercises.filter { exercise ->
                val matchesCategory = pickerSelectedCategory == "All" || exercise.muscleGroup.equals(pickerSelectedCategory, ignoreCase = true)
                val matchesQuery = pickerSearchQuery.isBlank() ||
                    exercise.name.contains(pickerSearchQuery, ignoreCase = true) ||
                    exercise.muscleGroup.contains(pickerSearchQuery, ignoreCase = true)
                matchesCategory && matchesQuery
            }
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
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Create custom exercise", modifier = Modifier.size(16.dp))
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
                                    containerColor = GymCoachColors.SurfaceDeep
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
                                        color = GymCoachColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.hideExercisePicker() }) {
                    Text("Close")
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

    supersetDialogExerciseIndex?.let { exIdx ->
        val exercises = currentWorkout?.exercises ?: emptyList()
        SupersetLinkDialog(
            currentExerciseIndex = exIdx,
            currentExerciseName = exercises.getOrNull(exIdx)?.exercise?.name ?: "",
            exercises = exercises.mapIndexed { idx, we -> Pair(idx, we.exercise.name) },
            existingGroup = viewModel.getSupersetForExercise(exIdx),
            onLink = { targetIdx ->
                viewModel.linkExercisesAsSuperset(exIdx, targetIdx)
                supersetDialogExerciseIndex = null
            },
            onUnlink = {
                viewModel.unlinkSuperset(exIdx)
                supersetDialogExerciseIndex = null
            },
            onDismiss = { supersetDialogExerciseIndex = null }
        )
    }

    if (selectedTechniqueExercise != null) {
        ExerciseTechniqueBottomSheet(
            exercise = selectedTechniqueExercise,
            animationRepository = animationRepository,
            onDismiss = { selectedTechniqueExerciseId = null }
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
        targetValue = if (!isPaused && timeRemaining > 0) GymCoachColors.Primary.copy(alpha = 0.65f)
                      else GymCoachColors.BorderSubtle,
        animationSpec = tween(durationMillis = 300),
        label = "restCardBorderColor"
    )

    val timerProgressBrush = remember {
        Brush.sweepGradient(
            listOf(GymCoachColors.Primary, GymCoachColors.CyanAccent, GymCoachColors.Primary)
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
                                    GymCoachColors.Primary,
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
                                .size(48.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(
                                    if (isPaused) GymCoachColors.SurfaceDeep
                                    else GymCoachColors.Primary.copy(alpha = 0.2f)
                                )
                                .clickable(
                                    role = androidx.compose.ui.semantics.Role.Button,
                                    onClick = onPauseResume
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "Resume Rest" else "Pause Rest",
                                tint = GymCoachColors.Primary,
                                modifier = Modifier.size(22.dp)
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
                            color = if (isPaused) GymCoachColors.TextSecondary else GymCoachColors.Primary
                        )
                        Text(
                            text = "${timeRemaining}s",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 26.sp
                            ),
                            color = GymCoachColors.TextPrimary
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
                            contentColor = GymCoachColors.TextPrimary
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
                            contentColor = GymCoachColors.TextPrimary
                        ),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("+15s", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = onPauseResume) {
                        Icon(
                            if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Pause/Resume",
                            tint = GymCoachColors.TextPrimary
                        )
                    }
                    IconButton(onClick = onSkip) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Skip",
                            tint = GymCoachColors.TextSecondary
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
                color = GymCoachColors.Primary,
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
                            labelColor = GymCoachColors.TextSecondary,
                            selectedContainerColor = GymCoachColors.Primary,
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
    @Suppress("UNUSED_PARAMETER") onRestSecondsChange: (Int, Int) -> Unit,
    onSetTypeChange: (Int, com.gymcoach.app.domain.model.SetType) -> Unit,
    onToggleComplete: (Int) -> Unit,
    onOpenPlateCalculator: (Double) -> Unit = {},
    onOpenWarmupCalculator: (Double) -> Unit = {},
    onSubstituteExercise: () -> Unit = {},
    onCameraClick: ((com.gymcoach.app.core.ml.ExerciseType) -> Unit)? = null,
    supersetGroup: com.gymcoach.app.domain.model.SupersetGroup? = null,
    onOpenSupersetDialog: () -> Unit = {},
    onOpenTechniqueGuide: () -> Unit = {},
    weightUnit: com.gymcoach.app.core.preferences.WeightUnit = com.gymcoach.app.core.preferences.WeightUnit.KG
) {
    var showInstructions by rememberSaveable { mutableStateOf(false) }
    var showMoreMenu by rememberSaveable { mutableStateOf(false) }
    val isExerciseActive = sets.isNotEmpty() && sets.any { !it.completed }
    val isExerciseCompleted = sets.isNotEmpty() && sets.all { it.completed }

    val cardBorderColor by animateColorAsState(
        targetValue = when {
            isExerciseCompleted -> GymCoachColors.Success.copy(alpha = 0.55f)
            isExerciseActive -> GymCoachColors.Primary.copy(alpha = 0.65f)
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
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(GymCoachShapes.xs)
                                .background(GymCoachColors.Primary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = muscleGroup.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = GymCoachColors.Primary
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
                                                contentDescription = "Set completed",
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
                                            .background(GymCoachColors.Primary.copy(alpha = 0.18f))
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "CURRENT",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                letterSpacing = 0.5.sp
                                            ),
                                            color = GymCoachColors.Primary
                                        )
                                    }
                                }
                                else -> {}
                            }
                        }

                        // Superset badge
                        if (supersetGroup != null) {
                            Surface(
                                shape = GymCoachShapes.pill,
                                color = GymCoachColors.Primary.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GymCoachColors.Primary.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = supersetGroup.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GymCoachColors.Primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(Modifier.width(GymCoachSpacing.xs))
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
                                tint = GymCoachColors.Primary
                            )
                        }
                    }
                    Surface(
                        onClick = onOpenTechniqueGuide,
                        shape = RoundedCornerShape(12.dp),
                        color = GymCoachColors.CyanAccent.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GymCoachColors.CyanAccent.copy(alpha = 0.6f)),
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleOutline,
                                contentDescription = "Exercise Technique & Biomechanical Form",
                                tint = GymCoachColors.CyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "FORM",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = GymCoachColors.CyanAccent
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Exercise Options",
                                tint = GymCoachColors.TextSecondary
                            )
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Plate Calculator") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = GymCoachColors.Primary
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    val maxWeight = sets.map { it.weight }.filter { it > 0 }.maxOrNull() ?: 20.0
                                    onOpenPlateCalculator(maxWeight)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Warm-Up Protocol") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Whatshot,
                                        contentDescription = null,
                                        tint = GymCoachColors.Primary
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    val maxWeight = sets.map { it.weight }.filter { it > 0 }.maxOrNull() ?: 20.0
                                    onOpenWarmupCalculator(maxWeight)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (supersetGroup != null) "Manage Superset" else "Pair as Superset") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (supersetGroup != null) Icons.Default.LinkOff else Icons.Default.Link,
                                        contentDescription = null,
                                        tint = if (supersetGroup != null) GymCoachColors.Primary else GymCoachColors.CyanAccent
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenSupersetDialog()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Substitute Exercise") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = null,
                                        tint = GymCoachColors.CyanAccent
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onSubstituteExercise()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Remove Exercise", color = GymCoachColors.ErrorRed) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = GymCoachColors.ErrorRed
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onRemoveExercise()
                                }
                            )
                        }
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
                                    contentDescription = "Adaptive progression tip",
                                    modifier = Modifier.size(15.dp),
                                    tint = GymCoachColors.Primary
                                )
                                Text(
                                    text = "WHY THIS WEIGHT? • ADAPTIVE COACH",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        fontSize = 10.sp
                                    ),
                                    color = GymCoachColors.Primary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(GymCoachShapes.xs)
                                    .background(GymCoachColors.Primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "AUTOREGULATED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    ),
                                    color = GymCoachColors.Primary
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
                                    color = GymCoachColors.TextSecondary
                                )
                                Text(
                                    text = "${recommendation.recommendedWeight} ${weightUnit.code} × ${recommendation.recommendedReps} reps",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = GymCoachColors.TextPrimary
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
                                contentDescription = "Previous session achieved",
                                tint = GymCoachColors.Success,
                                modifier = Modifier.size(14.dp).padding(top = 2.dp)
                            )
                            Text(
                                text = recommendation.reason,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    lineHeight = 18.sp,
                                    fontSize = 12.sp
                                ),
                                color = GymCoachColors.TextPrimary
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
                    "${it.weight}${weightUnit.code} × ${it.reps}" 
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
                                color = GymCoachColors.TextMuted
                            )
                            Text(
                                text = "Session Best: ${bestWeight}${weightUnit.code}",
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
                                color = GymCoachColors.TextSecondary
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
                        color = GymCoachColors.Primary
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
                                color = GymCoachColors.TextSecondary
                            )
                        }
                    }
                }
            }

            // Set Column Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "SET",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = GymCoachColors.TextSecondary,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    weightUnit.code.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = GymCoachColors.TextSecondary,
                    modifier = Modifier.weight(1.2f),
                    textAlign = TextAlign.Center
                )
                Text(
                    "REPS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = GymCoachColors.TextSecondary,
                    modifier = Modifier.weight(1.0f),
                    textAlign = TextAlign.Center
                )
                Text(
                    "DONE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = GymCoachColors.TextSecondary,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.Center
                )
            }

            val sortedSets = remember(sets) { sets.sortedBy { it.setNumber } }
            sortedSets.forEachIndexed { index, set ->
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
                            GymCoachColors.DangerBg
                        } else {
                            GymCoachColors.SurfaceElevated
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color, shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = GymCoachColors.Danger)
                            }
                        }
                    }
                ) {
                    val prevPerfStr = previousSets?.getOrNull(index)?.let {
                        "${it.weight}${weightUnit.code} × ${it.reps}"
                    }
                    ActiveWorkoutSetRow(
                        set = set,
                        previousPerformance = prevPerfStr,
                        weightUnit = weightUnit,
                        onWeightChange = { weight -> onWeightChange(index, weight) },
                        onRepsChange = { reps -> onRepsChange(index, reps) },
                        onRpeChange = { rpe -> onRpeChange(index, rpe) },
                        onSetTypeChange = { type -> onSetTypeChange(index, type) },
                        onCompleteToggle = { onToggleComplete(index) },
                        onQuickFill = {
                            previousSets?.getOrNull(index)?.let { prev ->
                                onWeightChange(index, prev.weight)
                                onRepsChange(index, prev.reps)
                            }
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val hasWorkingWeight = sets.any { it.weight > 0 }
                if (hasWorkingWeight) {
                    FilledTonalButton(
                        onClick = {
                            val maxWeight = sets.map { it.weight }.filter { it > 0 }.maxOrNull() ?: 20.0
                            onOpenWarmupCalculator(maxWeight)
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        shape = GymCoachShapes.sm,
                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                            containerColor = GymCoachColors.SurfaceDeep,
                            contentColor = GymCoachColors.TextPrimary
                        )
                    ) {
                        Icon(Icons.Default.Whatshot, contentDescription = "Warm-Up", modifier = Modifier.size(16.dp), tint = GymCoachColors.Primary)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Warm-Up",
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                FilledTonalButton(
                    onClick = onAddSet,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    shape = GymCoachShapes.sm,
                    colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                        containerColor = GymCoachColors.SurfaceCardElevated,
                        contentColor = GymCoachColors.TextPrimary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add set", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Add Set",
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
            com.gymcoach.app.ui.theme.GymCoachColors.SetWarmup.copy(alpha = 0.4f)
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
                    color = com.gymcoach.app.ui.theme.GymCoachColors.SetWarmup,
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
                    tint = com.gymcoach.app.ui.theme.GymCoachColors.SetWarmup
                )
            }
        }
    }
}

@Composable
internal fun WorkoutCompletionView(
    summary: WorkoutLoggingViewModel.WorkoutSummary,
    onDone: () -> Unit,
    onViewHistoryDetail: (Long) -> Unit,
    weightUnit: com.gymcoach.app.core.preferences.WeightUnit = com.gymcoach.app.core.preferences.WeightUnit.KG
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
            .background(GymCoachColors.PureDark)
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
                            GymCoachColors.Primary,
                            Color.Transparent
                        )
                    )
                }
                val celebrationCircleBrush = remember {
                    Brush.linearGradient(
                        listOf(GymCoachColors.Primary, GymCoachColors.CyanAccent)
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
                color = GymCoachColors.Primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${summary.workoutName} • ${java.time.LocalDate.now()}",
                style = MaterialTheme.typography.titleMedium,
                color = GymCoachColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (summary.newPRs.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(summary.newPRs, key = { "${it.exerciseId}_${it.type.name}" }) { pr ->
                        Surface(
                            shape = GymCoachShapes.pill,
                            color = com.gymcoach.app.ui.theme.GymCoachColors.SetWarmup.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, com.gymcoach.app.ui.theme.GymCoachColors.SetWarmup.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "🏆 ${pr.exerciseName}: ${pr.value} ${if (pr.type == com.gymcoach.app.core.progression.PRDetector.PRType.REP) "reps" else "kg"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = com.gymcoach.app.ui.theme.GymCoachColors.SetWarmup,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (summary.musclesTrained.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(summary.musclesTrained, key = { it }) { muscle ->
                        Surface(
                            shape = GymCoachShapes.pill,
                            color = GymCoachColors.Primary.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GymCoachColors.Primary.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = muscle.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = GymCoachColors.Primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 2x2 Grid using Rows and Columns
            val volumeFormatted = java.text.NumberFormat.getNumberInstance(Locale.US).format(summary.totalVolumeKg)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    StatCard(
                        title = "Total Volume",
                        value = "$volumeFormatted ${weightUnit.code}"
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    StatCard(
                        title = "Sets Completed",
                        value = "${summary.completedSetsCount} / ${summary.totalSetsCount}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    StatCard(
                        title = "Exercises",
                        value = "${summary.exercisesCompletedCount}"
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    StatCard(
                        title = "Duration",
                        value = formatDuration(summary.durationSeconds)
                    )
                }
            }


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
            containerColor = if (highlight) GymCoachColors.SurfaceCardElevated.copy(alpha = 0.4f)
                             else GymCoachColors.SurfaceCard
        ),
        border = if (highlight) androidx.compose.foundation.BorderStroke(1.dp, GymCoachColors.Primary.copy(alpha = 0.5f))
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
                color = if (highlight) GymCoachColors.Primary else GymCoachColors.TextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = if (highlight) GymCoachColors.Primary else GymCoachColors.TextPrimary,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        }
    }
}


