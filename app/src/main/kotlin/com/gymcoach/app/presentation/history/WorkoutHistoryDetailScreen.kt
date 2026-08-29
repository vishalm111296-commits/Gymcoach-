package com.gymcoach.app.presentation.history

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.domain.model.WorkoutWithDetails
import com.gymcoach.app.domain.model.WorkoutWithStats
import com.gymcoach.app.domain.repository.AnalyticsRepository
import com.gymcoach.app.domain.repository.PersonalRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class WorkoutHistoryDetailViewModel @Inject constructor(
    private val workoutRepository: com.gymcoach.app.domain.repository.WorkoutRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<WorkoutHistoryDetailUiState>(WorkoutHistoryDetailUiState())
    val uiState: StateFlow<WorkoutHistoryDetailUiState> = _uiState.asStateFlow()

    private val _showDeleteConfirmation = MutableStateFlow(false)
    val showDeleteConfirmation: StateFlow<Boolean> = _showDeleteConfirmation

    private val _deleteTarget = MutableStateFlow<Long?>(null)
    val deleteTarget: StateFlow<Long?> = _deleteTarget

    fun loadWorkout(workoutId: Long) {
        viewModelScope.launch {
            _uiState.value = WorkoutHistoryDetailUiState(isLoading = true)
            try {
                val workout = workoutRepository.getWorkoutWithDetails(workoutId).first()
                _uiState.value = WorkoutHistoryDetailUiState(
                    isLoading = false,
                    workout = workout,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = WorkoutHistoryDetailUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load workout"
                )
            }
        }
    }

    fun onDeleteClick(workoutId: Long) {
        _deleteTarget.value = workoutId
    }

    fun confirmDelete() {
        _deleteTarget.value?.let { workoutId ->
            viewModelScope.launch {
                workoutRepository.deleteWorkout(workoutId)
            }
        }
    }

    private val _performAgainEvent = kotlinx.coroutines.flow.MutableSharedFlow<Long>()
    val performAgainEvent = _performAgainEvent.asSharedFlow()

    fun performAgain() {
        val currentWorkout = _uiState.value.workout
        if (currentWorkout != null) {
            viewModelScope.launch {
                try {
                    val newWorkoutId = workoutRepository.createSessionFromHistory(currentWorkout.workout.id)
                    _performAgainEvent.emit(newWorkoutId)
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.message ?: "Failed to perform again") }
                }
            }
        }
    }

    fun cancelDelete() {
        _deleteTarget.value = null
    }
}

