package com.gymcoach.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.home.VtaperAttribution
import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.data.local.entity.ProgramDayEntity
import com.gymcoach.app.data.local.entity.ProgramExerciseEntity
import com.gymcoach.app.data.local.entity.ProgramEntity
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.repository.AnalyticsRepository
import com.gymcoach.app.domain.repository.ExerciseRepository
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

/**
 * Dashboard bars: user-facing label to the attribution/balance muscle key.
 * Keys must match VtaperAttribution contributor names (labeled "Lats",
 * "Lateral Deltoid", "Rear Deltoid", "Upper Chest") plus the "Legs" bar pseudo-muscle.
 */
private val VTAPER_BAR_SOURCES = listOf(
    "Lats" to "Lats",
    "Lateral Delts" to "Lateral Deltoid",
    "Rear Delts" to "Rear Deltoid",
    "Chest" to "Upper Chest",
    "Legs" to "Legs"
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
    analyticsRepository: AnalyticsRepository, // PR count until PR queries live on WorkoutRepository
    exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _prCount = MutableStateFlow(0)
    private val _exercises = MutableStateFlow<Map<Long, Exercise>>(emptyMap())

    init {
        viewModelScope.launch {
            runCatching { analyticsRepository.getAllPersonalRecords() }
                .onSuccess { records -> _prCount.value = records.size }
        }
        viewModelScope.launch {
            exerciseRepository.getAllExercises().collect { exercises ->
                _exercises.value = exercises.associateBy { it.id }
            }
        }
        viewModelScope.launch {
            programRepository.getActiveProgram()
                .flatMapLatest { program ->
                    if (program == null) {
                        flowOf(null)
                    } else {
                        programRepository.getDaysForProgram(program.id).flatMapLatest { days ->
                            val nonRest = days.filter { !it.isRestDay }.sortedBy { it.dayNumber }
                            programRepository.getExercisesForDays(days.map { it.id }).map { byDay ->
                                ProgramCore(program, pickToday(nonRest), days, byDay)
                            }
                        }
                    }
                }
                .combine(workoutRepository.getCompletedWorkouts()) { core, workouts -> core to workouts }
                .combine(_prCount.asStateFlow().combine(_exercises.asStateFlow()) { pr, exercises -> pr to exercises }) { pair, prAndExercises ->
                    buildUiState(pair.first, pair.second, prAndExercises.first, prAndExercises.second)
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
        prCount: Int,
        exercises: Map<Long, Exercise> = emptyMap()
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

        // ponytail: bars/insight use planned volume because workout_sets lacks
        // exerciseId+date columns; once added, swap to
        // volumeCalculator.calculateWeeklyVolume(completedSets, muscleMap).
        val plannedVolume = plannedMuscleVolume(core.exercisesByDay, core.allDays, exercises)
        val bars = VTAPER_BAR_SOURCES.map { (label, muscleKey) ->
            VtaperMuscleData(
                label = label,
                current = plannedVolume[muscleKey] ?: 0,
                target = TARGET_WEEKLY_SETS
            )
        }
        val insight = volumeCalculator
            .calculateVtaperBalance(buildTrainingBalance(plannedVolume))
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
     * Planned weekly sets per balance/dashboard muscle, attributed per-exercise via
     * [VtaperAttribution] (no day-level broadcast). Unknown exercise ids are skipped.
     */
    private fun plannedMuscleVolume(
        exercisesByDay: Map<Long, List<ProgramExerciseEntity>>,
        days: List<ProgramDayEntity>,
        exercises: Map<Long, Exercise>
    ): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        for (day in days) {
            val dayExercises = exercisesByDay[day.id] ?: continue
            for (exercise in dayExercises) {
                val exerciseMeta = exercises[exercise.exerciseId] ?: continue
                for (muscle in VtaperAttribution.contributors(exerciseMeta)) {
                    result[muscle] = (result[muscle] ?: 0) + exercise.sets
                }
            }
        }
        return result
    }

    /** Mirrors VolumeCalculator's evidence bands (its classifier is private). */
    private fun statusFor(weeklyVolume: Double): VolumeCalculator.VolumeStatus = when {
        weeklyVolume < 10 -> VolumeCalculator.VolumeStatus.INSUFFICIENT
        weeklyVolume < 14 -> VolumeCalculator.VolumeStatus.MODERATE
        weeklyVolume < 18 -> VolumeCalculator.VolumeStatus.OPTIMAL
        weeklyVolume < 22 -> VolumeCalculator.VolumeStatus.HIGH
        else -> VolumeCalculator.VolumeStatus.EXCESSIVE
    }

    /** Planned volume projection: per-week credits on the balance's muscle keys. */
    private fun volume(name: String, planned: Map<String, Int>): VolumeCalculator.MuscleVolume {
        val sets = planned[name] ?: 0
        return VolumeCalculator.MuscleVolume(
            muscleName = name,
            weeklyVolume = sets.toDouble(),
            directSets = sets,
            indirectSets = 0,
            status = statusFor(sets.toDouble())
        )
    }

    private fun buildTrainingBalance(planned: Map<String, Int>): VolumeCalculator.TrainingBalance {
        return VolumeCalculator.TrainingBalance(
            latVolume = volume("Lats", planned),
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
