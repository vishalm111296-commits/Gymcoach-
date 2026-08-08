package com.gymcoach.app.presentation.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.domain.model.UserProfile
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutExercise
import com.gymcoach.app.domain.repository.ProfileRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import com.gymcoach.app.domain.repository.PersonalRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                profileRepository.getUserProfile().collect { profile ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        profile = profile,
                        isNewUser = profile == null,
                        form = ProfileForm.fromProfile(profile)
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load profile"
                )
            }
        }
    }

    fun onSaveProfile() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            try {
                if (validateForm()) {
                    val profile = formToProfile()
                    profileRepository.saveUserProfile(profile)
                    _state.value = _state.value.copy(
                        isSaving = false,
                        profile = profile,
                        isEditing = false,
                        isSaved = true,
                        error = null
                    )
                    loadProfile()
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to save profile"
                )
            }
        }
    }

    fun onFormFieldChange(field: ProfileFormField, value: String) {
        val currentForm = _state.value.form
        val updatedForm = when (field) {
            is ProfileFormField.Name -> currentForm.copy(name = value)
            is ProfileFormField.Age -> currentForm.copy(age = value.toIntOrNull() ?: 0)
            is ProfileFormField.Height -> currentForm.copy(height = value.toDoubleOrNull() ?: 0.0)
            is ProfileFormField.Weight -> currentForm.copy(weight = value.toDoubleOrNull() ?: 0.0)
            is ProfileFormField.GoalWeight -> currentForm.copy(goalWeight = value.toDoubleOrNull() ?: 0.0)
            is ProfileFormField.Gender -> currentForm.copy(gender = value)
            is ProfileFormField.Experience -> currentForm.copy(experience = value)
            is ProfileFormField.TrainingStyle -> currentForm.copy(trainingStyle = value)
            is ProfileFormField.PreferredSplit -> currentForm.copy(preferredSplit = value)
            is ProfileFormField.ActivityLevel -> currentForm.copy(activityLevel = value)
            is ProfileFormField.WeeklyGoal -> currentForm.copy(weeklyWorkoutGoal = value.toIntOrNull() ?: 0)
            is ProfileFormField.ProteinGoal -> currentForm.copy(proteinGoal = value.toDoubleOrNull() ?: 0.0)
            is ProfileFormField.CaloriesGoal -> currentForm.copy(caloriesGoal = value.toIntOrNull() ?: 0)
            is ProfileFormField.Units -> currentForm.copy(units = value)
        }
        _state.value = _state.value.copy(form = updatedForm)
    }

    private fun validateForm(): Boolean {
        val form = _state.value.form
        val errors = mutableMapOf<ProfileFormField, String>()

        if (form.name.isBlank()) {
            errors[ProfileFormField.Name] = "Name is required"
        }

        if (form.age <= 0) {
            errors[ProfileFormField.Age] = "Please enter a valid age"
        }

        if (form.height <= 0) {
            errors[ProfileFormField.Height] = "Please enter a valid height"
        }

        if (form.weight <= 0) {
            errors[ProfileFormField.Weight] = "Please enter a valid weight"
        }

        if (form.goalWeight > 0 && form.goalWeight < form.weight) {
            errors[ProfileFormField.GoalWeight] = "Goal weight should be greater than current weight"
        }

        _state.value = _state.value.copy(formErrors = errors)
        return errors.isEmpty()
    }

    private fun formToProfile(): UserProfile {
        val form = _state.value.form
        return UserProfile(
            id = 1,
            name = form.name,
            age = form.age,
            gender = form.gender,
            height = form.height,
            weight = form.weight,
            goalWeight = form.goalWeight,
            currentGoal = "",
            experience = form.experience,
            trainingStyle = form.trainingStyle,
            preferredSplit = form.preferredSplit,
            activityLevel = form.activityLevel,
            weeklyWorkoutGoal = form.weeklyWorkoutGoal,
            proteinGoal = form.proteinGoal,
            caloriesGoal = form.caloriesGoal,
            units = form.units,
            avatarUrl = "",
            leanBodyMass = 0.0,
            maintenanceCalories = 0
        )
    }

    fun onEditClick() {
        _state.value = _state.value.copy(isEditing = true)
    }

    fun onCancelEdit() {
        _state.value = _state.value.copy(
            isEditing = false,
            form = _state.value.form.copy(profile = _state.value.profile)
        )
    }

    fun onRetry() {
        loadProfile()
    }
}

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isNewUser: Boolean = false,
    val isEditing: Boolean = false,
    val isSaved: Boolean = false,
    val profile: UserProfile? = null,
    val form: ProfileForm = ProfileForm(),
    val formErrors: Map<ProfileFormField, String> = emptyMap(),
    val error: String? = null
)

data class ProfileForm(
    val name: String = "",
    val age: Int = 0,
    val gender: String = "",
    val height: Double = 0.0,
    val weight: Double = 0.0,
    val goalWeight: Double = 0.0,
    val experience: String = "",
    val trainingStyle: String = "",
    val preferredSplit: String = "",
    val activityLevel: String = "",
    val weeklyWorkoutGoal: Int = 0,
    val proteinGoal: Double = 0.0,
    val caloriesGoal: Int = 0,
    val units: String = "",
    val profile: UserProfile? = null
) {
    companion object {
        fun fromProfile(profile: UserProfile?): ProfileForm = if (profile == null) {
            ProfileForm()
        } else {
            ProfileForm(
                name = profile.name,
                age = profile.age,
                gender = profile.gender,
                height = profile.height,
                weight = profile.weight,
                goalWeight = profile.goalWeight,
                experience = profile.experience,
                trainingStyle = profile.trainingStyle,
                preferredSplit = profile.preferredSplit,
                activityLevel = profile.activityLevel,
                weeklyWorkoutGoal = profile.weeklyWorkoutGoal,
                proteinGoal = profile.proteinGoal,
                caloriesGoal = profile.caloriesGoal,
                units = profile.units
            )
        }
    }
}

sealed class ProfileFormField {
    data object Name : ProfileFormField()
    data object Age : ProfileFormField()
    data object Height : ProfileFormField()
    data object Weight : ProfileFormField()
    data object GoalWeight : ProfileFormField()
    data object Gender : ProfileFormField()
    data object Experience : ProfileFormField()
    data object TrainingStyle : ProfileFormField()
    data object PreferredSplit : ProfileFormField()
    data object ActivityLevel : ProfileFormField()
    data object WeeklyGoal : ProfileFormField()
    data object ProteinGoal : ProfileFormField()
    data object CaloriesGoal : ProfileFormField()
    data object Units : ProfileFormField()
}

