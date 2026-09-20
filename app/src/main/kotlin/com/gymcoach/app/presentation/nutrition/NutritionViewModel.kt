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
    private val nutritionRepository: NutritionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NutritionUiState())
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()

    init {
        loadDay(LocalDate.now())
    }

    fun loadDay(date: LocalDate) {
        val zone = ZoneId.systemDefault()
        val startOfDay = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedDate = date) }
            nutritionRepository.getLogsForDay(startOfDay, endOfDay).collect { logs ->
                val summary = DailyNutritionSummary(
                    totalCalories = logs.sumOf { it.calories },
                    totalProtein = logs.sumOf { it.proteinGrams.toDouble() }.toFloat(),
                    totalCarbs = logs.sumOf { it.carbsGrams.toDouble() }.toFloat(),
                    totalFat = logs.sumOf { it.fatGrams.toDouble() }.toFloat(),
                    totalFiber = logs.sumOf { it.fiberGrams.toDouble() }.toFloat(),
                    totalWaterMl = logs.sumOf { it.waterMl }
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
