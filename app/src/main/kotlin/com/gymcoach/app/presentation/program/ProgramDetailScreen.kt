package com.gymcoach.app.presentation.program

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import com.gymcoach.app.ui.GymCoachBottomNav
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.remember
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
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.ProgramDayEntity
import com.gymcoach.app.data.local.entity.ProgramEntity
import com.gymcoach.app.data.local.entity.ProgramExerciseEntity
import com.gymcoach.app.domain.repository.CustomRoutineDay
import com.gymcoach.app.domain.repository.CustomRoutineExercise
import com.gymcoach.app.domain.repository.ProgramRepository
import com.gymcoach.app.ui.theme.GymCoachBorders
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes
import com.gymcoach.app.ui.theme.GymCoachSpacing

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
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
    private val exerciseDao: ExerciseDao,
    private val workoutRepository: com.gymcoach.app.domain.repository.WorkoutRepository,
    private val programGenerator: com.gymcoach.app.core.program.ProgramGenerator
) : ViewModel() {

    constructor(
        programRepository: ProgramRepository,
        exerciseDao: ExerciseDao,
        workoutRepository: com.gymcoach.app.domain.repository.WorkoutRepository
    ) : this(
        programRepository,
        exerciseDao,
        workoutRepository,
        com.gymcoach.app.core.program.ProgramGenerator(
            exerciseDao = exerciseDao,
            equipmentAvailability = com.gymcoach.app.core.exercise.EquipmentAvailability(),
            readinessRepository = object : com.gymcoach.app.domain.repository.ReadinessRepository {
                override fun getAllReadiness() = kotlinx.coroutines.flow.emptyFlow<List<com.gymcoach.app.data.local.entity.ReadinessEntity>>()
                override fun getLatestReadiness() = kotlinx.coroutines.flow.flowOf(null)
                override fun getReadinessInRange(startTime: Long, endTime: Long) = kotlinx.coroutines.flow.emptyFlow<List<com.gymcoach.app.data.local.entity.ReadinessEntity>>()
                override fun getRecentReadiness(since: Long) = kotlinx.coroutines.flow.emptyFlow<List<com.gymcoach.app.data.local.entity.ReadinessEntity>>()
                override suspend fun saveReadiness(readiness: com.gymcoach.app.data.local.entity.ReadinessEntity) = 0L
                override suspend fun updateReadiness(readiness: com.gymcoach.app.data.local.entity.ReadinessEntity) {}
                override suspend fun deleteReadiness(id: Long) {}
            }
        )
    )

    private val _uiState = MutableStateFlow(ProgramDetailUiState())
    val uiState: StateFlow<ProgramDetailUiState> = _uiState.asStateFlow()

    val availableExercises: StateFlow<List<ExerciseEntity>> = exerciseDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadActiveProgram()
    }

    fun generateAndActivateProgram(frequency: Int, equipmentType: String, goal: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val generated = programGenerator.generateProgram(
                    frequency = frequency,
                    equipmentType = equipmentType,
                    goal = goal
                )
                programRepository.saveGeneratedProgram(generated)
                loadActiveProgram()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to generate program")
            }
        }
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
                    val dayIds = days.map { it.id }
                    val exercisesByDay = if (dayIds.isNotEmpty()) {
                        programRepository.getExercisesForDays(dayIds).firstOrNull() ?: emptyMap()
                    } else {
                        emptyMap()
                    }
                    val allExercisesMap = exerciseDao.getAll().firstOrNull()?.associateBy { it.id } ?: emptyMap()
                    val daysWithEx = days.map { day ->
                        val exercises = exercisesByDay[day.id] ?: emptyList()
                        val detailedExercises = exercises.map { pe ->
                            val exEntity = allExercisesMap[pe.exerciseId]
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
            } catch (e: CancellationException) {
                throw e
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to create routine")
            }
        }
    }

    fun startWorkoutForDay(dayId: Long, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            try {
                val newId = workoutRepository.createWorkoutFromProgramDay(dayId)
                if (newId != null) {
                    onCreated(newId)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to start workout")
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramDetailScreen(
    onBackClick: () -> Unit,
    onStartWorkout: (Long) -> Unit = {},
    onNavigateBottomBar: (String) -> Unit = {},
    viewModel: ProgramDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val availableExercises by viewModel.availableExercises.collectAsState()
    var showBuilderSheet by rememberSaveable { mutableStateOf(false) }
    var showGenerateSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = GymCoachColors.PureDark,
        bottomBar = {
            GymCoachBottomNav(currentRoute = "program_detail", onNavigate = onNavigateBottomBar)
        },
        topBar = {
            TopAppBar(
                title = { Text("Training Program", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = GymCoachColors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GymCoachColors.TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showBuilderSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = "New Routine", tint = GymCoachColors.Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GymCoachColors.PureDark)
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GymCoachColors.Primary)
                }
            }
            state.error != null && state.program == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding).padding(GymCoachSpacing.xxl), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.lg)) {
                        Text(
                            text = state.error ?: "Failed to load program",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.loadActiveProgram() },
                            shape = GymCoachShapes.md,
                            colors = ButtonDefaults.buttonColors(containerColor = GymCoachColors.Primary)
                        ) {
                            Text("Retry", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            state.program == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding).padding(GymCoachSpacing.xxl), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.lg)) {
                        Text(
                            "No active training program.",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = GymCoachColors.TextPrimary
                        )
                        Button(
                            onClick = { showBuilderSheet = true },
                            shape = GymCoachShapes.md,
                            colors = ButtonDefaults.buttonColors(containerColor = GymCoachColors.Primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Create Custom Routine", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { showGenerateSheet = true },
                            shape = GymCoachShapes.md,
                            border = GymCoachBorders.subtle,
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = GymCoachColors.TextPrimary)
                        ) {
                            Text("Adaptive Program Generator", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            else -> {
                val program = state.program!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.lg)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
                            shape = GymCoachShapes.lg,
                            border = GymCoachBorders.subtle
                        ) {
                            Column(modifier = Modifier.padding(GymCoachSpacing.lg), verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(program.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = GymCoachColors.TextPrimary)
                                    Box(
                                        modifier = Modifier
                                            .clip(GymCoachShapes.xs)
                                            .background(GymCoachColors.Primary.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("ACTIVE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = GymCoachColors.Primary)
                                    }
                                }
                                Text(program.description, style = MaterialTheme.typography.bodyMedium, color = GymCoachColors.TextSecondary)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Goal: ${program.goal}", style = MaterialTheme.typography.labelMedium, color = GymCoachColors.TextSecondary)
                                    Text("Frequency: ${program.daysPerWeek} days/week", style = MaterialTheme.typography.labelMedium, color = GymCoachColors.TextSecondary)
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GymCoachSpacing.md)) {
                                    Button(
                                        onClick = {
                                            val firstDay = state.daysWithExercises.firstOrNull()?.day?.id
                                            if (firstDay != null) {
                                                viewModel.startWorkoutForDay(firstDay, onStartWorkout)
                                            }
                                        },
                                        shape = GymCoachShapes.md,
                                        colors = ButtonDefaults.buttonColors(containerColor = GymCoachColors.Primary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.FitnessCenter, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Start Workout", fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { showBuilderSheet = true },
                                        shape = GymCoachShapes.md,
                                        border = GymCoachBorders.subtle,
                                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = GymCoachColors.TextPrimary)
                                    ) {
                                        Text("New Routine")
                                    }
                                    OutlinedButton(
                                        onClick = { showGenerateSheet = true },
                                        shape = GymCoachShapes.md,
                                        border = GymCoachBorders.subtle,
                                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = GymCoachColors.TextPrimary)
                                    ) {
                                        Text("Adaptive")
                                    }
                                }
                            }
                        }
                    }

                    items(state.daysWithExercises) { dayWithEx ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
                            shape = GymCoachShapes.lg,
                            border = GymCoachBorders.subtle
                        ) {
                            Column(modifier = Modifier.padding(GymCoachSpacing.lg), verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.md)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Day ${dayWithEx.day.dayNumber}: ${dayWithEx.day.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GymCoachColors.TextPrimary)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(GymCoachShapes.xs)
                                                .background(GymCoachColors.Primary.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(dayWithEx.day.targetMuscles, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = GymCoachColors.Primary)
                                        }
                                        Button(
                                            onClick = { viewModel.startWorkoutForDay(dayWithEx.day.id, onStartWorkout) },
                                            modifier = Modifier.height(32.dp),
                                            shape = GymCoachShapes.xs,
                                            colors = ButtonDefaults.buttonColors(containerColor = GymCoachColors.Primary),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                        ) {
                                            Text("Start", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm)) {
                                    dayWithEx.exercises.forEachIndexed { idx, ex ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(GymCoachShapes.sm)
                                                .background(GymCoachColors.SurfaceDeep)
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${idx + 1}. ${ex.exerciseName}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = GymCoachColors.TextPrimary
                                                )
                                                Text(
                                                    text = "${ex.muscleGroup} • ${ex.equipment}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = GymCoachColors.TextSecondary
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "${ex.entity.sets} sets × ${ex.entity.targetReps}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GymCoachColors.Primary
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Timer,
                                                        contentDescription = "Rest",
                                                        modifier = Modifier.size(12.dp),
                                                        tint = GymCoachColors.TextMuted
                                                    )
                                                    Spacer(Modifier.width(3.dp))
                                                    Text(
                                                        text = "${ex.entity.restSeconds}s",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = GymCoachColors.TextSecondary
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


    if (showGenerateSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showGenerateSheet = false },
            sheetState = sheetState
        ) {
            GenerateProgramBottomSheet(
                onDismiss = { showGenerateSheet = false },
                onGenerate = { freq, eq, goal ->
                    viewModel.generateAndActivateProgram(freq, eq, goal)
                }
            )
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
                availableExercises = availableExercises,
                onSave = { name, desc, goal, days ->
                    viewModel.createCustomRoutine(name, desc, goal, days)
                    showBuilderSheet = false
                }
            )
        }
    }

    state.error?.let { err ->
        if (state.program != null) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissError() },
                title = { Text("Program Error") },
                text = { Text(err) },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissError() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

