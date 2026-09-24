package com.gymcoach.app.presentation.analytics

import com.gymcoach.app.core.analytics.MuscleBalanceAnalyzer
import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.repository.ExerciseRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class MuscleBalanceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var exerciseRepository: ExerciseRepository
    private val muscleBalanceAnalyzer = MuscleBalanceAnalyzer()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        workoutRepository = mockk(relaxed = true)
        exerciseRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `no completed sets emits Empty state`() = runTest(testDispatcher) {
        every { workoutRepository.getCompletedSetsWithContext() } returns flowOf(emptyList())

        val viewModel = MuscleBalanceViewModel(workoutRepository, exerciseRepository, muscleBalanceAnalyzer)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is MuscleBalanceUiState.Empty)
    }

    @Test
    fun `valid sets and exercises emit Success state with report`() = runTest(testDispatcher) {
        val now = System.currentTimeMillis()
        val sets = listOf(
            VolumeCalculator.SetWithContext(
                set = WorkoutSetEntity(
                    id = 1L,
                    workoutExerciseId = 1L,
                    setNumber = 1,
                    reps = 10,
                    weight = 80.0,
                    rpe = 8.0,
                    restSeconds = 90,
                    completed = true,
                    setType = 0
                ),
                exerciseId = 100L,
                workoutDate = now
            ),
            VolumeCalculator.SetWithContext(
                set = WorkoutSetEntity(
                    id = 2L,
                    workoutExerciseId = 2L,
                    setNumber = 1,
                    reps = 10,
                    weight = 75.0,
                    rpe = 8.0,
                    restSeconds = 90,
                    completed = true,
                    setType = 0
                ),
                exerciseId = 101L,
                workoutDate = now
            )
        )
        val exercises = listOf(
            Exercise(
                id = 100L,
                name = "Bench Press",
                description = "Chest press",
                muscleGroup = "Chest",
                equipment = "Barbell",
                difficulty = "Intermediate",
                secondaryMuscles = "Triceps, Shoulders"
            ),
            Exercise(
                id = 101L,
                name = "Barbell Row",
                description = "Back row",
                muscleGroup = "Back",
                equipment = "Barbell",
                difficulty = "Intermediate",
                secondaryMuscles = "Biceps, Rear Delts"
            )
        )

        every { workoutRepository.getCompletedSetsWithContext() } returns flowOf(sets)
        every { exerciseRepository.getAllExercises() } returns flowOf(exercises)

        val viewModel = MuscleBalanceViewModel(workoutRepository, exerciseRepository, muscleBalanceAnalyzer)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is MuscleBalanceUiState.Success)
        val success = state as MuscleBalanceUiState.Success
        assertTrue(success.report.overallBalanceScore > 0)
        assertEquals(800.0, success.report.pushPullRatio.agonistVolumeKg, 0.01)
        assertEquals(750.0, success.report.pushPullRatio.antagonistVolumeKg, 0.01)
    }
}
