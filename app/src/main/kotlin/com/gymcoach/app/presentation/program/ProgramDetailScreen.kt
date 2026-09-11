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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.entity.ProgramDayEntity
import com.gymcoach.app.data.local.entity.ProgramEntity
import com.gymcoach.app.data.local.entity.ProgramExerciseEntity
import com.gymcoach.app.domain.repository.CustomRoutineDay
import com.gymcoach.app.domain.repository.CustomRoutineExercise
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

data class ProgramExerciseDetail(
    val entity: ProgramExerciseEntity,
    val exerciseName: String,
    val muscleGroup: String,
    val equipment: String
)

data class ProgramDayWithExercises(
    val day: ProgramDayEntity,
    val exercises: List<ProgramExerciseDetail>
)

data class ProgramDetailUiState(
    val isLoading: Boolean = true,
    val program: ProgramEntity? = null,
    val daysWithExercises: List<ProgramDayWithExercises> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ProgramDetailViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    private val exerciseDao: ExerciseDao
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
                        val detailedExercises = exercises.map { pe ->
                            val exEntity = exerciseDao.getById(pe.exerciseId).firstOrNull()
                            ProgramExerciseDetail(
                                entity = pe,
                                exerciseName = exEntity?.name ?: "Exercise #${pe.exerciseId}",
                                muscleGroup = exEntity?.muscleGroup ?: "General",
                                equipment = exEntity?.equipment ?: "Standard"
                            )
                        }
                        ProgramDayWithExercises(day, detailedExercises)
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

    fun createCustomRoutine(name: String, description: String, goal: String, days: List<CustomRoutineDay>) {
        viewModelScope.launch {
            try {
                programRepository.saveCustomRoutine(
                    name = name,
                    description = description,
                    goal = goal,
                    days = days,
                    setAsActive = true
                )
                loadActiveProgram()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to create routine")
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
    var showBuilderSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Training Program", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = { showBuilderSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = "New Routine", tint = MaterialTheme.colorScheme.primary)
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "No active training program.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Button(onClick = { showBuilderSheet = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Create Custom Routine")
                        }
                    }
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
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(program.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("ACTIVE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Text(program.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Goal: ${program.goal}", style = MaterialTheme.typography.labelMedium)
                                    Text("Frequency: ${program.daysPerWeek} days/week", style = MaterialTheme.typography.labelMedium)
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(onClick = onStartWorkout, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Filled.FitnessCenter, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Start Workout")
                                    }
                                    OutlinedButton(onClick = { showBuilderSheet = true }) {
                                        Text("New Routine")
                                    }
                                }
                            }
                        }
                    }

                    items(state.daysWithExercises) { dayWithEx ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Day ${dayWithEx.day.dayNumber}: ${dayWithEx.day.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(dayWithEx.day.targetMuscles, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    dayWithEx.exercises.forEachIndexed { idx, ex ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${idx + 1}. ${ex.exerciseName}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "${ex.muscleGroup} • ${ex.equipment}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "${ex.entity.sets} sets × ${ex.entity.targetReps}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Timer,
                                                        contentDescription = "Rest",
                                                        modifier = Modifier.size(12.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(Modifier.width(3.dp))
                                                    Text(
                                                        text = "${ex.entity.restSeconds}s",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
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

    if (showBuilderSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showBuilderSheet = false },
            sheetState = sheetState
        ) {
            RoutineBuilderContent(
                onDismiss = { showBuilderSheet = false },
                onSave = { name, desc, goal, days ->
                    viewModel.createCustomRoutine(name, desc, goal, days)
                    showBuilderSheet = false
                }
            )
        }
    }
}

@Composable
private fun RoutineBuilderContent(
    onDismiss: () -> Unit,
    onSave: (String, String, String, List<CustomRoutineDay>) -> Unit
) {
    var routineName by rememberSaveable { mutableStateOf("My Custom Routine") }
    var description by rememberSaveable { mutableStateOf("Tailored personalized workout routine") }
    var goal by rememberSaveable { mutableStateOf("Hypertrophy") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Routine Builder",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = routineName,
            onValueChange = { routineName = it },
            label = { Text("Routine Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = goal,
            onValueChange = { goal = it },
            label = { Text("Primary Goal (Hypertrophy / Strength / Endurance)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text(
            text = "Sample Day 1: Upper Body Power",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "3 exercises: Bench Press (1), Bent Over Row (3), Overhead Press (8)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val defaultDays = listOf(
                        CustomRoutineDay(
                            dayNumber = 1,
                            name = "Upper Body Focus",
                            targetMuscles = "Chest,Back,Shoulders",
                            exercises = listOf(
                                CustomRoutineExercise(exerciseId = 1L, targetSets = 3, targetReps = "8-12", restSeconds = 90),
                                CustomRoutineExercise(exerciseId = 3L, targetSets = 3, targetReps = "8-12", restSeconds = 90),
                                CustomRoutineExercise(exerciseId = 8L, targetSets = 3, targetReps = "10-12", restSeconds = 60)
                            )
                        ),
                        CustomRoutineDay(
                            dayNumber = 2,
                            name = "Lower Body Focus",
                            targetMuscles = "Legs,Core",
                            exercises = listOf(
                                CustomRoutineExercise(exerciseId = 2L, targetSets = 3, targetReps = "6-10", restSeconds = 120),
                                CustomRoutineExercise(exerciseId = 4L, targetSets = 3, targetReps = "8-12", restSeconds = 90)
                            )
                        )
                    )
                    onSave(routineName, description, goal, defaultDays)
                }
            ) {
                Text("Create Routine")
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
