package com.gymcoach.app.presentation.home

import com.gymcoach.app.data.local.entity.NutritionLogEntity
import com.gymcoach.app.data.local.entity.ReadinessEntity
import com.gymcoach.app.domain.model.WorkoutWithStats
import com.gymcoach.app.domain.repository.AnalyticsRepository
import com.gymcoach.app.domain.repository.ExerciseRepository
import com.gymcoach.app.domain.repository.NutritionRepository
import com.gymcoach.app.domain.repository.ProgramRepository
import com.gymcoach.app.domain.repository.ReadinessRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import com.gymcoach.app.core.program.VolumeCalculator
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var programRepository: ProgramRepository
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var volumeCalculator: VolumeCalculator
    private lateinit var analyticsRepository: AnalyticsRepository
    private lateinit var readinessRepository: ReadinessRepository
    private lateinit var nutritionRepository: NutritionRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        programRepository = mockk()
        workoutRepository = mockk()
        exerciseRepository = mockk()
        volumeCalculator = mockk(relaxed = true)
        analyticsRepository = mockk()
        readinessRepository = mockk()
        nutritionRepository = mockk()

        // Default stubs
        every { programRepository.getActiveProgram() } returns flowOf(com.gymcoach.app.data.local.entity.ProgramEntity(id = 1, name = "Test", daysPerWeek = 4, durationWeeks = 4, isActive = true, createdAt = 0))
        every { programRepository.getDaysForProgram(any()) } returns flowOf(emptyList())
        every { programRepository.getExercisesForDays(any()) } returns flowOf(emptyMap())
        every { workoutRepository.getCompletedSetsWithContext() } returns flowOf(emptyList())
        every { exerciseRepository.getAllExercises() } returns flowOf(emptyList())
        every { exerciseRepository.getAllExerciseMuscleDetails() } returns flowOf(emptyList())
        coEvery { analyticsRepository.getAllPersonalRecords() } returns emptyList()
        every { readinessRepository.getLatestReadiness() } returns flowOf(null)
        every { nutritionRepository.getLogsForDay(any(), any()) } returns flowOf(emptyList())
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `weeklyWorkoutCount reflects completed workouts this week`() = runTest {
        val weekStart = java.util.Calendar.getInstance().apply {
            firstDayOfWeek = java.util.Calendar.MONDAY
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val mockWorkouts = listOf(
            mockk<WorkoutWithStats> {
                every { completed } returns true
                every { date } returns Instant.ofEpochMilli(weekStart + 1000)
            },
            mockk<WorkoutWithStats> {
                every { completed } returns true
                every { date } returns Instant.ofEpochMilli(weekStart + 2000)
            },
            mockk<WorkoutWithStats> {
                every { completed } returns false
                every { date } returns Instant.ofEpochMilli(weekStart + 3000)
            }
        )

        val workoutsFlow = MutableStateFlow(mockWorkouts)
        every { workoutRepository.getCompletedWorkouts() } returns workoutsFlow

        val viewModel = HomeViewModel(
            programRepository, workoutRepository, exerciseRepository, volumeCalculator,
            analyticsRepository, readinessRepository, nutritionRepository
        )

        val job = launch(testDispatcher) { viewModel.weeklyWorkoutCount.collect {} }

        advanceUntilIdle()

        assertEquals(2, viewModel.weeklyWorkoutCount.value)
        job.cancel()
    }

    @Test
    fun `latestReadiness emits correctly`() = runTest {
        every { workoutRepository.getCompletedWorkouts() } returns flowOf(emptyList())

        val readinessEntity = mockk<ReadinessEntity> {
            every { readinessScore } returns 85.0
        }
        val readinessFlow = MutableStateFlow<ReadinessEntity?>(readinessEntity)
        every { readinessRepository.getLatestReadiness() } returns readinessFlow

        val viewModel = HomeViewModel(
            programRepository, workoutRepository, exerciseRepository, volumeCalculator,
            analyticsRepository, readinessRepository, nutritionRepository
        )

        val job = launch(testDispatcher) { viewModel.latestReadiness.collect {} }

        advanceUntilIdle()

        assertEquals(85, viewModel.latestReadiness.value)
        job.cancel()
    }

    @Test
    fun `todayCalories reactively sums calories of all logs emitted`() = runTest {
        every { workoutRepository.getCompletedWorkouts() } returns flowOf(emptyList())

        val logs = listOf(
            NutritionLogEntity(date = 0, mealName = "Meal1", calories = 300, proteinGrams = 0f, carbsGrams = 0f, fatGrams = 0f),
            NutritionLogEntity(date = 0, mealName = "Meal2", calories = 550, proteinGrams = 0f, carbsGrams = 0f, fatGrams = 0f)
        )
        val logsFlow = MutableStateFlow(logs)
        every { nutritionRepository.getLogsForDay(any(), any()) } returns logsFlow

        val viewModel = HomeViewModel(
            programRepository, workoutRepository, exerciseRepository, volumeCalculator,
            analyticsRepository, readinessRepository, nutritionRepository
        )

        val job = launch(testDispatcher) { viewModel.todayCalories.collect {} }

        advanceUntilIdle()

        assertEquals(850, viewModel.todayCalories.value)
        job.cancel()
    }
}
