package com.gymcoach.app.presentation.history

import com.gymcoach.app.core.export.WorkoutDataExporter
import com.gymcoach.app.core.timer.RestTimerManager
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.model.SetType
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutExercise
import com.gymcoach.app.domain.model.WorkoutExerciseWithSets
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutWithDetails
import com.gymcoach.app.domain.model.WorkoutWithStats
import com.gymcoach.app.domain.repository.WorkoutRepository
import io.mockk.coEvery
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutHistoryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var restTimer: RestTimerManager
    private lateinit var workoutDataExporter: WorkoutDataExporter
    private lateinit var viewModel: WorkoutHistoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        workoutRepository = mockk(relaxed = true)
        restTimer = mockk(relaxed = true)
        workoutDataExporter = WorkoutDataExporter()

        val sampleStats = WorkoutWithStats(
            id = 101L,
            date = Instant.ofEpochMilli(1700000000000L),
            startTime = Instant.ofEpochMilli(1700000000000L),
            endTime = Instant.ofEpochMilli(1700003600000L),
            duration = 3600L,
            notes = "Great leg session",
            completed = true,
            status = "COMPLETED",
            volume = 1500.0,
            setCount = 5,
            repCount = 25,
            exerciseCount = 1
        )
        every { workoutRepository.getCompletedWorkouts() } returns flowOf(listOf(sampleStats))
        coEvery { workoutRepository.getIncompleteWorkout() } returns null

        val sampleWorkout = Workout(
            id = 101L,
            date = Instant.ofEpochMilli(1700000000000L),
            startTime = Instant.ofEpochMilli(1700000000000L),
            endTime = Instant.ofEpochMilli(1700003600000L),
            duration = 3600L,
            completed = true,
            status = "COMPLETED",
            notes = "Great leg session"
        )
        val sampleExercise = Exercise(
            id = 1L,
            name = "Barbell Squat",
            description = "Compound leg exercise",
            category = "Legs",
            muscleGroup = "Quadriceps",
            secondaryMuscles = "Glutes, Hamstrings",
            equipment = "Barbell",
            difficulty = "Intermediate",
            instructions = "",
            tips = ""
        )
        val sampleSets = listOf(
            WorkoutSet(
                id = 1L,
                workoutExerciseId = 1L,
                setNumber = 1,
                weight = 100.0,
                reps = 5,
                rpe = 8.0,
                completed = true,
                setType = SetType.NORMAL,
                restSeconds = 120
            )
        )
        val workoutDetails = WorkoutWithDetails(
            workout = sampleWorkout,
            exercises = listOf(
                WorkoutExerciseWithSets(
                    workoutExercise = WorkoutExercise(id = 1L, workoutId = 101L, exerciseId = 1L, orderIndex = 0),
                    exercise = sampleExercise,
                    sets = sampleSets
                )
            )
        )
        every { workoutRepository.getWorkoutWithDetails(101L) } returns flowOf(workoutDetails)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `exportData CSV_SPREADSHEET generates valid csv result`() = runTest {
        viewModel = WorkoutHistoryViewModel(workoutRepository, restTimer, workoutDataExporter)

        viewModel.exportData(ExportFormat.CSV_SPREADSHEET)

        val result = viewModel.exportResult.value
        assertNotNull(result)
        assertEquals("text/csv", result!!.mimeType)
        assertTrue(result.filename.startsWith("gymcoach_workouts_"))
        assertTrue(result.filename.endsWith(".csv"))
        assertTrue(result.content.contains("Barbell Squat"))
        assertTrue(result.content.contains("100.0,5,8.0"))
    }

    @Test
    fun `exportData CSV_STRONG generates exact Strong app compatible header and rows`() = runTest {
        viewModel = WorkoutHistoryViewModel(workoutRepository, restTimer, workoutDataExporter)

        viewModel.exportData(ExportFormat.CSV_STRONG)

        val result = viewModel.exportResult.value
        assertNotNull(result)
        assertEquals("text/csv", result!!.mimeType)
        assertTrue(result.filename.startsWith("strong_workouts_"))
        assertTrue(result.filename.endsWith(".csv"))
        assertTrue(result.content.startsWith("Date,Workout Name,Duration,Exercise Name,Set Order,Weight,Reps,Distance,Seconds,Notes,Workout Notes,RPE\n"))
        assertTrue(result.content.contains("Barbell Squat"))
        assertTrue(result.content.contains("100.0,5,0,0,,Great leg session,8.0"))
    }

    @Test
    fun `exportData JSON generates valid json result with non-backup filename`() = runTest {
        viewModel = WorkoutHistoryViewModel(workoutRepository, restTimer, workoutDataExporter)

        viewModel.exportData(ExportFormat.JSON)

        val result = viewModel.exportResult.value
        assertNotNull(result)
        assertEquals("application/json", result!!.mimeType)
        assertTrue(result.filename.startsWith("gymcoach_workouts_"))
        assertTrue(result.filename.endsWith(".json"))
        assertTrue(result.content.contains("exerciseName"))
        assertTrue(result.content.contains("Barbell Squat"))
        assertTrue(result.content.contains("workoutCount"))
    }

    @Test
    fun `clearExportResult resets state to null`() = runTest {
        viewModel = WorkoutHistoryViewModel(workoutRepository, restTimer, workoutDataExporter)
        viewModel.exportData(ExportFormat.CSV_SPREADSHEET)
        assertNotNull(viewModel.exportResult.value)

        viewModel.clearExportResult()
        assertNull(viewModel.exportResult.value)
    }
}
