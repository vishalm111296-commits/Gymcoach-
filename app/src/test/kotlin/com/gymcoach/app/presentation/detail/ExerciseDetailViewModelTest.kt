package com.gymcoach.app.presentation.detail

import com.gymcoach.app.core.animation.AnimationRepository
import com.gymcoach.app.core.animation.ExerciseAnimationDefinition
import com.gymcoach.app.core.exercise.SubstitutionEngine
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.repository.ExerciseRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: ExerciseRepository
    private lateinit var substitutionEngine: SubstitutionEngine
    private lateinit var animationRepository: AnimationRepository
    private lateinit var viewModel: ExerciseDetailViewModel

    private val sampleExercise = Exercise(
        id = 42L,
        name = "Incline Dumbbell Press",
        description = "Upper chest pressing movement",
        muscleGroup = "Chest",
        equipment = "Dumbbell",
        difficulty = "Intermediate",
        videoUrl = "https://example.com/video.mp4",
        animationUrl = "incline_db_press",
        isFavorite = false
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        substitutionEngine = mockk(relaxed = true)
        animationRepository = mockk(relaxed = true)

        every { repository.getExerciseById(42L) } returns flowOf(sampleExercise)
        coEvery { substitutionEngine.findSubstitutes(any(), any(), any()) } returns emptyList()
        coEvery { animationRepository.getAnimation(any(), any()) } returns mockk<ExerciseAnimationDefinition>(relaxed = true)

        viewModel = ExerciseDetailViewModel(repository, substitutionEngine, animationRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadExercise populates exercise, favorite status, and animation`() = runTest {
        viewModel.loadExercise(42L)

        val exercise = viewModel.exercise.value
        assertNotNull(exercise)
        assertEquals(42L, exercise?.id)
        assertEquals("Incline Dumbbell Press", exercise?.name)
        assertEquals("https://example.com/video.mp4", exercise?.videoUrl)
        assertFalse(viewModel.isFavorite.value)
        assertNotNull(viewModel.animationDefinition.value)
    }

    @Test
    fun `toggleFavorite inverts favorite status and persists to repository`() = runTest {
        viewModel.loadExercise(42L)
        assertFalse(viewModel.isFavorite.value)

        coEvery { repository.updateExercise(any()) } returns Unit

        viewModel.toggleFavorite()

        assertTrue(viewModel.isFavorite.value)
        coVerify(exactly = 1) {
            repository.updateExercise(match { it.id == 42L && it.isFavorite })
        }
    }
}
