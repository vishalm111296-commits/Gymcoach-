package com.gymcoach.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.program.ProgramGenerator
import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.ProgramDayEntity
import com.gymcoach.app.data.local.entity.ProgramEntity
import com.gymcoach.app.data.local.entity.ProgramExerciseEntity
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

/** Evidence-based optimal band floor (14-17 weekly sets) used as the bar target. */
private const val TARGET_WEEKLY_SETS = 14
private const val ESTIMATED_WORK_SECONDS_PER_SET = 40

/** Dashboard bars mapped from the generator's muscle vocabulary to user-facing groups. */
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
    val exercisesByDay: Map<Long, List<ProgramExerciseEntity>>,
    val allExercisesMap: Map<Long, ExerciseEntity>
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    workoutRepository: WorkoutRepository,
    private val exerciseDao: ExerciseDao,
    private val volumeCalculator: VolumeCalculator,
    analyticsRepository: AnalyticsRepository // PR count until PR queries live on WorkoutRepository
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
            exerciseDao.getAll().flatMapLatest { allExercisesList ->
                val allExercisesMap = allExercisesList.associateBy { it.id }
                programRepository.getActiveProgram().flatMapLatest { program ->
                    if (program == null) {
                        flowOf(null)
                    } else {
                        programRepository.getDaysForProgram(program.id).flatMapLatest { days ->
                            val nonRest = days.filter { !it.isRestDay }.sortedBy { it.dayNumber }
                            programRepository.getExercisesForDays(days.map { it.id }).map { byDay ->
                                ProgramCore(program, pickToday(nonRest), days, byDay, allExercisesMap)
                            }
                        }
                    }
                }
            }
                .combine(workoutRepository.getCompletedWorkouts()) { core, workouts -> core to workouts }
                .combine(_prCount.asStateFlow()) { pair, prCount ->
                    buildUiState(pair.first, pair.second, prCount)
                }
                .collect { state -> _uiState.value = state }
        }
    }

    /** Deterministic daily rotation through the program's training days. */
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

        val completedThisWeek = workouts.count {
            it.completed && it.date.toEpochMilli() >= weekStartMillis()
        }

        val plannedSets = plannedWeeklySets(core.exercisesByDay, core.allDays, core.allExercisesMap)
        val bars = VTAPER_BAR_SOURCES.map { (label, sources) ->
            VtaperMuscleData(
                label = label,
                current = sources.sumOf { plannedSets[it] ?: 0 },
                target = TARGET_WEEKLY_SETS
            )
        }
        val insight = volumeCalculator
            .calculateVtaperBalance(buildTrainingBalance(plannedSets))
            .overallBalance

        val todayExercises = core.todayDay?.let { core.exercisesByDay[it.id] }.orEmpty()
        val estimatedDuration = if (todayExercises.isEmpty()) {
            0
        } else {
            todayExercises.sumOf { it.sets * (it.restSeconds + ESTIMATED_WORK_SECONDS_PER_SET) } / 60
        }

        return HomeUiState(
            isLoading = false,
            hasProgram = true,
            todayWorkout = TodayWorkoutUiModel(
                name = core.todayDay?.name?.takeIf { it.isNotBlank() } ?: "Training Session",
                targetMuscles = targetMusclesMuscles(core.todayDay),
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

    private fun targetMusclesMuscles(day: ProgramDayEntity?): List<String> =
        day?.targetMuscles?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()

    /**
     * Planned weekly sets per muscle attributed directly to exercise metadata per slot.
     */
    private fun plannedWeeklySets(
        exercisesByDay: Map<Long, List<ProgramExerciseEntity>>,
        days: List<ProgramDayEntity>,
        allExercisesMap: Map<Long, ExerciseEntity>
    ): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        for (day in days) {
            val programExercises = exercisesByDay[day.id].orEmpty()
            val dayTargetSlots = day.targetMuscles.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            for (pEx in programExercises) {
                val exEntity = allExercisesMap[pEx.exerciseId]
                if (exEntity != null) {
                    var matchedSlot = false
                    for (slot in dayTargetSlots) {
                        if (ProgramGenerator.matchesMuscleSlot(exEntity, slot)) {
                            result[slot] = (result[slot] ?: 0) + pEx.sets
                            matchedSlot = true
                        }
                    }
                    if (!matchedSlot) {
                        // Fallback to exercise's primary muscleGroup if no day target slot matches
                        val group = exEntity.muscleGroup
                        result[group] = (result[group] ?: 0) + pEx.sets
                    }
                } else if (dayTargetSlots.isNotEmpty()) {
                    // Fallback to first day target slot if exercise metadata unavailable
                    val slot = dayTargetSlots.first()
                    result[slot] = (result[slot] ?: 0) + pEx.sets
                }
            }
        }
        return result
    }

    /** Mirrors VolumeCalculator's evidence bands (its classifier is private). */
    private fun statusFor(weeklySets: Int): VolumeCalculator.VolumeStatus = when {
        weeklySets < 10 -> VolumeCalculator.VolumeStatus.INSUFFICIENT
        weeklySets < 14 -> VolumeCalculator.VolumeStatus.MODERATE
        weeklySets < 18 -> VolumeCalculator.VolumeStatus.OPTIMAL
        weeklySets < 22 -> VolumeCalculator.VolumeStatus.HIGH
        else -> VolumeCalculator.VolumeStatus.EXCESSIVE
    }

    private fun volume(name: String, planned: Map<String, Int>): VolumeCalculator.MuscleVolume {
        val sets = planned[name] ?: 0
        return VolumeCalculator.MuscleVolume(
            muscleName = name,
            weeklySets = sets,
            directSets = sets,
            indirectSets = 0,
            status = statusFor(sets)
        )
    }

    private fun buildTrainingBalance(planned: Map<String, Int>): VolumeCalculator.TrainingBalance {
        return VolumeCalculator.TrainingBalance(
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
    }

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
