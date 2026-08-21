package com.gymcoach.app.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.domain.model.UserProfile
import androidx.compose.ui.text.input.ImeAction
import com.gymcoach.app.presentation.components.LoadingState
import com.gymcoach.app.presentation.components.ErrorState
import com.gymcoach.app.presentation.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onMeasurementsClick: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
    analyticsViewModel: ProfileAnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val analyticsState by analyticsViewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.profile != null) {
                        if (state.isEditing) {
                            IconButton(onClick = viewModel::onSaveProfile, enabled = !state.isSaving) {
                                Icon(Icons.Filled.Check, contentDescription = "Save Profile")
                            }
                            IconButton(onClick = viewModel::onCancelEdit, enabled = !state.isSaving) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancel Editing")
                            }
                        } else {
                            IconButton(onClick = viewModel::onEditClick) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit Profile")
                            }
                        }
                        IconButton(onClick = onMeasurementsClick) {
                            Icon(Icons.Filled.Straighten, contentDescription = "Measurements")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            when {
                state.isLoading -> LoadingState(modifier = Modifier.fillMaxSize())
                state.error != null -> ErrorState(message = state.error ?: "", onRetry = viewModel::onRetry, modifier = Modifier.fillMaxSize())
                state.profile == null && !state.isEditing -> {
                    EmptyState(
                        message = "No profile found. Let's create one!",
                        onPrimaryAction = viewModel::onEditClick,
                        primaryActionLabel = "Create Profile",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> ProfileContent(state = state, viewModel = viewModel, analyticsState = analyticsState)
            }
        }
    }
}

@Composable
fun ProfileContent(state: ProfileUiState, viewModel: ProfileViewModel, analyticsState: ProfileAnalyticsUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(120.dp)
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = "Profile Avatar",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.height(8.dp))

        // Profile Fields
        ProfileField(
            label = "Name",
            value = state.form.name,
            onValueChange = { viewModel.onFormFieldChange(ProfileFormField.Name, it) },
            isEditable = state.isEditing,
            keyboardType = KeyboardType.Text,
            errorMessage = state.formErrors[ProfileFormField.Name]
        )
        ProfileField(
            label = "Age",
            value = state.form.age.toString(),
            onValueChange = { viewModel.onFormFieldChange(ProfileFormField.Age, it) },
            isEditable = state.isEditing,
            keyboardType = KeyboardType.Number,
            errorMessage = state.formErrors[ProfileFormField.Age]
        )
        ProfileField(
            label = "Gender",
            value = state.form.gender,
            onValueChange = { viewModel.onFormFieldChange(ProfileFormField.Gender, it) },
            isEditable = state.isEditing,
            keyboardType = KeyboardType.Text,
            errorMessage = state.formErrors[ProfileFormField.Gender]
        )
        ProfileField(
            label = "Height (cm)",
            value = state.form.height.toString(),
            onValueChange = { viewModel.onFormFieldChange(ProfileFormField.Height, it) },
            isEditable = state.isEditing,
            keyboardType = KeyboardType.Number,
            errorMessage = state.formErrors[ProfileFormField.Height]
        )
        ProfileField(
            label = "Weight (kg)",
            value = state.form.weight.toString(),
            onValueChange = { viewModel.onFormFieldChange(ProfileFormField.Weight, it) },
            isEditable = state.isEditing,
            keyboardType = KeyboardType.Number,
            errorMessage = state.formErrors[ProfileFormField.Weight]
        )
        ProfileField(
            label = "Goal Weight (kg)",
            value = state.form.goalWeight.toString(),
            onValueChange = { viewModel.onFormFieldChange(ProfileFormField.GoalWeight, it) },
            isEditable = state.isEditing,
            keyboardType = KeyboardType.Number,
            errorMessage = state.formErrors[ProfileFormField.GoalWeight]
        )
        ProfileField(
            label = "Experience",
            value = state.form.experience,
            onValueChange = { viewModel.onFormFieldChange(ProfileFormField.Experience, it) },
            isEditable = state.isEditing,
            keyboardType = KeyboardType.Text,
            errorMessage = state.formErrors[ProfileFormField.Experience]
        )
        ProfileField(
            label = "Activity Level",
            value = state.form.activityLevel,
            onValueChange = { viewModel.onFormFieldChange(ProfileFormField.ActivityLevel, it) },
            isEditable = state.isEditing,
            keyboardType = KeyboardType.Text,
            errorMessage = state.formErrors[ProfileFormField.ActivityLevel]
        )
        ProfileField(
            label = "Weekly Workout Goal",
            value = state.form.weeklyWorkoutGoal.toString(),
            onValueChange = { viewModel.onFormFieldChange(ProfileFormField.WeeklyGoal, it) },
            isEditable = state.isEditing,
            keyboardType = KeyboardType.Number,
            errorMessage = state.formErrors[ProfileFormField.WeeklyGoal]
        )
        ProfileField(
            label = "Protein Goal (g)",
            value = state.form.proteinGoal.toString(),
            onValueChange = { viewModel.onFormFieldChange(ProfileFormField.ProteinGoal, it) },
            isEditable = state.isEditing,
            keyboardType = KeyboardType.Number,
            errorMessage = state.formErrors[ProfileFormField.ProteinGoal]
        )
        ProfileField(
            label = "Calories Goal",
            value = state.form.caloriesGoal.toString(),
            onValueChange = { viewModel.onFormFieldChange(ProfileFormField.CaloriesGoal, it) },
            isEditable = state.isEditing,
            keyboardType = KeyboardType.Number,
            errorMessage = state.formErrors[ProfileFormField.CaloriesGoal]
        )
        ProfileField(
            label = "Units",
            value = state.form.units,
            onValueChange = { viewModel.onFormFieldChange(ProfileFormField.Units, it) },
            isEditable = state.isEditing,
            keyboardType = KeyboardType.Text,
            errorMessage = state.formErrors[ProfileFormField.Units]
        )

        Spacer(Modifier.height(16.dp))

        // Analytics
        if (state.profile != null) {
            if (analyticsState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else if (analyticsState.error != null) {
                Text("Analytics unavailable", style = MaterialTheme.typography.bodyMedium)
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Profile Analytics", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Total Workouts: ${analyticsState.workoutCounts.total}", style = MaterialTheme.typography.bodyMedium)
                        Text("This Week: ${analyticsState.workoutCounts.week}  |  This Month: ${analyticsState.workoutCounts.month}", style = MaterialTheme.typography.bodyMedium)
                        Text("Total Volume: ${analyticsState.totalVolume.toInt()} kg", style = MaterialTheme.typography.bodyMedium)
                        Text("Avg Workout Volume: ${analyticsState.averageWorkoutVolume.toInt()} kg", style = MaterialTheme.typography.bodyMedium)
                        Text("Personal Records: ${analyticsState.personalRecords.size}", style = MaterialTheme.typography.bodyMedium)
                        if (analyticsState.muscleGroupDistribution.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Top Muscle Groups", style = MaterialTheme.typography.titleSmall)
                            analyticsState.muscleGroupDistribution.sortedByDescending { it.totalReps }.take(5).forEach { stats ->
                                Text("${stats.name}: ${stats.totalReps} reps", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isEditable: Boolean,
    keyboardType: KeyboardType,
    errorMessage: String?
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        readOnly = !isEditable,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        isError = errorMessage != null,
        supportingText = { if (errorMessage != null) Text(errorMessage, color = MaterialTheme.colorScheme.error) }
    )
}
