package com.gymcoach.app.presentation.onboarding

import android.content.Context
import android.content.SharedPreferences
import com.gymcoach.app.core.program.ProgramGenerator
import com.gymcoach.app.domain.repository.ProgramRepository
import com.gymcoach.app.domain.repository.UserProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val context = mockk<Context>(relaxed = true)
    private val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val userProfileRepository = mockk<UserProfileRepository>(relaxed = true)
    private val programGenerator = mockk<ProgramGenerator>(relaxed = true)
    private val programRepository = mockk<ProgramRepository>(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { context.getSharedPreferences("gymcoach_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): OnboardingViewModel {
        return OnboardingViewModel(
            context = context,
            userProfileRepository = userProfileRepository,
            programGenerator = programGenerator,
            programRepository = programRepository
        )
    }

    @Test
    fun `initial state starts at WELCOME step with defaults`() {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertEquals(OnboardingStep.WELCOME, state.step)
        assertNull(state.goal)
        assertNull(state.experience)
        assertEquals("Male", state.sex)
        assertEquals(25f, state.age)
        assertEquals(175f, state.heightCm)
        assertEquals(75f, state.weightKg)
        assertEquals(4, state.daysPerWeek)
        assertEquals(60, state.sessionMinutes)
        assertTrue(state.isStepValid)
        assertFalse(state.isLastContentStep)
    }

    @Test
    fun `step progression with next and back moves through flow`() {
        val viewModel = createViewModel()

        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.step)

        viewModel.next()
        assertEquals(OnboardingStep.GOAL, viewModel.uiState.value.step)

        viewModel.next()
        assertEquals(OnboardingStep.EXPERIENCE, viewModel.uiState.value.step)

        viewModel.back()
        assertEquals(OnboardingStep.GOAL, viewModel.uiState.value.step)
    }

    @Test
    fun `field update methods mutate state correctly`() {
        val viewModel = createViewModel()

        viewModel.selectGoal("Muscle Gain")
        viewModel.selectExperience("Intermediate")
        viewModel.setAge(28f)
        viewModel.setHeight(182f)
        viewModel.setWeight(82f)
        viewModel.setSex("Female")
        viewModel.setDaysPerWeek(5)
        viewModel.setSessionMinutes(75)
        viewModel.setPreferredSchedule("Evening")
        viewModel.setLimitationsPreferences("Time")

        val state = viewModel.uiState.value
        assertEquals("Muscle Gain", state.goal)
        assertEquals("Intermediate", state.experience)
        assertEquals(28f, state.age)
        assertEquals(182f, state.heightCm)
        assertEquals(82f, state.weightKg)
        assertEquals("Female", state.sex)
        assertEquals(5, state.daysPerWeek)
        assertEquals(75, state.sessionMinutes)
        assertEquals("Evening", state.preferredSchedule)
        assertEquals("Time", state.limitationsPreferences)
    }

    @Test
    fun `toggleEquipment adds and removes items properly`() {
        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.selectedEquipment.isEmpty())

        viewModel.toggleEquipment("Barbell")
        assertTrue("Barbell" in viewModel.uiState.value.selectedEquipment)

        viewModel.toggleEquipment("Dumbbell")
        assertEquals(setOf("Barbell", "Dumbbell"), viewModel.uiState.value.selectedEquipment)

        viewModel.toggleEquipment("Barbell")
        assertEquals(setOf("Dumbbell"), viewModel.uiState.value.selectedEquipment)
    }

    @Test
    fun `isStepValid validates mandatory selections`() {
        val viewModel = createViewModel()

        // GOAL step requires non-null goal
        viewModel.next() // GOAL
        assertFalse(viewModel.uiState.value.isStepValid)
        viewModel.selectGoal("Fat Loss")
        assertTrue(viewModel.uiState.value.isStepValid)

        // EXPERIENCE step requires non-null experience
        viewModel.next() // EXPERIENCE
        assertFalse(viewModel.uiState.value.isStepValid)
        viewModel.selectExperience("Beginner")
        assertTrue(viewModel.uiState.value.isStepValid)
    }

    @Test
    fun `completeOnboarding generates program, saves profile, and calls onComplete callback`() = runTest(testDispatcher) {
        val mockGeneratedProgram = mockk<ProgramGenerator.GeneratedProgram>(relaxed = true)
        coEvery { programGenerator.generateProgram(any(), any(), any()) } returns mockGeneratedProgram

        val viewModel = createViewModel()

        // Advance to REVIEW step
        viewModel.selectGoal("V-Taper Hypertrophy")
        viewModel.selectExperience("Advanced")
        viewModel.toggleEquipment("Barbell")

        while (viewModel.uiState.value.step != OnboardingStep.REVIEW) {
            viewModel.next()
        }

        assertTrue(viewModel.uiState.value.isLastContentStep)

        var completed = false
        viewModel.completeOnboarding { completed = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(completed)
        assertEquals(OnboardingStep.COMPLETE, viewModel.uiState.value.step)
        assertFalse(viewModel.uiState.value.isGenerating)
        assertNull(viewModel.uiState.value.error)

        coVerify { userProfileRepository.saveProfile(match { it.goal == "V-Taper Hypertrophy" && it.experience == "Advanced" }) }
        coVerify { programRepository.saveGeneratedProgram(mockGeneratedProgram) }
        verify { editor.putBoolean("onboarding_complete", true) }
    }
}
