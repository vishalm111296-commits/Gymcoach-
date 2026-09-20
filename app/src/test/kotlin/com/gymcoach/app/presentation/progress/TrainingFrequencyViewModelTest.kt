package com.gymcoach.app.presentation.progress

import com.gymcoach.app.domain.model.WorkoutWithStats
import com.gymcoach.app.domain.repository.WorkoutRepository
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
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoField

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingFrequencyViewModelTest {

    private val workoutRepository = mockk<WorkoutRepository>()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test calculations with completed workouts`() = runTest(testDispatcher) {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val twoDaysAgo = today.minusDays(2)
        val fourDaysAgo = today.minusDays(4) // Break the streak

        val workouts = listOf(
            createWorkoutForDate(today),
            createWorkoutForDate(yesterday),
            createWorkoutForDate(twoDaysAgo),
            createWorkoutForDate(fourDaysAgo)
        )

        every { workoutRepository.getCompletedWorkouts() } returns flowOf(workouts)

        val viewModel = TrainingFrequencyViewModel(workoutRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(false, state.isLoading)
        
        // Count how many of these fall into the current week based on ALIGNED_WEEK_OF_YEAR logic in the view model
        val expectedWeekCount = workouts.count {
            it.date.atZone(ZoneId.systemDefault()).toLocalDate().get(ChronoField.ALIGNED_WEEK_OF_YEAR) == today.get(ChronoField.ALIGNED_WEEK_OF_YEAR) &&
            it.date.atZone(ZoneId.systemDefault()).toLocalDate().year == today.year
        }
        
        assertEquals(expectedWeekCount, state.workoutsThisWeek)
        
        // Month count
        val expectedMonthCount = workouts.count {
            it.date.atZone(ZoneId.systemDefault()).toLocalDate().month == today.month &&
            it.date.atZone(ZoneId.systemDefault()).toLocalDate().year == today.year
        }
        
        assertEquals(expectedMonthCount, state.workoutsThisMonth)

        // The best streak is 3 (today, yesterday, twoDaysAgo)
        assertEquals(3, state.bestStreak)
    }

    @Test
    fun `test zero workouts state`() = runTest(testDispatcher) {
        every { workoutRepository.getCompletedWorkouts() } returns flowOf(emptyList())

        val viewModel = TrainingFrequencyViewModel(workoutRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(0, state.workoutsThisWeek)
        assertEquals(0, state.workoutsThisMonth)
        assertEquals(0, state.bestStreak)
        assertEquals(true, state.heatmapData.isEmpty())
        assertEquals(true, state.dayFrequency.isEmpty())
        assertEquals(true, state.monthlyData.isEmpty())
    }

    private fun createWorkoutForDate(date: LocalDate): WorkoutWithStats {
        val instant = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        return WorkoutWithStats(
            id = 1L,
            date = instant,
            startTime = instant,
            endTime = instant,
            duration = 3600,
            notes = "",
            completed = true,
            volume = 1000.0,
            setCount = 10,
            repCount = 100,
            exerciseCount = 5
        )
    }
}
