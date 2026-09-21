package com.gymcoach.app.presentation.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.data.local.entity.NutritionLogEntity
import com.gymcoach.app.domain.repository.NutritionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val fatGoalGrams: Float = 70f
)

data class NutritionUiState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
    val todayLogs: List<NutritionLogEntity> = emptyList(),
    val dailySummary: DailyNutritionSummary = DailyNutritionSummary(),
    val showAddDialog: Boolean = false,
    val editingLog: NutritionLogEntity? = null,
    val error: String? = null
)

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val nutritionRepository: NutritionRepository,
    private val userProfileRepository: com.gymcoach.app.domain.repository.UserProfileRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(NutritionUiState())
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()

    private var cachedProfile: com.gymcoach.app.data.local.entity.UserProfileEntity? = null

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
        val goals = calculateGoalsFromProfile(cachedProfile)
        val summary = DailyNutritionSummary(
            totalCalories = logs.sumOf { it.calories },
            totalProtein = logs.sumOf { it.proteinGrams.toDouble() }.toFloat(),
            totalCarbs = logs.sumOf { it.carbsGrams.toDouble() }.toFloat(),
            totalFat = logs.sumOf { it.fatGrams.toDouble() }.toFloat(),
            totalFiber = logs.sumOf { it.fiberGrams.toDouble() }.toFloat(),
            totalWaterMl = logs.sumOf { it.waterMl },
            calorieGoal = goals.first,
            proteinGoalGrams = goals.second,
            carbsGoalGrams = goals.third,
            fatGoalGrams = goals.fourth
        )
        _uiState.update { it.copy(dailySummary = summary) }
    }

    private fun calculateGoalsFromProfile(profile: com.gymcoach.app.data.local.entity.UserProfileEntity?): Quadruple<Int, Float, Float, Float> {
        val weight = (profile?.weightKg?.takeIf { it > 30.0 } ?: 70.0)
        val goal = (profile?.goal ?: "Hypertrophy").lowercase()
        val baseCalories = when {
            goal.contains("cut") || goal.contains("fat loss") || goal.contains("weight loss") ->
                (weight * 28.0).toInt().coerceIn(1600, 3200)
            goal.contains("bulk") || goal.contains("mass") || goal.contains("muscle") ->
                (weight * 36.0).toInt().coerceIn(2400, 4200)
            else ->
                (weight * 32.0).toInt().coerceIn(2000, 3600)
        }
        val proteinGoal = (weight * 2.0).toFloat().coerceIn(120f, 240f)
        val fatGoal = ((baseCalories * 0.25f) / 9f).coerceIn(50f, 100f)
        val carbsGoal = ((baseCalories - (proteinGoal * 4f) - (fatGoal * 9f)) / 4f).coerceIn(150f, 500f)
        return Quadruple(baseCalories, proteinGoal, carbsGoal, fatGoal)
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    fun loadDay(date: LocalDate) {
        val zone = ZoneId.systemDefault()
        val startOfDay = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedDate = date) }
            nutritionRepository.getLogsForDay(startOfDay, endOfDay).collect { logs ->
                val goals = calculateGoalsFromProfile(cachedProfile)
                val summary = DailyNutritionSummary(
                    totalCalories = logs.sumOf { it.calories },
                    totalProtein = logs.sumOf { it.proteinGrams.toDouble() }.toFloat(),
                    totalCarbs = logs.sumOf { it.carbsGrams.toDouble() }.toFloat(),
                    totalFat = logs.sumOf { it.fatGrams.toDouble() }.toFloat(),
                    totalFiber = logs.sumOf { it.fiberGrams.toDouble() }.toFloat(),
                    totalWaterMl = logs.sumOf { it.waterMl },
                    calorieGoal = goals.first,
                    proteinGoalGrams = goals.second,
                    carbsGoalGrams = goals.third,
                    fatGoalGrams = goals.fourth
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        todayLogs = logs,
                        dailySummary = summary
                    )
                }
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
        viewModelScope.launch {
            val zone = java.time.ZoneId.systemDefault()
            val dateMillis = _uiState.value.selectedDate.atStartOfDay(zone).toInstant().toEpochMilli() +
                (System.currentTimeMillis() % 86_400_000L)

            val log = com.gymcoach.app.data.local.entity.NutritionLogEntity(
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
        }
    }

    fun deleteLog(log: NutritionLogEntity) {
        viewModelScope.launch {
            nutritionRepository.deleteLog(log)
        }
    }
}
