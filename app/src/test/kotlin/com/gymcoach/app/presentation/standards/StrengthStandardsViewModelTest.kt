package com.gymcoach.app.presentation.standards

import com.gymcoach.app.core.standards.StrengthStandardsEngine
import com.gymcoach.app.core.standards.StrengthTier
import com.gymcoach.app.data.local.dao.BodyMeasurementDao
import com.gymcoach.app.data.local.entity.BodyMeasurementEntity
import com.gymcoach.app.domain.repository.AnalyticsRepository
import com.gymcoach.app.domain.repository.PersonalRecord
import io.mockk.coEvery
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
class StrengthStandardsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var analyticsRepository: AnalyticsRepository
    private lateinit var bodyMeasurementDao: BodyMeasurementDao
    private val strengthStandardsEngine = StrengthStandardsEngine()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        analyticsRepository = mockk(relaxed = true)
        bodyMeasurementDao = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `no bodyweight emits Empty state`() = runTest(testDispatcher) {
        coEvery { analyticsRepository.getAllPersonalRecords() } returns emptyList()
        every { bodyMeasurementDao.getLatest() } returns flowOf(null)

        val viewModel = StrengthStandardsViewModel(
            analyticsRepository,
            bodyMeasurementDao,
            strengthStandardsEngine
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is StrengthStandardsUiState.Empty)
    }

    @Test
    fun `valid bodyweight and PRs emit Success state`() = runTest(testDispatcher) {
        val prs = listOf(
            PersonalRecord("Bench Press", 100.0),
            PersonalRecord("Squat", 140.0),
            PersonalRecord("Deadlift", 180.0),
            PersonalRecord("Overhead Press", 65.0)
        )
        coEvery { analyticsRepository.getAllPersonalRecords() } returns prs
        every { bodyMeasurementDao.getLatest() } returns flowOf(
            BodyMeasurementEntity(id = 1L, weightKg = 80.0)
        )

        val viewModel = StrengthStandardsViewModel(
            analyticsRepository,
            bodyMeasurementDao,
            strengthStandardsEngine
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is StrengthStandardsUiState.Success)
        val success = state as StrengthStandardsUiState.Success
        assertEquals(80.0, success.profile.bodyweightKg, 0.01)
        assertEquals(4, success.allPrs.size)
        assertTrue(success.profile.overallTier != StrengthTier.UNTRAINED)
    }

    @Test
    fun `database exception gracefully falls back to Empty state`() = runTest(testDispatcher) {
        coEvery { analyticsRepository.getAllPersonalRecords() } throws RuntimeException("DB error")
        every { bodyMeasurementDao.getLatest() } returns flowOf(null)

        val viewModel = StrengthStandardsViewModel(
            analyticsRepository,
            bodyMeasurementDao,
            strengthStandardsEngine
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is StrengthStandardsUiState.Empty)
    }
}
