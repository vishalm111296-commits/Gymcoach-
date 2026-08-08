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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.domain.model.UserProfile
import com.gymcoach.app.domain.repository.AnalyticsRepository
import com.gymcoach.app.domain.repository.MuscleGroupStats
import com.gymcoach.app.domain.repository.PersonalRecord
import com.gymcoach.app.domain.repository.WorkoutCounts
import com.gymcoach.app.presentation.history.formatDuration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.gymcoach.app.domain.repository.ProfileRepository

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

data class ProfileAnalyticsUiState(
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
