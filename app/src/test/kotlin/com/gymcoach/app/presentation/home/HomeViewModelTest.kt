package com.gymcoach.app.presentation.home

import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.data.local.entity.ProgramEntity
import com.gymcoach.app.data.local.entity.ReadinessEntity
import com.gymcoach.app.domain.repository.AnalyticsRepository
import com.gymcoach.app.domain.repository.ProgramRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import com.gymcoach.app.domain.usecase.readiness.GetLatestReadinessUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var programRepository: ProgramRepository
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var volumeCalculator: VolumeCalculator
    private lateinit var getLatestReadinessUseCase: GetLatestReadinessUseCase
    private lateinit var analyticsRepository: AnalyticsRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        programRepository = mockk()
        workoutRepository = mockk()
        volumeCalculator = mockk()
        getLatestReadinessUseCase = mockk()
        analyticsRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when no program exists, state shows no program and default readiness`() = runTest {
        // Given
        coEvery { analyticsRepository.getAllPersonalRecords() } returns emptyList()
        coEvery { analyticsRepository.getTotalWorkouts() } returns 0
        every { programRepository.getActiveProgram() } returns flowOf(null)
        every { workoutRepository.getCompletedWorkouts() } returns flowOf(emptyList())
        every { getLatestReadinessUseCase() } returns flowOf(null)

        val viewModel = HomeViewModel(
            programRepository,
            workoutRepository,
            volumeCalculator,
            getLatestReadinessUseCase,
            analyticsRepository
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.hasProgram)
        assertEquals("Your first session is ready once you set up your plan.", state.trainingInsight)
        assertFalse(state.readiness.isLoggedToday)
        assertEquals("No readiness logged today", state.readiness.recommendation)
        assertEquals(0, state.totalWorkouts)
        assertEquals(0, state.prCount)
    }

    @Test
    fun `when program exists but no readiness, state shows program and no readiness logged`() = runTest {
        // Given
        val programId = 1L
        val program = ProgramEntity(id = programId, daysPerWeek = 4, name = "Test Plan")

        coEvery { analyticsRepository.getAllPersonalRecords() } returns listOf(mockk())
        coEvery { analyticsRepository.getTotalWorkouts() } returns 5
        every { programRepository.getActiveProgram() } returns flowOf(program)
        every { programRepository.getDaysForProgram(programId) } returns flowOf(emptyList())
        every { programRepository.getExercisesForDays(any()) } returns flowOf(emptyMap())
        every { workoutRepository.getCompletedWorkouts() } returns flowOf(emptyList())
        every { getLatestReadinessUseCase() } returns flowOf(null)

        every { volumeCalculator.calculateVtaperBalance(any()) } returns VolumeCalculator.VtaperBalance(
            primaryScore = 0.0, secondaryScore = 0.0, overallBalance = "Low V-taper volume"
        )

        val viewModel = HomeViewModel(
            programRepository,
            workoutRepository,
            volumeCalculator,
            getLatestReadinessUseCase,
            analyticsRepository
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.hasProgram)
        assertEquals("Low V-taper volume", state.trainingInsight)
        assertFalse(state.readiness.isLoggedToday)
        assertEquals("No readiness logged today", state.readiness.recommendation)
        assertEquals(5, state.totalWorkouts)
        assertEquals(1, state.prCount)
    }

    @Test
    fun `when readiness is logged today, state shows readiness recommendation`() = runTest {
        // Given
        val programId = 1L
        val program = ProgramEntity(id = programId, daysPerWeek = 4, name = "Test Plan")

        coEvery { analyticsRepository.getAllPersonalRecords() } returns emptyList()
        coEvery { analyticsRepository.getTotalWorkouts() } returns 5
        every { programRepository.getActiveProgram() } returns flowOf(program)
        every { programRepository.getDaysForProgram(programId) } returns flowOf(emptyList())
        every { programRepository.getExercisesForDays(any()) } returns flowOf(emptyMap())
        every { workoutRepository.getCompletedWorkouts() } returns flowOf(emptyList())

        val readinessEntity = ReadinessEntity(
            recordedAt = System.currentTimeMillis(),
            sleepQuality = 5, soreness = 5, energy = 5, motivation = 5
        )
        every { getLatestReadinessUseCase() } returns flowOf(readinessEntity)

        every { volumeCalculator.calculateVtaperBalance(any()) } returns VolumeCalculator.VtaperBalance(
            primaryScore = 0.0, secondaryScore = 0.0, overallBalance = "Insight"
        )

        val viewModel = HomeViewModel(
            programRepository,
            workoutRepository,
            volumeCalculator,
            getLatestReadinessUseCase,
            analyticsRepository
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.readiness.isLoggedToday)
        assertEquals("Full intensity session recommended", state.readiness.recommendation)
    }
}
