package com.gymcoach.app.presentation.body

import com.gymcoach.app.data.local.entity.BodyMeasurementEntity
import com.gymcoach.app.domain.repository.BodyMeasurementRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
class BodyCompositionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: BodyMeasurementRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `empty measurements emit Empty state`() = runTest(testDispatcher) {
        every { repository.getAllMeasurements() } returns flowOf(emptyList())

        val viewModel = BodyCompositionViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is BodyCompositionUiState.Empty)
    }

    @Test
    fun `valid measurements emit Success state with trend`() = runTest(testDispatcher) {
        val measurements = listOf(
            BodyMeasurementEntity(
                id = 1L,
                recordedAt = 1000L,
                weightKg = 80.0,
                bodyFatPct = 15.0,
                waistCm = 82.0,
                shouldersCm = 120.0
            ),
            BodyMeasurementEntity(
                id = 2L,
                recordedAt = 2000L,
                weightKg = 79.5,
                bodyFatPct = 14.8,
                waistCm = 81.5,
                shouldersCm = 121.0
            )
        )
        every { repository.getAllMeasurements() } returns flowOf(measurements)

        val viewModel = BodyCompositionViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is BodyCompositionUiState.Success)
        val success = state as BodyCompositionUiState.Success
        assertEquals(79.5, success.trend.currentWeightKg, 0.01)
    }

    @Test
    fun `saveMeasurement calls repository`() = runTest(testDispatcher) {
        every { repository.getAllMeasurements() } returns flowOf(emptyList())
        coEvery { repository.saveMeasurement(any()) } returns 1L

        val viewModel = BodyCompositionViewModel(repository)
        advanceUntilIdle()

        viewModel.saveMeasurement(
            weightKg = 82.5,
            bodyFatPct = 14.0,
            chestCm = 105.0,
            waistCm = 80.0,
            shouldersCm = 125.0,
            armCm = 38.0,
            thighCm = 60.0,
            calfCm = 39.0,
            notes = "Morning check-in"
        )
        advanceUntilIdle()

        coVerify {
            repository.saveMeasurement(match {
                it.weightKg == 82.5 && it.bodyFatPct == 14.0 && it.notes == "Morning check-in"
            })
        }
    }

    @Test
    fun `deleteMeasurement calls repository delete`() = runTest(testDispatcher) {
        every { repository.getAllMeasurements() } returns flowOf(emptyList())
        coEvery { repository.deleteMeasurement(any()) } returns 1

        val viewModel = BodyCompositionViewModel(repository)
        advanceUntilIdle()

        viewModel.deleteMeasurement(42L)
        advanceUntilIdle()

        coVerify { repository.deleteMeasurement(42L) }
    }

    @Test
    fun `repository getAllMeasurements exception surfaces as Error state`() = runTest(testDispatcher) {
        every { repository.getAllMeasurements() } throws RuntimeException("DB connection lost")

        val viewModel = BodyCompositionViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is BodyCompositionUiState.Error)
        assertTrue((state as BodyCompositionUiState.Error).message.contains("DB connection lost"))
    }

    @Test
    fun `saveMeasurement repository exception surfaces as Error state`() = runTest(testDispatcher) {
        every { repository.getAllMeasurements() } returns flowOf(emptyList())
        coEvery { repository.saveMeasurement(any()) } throws RuntimeException("Disk full")

        val viewModel = BodyCompositionViewModel(repository)
        advanceUntilIdle()

        viewModel.saveMeasurement(
            weightKg = 80.0, bodyFatPct = 15.0, chestCm = 100.0,
            waistCm = 82.0, shouldersCm = 120.0, armCm = 38.0,
            thighCm = 60.0, calfCm = 38.0, notes = "Test"
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is BodyCompositionUiState.Error)
        assertTrue((state as BodyCompositionUiState.Error).message.contains("Disk full"))
    }
}
