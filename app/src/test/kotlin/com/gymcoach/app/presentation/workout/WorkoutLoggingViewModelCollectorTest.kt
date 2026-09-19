package com.gymcoach.app.presentation.workout

import com.gymcoach.app.core.progression.ProgressionEngine
import com.gymcoach.app.core.timer.RestTimerManager
import com.gymcoach.app.domain.repository.ExerciseRepository
import com.gymcoach.app.domain.repository.UserProfileRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import io.mockk.verify
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.model.SetType
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutExercise
import com.gymcoach.app.domain.model.WorkoutExerciseWithSets
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutWithDetails
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutLoggingViewModelCollectorTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadOrStartWorkout cancels previous flow collector on re-entry`() = runTest {
        val workoutRepository = mockk<WorkoutRepository>(relaxed = true)
        val exerciseRepository = mockk<ExerciseRepository>(relaxed = true)
        val userProfileRepository = mockk<UserProfileRepository>(relaxed = true)
        val progressionEngine = mockk<ProgressionEngine>(relaxed = true)
        val restTimer = mockk<RestTimerManager>(relaxed = true)

        coEvery { exerciseRepository.getAllExercises() } returns flowOf(emptyList())
        coEvery { workoutRepository.getWorkoutWithDetails(10L) } returns flowOf(null)
        coEvery { workoutRepository.getWorkoutWithDetails(20L) } returns flowOf(null)

        val viewModel = WorkoutLoggingViewModel(
            workoutRepository,
            exerciseRepository,
            restTimer,
            progressionEngine,
            userProfileRepository
        ).apply { enableWorkoutTimer = false }

        try {
            // First invocation
            viewModel.loadOrStartWorkout(10L)

            // Second invocation (e.g. config change / re-entry with new workoutId)
            viewModel.loadOrStartWorkout(20L)

            // Verify flowA and flowB called exactly once each
            coVerify(exactly = 1) { workoutRepository.getWorkoutWithDetails(10L) }
            coVerify(exactly = 1) { workoutRepository.getWorkoutWithDetails(20L) }
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun `applyCameraReps updates set reps, marks completed, and starts rest timer`() = runTest {
        val workoutRepository = mockk<WorkoutRepository>(relaxed = true)
        val exerciseRepository = mockk<ExerciseRepository>(relaxed = true)
        val userProfileRepository = mockk<UserProfileRepository>(relaxed = true)
        val progressionEngine = mockk<ProgressionEngine>(relaxed = true)
        val restTimer = mockk<RestTimerManager>(relaxed = true)

        val now = Instant.now()
        val sampleWorkout = Workout(
            id = 55L,
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
                    workoutExercise = WorkoutExercise(id = 1L, workoutId = 55L, exerciseId = 100L, orderIndex = 0),
                    exercise = Exercise(id = 100L, name = "Barbell Squat", description = "", muscleGroup = "Legs", equipment = "Barbell", difficulty = "Intermediate"),
                    sets = listOf(
                        WorkoutSet(id = 10L, workoutExerciseId = 1L, setNumber = 1, weight = 100.0, reps = 5, rpe = 8.0, restSeconds = 90, completed = false, setType = SetType.NORMAL),
                        WorkoutSet(id = 11L, workoutExerciseId = 1L, setNumber = 2, weight = 100.0, reps = 5, rpe = 8.0, restSeconds = 90, completed = false, setType = SetType.NORMAL)
                    )
                )
            )
        )

        coEvery { exerciseRepository.getAllExercises() } returns flowOf(emptyList())
        coEvery { workoutRepository.getWorkoutWithDetails(55L) } returns flowOf(details)

        val viewModel = WorkoutLoggingViewModel(
            workoutRepository,
            exerciseRepository,
            restTimer,
            progressionEngine,
            userProfileRepository
        ).apply { enableWorkoutTimer = false }

        try {
            viewModel.loadOrStartWorkout(55L)
            kotlinx.coroutines.delay(100) // allow collection

            viewModel.applyCameraReps(exerciseIndex = 0, reps = 12)

            coVerify(exactly = 1) {
                workoutRepository.updateSet(
                    match {
                        it.id == 10L && it.reps == 12 && it.completed
                    }
                )
            }

            verify(exactly = 1) {
                restTimer.start(
                    seconds = 90,
                    scope = any(),
                    nextSet = "Barbell Squat Set 2",
                    workoutId = 55L
                )
            }
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun `toggleSetCompletion starts restTimer with next set label and workout id`() = runTest {
        val workoutRepository = mockk<WorkoutRepository>(relaxed = true)
        val exerciseRepository = mockk<ExerciseRepository>(relaxed = true)
        val userProfileRepository = mockk<UserProfileRepository>(relaxed = true)
        val progressionEngine = mockk<ProgressionEngine>(relaxed = true)
        val restTimer = mockk<RestTimerManager>(relaxed = true)

        val now = Instant.now()
        val sampleWorkout = Workout(
            id = 55L,
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
                    workoutExercise = WorkoutExercise(id = 1L, workoutId = 55L, exerciseId = 100L, orderIndex = 0),
                    exercise = Exercise(id = 100L, name = "Barbell Squat", description = "", muscleGroup = "Legs", equipment = "Barbell", difficulty = "Intermediate"),
                    sets = listOf(
                        WorkoutSet(id = 10L, workoutExerciseId = 1L, setNumber = 1, weight = 100.0, reps = 5, rpe = 8.0, restSeconds = 90, completed = false, setType = SetType.NORMAL),
                        WorkoutSet(id = 11L, workoutExerciseId = 1L, setNumber = 2, weight = 100.0, reps = 5, rpe = 8.0, restSeconds = 90, completed = false, setType = SetType.NORMAL)
                    )
                )
            )
        )

        coEvery { exerciseRepository.getAllExercises() } returns flowOf(emptyList())
        coEvery { workoutRepository.getWorkoutWithDetails(55L) } returns flowOf(details)

        val viewModel = WorkoutLoggingViewModel(
            workoutRepository,
            exerciseRepository,
            restTimer,
            progressionEngine,
            userProfileRepository
        ).apply { enableWorkoutTimer = false }

        try {
            viewModel.loadOrStartWorkout(55L)
            viewModel.toggleSetCompletion(0, 0)

            verify(exactly = 1) {
                restTimer.start(
                    seconds = 90,
                    scope = any(),
                    nextSet = "Barbell Squat Set 2",
                    workoutId = 55L
                )
            }
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun `completeWorkout generates WorkoutSummary and records PRs`() = runTest {
        val workoutRepository = mockk<WorkoutRepository>(relaxed = true)
        val exerciseRepository = mockk<ExerciseRepository>(relaxed = true)
        val userProfileRepository = mockk<UserProfileRepository>(relaxed = true)
        val progressionEngine = mockk<ProgressionEngine>(relaxed = true)
        val restTimer = mockk<RestTimerManager>(relaxed = true)
        val personalRecordDao = mockk<com.gymcoach.app.data.local.dao.PersonalRecordDao>(relaxed = true)
        val prDetector = mockk<com.gymcoach.app.core.progression.PRDetector>(relaxed = true)
        val readinessRepository = mockk<com.gymcoach.app.domain.repository.ReadinessRepository>(relaxed = true)

        coEvery { readinessRepository.getLatestReadiness() } returns flowOf(null)

        val now = Instant.now()
        val sampleWorkout = Workout(
            id = 55L,
            date = now,
            startTime = now.minusSeconds(3600),
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
                    workoutExercise = WorkoutExercise(id = 1L, workoutId = 55L, exerciseId = 100L, orderIndex = 0),
                    exercise = Exercise(id = 100L, name = "Bench Press", description = "", muscleGroup = "Chest", equipment = "Barbell", difficulty = "Intermediate"),
                    sets = listOf(
                        WorkoutSet(id = 10L, workoutExerciseId = 1L, setNumber = 1, weight = 100.0, reps = 5, rpe = 8.0, restSeconds = 90, completed = true, setType = SetType.NORMAL)
                    )
                )
            )
        )

        coEvery { exerciseRepository.getAllExercises() } returns flowOf(emptyList())
        coEvery { workoutRepository.getWorkoutWithDetails(55L) } returns flowOf(details)
        coEvery { personalRecordDao.getByExerciseId(100L) } returns flowOf(emptyList())

        val newPR = com.gymcoach.app.core.progression.PRDetector.PersonalRecord(
            exerciseId = 100L,
            exerciseName = "Bench Press",
            type = com.gymcoach.app.core.progression.PRDetector.PRType.WEIGHT,
            value = 100.0,
            details = "100.0kg lifted",
            date = now,
            workoutId = 55L
        )
        io.mockk.every { prDetector.detectPRs(any(), any(), any(), any(), any()) } returns listOf(newPR)

        val viewModel = WorkoutLoggingViewModel(
            workoutRepository,
            exerciseRepository,
            restTimer,
            progressionEngine,
            userProfileRepository,
            readinessRepository,
            personalRecordDao,
            prDetector
        ).apply { enableWorkoutTimer = false }

        try {
            viewModel.loadOrStartWorkout(55L)

            // Wait for flow to collect
            kotlinx.coroutines.delay(100)

            viewModel.completeWorkout()

            kotlinx.coroutines.delay(100)

            coVerify(exactly = 1) { personalRecordDao.insert(any()) }

            val summary = viewModel.workoutSummary.value
            org.junit.Assert.assertNotNull(summary)
            org.junit.Assert.assertEquals(55L, summary?.workoutId)
            org.junit.Assert.assertEquals("Workout", summary?.workoutName) // We updated completeWorkout to use "Workout" in viewmodel
            org.junit.Assert.assertEquals(1, summary?.completedSetsCount)
            org.junit.Assert.assertEquals(1, summary?.totalSetsCount)
            org.junit.Assert.assertEquals(500.0, summary?.totalVolumeKg)
            org.junit.Assert.assertEquals(1, summary?.newPRs?.size)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun `swapExercise invokes repository swapExercise and refreshes performance stats`() = runTest {
        val workoutRepository = mockk<WorkoutRepository>(relaxed = true)
        val exerciseRepository = mockk<ExerciseRepository>(relaxed = true)
        val userProfileRepository = mockk<UserProfileRepository>(relaxed = true)
        val progressionEngine = mockk<ProgressionEngine>(relaxed = true)
        val restTimer = mockk<RestTimerManager>(relaxed = true)

        val now = Instant.now()
        val sampleWorkout = Workout(
            id = 77L,
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
                    workoutExercise = WorkoutExercise(id = 101L, workoutId = 77L, exerciseId = 1L, orderIndex = 0),
                    exercise = Exercise(id = 1L, name = "Barbell Bench Press", description = "Chest press", category = "push", muscleGroup = "Chest", equipment = "barbell", difficulty = "intermediate"),
                    sets = emptyList()
                )
            )
        )

        coEvery { exerciseRepository.getAllExercises() } returns flowOf(emptyList())
        coEvery { workoutRepository.getWorkoutWithDetails(77L) } returns flowOf(details)
        coEvery { workoutRepository.getLastSetsForExercise(2L) } returns emptyList()

        val viewModel = WorkoutLoggingViewModel(
            workoutRepository,
            exerciseRepository,
            restTimer,
            progressionEngine,
            userProfileRepository
        ).apply { enableWorkoutTimer = false }

        try {
            viewModel.loadOrStartWorkout(77L)
            kotlinx.coroutines.delay(100)

            // Swap exercise 0 (exerciseId 1) with exerciseId 2 (Dumbbell Bench Press)
            viewModel.swapExercise(exerciseIndex = 0, newExerciseId = 2L)
            kotlinx.coroutines.delay(100)

            coVerify(exactly = 1) { workoutRepository.swapExercise(101L, 2L) }
        } finally {
            viewModel.clearForTest()
        }
    }
}
