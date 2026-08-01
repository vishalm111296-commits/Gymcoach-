package com.gymcoach.app.presentation.history

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
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
                val workout = workoutRepository.getWorkoutWithDetails(workoutId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null).value
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
    viewModel: WorkoutHistoryDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val showDeleteConfirmation by viewModel.showDeleteConfirmation.collectAsState()

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
                        text = state.error!!,
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

    // Delete confirmation dialog
    if (viewModel.deleteTarget.collectAsState() != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("Delete Workout") },
            text = { Text("Are you sure you want to delete this workout?") },
            confirmButton = {
                Button(onClick = { viewModel.confirmDelete() }) {
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
                Text("Set", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.3f))
                Text("Weight", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.35f))
                Text("Reps", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.35f))
            }

            exerciseWithSets.sets.sortedBy { it.setNumber }.forEach { set ->
                SetRow(
                    setNumber = set.setNumber,
                    weight = set.weight,
                    reps = set.reps,
                    rpe = set.rpe,
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
            modifier = Modifier.weight(0.3f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "%.1f kg".format(weight),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = reps.toString(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.35f)
        )
        // RPE
        Text(
            text = "RPE ${"%.1f".format(rpe)}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.35f)
        )
        // Completed indicator
        if (completed) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(24.dp)
            )
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

}
