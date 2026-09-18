package com.gymcoach.app.presentation.program

import com.gymcoach.app.core.program.ProgramGenerator
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.entity.ProgramEntity
import com.gymcoach.app.domain.repository.ProgramRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProgramDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var programRepository: ProgramRepository
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var programGenerator: ProgramGenerator
    private lateinit var viewModel: ProgramDetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        programRepository = mockk(relaxed = true)
        exerciseDao = mockk(relaxed = true)
        workoutRepository = mockk(relaxed = true)
        programGenerator = mockk(relaxed = true)

        every { programRepository.getActiveProgram() } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `generateAndActivateProgram generates saves and reloads active program`() = runTest {
        val dummyGenerated = ProgramGenerator.GeneratedProgram(
            name = "Hypertrophy Program",
            description = "4 day split",
            goal = "Hypertrophy",
            frequency = 4,
            days = emptyList()
        )

        coEvery {
            programGenerator.generateProgram(
                frequency = 4,
                equipmentType = "gym",
                goal = "Hypertrophy"
            )
        } returns dummyGenerated

        coEvery { programRepository.saveGeneratedProgram(dummyGenerated) } returns 1L

        val activeProgram = ProgramEntity(id = 1L, name = "Hypertrophy Program", goal = "Hypertrophy")
        every { programRepository.getActiveProgram() } returns flowOf(activeProgram)
        every { programRepository.getDaysForProgram(1L) } returns flowOf(emptyList())

        viewModel = ProgramDetailViewModel(
            programRepository = programRepository,
            exerciseDao = exerciseDao,
            workoutRepository = workoutRepository,
            programGenerator = programGenerator
        )

        viewModel.generateAndActivateProgram(frequency = 4, equipmentType = "gym", goal = "Hypertrophy")

        coVerify(exactly = 1) {
            programGenerator.generateProgram(4, "gym", "Hypertrophy")
            programRepository.saveGeneratedProgram(dummyGenerated)
        }

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.program)
        assertEquals("Hypertrophy Program", state.program?.name)
    }

    @Test
    fun `createCustomRoutine saves custom routine and reloads active program`() = runTest {
        val days = listOf(
            com.gymcoach.app.domain.repository.CustomRoutineDay(
                dayNumber = 1,
                name = "Day 1 - Push",
                targetMuscles = "Chest, Triceps",
                exercises = listOf(
                    com.gymcoach.app.domain.repository.CustomRoutineExercise(
                        exerciseId = 10L,
                        targetSets = 4,
                        targetReps = "6-10",
                        restSeconds = 120
                    )
                )
            )
        )

        coEvery {
            programRepository.saveCustomRoutine(
                name = "My Custom Split",
                description = "Custom Desc",
                goal = "Strength",
                days = days,
                setAsActive = true
            )
        } returns 2L

        val activeProgram = ProgramEntity(id = 2L, name = "My Custom Split", goal = "Strength")
        every { programRepository.getActiveProgram() } returns flowOf(activeProgram)
        every { programRepository.getDaysForProgram(2L) } returns flowOf(emptyList())

        viewModel = ProgramDetailViewModel(
            programRepository = programRepository,
            exerciseDao = exerciseDao,
            workoutRepository = workoutRepository,
            programGenerator = programGenerator
        )

        viewModel.createCustomRoutine(
            name = "My Custom Split",
            description = "Custom Desc",
            goal = "Strength",
            days = days
        )

        coVerify(exactly = 1) {
            programRepository.saveCustomRoutine(
                name = "My Custom Split",
                description = "Custom Desc",
                goal = "Strength",
                days = days,
                setAsActive = true
            )
        }

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.program)
        assertEquals("My Custom Split", state.program?.name)
    }

    @Test
    fun `availableExercises exposes exercises flow from exerciseDao`() = runTest {
        val mockExercises = listOf(
            com.gymcoach.app.data.local.entity.ExerciseEntity(
                id = 1L,
                name = "Bench Press",
                description = "Chest press",
                muscleGroup = "Chest",
                equipment = "Barbell",
                difficulty = "Intermediate"
            )
        )
        every { exerciseDao.getAll() } returns flowOf(mockExercises)

        viewModel = ProgramDetailViewModel(
            programRepository = programRepository,
            exerciseDao = exerciseDao,
            workoutRepository = workoutRepository,
            programGenerator = programGenerator
        )

        val collected = mutableListOf<List<com.gymcoach.app.data.local.entity.ExerciseEntity>>()
        val collectJob = backgroundScope.launch {
            viewModel.availableExercises.collect { collected.add(it) }
        }
        advanceUntilIdle()
        assertTrue("Expected at least one emission", collected.isNotEmpty())
        val exercises = collected.last()
        assertEquals(1, exercises.size)
        assertEquals("Bench Press", exercises.first().name)
        collectJob.cancel()
    }
}
