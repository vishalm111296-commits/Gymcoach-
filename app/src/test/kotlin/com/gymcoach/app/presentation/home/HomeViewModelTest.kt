package com.gymcoach.app.presentation.home

import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.data.local.entity.ProgramDayEntity
import com.gymcoach.app.data.local.entity.ProgramEntity
import com.gymcoach.app.data.local.entity.ProgramExerciseEntity
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.repository.AnalyticsRepository
import com.gymcoach.app.domain.repository.ExerciseRepository
import com.gymcoach.app.domain.repository.ProgramRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var programRepository: ProgramRepository
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var volumeCalculator: VolumeCalculator
    private lateinit var analyticsRepository: AnalyticsRepository

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        programRepository = mockk()
        exerciseRepository = mockk()
        workoutRepository = mockk()
        volumeCalculator = VolumeCalculator()
        analyticsRepository = mockk()

        coEvery { analyticsRepository.getAllPersonalRecords() } returns emptyList()
        every { workoutRepository.getCompletedWorkouts() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `plannedWeeklySets attributes volume at exercise level without day-level set broadcasting`() = runTest {
        val program = ProgramEntity(id = 1, name = "V-Taper 4-Day", goal = "Hypertrophy", daysPerWeek = 4)
        val day1 = ProgramDayEntity(id = 10, programId = 1, dayNumber = 1, name = "Upper A", targetMuscles = "Back, Chest")

        // 2 Back exercises x 3 sets = 6 sets Back
        // 2 Chest exercises x 3 sets = 6 sets Chest
        val pe1 = ProgramExerciseEntity(id = 101, programDayId = 10, exerciseId = 1, sets = 3)
        val pe2 = ProgramExerciseEntity(id = 102, programDayId = 10, exerciseId = 2, sets = 3)
        val pe3 = ProgramExerciseEntity(id = 103, programDayId = 10, exerciseId = 3, sets = 3)
        val pe4 = ProgramExerciseEntity(id = 104, programDayId = 10, exerciseId = 4, sets = 3)

        val ex1 = Exercise(id = 1, name = "Lat Pulldown", description = "", muscleGroup = "Back", equipment = "cable", difficulty = "Beginner", category = "back", vtaperLat = 8)
        val ex2 = Exercise(id = 2, name = "Barbell Row", description = "", muscleGroup = "Back", equipment = "barbell", difficulty = "Intermediate", category = "back", vtaperLat = 9)
        val ex3 = Exercise(id = 3, name = "Bench Press", description = "", muscleGroup = "Chest", equipment = "barbell", difficulty = "Intermediate", category = "chest", vtaperUpperChest = 0)
        val ex4 = Exercise(id = 4, name = "Incline Press", description = "", muscleGroup = "Chest", equipment = "dumbbell", difficulty = "Intermediate", category = "chest", vtaperUpperChest = 8)

        every { programRepository.getActiveProgram() } returns flowOf(program)
        every { programRepository.getDaysForProgram(1) } returns flowOf(listOf(day1))
        every { programRepository.getExercisesForDays(listOf(10)) } returns flowOf(mapOf(10L to listOf(pe1, pe2, pe3, pe4)))
        every { exerciseRepository.getAllExercises() } returns flowOf(listOf(ex1, ex2, ex3, ex4))

        viewModel = HomeViewModel(
            programRepository,
            exerciseRepository,
            workoutRepository,
            volumeCalculator,
            analyticsRepository
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state)
        val latsBar = state.vtaperBars.find { it.label == "Lats" }
        val chestBar = state.vtaperBars.find { it.label == "Chest" }

        assertNotNull(latsBar)
        assertNotNull(chestBar)

        // Back receives 6 sets, Chest receives 6 sets (Total day sets = 12, so neither receives 12)
        assertEquals(6, latsBar?.current)
        assertEquals(6, chestBar?.current)
    }

    @Test
    fun `legs vtaper bar aggregates quadriceps hamstrings glutes and calves volume correctly`() = runTest {
        val program = ProgramEntity(id = 1, name = "V-Taper 4-Day", goal = "Hypertrophy", daysPerWeek = 4)
        val day1 = ProgramDayEntity(id = 20, programId = 1, dayNumber = 1, name = "Lower A", targetMuscles = "Quadriceps, Hamstrings, Glutes, Calves")

        val pe1 = ProgramExerciseEntity(id = 201, programDayId = 20, exerciseId = 10, sets = 3)
        val pe2 = ProgramExerciseEntity(id = 202, programDayId = 20, exerciseId = 11, sets = 3)
        val pe3 = ProgramExerciseEntity(id = 203, programDayId = 20, exerciseId = 12, sets = 3)
        val pe4 = ProgramExerciseEntity(id = 204, programDayId = 20, exerciseId = 13, sets = 3)

        val ex10 = Exercise(id = 10, name = "Squat", description = "", muscleGroup = "Quadriceps", equipment = "barbell", difficulty = "Intermediate", category = "legs")
        val ex11 = Exercise(id = 11, name = "Leg Curl", description = "", muscleGroup = "Hamstrings", equipment = "machine", difficulty = "Beginner", category = "legs")
        val ex12 = Exercise(id = 12, name = "Hip Thrust", description = "", muscleGroup = "Glutes", equipment = "barbell", difficulty = "Intermediate", category = "legs")
        val ex13 = Exercise(id = 13, name = "Calf Raise", description = "", muscleGroup = "Calves", equipment = "machine", difficulty = "Beginner", category = "legs")

        every { programRepository.getActiveProgram() } returns flowOf(program)
        every { programRepository.getDaysForProgram(1) } returns flowOf(listOf(day1))
        every { programRepository.getExercisesForDays(listOf(20)) } returns flowOf(mapOf(20L to listOf(pe1, pe2, pe3, pe4)))
        every { exerciseRepository.getAllExercises() } returns flowOf(listOf(ex10, ex11, ex12, ex13))

        viewModel = HomeViewModel(
            programRepository,
            exerciseRepository,
            workoutRepository,
            volumeCalculator,
            analyticsRepository
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        val legsBar = state.vtaperBars.find { it.label == "Legs" }

        assertNotNull(legsBar)
        // Each leg exercise contributes 3 sets = 12 sets total for Legs
        assertEquals(12, legsBar?.current)
    }
}
