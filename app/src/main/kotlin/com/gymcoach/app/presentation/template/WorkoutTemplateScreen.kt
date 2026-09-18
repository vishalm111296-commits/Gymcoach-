package com.gymcoach.app.presentation.template

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.domain.model.TemplateExercise
import com.gymcoach.app.domain.model.WorkoutTemplate
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes
import com.gymcoach.app.ui.theme.GymCoachSpacing




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTemplateScreen(
    onBackClick: () -> Unit,
    onStartWorkout: (Long) -> Unit,
    viewModel: WorkoutTemplateViewModel = hiltViewModel()
) {
    val templates by viewModel.templates.collectAsState()
    val archivedTemplates by viewModel.archivedTemplates.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val validationErrors by viewModel.validationErrors.collectAsState()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Active, 1 = Archived
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text("Template Error") },
            text = { Text(errorMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Templates", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editingTemplate = null
                        showCreateDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Template",
                            tint = GymCoachColors.CyanAccent
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTemplate = null
                    showCreateDialog = true
                },
                containerColor = GymCoachColors.CyanAccent,
                contentColor = GymCoachColors.SurfaceCardElevated
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Template")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Active (${templates.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Archived (${archivedTemplates.size})") }
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GymCoachColors.CyanAccent)
                }
            } else {
                val currentList = if (selectedTab == 0) templates else archivedTemplates

                if (currentList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (selectedTab == 0) "No active templates" else "No archived templates",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (selectedTab == 0) {
                                    "Build reusable workout templates with custom sets, reps, and exercises."
                                } else {
                                    "Archived templates will appear here."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (selectedTab == 0) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        editingTemplate = null
                                        showCreateDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GymCoachColors.CyanAccent,
                                        contentColor = GymCoachColors.SurfaceCardElevated
                                    )
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Create First Template", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.md)
                    ) {
                        items(currentList, key = { it.id }) { template ->
                            TemplateCard(
                                template = template,
                                isArchived = selectedTab == 1,
                                onStartWorkout = { viewModel.startWorkoutFromTemplate(template.id, onStartWorkout) },
                                onDuplicate = { viewModel.duplicateTemplate(template.id) },
                                onArchive = { viewModel.archiveTemplate(template.id) },
                                onUnarchive = { viewModel.unarchiveTemplate(template.id) },
                                onDelete = { viewModel.deleteTemplate(template.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateEditTemplateDialog(
            template = editingTemplate,
            validationErrors = validationErrors,
            onDismiss = {
                showCreateDialog = false
                viewModel.clearValidationErrors()
            },
            onSave = { name, description, exercises ->
                val t = editingTemplate?.copy(
                    name = name,
                    description = description
                ) ?: WorkoutTemplate(
                    name = name,
                    description = description
                )
                viewModel.saveTemplate(t, exercises) {
                    showCreateDialog = false
                    viewModel.clearValidationErrors()
                }
            }
        )
    }
}

@Composable
private fun TemplateCard(
    template: WorkoutTemplate,
    isArchived: Boolean,
    onStartWorkout: () -> Unit,
    onDuplicate: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = GymCoachShapes.lg,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(GymCoachSpacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (template.description.isNotBlank()) {
                        Text(
                            text = template.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onDuplicate) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Duplicate Template",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isArchived) {
                        IconButton(onClick = onUnarchive) {
                            Icon(
                                imageVector = Icons.Default.Unarchive,
                                contentDescription = "Unarchive Template",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        IconButton(onClick = onArchive) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = "Archive Template",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Template",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Exercise summary chips
            if (template.exercises.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    template.exercises.take(4).forEach { ex ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "• ${ex.exerciseName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${ex.targetSets} sets × ${ex.targetReps}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (template.exercises.size > 4) {
                        Text(
                            text = "+ ${template.exercises.size - 4} more exercises",
                            style = MaterialTheme.typography.labelSmall,
                            color = GymCoachColors.CyanAccent
                        )
                    }
                }
            } else {
                Text(
                    text = "No exercises added",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Start workout button
            if (!isArchived) {
                Button(
                    onClick = onStartWorkout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GymCoachColors.CyanAccent,
                        contentColor = GymCoachColors.SurfaceCardElevated
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Start Workout",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateEditTemplateDialog(
    template: WorkoutTemplate?,
    validationErrors: Map<String, String>,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, exercises: List<TemplateExercise>) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(template?.name ?: "") }
    var description by rememberSaveable { mutableStateOf(template?.description ?: "") }

    // Preset exercises list for easy selection
    var exercises by remember {
        mutableStateOf(
            template?.exercises ?: listOf(
                TemplateExercise(exerciseId = 1L, exerciseName = "Barbell Bench Press", targetSets = 3, targetReps = "8-12"),
                TemplateExercise(exerciseId = 4L, exerciseName = "Incline Dumbbell Press", targetSets = 3, targetReps = "10-12"),
                TemplateExercise(exerciseId = 14L, exerciseName = "Lateral Raise", targetSets = 4, targetReps = "12-15")
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (template == null) "Create Template" else "Edit Template",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Template Name *") },
                    placeholder = { Text("e.g., Push Day A") },
                    isError = validationErrors.containsKey("name"),
                    supportingText = validationErrors["name"]?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Focus on upper chest and lateral delts") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Exercises (${exercises.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (validationErrors.containsKey("exercises")) {
                    Text(
                        text = validationErrors["exercises"] ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                exercises.forEachIndexed { index, ex ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(GymCoachSpacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${index + 1}. ${ex.exerciseName}",
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${ex.targetSets} sets × ${ex.targetReps}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                exercises = exercises.filterIndexed { i, _ -> i != index }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Exercise",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, description, exercises) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GymCoachColors.CyanAccent,
                    contentColor = GymCoachColors.SurfaceCardElevated
                )
            ) {
                Text("Save Template", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
