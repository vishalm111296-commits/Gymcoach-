package com.gymcoach.app.presentation.workout

import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.progression.ProgressionEngine
import com.gymcoach.app.core.timer.RestTimerManager
import com.gymcoach.app.domain.model.Exercise
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Concurrency regression tests for WorkoutLoggingViewModel.
 *
 * Covers:
 * - Workout creation race (Mutex serializes check-then-create)
 * - APP-016: concurrent addExerciseToWorkout (Mutex prevents duplicate inserts)
 * - removeExercise: stable ID (workoutExerciseId captured at call site)
 * - addSet: concurrent addSet (Mutex prevents duplicate setNumbers)
 * - completeWorkout: AtomicBoolean.compareAndSet (truly atomic admission)
 * - completeWorkout: durability (DB write before UI terminal state)
 * - Flow collector: duplicate loadOrStartWorkout(null) regression
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutConcurrencyTest {

    private val testDispatcher = StandardTestDispatcher()
    private val applicationScope = CoroutineScope(SupervisorJob() + testDispatcher)
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var restTimer: RestTimerManager
    private lateinit var progressionEngine: ProgressionEngine
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var viewModel: WorkoutLoggingViewModel

    private val now = Instant.now()

    private fun vmRunTest(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit): kotlinx.coroutines.test.TestResult =
        runTest {
            try {
                block()
            } finally {
                if (::viewModel.isInitialized) viewModel.viewModelScope.cancel()
            }
        }

    private fun makeWorkout(
        id: Long = 1L, status: String = "ACTIVE", completed: Boolean = false
    ) = Workout(
        id = id, date = now, startTime = now, endTime = now,
        duration = 0, notes = "", completed = completed, status = status
    )

    private fun makeExercise(id: Long = 10L, name: String = "Bench Press") = Exercise(
        id = id, name = name, description = "", muscleGroup = "Chest",
        equipment = "barbell", difficulty = "intermediate", secondaryMuscles = "",
        instructions = "", tips = "", commonMistakes = "", safetyNotes = "",
        recommendedRepRange = "8-12", recommendedRestTime = "90",
        estimatedCalories = 100, category = "compound", tags = "",
        isFavorite = false, lastViewed = 0L, vtaperLat = 0,
        vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
        movementPattern = "push", imageUrl = null, videoUrl = null,
        animationUrl = null, setupInstructions = "", executionInstructions = "",
        breathingInstructions = "", tempoGuidance = "",
        beginnerVariantId = null, advancedVariantId = null
    )

    private fun makeSet(
        id: Long = 100L, setNumber: Int = 1, weight: Double = 50.0,
        reps: Int = 10, completed: Boolean = false
    ) = WorkoutSet(
        id = id, workoutExerciseId = 200L, setNumber = setNumber,
        weight = weight, reps = reps, rpe = 7.0, restSeconds = 90,
        completed = completed
    )

    private fun makeWorkoutExercise(
        id: Long = 200L, workoutId: Long = 1L, exerciseId: Long = 10L,
        orderIndex: Int = 0
    ) = WorkoutExercise(id = id, workoutId = workoutId, exerciseId = exerciseId, orderIndex = orderIndex)

    private fun makeWorkoutWithDetails(
        workout: Workout = makeWorkout(), exercises: List<WorkoutExerciseWithSets> = emptyList()
    ) = WorkoutWithDetails(workout, exercises)

    private fun createViewModel(workoutDetails: WorkoutWithDetails): WorkoutLoggingViewModel {
        val workoutFlow = MutableStateFlow(workoutDetails)
        coEvery { workoutRepository.getWorkoutWithDetails(any()) } returns workoutFlow
        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns workoutDetails.workout
        coEvery { workoutRepository.addExerciseToWorkout(any(), any(), any()) } returns 300L
        coEvery { workoutRepository.addSetToExercise(any(), any()) } returns 400L
        // DB-truth reads (default: empty; individual tests override with callCount).
        coEvery { workoutRepository.getExerciseIdsForWorkout(any()) } returns emptyList()
        coEvery { workoutRepository.getSetNumbersForWorkoutExercise(any()) } returns emptyList()
        coEvery { workoutRepository.getLastSetsForExercise(any()) } returns emptyList()
        coEvery { workoutRepository.getLastPerformanceForExercise(any()) } returns null
        coEvery { workoutRepository.getLastSetsForExercises(any()) } returns emptyMap()
        coEvery { workoutRepository.getLastPerformancesForExercises(any()) } returns emptyMap()
        coEvery { workoutRepository.updateSet(any()) } returns Unit
        coEvery { workoutRepository.deleteSet(any()) } returns Unit
        coEvery { workoutRepository.removeExerciseFromWorkout(any()) } returns Unit
        coEvery { workoutRepository.updateWorkout(any()) } returns Unit
        coEvery { userProfileRepository.getLatestProfile() } returns MutableStateFlow(null)
        every { restTimer.state } returns MutableStateFlow(
            com.gymcoach.app.core.timer.RestTimerState()
        )
        return WorkoutLoggingViewModel(
            workoutRepository, exerciseRepository, restTimer,
            progressionEngine, userProfileRepository, applicationScope
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        workoutRepository = mockk(relaxed = true)
        exerciseRepository = mockk(relaxed = true)
        restTimer = mockk(relaxed = true)
        progressionEngine = mockk(relaxed = true)
        userProfileRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ═══════════════════════════════════════════════════════════════════
    // 1. WORKOUT CREATION RACE
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Two sequential loadOrStartWorkout(null) calls must not create
     * two ACTIVE workouts.
     *
     * First call: no existing → creates workout ID 1
     * Second call: finds existing (ID 1) → resumes it, does not create
     */
    @Test
    fun twoSequential_starts_createsOnlyOneWorkout() = vmRunTest {
        val workoutDetails = makeWorkoutWithDetails(
            workout = makeWorkout(id = 1L), exercises = emptyList()
        )
        viewModel = createViewModel(workoutDetails)

        // Mock: first call returns null (no existing), second call returns the created workout.
        var callCount = 0
        coEvery { workoutRepository.getLatestIncompleteWorkout() } coAnswers {
            callCount++
            if (callCount <= 1) null else workoutDetails.workout
        }
        coEvery { workoutRepository.createWorkout(any()) } returns 1L

        viewModel.loadOrStartWorkout()
        runCurrent()
        viewModel.loadOrStartWorkout()
        runCurrent()

        // createWorkout should be called exactly once.
        coVerify(exactly = 1) { workoutRepository.createWorkout(any()) }
    }

    /**
     * Two concurrent loadOrStartWorkout(null) calls must not create
     * two ACTIVE workouts. The Mutex serializes the check-then-create.
     */
    @Test
    fun concurrent_loadOrStart_createsOnlyOneWorkout() = vmRunTest {
        val workoutDetails = makeWorkoutWithDetails(
            workout = makeWorkout(id = 1L), exercises = emptyList()
        )
        viewModel = createViewModel(workoutDetails)

        // Mutex serializes; first call sees null → creates; second call (after
        // the first created row is committed) sees the new workout → resumes.
        var callCount = 0
        coEvery { workoutRepository.getLatestIncompleteWorkout() } coAnswers {
            callCount++
            if (callCount <= 1) null else workoutDetails.workout
        }
        coEvery { workoutRepository.createWorkout(any()) } returns 1L

        val job1 = async(Dispatchers.Default) { viewModel.loadOrStartWorkout() }
        val job2 = async(Dispatchers.Default) { viewModel.loadOrStartWorkout() }
        awaitAll(job1, job2)
        runCurrent()

        // createWorkout should be called exactly once (Mutex prevents double creation).
        coVerify(exactly = 1) { workoutRepository.createWorkout(any()) }
    }

    /**
     * loadOrStartWorkout(null) finds existing ACTIVE workout and resumes it
     * rather than creating a duplicate.
     */
    @Test
    fun existingActiveWorkout_isResumedNotDuplicated() = vmRunTest {
        val existingWorkout = makeWorkout(id = 42L, status = "ACTIVE")
        val workoutDetails = makeWorkoutWithDetails(
            workout = existingWorkout, exercises = emptyList()
        )
        viewModel = createViewModel(workoutDetails)
        coEvery { workoutRepository.getLatestIncompleteWorkout() } returns existingWorkout

        viewModel.loadOrStartWorkout()
        runCurrent()

        // Should NOT create a new workout.
        coVerify(exactly = 0) { workoutRepository.createWorkout(any()) }
    }

    /**
     * Two concurrent startNewWorkout() calls must create exactly one workout.
     * Check-then-create inside the Mutex: a rapid second tap converges to the
     * just-created workout instead of creating a duplicate ACTIVE row.
     */
    @Test
    fun concurrent_startNewWorkout_createsExactlyOneWorkout() = vmRunTest {
        val workoutDetails = makeWorkoutWithDetails(
            workout = makeWorkout(id = 1L), exercises = emptyList()
        )
        coEvery { workoutRepository.createWorkout(any()) } returns 1L
        viewModel = createViewModel(workoutDetails)

        // First call: no existing → creates. Second call: sees created workout.
        var callCount = 0
        coEvery { workoutRepository.getLatestIncompleteWorkout() } coAnswers {
            callCount++
            if (callCount <= 1) null else workoutDetails.workout
        }

        val job1 = async(Dispatchers.Default) { viewModel.startNewWorkout() }
        val job2 = async(Dispatchers.Default) { viewModel.startNewWorkout() }
        awaitAll(job1, job2)
        runCurrent()

        coVerify(exactly = 1) { workoutRepository.createWorkout(any()) }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 2. APP-016: CONCURRENT addExercise RACE
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun concurrent_addExercise_sameExercise_exactlyOneInsert() = vmRunTest {
        val exercise = makeExercise(id = 10L)
        val workoutDetails = makeWorkoutWithDetails(
            workout = makeWorkout(), exercises = emptyList()
        )
        viewModel = createViewModel(workoutDetails)
        viewModel.loadOrStartWorkout()
        runCurrent()

        coEvery { workoutRepository.addExerciseToWorkout(any(), any(), any()) } returns 300L
        // DB-truth: first lock holder sees no rows → inserts; second sees the
        // committed row → skips (exercise 10 already present).
        var exerciseQueryCount = 0
        coEvery { workoutRepository.getExerciseIdsForWorkout(any()) } coAnswers {
            exerciseQueryCount++
            if (exerciseQueryCount <= 1) emptyList() else listOf(10L)
        }

        val job1 = async(Dispatchers.Default) { viewModel.addExerciseToWorkout(exercise) }
        val job2 = async(Dispatchers.Default) { viewModel.addExerciseToWorkout(exercise) }
        awaitAll(job1, job2)
        runCurrent()

        coVerify(exactly = 1) { workoutRepository.addExerciseToWorkout(any(), eq(10L), any()) }
    }

    @Test
    fun concurrent_addExercise_existingExercisesPreserved() = vmRunTest {
        val exercise1 = makeExercise(id = 10L)
        val exercise2 = makeExercise(id = 11L)
        val we = makeWorkoutExercise(id = 200L, exerciseId = 10L)
        val workoutDetails = makeWorkoutWithDetails(
            workout = makeWorkout(),
            exercises = listOf(WorkoutExerciseWithSets(we, exercise1, listOf(makeSet())))
        )
        viewModel = createViewModel(workoutDetails)
        viewModel.loadOrStartWorkout()
        runCurrent()

        // DB-truth: exercise 10 is already committed.
        coEvery { workoutRepository.getExerciseIdsForWorkout(any()) } returns listOf(10L)

        val job1 = async(Dispatchers.Default) { viewModel.addExerciseToWorkout(exercise1) } // dup
        val job2 = async(Dispatchers.Default) { viewModel.addExerciseToWorkout(exercise2) } // new
        awaitAll(job1, job2)
        runCurrent()

        coVerify(exactly = 1) { workoutRepository.addExerciseToWorkout(any(), eq(11L), any()) }
        coVerify(exactly = 0) { workoutRepository.addExerciseToWorkout(any(), eq(10L), any()) }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 3. REMOVE EXERCISE: STABLE ID
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Double-tap removeExercise(1) must target the same workoutExerciseId.
     * The second tap uses the same stale snapshot → same DB ID → no-op.
     */
    @Test
    fun doubleTap_removeExercise_targetsSameStableId() = vmRunTest {
        val exerciseA = makeExercise(id = 10L, name = "Bench Press")
        val exerciseB = makeExercise(id = 11L, name = "Squat")
        val weA = makeWorkoutExercise(id = 200L, exerciseId = 10L, orderIndex = 0)
        val weB = makeWorkoutExercise(id = 201L, exerciseId = 11L, orderIndex = 1)
        val workoutDetails = makeWorkoutWithDetails(
            workout = makeWorkout(),
            exercises = listOf(
                WorkoutExerciseWithSets(weA, exerciseA, emptyList()),
                WorkoutExerciseWithSets(weB, exerciseB, emptyList())
            )
        )
        viewModel = createViewModel(workoutDetails)
        viewModel.loadOrStartWorkout()
        runCurrent()

        viewModel.removeExercise(1)
        viewModel.removeExercise(1)
        runCurrent()

        coVerify(exactly = 2) { workoutRepository.removeExerciseFromWorkout(eq(201L)) }
        coVerify(exactly = 0) { workoutRepository.removeExerciseFromWorkout(eq(200L)) }
    }

    /**
     * Stale index: after first removal, second call at same index targets
     * the SAME entity (stale snapshot), not the next exercise.
     */
    @Test
    fun staleIndex_removeExercise_doesNotShiftTarget() = vmRunTest {
        val exerciseA = makeExercise(id = 10L)
        val exerciseB = makeExercise(id = 11L)
        val exerciseC = makeExercise(id = 12L)
        val weA = makeWorkoutExercise(id = 200L, exerciseId = 10L, orderIndex = 0)
        val weB = makeWorkoutExercise(id = 201L, exerciseId = 11L, orderIndex = 1)
        val weC = makeWorkoutExercise(id = 202L, exerciseId = 12L, orderIndex = 2)
        val workoutDetails = makeWorkoutWithDetails(
            workout = makeWorkout(),
            exercises = listOf(
                WorkoutExerciseWithSets(weA, exerciseA, emptyList()),
                WorkoutExerciseWithSets(weB, exerciseB, emptyList()),
                WorkoutExerciseWithSets(weC, exerciseC, emptyList())
            )
        )
        viewModel = createViewModel(workoutDetails)
        viewModel.loadOrStartWorkout()
        runCurrent()

        viewModel.removeExercise(1) // targets weB (stale snapshot index 1)
        viewModel.removeExercise(1) // still targets weB (same stale snapshot)
        runCurrent()

        coVerify(exactly = 2) { workoutRepository.removeExerciseFromWorkout(eq(201L)) }
        coVerify(exactly = 0) { workoutRepository.removeExerciseFromWorkout(eq(202L)) }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 4. ADD SET: CONCURRENT RACE
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun concurrent_addSet_noDuplicateSetNumbers() = vmRunTest {
        val exercise = makeExercise(id = 10L)
        val we = makeWorkoutExercise(id = 200L, exerciseId = 10L)
        val workoutDetails = makeWorkoutWithDetails(
            workout = makeWorkout(),
            exercises = listOf(
                WorkoutExerciseWithSets(we, exercise, listOf(makeSet(id = 100L, setNumber = 1)))
            )
        )
        viewModel = createViewModel(workoutDetails)
        viewModel.loadOrStartWorkout()
        runCurrent()

        // Exercise 10 exists in the DB.
        coEvery { workoutRepository.getExerciseIdsForWorkout(any()) } returns listOf(10L)
        // DB-truth set numbers: second lock holder sees the row committed by first.
        var setQueryCount = 0
        coEvery { workoutRepository.getSetNumbersForWorkoutExercise(any()) } coAnswers {
            setQueryCount++
            if (setQueryCount <= 1) listOf(1) else listOf(1, 2)
        }

        val setNumbers = mutableListOf<Int>()
        coEvery { workoutRepository.addSetToExercise(any(), capture(slot())) } coAnswers {
            val set = arg<com.gymcoach.app.domain.model.WorkoutSet>(1)
            setNumbers.add(set.setNumber)
            400L + setNumbers.size
        }

        val job1 = async(Dispatchers.Default) { viewModel.addSet(0) }
        val job2 = async(Dispatchers.Default) { viewModel.addSet(0) }
        awaitAll(job1, job2)
        runCurrent()

        assertEquals(2, setNumbers.size)
        // Mutex serialization + DB-truth read: first sees [1] → 2, second sees [1,2] → 3
        assertEquals(2, setNumbers[0])
        assertEquals(3, setNumbers[1])
        assertTrue("No duplicate set numbers", setNumbers.distinct().size == setNumbers.size)
    }

    @Test
    fun concurrent_addSet_bothSetsPreserved() = vmRunTest {
        val exercise = makeExercise(id = 10L)
        val we = makeWorkoutExercise(id = 200L, exerciseId = 10L)
        val workoutDetails = makeWorkoutWithDetails(
            workout = makeWorkout(),
            exercises = listOf(WorkoutExerciseWithSets(we, exercise, emptyList()))
        )
        viewModel = createViewModel(workoutDetails)
        viewModel.loadOrStartWorkout()
        runCurrent()

        // Exercise 10 exists in the DB; set numbers serialize to 1 then 2.
        coEvery { workoutRepository.getExerciseIdsForWorkout(any()) } returns listOf(10L)
        var setQueryCount = 0
        coEvery { workoutRepository.getSetNumbersForWorkoutExercise(any()) } coAnswers {
            setQueryCount++
            if (setQueryCount <= 1) emptyList() else listOf(1)
        }

        val job1 = async(Dispatchers.Default) { viewModel.addSet(0) }
        val job2 = async(Dispatchers.Default) { viewModel.addSet(0) }
        awaitAll(job1, job2)
        runCurrent()

        coVerify(exactly = 2) { workoutRepository.addSetToExercise(eq(200L), any()) }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 5. COMPLETE WORKOUT: AtomicBoolean + DURABILITY
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun doubleCall_completeWorkout_exactlyOneUpdate() = vmRunTest {
        val workoutDetails = makeWorkoutWithDetails(
            workout = makeWorkout(status = "ACTIVE", completed = false),
            exercises = listOf(
                WorkoutExerciseWithSets(makeWorkoutExercise(), makeExercise(), listOf(makeSet(completed = true)))
            )
        )
        viewModel = createViewModel(workoutDetails)
        viewModel.loadOrStartWorkout()
        runCurrent()

        viewModel.completeWorkout()
        viewModel.completeWorkout()
        runCurrent()

        coVerify(exactly = 1) { workoutRepository.updateWorkout(any()) }
    }

    @Test
    fun completeWorkout_persistsTerminalState() = vmRunTest {
        val workoutDetails = makeWorkoutWithDetails(
            workout = makeWorkout(status = "ACTIVE", completed = false),
            exercises = listOf(
                WorkoutExerciseWithSets(makeWorkoutExercise(), makeExercise(), listOf(makeSet(completed = true)))
            )
        )
        viewModel = createViewModel(workoutDetails)
        viewModel.loadOrStartWorkout()
        runCurrent()

        val workoutSlot = slot<Workout>()
        coEvery { workoutRepository.updateWorkout(capture(workoutSlot)) } returns Unit

        viewModel.completeWorkout()
        runCurrent()

        val persisted = workoutSlot.captured
        assertEquals("COMPLETED", persisted.status)
        assertTrue(persisted.completed)
        assertTrue(persisted.endTime.epochSecond >= persisted.startTime.epochSecond)
        assertTrue(persisted.duration >= 0)
    }

    @Test
    fun completeWorkout_uiStateAfterPersistence() = vmRunTest {
        val workoutDetails = makeWorkoutWithDetails(
            workout = makeWorkout(status = "ACTIVE", completed = false),
            exercises = listOf(
                WorkoutExerciseWithSets(makeWorkoutExercise(), makeExercise(), listOf(makeSet(completed = true)))
            )
        )
        viewModel = createViewModel(workoutDetails)
        viewModel.loadOrStartWorkout()
        runCurrent()

        // Before completion, completed is false.
        assertEquals(false, viewModel.completed.value)

        viewModel.completeWorkout()
        runCurrent()

        // After DB write succeeds, completed is true.
        assertTrue(viewModel.completed.value)
    }

    @Test
    fun completeWorkout_dbFailure_allowsRetry() = vmRunTest {
        val workoutDetails = makeWorkoutWithDetails(
            workout = makeWorkout(status = "ACTIVE", completed = false),
            exercises = listOf(
                WorkoutExerciseWithSets(makeWorkoutExercise(), makeExercise(), listOf(makeSet(completed = true)))
            )
        )
        viewModel = createViewModel(workoutDetails)
        viewModel.loadOrStartWorkout()
        runCurrent()

        // First call: DB fails.
        coEvery { workoutRepository.updateWorkout(any()) } throws RuntimeException("DB error")
        viewModel.completeWorkout()
        runCurrent()

        // Error should be set.
        assertTrue(viewModel.error.value?.contains("Failed to save workout") == true)

        // Second call: guard was reset by catch, should be allowed.
        coEvery { workoutRepository.updateWorkout(any()) } returns Unit
        viewModel.completeWorkout()
        runCurrent()

        // Now it should succeed.
        assertTrue(viewModel.completed.value)
    }

    @Test
    fun completeWorkout_alreadyCompleted_rejected() = vmRunTest {
        val workoutDetails = makeWorkoutWithDetails(
            workout = makeWorkout(status = "COMPLETED", completed = true),
            exercises = listOf(
                WorkoutExerciseWithSets(makeWorkoutExercise(), makeExercise(), listOf(makeSet(completed = true)))
            )
        )
        viewModel = createViewModel(workoutDetails)
        viewModel.loadOrStartWorkout()
        runCurrent()

        viewModel.completeWorkout()
        runCurrent()

        coVerify(exactly = 0) { workoutRepository.updateWorkout(any()) }
    }

    @Test
    fun completeWorkout_abandoned_rejected() = vmRunTest {
        val workoutDetails = makeWorkoutWithDetails(
            workout = makeWorkout(status = "ABANDONED", completed = false),
            exercises = listOf(
                WorkoutExerciseWithSets(makeWorkoutExercise(), makeExercise(), listOf(makeSet(completed = true)))
            )
        )
        viewModel = createViewModel(workoutDetails)
        viewModel.loadOrStartWorkout()
        runCurrent()

        viewModel.completeWorkout()
        runCurrent()

        coVerify(exactly = 0) { workoutRepository.updateWorkout(any()) }
    }

    @Test
    fun completeWorkout_statsNotCorrupted() = vmRunTest {
        val workoutDetails = makeWorkoutWithDetails(
            workout = makeWorkout(status = "ACTIVE", completed = false),
            exercises = listOf(
                WorkoutExerciseWithSets(
                    makeWorkoutExercise(), makeExercise(),
                    listOf(
                        makeSet(id = 100L, setNumber = 1, weight = 100.0, reps = 10, completed = true),
                        makeSet(id = 101L, setNumber = 2, weight = 100.0, reps = 8, completed = true)
                    )
                )
            )
        )
        viewModel = createViewModel(workoutDetails)
        viewModel.loadOrStartWorkout()
        runCurrent()

        viewModel.completeWorkout()
        viewModel.completeWorkout()
        runCurrent()

        val stats = viewModel.completionStats.value
        assertEquals(2, stats.totalSets)
        assertEquals(18, stats.totalReps)
        assertEquals(1800.0, stats.totalVolume, 0.01)
        assertEquals(1, stats.exerciseCount)
    }

    // ═══════════════════════════════════════════════════════════════════
    // 6. FLOW COLLECTOR: DUPLICATE LOAD REGRESSION
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun doubleLoadOrStartWorkout_createsOnlyOneWorkout() = vmRunTest {
        val workoutDetails = makeWorkoutWithDetails(
            workout = makeWorkout(), exercises = emptyList()
        )
        viewModel = createViewModel(workoutDetails)
        viewModel.loadOrStartWorkout()
        runCurrent()

        viewModel.loadOrStartWorkout()
        runCurrent()

        coVerify(exactly = 0) { workoutRepository.createWorkout(any()) }
    }
}
