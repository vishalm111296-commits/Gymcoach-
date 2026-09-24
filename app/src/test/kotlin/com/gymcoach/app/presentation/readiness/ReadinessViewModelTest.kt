package com.gymcoach.app.presentation.readiness

import com.gymcoach.app.data.local.entity.ReadinessEntity
import com.gymcoach.app.domain.repository.ReadinessRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReadinessViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var readinessRepository: ReadinessRepository

    private val sampleEntity = ReadinessEntity(
        id = 1L,
        recordedAt = 1000L,
        sleepQuality = 4,
        soreness = 3,
        energy = 4,
        motivation = 5,
        notes = "Feeling great"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        readinessRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads latest and recent readiness correctly`() = runTest(testDispatcher) {
        every { readinessRepository.getLatestReadiness() } returns flowOf(sampleEntity)
        every { readinessRepository.getRecentReadiness(any()) } returns flowOf(listOf(sampleEntity))

        val viewModel = ReadinessViewModel(readinessRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(sampleEntity, state.latestReadiness)
        assertEquals(1, state.recentReadiness.size)
    }

    @Test
    fun `load handles exception gracefully and populates errorMessage`() = runTest(testDispatcher) {
        every { readinessRepository.getLatestReadiness() } returns flow {
            throw RuntimeException("Database read failure")
        }

        val viewModel = ReadinessViewModel(readinessRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertEquals("Database read failure", state.errorMessage)
    }

    @Test
    fun `form setters update form state with range coercion`() = runTest(testDispatcher) {
        every { readinessRepository.getLatestReadiness() } returns flowOf(null)
        every { readinessRepository.getRecentReadiness(any()) } returns flowOf(emptyList())

        val viewModel = ReadinessViewModel(readinessRepository)
        advanceUntilIdle()

        viewModel.setSleepQuality(5)
        viewModel.setSoreness(1)
        viewModel.setEnergy(10) // should coerce to 5
        viewModel.setMotivation(-2) // should coerce to 1
        viewModel.setNotes("Leg day recovery")

        val state = viewModel.uiState.value
        assertEquals(5, state.sleepQuality)
        assertEquals(1, state.soreness)
        assertEquals(5, state.energy)
        assertEquals(1, state.motivation)
        assertEquals("Leg day recovery", state.notes)
    }

    @Test
    fun `saveReadiness saves entity to repository and refreshes`() = runTest(testDispatcher) {
        every { readinessRepository.getLatestReadiness() } returns flowOf(sampleEntity)
        every { readinessRepository.getRecentReadiness(any()) } returns flowOf(listOf(sampleEntity))
        coEvery { readinessRepository.saveReadiness(any()) } returns 1L

        val viewModel = ReadinessViewModel(readinessRepository)
        advanceUntilIdle()

        viewModel.showLogDialog()
        assertTrue(viewModel.uiState.value.showDialog)

        viewModel.setSleepQuality(5)
        viewModel.setNotes("New note")
        viewModel.saveReadiness()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showDialog)
        coVerify(atLeast = 1) {
            readinessRepository.saveReadiness(
                match { it.sleepQuality == 5 && it.notes == "New note" }
            )
        }
    }
}
