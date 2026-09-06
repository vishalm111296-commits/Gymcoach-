package com.gymcoach.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.data.local.entity.ProgramDayEntity
import com.gymcoach.app.data.local.entity.ProgramExerciseEntity
import com.gymcoach.app.data.local.entity.ProgramEntity
import com.gymcoach.app.domain.repository.AnalyticsRepository
import com.gymcoach.app.domain.repository.ProgramRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import com.gymcoach.app.presentation.home.components.VtaperMuscleData
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class TodayWorkoutUiModel(
    val name: String,
    val targetMuscles: List<String>,
    val exerciseCount: Int,
    val estimatedDurationMin: Int
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val hasProgram: Boolean = false,
    val todayWorkout: TodayWorkoutUiModel? = null,
    val coachInsight: String = "",
    val workoutsThisWeek: Int = 0,
    val targetWorkouts: Int = 0,
    val prCount: Int = 0,
    val vtaperBars: List<VtaperMuscleData> = emptyList()
)

private const val TARGET_WEEKLY_SETS = 10
private const val ESTIMATED_WORK_SECONDS_PER_SET = 40

private val VTAPER_BAR_SOURCES = listOf(
    "Lats" to listOf("Back"),
    "Lateral Delts" to listOf("Lateral Deltoid"),
    "Chest" to listOf("Chest", "Upper Chest"),
    "Legs" to listOf("Quadriceps", "Hamstrings", "Glutes", "Calves")
)

private data class ProgramCore(
    val program: ProgramEntity,
    val todayDay: ProgramDayEntity?,
    val allDays: List<ProgramDayEntity>,
    val exercisesByDay: Map<Long, List<ProgramExerciseEntity>>
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    workoutRepository: WorkoutRepository,
    private val volumeCalculator: VolumeCalculator,
    analyticsRepository: AnalyticsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val _prCount = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            runCatching { analyticsRepository.getAllPersonalRecords() }
                .onSuccess { records -> _prCount.value = records.size }
        }
        viewModelScope.launch {
            programRepository.getActiveProgram()
                .flatMapLatest { program ->
                    if (program == null) flowOf(null)
                    else programRepository.getDaysForProgram(program.id).flatMapLatest { days ->
                        val nonRest = days.filter { !it.isRestDay }.sortedBy { it.dayNumber }
                        programRepository.getExercisesForDays(days.map { it.id }).map { byDay ->
                            ProgramCore(program, pickToday(nonRest), days, byDay)
                        }
                    }
                }
                .combine(workoutRepository.getCompletedWorkouts()) { core, workouts -> core to workouts }
                .combine(_prCount.asStateFlow()) { pair, prCount -> buildUiState(pair.first, pair.second, prCount) }
                .collect { _uiState.value = it }
        }
    }

    private fun pickToday(nonRestDays: List<ProgramDayEntity>): ProgramDayEntity? {
        if (nonRestDays.isEmpty()) return null
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return nonRestDays[(dayOfYear - 1) % nonRestDays.size]
    }

    private fun buildUiState(
        core: ProgramCore?,
        workouts: List<com.gymcoach.app.domain.model.WorkoutWithStats>,
        prCount: Int
    ): HomeUiState {
        if (core == null) {
            return HomeUiState(
                isLoading = false,
                hasProgram = false,
                coachInsight = "Your first session is ready once you set up your plan.",
                prCount = prCount
            )
        }

        val completedThisWeek = workouts.count { it.completed && it.date.toEpochMilli() >= weekStartMillis() }
        val plannedSets = plannedWeeklySets(core.exercisesByDay, core.allDays)
        val bars = VTAPER_BAR_SOURCES.map { (label, sources) ->
            VtaperMuscleData(
                label = label,
                current = sources.sumOf { plannedSets[it] ?: 0 },
                target = TARGET_WEEKLY_SETS
            )
        }

        val insight = volumeCalculator.calculateVtaperBalance(buildTrainingBalance(plannedSets)).overallBalance
        val todayExercises = core.todayDay?.let { core.exercisesByDay[it.id] }.orEmpty()
        val estimatedDuration = if (todayExercises.isEmpty()) 0
        else todayExercises.sumOf { it.sets * (it.restSeconds + ESTIMATED_WORK_SECONDS_PER_SET) } / 60

        return HomeUiState(
            isLoading = false,
            hasProgram = true,
            todayWorkout = TodayWorkoutUiModel(
                name = core.todayDay?.name?.takeIf { it.isNotBlank() } ?: "Training Session",
                targetMuscles = core.todayDay?.targetMuscles?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty(),
                exerciseCount = todayExercises.size,
                estimatedDurationMin = estimatedDuration
            ),
            coachInsight = insight,
            workoutsThisWeek = completedThisWeek,
            targetWorkouts = core.program.daysPerWeek,
            prCount = prCount,
            vtaperBars = bars
        )
    }

    private fun plannedWeeklySets(
        exercisesByDay: Map<Long, List<ProgramExerciseEntity>>,
        days: List<ProgramDayEntity>
    ): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        for (day in days) {
            val daySets = exercisesByDay[day.id]?.sumOf { it.sets } ?: continue
            if (daySets <= 0) continue
            day.targetMuscles.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { muscle ->
                result[muscle] = (result[muscle] ?: 0) + daySets
            }
        }
        return result
    }

    private fun volume(name: String, planned: Map<String, Int>): VolumeCalculator.MuscleVolume {
        val sets = planned[name] ?: 0
        val status = when {
            sets < 6 -> VolumeCalculator.VolumeStatus.LOW
            sets < 10 -> VolumeCalculator.VolumeStatus.MODERATE
            else -> VolumeCalculator.VolumeStatus.HIGH
        }
        return VolumeCalculator.MuscleVolume(name, sets, sets, 0, status)
    }

    private fun buildTrainingBalance(planned: Map<String, Int>): VolumeCalculator.TrainingBalance =
        VolumeCalculator.TrainingBalance(
            latVolume = volume("Back", planned),
            lateralDeltVolume = volume("Lateral Deltoid", planned),
            rearDeltVolume = volume("Rear Deltoid", planned),
            upperChestVolume = volume("Upper Chest", planned),
            upperBackVolume = volume("Upper Back", planned),
            bicepsVolume = volume("Biceps", planned),
            tricepsVolume = volume("Triceps", planned),
            quadricepsVolume = volume("Quadriceps", planned),
            hamstringsVolume = volume("Hamstrings", planned),
            glutesVolume = volume("Glutes", planned),
            calvesVolume = volume("Calves", planned),
            coreVolume = volume("Core", planned)
        )

    private fun weekStartMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
