package com.gymcoach.app.presentation.program

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.data.local.entity.ProgramDayEntity
import com.gymcoach.app.data.local.entity.ProgramEntity
import com.gymcoach.app.data.local.entity.ProgramExerciseEntity
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.repository.ExerciseRepository
import com.gymcoach.app.domain.repository.ProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class ProgramDayUiModel(
    val dayNumber: Int,
    val name: String,
    val targetMuscles: List<String>,
    val exercises: List<ProgramExerciseUiModel>,
    val isRestDay: Boolean
)

data class ProgramExerciseUiModel(
    val exerciseName: String,
    val muscleGroup: String,
    val sets: Int,
    val targetReps: String,
    val targetWeightKg: Double,
    val restSeconds: Int
)

data class ProgramUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val program: ProgramEntity? = null,
    val days: List<ProgramDayUiModel> = emptyList(),
    val exerciseMap: Map<Long, Exercise> = emptyMap(),
    val volumeCalculator: VolumeCalculator? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProgramViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    private val exerciseRepository: ExerciseRepository,
    private val volumeCalculator: VolumeCalculator
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgramUiState())
    val uiState: StateFlow<ProgramUiState> = _uiState.asStateFlow()

    private val _exercises = MutableStateFlow<Map<Long, Exercise>>(emptyMap())
    val exercises: StateFlow<Map<Long, Exercise>> = _exercises.asStateFlow()

    init {
        loadProgram()
        loadExercises()
    }

    private fun loadProgram() {
        viewModelScope.launch {
            programRepository.getActiveProgram()
                .flatMapLatest { program ->
                    if (program == null) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        emptyFlow()
                    } else {
                        programRepository.getDaysForProgram(program.id)
                            .flatMapLatest { days ->
                                val nonRestDays = days.filter { !it.isRestDay }.sortedBy { it.dayNumber }
                                programRepository.getExercisesForDays(days.map { it.id })
                                    .map { exercisesByDay ->
                                        ProgramUiState(
                                            isLoading = false,
                                            program = program,
                                            days = buildDayUiModels(nonRestDays, exercisesByDay),
                                            exerciseMap = _exercises.value,
                                            volumeCalculator = volumeCalculator
                                        )
                                    }
                            }
                    }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    private fun loadExercises() {
        viewModelScope.launch {
            exerciseRepository.getAllExercises().collect { exercises ->
                _exercises.value = exercises.associateBy { it.id }
            }
        }
    }

    private fun buildDayUiModels(
        days: List<ProgramDayEntity>,
        exercisesByDay: Map<Long, List<ProgramExerciseEntity>>
    ): List<ProgramDayUiModel> {
        return days.map { day ->
            val dayExercises = exercisesByDay[day.id] ?: emptyList()
            val exerciseUiModels = dayExercises.map { exercise ->
                val exerciseMeta = _exercises.value[exercise.exerciseId]
                ProgramExerciseUiModel(
                    exerciseName = exerciseMeta?.name ?: "Unknown Exercise",
                    muscleGroup = exerciseMeta?.muscleGroup ?: "",
                    sets = exercise.sets,
                    targetReps = exercise.targetReps,
                    targetWeightKg = exercise.targetWeightKg,
                    restSeconds = exercise.restSeconds
                )
            }
            ProgramDayUiModel(
                dayNumber = day.dayNumber,
                name = day.name,
                targetMuscles = day.targetMuscles.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                exercises = exerciseUiModels,
                isRestDay = day.isRestDay
            )
        }
    }
}