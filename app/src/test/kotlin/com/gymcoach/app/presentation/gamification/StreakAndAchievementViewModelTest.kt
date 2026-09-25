package com.gymcoach.app.presentation.gamification

import com.gymcoach.app.core.gamification.StreakAndAchievementEngine
import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import com.gymcoach.app.domain.model.WorkoutWithStats
import com.gymcoach.app.domain.repository.WorkoutRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class StreakAndAchievementViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var workoutRepository: WorkoutRepository
    private val clock = Clock.fixed(Instant.parse("2026-09-24T12:00:00Z"), ZoneId.of("UTC"))
    private val streakEngine = StreakAndAchievementEngine(clock)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        workoutRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `no completed workouts emits Empty state`() = runTest(testDispatcher) {
        every { workoutRepository.getCompletedWorkouts() } returns flowOf(emptyList())
        every { workoutRepository.getCompletedSetsWithContext() } returns flowOf(emptyList())

        val viewModel = StreakAndAchievementViewModel(workoutRepository, streakEngine)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is StreakUiState.Empty)
    }

    @Test
    fun `completed workouts emit Success state with streak report`() = runTest(testDispatcher) {
        val now = Instant.parse("2026-09-23T10:00:00Z")
        val workouts = listOf(
            WorkoutWithStats(
                id = 1L,
                date = now,
                startTime = now,
                endTime = now.plusSeconds(3600),
                duration = 3600L,
                notes = "Upper body power",
                completed = true,
                status = "COMPLETED",
                volume = 5000.0,
                setCount = 15,
                repCount = 120,
                exerciseCount = 5
            )
        )
        val sets = listOf(
            VolumeCalculator.SetWithContext(
                set = WorkoutSetEntity(
                    id = 1L,
                    workoutExerciseId = 1L,
                    setNumber = 1,
                    reps = 10,
                    weight = 100.0,
                    rpe = 8.0,
                    restSeconds = 90,
                    completed = true,
                    setType = 0
                ),
                exerciseId = 100L,
                workoutDate = now.toEpochMilli()
            )
        )

        every { workoutRepository.getCompletedWorkouts() } returns flowOf(workouts)
        every { workoutRepository.getCompletedSetsWithContext() } returns flowOf(sets)

        val viewModel = StreakAndAchievementViewModel(workoutRepository, streakEngine)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is StreakUiState.Success)
        val success = state as StreakUiState.Success
        assertEquals(1, success.report.totalWorkouts)
        assertEquals(1000.0, success.report.totalVolumeKg, 0.01)
        assertEquals(1, success.report.totalSets)
    }

    @Test
    fun `repository exception gracefully emits Empty state`() = runTest(testDispatcher) {
        every { workoutRepository.getCompletedWorkouts() } throws RuntimeException("Database error")

        val viewModel = StreakAndAchievementViewModel(workoutRepository, streakEngine)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is StreakUiState.Empty)
    }
}
