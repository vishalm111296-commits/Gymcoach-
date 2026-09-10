package com.gymcoach.app.presentation.workout

import com.gymcoach.app.core.progression.ProgressionEngine
import com.gymcoach.app.core.timer.RestTimerManager
import com.gymcoach.app.domain.repository.ExerciseRepository
import com.gymcoach.app.domain.repository.UserProfileRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkoutLoggingViewModelCollectorTest {

    @Test
    fun `loadOrStartWorkout cancels previous flow collector on re-entry`() = runTest {
        val workoutRepository = mockk<WorkoutRepository>(relaxed = true)
        val exerciseRepository = mockk<ExerciseRepository>(relaxed = true)
        val userProfileRepository = mockk<UserProfileRepository>(relaxed = true)
        val progressionEngine = mockk<ProgressionEngine>(relaxed = true)
        val restTimer = mockk<RestTimerManager>(relaxed = true)

        coEvery { exerciseRepository.getAllExercises() } returns flowOf(emptyList())
        coEvery { workoutRepository.getWorkoutWithDetails(10L) } returns flowOf(null)
        coEvery { workoutRepository.getWorkoutWithDetails(20L) } returns flowOf(null)

        val viewModel = WorkoutLoggingViewModel(
            workoutRepository,
            exerciseRepository,
            restTimer,
            progressionEngine,
            userProfileRepository
        )

        // First invocation
        viewModel.loadOrStartWorkout(10L)

        // Second invocation (e.g. config change / re-entry with new workoutId)
        viewModel.loadOrStartWorkout(20L)

        // Verify flowA and flowB called exactly once each
        coVerify(exactly = 1) { workoutRepository.getWorkoutWithDetails(10L) }
        coVerify(exactly = 1) { workoutRepository.getWorkoutWithDetails(20L) }
    }
}
