package com.gymcoach.app.presentation.home

import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.data.local.entity.ProgramDayEntity
import com.gymcoach.app.data.local.entity.ProgramEntity
import com.gymcoach.app.domain.repository.AnalyticsRepository
import com.gymcoach.app.domain.repository.ProgramRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        programRepository = mockk(relaxed = true)
        workoutRepository = mockk(relaxed = true)
        volumeCalculator = VolumeCalculator()
        analyticsRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState shows no program when active program is null`() = runTest {
        every { programRepository.getActiveProgram() } returns flowOf(null)
        every { workoutRepository.getCompletedWorkouts() } returns flowOf(emptyList())
        every { workoutRepository.getCompletedSetsByMuscle(any()) } returns flowOf(emptyMap())
        coEvery { analyticsRepository.getAllPersonalRecords() } returns emptyList()

        val viewModel = HomeViewModel(
            programRepository = programRepository,
            workoutRepository = workoutRepository,
            volumeCalculator = volumeCalculator,
            analyticsRepository = analyticsRepository
        )

        val state = viewModel.uiState.first()

        assertTrue(state.hasProgram == false)
        assertTrue(state.vtaperBars.isEmpty())
    }

    @Test
    fun `uiState populates vtaperBars from completed workout sets`() = runTest {
        val program = ProgramEntity(
            id = 1L,
            userId = 1L,
            name = "V-Taper 4-Day Program",
            description = "Test",
            splitType = "upper_lower",
            durationWeeks = 4,
            daysPerWeek = 4,
            difficulty = "INTERMEDIATE",
            goal = "HYPERTROPHY",
            isActive = true
        )
        val day = ProgramDayEntity(id = 10L, programId = 1L, dayNumber = 1, name = "Upper A", targetMuscles = "Back,Chest")

        every { programRepository.getActiveProgram() } returns flowOf(program)
        every { programRepository.getDaysForProgram(1L) } returns flowOf(listOf(day))
        every { programRepository.getExercisesForDays(listOf(10L)) } returns flowOf(mapOf(10L to emptyList()))
        every { workoutRepository.getCompletedWorkouts() } returns flowOf(emptyList())
        every { workoutRepository.getCompletedSetsByMuscle(any()) } returns flowOf(
            mapOf("Back" to 8, "Lateral Deltoid" to 6, "Chest" to 10, "Quadriceps" to 12)
        )
        coEvery { analyticsRepository.getAllPersonalRecords() } returns emptyList()

        val viewModel = HomeViewModel(
            programRepository = programRepository,
            workoutRepository = workoutRepository,
            volumeCalculator = volumeCalculator,
            analyticsRepository = analyticsRepository
        )

        val state = viewModel.uiState.first()

        assertTrue(state.hasProgram)
        assertEquals(4, state.vtaperBars.size)

        val latsBar = state.vtaperBars.find { it.label == "Lats" }
        assertEquals(8, latsBar?.current)

        val lateralDeltsBar = state.vtaperBars.find { it.label == "Lateral Delts" }
        assertEquals(6, lateralDeltsBar?.current)

        val chestBar = state.vtaperBars.find { it.label == "Chest" }
        assertEquals(10, chestBar?.current)

        val legsBar = state.vtaperBars.find { it.label == "Legs" }
        assertEquals(12, legsBar?.current)
    }
}
