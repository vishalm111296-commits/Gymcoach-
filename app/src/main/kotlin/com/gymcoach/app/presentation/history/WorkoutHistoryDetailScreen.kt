package com.gymcoach.app.presentation.history

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.domain.model.WorkoutWithDetails
import com.gymcoach.app.domain.model.WorkoutWithStats
import com.gymcoach.app.domain.repository.WorkoutRepository
import com.gymcoach.app.presentation.components.PremiumEmptyState
import com.gymcoach.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class WorkoutHistoryDetailViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
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

    fun cancelDelete() {
        _deleteTarget.value = null
    }
}

data class WorkoutHistoryDetailUiState(
    val isLoading: Boolean = true,
    val workout: WorkoutWithDetails? = null,
    val error: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryDetailScreen(
    workoutId: Long,
    onBackClick: () -> Unit,
    onEditClick: (Long) -> Unit = {},
    viewModel: WorkoutHistoryDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val deleteTarget by viewModel.deleteTarget.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(workoutId) {
        viewModel.loadWorkout(workoutId)
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("WORKOUT DETAILS", fontWeight = FontWeight.Black, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onDeleteClick(workoutId) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = ErrorRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            }
            state.error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    PremiumEmptyState("ERROR", state.error!!)
                }
            }
            else -> {
                state.workout?.let { workout ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Header
                        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                            Text(
                                text = workout.workout.notes.ifBlank { "TRAINING SESSION" }.uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = formatDate(workout.workout.date),
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextTertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val duration = formatDuration(workout.workout.duration)
                            val totalVolume = workout.exercises.sumOf { ex ->
                                ex.sets.filter { it.completed }.sumOf { it.weight * it.reps }
                            }
                            PremiumSummaryMetricNode("DURATION", duration, Modifier.weight(1f))
                            PremiumSummaryMetricNode("VOLUME", "${totalVolume.toInt()} kg", Modifier.weight(1f))
                        }

                        Spacer(Modifier.height(32.dp))

                        // Exercises
                        Text(
                            text = "EXERCISES",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextTertiary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp)
                        )

                        workout.exercises.forEach { we ->
                            PremiumCompletedExerciseCard(we)
                            Spacer(Modifier.height(16.dp))
                        }

                        Spacer(Modifier.height(48.dp))
                    }
                }
            }
        }
    }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("Delete Workout", color = TextPrimary) },
            text = { Text("Are you sure you want to delete this workout? This cannot be undone.", color = TextSecondary) },
            containerColor = DarkSurface,
            confirmButton = {
                Button(onClick = { viewModel.confirmDelete(); onBackClick() }, colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun PremiumSummaryMetricNode(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = AccentBlueLight,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun PremiumCompletedExerciseCard(exerciseDetails: com.gymcoach.app.domain.model.WorkoutExerciseWithSets) {
    val completedSets = exerciseDetails.sets.filter { it.completed }
    if (completedSets.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = exerciseDetails.exercise.name.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("SET", style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.weight(0.2f))
                Text("KG", style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.weight(0.4f))
                Text("REPS", style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.weight(0.4f))
            }

            completedSets.sortedBy { it.setNumber }.forEachIndexed { index, set ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen,
                        modifier = Modifier.weight(0.2f)
                    )
                    Text(
                        text = "${set.weight}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(0.4f)
                    )
                    Text(
                        text = "${set.reps}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(0.4f)
                    )
                }
            }
        }
    }
}

internal fun formatDate(dateMs: Instant): String {
    val formatter = SimpleDateFormat("MMM d, yyyy - h:mm a", Locale.getDefault())
    return formatter.format(Date(dateMs.toEpochMilli()))
}

fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return "--"
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    return if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
}
