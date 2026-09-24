package com.gymcoach.app.presentation

import com.gymcoach.app.core.animation.AnimationRepository
import com.gymcoach.app.core.animation.ExerciseAnimationDefinition
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.repository.ExerciseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class ExerciseViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: ExerciseRepository
    private lateinit var animationRepository: AnimationRepository
    private lateinit var viewModel: ExerciseViewModel

    private val sampleExercises = listOf(
        Exercise(
            id = 1L,
            name = "Barbell Bench Press",
            description = "Barbell horizontal press",
            muscleGroup = "Chest",
            equipment = "Barbell",
            difficulty = "Intermediate",
            movementPattern = "Push",
            animationUrl = "barbell_bench_press",
            isFavorite = true
        ),
        Exercise(
            id = 2L,
            name = "Bodyweight Squat",
            description = "Fundamental lower body movement",
            muscleGroup = "Legs",
            equipment = "Bodyweight",
            difficulty = "Beginner",
            movementPattern = "Squat",
            animationUrl = "bodyweight_squat",
            isFavorite = false
        ),
        Exercise(
            id = 3L,
            name = "Custom Lateral Raise",
            description = "Side delt isolation",
            muscleGroup = "Shoulders",
            equipment = "Dumbbell",
            difficulty = "Intermediate",
            movementPattern = "Isolation",
            animationUrl = null,
            isFavorite = false,
            isCustom = true
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        animationRepository = mockk(relaxed = true)

        every { repository.getFilteredExercises(any(), any(), any()) } returns flowOf(sampleExercises)
        every { repository.searchExercises(any()) } returns flowOf(sampleExercises)
        coEvery { animationRepository.getAllAnimations() } returns listOf(
            mockk<ExerciseAnimationDefinition>(relaxed = true) {
                every { exerciseName } returns "Barbell Bench Press"
            },
            mockk<ExerciseAnimationDefinition>(relaxed = true) {
                every { exerciseName } returns "Bodyweight Squat"
            }
        )

        viewModel = ExerciseViewModel(repository, animationRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial filter states are default`() {
        assertEquals("All", viewModel.filterCategory.value)
        assertEquals("All", viewModel.filterDifficulty.value)
        assertEquals("All", viewModel.filterEquipment.value)
        assertEquals("All", viewModel.filterMovementPattern.value)
        assertFalse(viewModel.showFavoritesOnly.value)
        assertFalse(viewModel.showAnimationOnly.value)
        assertFalse(viewModel.showCameraCoachOnly.value)
    }

    @Test
    fun `toggleFavoritesOnly inverts state`() {
        assertFalse(viewModel.showFavoritesOnly.value)
        viewModel.toggleFavoritesOnly()
        assertTrue(viewModel.showFavoritesOnly.value)
        viewModel.toggleFavoritesOnly()
        assertFalse(viewModel.showFavoritesOnly.value)
    }

    @Test
    fun `toggleAnimationOnly inverts state`() {
        assertFalse(viewModel.showAnimationOnly.value)
        viewModel.toggleAnimationOnly()
        assertTrue(viewModel.showAnimationOnly.value)
        viewModel.toggleAnimationOnly()
        assertFalse(viewModel.showAnimationOnly.value)
    }

    @Test
    fun `toggleCameraCoachOnly inverts state`() {
        assertFalse(viewModel.showCameraCoachOnly.value)
        viewModel.toggleCameraCoachOnly()
        assertTrue(viewModel.showCameraCoachOnly.value)
        viewModel.toggleCameraCoachOnly()
        assertFalse(viewModel.showCameraCoachOnly.value)
    }

    @Test
    fun `onCategorySelected updates filterCategory`() {
        viewModel.onCategorySelected("Chest")
        assertEquals("Chest", viewModel.filterCategory.value)
    }

    @Test
    fun `onDifficultySelected updates filterDifficulty`() {
        viewModel.onDifficultySelected("Advanced")
        assertEquals("Advanced", viewModel.filterDifficulty.value)
    }

    @Test
    fun `onEquipmentSelected updates filterEquipment`() {
        viewModel.onEquipmentSelected("Dumbbell")
        assertEquals("Dumbbell", viewModel.filterEquipment.value)
    }

    @Test
    fun `onMovementPatternSelected updates filterMovementPattern`() {
        viewModel.onMovementPatternSelected("Hinge")
        assertEquals("Hinge", viewModel.filterMovementPattern.value)
    }

    @Test
    fun `toggleFavorite inverts exercise favorite flag in repository`() = runTest {
        val exercise = sampleExercises[1] // Bodyweight Squat (isFavorite = false)
        viewModel.toggleFavorite(exercise)

        coVerify(exactly = 1) {
            repository.updateExercise(match { it.id == 2L && it.isFavorite })
        }
    }

    @Test
    fun `deleteExercise calls repository delete`() = runTest {
        val exercise = sampleExercises[0]
        viewModel.deleteExercise(exercise)

        coVerify(exactly = 1) {
            repository.deleteExercise(exercise)
        }
    }
}
