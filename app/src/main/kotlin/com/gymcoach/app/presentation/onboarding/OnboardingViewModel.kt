package com.gymcoach.app.presentation.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.program.ProgramGenerator
import com.gymcoach.app.data.local.entity.UserProfileEntity
import com.gymcoach.app.domain.repository.ProgramRepository
import com.gymcoach.app.domain.repository.UserProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
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
    // Personal defaults are the target profile for this personal app; every value remains editable.
    val sex: String? = "Male",
    val age: Float = 30f,
    val heightCm: Float = 170f,
    val weightKg: Float = 70f,
    val daysPerWeek: Int = 4,
    val sessionMinutes: Int = 60,
    val selectedEquipment: Set<String> = setOf("Dumbbell"),
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
    private val programRepository: ProgramRepository
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
                    goal = goal
                )
                programRepository.saveGeneratedProgram(generated)
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
     * Equipment is mapped by capability, not by a generic "home" bucket.
     * Dumbbell/bodyweight-only selections use the strict constrained profile;
     * adding a bench, bar, cable, band or other item opts into the broader home/gym profile.
     */
    private fun mapEquipmentType(equipment: Set<String>): String {
        val normalized = equipment.map { it.trim().lowercase() }.toSet()
        val strict = setOf("dumbbell", "adjustable dumbbell", "bodyweight", "floor")
        if (normalized.isEmpty() || normalized.all { it in strict }) return "dumbbell_bodyweight"
        return if (normalized.any { it in setOf("barbell", "cable", "smith machine", "leg press", "hack squat") }) {
            "gym"
        } else {
            "home"
        }
    }
}
