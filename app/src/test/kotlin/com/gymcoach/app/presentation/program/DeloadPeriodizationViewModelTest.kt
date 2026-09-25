package com.gymcoach.app.presentation.program

import com.gymcoach.app.core.program.DeloadEngine
import com.gymcoach.app.core.program.DeloadReason
import com.gymcoach.app.core.program.DeloadStatus
import com.gymcoach.app.data.local.entity.ProgramEntity
import com.gymcoach.app.data.local.entity.ReadinessEntity
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutWithDetails
import com.gymcoach.app.domain.model.WorkoutWithStats
import com.gymcoach.app.domain.repository.ProgramRepository
import com.gymcoach.app.domain.repository.ReadinessRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class DeloadPeriodizationViewModelTest {

    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var readinessRepository: ReadinessRepository
    private lateinit var programRepository: ProgramRepository
    private lateinit var deloadEngine: DeloadEngine
    private lateinit var viewModel: DeloadPeriodizationViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        workoutRepository = mockk()
        readinessRepository = mockk()
        programRepository = mockk()
        deloadEngine = mockk()

        // Default mock responses
        val activeProgram = ProgramEntity(id = 1L, name = "Test Program", description = "", goal = "", isActive = true)
        every { programRepository.getActiveProgram() } returns flowOf(activeProgram)

        // Mock 4 workouts to simulate currentWeek = 2
        val mockWorkout = Workout(
            id = 1L,
            date = Instant.now(),
            startTime = Instant.now(),
            endTime = Instant.now(),
            duration = 0L,
            notes = "",
            completed = true,
            status = "COMPLETED"
        )
        val mockWorkoutStats = WorkoutWithStats(
            id = 1L,
            date = Instant.now(),
            startTime = Instant.now(),
            endTime = Instant.now(),
            duration = 0L,
            notes = "",
            completed = true,
            status = "COMPLETED",
            volume = 0.0,
            setCount = 1,
            repCount = 1,
            exerciseCount = 1
        )
        every { workoutRepository.getCompletedWorkouts() } returns flowOf(List(4) { mockWorkoutStats })
        val mockWorkoutWithDetails = WorkoutWithDetails(mockWorkout, emptyList())
        every { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(mockWorkoutWithDetails)

        val readinessList = listOf(ReadinessEntity(
            id = 1L,
            userId = 1L,
            recordedAt = System.currentTimeMillis(),
            sleepQuality = 2,
            soreness = 2,
            energy = 3,
            motivation = 3,
            notes = ""
        ))
        every { readinessRepository.getAllReadiness() } returns flowOf(readinessList)

        every { deloadEngine.evaluateDeloadNeed(any(), any(), any(), any()) } returns DeloadStatus(
            isDeloadRecommended = true,
            reason = DeloadReason.FATIGUE_ACCUMULATION,
            accumulatedWeeks = 2,
            volumeReductionPercent = 50,
            intensityReductionPercent = 10,
            coachingAdvice = "Test Advice"
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialization loads data and evaluates deload need correctly`() = runTest {
        viewModel = DeloadPeriodizationViewModel(workoutRepository, readinessRepository, programRepository, deloadEngine)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Test Program", state.programName)
        assertEquals(2, state.currentBlockWeek)
        assertEquals(50, state.readinessAvg)
        assertNotNull(state.deloadStatus)
        assertTrue(state.deloadStatus!!.isDeloadRecommended)
        assertEquals(DeloadReason.FATIGUE_ACCUMULATION, state.deloadStatus!!.reason)
    }

    @Test
    fun `toggleDeloadProtocol toggles active state`() = runTest {
        viewModel = DeloadPeriodizationViewModel(workoutRepository, readinessRepository, programRepository, deloadEngine)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDeloadActive)

        viewModel.toggleDeloadProtocol()
        assertTrue(viewModel.uiState.value.isDeloadActive)

        viewModel.toggleDeloadProtocol()
        assertFalse(viewModel.uiState.value.isDeloadActive)
    }

    @Test
    fun `loadPeriodizationData surfaces error state on repository exception`() = runTest {
        every { programRepository.getActiveProgram() } throws RuntimeException("DB unavailable")

        viewModel = DeloadPeriodizationViewModel(workoutRepository, readinessRepository, programRepository, deloadEngine)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("DB unavailable"))
    }

    @Test
    fun `loadPeriodizationData with empty readiness uses default avgReadiness of 75`() = runTest {
        every { readinessRepository.getAllReadiness() } returns flowOf(emptyList())

        viewModel = DeloadPeriodizationViewModel(workoutRepository, readinessRepository, programRepository, deloadEngine)
        advanceUntilIdle()

        assertEquals(75, viewModel.uiState.value.readinessAvg)
    }
}
