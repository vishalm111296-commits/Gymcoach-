package com.gymcoach.app.presentation.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.nutrition.ActivityLevel
import com.gymcoach.app.core.nutrition.BiologicalSex
import com.gymcoach.app.core.nutrition.NutritionGoal
import com.gymcoach.app.core.nutrition.TdeeMacroCalculator
import com.gymcoach.app.core.nutrition.TdeeProfile
import com.gymcoach.app.data.local.entity.NutritionLogEntity
import com.gymcoach.app.data.local.entity.UserProfileEntity
import com.gymcoach.app.domain.repository.NutritionRepository
import com.gymcoach.app.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class DailyNutritionSummary(
    val totalCalories: Int = 0,
    val totalProtein: Float = 0f,
    val totalCarbs: Float = 0f,
    val totalFat: Float = 0f,
    val totalFiber: Float = 0f,
    val totalWaterMl: Int = 0,
    val calorieGoal: Int = 2500,
    val proteinGoalGrams: Float = 160f,
    val carbsGoalGrams: Float = 275f,
    val fatGoalGrams: Float = 70f,
    val fiberGoalGrams: Float = 35f,
    val waterGoalMl: Int = 3000,
    val tdeeProfile: TdeeProfile? = null
)

data class NutritionUiState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
    val todayLogs: List<NutritionLogEntity> = emptyList(),
    val dailySummary: DailyNutritionSummary = DailyNutritionSummary(),
    val showAddDialog: Boolean = false,
    val showTdeeCalculatorSheet: Boolean = false,
    val selectedGoalOverride: NutritionGoal? = null,
    val selectedActivityOverride: ActivityLevel? = null,
    val editingLog: NutritionLogEntity? = null,
    val error: String? = null
)

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val nutritionRepository: NutritionRepository,
    private val userProfileRepository: UserProfileRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(NutritionUiState())
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()

    private var cachedProfile: UserProfileEntity? = null

    init {
        viewModelScope.launch {
            userProfileRepository?.getLatestProfile()?.collect { profile ->
                cachedProfile = profile
                recalculateSummaryWithCurrentLogs()
            }
        }
        loadDay(LocalDate.now())
    }

    private fun recalculateSummaryWithCurrentLogs() {
        val logs = _uiState.value.todayLogs
        val tdeeProfile = computeTdeeProfile(
            profile = cachedProfile,
            goalOverride = _uiState.value.selectedGoalOverride,
            activityOverride = _uiState.value.selectedActivityOverride
        )
        val summary = DailyNutritionSummary(
            totalCalories = logs.sumOf { it.calories },
            totalProtein = logs.sumOf { it.proteinGrams.toDouble() }.toFloat(),
            totalCarbs = logs.sumOf { it.carbsGrams.toDouble() }.toFloat(),
            totalFat = logs.sumOf { it.fatGrams.toDouble() }.toFloat(),
            totalFiber = logs.sumOf { it.fiberGrams.toDouble() }.toFloat(),
            totalWaterMl = logs.sumOf { it.waterMl },
            calorieGoal = tdeeProfile.targetCalories,
            proteinGoalGrams = tdeeProfile.macroSplit.proteinGrams,
            carbsGoalGrams = tdeeProfile.macroSplit.carbsGrams,
            fatGoalGrams = tdeeProfile.macroSplit.fatGrams,
            fiberGoalGrams = tdeeProfile.fiberGrams,
            waterGoalMl = tdeeProfile.waterMlTarget,
            tdeeProfile = tdeeProfile
        )
        _uiState.update { it.copy(dailySummary = summary) }
    }

    fun computeTdeeProfile(
        profile: UserProfileEntity?,
        goalOverride: NutritionGoal? = null,
        activityOverride: ActivityLevel? = null
    ): TdeeProfile {
        val weight = profile?.weightKg?.takeIf { it > 30.0 } ?: 75.0
        val height = profile?.heightCm?.takeIf { it > 100.0 } ?: 175.0
        val age = profile?.age?.takeIf { it in 14..100 } ?: 25
        val sex = BiologicalSex.fromString(profile?.sex ?: "")
        val trainingDays = profile?.trainingDaysPerWeek ?: 4
        val sessionLength = profile?.sessionLengthMinutes ?: 60

        val goal = goalOverride ?: NutritionGoal.fromString(profile?.goal ?: "Hypertrophy")
        val activity = activityOverride ?: ActivityLevel.inferFromTraining(trainingDays, sessionLength)

        return TdeeMacroCalculator.calculate(
            weightKg = weight,
            heightCm = height,
            age = age,
            sex = sex,
            goal = goal,
            activityLevel = activity,
            trainingDaysPerWeek = trainingDays,
            sessionLengthMinutes = sessionLength
        )
    }

    fun selectGoalOverride(goal: NutritionGoal?) {
        _uiState.update { it.copy(selectedGoalOverride = goal) }
        recalculateSummaryWithCurrentLogs()
    }

    fun selectActivityOverride(activity: ActivityLevel?) {
        _uiState.update { it.copy(selectedActivityOverride = activity) }
        recalculateSummaryWithCurrentLogs()
    }

    fun showTdeeSheet() {
        _uiState.update { it.copy(showTdeeCalculatorSheet = true) }
    }

    fun hideTdeeSheet() {
        _uiState.update { it.copy(showTdeeCalculatorSheet = false) }
    }

    fun loadDay(date: LocalDate) {
        val zone = ZoneId.systemDefault()
        val startOfDay = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedDate = date) }
            try {
                nutritionRepository.getLogsForDay(startOfDay, endOfDay).collect { logs ->
                    val tdeeProfile = computeTdeeProfile(
                        profile = cachedProfile,
                        goalOverride = _uiState.value.selectedGoalOverride,
                        activityOverride = _uiState.value.selectedActivityOverride
                    )
                    val summary = DailyNutritionSummary(
                        totalCalories = logs.sumOf { it.calories },
                        totalProtein = logs.sumOf { it.proteinGrams.toDouble() }.toFloat(),
                        totalCarbs = logs.sumOf { it.carbsGrams.toDouble() }.toFloat(),
                        totalFat = logs.sumOf { it.fatGrams.toDouble() }.toFloat(),
                        totalFiber = logs.sumOf { it.fiberGrams.toDouble() }.toFloat(),
                        totalWaterMl = logs.sumOf { it.waterMl },
                        calorieGoal = tdeeProfile.targetCalories,
                        proteinGoalGrams = tdeeProfile.macroSplit.proteinGrams,
                        carbsGoalGrams = tdeeProfile.macroSplit.carbsGrams,
                        fatGoalGrams = tdeeProfile.macroSplit.fatGrams,
                        fiberGoalGrams = tdeeProfile.fiberGrams,
                        waterGoalMl = tdeeProfile.waterMlTarget,
                        tdeeProfile = tdeeProfile
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            todayLogs = logs,
                            dailySummary = summary
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isLoading = false, error = "Failed to load nutrition data: ${e.message}") }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true, editingLog = null) }
    }

    fun hideDialog() {
        _uiState.update { it.copy(showAddDialog = false, editingLog = null) }
    }

    fun editLog(log: NutritionLogEntity) {
        _uiState.update { it.copy(showAddDialog = true, editingLog = log) }
    }

    fun logWater(amountMl: Int) {
        if (amountMl <= 0) return
        viewModelScope.launch {
            try {
                val zone = ZoneId.systemDefault()
                val dateMillis = _uiState.value.selectedDate.atStartOfDay(zone).toInstant().toEpochMilli() +
                    (System.currentTimeMillis() % 86_400_000L)

                val log = NutritionLogEntity(
                    date = dateMillis,
                    mealName = "Water",
                    calories = 0,
                    proteinGrams = 0f,
                    carbsGrams = 0f,
                    fatGrams = 0f,
                    fiberGrams = 0f,
                    waterMl = amountMl,
                    notes = "Hydration"
                )
                nutritionRepository.addLog(log)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(error = "Failed to log water: ${e.message}") }
            }
        }
    }

    fun saveLog(
        mealName: String,
        calories: Int,
        proteinGrams: Float,
        carbsGrams: Float,
        fatGrams: Float,
        fiberGrams: Float = 0f,
        waterMl: Int = 0,
        notes: String = ""
    ) {
        viewModelScope.launch {
            try {
                val existing = _uiState.value.editingLog
                val zone = ZoneId.systemDefault()
                val dateMillis = _uiState.value.selectedDate.atStartOfDay(zone).toInstant().toEpochMilli() +
                    (System.currentTimeMillis() % 86_400_000L)

                val log = NutritionLogEntity(
                    id = existing?.id ?: 0L,
                    date = existing?.date ?: dateMillis,
                    mealName = mealName,
                    calories = calories,
                    proteinGrams = proteinGrams,
                    carbsGrams = carbsGrams,
                    fatGrams = fatGrams,
                    fiberGrams = fiberGrams,
                    waterMl = waterMl,
                    notes = notes
                )

                if (log.id == 0L) {
                    nutritionRepository.addLog(log)
                } else {
                    nutritionRepository.updateLog(log)
                }
                _uiState.update { it.copy(showAddDialog = false, editingLog = null) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(error = "Failed to save nutrition log: ${e.message}") }
            }
        }
    }

    fun deleteLog(log: NutritionLogEntity) {
        viewModelScope.launch {
            try {
                nutritionRepository.deleteLog(log)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(error = "Failed to delete nutrition log: ${e.message}") }
            }
        }
    }
}
