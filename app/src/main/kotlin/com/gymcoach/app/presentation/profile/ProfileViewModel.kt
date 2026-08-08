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
import com.gymcoach.app.domain.model.UserProfile
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutExercise
import com.gymcoach.app.domain.repository.ProfileRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import com.gymcoach.app.domain.repository.PersonalRecord
import com.gymcoach.app.domain.repository.WorkoutStats
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
                    profileRepository.updateProfile(profile)
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

sealed class ProfileUiState(
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

@HiltViewModel
class ProfileSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileSettingsState())
    val state: StateFlow<ProfileSettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _state.value = ProfileSettingsState(
            profileSyncEnabled = prefs.getBoolean(KEY_PROFILE_SYNC, true),
            autoSync = prefs.getBoolean(KEY_AUTO_SYNC, true),
            syncFrequency = prefs.getString(KEY_SYNC_FREQUENCY, "daily") ?: "daily",
            dataUsageLimit = prefs.getLong(KEY_DATA_USAGE_LIMIT, 100 * 1024 * 1024),
            compressData = prefs.getBoolean(KEY_COMPRESS_DATA, true)
        )
    }

    fun onProfileSyncToggle(enabled: Boolean) {
        _state.value = _state.value.copy(profileSyncEnabled = enabled)
        savePreference(context, KEY_PROFILE_SYNC, enabled)
    }

    fun onAutoSyncToggle(enabled: Boolean) {
        _state.value = _state.value.copy(autoSync = enabled)
        savePreference(context, KEY_AUTO_SYNC, enabled)
    }

    fun onSyncFrequencyChange(frequency: String) {
        _state.value = _state.value.copy(syncFrequency = frequency)
        savePreference(context, KEY_SYNC_FREQUENCY, frequency)
    }

    fun onDataUsageLimitChange(limit: Long) {
        _state.value = _state.value.copy(dataUsageLimit = limit)
        savePreference(context, KEY_DATA_USAGE_LIMIT, limit)
    }

    fun onCompressDataToggle(enabled: Boolean) {
        _state.value = _state.value.copy(compressData = enabled)
        savePreference(context, KEY_COMPRESS_DATA, enabled)
    }

    private fun savePreference(context: Context, key: String, value: Any) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        when (value) {
            is Boolean -> prefs.putBoolean(key, value)
            is Long -> prefs.putLong(key, value)
            is String -> prefs.putString(key, value)
        }
        prefs.apply()
    }

    companion object {
        private const val PREFS_NAME = "GymCoachProfileSettings"
        private const val KEY_PROFILE_SYNC = "profile_sync_enabled"
        private const val KEY_AUTO_SYNC = "auto_sync_enabled"
        private const val KEY_SYNC_FREQUENCY = "sync_frequency"
        private const val KEY_DATA_USAGE_LIMIT = "data_usage_limit_bytes"
        private const val KEY_COMPRESS_DATA = "compress_data"
    }
}

sealed class ProfileSettingsState(
    val profileSyncEnabled: Boolean = true,
    val autoSync: Boolean = true,
    val syncFrequency: String = "daily",
    val dataUsageLimit: Long = 100 * 1024 * 1024,
    val compressData: Boolean = true
)

@HiltViewModel
class ProfileAnalyticsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileAnalyticsUiState())
    val state: StateFlow<ProfileAnalyticsUiState> = _state.asStateFlow()

    init {
        loadAnalytics()
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val userProfile = profileRepository.getUserProfile().firstOrNull()
                if (userProfile != null) {
                    val workoutCounts = analyticsRepository.getWorkoutCounts()
                    val totalVolume = analyticsRepository.getTotalVolume()
                    val averageWorkoutVolume = analyticsRepository.getAverageWorkoutVolume()
                    val personalRecords = analyticsRepository.getAllPersonalRecords()
                    val muscleGroupDistribution = analyticsRepository.getMuscleGroupDistribution()

                    _state.value = _state.value.copy(
                        isLoading = false,
                        profile = userProfile,
                        workoutCounts = workoutCounts,
                        totalVolume = totalVolume,
                        averageWorkoutVolume = averageWorkoutVolume,
                        personalRecords = personalRecords,
                        muscleGroupDistribution = muscleGroupDistribution,
                        profileCompletionPercentage = calculateProfileCompletion(userProfile)
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "No profile found"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load analytics"
                )
            }
        }
    }

    private fun calculateProfileCompletion(profile: UserProfile): Float {
        val fields = listOf(
            profile.name.isNotBlank(),
            profile.age > 0,
            profile.height > 0.0,
            profile.weight > 0.0,
            profile.gender.isNotBlank(),
            profile.experience.isNotBlank(),
            profile.activityLevel.isNotBlank(),
            profile.weeklyWorkoutGoal > 0
        )
        return (fields.count { it } * 100f) / fields.size
    }

    fun refresh() = loadAnalytics()
}

sealed class ProfileAnalyticsUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val workoutCounts: WorkoutCounts = WorkoutCounts(0, 0, 0, 0),
    val totalVolume: Double = 0.0,
    val averageWorkoutVolume: Double = 0.0,
    val personalRecords: List<PersonalRecord> = emptyList(),
    val muscleGroupDistribution: List<MuscleGroupStats> = emptyList(),
    val profileCompletionPercentage: Float = 0.0f,
    val error: String? = null
)
