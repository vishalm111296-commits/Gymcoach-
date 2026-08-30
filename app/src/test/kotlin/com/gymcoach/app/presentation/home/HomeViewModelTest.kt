package com.gymcoach.app.presentation.home

import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.data.local.entity.ProgramDayEntity
import com.gymcoach.app.data.local.entity.ProgramExerciseEntity
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.repository.AnalyticsRepository
import com.gymcoach.app.domain.repository.ExerciseRepository
import com.gymcoach.app.domain.repository.ProgramRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var programRepository: ProgramRepository
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var volumeCalculator: VolumeCalculator
    private lateinit var analyticsRepository: AnalyticsRepository
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var viewModel: HomeViewModel

    private val backEx = Exercise(
        id = 101, name = "Barbell Row", description = "", muscleGroup = "Back",
        equipment = "barbell", difficulty = "Intermediate", vtaperLat = 9
    )
    private val chestEx = Exercise(
        id = 102, name = "Bench Press", description = "", muscleGroup = "Chest",
        equipment = "barbell", difficulty = "Beginner", vtaperUpperChest = 8
    )
    private val latDeltEx = Exercise(
        id = 103, name = "Cable Lateral Raise", description = "", muscleGroup = "shoulders",
        equipment = "cable", difficulty = "Beginner", secondaryMuscles = "lateral_deltoid"
    )
    private val quadEx = Exercise(
        id = 104, name = "Barbell Squat", description = "", muscleGroup = "Legs",
        equipment = "barbell", difficulty = "Intermediate", secondaryMuscles = "quadriceps"
    )
    private val hamsEx = Exercise(
        id = 105, name = "Romanian Deadlift", description = "", muscleGroup = "Legs",
        equipment = "barbell", difficulty = "Intermediate", secondaryMuscles = "hamstrings"
    )

    private val exerciseMap = listOf(backEx, chestEx, latDeltEx, quadEx, hamsEx).associateBy { it.id }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        programRepository = mockk()
        workoutRepository = mockk()
        volumeCalculator = VolumeCalculator()
        analyticsRepository = mockk()
        exerciseRepository = mockk()

        coEvery { analyticsRepository.getAllPersonalRecords() } returns emptyList()
        coEvery { workoutRepository.getCompletedWorkouts() } returns flowOf(emptyList())
        coEvery { exerciseRepository.getAllExercises() } returns flowOf(exerciseMap.values.toList())
        coEvery { programRepository.getActiveProgram() } returns flowOf(null)

        viewModel = HomeViewModel(
            programRepository,
            workoutRepository,
            volumeCalculator,
            analyticsRepository,
            exerciseRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `TEST 1 - Day Volume Attribution attributes sets to individual exercises not entire day total`() {
        val day = ProgramDayEntity(id = 1, programId = 1, dayNumber = 1, name = "Upper", targetMuscles = "Back,Chest,Lateral Deltoid")
        val exercises = listOf(
            ProgramExerciseEntity(id = 1, programDayId = 1, exerciseId = 101, orderIndex = 0, sets = 3, targetReps = "8-12", restSeconds = 90),
            ProgramExerciseEntity(id = 2, programDayId = 1, exerciseId = 101, orderIndex = 1, sets = 3, targetReps = "8-12", restSeconds = 90),
            ProgramExerciseEntity(id = 3, programDayId = 1, exerciseId = 102, orderIndex = 2, sets = 3, targetReps = "8-12", restSeconds = 90),
            ProgramExerciseEntity(id = 4, programDayId = 1, exerciseId = 102, orderIndex = 3, sets = 3, targetReps = "8-12", restSeconds = 90),
            ProgramExerciseEntity(id = 5, programDayId = 1, exerciseId = 103, orderIndex = 4, sets = 3, targetReps = "8-12", restSeconds = 90),
            ProgramExerciseEntity(id = 6, programDayId = 1, exerciseId = 103, orderIndex = 5, sets = 3, targetReps = "8-12", restSeconds = 90)
        )

        val plannedSets = viewModel.plannedWeeklySets(
            exercisesByDay = mapOf(1L to exercises),
            days = listOf(day),
            exerciseMap = exerciseMap
        )

        assertEquals("Back should receive exactly 6 sets", 6, plannedSets["Back"])
        assertEquals("Chest should receive exactly 6 sets", 6, plannedSets["Chest"])
        assertEquals("Lateral Deltoid should receive exactly 6 sets", 6, plannedSets["Lateral Deltoid"])
    }

    @Test
    fun `TEST 2 - No Day-Level Broadcast proves total day sets is not assigned to every muscle tag`() {
        val day = ProgramDayEntity(id = 1, programId = 1, dayNumber = 1, name = "Upper", targetMuscles = "Back,Chest,Lateral Deltoid")
        val exercises = listOf(
            ProgramExerciseEntity(id = 1, programDayId = 1, exerciseId = 101, orderIndex = 0, sets = 3, targetReps = "8-12", restSeconds = 90),
            ProgramExerciseEntity(id = 2, programDayId = 1, exerciseId = 102, orderIndex = 1, sets = 3, targetReps = "8-12", restSeconds = 90)
        )
        val totalDaySets = 6 // 3 + 3

        val plannedSets = viewModel.plannedWeeklySets(
            exercisesByDay = mapOf(1L to exercises),
            days = listOf(day),
            exerciseMap = exerciseMap
        )

        assertEquals("Back gets its 3 sets", 3, plannedSets["Back"])
        assertEquals("Chest gets its 3 sets", 3, plannedSets["Chest"])
        assertNotEquals("Back sets must not equal total day sets when multiple muscles exist", totalDaySets, plannedSets["Back"])
    }

    @Test
    fun `TEST 5 and 6 - V-Taper metrics and Legs aggregation match hand-calculated expected values`() {
        val upperA = ProgramDayEntity(id = 1, programId = 1, dayNumber = 1, name = "Upper A", targetMuscles = "Back,Chest,Lateral Deltoid")
        val lowerA = ProgramDayEntity(id = 2, programId = 1, dayNumber = 2, name = "Lower A", targetMuscles = "Quadriceps,Hamstrings")

        val upperExercises = listOf(
            ProgramExerciseEntity(id = 1, programDayId = 1, exerciseId = 101, orderIndex = 0, sets = 4, targetReps = "8-12", restSeconds = 90),
            ProgramExerciseEntity(id = 2, programDayId = 1, exerciseId = 102, orderIndex = 1, sets = 4, targetReps = "8-12", restSeconds = 90),
            ProgramExerciseEntity(id = 3, programDayId = 1, exerciseId = 103, orderIndex = 2, sets = 4, targetReps = "8-12", restSeconds = 90)
        )
        val lowerExercises = listOf(
            ProgramExerciseEntity(id = 4, programDayId = 2, exerciseId = 104, orderIndex = 0, sets = 4, targetReps = "8-12", restSeconds = 90),
            ProgramExerciseEntity(id = 5, programDayId = 2, exerciseId = 105, orderIndex = 1, sets = 4, targetReps = "8-12", restSeconds = 90)
        )

        val plannedSets = viewModel.plannedWeeklySets(
            exercisesByDay = mapOf(1L to upperExercises, 2L to lowerExercises),
            days = listOf(upperA, lowerA),
            exerciseMap = exerciseMap
        )

        assertEquals(4, plannedSets["Back"])
        assertEquals(4, plannedSets["Chest"])
        assertEquals(4, plannedSets["Lateral Deltoid"])
        assertEquals(4, plannedSets["Quadriceps"])
        assertEquals(4, plannedSets["Hamstrings"])

        val legsTotal = listOf("Quadriceps", "Hamstrings", "Glutes", "Calves").sumOf { plannedSets[it] ?: 0 }
        assertEquals("Legs total should be 8 sets (4 quads + 4 hams)", 8, legsTotal)
    }

    @Test
    fun `primaryMuscleSlot classifies lateral_deltoid in secondaryMuscles as Lateral Deltoid not Back`() {
        val latDeltSecEx = Exercise(
            id = 201, name = "Special Cable Raise", description = "", muscleGroup = "shoulders",
            equipment = "cable", difficulty = "Beginner", secondaryMuscles = "lateral_deltoid"
        )
        val slot = HomeViewModel.primaryMuscleSlot(latDeltSecEx)
        assertEquals("Lateral Deltoid", slot)
    }
}
