package com.gymcoach.app.presentation.history

import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.model.SetType
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutExercise
import com.gymcoach.app.domain.model.WorkoutExerciseWithSets
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutWithDetails
import com.gymcoach.app.domain.repository.WorkoutRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutHistoryDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var viewModel: WorkoutHistoryDetailViewModel

    private val sampleWorkout = Workout(
        id = 201L,
        date = Instant.ofEpochMilli(1700000000000L),
        startTime = Instant.ofEpochMilli(1700000000000L),
        endTime = Instant.ofEpochMilli(1700003600000L),
        duration = 3600L,
        completed = true,
        status = "COMPLETED",
        notes = "Original notes"
    )

    private val sampleDetails = WorkoutWithDetails(
        workout = sampleWorkout,
        exercises = listOf(
            WorkoutExerciseWithSets(
                workoutExercise = WorkoutExercise(id = 1L, workoutId = 201L, exerciseId = 10L, orderIndex = 0),
                exercise = Exercise(
                    id = 10L,
                    name = "Bench Press",
                    description = "Barbell chest press",
                    muscleGroup = "Chest",
                    equipment = "Barbell",
                    difficulty = "Intermediate"
                ),
                sets = listOf(
                    WorkoutSet(id = 1L, workoutExerciseId = 1L, setNumber = 1, weight = 100.0, reps = 8, rpe = 8.0, restSeconds = 90, completed = true, setType = SetType.NORMAL)
                )
            )
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        workoutRepository = mockk(relaxed = true)
        every { workoutRepository.getWorkoutWithDetails(201L) } returns flowOf(sampleDetails)
        viewModel = WorkoutHistoryDetailViewModel(workoutRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadWorkout populates uiState successfully`() = runTest {
        viewModel.loadWorkout(201L)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNotNull(state.workout)
        assertEquals(201L, state.workout?.workout?.id)
        assertEquals("Original notes", state.workout?.workout?.notes)
    }

    @Test
    fun `updateNotes calls repository updateWorkout and refreshes state in place`() = runTest {
        viewModel.loadWorkout(201L)
        val updatedNotes = "Updated session notes with PR commentary"

        coEvery { workoutRepository.updateWorkout(any()) } returns Unit

        viewModel.updateNotes(updatedNotes)

        coVerify(exactly = 1) {
            workoutRepository.updateWorkout(match { it.id == 201L && it.notes == updatedNotes })
        }
        assertEquals(updatedNotes, viewModel.uiState.value.workout?.workout?.notes)
    }

    @Test
    fun `performAgain invokes createWorkoutFromHistory and triggers callback`() = runTest {
        coEvery { workoutRepository.createWorkoutFromHistory(201L) } returns 305L

        var callbackResult: Long? = null
        viewModel.performAgain(201L) { createdId ->
            callbackResult = createdId
        }

        assertEquals(305L, callbackResult)
        coVerify(exactly = 1) { workoutRepository.createWorkoutFromHistory(201L) }
    }

    @Test
    fun `delete dialog flow toggles showDeleteConfirmation sets target and calls deleteWorkout`() = runTest {
        assertFalse(viewModel.showDeleteConfirmation.value)
        assertNull(viewModel.deleteTarget.value)

        viewModel.onDeleteClick(201L)
        assertEquals(201L, viewModel.deleteTarget.value)
        assertTrue(viewModel.showDeleteConfirmation.value)

        viewModel.confirmDelete()
        coVerify(exactly = 1) { workoutRepository.deleteWorkout(201L) }
        assertNull(viewModel.deleteTarget.value)
        assertFalse(viewModel.showDeleteConfirmation.value)

        viewModel.onDeleteClick(201L)
        assertTrue(viewModel.showDeleteConfirmation.value)
        viewModel.cancelDelete()
        assertNull(viewModel.deleteTarget.value)
        assertFalse(viewModel.showDeleteConfirmation.value)
    }

    @Test
    fun `loadWorkout repository exception surfaces as error state with isLoading false`() = runTest {
        every { workoutRepository.getWorkoutWithDetails(999L) } throws RuntimeException("Workout not found")

        viewModel.loadWorkout(999L)

        val state = viewModel.uiState.value
        assertEquals("Workout not found", state.error)
        assertFalse(state.isLoading)
        assertNull(state.workout)
    }

    @Test
    fun `updateNotes repository exception surfaces as error state`() = runTest {
        viewModel.loadWorkout(201L)
        coEvery { workoutRepository.updateWorkout(any()) } throws RuntimeException("Update failed")

        viewModel.updateNotes("New Note")

        val state = viewModel.uiState.value
        assertEquals("Update failed", state.error)
    }

    @Test
    fun `performAgain repository exception surfaces as error state`() = runTest {
        coEvery { workoutRepository.createWorkoutFromHistory(201L) } throws RuntimeException("Create failed")

        var callbackInvoked = false
        viewModel.performAgain(201L) { callbackInvoked = true }

        assertFalse("Callback should not be invoked on failure", callbackInvoked)
        assertEquals("Failed to repeat workout", viewModel.uiState.value.error)
    }

    @Test
    fun `confirmDelete repository exception surfaces as error state`() = runTest {
        coEvery { workoutRepository.deleteWorkout(201L) } throws RuntimeException("Delete failed")

        viewModel.onDeleteClick(201L)
        viewModel.confirmDelete()

        assertEquals("Failed to delete workout", viewModel.uiState.value.error)
    }

    @Test
    fun `dismissError clears error state`() = runTest {
        coEvery { workoutRepository.deleteWorkout(201L) } throws RuntimeException("Delete failed")

        viewModel.onDeleteClick(201L)
        viewModel.confirmDelete()

        assertNotNull(viewModel.uiState.value.error)
        viewModel.dismissError()
        assertNull(viewModel.uiState.value.error)
    }
}