data class EditableExercise(
    val id: String = java.util.UUID.randomUUID().toString(),
    val exerciseId: Long,
    val exerciseName: String,
    val muscleGroup: String,
    val sets: Int = 3,
    val reps: String = "8-12",
    val restSeconds: Int = 90
)

data class EditableDay(
    val id: String = java.util.UUID.randomUUID().toString(),
    val dayNumber: Int,
    val name: String,
    val targetMuscles: String,
    val exercises: List<EditableExercise> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineBuilderContent(
    onDismiss: () -> Unit,
    availableExercises: List<ExerciseEntity>,
    onSave: (String, String, String, List<CustomRoutineDay>) -> Unit
) {
    var routineName by remember { mutableStateOf<String>("My Custom Routine") }
    var description by remember { mutableStateOf<String>("Personalized custom workout program") }
    var goal by remember { mutableStateOf<String>("Hypertrophy") }
    var days by remember {
        mutableStateOf<List<EditableDay>>(
            listOf(
                EditableDay(
                    dayNumber = 1,
                    name = "Day 1 - Full Body",
                    targetMuscles = "Chest, Back, Legs"
                )
            )
        )
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pickerDayIndex by remember { mutableStateOf<Int?>(null) }
    var pickerSearch by remember { mutableStateOf<String>("") }
    var pickerCategory by remember { mutableStateOf<String>("All") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.lg)
    ) {
        Text(
            text = "Routine Builder",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(GymCoachSpacing.md)
                )
            }
        }

        OutlinedTextField(
            value = routineName,
            onValueChange = { raw: String -> routineName = raw; errorMessage = null },
            label = { Text("Routine Name *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = description,
            onValueChange = { raw: String -> description = raw },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text("Primary Goal", style = MaterialTheme.typography.titleSmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            listOf("Hypertrophy", "Strength", "Endurance", "Fat Loss").forEach { g ->
                FilterChip(
                    selected = goal == g,
                    onClick = { goal = g },
                    label = { Text(g) }
                )
            }
        }

        Text(
            text = "Workout Days (${days.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        for ((dayIndex, day) in days.withIndex()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
                shape = GymCoachShapes.md,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Day ${day.dayNumber}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (days.size > 1) {
                            IconButton(
                                onClick = {
                                    days = days.filterIndexed { i, _ -> i != dayIndex }
                                        .mapIndexed { i, d -> d.copy(dayNumber = i + 1) }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove Day",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = day.name,
                        onValueChange = { newName: String ->
                            days = days.toMutableList().also { it[dayIndex] = day.copy(name = newName) }
                            errorMessage = null
                        },
                        label = { Text("Day Title (e.g. Upper Body)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = day.targetMuscles,
                        onValueChange = { newMuscles: String ->
                            days = days.toMutableList().also { it[dayIndex] = day.copy(targetMuscles = newMuscles) }
                        },
                        label = { Text("Focus / Target Muscles") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text(
                        text = "Exercises (${day.exercises.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (day.exercises.isEmpty()) {
                        Text(
                            text = "No exercises added yet. Tap '+ Add Exercise' below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        for ((exIndex, ex) in day.exercises.withIndex()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = GymCoachShapes.sm,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = ex.exerciseName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = ex.muscleGroup,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                val updatedEx = day.exercises.filterIndexed { i, _ -> i != exIndex }
                                                days = days.toMutableList().also { it[dayIndex] = day.copy(exercises = updatedEx) }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove Exercise",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm)
                                    ) {
                                        OutlinedTextField(
                                            value = ex.sets.toString(),
                                            onValueChange = { raw: String ->
                                                val parsed = raw.filter { c -> c.isDigit() }.toIntOrNull() ?: 1
                                                val updatedEx = day.exercises.toMutableList().also {
                                                     it[exIndex] = ex.copy(sets = parsed)
                                                }
                                                days = days.toMutableList().also { it[dayIndex] = day.copy(exercises = updatedEx) }
                                            },
                                            label = { Text("Sets") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = ex.reps,
                                            onValueChange = { raw: String ->
                                                val updatedEx = day.exercises.toMutableList().also {
                                                     it[exIndex] = ex.copy(reps = raw)
                                                }
                                                days = days.toMutableList().also { it[dayIndex] = day.copy(exercises = updatedEx) }
                                            },
                                            label = { Text("Reps") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = ex.restSeconds.toString(),
                                            onValueChange = { raw: String ->
                                                val parsed = raw.filter { c -> c.isDigit() }.toIntOrNull() ?: 90
                                                val updatedEx = day.exercises.toMutableList().also {
                                                     it[exIndex] = ex.copy(restSeconds = parsed)
                                                }
                                                days = days.toMutableList().also { it[dayIndex] = day.copy(exercises = updatedEx) }
                                            },
                                            label = { Text("Rest (s)") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            pickerDayIndex = dayIndex
                            pickerSearch = ""
                            pickerCategory = "All"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Exercise to Day ${day.dayNumber}")
                    }
                }
            }
        }

        OutlinedButton(
            onClick = {
                days = days + EditableDay(
                    dayNumber = days.size + 1,
                    name = "Day ${days.size + 1}",
                    targetMuscles = ""
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add Another Training Day")
        }

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
                    if (routineName.isBlank()) {
                        errorMessage = "Please enter a routine name."
                        return@Button
                    }
                    if (days.isEmpty()) {
                        errorMessage = "Please add at least one workout day."
                        return@Button
                    }
                    val emptyDay = days.firstOrNull { it.exercises.isEmpty() }
                    if (emptyDay != null) {
                        errorMessage = "Day ${emptyDay.dayNumber} has no exercises. Please add at least one exercise."
                        return@Button
                    }
                    val invalidSets = days.any { d -> d.exercises.any { it.sets <= 0 } }
                    if (invalidSets) {
                        errorMessage = "All exercises must have at least 1 set."
                        return@Button
                    }

                    val customDays = days.mapIndexed { dayIdx, day ->
                        CustomRoutineDay(
                            dayNumber = dayIdx + 1,
                            name = day.name.ifBlank { "Day ${dayIdx + 1}" },
                            targetMuscles = day.targetMuscles.ifBlank { "Full Body" },
                            exercises = day.exercises.map { ex ->
                                CustomRoutineExercise(
                                    exerciseId = ex.exerciseId,
                                    targetSets = ex.sets.coerceAtLeast(1),
                                    targetReps = ex.reps.ifBlank { "8-12" },
                                    restSeconds = ex.restSeconds.coerceAtLeast(30)
                                )
                            }
                        )
                    }
                    onSave(routineName.trim(), description.trim(), goal.trim(), customDays)
                }
            ) {
                Text("Save & Activate Routine")
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    if (pickerDayIndex != null) {
        val targetDayIdx = pickerDayIndex ?: 0
        AlertDialog(
            onDismissRequest = { pickerDayIndex = null },
            title = {
                Text(
                    text = "Select Exercise (Day ${targetDayIdx + 1})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm)
                ) {
                    OutlinedTextField(
                        value = pickerSearch,
                        onValueChange = { raw: String -> pickerSearch = raw },
                        label = { Text("Search exercise name...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        listOf("All", "Chest", "Back", "Legs", "Shoulders", "Arms", "Core").forEach { cat ->
                            FilterChip(
                                selected = pickerCategory == cat,
                                onClick = { pickerCategory = cat },
                                label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    val filteredExercises = availableExercises.filter { ex ->
                        val matchesCat = pickerCategory == "All" || ex.muscleGroup.lowercase().contains(pickerCategory.lowercase())
                        val matchesQuery = pickerSearch.isBlank() ||
                                ex.name.lowercase().contains(pickerSearch.lowercase()) ||
                                ex.muscleGroup.lowercase().contains(pickerSearch.lowercase())
                        matchesCat && matchesQuery
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (filteredExercises.isEmpty()) {
                            item {
                                Text(
                                    text = "No matching exercises found.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(GymCoachSpacing.lg)
                                )
                            }
                        } else {
                            items(filteredExercises) { exercise ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
                                    shape = GymCoachShapes.sm,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val currentDay = days[targetDayIdx]
                                            val updatedExercises = currentDay.exercises + EditableExercise(
                                                exerciseId = exercise.id,
                                                exerciseName = exercise.name,
                                                muscleGroup = exercise.muscleGroup,
                                                sets = 3,
                                                reps = "8-12",
                                                restSeconds = 90
                                            )
                                            val updatedMuscles = if (currentDay.targetMuscles.isBlank()) {
                                                exercise.muscleGroup
                                            } else if (!currentDay.targetMuscles.lowercase().contains(exercise.muscleGroup.lowercase())) {
                                                "${currentDay.targetMuscles}, ${exercise.muscleGroup}"
                                            } else {
                                                currentDay.targetMuscles
                                            }
                                            days = days.toMutableList().also {
                                                it[targetDayIdx] = currentDay.copy(
                                                    exercises = updatedExercises,
                                                    targetMuscles = updatedMuscles
                                                )
                                            }
                                            errorMessage = null
                                            pickerDayIndex = null
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(GymCoachSpacing.md),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = exercise.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${exercise.muscleGroup} • ${exercise.equipment}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickerDayIndex = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenerateProgramBottomSheet(
    onDismiss: () -> Unit,
    onGenerate: (Int, String, String) -> Unit
) {
    var goal by rememberSaveable { mutableStateOf("Hypertrophy") }
    var frequency by rememberSaveable { mutableStateOf(4) }
    var equipment by rememberSaveable { mutableStateOf("gym") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.lg)
    ) {
        Text(
            text = "Adaptive Program Generator",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text("Primary Goal", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf("Hypertrophy", "Strength", "Fat Loss", "Endurance").forEach { g ->
                FilterChip(
                    selected = goal == g,
                    onClick = { goal = g },
                    label = { Text(g) }
                )
            }
        }

        Text("Training Days / Week", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf(2, 3, 4, 5, 6).forEach { f ->
                FilterChip(
                    selected = frequency == f,
                    onClick = { frequency = f },
                    label = { Text("$f Days") }
                )
            }
        }

        Text("Available Equipment", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(selected = equipment == "gym", onClick = { equipment = "gym" }, label = { Text("Full Gym") })
            FilterChip(selected = equipment == "home", onClick = { equipment = "home" }, label = { Text("Dumbbells/Bands") })
            FilterChip(selected = equipment == "custom", onClick = { equipment = "custom" }, label = { Text("Bodyweight") })
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    onGenerate(frequency, equipment, goal)
                    onDismiss()
                }
            ) {
                Text("Generate & Activate Program")
            }
        }
        
        Spacer(Modifier.height(32.dp))
    }
}
