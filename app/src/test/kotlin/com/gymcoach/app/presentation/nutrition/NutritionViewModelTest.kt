package com.gymcoach.app.presentation.nutrition

import com.gymcoach.app.core.nutrition.ActivityLevel
import com.gymcoach.app.core.nutrition.NutritionGoal
import com.gymcoach.app.data.local.entity.NutritionLogEntity
import com.gymcoach.app.data.local.entity.UserProfileEntity
import com.gymcoach.app.domain.repository.NutritionRepository
import com.gymcoach.app.domain.repository.UserProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class NutritionViewModelTest {

    private val nutritionRepository = mockk<NutritionRepository>(relaxed = true)
    private val userProfileRepository = mockk<UserProfileRepository>(relaxed = true)
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
        assertNotNull(state.dailySummary.tdeeProfile)
    }

    @Test
    fun `userProfile emission dynamically recalculates TDEE and targets`() = runTest(testDispatcher) {
        val profileFlow = MutableSharedFlow<UserProfileEntity?>(replay = 1)
        every { userProfileRepository.getLatestProfile() } returns profileFlow
        every { nutritionRepository.getLogsForDay(any(), any()) } returns flowOf(emptyList())

        val viewModel = NutritionViewModel(nutritionRepository, userProfileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Emit profile: 85kg male, 180cm, 25 years old, Bulk goal, 5 days training
        val testProfile = UserProfileEntity(
            id = 1L,
            weightKg = 85.0,
            heightCm = 180.0,
            age = 25,
            sex = "male",
            goal = "muscle gain",
            trainingDaysPerWeek = 5,
            sessionLengthMinutes = 75
        )
        profileFlow.emit(testProfile)
        testDispatcher.scheduler.advanceUntilIdle()

        val summary = viewModel.uiState.value.dailySummary
        assertNotNull(summary.tdeeProfile)
        assertEquals(NutritionGoal.LEAN_BULK, summary.tdeeProfile!!.goal)
        assertTrue(summary.calorieGoal > summary.tdeeProfile!!.tdee)
        assertEquals(summary.tdeeProfile!!.targetCalories, summary.calorieGoal)
        assertEquals(summary.tdeeProfile!!.macroSplit.proteinGrams, summary.proteinGoalGrams, 0.01f)
    }

    @Test
    fun `selectGoalOverride and selectActivityOverride update target goals`() = runTest(testDispatcher) {
        every { nutritionRepository.getLogsForDay(any(), any()) } returns flowOf(emptyList())

        val viewModel = NutritionViewModel(nutritionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectGoalOverride(NutritionGoal.AGGRESSIVE_CUT)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(NutritionGoal.AGGRESSIVE_CUT, viewModel.uiState.value.selectedGoalOverride)
        val cutSummary = viewModel.uiState.value.dailySummary
        assertTrue(cutSummary.calorieGoal < cutSummary.tdeeProfile!!.tdee)

        viewModel.selectActivityOverride(ActivityLevel.VERY_ACTIVE)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ActivityLevel.VERY_ACTIVE, viewModel.uiState.value.selectedActivityOverride)
        assertEquals(ActivityLevel.VERY_ACTIVE, viewModel.uiState.value.dailySummary.tdeeProfile!!.activityLevel)
    }

    @Test
    fun `showTdeeSheet and hideTdeeSheet toggle bottom sheet state`() = runTest(testDispatcher) {
        every { nutritionRepository.getLogsForDay(any(), any()) } returns flowOf(emptyList())

        val viewModel = NutritionViewModel(nutritionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showTdeeCalculatorSheet)

        viewModel.showTdeeSheet()
        assertTrue(viewModel.uiState.value.showTdeeCalculatorSheet)

        viewModel.hideTdeeSheet()
        assertFalse(viewModel.uiState.value.showTdeeCalculatorSheet)
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

    @Test
    fun `logWater calls addLog with water amount`() = runTest(testDispatcher) {
        every { nutritionRepository.getLogsForDay(any(), any()) } returns flowOf(emptyList())

        val viewModel = NutritionViewModel(nutritionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.logWater(500)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { nutritionRepository.addLog(match { it.mealName == "Water" && it.waterMl == 500 }) }
    }

    @Test
    fun `deleteLog calls deleteLog on repository`() = runTest(testDispatcher) {
        every { nutritionRepository.getLogsForDay(any(), any()) } returns flowOf(emptyList())
        val log = NutritionLogEntity(id = 10L, mealName = "Snack", calories = 200, proteinGrams = 10f, carbsGrams = 20f, fatGrams = 5f)

        val viewModel = NutritionViewModel(nutritionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteLog(log)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { nutritionRepository.deleteLog(log) }
    }
}