data class WorkoutHistoryDetailUiState(
    val isLoading: Boolean = true,
    val workout: WorkoutWithDetails? = null,
    val error: String? = null
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryDetailScreen(
    workoutId: Long,
    onBackClick: () -> Unit,
    onEditClick: (Long) -> Unit = {},
    onPerformAgainClick: (Long) -> Unit = {},
    viewModel: WorkoutHistoryDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val showDeleteConfirmation by viewModel.showDeleteConfirmation.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viewModel.performAgainEvent) {
        viewModel.performAgainEvent.collect { newWorkoutId ->
            onPerformAgainClick(newWorkoutId)
        }
    }

    LaunchedEffect(workoutId) {
        viewModel.loadWorkout(workoutId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    androidx.compose.material3.TextButton(onClick = { viewModel.performAgain() }) {
                        androidx.compose.material3.Text("PERFORM AGAIN", color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                    }
                    // Share button
                    IconButton(onClick = {
                        state.workout?.let { workout ->
                            shareWorkoutSummary(context, workout)
                        }
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { onEditClick(workoutId) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = {
                        viewModel.onDeleteClick(workoutId)
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.error ?: "Unknown error",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                state.workout?.let { workout ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(Modifier.height(8.dp))

                        // Workout header
                        WorkoutHeaderCard(workout = workout.workout)

                        Spacer(Modifier.height(16.dp))

                        // Workout Summary Card
                        WorkoutSummaryCard(workout = workout)

                        Spacer(Modifier.height(16.dp))

                        // Muscle Group Breakdown
                        SectionHeader("Muscle Groups")
                        Spacer(Modifier.height(8.dp))
                        val muscleBreakdown = calculateMuscleBreakdown(workout)
                        if (muscleBreakdown.isNotEmpty()) {
                            muscleBreakdown.forEach { (muscle, data) ->
                                MuscleGroupRow(
                                    muscleName = muscle,
                                    sets = data.sets,
                                    totalVolume = data.volume,
                                    totalReps = data.reps
                                )
                            }
                        } else {
                            Text(
                                text = "No muscle data available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // Exercises
                        SectionHeader("Exercises")
                        Spacer(Modifier.height(8.dp))
                        workout.exercises.forEach { exerciseWithSets ->
                            ExerciseDetailCard(exerciseWithSets = exerciseWithSets)
                            Spacer(Modifier.height(12.dp))
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("Delete Workout") },
            text = { Text("Are you sure you want to delete this workout?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.confirmDelete()
                    onBackClick()
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// --- Share ---

private fun shareWorkoutSummary(context: Context, workout: WorkoutWithDetails) {
    val w = workout.workout
    val date = formatDate(w.date)
    val duration = formatDuration(w.duration)

    // Calculate totals
    var totalSets = 0
    var totalReps = 0
    var totalVolume = 0.0
    val muscleGroups = linkedMapOf<String, Int>()

    workout.exercises.forEach { entry ->
        val doneSets = entry.sets.filter { it.completed }
        totalSets += doneSets.size
        totalReps += doneSets.sumOf { it.reps }
        totalVolume += doneSets.sumOf { it.weight * it.reps }
        val muscle = entry.exercise.muscleGroup
        muscleGroups[muscle] = (muscleGroups[muscle] ?: 0) + doneSets.size
    }

    val sb = StringBuilder()
    sb.appendLine("\uD83C\uDFCB\uFE0F Workout Summary")
    sb.appendLine("Date: $date")
    sb.appendLine("Duration: $duration")
    sb.appendLine("Total Volume: %.1f kg".format(totalVolume))
    sb.appendLine("Sets: $totalSets | Reps: $totalReps")
    sb.appendLine()

    // Muscle groups
    if (muscleGroups.isNotEmpty()) {
        sb.appendLine("\uD83C\uDFAF Muscles Hit:")
        muscleGroups.entries.sortedByDescending { it.value }.forEach { (muscle, sets) ->
            sb.appendLine("  $muscle: $sets sets")
        }
        sb.appendLine()
    }

    // Exercises
    sb.appendLine("\uD83D\uDCAA Exercises:")
    workout.exercises.forEach { entry ->
        sb.appendLine("  ${entry.exercise.name}")
        entry.sets.sortedBy { it.setNumber }.forEach { set ->
            if (set.completed) {
                sb.appendLine("    Set ${set.setNumber}: ${set.weight}kg x ${set.reps} reps (RPE ${set.rpe})")
            }
        }
    }

    if (w.notes.isNotBlank()) {
        sb.appendLine()
        sb.appendLine("\uD83D\uDCDD Notes: ${w.notes}")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, sb.toString())
        putExtra(Intent.EXTRA_SUBJECT, "My Workout - $date")
    }
    context.startActivity(Intent.createChooser(intent, "Share Workout"))
}

// --- Muscle Breakdown ---

private data class MuscleData(var sets: Int = 0, var reps: Int = 0, var volume: Double = 0.0)

private fun calculateMuscleBreakdown(workout: WorkoutWithDetails): Map<String, MuscleData> {
    val breakdown = linkedMapOf<String, MuscleData>()

    workout.exercises.forEach { entry ->
        val doneSets = entry.sets.filter { it.completed }
        if (doneSets.isEmpty()) return@forEach

        val muscle = entry.exercise.muscleGroup.uppercase()
        val data = breakdown.getOrPut(muscle) { MuscleData() }
        data.sets += doneSets.size
        data.reps += doneSets.sumOf { it.reps }
        data.volume += doneSets.sumOf { it.weight * it.reps }
    }

    return breakdown.entries
        .sortedByDescending { it.value.sets }
        .associate { it.key to it.value }
}

// --- UI Components ---

@Composable
fun WorkoutSummaryCard(workout: WorkoutWithDetails) {
    val w = workout.workout
    var totalSets = 0
    var totalReps = 0
    var totalVolume = 0.0
    val exerciseCount = workout.exercises.size

    workout.exercises.forEach { entry ->
        val doneSets = entry.sets.filter { it.completed }
        totalSets += doneSets.size
        totalReps += doneSets.sumOf { it.reps }
        totalVolume += doneSets.sumOf { it.weight * it.reps }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Workout Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStatItem(label = "Duration", value = formatDuration(w.duration))
                SummaryStatItem(label = "Exercises", value = "$exerciseCount")
                SummaryStatItem(label = "Sets", value = "$totalSets")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStatItem(label = "Reps", value = "$totalReps")
                SummaryStatItem(label = "Volume", value = "%.1f kg".format(totalVolume))
                SummaryStatItem(label = "Est. Calories", value = "%.0f".format(totalVolume * 0.05))
            }
        }
    }
}

@Composable
private fun SummaryStatItem(label: String, value: String) {
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
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun MuscleGroupRow(
    muscleName: String,
    sets: Int,
    totalVolume: Double,
    totalReps: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = muscleName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(0.4f)
            )
            Text(
                text = "$sets sets",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(0.2f)
            )
            Text(
                text = "$totalReps reps",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(0.2f)
            )
            Text(
                text = "%.0f kg".format(totalVolume),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(0.2f)
            )
        }
    }
}

@Composable
fun WorkoutHeaderCard(workout: com.gymcoach.app.domain.model.Workout) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = workout.notes.ifBlank { "Workout" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatDate(workout.date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatItem(label = "Duration", value = formatDuration(workout.duration))
                StatItem(label = "Completed", value = if (workout.completed) "Yes" else "No")
                if (workout.notes.isNotBlank()) {
                    StatItem(label = "Notes", value = workout.notes)
                }
            }
        }
    }
}

@Composable
fun ExerciseDetailCard(exerciseWithSets: com.gymcoach.app.domain.model.WorkoutExerciseWithSets) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = exerciseWithSets.exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = exerciseWithSets.exercise.muscleGroup,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Set headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Set", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.15f))
                Text("Weight", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.2f))
                Text("Reps", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.2f))
                Text("RPE", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.15f))
                Text("Rest(s)", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.15f))
                Spacer(modifier = Modifier.width(24.dp))
            }

            exerciseWithSets.sets.sortedBy { it.setNumber }.forEach { set ->
                SetRow(
                    setNumber = set.setNumber,
                    weight = set.weight,
                    reps = set.reps,
                    rpe = set.rpe,
                    restSeconds = set.restSeconds,
                    completed = set.completed
                )
            }
        }
    }
}

@Composable
fun SetRow(
    setNumber: Int,
    weight: Double,
    reps: Int,
    rpe: Double,
    restSeconds: Int,
    completed: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = setNumber.toString(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.15f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "%.1f kg".format(weight),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.2f)
        )
        Text(
            text = reps.toString(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.2f)
        )
        Text(
            text = "RPE ${"%.1f".format(rpe)}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.15f)
        )
        Text(
            text = "${restSeconds}s",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.15f)
        )
        if (completed) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(24.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(24.dp))
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

fun formatDate(instant: java.time.Instant): String {
    val fmt = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return fmt.format(Date.from(instant))
}

fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
