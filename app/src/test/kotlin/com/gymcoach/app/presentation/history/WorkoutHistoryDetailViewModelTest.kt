package com.gymcoach.app.presentation.history

import androidx.lifecycle.viewModelScope
import com.gymcoach.app.domain.repository.WorkoutRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * WorkoutHistoryDetailViewModel tests for the T2.8 UX-audit fix APP-038:
 * delete must not silently fail — Confirming -> Deleting -> Success/Failed
 * state machine, error surfaced on failure, Idle on cancel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutHistoryDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var viewModel: WorkoutHistoryDetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        workoutRepository = mockk(relaxed = true)
        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(null)
        viewModel = WorkoutHistoryDetailViewModel(workoutRepository)
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

@Test
    fun `APP-038 onDeleteClick enters Confirming state`() = runTest {
        viewModel.onDeleteClick(42L)

        assertTrue(viewModel.deleteState.value is DeleteState.Confirming)
    }

    @Test
    fun `APP-038 cancelDelete returns to Idle and clears target`() = runTest {
        viewModel.onDeleteClick(42L)
        viewModel.cancelDelete()

        assertTrue(viewModel.deleteState.value is DeleteState.Idle)
        assertNull(viewModel.deleteTarget.value)
    }

    @Test
    fun `APP-038 successful delete ends in Success state`() = runTest {
        coEvery { workoutRepository.deleteWorkout(42L) } returns Unit
        viewModel.onDeleteClick(42L)
        runCurrent()

        viewModel.confirmDelete()
        runCurrent()

        assertTrue("delete must succeed", viewModel.deleteState.value is DeleteState.Success)
    }

    @Test
    fun `APP-038 failed delete surfaces Failure state with message instead of silent pop`() = runTest {
        coEvery { workoutRepository.deleteWorkout(42L) } throws RuntimeException("disk full")
        viewModel.onDeleteClick(42L)
        runCurrent()

        viewModel.confirmDelete()
        runCurrent()

        val state = viewModel.deleteState.value
        assertTrue("delete failure must be surfaced", state is DeleteState.Failed)
        assertEquals("disk full", (state as DeleteState.Failed).message)
    }
}