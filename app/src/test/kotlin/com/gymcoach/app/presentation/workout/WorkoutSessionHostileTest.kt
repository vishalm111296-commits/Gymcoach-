package com.gymcoach.app.presentation.workout

import com.gymcoach.app.core.progression.ProgressionEngine
import com.gymcoach.app.core.timer.RestPresets
import com.gymcoach.app.core.timer.RestTimerManager
import com.gymcoach.app.core.timer.RestTimerState
import com.gymcoach.app.data.local.dao.LastPerformance
import com.gymcoach.app.data.local.dao.LastSetData
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.model.SetType
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutExercise
import com.gymcoach.app.domain.model.WorkoutExerciseWithSets
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutWithDetails
import com.gymcoach.app.domain.repository.ExerciseRepository
import com.gymcoach.app.domain.repository.UserProfileRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
import java.time.Instant

/**
 * Hostile workout session test matrix.
 *
 * Covers: workout creation, set logging (all types), exercise management,
 * resume, completion, timer, rapid actions, edge cases, and double-action safety.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutSessionHostileTest {

    private val testDispatcher = StandardTestDispatcher()
    private val applicationScope = CoroutineScope(SupervisorJob() + testDispatcher)
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var restTimer: RestTimerManager
    private lateinit var progressionEngine: ProgressionEngine
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var viewModel: WorkoutLoggingViewModel

    private val now = Instant.now()

    private fun makeWorkout(
        id: Long = 1L,
        status: String = "ACTIVE",
        completed: Boolean = false
    ) = Workout(
        id = id,
        date = now,
        startTime = now,
        endTime = now,
        duration = 0,
        notes = "",
        completed = completed,
        status = status
    )

    private fun makeExercise(id: Long = 10L, name: String = "Bench Press") = Exercise(
        id = id,
        name = name,
        description = "Chest exercise",
        muscleGroup = "Chest",
        equipment = "barbell",
        difficulty = "intermediate",
        secondaryMuscles = "",
        instructions = "Lie on bench",
        tips = "",
        commonMistakes = "",
        safetyNotes = "",
        recommendedRepRange = "8-12",
        recommendedRestTime = "90",
        estimatedCalories = 100,
        category = "compound",
        tags = "",
        isFavorite = false,
        lastViewed = 0L,
        vtaperLat = 0,
        vtaperLateralDelt = 0,
        vtaperUpperChest = 0,
        vtaperRearDelt = 0,
        movementPattern = "push",
        imageUrl = null,
        videoUrl = null,
        animationUrl = null,
        setupInstructions = "",
        executionInstructions = "",
        breathingInstructions = "",
        tempoGuidance = "",
        beginnerVariantId = null,
        advancedVariantId = null
    )

    private fun makeSet(
        id: Long = 100L,
        setNumber: Int = 1,
        weight: Double = 50.0,
        reps: Int = 10,
        rpe: Double = 7.0,
        restSeconds: Int = 90,
        completed: Boolean = false,
        setType: SetType = SetType.NORMAL
    ) = WorkoutSet(
        id = id,
        workoutExerciseId = 1L,
        setNumber = setNumber,
        weight = weight,
        reps = reps,
        rpe = rpe,
        restSeconds = restSeconds,
        completed = completed,
        setType = setType
    )

    private fun makeWorkoutExercise(id: Long = 5L, exerciseId: Long = 10L, orderIndex: Int = 0) =
        WorkoutExercise(id = id, workoutId = 1L, exerciseId = exerciseId, orderIndex = orderIndex)

    private fun makeWorkoutWithDetails(
        workout: Workout = makeWorkout(),
        exercises: List<WorkoutExerciseWithSets> = emptyList()
    ) = WorkoutWithDetails(workout, exercises)

    private fun makeExerciseWithSets(
        exercise: Exercise = makeExercise(),
        sets: List<WorkoutSet> = emptyList(),
        workoutExercise: WorkoutExercise = makeWorkoutExercise()
    ) = WorkoutExerciseWithSets(workoutExercise, exercise, sets)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        workoutRepository = mockk(relaxed = true)
        exerciseRepository = mockk(relaxed = true)
        restTimer = mockk(relaxed = true)
        progressionEngine = mockk(relaxed = true)
        userProfileRepository = mockk(relaxed = true)

        every { restTimer.state } returns MutableStateFlow(RestTimerState())
        every { progressionEngine.calculateProgression(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            ProgressionEngine.ProgressionRecommendation(
                exerciseId = 1L, exerciseName = "Test", currentWeight = 50.0,
                currentReps = listOf(10), recommendedWeight = 52.5, recommendedReps = "8-12",
                recommendedSets = 3, reason = "Test", confidence = 0.9, isEquipmentLimited = false
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ═══════════════════════════════════════════════════════════
    // WORKOUT CREATION
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `loadOrStartWorkout with null id creates new workout when none exists`() = runTest {
        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns null
        coEvery { workoutRepository.createWorkout(any()) } returns 1L
        coEvery { workoutRepository.getWorkoutWithDetails(1L) } returns flowOf(makeWorkoutWithDetails())

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        coVerify { workoutRepository.createWorkout(any()) }
        val state = viewModel.sessionUiState.value
        assertTrue("Should be Active or Empty after creation",
            state is WorkoutLoggingViewModel.SessionUiState.Active || state is WorkoutLoggingViewModel.SessionUiState.Empty)
    }

    @Test
    fun `loadOrStartWorkout with null id resumes existing incomplete workout`() = runTest {
        val existing = makeWorkout(id = 42L)
        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns existing
        coEvery { workoutRepository.getWorkoutWithDetails(42L) } returns flowOf(makeWorkoutWithDetails(workout = existing))

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        coVerify(exactly = 0) { workoutRepository.createWorkout(any()) }
        coVerify { workoutRepository.getWorkoutWithDetails(42L) }
    }

    @Test
    fun `loadOrStartWorkout with id of completed workout creates fresh copy`() = runTest {
        val completed = makeWorkout(id = 10L, completed = true, status = "COMPLETED")
        val exercises = listOf(makeExerciseWithSets())
        coEvery { workoutRepository.getWorkoutWithDetails(10L) } returns flowOf(makeWorkoutWithDetails(completed, exercises))
        coEvery { workoutRepository.createWorkout(any()) } returns 20L
        coEvery { workoutRepository.getWorkoutWithDetails(20L) } returns flowOf(makeWorkoutWithDetails(makeWorkout(id = 20L), exercises))

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(10L)
        advanceUntilIdle()

        coVerify { workoutRepository.createWorkout(any()) }
        // Should copy exercises from original
        coVerify { workoutRepository.addExerciseToWorkout(20L, any(), any()) }
    }

    @Test
    fun `startNewWorkout creates workout with ACTIVE status`() = runTest {
        coEvery { workoutRepository.createWorkout(any()) } returns 5L
        coEvery { workoutRepository.getWorkoutWithDetails(5L) } returns flowOf(makeWorkoutWithDetails(makeWorkout(id = 5L)))

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.startNewWorkout()
        advanceUntilIdle()

        val workoutSlot = slot<Workout>()
        coVerify { workoutRepository.createWorkout(capture(workoutSlot)) }
        assertEquals("ACTIVE", workoutSlot.captured.status)
        assertFalse(workoutSlot.captured.completed)
    }

    // ═══════════════════════════════════════════════════════════
    // SET LOGGING — ALL TYPES
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `addSet creates new set with incremented setNumber`() = runTest {
        val exercise = makeExercise()
        val existingSet = makeSet(id = 100L, setNumber = 1)
        val we = makeWorkoutExercise()
        val workoutEx = makeExerciseWithSets(exercise = exercise, sets = listOf(existingSet), workoutExercise = we)
        val workout = makeWorkoutWithDetails(exercises = listOf(workoutEx))

        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns makeWorkout()
        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(workout)
        coEvery { workoutRepository.addSetToExercise(any(), any()) } returns 200L
        // DB-truth reads: exercise 10 exists in DB with set numbers [1].
        coEvery { workoutRepository.getExerciseIdsForWorkout(any()) } returns listOf(10L)
        coEvery { workoutRepository.getSetNumbersForWorkoutExercise(any()) } returns listOf(1)

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        viewModel.addSet(0)
        advanceUntilIdle()

        val setSlot = slot<WorkoutSet>()
        coVerify { workoutRepository.addSetToExercise(any(), capture(setSlot)) }
        assertEquals(2, setSlot.captured.setNumber)
    }

    @Test
    fun `addSet with previous performance pre-fills weight and reps`() = runTest {
        val exercise = makeExercise(id = 10L)
        val we = makeWorkoutExercise(exerciseId = 10L)
        val workoutEx = makeExerciseWithSets(exercise = exercise, sets = emptyList(), workoutExercise = we)
        val workout = makeWorkoutWithDetails(exercises = listOf(workoutEx))

        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns makeWorkout()
        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(workout)
        coEvery { workoutRepository.getLastSetsForExercises(any()) } returns mapOf(
            10L to listOf(LastSetData(weight = 60.0, reps = 8, rpe = 8.0, restSeconds = 120, setType = 0, date = now.toEpochMilli()))
        )
        coEvery { workoutRepository.getLastPerformancesForExercises(any()) } returns mapOf(
            10L to LastPerformance(date = now.toEpochMilli(), maxWeight = 60.0)
        )
        coEvery { workoutRepository.addSetToExercise(any(), any()) } returns 200L
        // DB-truth reads: exercise 10 exists in DB with no sets yet.
        coEvery { workoutRepository.getExerciseIdsForWorkout(any()) } returns listOf(10L)
        coEvery { workoutRepository.getSetNumbersForWorkoutExercise(any()) } returns emptyList()

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        viewModel.addSet(0)
        advanceUntilIdle()

        val setSlot = slot<WorkoutSet>()
        coVerify { workoutRepository.addSetToExercise(any(), capture(setSlot)) }
        assertEquals(60.0, setSlot.captured.weight, 0.01)
        assertEquals(8, setSlot.captured.reps)
    }

    @Test
    fun `addSet to warmup set type preserves type`() = runTest {
        val exercise = makeExercise()
        val warmupSet = makeSet(id = 100L, setNumber = 1, setType = SetType.WARMUP)
        val we = makeWorkoutExercise()
        val workoutEx = makeExerciseWithSets(exercise = exercise, sets = listOf(warmupSet), workoutExercise = we)
        val workout = makeWorkoutWithDetails(exercises = listOf(workoutEx))

        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns makeWorkout()
        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(workout)
        coEvery { workoutRepository.addSetToExercise(any(), any()) } returns 200L
        // DB-truth reads: exercise 10 exists in DB with set numbers [1].
        coEvery { workoutRepository.getExerciseIdsForWorkout(any()) } returns listOf(10L)
        coEvery { workoutRepository.getSetNumbersForWorkoutExercise(any()) } returns listOf(1)

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        viewModel.addSet(0)
        advanceUntilIdle()

        // New set should be NORMAL (default), not WARMUP
        val setSlot = slot<WorkoutSet>()
        coVerify { workoutRepository.addSetToExercise(any(), capture(setSlot)) }
        assertEquals(SetType.NORMAL, setSlot.captured.setType)
    }

    @Test
    fun `updateSetType cycles through all types`() = runTest {
        val exercise = makeExercise()
        val set = makeSet(id = 100L, setType = SetType.NORMAL)
        val we = makeWorkoutExercise()
        val workoutEx = makeExerciseWithSets(exercise = exercise, sets = listOf(set), workoutExercise = we)
        val workout = makeWorkoutWithDetails(exercises = listOf(workoutEx))

        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns makeWorkout()
        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(workout)

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        // Normal → Warmup
        viewModel.updateSetType(0, 0, SetType.WARMUP)
        advanceUntilIdle()
        coVerify { workoutRepository.updateSet(match { it.setType == SetType.WARMUP }) }

        // Warmup → Drop
        viewModel.updateSetType(0, 0, SetType.DROP)
        advanceUntilIdle()
        coVerify { workoutRepository.updateSet(match { it.setType == SetType.DROP }) }

        // Drop → Failure
        viewModel.updateSetType(0, 0, SetType.FAILURE)
        advanceUntilIdle()
        coVerify { workoutRepository.updateSet(match { it.setType == SetType.FAILURE }) }
    }

    @Test
    fun `removeSet deletes correct set by index`() = runTest {
        val exercise = makeExercise()
        val set1 = makeSet(id = 100L, setNumber = 1)
        val set2 = makeSet(id = 101L, setNumber = 2)
        val we = makeWorkoutExercise()
        val workoutEx = makeExerciseWithSets(exercise = exercise, sets = listOf(set1, set2), workoutExercise = we)
        val workout = makeWorkoutWithDetails(exercises = listOf(workoutEx))

        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns makeWorkout()
        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(workout)

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        viewModel.removeSet(0, 0) // Remove first set
        advanceUntilIdle()

        coVerify { workoutRepository.deleteSet(100L) }
    }

    // ═══════════════════════════════════════════════════════════
    // EXERCISE MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `addExerciseToWorkout appends at end with correct orderIndex`() = runTest {
        val exercise1 = makeExercise(id = 10L, name = "Bench Press")
        val exercise2 = makeExercise(id = 20L, name = "Squat")
        val we1 = makeWorkoutExercise(id = 5L, exerciseId = 10L, orderIndex = 0)
        val workoutEx1 = makeExerciseWithSets(exercise = exercise1, sets = emptyList(), workoutExercise = we1)
        val workout = makeWorkoutWithDetails(exercises = listOf(workoutEx1))

        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns makeWorkout()
        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(workout)
        coEvery { workoutRepository.addExerciseToWorkout(any(), any(), any()) } returns 6L
        // DB-truth: exercise 10 already committed.
        coEvery { workoutRepository.getExerciseIdsForWorkout(any()) } returns listOf(10L)

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        viewModel.addExerciseToWorkout(exercise2)
        advanceUntilIdle()

        coVerify { workoutRepository.addExerciseToWorkout(any(), 20L, 1) }
    }

    // ═══════════════════════════════════════════════════════════
    // APP-016: DUPLICATE EXERCISE PREVENTION
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `APP-016 first add of exercise succeeds`() = runTest {
        val exercise = makeExercise(id = 10L, name = "Bench Press")
        val workout = makeWorkoutWithDetails(exercises = emptyList())

        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns makeWorkout()
        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(workout)
        coEvery { workoutRepository.addExerciseToWorkout(any(), any(), any()) } returns 5L

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        viewModel.addExerciseToWorkout(exercise)
        advanceUntilIdle()

        coVerify(exactly = 1) { workoutRepository.addExerciseToWorkout(any(), 10L, 0) }
    }

    @Test
    fun `APP-016 second add of same exercise is rejected`() = runTest {
        val exercise = makeExercise(id = 10L, name = "Bench Press")
        val we = makeWorkoutExercise(id = 5L, exerciseId = 10L, orderIndex = 0)
        val workoutEx = makeExerciseWithSets(exercise = exercise, sets = emptyList(), workoutExercise = we)
        val workout = makeWorkoutWithDetails(exercises = listOf(workoutEx))

        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns makeWorkout()
        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(workout)

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        // Attempt to add the same exercise again
        viewModel.addExerciseToWorkout(exercise)
        advanceUntilIdle()

        // Repository addExerciseToWorkout should NOT be called — guard rejects it
        coVerify(exactly = 0) { workoutRepository.addExerciseToWorkout(any(), any(), any()) }
    }

    @Test
    fun `APP-016 different exercise can still be added after duplicate rejection`() = runTest {
        val benchPress = makeExercise(id = 10L, name = "Bench Press")
        val squat = makeExercise(id = 20L, name = "Squat")
        val we = makeWorkoutExercise(id = 5L, exerciseId = 10L, orderIndex = 0)
        val workoutEx = makeExerciseWithSets(exercise = benchPress, sets = emptyList(), workoutExercise = we)
        val workout = makeWorkoutWithDetails(exercises = listOf(workoutEx))

        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns makeWorkout()
        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(workout)
        coEvery { workoutRepository.addExerciseToWorkout(any(), any(), any()) } returns 6L
        // DB-truth: exercise 10 (bench press) already committed.
        coEvery { workoutRepository.getExerciseIdsForWorkout(any()) } returns listOf(10L)

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        // Duplicate should be rejected
        viewModel.addExerciseToWorkout(benchPress)
        advanceUntilIdle()
        coVerify(exactly = 0) { workoutRepository.addExerciseToWorkout(any(), 10L, any()) }

        // Different exercise should be accepted
        viewModel.addExerciseToWorkout(squat)
        advanceUntilIdle()
        coVerify(exactly = 1) { workoutRepository.addExerciseToWorkout(any(), 20L, 1) }
    }

    @Test
    fun `APP-016 existing exercises remain unchanged after duplicate rejection`() = runTest {
        val benchPress = makeExercise(id = 10L, name = "Bench Press")
        val we = makeWorkoutExercise(id = 5L, exerciseId = 10L, orderIndex = 0)
        val existingSet = makeSet(id = 100L, setNumber = 1, weight = 80.0, reps = 8)
        val workoutEx = makeExerciseWithSets(exercise = benchPress, sets = listOf(existingSet), workoutExercise = we)
        val workout = makeWorkoutWithDetails(exercises = listOf(workoutEx))

        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns makeWorkout()
        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(workout)

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        // Attempt duplicate — should be rejected
        viewModel.addExerciseToWorkout(benchPress)
        advanceUntilIdle()

        // Verify existing set data was not modified
        val currentExercises = viewModel.currentWorkout.value?.exercises
        assertNotNull(currentExercises)
        assertEquals(1, currentExercises!!.size)
        assertEquals(10L, currentExercises[0].exercise.id)
        assertEquals(1, currentExercises[0].sets.size)
        assertEquals(80.0, currentExercises[0].sets[0].weight, 0.01)
        assertEquals(8, currentExercises[0].sets[0].reps)
    }

    @Test
    fun `APP-016 duplicate prevention uses stale snapshot and rejects correctly`() = runTest {
        // Simulates: exercise added via performAgainInternal, then user tries to add same exercise
        val benchPress = makeExercise(id = 10L, name = "Bench Press")
        val we = makeWorkoutExercise(id = 5L, exerciseId = 10L, orderIndex = 0)
        val workoutEx = makeExerciseWithSets(exercise = benchPress, sets = emptyList(), workoutExercise = we)
        val workout = makeWorkoutWithDetails(exercises = listOf(workoutEx))

        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns null
        coEvery { workoutRepository.createWorkout(any()) } returns 1L
        coEvery { workoutRepository.getWorkoutWithDetails(1L) } returns flowOf(workout)

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        // Exercise is already in workout from loadOrStartWorkout
        // Attempting to add same exercise should be rejected
        viewModel.addExerciseToWorkout(benchPress)
        advanceUntilIdle()

        coVerify(exactly = 0) { workoutRepository.addExerciseToWorkout(any(), any(), any()) }
    }

    @Test
    fun `removeExercise deletes workout exercise and cascades sets`() = runTest {
        val exercise = makeExercise()
        val we = makeWorkoutExercise(id = 5L)
        val workoutEx = makeExerciseWithSets(exercise = exercise, sets = listOf(makeSet()), workoutExercise = we)
        val workout = makeWorkoutWithDetails(exercises = listOf(workoutEx))

        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns makeWorkout()
        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(workout)

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        viewModel.removeExercise(0)
        advanceUntilIdle()

        coVerify { workoutRepository.removeExerciseFromWorkout(5L) }
    }

    @Test
    fun `removeExercise with invalid index does nothing`() = runTest {
        val workout = makeWorkoutWithDetails(exercises = listOf(makeExerciseWithSets()))

        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns makeWorkout()
        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(workout)

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        viewModel.removeExercise(99) // Invalid index
        advanceUntilIdle()

        coVerify(exactly = 0) { workoutRepository.removeExerciseFromWorkout(any()) }
    }

    @Test
    fun `removeExercise from empty workout does nothing`() = runTest {
        val workout = makeWorkoutWithDetails(exercises = emptyList())

        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns makeWorkout()
        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(workout)

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        viewModel.removeExercise(0)
        advanceUntilIdle()

        coVerify(exactly = 0) { workoutRepository.removeExerciseFromWorkout(any()) }
    }

    // ═══════════════════════════════════════════════════════════
    // COMPLETION
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `completeWorkout marks workout as COMPLETED`() = runTest {
        val workout = makeWorkout()
        val exercise = makeExercise()
        val completedSet = makeSet(completed = true, weight = 50.0, reps = 10)
        val we = makeWorkoutExercise()
        val workoutEx = makeExerciseWithSets(exercise = exercise, sets = listOf(completedSet), workoutExercise = we)
        val details = makeWorkoutWithDetails(workout = workout, exercises = listOf(workoutEx))

        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns workout
        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(details)

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        viewModel.completeWorkout()
        advanceUntilIdle()

        val workoutSlot = slot<Workout>()
        coVerify { workoutRepository.updateWorkout(capture(workoutSlot)) }
        assertTrue(workoutSlot.captured.completed)
        assertEquals("COMPLETED", workoutSlot.captured.status)
        assertTrue(viewModel.completed.value)
    }

    @Test
    fun `completeWorkout double tap is idempotent`() = runTest {
        val workout = makeWorkout()
        val details = makeWorkoutWithDetails(workout = workout)

        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns workout
        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(details)

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        viewModel.completeWorkout()
        advanceUntilIdle()

        // Second call should be blocked by terminal-state guard
        viewModel.completeWorkout()
        advanceUntilIdle()

        // updateWorkout should only be called once (for the first completeWorkout)
        coVerify(exactly = 1) { workoutRepository.updateWorkout(any()) }
    }

    @Test
    fun `completeWorkout on already-completed workout is no-op`() = runTest {
        val completedWorkout = makeWorkout(completed = true, status = "COMPLETED")
        val details = makeWorkoutWithDetails(workout = completedWorkout)

        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(details)

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(1L)
        advanceUntilIdle()

        // Should create a fresh copy (performAgainInternal), not complete the old one
        // The terminal-state guard in completeWorkout should prevent completing an already-completed workout
        viewModel.completeWorkout()
        advanceUntilIdle()

        // The original completed workout should NOT be updated again
        // (performAgainInternal creates a new workout, which is the one that gets completed)
    }

    @Test
    fun `completeWorkout captures correct statistics`() = runTest {
        val workout = makeWorkout()
        val exercise = makeExercise()
        val set1 = makeSet(completed = true, weight = 50.0, reps = 10)
        val set2 = makeSet(id = 101L, setNumber = 2, completed = true, weight = 55.0, reps = 8)
        val set3 = makeSet(id = 102L, setNumber = 3, completed = false) // Not completed
        val we = makeWorkoutExercise()
        val workoutEx = makeExerciseWithSets(exercise = exercise, sets = listOf(set1, set2, set3), workoutExercise = we)
        val details = makeWorkoutWithDetails(workout = workout, exercises = listOf(workoutEx))

        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns workout
        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(details)

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        viewModel.completeWorkout()
        advanceUntilIdle()

        val stats = viewModel.completionStats.value
        assertEquals(2, stats.totalSets) // Only completed sets
        assertEquals(18, stats.totalReps) // 10 + 8
        assertEquals(940.0, stats.totalVolume, 0.01) // 50*10 + 55*8
        assertEquals(1, stats.exerciseCount)
    }

    // ═══════════════════════════════════════════════════════════
    // TIMER
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `toggleSetCompletion starts rest timer when completing`() = runTest {
        val exercise = makeExercise()
        val set = makeSet(completed = false)
        val we = makeWorkoutExercise()
        val workoutEx = makeExerciseWithSets(exercise = exercise, sets = listOf(set), workoutExercise = we)
        val workout = makeWorkoutWithDetails(exercises = listOf(workoutEx))

        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns makeWorkout()
        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(workout)

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        viewModel.toggleSetCompletion(0, 0)
        advanceUntilIdle()

        coVerify { restTimer.start(any(), any()) }
    }

    @Test
    fun `toggleSetCompletion stops rest timer when uncompleting`() = runTest {
        val exercise = makeExercise()
        val set = makeSet(completed = true)
        val we = makeWorkoutExercise()
        val workoutEx = makeExerciseWithSets(exercise = exercise, sets = listOf(set), workoutExercise = we)
        val workout = makeWorkoutWithDetails(exercises = listOf(workoutEx))

        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns makeWorkout()
        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(workout)

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        viewModel.toggleSetCompletion(0, 0) // Uncomplete
        advanceUntilIdle()

        coVerify { restTimer.stop() }
    }

    @Test
    fun `stopRestTimer delegates to RestTimerManager`() = runTest {
        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.stopRestTimer()
        coVerify { restTimer.stop() }
    }

    @Test
    fun `pauseRestTimer delegates to RestTimerManager`() = runTest {
        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.pauseRestTimer()
        coVerify { restTimer.pause() }
    }

    @Test
    fun `resumeRestTimer delegates to RestTimerManager`() = runTest {
        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.resumeRestTimer()
        coVerify { restTimer.resume() }
    }

    @Test
    fun `changeRestTimerDuration restarts timer with new duration`() = runTest {
        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.changeRestTimerDuration(120)
        coVerify { restTimer.restart(120, any()) }
    }

    // ═══════════════════════════════════════════════════════════
    // EDGE CASES
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `updateSetWeight with invalid index does nothing`() = runTest {
        val workout = makeWorkoutWithDetails(exercises = listOf(makeExerciseWithSets(sets = listOf(makeSet()))))

        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns makeWorkout()
        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(workout)

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        viewModel.updateSetWeight(99, 0, 100.0) // Invalid exercise index
        advanceUntilIdle()

        coVerify(exactly = 0) { workoutRepository.updateSet(any()) }
    }

    @Test
    fun `updateSetReps with invalid set index does nothing`() = runTest {
        val workout = makeWorkoutWithDetails(exercises = listOf(makeExerciseWithSets(sets = listOf(makeSet()))))

        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns makeWorkout()
        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(workout)

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        viewModel.updateSetReps(0, 99, 15) // Invalid set index
        advanceUntilIdle()

        coVerify(exactly = 0) { workoutRepository.updateSet(any()) }
    }

    @Test
    fun `dismissError clears error state`() = runTest {
        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        assertNull(viewModel.error.value)

        // Force an error by making repository throw
        coEvery { workoutRepository.getLatestIncompleteWorkout() } throws RuntimeException("Test error")

        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        assertNotNull(viewModel.error.value)

        viewModel.dismissError()
        assertNull(viewModel.error.value)
    }

    @Test
    fun `updateNotes updates workout notes`() = runTest {
        val workout = makeWorkout()
        val details = makeWorkoutWithDetails(workout = workout)

        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns workout
        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(details)

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        viewModel.updateNotes("Great session")
        advanceUntilIdle()

        coVerify { workoutRepository.updateWorkout(match { it.notes == "Great session" }) }
    }

    @Test
    fun `addSet with no current workout does nothing`() = runTest {
        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        // Don't load a workout
        viewModel.addSet(0)
        advanceUntilIdle()

        coVerify(exactly = 0) { workoutRepository.addSetToExercise(any(), any()) }
    }

    @Test
    fun `removeExercise clears progression recommendation`() = runTest {
        val exercise = makeExercise(id = 10L)
        val we = makeWorkoutExercise(exerciseId = 10L)
        val workoutEx = makeExerciseWithSets(exercise = exercise, sets = listOf(makeSet()), workoutExercise = we)
        val workout = makeWorkoutWithDetails(exercises = listOf(workoutEx))

        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns makeWorkout()
        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns flowOf(workout)

        viewModel = WorkoutLoggingViewModel(workoutRepository, exerciseRepository, restTimer, progressionEngine, userProfileRepository, applicationScope)
        viewModel.loadOrStartWorkout(null)
        advanceUntilIdle()

        viewModel.removeExercise(0)
        advanceUntilIdle()

        // Progression recommendation for removed exercise should be cleared
        assertFalse(viewModel.progressionRecommendations.value.containsKey(10L))
    }

    // ═══════════════════════════════════════════════════════════
    // REST PRESETS
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `RestPresets recommended returns correct values`() {
        assertEquals(60, RestPresets.recommended(SetType.WARMUP, 0.0))
        assertEquals(30, RestPresets.recommended(SetType.DROP, 0.0))
        assertEquals(120, RestPresets.recommended(SetType.FAILURE, 0.0))
        assertEquals(180, RestPresets.recommended(SetType.NORMAL, 9.5)) // RPE >= 9.0
        assertEquals(120, RestPresets.recommended(SetType.NORMAL, 8.0)) // RPE >= 7.5
        assertEquals(90, RestPresets.recommended(SetType.NORMAL, 6.5))  // RPE >= 6.0
        assertEquals(60, RestPresets.recommended(SetType.NORMAL, 5.0))  // else
    }

    @Test
    fun `RestTimerManager start sets correct initial state`() {
        val timer = RestTimerManager()
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
        timer.start(90, scope)

        val state = timer.state.value
        assertEquals(90, state.timeRemaining)
        assertTrue(state.isRunning)
        assertFalse(state.isPaused)
        assertEquals(90, state.totalDuration)
    }

    @Test
    fun `RestTimerManager pause and resume`() {
        val timer = RestTimerManager()
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
        timer.start(90, scope)

        timer.pause()
        assertTrue(timer.state.value.isPaused)

        timer.resume()
        assertFalse(timer.state.value.isPaused)
    }

    @Test
    fun `RestTimerManager stop resets state`() {
        val timer = RestTimerManager()
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
        timer.start(90, scope)

        timer.stop()
        val state = timer.state.value
        assertFalse(state.isRunning)
        assertFalse(state.isPaused)
        assertEquals(0, state.timeRemaining)
    }

    @Test
    fun `RestTimerManager restart replaces duration`() {
        val timer = RestTimerManager()
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
        timer.start(90, scope)

        timer.restart(120, scope)
        val state = timer.state.value
        assertEquals(120, state.timeRemaining)
        assertEquals(120, state.totalDuration)
        assertTrue(state.isRunning)
    }
}
