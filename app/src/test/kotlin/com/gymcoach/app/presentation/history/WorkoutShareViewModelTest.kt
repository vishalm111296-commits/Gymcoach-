package com.gymcoach.app.presentation.history

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.gymcoach.app.core.share.WorkoutShareCardBuilder
import com.gymcoach.app.core.share.WorkoutShareCardData
import com.gymcoach.app.core.share.WorkoutShareCardRenderer
import com.gymcoach.app.domain.model.WorkoutWithDetails
import com.gymcoach.app.domain.repository.WorkoutRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutShareViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    
    private lateinit var viewModel: WorkoutShareViewModel
    private val workoutRepository: WorkoutRepository = mockk()
    private val shareCardBuilder: WorkoutShareCardBuilder = mockk()
    private val shareCardRenderer: WorkoutShareCardRenderer = mockk()
    private val context: Context = mockk()
    private val uri: Uri = mockk()
    private val bitmap: Bitmap = mockk()
    private val workoutWithDetails: WorkoutWithDetails = mockk()
    private val cardData: WorkoutShareCardData = mockk()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = WorkoutShareViewModel(workoutRepository, shareCardBuilder, shareCardRenderer)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() {
        assertEquals(ShareState.Idle, viewModel.shareState.value)
    }

    @Test
    fun `generateStoryCard transitions to Success when all steps succeed`() = runTest(testDispatcher) {
        val workoutId = 1L
        
        coEvery { workoutRepository.getWorkoutWithDetails(workoutId) } returns flowOf(workoutWithDetails)
        every { shareCardBuilder.buildShareData(workoutWithDetails) } returns cardData
        every { shareCardRenderer.renderToBitmap(cardData) } returns bitmap
        every { shareCardRenderer.saveShareImage(context, bitmap, workoutId) } returns uri

        viewModel.generateStoryCard(workoutId, context)
        
        // After completion it should be Success
        assertTrue(viewModel.shareState.value is ShareState.Success)
        assertEquals(uri, (viewModel.shareState.value as ShareState.Success).uri)
    }
    
    @Test
    fun `generateStoryCard transitions to Error when workout not found`() = runTest(testDispatcher) {
        val workoutId = 1L
        
        coEvery { workoutRepository.getWorkoutWithDetails(workoutId) } returns flowOf(null)

        viewModel.generateStoryCard(workoutId, context)
        
        assertTrue(viewModel.shareState.value is ShareState.Error)
        assertEquals("Workout not found", (viewModel.shareState.value as ShareState.Error).msg)
    }

    @Test
    fun `generateStoryCard transitions to Error when uri is null`() = runTest(testDispatcher) {
        val workoutId = 1L
        
        coEvery { workoutRepository.getWorkoutWithDetails(workoutId) } returns flowOf(workoutWithDetails)
        every { shareCardBuilder.buildShareData(workoutWithDetails) } returns cardData
        every { shareCardRenderer.renderToBitmap(cardData) } returns bitmap
        every { shareCardRenderer.saveShareImage(context, bitmap, workoutId) } returns null

        viewModel.generateStoryCard(workoutId, context)
        
        assertTrue(viewModel.shareState.value is ShareState.Error)
        assertEquals("Failed to save image", (viewModel.shareState.value as ShareState.Error).msg)
    }

    @Test
    fun `generateStoryCard transitions to Error when exception occurs`() = runTest(testDispatcher) {
        val workoutId = 1L
        
        coEvery { workoutRepository.getWorkoutWithDetails(workoutId) } returns flowOf(workoutWithDetails)
        every { shareCardBuilder.buildShareData(workoutWithDetails) } throws RuntimeException("Rendering failed")

        viewModel.generateStoryCard(workoutId, context)
        
        assertTrue(viewModel.shareState.value is ShareState.Error)
        assertEquals("Rendering failed", (viewModel.shareState.value as ShareState.Error).msg)
    }
    
    @Test
    fun `resetState changes state to Idle`() {
        viewModel.resetState()
        assertEquals(ShareState.Idle, viewModel.shareState.value)
    }

    @Test
    fun `generateStoryCard transitions to Error on repository exception`() = runTest(testDispatcher) {
        val workoutId = 99L
        coEvery { workoutRepository.getWorkoutWithDetails(workoutId) } returns flowOf(workoutWithDetails)
        every { shareCardBuilder.buildShareData(workoutWithDetails) } throws RuntimeException("Bitmap OOM")

        viewModel.generateStoryCard(workoutId, context)

        val state = viewModel.shareState.value
        assertTrue(state is ShareState.Error)
        assertEquals("Bitmap OOM", (state as ShareState.Error).msg)
    }

    @Test
    fun `resetState after Success resets to Idle`() = runTest(testDispatcher) {
        val workoutId = 2L
        coEvery { workoutRepository.getWorkoutWithDetails(workoutId) } returns flowOf(workoutWithDetails)
        every { shareCardBuilder.buildShareData(workoutWithDetails) } returns cardData
        every { shareCardRenderer.renderToBitmap(cardData) } returns bitmap
        every { shareCardRenderer.saveShareImage(context, bitmap, workoutId) } returns uri

        viewModel.generateStoryCard(workoutId, context)
        assertTrue(viewModel.shareState.value is ShareState.Success)

        viewModel.resetState()
        assertEquals(ShareState.Idle, viewModel.shareState.value)
    }
}
