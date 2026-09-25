package com.gymcoach.app.domain.model

import com.gymcoach.app.core.progression.ProgressionEngine
import com.gymcoach.app.core.timer.RestTimerManager
import com.gymcoach.app.domain.repository.ExerciseRepository
import com.gymcoach.app.domain.repository.UserProfileRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import com.gymcoach.app.presentation.workout.WorkoutLoggingViewModel
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
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
class SupersetGroupTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `paired exercise indexing and rotation works correctly`() {
        // Pair: 0 -> 1 -> 0
        val pair = SupersetGroup(
            id = "SS_1",
            label = "Superset A",
            exerciseIndices = listOf(0, 1),
            transitionRestSeconds = 30,
            roundRestSeconds = 90
        )

        assertEquals(1, pair.getNextExerciseIndex(0))
        assertEquals(0, pair.getNextExerciseIndex(1))
        assertNull(pair.getNextExerciseIndex(2))

        // Tri-set: 0 -> 1 -> 2 -> 0
        val triSet = SupersetGroup(
            id = "SS_2",
            label = "Tri-set A",
            exerciseIndices = listOf(0, 1, 2)
        )
        assertEquals(1, triSet.getNextExerciseIndex(0))
        assertEquals(2, triSet.getNextExerciseIndex(1))
        assertEquals(0, triSet.getNextExerciseIndex(2))
    }

    @Test
    fun `end of round detection works correctly`() {
        val pair = SupersetGroup(
            id = "SS_1",
            label = "Superset A",
            exerciseIndices = listOf(0, 1)
        )

        assertFalse("A1 is transition, not end of round", pair.isEndOfRound(0))
        assertTrue("A2 is end of round", pair.isEndOfRound(1))
        assertFalse("Exercise not in group is not end of round", pair.isEndOfRound(2))
    }

    @Test
    fun `recommended rest seconds returns transition rest vs round rest`() {
        val pair = SupersetGroup(
            id = "SS_1",
            label = "Superset A",
            exerciseIndices = listOf(0, 1),
            transitionRestSeconds = 30,
            roundRestSeconds = 90
        )

        assertEquals(30, pair.getRecommendedRestSeconds(0))
        assertEquals(90, pair.getRecommendedRestSeconds(1))

        // Custom rests
        val customPair = SupersetGroup(
            id = "SS_2",
            label = "Superset B",
            exerciseIndices = listOf(3, 4),
            transitionRestSeconds = 45,
            roundRestSeconds = 120
        )
        assertEquals(45, customPair.getRecommendedRestSeconds(3))
        assertEquals(120, customPair.getRecommendedRestSeconds(4))
    }

    @Test
    fun `viewModel linking and unlinking manages superset groups properly`() {
        val workoutRepository = mockk<WorkoutRepository>(relaxed = true)
        val exerciseRepository = mockk<ExerciseRepository>(relaxed = true)
        val userProfileRepository = mockk<UserProfileRepository>(relaxed = true)
        val progressionEngine = mockk<ProgressionEngine>(relaxed = true)
        val restTimer = mockk<RestTimerManager>(relaxed = true)

        val viewModel = WorkoutLoggingViewModel(
            workoutRepository,
            exerciseRepository,
            restTimer,
            progressionEngine,
            userProfileRepository
        )

        try {
            // Initially empty
            assertTrue(viewModel.supersetGroups.value.isEmpty())
            assertNull(viewModel.getSupersetForExercise(0))

            // Link 0 and 1
            viewModel.linkExercisesAsSuperset(0, 1)
            assertEquals(1, viewModel.supersetGroups.value.size)

            val groupA = viewModel.getSupersetForExercise(0)
            assertNotNull(groupA)
            assertEquals("Superset A", groupA?.label)
            assertEquals(listOf(0, 1), groupA?.exerciseIndices)
            assertEquals(groupA, viewModel.getSupersetForExercise(1))
            assertNull(viewModel.getSupersetForExercise(2))

            // Link 3 and 2 (verify sorted order and label 'Superset B')
            viewModel.linkExercisesAsSuperset(3, 2)
            assertEquals(2, viewModel.supersetGroups.value.size)

            val groupB = viewModel.getSupersetForExercise(2)
            assertNotNull(groupB)
            assertEquals("Superset B", groupB?.label)
            assertEquals(listOf(2, 3), groupB?.exerciseIndices)

            // Re-linking an exercise replaces the old group containing it
            viewModel.linkExercisesAsSuperset(1, 4)
            assertNull(viewModel.getSupersetForExercise(0)) // 0 was in group with 1, which got removed
            val newGroup = viewModel.getSupersetForExercise(1)
            assertNotNull(newGroup)
            assertEquals(listOf(1, 4), newGroup?.exerciseIndices)

            // Unlink exercise
            viewModel.unlinkSuperset(1)
            assertNull(viewModel.getSupersetForExercise(1))
            assertNull(viewModel.getSupersetForExercise(4))
            // Group B should still be intact
            assertNotNull(viewModel.getSupersetForExercise(2))
            assertNotNull(viewModel.getSupersetForExercise(3))
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun `logSet triggers superset transition and round rest timers`() = runTest {
        val workoutRepository = mockk<WorkoutRepository>(relaxed = true)
        val exerciseRepository = mockk<ExerciseRepository>(relaxed = true)
        val userProfileRepository = mockk<UserProfileRepository>(relaxed = true)
        val progressionEngine = mockk<ProgressionEngine>(relaxed = true)
        val restTimer = mockk<RestTimerManager>(relaxed = true)

        val now = Instant.now()
        val sampleWorkout = Workout(
            id = 42L,
            date = now,
            startTime = now,
            endTime = now,
            duration = 0,
            completed = false,
            status = "ACTIVE",
            notes = ""
        )
        val details = WorkoutWithDetails(
            workout = sampleWorkout,
            exercises = listOf(
                WorkoutExerciseWithSets(
                    workoutExercise = WorkoutExercise(id = 1L, workoutId = 42L, exerciseId = 101L, orderIndex = 0),
                    exercise = Exercise(id = 101L, name = "Bench Press", description = "", muscleGroup = "Chest", equipment = "Barbell", difficulty = "Intermediate"),
                    sets = listOf(
                        WorkoutSet(id = 10L, workoutExerciseId = 1L, setNumber = 1, weight = 80.0, reps = 8, rpe = 8.0, restSeconds = 90, completed = false, setType = SetType.NORMAL),
                        WorkoutSet(id = 11L, workoutExerciseId = 1L, setNumber = 2, weight = 80.0, reps = 8, rpe = 8.0, restSeconds = 90, completed = false, setType = SetType.NORMAL)
                    )
                ),
                WorkoutExerciseWithSets(
                    workoutExercise = WorkoutExercise(id = 2L, workoutId = 42L, exerciseId = 102L, orderIndex = 1),
                    exercise = Exercise(id = 102L, name = "Barbell Row", description = "", muscleGroup = "Back", equipment = "Barbell", difficulty = "Intermediate"),
                    sets = listOf(
                        WorkoutSet(id = 20L, workoutExerciseId = 2L, setNumber = 1, weight = 70.0, reps = 8, rpe = 8.0, restSeconds = 90, completed = false, setType = SetType.NORMAL),
                        WorkoutSet(id = 21L, workoutExerciseId = 2L, setNumber = 2, weight = 70.0, reps = 8, rpe = 8.0, restSeconds = 90, completed = false, setType = SetType.NORMAL)
                    )
                )
            )
        )

        coEvery { exerciseRepository.getAllExercises() } returns flowOf(emptyList())
        coEvery { workoutRepository.getWorkoutWithDetails(42L) } returns flowOf(details)

        val viewModel = WorkoutLoggingViewModel(
            workoutRepository,
            exerciseRepository,
            restTimer,
            progressionEngine,
            userProfileRepository
        ).apply { enableWorkoutTimer = false }

        try {
            viewModel.loadOrStartWorkout(42L)
            viewModel.linkExercisesAsSuperset(0, 1)

            // Logging set on exercise 0 (transition within pair) -> should use transitionRestSeconds = 30
            viewModel.logSet(exerciseIndex = 0, setIndex = 0)
            verify(exactly = 1) {
                restTimer.start(
                    seconds = 30,
                    scope = any(),
                    nextSet = "Barbell Row Set 1",
                    workoutId = 42L
                )
            }

            // Logging set on exercise 1 (end of round within pair) -> should use roundRestSeconds = 90
            viewModel.logSet(exerciseIndex = 1, setIndex = 0)
            verify(exactly = 1) {
                restTimer.start(
                    seconds = 90,
                    scope = any(),
                    nextSet = any(),
                    workoutId = 42L
                )
            }
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun `empty exerciseIndices handles indexing and round detection safely`() {
        val emptyGroup = SupersetGroup(
            id = "SS_EMPTY",
            label = "Empty Superset",
            exerciseIndices = emptyList(),
            transitionRestSeconds = 30,
            roundRestSeconds = 90
        )
        assertNull(emptyGroup.getNextExerciseIndex(0))
        assertFalse(emptyGroup.isEndOfRound(0))
        assertEquals(30, emptyGroup.getRecommendedRestSeconds(0))
    }

    @Test
    fun `single exercise group loops back to itself and treats every set as end of round`() {
        val singleGroup = SupersetGroup(
            id = "SS_SINGLE",
            label = "Single Circuit",
            exerciseIndices = listOf(5),
            transitionRestSeconds = 15,
            roundRestSeconds = 75
        )
        assertEquals(5, singleGroup.getNextExerciseIndex(5))
        assertNull(singleGroup.getNextExerciseIndex(0))
        assertTrue(singleGroup.isEndOfRound(5))
        assertEquals(75, singleGroup.getRecommendedRestSeconds(5))
    }

    @Test
    fun `quad-set giant set handles 4-station circular indexing and rest intervals`() {
        val quadSet = SupersetGroup(
            id = "SS_GIANT",
            label = "Giant Set A",
            exerciseIndices = listOf(10, 20, 30, 40),
            transitionRestSeconds = 20,
            roundRestSeconds = 120
        )
        assertEquals(20, quadSet.getNextExerciseIndex(10))
        assertEquals(30, quadSet.getNextExerciseIndex(20))
        assertEquals(40, quadSet.getNextExerciseIndex(30))
        assertEquals(10, quadSet.getNextExerciseIndex(40))

        assertFalse(quadSet.isEndOfRound(10))
        assertFalse(quadSet.isEndOfRound(20))
        assertFalse(quadSet.isEndOfRound(30))
        assertTrue(quadSet.isEndOfRound(40))

        assertEquals(20, quadSet.getRecommendedRestSeconds(10))
        assertEquals(20, quadSet.getRecommendedRestSeconds(20))
        assertEquals(20, quadSet.getRecommendedRestSeconds(30))
        assertEquals(120, quadSet.getRecommendedRestSeconds(40))
    }
}
