package com.gymcoach.app.presentation.progress

import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.core.progression.PRDetector
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import com.gymcoach.app.domain.repository.WorkoutRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressionAnalyticsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var prDetector: PRDetector
    private lateinit var viewModel: ProgressionAnalyticsViewModel

    // Helper: create a SetWithContext
    private fun makeSet(
        exerciseId: Long,
        weight: Double,
        reps: Int,
        workoutDate: Long,
        id: Long = 1L,
        completed: Boolean = true
    ) = VolumeCalculator.SetWithContext(
        set = WorkoutSetEntity(
            id = id,
            workoutExerciseId = 1L,
            setNumber = 1,
            weight = weight,
            reps = reps,
            rpe = 8.0,
            restSeconds = 90,
            completed = completed,
            setType = 0
        ),
        exerciseId = exerciseId,
        workoutDate = workoutDate
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        workoutRepository = mockk(relaxed = true)
        prDetector = PRDetector()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isLoading() {
        every { workoutRepository.getCompletedSetsWithContext() } returns flowOf(emptyList())
        viewModel = ProgressionAnalyticsViewModel(workoutRepository, prDetector)
        // Before loadExercise is called, state is initial
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun loadExercise_withNoData_setsIsEmpty() = runTest {
        every { workoutRepository.getCompletedSetsWithContext() } returns flowOf(emptyList())
        viewModel = ProgressionAnalyticsViewModel(workoutRepository, prDetector)

        val collectJob = backgroundScope.launch { viewModel.uiState.collect { } }
        viewModel.loadExercise(1L, "Bench Press")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isEmpty)
        assertEquals(0, state.e1rmTrend.size)
        assertEquals(0, state.prHistory.size)
        collectJob.cancel()
    }

    @Test
    fun loadExercise_withOnlyOtherExercise_setsIsEmpty() = runTest {
        val sets = listOf(makeSet(exerciseId = 99L, weight = 100.0, reps = 5, workoutDate = 1_000_000L))
        every { workoutRepository.getCompletedSetsWithContext() } returns flowOf(sets)
        viewModel = ProgressionAnalyticsViewModel(workoutRepository, prDetector)

        val collectJob = backgroundScope.launch { viewModel.uiState.collect { } }
        viewModel.loadExercise(1L, "Bench Press")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEmpty)
        collectJob.cancel()
    }

    @Test
    fun loadExercise_withSingleSet_computesE1RMCorrectly() = runTest {
        // Epley: 100 * (1 + 5/30.0) = 116.67
        val sets = listOf(makeSet(exerciseId = 1L, weight = 100.0, reps = 5, workoutDate = 1_000_000L))
        every { workoutRepository.getCompletedSetsWithContext() } returns flowOf(sets)
        viewModel = ProgressionAnalyticsViewModel(workoutRepository, prDetector)

        val collectJob = backgroundScope.launch { viewModel.uiState.collect { } }
        viewModel.loadExercise(1L, "Bench Press")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isEmpty)
        assertEquals(1, state.e1rmTrend.size)
        assertEquals(100.0 * (1 + 5.0 / 30.0), state.e1rmTrend[0].e1rm, 0.01)
        assertEquals(1, state.prHistory.size) // First session is always a PR
        assertEquals(1, state.totalSessions)
        collectJob.cancel()
    }

    @Test
    fun loadExercise_multipleSessionsProgressingWeight_incrementsPRCount() = runTest {
        val day1 = 1_000_000L
        val day2 = 2_000_000L
        val day3 = 3_000_000L
        val sets = listOf(
            makeSet(exerciseId = 1L, weight = 60.0, reps = 8, workoutDate = day1, id = 1),
            makeSet(exerciseId = 1L, weight = 65.0, reps = 8, workoutDate = day2, id = 2),
            makeSet(exerciseId = 1L, weight = 70.0, reps = 8, workoutDate = day3, id = 3)
        )
        every { workoutRepository.getCompletedSetsWithContext() } returns flowOf(sets)
        viewModel = ProgressionAnalyticsViewModel(workoutRepository, prDetector)

        val collectJob = backgroundScope.launch { viewModel.uiState.collect { } }
        viewModel.loadExercise(1L, "Bench Press")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.e1rmTrend.size)
        assertEquals(3, state.prHistory.size) // Each session is a new PR
        assertEquals(3, state.totalSessions)
        // Most recent first
        assertEquals(70.0, state.prHistory.first().weight, 0.01)
        collectJob.cancel()
    }

    @Test
    fun loadExercise_withRegressingWeight_doesNotAddFalsePR() = runTest {
        val day1 = 1_000_000L
        val day2 = 2_000_000L
        // Day 2 is lower weight - should not be a PR
        val sets = listOf(
            makeSet(exerciseId = 1L, weight = 100.0, reps = 5, workoutDate = day1, id = 1),
            makeSet(exerciseId = 1L, weight = 80.0, reps = 5, workoutDate = day2, id = 2)
        )
        every { workoutRepository.getCompletedSetsWithContext() } returns flowOf(sets)
        viewModel = ProgressionAnalyticsViewModel(workoutRepository, prDetector)

        val collectJob = backgroundScope.launch { viewModel.uiState.collect { } }
        viewModel.loadExercise(1L, "Bench Press")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.totalSessions)
        assertEquals(1, state.prHistory.size) // Only first session is a PR
        assertEquals(100.0 * (1 + 5.0 / 30.0), state.peakE1RM, 0.01)
        // recentE1RM is lower than peak
        assertTrue(state.recentE1RM < state.peakE1RM)
        collectJob.cancel()
    }

    @Test
    fun loadExercise_skipsIncompleteSets() = runTest {
        val sets = listOf(
            makeSet(exerciseId = 1L, weight = 100.0, reps = 5, workoutDate = 1_000_000L, completed = false)
        )
        every { workoutRepository.getCompletedSetsWithContext() } returns flowOf(sets)
        viewModel = ProgressionAnalyticsViewModel(workoutRepository, prDetector)

        val collectJob = backgroundScope.launch { viewModel.uiState.collect { } }
        viewModel.loadExercise(1L, "Bench Press")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEmpty)
        collectJob.cancel()
    }

    @Test
    fun loadExercise_weeklyVolumeAggregation_computesCorrectly() = runTest {
        // 2 sets in same workout (same workoutDate)
        val workoutDate = 1_700_000_000_000L // Nov 2023
        val sets = listOf(
            makeSet(exerciseId = 1L, weight = 60.0, reps = 10, workoutDate = workoutDate, id = 1),
            makeSet(exerciseId = 1L, weight = 60.0, reps = 10, workoutDate = workoutDate, id = 2)
        )
        every { workoutRepository.getCompletedSetsWithContext() } returns flowOf(sets)
        viewModel = ProgressionAnalyticsViewModel(workoutRepository, prDetector)

        val collectJob = backgroundScope.launch { viewModel.uiState.collect { } }
        viewModel.loadExercise(1L, "Bench Press")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.weeklyVolume.size)
        // Both sets: 60 * 10 + 60 * 10 = 1200
        assertEquals(1200.0, state.weeklyVolume.first().volumeKg, 0.01)
        collectJob.cancel()
    }
}
