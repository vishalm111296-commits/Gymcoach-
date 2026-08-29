package com.gymcoach.app.presentation.onboarding

import android.content.Context
import android.content.SharedPreferences
import com.gymcoach.app.core.program.ProgramGenerator.GeneratedProgram
import com.gymcoach.app.core.program.ProgramGenerator
import com.gymcoach.app.data.local.entity.UserProfileEntity
import com.gymcoach.app.domain.repository.ProgramRepository
import com.gymcoach.app.domain.repository.UserProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var sharedPrefsEditor: SharedPreferences.Editor
    private lateinit var userProfileRepo: UserProfileRepository
    private lateinit var programGenerator: ProgramGenerator
    private lateinit var programRepo: ProgramRepository
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk()
        sharedPrefs = mockk()
        sharedPrefsEditor = mockk()

        every { context.getSharedPreferences("gymcoach_prefs", Context.MODE_PRIVATE) } returns sharedPrefs
        every { sharedPrefs.edit() } returns sharedPrefsEditor
        every { sharedPrefsEditor.putBoolean(any(), any()) } returns sharedPrefsEditor
        every { sharedPrefsEditor.apply() } returns Unit
        coEvery { sharedPrefsEditor.putBoolean(any(), any()) } returns sharedPrefsEditor
        coEvery { sharedPrefsEditor.apply() } returns Unit

        userProfileRepo = mockk()
        programGenerator = mockk()
        programRepo = mockk()

        viewModel = OnboardingViewModel(context, userProfileRepo, programGenerator, programRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is WELCOME`() {
        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.step)
    }

    @Test
    fun `next() advances to next step`() {
        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.step)
        viewModel.next()
        assertEquals(OnboardingStep.GOAL, viewModel.uiState.value.step)
        viewModel.next()
        assertEquals(OnboardingStep.EXPERIENCE, viewModel.uiState.value.step)
    }

    @Test
    fun `back() returns to previous step`() {
        viewModel.next()
        assertEquals(OnboardingStep.GOAL, viewModel.uiState.value.step)
        viewModel.back()
        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.step)
    }

    @Test
    fun `completeOnboarding fails if not on last content step`() = runTest {
        viewModel.next() // Go to GOAL
        viewModel.selectGoal("Hypertrophy")
        viewModel.completeOnboarding {}
        advanceUntilIdle()

        // Should not save profile or program if not on REVIEW step
        coVerify(exactly = 0) { userProfileRepo.saveProfile(any()) }
        coVerify(exactly = 0) { programGenerator.generateProgram(any(), any(), any()) }
    }

    @Test
    fun `completeOnboarding processes valid profile and saves program`() = runTest {
        // Move to last content step
        while (viewModel.uiState.value.step != OnboardingStep.REVIEW) {
            viewModel.next()
        }

        viewModel.selectGoal("V-Taper")
        viewModel.selectExperience("Intermediate")
        viewModel.setDaysPerWeek(4)
        viewModel.toggleEquipment("Dumbbell")

        coEvery { userProfileRepo.saveProfile(any()) } returns 1L
        coEvery { sharedPrefsEditor.putBoolean(any(), any()) } returns sharedPrefsEditor
        coEvery { sharedPrefsEditor.apply() } returns Unit
        coEvery { programGenerator.generateProgram(any(), any(), any()) } returns ProgramGenerator.GeneratedProgram("Mock", "Mock", "Strength", 4, emptyList())
        coEvery { programRepo.saveGeneratedProgram(any()) } returns 1L
        coEvery { sharedPrefsEditor.putBoolean(any(), any()) } returns sharedPrefsEditor
        coEvery { sharedPrefsEditor.apply() } returns Unit

        var completeCalled = false
        viewModel.completeOnboarding { completeCalled = true }
        advanceUntilIdle()

        assertTrue(completeCalled)
        assertEquals(OnboardingStep.COMPLETE, viewModel.uiState.value.step)

        coVerify {
            userProfileRepo.saveProfile(match {
                it.goal == "V-Taper" && it.experience == "Intermediate" && it.equipmentType == "home"
            })
        }
        coVerify { programGenerator.generateProgram(4, "home", "V-Taper") }
        coVerify { programRepo.saveGeneratedProgram(any()) }
    }

    @Test
    fun `equipment mapped correctly to gym`() = runTest {
        while (viewModel.uiState.value.step != OnboardingStep.REVIEW) viewModel.next()
        viewModel.selectGoal("Strength")
        viewModel.selectExperience("Advanced")
        viewModel.toggleEquipment("Barbell") // Triggers gym mapping

        coEvery { userProfileRepo.saveProfile(any()) } returns 1L
        coEvery { sharedPrefsEditor.putBoolean(any(), any()) } returns sharedPrefsEditor
        coEvery { sharedPrefsEditor.apply() } returns Unit
        coEvery { programGenerator.generateProgram(any(), any(), any()) } returns ProgramGenerator.GeneratedProgram("Mock", "Mock", "Strength", 4, emptyList())
        coEvery { programRepo.saveGeneratedProgram(any()) } returns 1L
        coEvery { sharedPrefsEditor.putBoolean(any(), any()) } returns sharedPrefsEditor
        coEvery { sharedPrefsEditor.apply() } returns Unit

        viewModel.completeOnboarding {}
        advanceUntilIdle()

        coVerify { programGenerator.generateProgram(any(), "gym", any()) }
    }

    @Test
    fun `equipment mapped correctly to custom when empty`() = runTest {
        while (viewModel.uiState.value.step != OnboardingStep.REVIEW) viewModel.next()
        viewModel.selectGoal("Health")
        viewModel.selectExperience("Beginner")
        // No equipment selected -> custom (bodyweight only)

        coEvery { userProfileRepo.saveProfile(any()) } returns 1L
        coEvery { sharedPrefsEditor.putBoolean(any(), any()) } returns sharedPrefsEditor
        coEvery { sharedPrefsEditor.apply() } returns Unit
        coEvery { programGenerator.generateProgram(any(), any(), any()) } returns ProgramGenerator.GeneratedProgram("Mock", "Mock", "Strength", 4, emptyList())
        coEvery { programRepo.saveGeneratedProgram(any()) } returns 1L
        coEvery { sharedPrefsEditor.putBoolean(any(), any()) } returns sharedPrefsEditor
        coEvery { sharedPrefsEditor.apply() } returns Unit

        viewModel.completeOnboarding {}
        advanceUntilIdle()

        coVerify { programGenerator.generateProgram(any(), "custom", any()) }
    }

    @Test
    fun `completeOnboarding handles exceptions securely`() = runTest {
        while (viewModel.uiState.value.step != OnboardingStep.REVIEW) viewModel.next()
        viewModel.selectGoal("General")
        viewModel.selectExperience("Beginner")

        coEvery { userProfileRepo.saveProfile(any()) } throws Exception("Database Error")

        viewModel.completeOnboarding {}
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isGenerating)
        assertEquals("Database Error", viewModel.uiState.value.error)
        assertEquals(OnboardingStep.REVIEW, viewModel.uiState.value.step) // Did not advance
    }
}
