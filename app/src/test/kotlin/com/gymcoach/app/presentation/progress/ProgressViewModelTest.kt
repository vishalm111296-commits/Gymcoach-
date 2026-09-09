package com.gymcoach.app.presentation.progress

import com.gymcoach.app.data.local.dao.BodyMeasurementDao
import com.gymcoach.app.data.local.entity.BodyMeasurementEntity
import com.gymcoach.app.domain.repository.AnalyticsRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var analyticsRepository: AnalyticsRepository
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var bodyMeasurementDao: BodyMeasurementDao
    private lateinit var viewModel: ProgressViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        analyticsRepository = mockk(relaxed = true)
        workoutRepository = mockk(relaxed = true)
        bodyMeasurementDao = mockk(relaxed = true)

        every { workoutRepository.getCompletedWorkouts() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shoulders 120 waist 80 calculates ratio 1_5`() = runTest {
        val measurement = BodyMeasurementEntity(
            recordedAt = System.currentTimeMillis(),
            weightKg = 75.0,
            waistCm = 80.0,
            shouldersCm = 120.0,
            chestCm = 100.0
        )
        every { bodyMeasurementDao.getAll() } returns flowOf(listOf(measurement))

        viewModel = ProgressViewModel(analyticsRepository, workoutRepository, bodyMeasurementDao)

        val state = viewModel.uiState.value
        assertEquals(120.0, state.latestShoulders!!, 0.01)
        assertEquals(80.0, state.latestWaist!!, 0.01)
        assertEquals(1.5, state.latestShoulderToWaistRatio!!, 0.01) // 120 / 80 = 1.5
        assertEquals(1, state.shoulderToWaistTrend.size)
    }

    @Test
    fun `shoulders 0 waist 80 returns null ratio`() = runTest {
        val measurement = BodyMeasurementEntity(
            recordedAt = System.currentTimeMillis(),
            weightKg = 75.0,
            waistCm = 80.0,
            shouldersCm = 0.0,
            chestCm = 100.0
        )
        every { bodyMeasurementDao.getAll() } returns flowOf(listOf(measurement))

        viewModel = ProgressViewModel(analyticsRepository, workoutRepository, bodyMeasurementDao)

        val state = viewModel.uiState.value
        assertNull("Shoulder-to-waist ratio must be null when shoulders are 0", state.latestShoulderToWaistRatio)
        assertTrue("Shoulder-to-waist trend must be empty", state.shoulderToWaistTrend.isEmpty())
    }

    @Test
    fun `shoulders missing chest 100 waist 80 returns null shoulder-to-waist ratio`() = runTest {
        val measurement = BodyMeasurementEntity(
            recordedAt = System.currentTimeMillis(),
            weightKg = 75.0,
            waistCm = 80.0,
            shouldersCm = 0.0, // Missing shoulder
            chestCm = 100.0 // Chest present
        )
        every { bodyMeasurementDao.getAll() } returns flowOf(listOf(measurement))

        viewModel = ProgressViewModel(analyticsRepository, workoutRepository, bodyMeasurementDao)

        val state = viewModel.uiState.value
        assertNull("Shoulder-to-waist ratio must NOT fall back to chest", state.latestShoulderToWaistRatio)
        assertEquals(1.25, state.latestChestToWaistRatio!!, 0.01) // Chest ratio evaluated separately as 100 / 80 = 1.25
    }

    @Test
    fun `shoulders 120 waist 0 returns null ratio`() = runTest {
        val measurement = BodyMeasurementEntity(
            recordedAt = System.currentTimeMillis(),
            weightKg = 75.0,
            waistCm = 0.0,
            shouldersCm = 120.0
        )
        every { bodyMeasurementDao.getAll() } returns flowOf(listOf(measurement))

        viewModel = ProgressViewModel(analyticsRepository, workoutRepository, bodyMeasurementDao)

        val state = viewModel.uiState.value
        assertNull(state.latestShoulderToWaistRatio)
        assertTrue(state.shoulderToWaistTrend.isEmpty())
    }

    @Test
    fun `shoulders missing waist missing returns null ratio`() = runTest {
        val measurement = BodyMeasurementEntity(
            recordedAt = System.currentTimeMillis(),
            weightKg = 75.0,
            waistCm = 0.0,
            shouldersCm = 0.0
        )
        every { bodyMeasurementDao.getAll() } returns flowOf(listOf(measurement))

        viewModel = ProgressViewModel(analyticsRepository, workoutRepository, bodyMeasurementDao)

        val state = viewModel.uiState.value
        assertNull(state.latestShoulderToWaistRatio)
        assertTrue(state.shoulderToWaistTrend.isEmpty())
    }

    @Test
    fun `chronological trend tracks ratio change over time`() = runTest {
        val now = System.currentTimeMillis()
        val m1 = BodyMeasurementEntity(recordedAt = now - 86400000, waistCm = 80.0, shouldersCm = 116.0) // 1.45
        val m2 = BodyMeasurementEntity(recordedAt = now, waistCm = 80.0, shouldersCm = 120.0) // 1.50
        every { bodyMeasurementDao.getAll() } returns flowOf(listOf(m2, m1))

        viewModel = ProgressViewModel(analyticsRepository, workoutRepository, bodyMeasurementDao)

        val state = viewModel.uiState.value
        assertEquals(2, state.shoulderToWaistTrend.size)
        assertEquals(1.45, state.shoulderToWaistTrend[0].value, 0.01)
        assertEquals(1.50, state.shoulderToWaistTrend[1].value, 0.01)
        assertEquals(0.05, state.shoulderToWaistChange!!, 0.01)
    }
}
