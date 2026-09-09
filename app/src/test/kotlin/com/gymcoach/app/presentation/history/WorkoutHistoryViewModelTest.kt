package com.gymcoach.app.presentation.history

import com.gymcoach.app.core.timer.RestTimerManager
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutWithStats
import com.gymcoach.app.domain.repository.WorkoutRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutHistoryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var restTimerManager: RestTimerManager
    private lateinit var viewModel: WorkoutHistoryViewModel

    private val completedWorkoutsFlow = MutableStateFlow<List<WorkoutWithStats>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        workoutRepository = mockk(relaxed = true)
        restTimerManager = mockk(relaxed = true)

        every { workoutRepository.getCompletedWorkouts() } returns completedWorkoutsFlow
        coEvery { workoutRepository.getIncompleteWorkout() } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createWorkoutWithStats(
        id: Long,
        dateMillis: Long,
        volume: Double = 1000.0,
        duration: Long = 3600L,
        notes: String = "Test workout $id"
    ): WorkoutWithStats {
        val instant = Instant.ofEpochMilli(dateMillis)
        return WorkoutWithStats(
            id = id,
            date = instant,
            startTime = instant,
            endTime = instant.plusSeconds(duration),
            duration = duration,
            notes = notes,
            completed = true,
            status = "COMPLETED",
            volume = volume,
            setCount = 10,
            repCount = 100,
            exerciseCount = 3
        )
    }

    @Test
    fun `initial state loads completed workouts and incomplete workout`() = runTest {
        val workout1 = createWorkoutWithStats(1L, System.currentTimeMillis())
        val incomplete = Workout(
            id = 99L,
            date = Instant.now(),
            startTime = Instant.now(),
            endTime = Instant.now(),
            duration = 0,
            notes = "Incomplete",
            completed = false,
            status = "ACTIVE"
        )

        completedWorkoutsFlow.value = listOf(workout1)
        coEvery { workoutRepository.getIncompleteWorkout() } returns incomplete

        viewModel = WorkoutHistoryViewModel(workoutRepository, restTimerManager)

        assertEquals(listOf(workout1), viewModel.workouts.value)
        assertEquals(incomplete, viewModel.getIncompleteWorkout())
        assertEquals(incomplete, viewModel.incompleteWorkout.value)
        assertEquals("", viewModel.searchQuery.value)
        assertEquals(WorkoutHistoryViewModel.FilterOption.ALL, viewModel.filterOption.value)
        assertEquals(WorkoutHistoryViewModel.SortOption.NEWEST, viewModel.sortOption.value)
        assertNull(viewModel.deleteTarget.value)
    }

    @Test
    fun `non-blank search query triggers searchWorkouts repository call`() = runTest {
        val searchResult = listOf(createWorkoutWithStats(2L, System.currentTimeMillis(), notes = "Leg Day"))
        coEvery { workoutRepository.searchWorkouts("Leg") } returns searchResult

        viewModel = WorkoutHistoryViewModel(workoutRepository, restTimerManager)
        viewModel.onSearchQueryChange("Leg")

        assertEquals("Leg", viewModel.searchQuery.value)
        assertEquals(searchResult, viewModel.workouts.value)
        coVerify { workoutRepository.searchWorkouts("Leg") }
    }

    @Test
    fun `blank search query reverts to getCompletedWorkouts`() = runTest {
        val workout1 = createWorkoutWithStats(1L, System.currentTimeMillis())
        val searchResult = listOf(createWorkoutWithStats(2L, System.currentTimeMillis()))
        coEvery { workoutRepository.searchWorkouts("Push") } returns searchResult

        completedWorkoutsFlow.value = listOf(workout1)

        viewModel = WorkoutHistoryViewModel(workoutRepository, restTimerManager)
        viewModel.onSearchQueryChange("Push")
        assertEquals(searchResult, viewModel.workouts.value)

        viewModel.onSearchQueryChange("   ")
        assertEquals(listOf(workout1), viewModel.workouts.value)
    }

    @Test
    fun `filter TODAY includes workouts from today start`() = runTest {
        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val workoutToday = createWorkoutWithStats(1L, todayStart + 3600000L)
        val workoutYesterday = createWorkoutWithStats(2L, todayStart - 3600000L)

        completedWorkoutsFlow.value = listOf(workoutToday, workoutYesterday)

        viewModel = WorkoutHistoryViewModel(workoutRepository, restTimerManager)
        viewModel.onFilterChange(WorkoutHistoryViewModel.FilterOption.TODAY)

        assertEquals(listOf(workoutToday), viewModel.workouts.value)
    }

    @Test
    fun `filter THIS_WEEK includes workouts within last 7 days`() = runTest {
        val now = System.currentTimeMillis()
        val workout5DaysAgo = createWorkoutWithStats(1L, now - 5L * 24 * 60 * 60 * 1000)
        val workout10DaysAgo = createWorkoutWithStats(2L, now - 10L * 24 * 60 * 60 * 1000)

        completedWorkoutsFlow.value = listOf(workout5DaysAgo, workout10DaysAgo)

        viewModel = WorkoutHistoryViewModel(workoutRepository, restTimerManager)
        viewModel.onFilterChange(WorkoutHistoryViewModel.FilterOption.THIS_WEEK)

        assertEquals(listOf(workout5DaysAgo), viewModel.workouts.value)
    }

    @Test
    fun `filter THIS_MONTH includes workouts within last 30 days`() = runTest {
        val now = System.currentTimeMillis()
        val workout20DaysAgo = createWorkoutWithStats(1L, now - 20L * 24 * 60 * 60 * 1000)
        val workout40DaysAgo = createWorkoutWithStats(2L, now - 40L * 24 * 60 * 60 * 1000)

        completedWorkoutsFlow.value = listOf(workout20DaysAgo, workout40DaysAgo)

        viewModel = WorkoutHistoryViewModel(workoutRepository, restTimerManager)
        viewModel.onFilterChange(WorkoutHistoryViewModel.FilterOption.THIS_MONTH)

        assertEquals(listOf(workout20DaysAgo), viewModel.workouts.value)
    }

    @Test
    fun `filter CUSTOM filters correctly by start and end dates`() = runTest {
        val start = 100000L
        val end = 200000L

        val workoutBefore = createWorkoutWithStats(1L, 50000L)
        val workoutInside = createWorkoutWithStats(2L, 150000L)
        val workoutAfter = createWorkoutWithStats(3L, 250000L)

        completedWorkoutsFlow.value = listOf(workoutBefore, workoutInside, workoutAfter)

        viewModel = WorkoutHistoryViewModel(workoutRepository, restTimerManager)
        viewModel.onCustomDateRangeChange(start, end)
        viewModel.onFilterChange(WorkoutHistoryViewModel.FilterOption.CUSTOM)

        assertEquals(start, viewModel.customStartDate.value)
        assertEquals(end, viewModel.customEndDate.value)
        assertEquals(listOf(workoutInside), viewModel.workouts.value)
    }

    @Test
    fun `sorting options order workouts properly`() = runTest {
        val w1 = createWorkoutWithStats(1L, dateMillis = 1000L, volume = 500.0, duration = 1800L)
        val w2 = createWorkoutWithStats(2L, dateMillis = 3000L, volume = 1500.0, duration = 3600L)
        val w3 = createWorkoutWithStats(3L, dateMillis = 2000L, volume = 1000.0, duration = 2700L)

        completedWorkoutsFlow.value = listOf(w1, w2, w3)
        viewModel = WorkoutHistoryViewModel(workoutRepository, restTimerManager)

        // Default NEWEST
        assertEquals(listOf(w2, w3, w1), viewModel.workouts.value)

        // OLDEST
        viewModel.onSortChange(WorkoutHistoryViewModel.SortOption.OLDEST)
        assertEquals(listOf(w1, w3, w2), viewModel.workouts.value)

        // VOLUME_DESC
        viewModel.onSortChange(WorkoutHistoryViewModel.SortOption.VOLUME_DESC)
        assertEquals(listOf(w2, w3, w1), viewModel.workouts.value)

        // VOLUME_ASC
        viewModel.onSortChange(WorkoutHistoryViewModel.SortOption.VOLUME_ASC)
        assertEquals(listOf(w1, w3, w2), viewModel.workouts.value)

        // DURATION_DESC
        viewModel.onSortChange(WorkoutHistoryViewModel.SortOption.DURATION_DESC)
        assertEquals(listOf(w2, w3, w1), viewModel.workouts.value)

        // DURATION_ASC
        viewModel.onSortChange(WorkoutHistoryViewModel.SortOption.DURATION_ASC)
        assertEquals(listOf(w1, w3, w2), viewModel.workouts.value)
    }

    @Test
    fun `delete workout confirmation flow calls deleteWorkout repository method`() = runTest {
        viewModel = WorkoutHistoryViewModel(workoutRepository, restTimerManager)

        // Selection & Delete target setting
        viewModel.onWorkoutClick(10L)
        viewModel.onDeleteClick(10L)
        assertEquals(10L, viewModel.deleteTarget.value)

        // Cancel delete
        viewModel.cancelDelete()
        assertNull(viewModel.deleteTarget.value)

        // Confirm delete
        viewModel.onDeleteClick(10L)
        viewModel.confirmDelete()
        coVerify { workoutRepository.deleteWorkout(10L) }
    }
}
