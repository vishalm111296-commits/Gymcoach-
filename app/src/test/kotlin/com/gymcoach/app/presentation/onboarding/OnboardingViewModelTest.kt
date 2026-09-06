package com.gymcoach.app.presentation.onboarding

import android.content.Context
import android.content.SharedPreferences
import com.gymcoach.app.core.program.ProgramGenerator
import com.gymcoach.app.data.local.entity.UserProfileEntity
import com.gymcoach.app.domain.repository.ProgramRepository
import com.gymcoach.app.domain.repository.UserProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var programGenerator: ProgramGenerator
    private lateinit var programRepository: ProgramRepository
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        userProfileRepository = mockk(relaxed = true)
        programGenerator = mockk(relaxed = true)
        programRepository = mockk(relaxed = true)

        every { context.getSharedPreferences("gymcoach_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor

        viewModel = OnboardingViewModel(
            context = context,
            userProfileRepository = userProfileRepository,
            programGenerator = programGenerator,
            programRepository = programRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial UI state is default welcome step`() {
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
        assertTrue(state.selectedEquipment.isEmpty())
        assertFalse(state.isGenerating)
        assertNull(state.error)
        assertTrue(state.isStepValid)
    }

    @Test
    fun `state setters update UI state correctly`() {
        viewModel.selectGoal("Build muscle")
        viewModel.selectExperience("Intermediate")
        viewModel.setAge(30f)
        viewModel.setHeight(180f)
        viewModel.setWeight(80f)
        viewModel.setSex("Female")
        viewModel.setDaysPerWeek(5)
        viewModel.setSessionMinutes(45)
        viewModel.setPreferredSchedule("Evening")
        viewModel.setLimitationsPreferences("No squats")

        val state = viewModel.uiState.value
        assertEquals("Build muscle", state.goal)
        assertEquals("Intermediate", state.experience)
        assertEquals(30f, state.age)
        assertEquals(180f, state.heightCm)
        assertEquals(80f, state.weightKg)
        assertEquals("Female", state.sex)
        assertEquals(5, state.daysPerWeek)
        assertEquals(45, state.sessionMinutes)
        assertEquals("Evening", state.preferredSchedule)
        assertEquals("No squats", state.limitationsPreferences)
    }

    @Test
    fun `toggleEquipment adds and removes equipment`() {
        viewModel.toggleEquipment("Dumbbell")
        assertTrue("Dumbbell" in viewModel.uiState.value.selectedEquipment)

        viewModel.toggleEquipment("Bench")
        assertEquals(setOf("Dumbbell", "Bench"), viewModel.uiState.value.selectedEquipment)

        viewModel.toggleEquipment("Dumbbell")
        assertEquals(setOf("Bench"), viewModel.uiState.value.selectedEquipment)
    }

    @Test
    fun `next and back navigate through steps`() {
        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.step)

        viewModel.next()
        assertEquals(OnboardingStep.GOAL, viewModel.uiState.value.step)

        viewModel.next()
        assertEquals(OnboardingStep.EXPERIENCE, viewModel.uiState.value.step)

        viewModel.back()
        assertEquals(OnboardingStep.GOAL, viewModel.uiState.value.step)

        viewModel.back()
        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.step)

        viewModel.back() // at start, stay at WELCOME
        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.step)
    }

    @Test
    fun `isStepValid validates specific steps correctly`() {
        // GOAL step
        viewModel.next() // GOAL
        assertFalse(viewModel.uiState.value.isStepValid)
        viewModel.selectGoal("Hypertrophy")
        assertTrue(viewModel.uiState.value.isStepValid)

        // EXPERIENCE step
        viewModel.next() // EXPERIENCE
        assertFalse(viewModel.uiState.value.isStepValid)
        viewModel.selectExperience("Beginner")
        assertTrue(viewModel.uiState.value.isStepValid)

        // PERSONAL_INFO step
        viewModel.next() // PERSONAL_INFO
        assertTrue(viewModel.uiState.value.isStepValid) // default values valid
        viewModel.setAge(10f) // < 14
        assertFalse(viewModel.uiState.value.isStepValid)
        viewModel.setAge(25f)
        assertTrue(viewModel.uiState.value.isStepValid)
    }

    @Test
    fun `completeOnboarding succeeds on last content step`() = runTest {
        // Navigate to REVIEW step
        while (viewModel.uiState.value.step != OnboardingStep.REVIEW) {
            viewModel.next()
        }
        viewModel.selectGoal("Build muscle")
        viewModel.selectExperience("Intermediate")
        viewModel.toggleEquipment("Barbell")
        viewModel.setDaysPerWeek(5)

        val generatedProgram = ProgramGenerator.GeneratedProgram(
            name = "V-Taper 5-Day",
            description = "Custom program",
            goal = "Build muscle",
            frequency = 5,
            days = emptyList()
        )
        coEvery { programGenerator.generateProgram(5, "gym", "Build muscle") } returns generatedProgram

        var completedCalled = false
        viewModel.completeOnboarding { completedCalled = true }

        assertTrue("onComplete callback should be called", completedCalled)
        val state = viewModel.uiState.value
        assertEquals(OnboardingStep.COMPLETE, state.step)
        assertFalse(state.isGenerating)
        assertNull(state.error)

        val profileSlot = slot<UserProfileEntity>()
        coVerify { userProfileRepository.saveProfile(capture(profileSlot)) }
        assertEquals("Build muscle", profileSlot.captured.goal)
        assertEquals("Intermediate", profileSlot.captured.experience)
        assertEquals("gym", profileSlot.captured.equipmentType)

        coVerify { programRepository.saveGeneratedProgram(generatedProgram) }
        verify { editor.putBoolean("onboarding_complete", true) }
        verify { editor.apply() }
    }

    @Test
    fun `completeOnboarding catches exception during saveProfile`() = runTest {
        navigateToReviewStep()
        coEvery { userProfileRepository.saveProfile(any()) } throws RuntimeException("Database error")

        var completedCalled = false
        viewModel.completeOnboarding { completedCalled = true }

        assertFalse("onComplete callback should not be called on error", completedCalled)
        val state = viewModel.uiState.value
        assertFalse(state.isGenerating)
        assertEquals("Database error", state.error)
        assertEquals(OnboardingStep.REVIEW, state.step)
    }

    @Test
    fun `completeOnboarding catches exception during generateProgram`() = runTest {
        navigateToReviewStep()
        coEvery { programGenerator.generateProgram(any(), any(), any()) } throws RuntimeException("Generation failed")

        var completedCalled = false
        viewModel.completeOnboarding { completedCalled = true }

        assertFalse(completedCalled)
        val state = viewModel.uiState.value
        assertFalse(state.isGenerating)
        assertEquals("Generation failed", state.error)
        assertEquals(OnboardingStep.REVIEW, state.step)
    }

    @Test
    fun `completeOnboarding catches exception during saveGeneratedProgram`() = runTest {
        navigateToReviewStep()
        val generatedProgram = ProgramGenerator.GeneratedProgram(
            name = "Test",
            description = "Test",
            goal = "Build muscle",
            frequency = 4,
            days = emptyList()
        )
        coEvery { programGenerator.generateProgram(any(), any(), any()) } returns generatedProgram
        coEvery { programRepository.saveGeneratedProgram(any()) } throws RuntimeException("Failed to save program")

        var completedCalled = false
        viewModel.completeOnboarding { completedCalled = true }

        assertFalse(completedCalled)
        val state = viewModel.uiState.value
        assertFalse(state.isGenerating)
        assertEquals("Failed to save program", state.error)
        assertEquals(OnboardingStep.REVIEW, state.step)
    }

    @Test
    fun `completeOnboarding handles exception with null message`() = runTest {
        navigateToReviewStep()
        coEvery { userProfileRepository.saveProfile(any()) } throws RuntimeException(null as String?)

        var completedCalled = false
        viewModel.completeOnboarding { completedCalled = true }

        assertFalse(completedCalled)
        val state = viewModel.uiState.value
        assertFalse(state.isGenerating)
        assertEquals("Could not generate your program", state.error)
    }

    @Test
    fun `completeOnboarding guard does nothing when not on REVIEW step`() = runTest {
        viewModel.selectGoal("Build muscle")
        viewModel.selectExperience("Intermediate")

        var completedCalled = false
        viewModel.completeOnboarding { completedCalled = true }

        assertFalse(completedCalled)
        coVerify(exactly = 0) { userProfileRepository.saveProfile(any()) }
    }

    @Test
    fun `completeOnboarding guard does nothing when goal is null`() = runTest {
        advanceToReviewStepWithoutSelections()
        viewModel.selectExperience("Intermediate")
        // goal is still null

        var completedCalled = false
        viewModel.completeOnboarding { completedCalled = true }

        assertFalse(completedCalled)
        coVerify(exactly = 0) { userProfileRepository.saveProfile(any()) }
    }

    @Test
    fun `completeOnboarding guard does nothing when experience is null`() = runTest {
        advanceToReviewStepWithoutSelections()
        viewModel.selectGoal("Build muscle")
        // experience is still null

        var completedCalled = false
        viewModel.completeOnboarding { completedCalled = true }

        assertFalse(completedCalled)
        coVerify(exactly = 0) { userProfileRepository.saveProfile(any()) }
    }

    private fun advanceToReviewStepWithoutSelections() {
        while (viewModel.uiState.value.step != OnboardingStep.REVIEW) {
            viewModel.next()
        }
    }

    private fun navigateToReviewStep() {
        advanceToReviewStepWithoutSelections()
        viewModel.selectGoal("Build muscle")
        viewModel.selectExperience("Intermediate")
    }
}
