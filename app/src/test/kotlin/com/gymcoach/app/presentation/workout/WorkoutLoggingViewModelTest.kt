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
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createTestWorkout(): WorkoutWithDetails {
        val exercise = Exercise(id = 1L, name = "Squat", description = "", muscleGroup = "Legs", equipment = "barbell", difficulty = "intermediate")
        val set1 = WorkoutSet(id = 1L, workoutExerciseId = 1L, setNumber = 1, weight = 100.0, reps = 10, rpe = 8.0, restSeconds = 60, completed = false, setType = com.gymcoach.app.domain.model.SetType.NORMAL)
        val workoutExercise = WorkoutExercise(id = 1L, workoutId = 1L, exerciseId = exercise.id, orderIndex = 1)
        val workoutExerciseWithSets = WorkoutExerciseWithSets(workoutExercise, exercise, listOf(set1))
        val workout = Workout(id = 1L, date = Instant.now(), startTime = Instant.now(), endTime = Instant.now(), duration = 0, notes = "", completed = false, status = "IN_PROGRESS")
        return WorkoutWithDetails(workout, listOf(workoutExerciseWithSets))
    }

    @Test
    fun givenIncompleteSet_whenToggleSetCompletion_thenSetIsCompletedAndTimerStarts() = runTest {
        val workout = createTestWorkout()
        currentWorkoutFlow.value = workout

        viewModel.toggleSetCompletion(0, 0)

        coVerify { workoutRepository.updateSet(any()) }
        coVerify { restTimerManager.start(60, any(), "Squat Set 2", 1L) }
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
        )
        
        newViewModel.loadOrStartWorkout(1L)
        
        assertEquals(workout.workout.id, newViewModel.currentWorkout.value?.workout?.id)
    }

    @Test
    fun givenValidWeightAndReps_whenUpdateSet_thenStateIsUpdated() = runTest {
        val workout = createTestWorkout()
        currentWorkoutFlow.value = workout
        
        viewModel.updateSetWeight(0, 0, 150.0)
        coVerify { workoutRepository.updateSet(match { it.weight == 150.0 }) }
        
        viewModel.updateSetReps(0, 0, 15)
        coVerify { workoutRepository.updateSet(match { it.reps == 15 }) }
    }

    @Test
    fun givenNegativeWeightAndReps_whenUpdateSet_thenConstrainedToZero() = runTest {
        val workout = createTestWorkout()
        currentWorkoutFlow.value = workout
        
        viewModel.updateSetWeight(0, 0, -10.0)
        coVerify { workoutRepository.updateSet(match { it.weight == 0.0 }) }
        
        viewModel.updateSetReps(0, 0, -5)
        coVerify { workoutRepository.updateSet(match { it.reps == 0 }) }
    }

    @Test
    fun givenLargeWeightAndReps_whenUpdateSet_thenStateIsUpdatedSuccessfully() = runTest {
        val workout = createTestWorkout()
        currentWorkoutFlow.value = workout
        
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
        
        viewModel.linkExercisesAsSuperset(0, 1)
        val superset = viewModel.getSupersetForExercise(0)
        
        assertNotNull(superset)
        assertEquals(true, superset?.exerciseIndices?.containsAll(listOf(0, 1)))

        viewModel.unlinkSuperset(0)
        assertEquals(null, viewModel.getSupersetForExercise(0))
    }

}
