package com.gymcoach.app.presentation.home

import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
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
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var volumeCalculator: VolumeCalculator
    private lateinit var analyticsRepository: AnalyticsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        programRepository = mockk()
        workoutRepository = mockk()
        exerciseDao = mockk()
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
    fun `planned weekly sets attributes volume per exercise without day-level broadcasting`() = runTest {
        val program = ProgramEntity(id = 1L, name = "V-Taper", goal = "Hypertrophy", daysPerWeek = 4)
        val day1 = ProgramDayEntity(id = 10L, programId = 1L, dayNumber = 1, name = "Upper A", targetMuscles = "Back, Chest, Lateral Deltoid")

        val latExercise = ExerciseEntity(id = 101L, name = "Lat Pulldown", description = "", muscleGroup = "Back", equipment = "cable", difficulty = "Beginner", vtaperLat = 9)
        val chestExercise = ExerciseEntity(id = 102L, name = "Bench Press", description = "", muscleGroup = "Chest", equipment = "barbell", difficulty = "Intermediate", vtaperUpperChest = 8)

        val progEx1 = ProgramExerciseEntity(id = 1L, programDayId = 10L, exerciseId = 101L, sets = 3)
        val progEx2 = ProgramExerciseEntity(id = 2L, programDayId = 10L, exerciseId = 102L, sets = 3)

        coEvery { exerciseDao.getAll() } returns flowOf(listOf(latExercise, chestExercise))
        coEvery { programRepository.getActiveProgram() } returns flowOf(program)
        coEvery { programRepository.getDaysForProgram(1L) } returns flowOf(listOf(day1))
        coEvery { programRepository.getExercisesForDays(listOf(10L)) } returns flowOf(mapOf(10L to listOf(progEx1, progEx2)))

        val viewModel = HomeViewModel(programRepository, workoutRepository, exerciseDao, volumeCalculator, analyticsRepository)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state)

        val latsBar = state.vtaperBars.find { it.label == "Lats" }
        val chestBar = state.vtaperBars.find { it.label == "Chest" }

        assertEquals("Lats volume must be 3 sets (not 6 from day broadcasting)", 3, latsBar?.current)
        assertEquals("Chest volume must be 3 sets (not 6 from day broadcasting)", 3, chestBar?.current)
    }

    @Test
    fun `legs metric aggregates lower body muscle slots accurately`() = runTest {
        val program = ProgramEntity(id = 2L, name = "Lower Test", goal = "Hypertrophy", daysPerWeek = 2)
        val dayLower = ProgramDayEntity(id = 20L, programId = 2L, dayNumber = 1, name = "Lower A", targetMuscles = "Quadriceps, Hamstrings, Glutes, Calves")

        val squat = ExerciseEntity(id = 201L, name = "Barbell Squat", description = "", muscleGroup = "Legs", equipment = "barbell", difficulty = "Intermediate")
        val rdl = ExerciseEntity(id = 202L, name = "Dumbbell RDL", description = "", muscleGroup = "Legs", equipment = "dumbbell", difficulty = "Intermediate")

        val progEx1 = ProgramExerciseEntity(id = 20L, programDayId = 20L, exerciseId = 201L, sets = 3)
        val progEx2 = ProgramExerciseEntity(id = 21L, programDayId = 20L, exerciseId = 202L, sets = 3)

        coEvery { exerciseDao.getAll() } returns flowOf(listOf(squat, rdl))
        coEvery { programRepository.getActiveProgram() } returns flowOf(program)
        coEvery { programRepository.getDaysForProgram(2L) } returns flowOf(listOf(dayLower))
        coEvery { programRepository.getExercisesForDays(listOf(20L)) } returns flowOf(mapOf(20L to listOf(progEx1, progEx2)))

        val viewModel = HomeViewModel(programRepository, workoutRepository, exerciseDao, volumeCalculator, analyticsRepository)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        val legsBar = state.vtaperBars.find { it.label == "Legs" }

        assertEquals("Legs volume must be 6 sets total for the day", 6, legsBar?.current)
    }
}
