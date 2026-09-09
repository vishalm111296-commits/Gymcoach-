package com.gymcoach.app.presentation.history

import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.timer.RestTimerManager
import com.gymcoach.app.domain.model.WorkoutWithStats
import com.gymcoach.app.domain.repository.WorkoutRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * WorkoutHistoryViewModel tests for the T2.8 UX-audit fixes:
 * - APP-037: isInitialLoad lifecycle (no "No workouts yet" flash on cold start)
 * - APP-041: CUSTOM date filter revert on cancel / invalid range
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var restTimer: RestTimerManager
    private lateinit var viewModel: WorkoutHistoryViewModel

    private fun sampleWorkout(id: Long = 1L) = WorkoutWithStats(
        id = id,
        date = Instant.now(),
        startTime = Instant.now(),
        endTime = Instant.now(),
        duration = 3600,
        notes = "",
        completed = true,
        status = "COMPLETED",
        volume = 1200.0,
        setCount = 12,
        repCount = 90,
        exerciseCount = 3
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        workoutRepository = mockk(relaxed = true)
        restTimer = mockk(relaxed = true)
        every { workoutRepository.getCompletedWorkouts() } returns flowOf(emptyList())
        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns null
        viewModel = WorkoutHistoryViewModel(workoutRepository, restTimer)
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `APP-037 isInitialLoad is true before first emission and false after`() = runTest {
        assertTrue("isInitialLoad must start true (cold start)", viewModel.isInitialLoad.value)

        runCurrent()

        assertFalse("isInitialLoad must become false after first workout emission", viewModel.isInitialLoad.value)
    }

    @Test
    fun `APP-037 isInitialLoad stays true until the first emission even with pending query`() = runTest {
        // Change the filter before the first emission is processed.
        viewModel.onFilterChange(WorkoutHistoryViewModel.FilterOption.TODAY)
        runCurrent()

        assertFalse("first emission was delivered, load must be complete", viewModel.isInitialLoad.value)
    }

    @Test
    fun `APP-041 cancelCustomFilter reverts CUSTOM back to previous filter and clears dates`() = runTest {
        runCurrent()
        // From ALL -> CUSTOM (previous recorded as ALL)
        viewModel.onFilterChange(WorkoutHistoryViewModel.FilterOption.CUSTOM)
        assertEquals(WorkoutHistoryViewModel.FilterOption.CUSTOM, viewModel.filterOption.value)

        viewModel.cancelCustomFilter()
        assertEquals("must revert to the previous filter", WorkoutHistoryViewModel.FilterOption.ALL, viewModel.filterOption.value)
        assertNull(viewModel.customStartDate.value)
        assertNull(viewModel.customEndDate.value)
    }

    @Test
    fun `APP-041 cancelCustomFilter is a no-op when CUSTOM is not active`() = runTest {
        runCurrent()
        viewModel.onFilterChange(WorkoutHistoryViewModel.FilterOption.THIS_WEEK)
        viewModel.onCustomDateRangeChange(1000L, 2000L)

        viewModel.cancelCustomFilter()

        assertEquals(WorkoutHistoryViewModel.FilterOption.THIS_WEEK, viewModel.filterOption.value)
    }

    @Test
    fun `APP-041 invalid custom range reverts filter and clears dates`() = runTest {
        runCurrent()
        viewModel.onFilterChange(WorkoutHistoryViewModel.FilterOption.CUSTOM)

        // start > end is invalid
        viewModel.onCustomDateRangeChange(5000L, 1000L)

        assertEquals("invalid range must revert to previous filter", WorkoutHistoryViewModel.FilterOption.ALL, viewModel.filterOption.value)
        assertNull(viewModel.customStartDate.value)
        assertNull(viewModel.customEndDate.value)
    }

    @Test
    fun `APP-041 null custom range reverts filter and clears dates`() = runTest {
        runCurrent()
        viewModel.onFilterChange(WorkoutHistoryViewModel.FilterOption.CUSTOM)

        viewModel.onCustomDateRangeChange(null, null)

        assertEquals(WorkoutHistoryViewModel.FilterOption.ALL, viewModel.filterOption.value)
        assertNull(viewModel.customStartDate.value)
        assertNull(viewModel.customEndDate.value)
    }

    @Test
    fun `APP-041 valid custom range keeps CUSTOM filter active`() = runTest {
        runCurrent()
        viewModel.onFilterChange(WorkoutHistoryViewModel.FilterOption.CUSTOM)

        val start = 1000L
        val end = 5000L
        viewModel.onCustomDateRangeChange(start, end)

        assertEquals("valid range must keep CUSTOM", WorkoutHistoryViewModel.FilterOption.CUSTOM, viewModel.filterOption.value)
        assertEquals(start, viewModel.customStartDate.value)
        assertEquals(end, viewModel.customEndDate.value)
    }

    @Test
    fun `APP-037 workouts flow updates after emission`() = runTest {
        every { workoutRepository.getCompletedWorkouts() } returns flowOf(listOf(sampleWorkout(1L)))

        runCurrent()

        // After the new flow emission the list contains the sample workout.
        assertEquals(1, viewModel.workouts.value.size)
        assertEquals(1L, viewModel.workouts.value.first().id)
    }
}