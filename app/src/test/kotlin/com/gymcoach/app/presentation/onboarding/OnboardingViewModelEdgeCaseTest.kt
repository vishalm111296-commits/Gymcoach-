package com.gymcoach.app.presentation.onboarding

import android.content.Context
import android.content.SharedPreferences
import com.gymcoach.app.core.program.ProgramGenerator
import com.gymcoach.app.domain.repository.ProgramRepository
import com.gymcoach.app.domain.repository.UserProfileRepository
import io.mockk.coEvery
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
class OnboardingViewModelEdgeCaseTest {

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

        every { context.getSharedPreferences(any(), any()) } returns sharedPrefs
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
    fun `completeOnboarding prevents silent missing data from advancing`() = runTest {
        // Do not set goal
        while (viewModel.uiState.value.step != OnboardingStep.REVIEW) viewModel.next()
        viewModel.selectExperience("Beginner")

        var completeCalled = false
        viewModel.completeOnboarding { completeCalled = true }
        advanceUntilIdle()

        assertFalse(completeCalled)
        assertEquals(OnboardingStep.REVIEW, viewModel.uiState.value.step)
    }

    @Test
    fun `completeOnboarding handles engine crash gracefully`() = runTest {
        while (viewModel.uiState.value.step != OnboardingStep.REVIEW) viewModel.next()
        viewModel.selectGoal("Strength")
        viewModel.selectExperience("Beginner")

        coEvery { userProfileRepo.saveProfile(any()) } returns 1L
        coEvery { programGenerator.generateProgram(any(), any(), any()) } throws IllegalStateException("Impossible Constraints")

        viewModel.completeOnboarding {}
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isGenerating)
        assertEquals("Impossible Constraints", viewModel.uiState.value.error)
        assertEquals(OnboardingStep.REVIEW, viewModel.uiState.value.step)
    }
}
