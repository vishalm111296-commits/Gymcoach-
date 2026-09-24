package com.gymcoach.app.presentation.analytics

import com.gymcoach.app.core.analytics.VTaperTransformationEngine
import com.gymcoach.app.data.local.dao.BodyMeasurementDao
import com.gymcoach.app.data.local.entity.BodyMeasurementEntity
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VTaperTransformationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var measurementDao: BodyMeasurementDao
    private val transformationEngine = VTaperTransformationEngine()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        measurementDao = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `empty measurements emit Empty state`() = runTest(testDispatcher) {
        every { measurementDao.getAll() } returns flowOf(emptyList())

        val viewModel = VTaperTransformationViewModel(measurementDao, transformationEngine)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is VTaperUiState.Empty)
    }

    @Test
    fun `valid measurements emit Success state with Adonis index`() = runTest(testDispatcher) {
        val measurements = listOf(
            BodyMeasurementEntity(
                id = 1L,
                recordedAt = 1000L,
                waistCm = 80.0,
                shouldersCm = 125.0,
                leftArmCm = 38.0,
                rightArmCm = 38.0
            )
        )
        every { measurementDao.getAll() } returns flowOf(measurements)

        val viewModel = VTaperTransformationViewModel(measurementDao, transformationEngine)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is VTaperUiState.Success)
        val success = state as VTaperUiState.Success
        assertEquals(1.5625, success.report.adonisIndex.currentRatio, 0.001)
    }

    @Test
    fun `flow error emits Error state`() = runTest(testDispatcher) {
        every { measurementDao.getAll() } returns flow {
            throw RuntimeException("Database error")
        }

        val viewModel = VTaperTransformationViewModel(measurementDao, transformationEngine)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is VTaperUiState.Error)
    }

    @Test
    fun `saveMeasurement inserts new measurement when id is 0`() = runTest(testDispatcher) {
        every { measurementDao.getAll() } returns flowOf(emptyList())
        coEvery { measurementDao.insert(any()) } returns 1L

        val viewModel = VTaperTransformationViewModel(measurementDao, transformationEngine)
        advanceUntilIdle()

        val newEntity = BodyMeasurementEntity(id = 0L, waistCm = 78.0, shouldersCm = 128.0)
        viewModel.saveMeasurement(newEntity)
        advanceUntilIdle()

        coVerify { measurementDao.insert(newEntity) }
    }

    @Test
    fun `deleteMeasurement calls dao deleteById`() = runTest(testDispatcher) {
        every { measurementDao.getAll() } returns flowOf(emptyList())
        coEvery { measurementDao.deleteById(any()) } returns 1

        val viewModel = VTaperTransformationViewModel(measurementDao, transformationEngine)
        advanceUntilIdle()

        viewModel.deleteMeasurement(99L)
        advanceUntilIdle()

        coVerify { measurementDao.deleteById(99L) }
    }
}
