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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

    fun loadWorkout(id: Long) {
        viewModelScope.launch {
            try {
                workoutRepository.getWorkoutWithDetails(id).collect { workout ->
                    if (workout != null) {
                        _uiState.value = WorkoutHistoryDetailUiState(
                            isLoading = false,
                            workout = workout
                        )
                    } else {
                        _uiState.value = WorkoutHistoryDetailUiState(
                            isLoading = false,
                            error = "Workout not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = WorkoutHistoryDetailUiState(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun deleteWorkout(id: Long) {
        viewModelScope.launch {
            try {
                workoutRepository.deleteWorkout(id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
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
    onPerformAgain: (Long) -> Unit = {},
    viewModel: WorkoutHistoryDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

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
                    if (state.workout != null) {
                        IconButton(onClick = {
                            shareWorkout(context, state.workout!!)
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share")
                        }
                        IconButton(onClick = { onEditClick(workoutId) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.error != null || state.workout == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = state.error ?: "Workout not found", color = MaterialTheme.colorScheme.error)
            }
        } else {
            val workout = state.workout!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { onPerformAgain(workoutId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.gymcoach.app.ui.theme.AccentBlue,
                        contentColor = com.gymcoach.app.ui.theme.WarmWhite
                    )
                ) {
                    Text("Perform Again", fontWeight = FontWeight.Bold)
                }

                WorkoutSummaryCard(workout)

                Text(
                    text = "Exercises",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                workout.exercises.forEach { exerciseWithSets ->
                    ExerciseDetailCard(exerciseWithSets)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Workout") },
            text = { Text("Are you sure you want to delete this workout? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteWorkout(workoutId)
                        onBackClick()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun WorkoutSummaryCard(workout: WorkoutWithDetails) {
    val formatter = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
    val dateStr = formatter.format(Date(workout.workout.date.toEpochMilli()))
    val durationStr = formatDuration(workout.workout.duration)

    val totalVolume = workout.exercises.sumOf { ex ->
        ex.sets.filter { it.completed }.sumOf { it.weight * it.reps }
    }
    val totalSets = workout.exercises.sumOf { ex ->
        ex.sets.count { it.completed }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = dateStr, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (workout.workout.duration > 0) {
                Text(text = "Duration: $durationStr", style = MaterialTheme.typography.bodyMedium)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total Volume", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${totalVolume} kg", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Sets", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$totalSets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ExerciseDetailCard(exerciseWithSets: com.gymcoach.app.domain.model.WorkoutExerciseWithSets) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = exerciseWithSets.exercise.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            // Table header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Set", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                Text("Weight", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(2f))
                Text("Reps", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(2f))
                Text("RPE", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(4.dp))

            // Sets
            exerciseWithSets.sets.forEach { set ->
                val color = if (set.completed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${set.setNumber}", style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.weight(1f))
                    Text("${set.weight} kg", style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.weight(2f))
                    Text("${set.reps}", style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.weight(2f))
                    Text(if (set.rpe > 0) "${set.rpe}" else "-", style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

fun formatDuration(seconds: Long): String {
    val d = Duration.ofSeconds(seconds)
    val h = d.toHours()
    val m = d.toMinutes() % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun shareWorkout(context: Context, workoutWithDetails: WorkoutWithDetails) {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val dateStr = formatter.format(Date(workoutWithDetails.workout.date.toEpochMilli()))

    val totalVolume = workoutWithDetails.exercises.sumOf { ex ->
        ex.sets.filter { it.completed }.sumOf { it.weight * it.reps }
    }

    val shareText = buildString {
        appendLine("Workout on $dateStr")
        appendLine("Total Volume: $totalVolume kg")
        appendLine()
        workoutWithDetails.exercises.forEach { ex ->
            appendLine(ex.exercise.name)
            val completedSets = ex.sets.filter { it.completed }
            if (completedSets.isNotEmpty()) {
                val bestSet = completedSets.maxByOrNull { it.weight }
                appendLine("Best: ${bestSet?.weight}kg x ${bestSet?.reps}")
            }
        }
    }

    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}
