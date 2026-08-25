package com.gymcoach.app.presentation.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.program.ProgramGenerator
import com.gymcoach.app.data.local.entity.UserProfileEntity
import com.gymcoach.app.domain.repository.ProgramRepository
import com.gymcoach.app.domain.repository.UserProfileRepository
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Onboarding flow steps. "Preferences" is folded into REVIEW to keep the flow short. */
enum class OnboardingStep {
    WELCOME, GOAL, EXPERIENCE, PERSONAL_INFO, SCHEDULE, EQUIPMENT, REVIEW, COMPLETE
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val goal: String? = null,
    val experience: String? = null,
    // Pre-selected defaults match the chips shown in PersonalInfoStep so the persisted
    // profile is never silently empty when the user skips tapping these controls.
    val sex: String? = "Male",
    val age: Float = 25f,
    val heightCm: Float = 175f,
    val weightKg: Float = 75f,
    val daysPerWeek: Int = 4,
    val sessionMinutes: Int = 60,
    val selectedEquipment: Set<String> = emptySet(),
    val preferredSchedule: String? = "Morning",
    val limitationsPreferences: String? = "None",
    val isGenerating: Boolean = false,
    val error: String? = null
) {
    val isStepValid: Boolean
        get() = when (step) {
            OnboardingStep.GOAL -> goal != null
            OnboardingStep.EXPERIENCE -> experience != null
            OnboardingStep.PERSONAL_INFO -> age in 14f..90f && heightCm >= 120f && weightKg >= 30f
            else -> true
        }

    val isLastContentStep: Boolean get() = step == OnboardingStep.REVIEW
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userProfileRepository: UserProfileRepository,
    private val programGenerator: ProgramGenerator,
    private val programRepository: ProgramRepository // persists the generated first program
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun selectGoal(goal: String) = _uiState.update { it.copy(goal = goal) }

    fun selectExperience(experience: String) = _uiState.update { it.copy(experience = experience) }

    fun setAge(age: Float) = _uiState.update { it.copy(age = age) }

    fun setHeight(heightCm: Float) = _uiState.update { it.copy(heightCm = heightCm) }

    fun setWeight(weightKg: Float) = _uiState.update { it.copy(weightKg = weightKg) }

    fun setSex(sex: String) = _uiState.update { it.copy(sex = sex) }

    fun setDaysPerWeek(days: Int) = _uiState.update { it.copy(daysPerWeek = days) }

    fun setSessionMinutes(minutes: Int) = _uiState.update { it.copy(sessionMinutes = minutes) }

    fun setPreferredSchedule(schedule: String) = _uiState.update { it.copy(preferredSchedule = schedule) }

    fun setLimitationsPreferences(limitations: String) = _uiState.update { it.copy(limitationsPreferences = limitations) }

    fun toggleEquipment(item: String) = _uiState.update { state ->
        val next = if (item in state.selectedEquipment) state.selectedEquipment - item
        else state.selectedEquipment + item
        state.copy(selectedEquipment = next)
    }

    fun next() = _uiState.update { state ->
        val order = OnboardingStep.entries
        val index = order.indexOf(state.step)
        if (index < order.lastIndex) state.copy(step = order[index + 1]) else state
    }

    fun back() = _uiState.update { state ->
        val order = OnboardingStep.entries
        val index = order.indexOf(state.step)
        if (index > 0) state.copy(step = order[index - 1]) else state
    }

    /** Saves the profile, generates and stores the first program, then signals completion. */
    fun completeOnboarding(onComplete: () -> Unit) {
        val state = _uiState.value
        if (state.isGenerating || !state.isLastContentStep) return
        val goal = state.goal ?: return
        val experience = state.experience ?: return
        _uiState.update { it.copy(isGenerating = true, error = null) }
        viewModelScope.launch {
            try {
                val equipmentType = mapEquipmentType(state.selectedEquipment)
                userProfileRepository.saveProfile(
                    UserProfileEntity(
                        goal = goal,
                        experience = experience,
                        sex = state.sex ?: "Male",
                        age = state.age.toInt(),
                        heightCm = state.heightCm.toDouble(),
                        weightKg = state.weightKg.toDouble(),
                        trainingDaysPerWeek = state.daysPerWeek,
                        sessionLengthMinutes = state.sessionMinutes,
                        equipmentType = equipmentType,
                        preferredExercises = "",
                        exercisesToAvoid = "",
                        preferredSchedule = state.preferredSchedule ?: "Morning",
                        limitationsPreferences = state.limitationsPreferences ?: "None"
                    )
                )
                val generated = programGenerator.generateProgram(
                    frequency = state.daysPerWeek,
                    equipmentType = equipmentType,
                    experienceLevel = experience,
                    goal = goal
                )
                programRepository.saveGeneratedProgram(generated)
                // Mark onboarding as complete so returning users skip it
                context.getSharedPreferences("gymcoach_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("onboarding_complete", true)
                    .apply()
                _uiState.update { it.copy(step = OnboardingStep.COMPLETE, isGenerating = false) }
                onComplete()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isGenerating = false, error = e.message ?: "Could not generate your program")
                }
            }
        }
    }

    /**
     * Maps UI equipment selection to the equipment type used by ProgramGenerator.
     * "gym" = has barbell/cable/machine access → full equipment set
     * "home" = has dumbbells/bands/bench → filtered equipment set
     * "custom" = no equipment → bodyweight only
     *
     * Equipment names now match ExerciseEntity.equipment values exactly.
     */
    private fun mapEquipmentType(equipment: Set<String>): String = when {
        equipment.any { it == "Barbell" || it == "Cable" } -> "gym"
        equipment.isNotEmpty() -> "home"
        else -> "custom"
    }
}
