package com.gymcoach.app.presentation.home

import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.data.local.entity.ProgramDayEntity
import com.gymcoach.app.data.local.entity.ProgramEntity
import com.gymcoach.app.data.local.entity.ProgramExerciseEntity
import com.gymcoach.app.domain.repository.AnalyticsRepository
import com.gymcoach.app.domain.repository.ProgramRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import io.mockk.coEvery
import io.mockk.every
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var programRepository: ProgramRepository
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var volumeCalculator: VolumeCalculator
    private lateinit var analyticsRepository: AnalyticsRepository

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        programRepository = mockk(relaxed = true)
        workoutRepository = mockk(relaxed = true)
        volumeCalculator = VolumeCalculator()
        analyticsRepository = mockk(relaxed = true)

        coEvery { analyticsRepository.getAllPersonalRecords() } returns emptyList()
        every { workoutRepository.getCompletedWorkouts() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when no active program exists UI state reflects no program`() = runTest {
        every { programRepository.getActiveProgram() } returns flowOf(null)

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
    fun `when active program exists UI state loads planned weekly volume bars`() = runTest {
        val program = ProgramEntity(id = 1L, name = "V-Taper Hypertrophy", daysPerWeek = 4)
        val day1 = ProgramDayEntity(id = 10L, programId = 1L, dayNumber = 1, name = "Upper A", targetMuscles = "Back, Lateral Deltoid")
        val day2 = ProgramDayEntity(id = 11L, programId = 1L, dayNumber = 2, name = "Lower A", targetMuscles = "Quadriceps, Hamstrings")

        val day1Exercise = ProgramExerciseEntity(id = 100L, programDayId = 10L, exerciseId = 1L, sets = 4)
        val day2Exercise = ProgramExerciseEntity(id = 101L, programDayId = 11L, exerciseId = 2L, sets = 3)

        every { programRepository.getActiveProgram() } returns flowOf(program)
        every { programRepository.getDaysForProgram(1L) } returns flowOf(listOf(day1, day2))
        every { programRepository.getExercisesForDays(listOf(10L, 11L)) } returns flowOf(
            mapOf(10L to listOf(day1Exercise), 11L to listOf(day2Exercise))
        )

        val viewModel = HomeViewModel(
            programRepository,
            workoutRepository,
            volumeCalculator,
            analyticsRepository
        )

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.hasProgram)
        assertEquals(4, state.targetWorkouts)
        assertNotNull(state.todayWorkout)

        // Verify planned weekly volume bars calculation
        val latsBar = state.vtaperBars.firstOrNull { it.label == "Lats" }
        assertNotNull(latsBar)
        assertEquals(4, latsBar?.current)

        val deltsBar = state.vtaperBars.firstOrNull { it.label == "Lateral Delts" }
        assertNotNull(deltsBar)
        assertEquals(4, deltsBar?.current)

        val legsBar = state.vtaperBars.firstOrNull { it.label == "Legs" }
        assertNotNull(legsBar)
        assertEquals(6, legsBar?.current) // 3 sets for Quads + 3 sets for Hamstrings
    }
}
