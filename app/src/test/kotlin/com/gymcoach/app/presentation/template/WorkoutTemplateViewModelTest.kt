package com.gymcoach.app.presentation.template

import com.gymcoach.app.domain.model.TemplateExercise
import com.gymcoach.app.domain.model.WorkoutTemplate
import com.gymcoach.app.domain.repository.WorkoutTemplateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutTemplateViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: WorkoutTemplateRepository
    private lateinit var viewModel: WorkoutTemplateViewModel

    private val sampleExercise = TemplateExercise(
        id = 1L,
        templateId = 10L,
        exerciseId = 101L,
        exerciseName = "Barbell Bench Press",
        muscleGroup = "CHEST",
        equipment = "BARBELL",
        orderIndex = 0,
        targetSets = 4,
        targetReps = "8-10",
        targetWeightKg = 80.0,
        targetRpe = 8.5,
        restSeconds = 120
    )

    private val sampleTemplate = WorkoutTemplate(
        id = 10L,
        name = "Chest Hypertrophy",
        description = "Heavy push day focus",
        isArchived = false,
        version = 1,
        exercises = listOf(sampleExercise)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)

        every { repository.getActiveTemplates() } returns flowOf(listOf(sampleTemplate))
        every { repository.getArchivedTemplates() } returns flowOf(emptyList())

        viewModel = WorkoutTemplateViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun templatesState_emitsActiveTemplatesFromRepository() = runTest {
        // SharingStarted.WhileSubscribed requires an active collector + advanceUntilIdle
        // to flush the upstream flowOf() into the StateFlow.
        val collected = mutableListOf<List<WorkoutTemplate>>()
        val collectJob = backgroundScope.launch {
            viewModel.templates.collect { collected.add(it) }
        }
        advanceUntilIdle()
        assertTrue("Expected at least one emission", collected.isNotEmpty())
        val templates = collected.last()
        assertEquals(1, templates.size)
        assertEquals("Chest Hypertrophy", templates.first().name)
        assertEquals(1, templates.first().exercises.size)
        collectJob.cancel()
    }

    @Test
    fun validateTemplate_withValidData_returnsTrueAndNoErrors() {
        val isValid = viewModel.validateTemplate("Push Day", listOf(sampleExercise))
        assertTrue(isValid)
        assertTrue(viewModel.validationErrors.value.isEmpty())
    }

    @Test
    fun validateTemplate_withBlankName_returnsFalseAndSetsNameError() {
        val isValid = viewModel.validateTemplate("   ", listOf(sampleExercise))
        assertFalse(isValid)
        assertEquals("Template name cannot be empty", viewModel.validationErrors.value["name"])
    }

    @Test
    fun validateTemplate_withEmptyExercises_returnsFalseAndSetsExerciseError() {
        val isValid = viewModel.validateTemplate("Leg Day", emptyList())
        assertFalse(isValid)
        assertEquals("Template must have at least one exercise", viewModel.validationErrors.value["exercises"])
    }

    @Test
    fun validateTemplate_withZeroSets_returnsFalseAndSetsSetError() {
        val invalidExercise = sampleExercise.copy(targetSets = 0)
        val isValid = viewModel.validateTemplate("Back Day", listOf(invalidExercise))
        assertFalse(isValid)
        assertEquals("Sets must be at least 1", viewModel.validationErrors.value["exercise_0"])
    }

    @Test
    fun saveTemplate_whenValid_callsRepositorySaveAndTriggersSuccess() = runTest {
        coEvery { repository.saveTemplate(any(), any()) } returns 42L
        var savedId: Long? = null

        viewModel.saveTemplate(sampleTemplate, listOf(sampleExercise)) { id ->
            savedId = id
        }

        assertEquals(42L, savedId)
        coVerify(exactly = 1) { repository.saveTemplate(sampleTemplate, listOf(sampleExercise)) }
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun saveTemplate_whenInvalid_doesNotCallRepository() = runTest {
        viewModel.saveTemplate(sampleTemplate.copy(name = ""), listOf(sampleExercise))

        coVerify(exactly = 0) { repository.saveTemplate(any(), any()) }
    }

    @Test
    fun duplicateTemplate_whenSuccessful_callsCallback() = runTest {
        coEvery { repository.duplicateTemplate(10L) } returns 99L
        var duplicatedId: Long? = null

        viewModel.duplicateTemplate(10L) { newId ->
            duplicatedId = newId
        }

        assertEquals(99L, duplicatedId)
        coVerify(exactly = 1) { repository.duplicateTemplate(10L) }
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun duplicateTemplate_whenFails_setsErrorMessage() = runTest {
        coEvery { repository.duplicateTemplate(10L) } throws RuntimeException("Database error")

        viewModel.duplicateTemplate(10L)

        assertEquals("Database error", viewModel.errorMessage.value)
    }

    @Test
    fun archiveTemplate_callsRepository() = runTest {
        viewModel.archiveTemplate(10L)
        coVerify(exactly = 1) { repository.archiveTemplate(10L) }
    }

    @Test
    fun unarchiveTemplate_callsRepository() = runTest {
        viewModel.unarchiveTemplate(10L)
        coVerify(exactly = 1) { repository.unarchiveTemplate(10L) }
    }

    @Test
    fun deleteTemplate_callsRepository() = runTest {
        viewModel.deleteTemplate(10L)
        coVerify(exactly = 1) { repository.deleteTemplate(10L) }
    }

    @Test
    fun startWorkoutFromTemplate_callsRepositoryAndInvokesCallback() = runTest {
        coEvery { repository.startWorkoutFromTemplate(10L) } returns 505L
        var startedWorkoutId: Long? = null

        viewModel.startWorkoutFromTemplate(10L) { workoutId ->
            startedWorkoutId = workoutId
        }

        assertEquals(505L, startedWorkoutId)
        coVerify(exactly = 1) { repository.startWorkoutFromTemplate(10L) }
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun validateTemplate_withNegativeWeight_returnsFalse() {
        val invalidExercise = sampleExercise.copy(targetWeightKg = -5.0)
        val isValid = viewModel.validateTemplate("Push Day", listOf(invalidExercise))
        assertFalse(isValid)
        assertEquals("Target weight cannot be negative", viewModel.validationErrors.value["exercise_0_weight"])
    }

    @Test
    fun validateTemplate_withNegativeRestSeconds_returnsFalse() {
        val invalidExercise = sampleExercise.copy(restSeconds = -30)
        val isValid = viewModel.validateTemplate("Push Day", listOf(invalidExercise))
        assertFalse(isValid)
        assertEquals("Rest seconds cannot be negative", viewModel.validationErrors.value["exercise_0_rest"])
    }

    @Test
    fun validateTemplate_withInvalidRpe_returnsFalse() {
        val invalidExercise = sampleExercise.copy(targetRpe = 11.5)
        val isValid = viewModel.validateTemplate("Push Day", listOf(invalidExercise))
        assertFalse(isValid)
        assertEquals("RPE must be between 1.0 and 10.0", viewModel.validationErrors.value["exercise_0_rpe"])
    }

    @Test
    fun reorderExercises_movesItemAndReindexesCorrectly() {
        val ex1 = sampleExercise.copy(id = 1L, exerciseName = "Bench Press", orderIndex = 0)
        val ex2 = sampleExercise.copy(id = 2L, exerciseName = "Incline Dumbbell", orderIndex = 1)
        val ex3 = sampleExercise.copy(id = 3L, exerciseName = "Cable Flyes", orderIndex = 2)

        val reordered = viewModel.reorderExercises(listOf(ex1, ex2, ex3), fromIndex = 2, toIndex = 0)

        assertEquals(3, reordered.size)
        assertEquals("Cable Flyes", reordered[0].exerciseName)
        assertEquals(0, reordered[0].orderIndex)
        assertEquals("Bench Press", reordered[1].exerciseName)
        assertEquals(1, reordered[1].orderIndex)
        assertEquals("Incline Dumbbell", reordered[2].exerciseName)
        assertEquals(2, reordered[2].orderIndex)
    }

    @Test
    fun reorderExercises_withOutOfBounds_returnsOriginalList() {
        val ex1 = sampleExercise.copy(id = 1L, orderIndex = 0)
        val list = listOf(ex1)

        val result = viewModel.reorderExercises(list, fromIndex = 0, toIndex = 5)
        assertEquals(list, result)
    }

    @Test
    fun addExerciseToTemplate_appendsAndSetsOrderIndex() {
        val ex1 = sampleExercise.copy(id = 1L, orderIndex = 0)
        val newEx = sampleExercise.copy(id = 2L, orderIndex = 99)

        val updated = viewModel.addExerciseToTemplate(listOf(ex1), newEx)
        assertEquals(2, updated.size)
        assertEquals(1, updated[1].orderIndex)
    }

    @Test
    fun removeExerciseFromTemplate_removesAndReindexesOrder() {
        val ex1 = sampleExercise.copy(id = 1L, exerciseName = "Squat", orderIndex = 0)
        val ex2 = sampleExercise.copy(id = 2L, exerciseName = "Leg Press", orderIndex = 1)
        val ex3 = sampleExercise.copy(id = 3L, exerciseName = "Calf Raise", orderIndex = 2)

        val updated = viewModel.removeExerciseFromTemplate(listOf(ex1, ex2, ex3), indexToRemove = 1)
        assertEquals(2, updated.size)
        assertEquals("Squat", updated[0].exerciseName)
        assertEquals(0, updated[0].orderIndex)
        assertEquals("Calf Raise", updated[1].exerciseName)
        assertEquals(1, updated[1].orderIndex)
    }
}
