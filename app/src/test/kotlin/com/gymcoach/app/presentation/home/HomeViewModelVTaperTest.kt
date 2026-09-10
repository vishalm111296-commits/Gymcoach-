package com.gymcoach.app.presentation.home

import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.data.local.entity.ProgramDayEntity
import com.gymcoach.app.data.local.entity.ProgramEntity
import com.gymcoach.app.data.local.entity.ProgramExerciseEntity
import com.gymcoach.app.domain.model.CompletedSetContext
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.model.ExerciseMuscleAssignment
import com.gymcoach.app.domain.model.WorkoutWithStats
import com.gymcoach.app.domain.repository.AnalyticsRepository
import com.gymcoach.app.domain.repository.ExerciseRepository
import com.gymcoach.app.domain.repository.ProgramRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelVTaperTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var programRepository: ProgramRepository
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var analyticsRepository: AnalyticsRepository
    private lateinit var volumeCalculator: VolumeCalculator

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        programRepository = mockk(relaxed = true)
        workoutRepository = mockk(relaxed = true)
        exerciseRepository = mockk(relaxed = true)
        analyticsRepository = mockk(relaxed = true)
        volumeCalculator = VolumeCalculator()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `no active program returns state with hasProgram false`() = runTest {
        every { programRepository.getActiveProgram() } returns flowOf(null)
        every { workoutRepository.getCompletedWorkouts() } returns flowOf(emptyList())
        every { workoutRepository.getCompletedSetsWithContext(any()) } returns flowOf(emptyList())
        every { exerciseRepository.getAllExercises() } returns flowOf(emptyList())
        every { exerciseRepository.getAllExerciseMuscleDetails() } returns flowOf(emptyList())

        val viewModel = HomeViewModel(programRepository, workoutRepository, exerciseRepository, volumeCalculator, analyticsRepository)
        assertFalse(viewModel.uiState.value.hasProgram)
    }

    @Test
    fun `active program with no completed workouts displays zero actual volume for all V-taper bars`() = runTest {
        val program = ProgramEntity(id = 1L, name = "V-Taper Plan", daysPerWeek = 4)
        val day = ProgramDayEntity(id = 10L, programId = 1L, dayNumber = 1, name = "Pull A", isRestDay = false)
        val exercise = ProgramExerciseEntity(id = 100L, programDayId = 10L, exerciseId = 500L, orderIndex = 0, sets = 3, targetReps = "8-12", restSeconds = 90)

        every { programRepository.getActiveProgram() } returns flowOf(program)
        every { programRepository.getDaysForProgram(1L) } returns flowOf(listOf(day))
        every { programRepository.getExercisesForDays(listOf(10L)) } returns flowOf(mapOf(10L to listOf(exercise)))

        every { workoutRepository.getCompletedWorkouts() } returns flowOf(emptyList())
        every { workoutRepository.getCompletedSetsWithContext(any()) } returns flowOf(emptyList())
        every { exerciseRepository.getAllExercises() } returns flowOf(emptyList())
        every { exerciseRepository.getAllExerciseMuscleDetails() } returns flowOf(emptyList())

        val viewModel = HomeViewModel(programRepository, workoutRepository, exerciseRepository, volumeCalculator, analyticsRepository)
        val state = viewModel.uiState.value

        assertTrue(state.hasProgram)
        assertEquals(4, state.vtaperBars.size)
        assertTrue("All V-Taper bars must be 0.0 with no completed workouts", state.vtaperBars.all { it.current == 0.0 })
    }

    @Test
    fun `completed workout this week computes legs volume as average lower body effective sets`() = runTest {
        val program = ProgramEntity(id = 1L, name = "V-Taper Plan", daysPerWeek = 4)
        val day = ProgramDayEntity(id = 10L, programId = 1L, dayNumber = 1, name = "Leg Day", isRestDay = false)

        val exQuad = Exercise(id = 501L, name = "Leg Extension", description = "", muscleGroup = "Quadriceps", equipment = "machine", difficulty = "Beginner")
        val exHam = Exercise(id = 502L, name = "Leg Curl", description = "", muscleGroup = "Hamstrings", equipment = "machine", difficulty = "Beginner")

        val assignments = listOf(
            ExerciseMuscleAssignment(exerciseId = 501L, muscleName = "Quadriceps", role = "primary"),
            ExerciseMuscleAssignment(exerciseId = 502L, muscleName = "Hamstrings", role = "primary")
        )

        val now = System.currentTimeMillis()
        val workouts = listOf(
            WorkoutWithStats(id = 1000L, date = Instant.ofEpochMilli(now), startTime = Instant.ofEpochMilli(now - 3600000), endTime = Instant.ofEpochMilli(now), duration = 3600, notes = "", completed = true, status = "COMPLETED", volume = 1000.0, setCount = 8, repCount = 80, exerciseCount = 2)
        )

        val completedSets = mutableListOf<CompletedSetContext>()
        repeat(4) { i -> completedSets.add(CompletedSetContext(setId = i + 1L, exerciseId = 501L, workoutDate = now, weightKg = 50.0, reps = 10, rpe = 8f, completed = true, setType = 0)) }
        repeat(4) { i -> completedSets.add(CompletedSetContext(setId = i + 10L, exerciseId = 502L, workoutDate = now, weightKg = 40.0, reps = 10, rpe = 8f, completed = true, setType = 0)) }

        every { programRepository.getActiveProgram() } returns flowOf(program)
        every { programRepository.getDaysForProgram(1L) } returns flowOf(listOf(day))
        every { programRepository.getExercisesForDays(listOf(10L)) } returns flowOf(emptyMap())

        every { workoutRepository.getCompletedWorkouts() } returns flowOf(workouts)
        every { workoutRepository.getCompletedSetsWithContext(any()) } returns flowOf(completedSets)
        every { exerciseRepository.getAllExercises() } returns flowOf(listOf(exQuad, exHam))
        every { exerciseRepository.getAllExerciseMuscleDetails() } returns flowOf(assignments)

        val viewModel = HomeViewModel(programRepository, workoutRepository, exerciseRepository, volumeCalculator, analyticsRepository)
        val state = viewModel.uiState.value

        val legsBar = state.vtaperBars.find { it.label == "Legs" }
        assertTrue("Legs bar must exist", legsBar != null)
        assertEquals("Legs volume should equal average across lower body muscles (8.0 total / 4 = 2.0)", 2.0, legsBar!!.current, 0.001)
        assertEquals("Target sets should equal 14", 14, legsBar.target)
    }

    @Test
    fun `asymmetric lower body distribution computes exact average effective volume`() = runTest {
        val program = ProgramEntity(id = 1L, name = "V-Taper Plan", daysPerWeek = 4)
        val day = ProgramDayEntity(id = 10L, programId = 1L, dayNumber = 1, name = "Leg Day", isRestDay = false)

        val exQuad = Exercise(id = 701L, name = "Squat", description = "", muscleGroup = "Quadriceps", equipment = "barbell", difficulty = "Intermediate")
        val exHam = Exercise(id = 702L, name = "RDL", description = "", muscleGroup = "Hamstrings", equipment = "barbell", difficulty = "Intermediate")
        val exGlute = Exercise(id = 703L, name = "Hip Thrust", description = "", muscleGroup = "Glutes", equipment = "barbell", difficulty = "Intermediate")

        val assignments = listOf(
            ExerciseMuscleAssignment(exerciseId = 701L, muscleName = "Quadriceps", role = "primary"),
            ExerciseMuscleAssignment(exerciseId = 702L, muscleName = "Hamstrings", role = "primary"),
            ExerciseMuscleAssignment(exerciseId = 703L, muscleName = "Glutes", role = "primary")
        )

        val now = System.currentTimeMillis()
        val workouts = listOf(
            WorkoutWithStats(id = 3000L, date = Instant.ofEpochMilli(now), startTime = Instant.ofEpochMilli(now - 3600000), endTime = Instant.ofEpochMilli(now), duration = 3600, notes = "", completed = true, status = "COMPLETED", volume = 3000.0, setCount = 24, repCount = 240, exerciseCount = 3)
        )

        // Quads = 12 sets, Hamstrings = 8 sets, Glutes = 4 sets, Calves = 0 sets
        // Total = 24.0 effective sets / 4 lower body muscle groups = 6.0 average effective sets
        val completedSets = mutableListOf<CompletedSetContext>()
        repeat(12) { i -> completedSets.add(CompletedSetContext(setId = i + 1L, exerciseId = 701L, workoutDate = now, weightKg = 100.0, reps = 8, rpe = 8f, completed = true, setType = 0)) }
        repeat(8) { i -> completedSets.add(CompletedSetContext(setId = i + 100L, exerciseId = 702L, workoutDate = now, weightKg = 90.0, reps = 8, rpe = 8f, completed = true, setType = 0)) }
        repeat(4) { i -> completedSets.add(CompletedSetContext(setId = i + 200L, exerciseId = 703L, workoutDate = now, weightKg = 110.0, reps = 8, rpe = 8f, completed = true, setType = 0)) }

        every { programRepository.getActiveProgram() } returns flowOf(program)
        every { programRepository.getDaysForProgram(1L) } returns flowOf(listOf(day))
        every { programRepository.getExercisesForDays(listOf(10L)) } returns flowOf(emptyMap())

        every { workoutRepository.getCompletedWorkouts() } returns flowOf(workouts)
        every { workoutRepository.getCompletedSetsWithContext(any()) } returns flowOf(completedSets)
        every { exerciseRepository.getAllExercises() } returns flowOf(listOf(exQuad, exHam, exGlute))
        every { exerciseRepository.getAllExerciseMuscleDetails() } returns flowOf(assignments)

        val viewModel = HomeViewModel(programRepository, workoutRepository, exerciseRepository, volumeCalculator, analyticsRepository)
        val state = viewModel.uiState.value

        val legsBar = state.vtaperBars.find { it.label == "Legs" }
        assertEquals("Asymmetric lower body (12+8+4+0 = 24 / 4) must equal 6.0 average effective sets", 6.0, legsBar!!.current, 0.001)
    }

    @Test
    fun `historical workout outside current week does not contribute to current week bars`() = runTest {
        val program = ProgramEntity(id = 1L, name = "V-Taper Plan", daysPerWeek = 4)
        val day = ProgramDayEntity(id = 10L, programId = 1L, dayNumber = 1, name = "Pull Day", isRestDay = false)

        val exLat = Exercise(id = 601L, name = "Lat Pulldown", description = "", muscleGroup = "Lats", equipment = "cable", difficulty = "Beginner")
        val assignments = listOf(ExerciseMuscleAssignment(exerciseId = 601L, muscleName = "Lats", role = "primary"))

        val oldDate = System.currentTimeMillis() - (21L * 86400000L)
        val oldWorkouts = listOf(
            WorkoutWithStats(id = 2000L, date = Instant.ofEpochMilli(oldDate), startTime = Instant.ofEpochMilli(oldDate), endTime = Instant.ofEpochMilli(oldDate + 3600000), duration = 3600, notes = "", completed = true, status = "COMPLETED", volume = 2000.0, setCount = 4, repCount = 40, exerciseCount = 1)
        )

        every { programRepository.getActiveProgram() } returns flowOf(program)
        every { programRepository.getDaysForProgram(1L) } returns flowOf(listOf(day))
        every { programRepository.getExercisesForDays(listOf(10L)) } returns flowOf(emptyMap())

        every { workoutRepository.getCompletedWorkouts() } returns flowOf(oldWorkouts)
        every { workoutRepository.getCompletedSetsWithContext(any()) } returns flowOf(emptyList())
        every { exerciseRepository.getAllExercises() } returns flowOf(listOf(exLat))
        every { exerciseRepository.getAllExerciseMuscleDetails() } returns flowOf(assignments)

        val viewModel = HomeViewModel(programRepository, workoutRepository, exerciseRepository, volumeCalculator, analyticsRepository)
        val state = viewModel.uiState.value

        val latsBar = state.vtaperBars.find { it.label == "Lats" }
        assertEquals("Historical workout outside current week must not contribute to current week bars", 0.0, latsBar!!.current, 0.001)
    }
}
