package com.gymcoach.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.data.local.dao.ExerciseMuscleWithDetails
import com.gymcoach.app.data.local.entity.ProgramDayEntity
import com.gymcoach.app.data.local.entity.ProgramExerciseEntity
import com.gymcoach.app.data.local.entity.ProgramEntity
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

private const val TARGET_WEEKLY_SETS = 14
private const val ESTIMATED_WORK_SECONDS_PER_SET = 40

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
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
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
            val programFlow = programRepository.getActiveProgram()
                .flatMapLatest { program ->
                    if (program == null) {
                        flowOf(null)
                    } else {
                        programRepository.getDaysForProgram(program.id).flatMapLatest { days ->
                            val nonRest = days.filter { !it.isRestDay }.sortedBy { it.dayNumber }
                            val dayIds = days.map { it.id }
                            if (dayIds.isEmpty()) {
                                flowOf(ProgramCore(program, pickToday(nonRest), days, emptyMap()))
                            } else {
                                programRepository.getExercisesForDays(dayIds).map { byDay ->
                                    ProgramCore(program, pickToday(nonRest), days, byDay)
                                }
                            }
                        }
                    }
                }

            combine(
                programFlow,
                workoutRepository.getCompletedWorkouts(),
                workoutRepository.getCompletedSetsWithContext(),
                exerciseRepository.getAllExercises(),
                exerciseRepository.getAllExerciseMuscleDetails()
            ) { p1, p2, p3, p4, p5 ->
                val core = p1 as ProgramCore?
                val workouts = p2 as List<com.gymcoach.app.domain.model.WorkoutWithStats>
                val completedSets = p3 as List<VolumeCalculator.SetWithContext>
                val exercises = p4 as List<com.gymcoach.app.domain.model.Exercise>
                val muscleDetails = p5 as List<ExerciseMuscleWithDetails>
                buildUiState(core, workouts, completedSets, exercises, muscleDetails, _prCount.value)
            }.collect { state -> _uiState.value = state }
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
        completedSets: List<VolumeCalculator.SetWithContext>,
        exercises: List<com.gymcoach.app.domain.model.Exercise>,
        muscleDetails: List<ExerciseMuscleWithDetails>,
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

        val weekStart = weekStartMillis()
        val completedThisWeek = workouts.count {
            it.completed && it.date.toEpochMilli() >= weekStart
        }

        val detailsByExercise = muscleDetails.groupBy { it.exerciseId }

        // Build muscle assignments for VolumeCalculator using canonical exercise_muscles relationships
        val muscleAssignments = exercises.associate { exercise ->
            val rels = detailsByExercise[exercise.id]
            val assignments = mutableListOf<VolumeCalculator.MuscleAssignment>()

            if (!rels.isNullOrEmpty()) {
                for (rel in rels) {
                    val role = when (rel.role.lowercase()) {
                        "primary" -> VolumeCalculator.MuscleRole.PRIMARY
                        "secondary" -> VolumeCalculator.MuscleRole.SECONDARY
                        "stabilizer" -> VolumeCalculator.MuscleRole.STABILIZER
                        else -> VolumeCalculator.MuscleRole.PRIMARY
                    }
                    assignments.add(VolumeCalculator.MuscleAssignment(mapMuscleName(rel.muscleName), role))
                }
            } else {
                if (exercise.muscleGroup.isNotBlank()) {
                    assignments.add(VolumeCalculator.MuscleAssignment(mapMuscleName(exercise.muscleGroup), VolumeCalculator.MuscleRole.PRIMARY))
                }
                if (exercise.secondaryMuscles.isNotBlank()) {
                    exercise.secondaryMuscles.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { sec ->
                        assignments.add(VolumeCalculator.MuscleAssignment(mapMuscleName(sec), VolumeCalculator.MuscleRole.SECONDARY))
                    }
                }
            }
            exercise.id to assignments
        }

        val currentWeekSets = completedSets.filter { it.workoutDate >= weekStart }
        val trainingBalance = volumeCalculator.calculateWeeklyVolume(currentWeekSets, muscleAssignments)

        val bars = listOf(
            VtaperMuscleData(label = "Lats", current = trainingBalance.backVolume.weeklySets, target = TARGET_WEEKLY_SETS),
            VtaperMuscleData(label = "Lateral Delts", current = trainingBalance.lateralDeltVolume.weeklySets, target = TARGET_WEEKLY_SETS),
            VtaperMuscleData(label = "Chest", current = trainingBalance.upperChestVolume.weeklySets, target = TARGET_WEEKLY_SETS),
            VtaperMuscleData(
                label = "Legs",
                current = trainingBalance.quadricepsVolume.weeklySets + trainingBalance.hamstringsVolume.weeklySets + trainingBalance.glutesVolume.weeklySets + trainingBalance.calvesVolume.weeklySets,
                target = TARGET_WEEKLY_SETS
            )
        )

        val insight = volumeCalculator.calculateVtaperBalance(trainingBalance).overallBalance

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

    private fun mapMuscleName(raw: String): String = when {
        raw.contains("latissimus", ignoreCase = true) || raw.contains("lat", ignoreCase = true) || raw.contains("back", ignoreCase = true) -> "Lats"
        raw.contains("lateral_deltoid", ignoreCase = true) || raw.contains("lateral delt", ignoreCase = true) || raw.contains("side delt", ignoreCase = true) -> "Lateral Deltoid"
        raw.contains("rear_deltoid", ignoreCase = true) || raw.contains("rear delt", ignoreCase = true) -> "Rear Deltoid"
        raw.contains("upper_chest", ignoreCase = true) || raw.contains("chest", ignoreCase = true) -> "Upper Chest"
        raw.contains("bicep", ignoreCase = true) -> "Biceps"
        raw.contains("tricep", ignoreCase = true) -> "Triceps"
        raw.contains("quadricep", ignoreCase = true) || raw.contains("quad", ignoreCase = true) -> "Quadriceps"
        raw.contains("hamstring", ignoreCase = true) -> "Hamstrings"
        raw.contains("glute", ignoreCase = true) -> "Glutes"
        raw.contains("calf", ignoreCase = true) || raw.contains("calves", ignoreCase = true) -> "Calves"
        raw.contains("core", ignoreCase = true) || raw.contains("abs", ignoreCase = true) -> "Core"
        else -> raw
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
