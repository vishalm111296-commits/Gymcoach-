package com.gymcoach.app.presentation.program

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.data.local.entity.ProgramDayEntity
import com.gymcoach.app.data.local.entity.ProgramEntity
import com.gymcoach.app.data.local.entity.ProgramExerciseEntity
import com.gymcoach.app.domain.repository.ProgramRepository
import com.gymcoach.app.ui.theme.DarkBackground
import com.gymcoach.app.ui.theme.DarkSurface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgramDayWithExercises(
    val day: ProgramDayEntity,
    val exercises: List<ProgramExerciseEntity>
)

data class ProgramDetailUiState(
    val isLoading: Boolean = true,
    val program: ProgramEntity? = null,
    val daysWithExercises: List<ProgramDayWithExercises> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ProgramDetailViewModel @Inject constructor(
    private val programRepository: ProgramRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProgramDetailUiState())
    val uiState: StateFlow<ProgramDetailUiState> = _uiState.asStateFlow()

    init {
        loadActiveProgram()
    }

    fun loadActiveProgram() {
        viewModelScope.launch {
            _uiState.value = ProgramDetailUiState(isLoading = true)
            try {
                val activeProgram = programRepository.getActiveProgram().firstOrNull()
                if (activeProgram == null) {
                    _uiState.value = ProgramDetailUiState(isLoading = false, program = null)
                } else {
                    val days = programRepository.getDaysForProgram(activeProgram.id).firstOrNull() ?: emptyList()
                    val daysWithEx = days.map { day ->
                        val exercises = programRepository.getExercisesForDay(day.id).firstOrNull() ?: emptyList()
                        ProgramDayWithExercises(day, exercises)
                    }
                    _uiState.value = ProgramDetailUiState(
                        isLoading = false,
                        program = activeProgram,
                        daysWithExercises = daysWithEx
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProgramDetailUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load active program"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramDetailScreen(
    onBackClick: () -> Unit,
    onStartWorkout: () -> Unit = {},
    viewModel: ProgramDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Active Training Program", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.program == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No active program found. Complete onboarding to generate one.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                }
            }
            else -> {
                val program = state.program!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(program.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(program.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Goal: ${program.goal}", style = MaterialTheme.typography.labelMedium)
                                    Text("Frequency: ${program.daysPerWeek} days/week", style = MaterialTheme.typography.labelMedium)
                                }
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = onStartWorkout, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Filled.FitnessCenter, contentDescription = null)
                                    Spacer(Modifier.padding(horizontal = 4.dp))
                                    Text("Start Today's Workout")
                                }
                            }
                        }
                    }

                    items(state.daysWithExercises) { dayWithEx ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Day ${dayWithEx.day.dayNumber}: ${dayWithEx.day.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Target Muscles: ${dayWithEx.day.targetMuscles}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(4.dp))
                                dayWithEx.exercises.forEach { ex ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("• Exercise #${ex.exerciseId}", style = MaterialTheme.typography.bodyMedium)
                                        Text("${ex.sets} sets × ${ex.targetReps} reps (${ex.restSeconds}s rest)", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}
