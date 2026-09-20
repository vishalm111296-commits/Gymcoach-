package com.gymcoach.app.presentation.nutrition

import com.gymcoach.app.data.local.entity.NutritionLogEntity
import com.gymcoach.app.domain.repository.NutritionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class NutritionViewModelTest {

    private val nutritionRepository = mockk<NutritionRepository>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadDay calculates total calories and macros accurately`() = runTest(testDispatcher) {
        val sampleLogs = listOf(
            NutritionLogEntity(
                id = 1L,
                mealName = "Breakfast",
                calories = 500,
                proteinGrams = 40f,
                carbsGrams = 50f,
                fatGrams = 15f
            ),
            NutritionLogEntity(
                id = 2L,
                mealName = "Lunch",
                calories = 700,
                proteinGrams = 50f,
                carbsGrams = 80f,
                fatGrams = 20f
            )
        )

        every { nutritionRepository.getLogsForDay(any(), any()) } returns flowOf(sampleLogs)

        val viewModel = NutritionViewModel(nutritionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.todayLogs.size)
        assertEquals(1200, state.dailySummary.totalCalories)
        assertEquals(90f, state.dailySummary.totalProtein, 0.01f)
        assertEquals(130f, state.dailySummary.totalCarbs, 0.01f)
        assertEquals(35f, state.dailySummary.totalFat, 0.01f)
    }

    @Test
    fun `showAddDialog and hideDialog toggle state`() = runTest(testDispatcher) {
        every { nutritionRepository.getLogsForDay(any(), any()) } returns flowOf(emptyList())

        val viewModel = NutritionViewModel(nutritionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showAddDialog)

        viewModel.showAddDialog()
        assertTrue(viewModel.uiState.value.showAddDialog)

        viewModel.hideDialog()
        assertFalse(viewModel.uiState.value.showAddDialog)
    }

    @Test
    fun `saveLog calls addLog for new log`() = runTest(testDispatcher) {
        every { nutritionRepository.getLogsForDay(any(), any()) } returns flowOf(emptyList())

        val viewModel = NutritionViewModel(nutritionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveLog("Dinner", 600, 45f, 60f, 18f)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { nutritionRepository.addLog(match { it.mealName == "Dinner" && it.calories == 600 }) }
    }
}
