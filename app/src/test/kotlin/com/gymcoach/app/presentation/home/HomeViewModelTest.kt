package com.gymcoach.app.presentation.home

import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.data.local.entity.ProgramDayEntity
import com.gymcoach.app.data.local.entity.ProgramEntity
import com.gymcoach.app.data.local.entity.ProgramExerciseEntity
import com.gymcoach.app.domain.repository.AnalyticsRepository
import com.gymcoach.app.domain.repository.ProgramRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var programRepository: ProgramRepository
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var volumeCalculator: VolumeCalculator
    private lateinit var analyticsRepository: AnalyticsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        programRepository = mockk()
        workoutRepository = mockk()
        volumeCalculator = VolumeCalculator()
        analyticsRepository = mockk()

        coEvery { analyticsRepository.getAllPersonalRecords() } returns emptyList()
        coEvery { workoutRepository.getCompletedWorkouts() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `null active program yields empty program state`() = runTest {
        coEvery { programRepository.getActiveProgram() } returns flowOf(null)

        val viewModel = HomeViewModel(
            programRepository,
            workoutRepository,
            volumeCalculator,
            analyticsRepository
        )

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.hasProgram)
        assertTrue(state.coachInsight.contains("ready once you set up your plan"))
    }

    @Test
    fun `active program computes planned weekly set volume for vtaper bars`() = runTest {
        val program = ProgramEntity(id = 1, name = "V-Taper 4-Day", description = "", daysPerWeek = 4)
        val day1 = ProgramDayEntity(id = 10, programId = 1, dayNumber = 1, name = "Upper A", targetMuscles = "Back, Chest, Lateral Deltoid", isRestDay = false)
        val ex1 = ProgramExerciseEntity(id = 100, programDayId = 10, exerciseId = 1, sets = 3, targetReps = "8-12")
        val ex2 = ProgramExerciseEntity(id = 101, programDayId = 10, exerciseId = 2, sets = 3, targetReps = "8-12")

        coEvery { programRepository.getActiveProgram() } returns flowOf(program)
        coEvery { programRepository.getDaysForProgram(1) } returns flowOf(listOf(day1))
        coEvery { programRepository.getExercisesForDays(listOf(10L)) } returns flowOf(mapOf(10L to listOf(ex1, ex2)))

        val viewModel = HomeViewModel(
            programRepository,
            workoutRepository,
            volumeCalculator,
            analyticsRepository
        )

        val state = viewModel.uiState.value
        assertTrue(state.hasProgram)
        val latsBar = state.vtaperBars.first { it.label == "Lats" }
        assertEquals(6, latsBar.current)
    }
}
