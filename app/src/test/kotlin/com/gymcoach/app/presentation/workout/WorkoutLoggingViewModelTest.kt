package com.gymcoach.app.presentation.workout

import com.gymcoach.app.core.progression.PRDetector
import com.gymcoach.app.core.progression.ProgressionEngine
import com.gymcoach.app.core.timer.RestTimerManager
import com.gymcoach.app.data.local.dao.PersonalRecordDao
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutExercise
import com.gymcoach.app.domain.model.WorkoutExerciseWithSets
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutWithDetails
import com.gymcoach.app.domain.repository.ExerciseRepository
import com.gymcoach.app.domain.repository.ReadinessRepository
import com.gymcoach.app.domain.repository.UserProfileRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutLoggingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: WorkoutLoggingViewModel
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var restTimerManager: RestTimerManager
    private lateinit var progressionEngine: ProgressionEngine
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var readinessRepository: ReadinessRepository
    private lateinit var personalRecordDao: PersonalRecordDao
    private lateinit var prDetector: PRDetector

    private val currentWorkoutFlow = MutableStateFlow<WorkoutWithDetails?>(null)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        workoutRepository = mockk(relaxed = true)
        exerciseRepository = mockk(relaxed = true)
        restTimerManager = mockk(relaxed = true)
        progressionEngine = mockk(relaxed = true)
        userProfileRepository = mockk(relaxed = true)
        readinessRepository = mockk(relaxed = true)
        personalRecordDao = mockk(relaxed = true)
        prDetector = mockk(relaxed = true)

        every { readinessRepository.getLatestReadiness() } returns emptyFlow()
        every { exerciseRepository.getAllExercises() } returns emptyFlow()
        every { workoutRepository.getWorkoutWithDetails(any()) } returns currentWorkoutFlow
        coEvery { workoutRepository.getLastSetsForExercises(any()) } returns emptyMap()
        every { restTimerManager.state } returns MutableStateFlow(com.gymcoach.app.core.timer.RestTimerState())

        viewModel = WorkoutLoggingViewModel(
            workoutRepository,
            exerciseRepository,
            restTimerManager,
            progressionEngine,
            userProfileRepository,
            readinessRepository,
            personalRecordDao,
            prDetector,
            null
        ).apply { enableWorkoutTimer = false }
    }

    @After
    fun tearDown() {
        viewModel.clearForTest()
        Dispatchers.resetMain()
    }

    private fun createTestWorkout(): WorkoutWithDetails {
        val exercise1 = Exercise(id = 1L, name = "Squat", description = "", muscleGroup = "Legs", equipment = "barbell", difficulty = "intermediate")
        val exercise2 = Exercise(id = 2L, name = "Bench Press", description = "", muscleGroup = "Chest", equipment = "barbell", difficulty = "intermediate")
        val set1 = WorkoutSet(id = 1L, workoutExerciseId = 1L, setNumber = 1, weight = 100.0, reps = 10, rpe = 8.0, restSeconds = 60, completed = false, setType = com.gymcoach.app.domain.model.SetType.NORMAL)
        val set2 = WorkoutSet(id = 2L, workoutExerciseId = 1L, setNumber = 2, weight = 100.0, reps = 10, rpe = 8.0, restSeconds = 60, completed = false, setType = com.gymcoach.app.domain.model.SetType.NORMAL)
        val workoutExercise1 = WorkoutExercise(id = 1L, workoutId = 1L, exerciseId = exercise1.id, orderIndex = 1)
        val workoutExercise2 = WorkoutExercise(id = 2L, workoutId = 1L, exerciseId = exercise2.id, orderIndex = 2)
        val workoutExerciseWithSets1 = WorkoutExerciseWithSets(workoutExercise1, exercise1, listOf(set1, set2))
        val workoutExerciseWithSets2 = WorkoutExerciseWithSets(workoutExercise2, exercise2, listOf(WorkoutSet(id = 3L, workoutExerciseId = 2L, setNumber = 1, weight = 80.0, reps = 8, rpe = 8.0, restSeconds = 60, completed = false, setType = com.gymcoach.app.domain.model.SetType.NORMAL)))
        val workout = Workout(id = 1L, date = Instant.now(), startTime = Instant.now(), endTime = Instant.now(), duration = 0, notes = "", completed = false, status = "IN_PROGRESS")
        return WorkoutWithDetails(workout, listOf(workoutExerciseWithSets1, workoutExerciseWithSets2))
    }

    @Test
    fun givenIncompleteSet_whenToggleSetCompletion_thenSetIsCompletedAndTimerStarts() = runTest {
        val workout = createTestWorkout()
        currentWorkoutFlow.value = workout
        viewModel.loadOrStartWorkout(workout.workout.id)

        viewModel.toggleSetCompletion(0, 0)

        coVerify { workoutRepository.updateSet(any()) }
        coVerify { restTimerManager.start(60, any(), "Squat Set 2", 1L) }
    }

    @Test
    fun givenAutoStartRestDisabled_whenToggleSetCompletion_thenSetIsCompletedButTimerDoesNotStart() = runTest {
        val workout = createTestWorkout()
        currentWorkoutFlow.value = workout
        val preferences = com.gymcoach.app.core.preferences.InMemoryAppPreferences()
        preferences.setAutoStartRestTimer(false)
        val customViewModel = WorkoutLoggingViewModel(
            workoutRepository,
            exerciseRepository,
            restTimerManager,
            progressionEngine,
            userProfileRepository,
            readinessRepository,
            personalRecordDao,
            prDetector,
            null,
            preferences
        ).apply { enableWorkoutTimer = false }

        customViewModel.loadOrStartWorkout(workout.workout.id)
        customViewModel.toggleSetCompletion(0, 0)

        coVerify { workoutRepository.updateSet(any()) }
        coVerify(exactly = 0) { restTimerManager.start(any(), any(), any(), any()) }
        customViewModel.clearForTest()
    }

    @Test
    fun givenLoadedWorkout_whenViewModelRecreated_thenStateIsPersisted() = runTest {
        val workout = createTestWorkout()
        currentWorkoutFlow.value = workout
        
        viewModel.loadOrStartWorkout(1L)
        
        assertEquals(workout.workout.id, viewModel.currentWorkout.value?.workout?.id)
        
        val newViewModel = WorkoutLoggingViewModel(
            workoutRepository,
            exerciseRepository,
            restTimerManager,
            progressionEngine,
            userProfileRepository,
            readinessRepository,
            personalRecordDao,
            prDetector,
            null
        ).apply { enableWorkoutTimer = false }
        
        newViewModel.loadOrStartWorkout(1L)
        
        assertEquals(workout.workout.id, newViewModel.currentWorkout.value?.workout?.id)
        newViewModel.clearForTest()
    }

    @Test
    fun givenValidWeightAndReps_whenUpdateSet_thenStateIsUpdated() = runTest {
        val workout = createTestWorkout()
        currentWorkoutFlow.value = workout
        viewModel.loadOrStartWorkout(workout.workout.id)
        
        viewModel.updateSetWeight(0, 0, 150.0)
        coVerify { workoutRepository.updateSet(match { it.weight == 150.0 }) }
        
        viewModel.updateSetReps(0, 0, 15)
        coVerify { workoutRepository.updateSet(match { it.reps == 15 }) }
    }

    @Test
    fun givenNegativeWeightAndReps_whenUpdateSet_thenConstrainedToZero() = runTest {
        val workout = createTestWorkout()
        currentWorkoutFlow.value = workout
        viewModel.loadOrStartWorkout(workout.workout.id)
        
        viewModel.updateSetWeight(0, 0, -10.0)
        coVerify { workoutRepository.updateSet(match { it.weight == 0.0 }) }
        
        viewModel.updateSetReps(0, 0, -5)
        coVerify { workoutRepository.updateSet(match { it.reps == 0 }) }
    }

    @Test
    fun givenLargeWeightAndReps_whenUpdateSet_thenStateIsUpdatedSuccessfully() = runTest {
        val workout = createTestWorkout()
        currentWorkoutFlow.value = workout
        viewModel.loadOrStartWorkout(workout.workout.id)
        
        viewModel.updateSetWeight(0, 0, 1000.0)
        coVerify { workoutRepository.updateSet(match { it.weight == 1000.0 }) }
        
        viewModel.updateSetReps(0, 0, 100)
        coVerify { workoutRepository.updateSet(match { it.reps == 100 }) }
    }

    @Test
    fun givenCompletedWorkoutWithNewPR_whenCompleteWorkout_thenSummaryContainsNewPR() = runTest {
        val exercise = Exercise(id = 1L, name = "Squat", description = "", muscleGroup = "Legs", equipment = "barbell", difficulty = "intermediate")
        val set1 = WorkoutSet(id = 1L, workoutExerciseId = 1L, setNumber = 1, weight = 150.0, reps = 5, rpe = 8.0, restSeconds = 60, completed = true, setType = com.gymcoach.app.domain.model.SetType.NORMAL)
        val workoutExercise = WorkoutExercise(id = 1L, workoutId = 1L, exerciseId = exercise.id, orderIndex = 1)
        val workoutExerciseWithSets = WorkoutExerciseWithSets(workoutExercise, exercise, listOf(set1))
        val workout = Workout(id = 1L, date = Instant.now(), startTime = Instant.now().minusSeconds(3600), endTime = Instant.now(), duration = 0, notes = "", completed = false, status = "IN_PROGRESS")
        val workoutDetails = WorkoutWithDetails(workout, listOf(workoutExerciseWithSets))
        currentWorkoutFlow.value = workoutDetails
        viewModel.loadOrStartWorkout(workout.id)

        // Mock PR detection
        val pr = PRDetector.PersonalRecord(
            exerciseId = 1L,
            exerciseName = "Squat",
            date = Instant.now(),
            type = PRDetector.PRType.WEIGHT,
            value = 150.0,
            details = "150.0kg",
            workoutId = 1L
        )
        every { prDetector.detectPRs(any(), any(), any(), any(), any()) } returns listOf(pr)

        viewModel.completeWorkout()
        
        val summary = viewModel.workoutSummary.value
        assertEquals(true, summary?.newPRs?.isNotEmpty())
        assertEquals(1L, summary?.newPRs?.first()?.exerciseId)
        assertEquals(150.0, summary?.newPRs?.first()?.value)
    }

    @Test
    fun givenCompletedSets_whenCompleteWorkout_thenSummaryGeneratesCorrectTotals() = runTest {
        val exercise = Exercise(id = 1L, name = "Squat", description = "", muscleGroup = "Legs", equipment = "barbell", difficulty = "intermediate")
        // 2 sets, 100kg x 10 reps each
        val set1 = WorkoutSet(id = 1L, workoutExerciseId = 1L, setNumber = 1, weight = 100.0, reps = 10, rpe = 8.0, restSeconds = 60, completed = true, setType = com.gymcoach.app.domain.model.SetType.NORMAL)
        val set2 = WorkoutSet(id = 2L, workoutExerciseId = 1L, setNumber = 2, weight = 100.0, reps = 10, rpe = 8.0, restSeconds = 60, completed = true, setType = com.gymcoach.app.domain.model.SetType.NORMAL)
        val workoutExercise = WorkoutExercise(id = 1L, workoutId = 1L, exerciseId = exercise.id, orderIndex = 1)
        val workoutExerciseWithSets = WorkoutExerciseWithSets(workoutExercise, exercise, listOf(set1, set2))
        
        val workout = Workout(id = 1L, date = Instant.now(), startTime = Instant.now().minusSeconds(3600), endTime = Instant.now(), duration = 0, notes = "", completed = false, status = "IN_PROGRESS")
        val workoutDetails = WorkoutWithDetails(workout, listOf(workoutExerciseWithSets))
        currentWorkoutFlow.value = workoutDetails
        viewModel.loadOrStartWorkout(workout.id)

        viewModel.completeWorkout()
        
        val summary = viewModel.workoutSummary.value
        assertNotNull(summary)
        assertEquals(2, summary?.completedSetsCount)
        assertEquals(1, summary?.exercisesCompletedCount)
        assertEquals(2000.0, summary?.totalVolumeKg)
    }


    @Test
    fun givenTwoExercises_whenLinkExercisesAsSuperset_thenGroupIsAddedAndCanBeUnlinked() = runTest {
        val workout = createTestWorkout()
        currentWorkoutFlow.value = workout
        viewModel.loadOrStartWorkout(workout.workout.id)
        
        viewModel.linkExercisesAsSuperset(0, 1)
        val superset = viewModel.getSupersetForExercise(0)
        
        assertNotNull(superset)
        assertEquals(true, superset?.exerciseIndices?.containsAll(listOf(0, 1)))

        viewModel.unlinkSuperset(0)
        assertEquals(null, viewModel.getSupersetForExercise(0))
    }

    @Test
    fun givenZeroCompletedSets_whenCompleteWorkout_thenWorkoutIsDiscardedAndNoCelebrationSummaryEmitted() = runTest {
        val workout = createTestWorkout() // has 1 set with completed = false
        currentWorkoutFlow.value = workout
        viewModel.loadOrStartWorkout(workout.workout.id)

        viewModel.completeWorkout()

        coVerify { workoutRepository.deleteWorkout(workout.workout.id) }
        assertEquals(null, viewModel.workoutSummary.value)
        assertEquals(false, viewModel.completed.value)
        assertEquals(true, viewModel.discarded.value)
    }

    @Test
    fun givenActiveWorkout_whenDiscardWorkout_thenWorkoutIsDeletedAndStateIsCleared() = runTest {
        val workout = createTestWorkout()
        currentWorkoutFlow.value = workout
        viewModel.loadOrStartWorkout(workout.workout.id)

        var callbackCalled = false
        viewModel.discardWorkout { callbackCalled = true }

        coVerify { workoutRepository.deleteWorkout(workout.workout.id) }
        assertEquals(true, callbackCalled)
        assertEquals(true, viewModel.discarded.value)
        assertEquals(null, viewModel.currentWorkout.value)
    }

    @Test
    fun givenIncompleteWorkout_whenCheckingCompletedSets_thenReportsAccurateCounts() = runTest {
        val workout = createTestWorkout()
        currentWorkoutFlow.value = workout
        viewModel.loadOrStartWorkout(workout.workout.id)

        // Initially 0 completed sets
        assertEquals(false, viewModel.hasCompletedSets())
        assertEquals(0, viewModel.getCompletedSetsCount())

        // Mark 1 set completed
        val exercise1 = workout.exercises[0]
        val completedSet = exercise1.sets[0].copy(completed = true)
        val updatedWe = exercise1.copy(sets = listOf(completedSet, exercise1.sets[1]))
        val updatedWorkout = workout.copy(exercises = listOf(updatedWe, workout.exercises[1]))
        currentWorkoutFlow.value = updatedWorkout

        assertEquals(true, viewModel.hasCompletedSets())
        assertEquals(1, viewModel.getCompletedSetsCount())
    }

}


