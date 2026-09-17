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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
}
