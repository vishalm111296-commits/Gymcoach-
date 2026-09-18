package com.gymcoach.app.core.export

import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.ProgramDayDao
import com.gymcoach.app.data.local.dao.ProgramExerciseDao
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutEntity
import com.gymcoach.app.data.local.entity.WorkoutExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import com.gymcoach.app.data.repository.WorkoutRepositoryImpl
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.model.SetType
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutExercise
import com.gymcoach.app.domain.model.WorkoutExerciseWithSets
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutWithDetails
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class WorkoutRoundtripIntegrationTest {

    private val exporter = WorkoutDataExporter()
    private val importer = WorkoutDataImporter()

    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var programDayDao: ProgramDayDao
    private lateinit var programExerciseDao: ProgramExerciseDao
    private lateinit var repository: WorkoutRepositoryImpl

    @Before
    fun setup() {
        workoutDao = mockk(relaxed = true)
        exerciseDao = mockk(relaxed = true)
        programDayDao = mockk(relaxed = true)
        programExerciseDao = mockk(relaxed = true)

        repository = WorkoutRepositoryImpl(
            workoutDao = workoutDao,
            exerciseDao = exerciseDao,
            programDayDao = programDayDao,
            programExerciseDao = programExerciseDao
        )
    }

    @Test
    fun `end-to-end roundtrip export, parse, and repository import preserves data fidelity`() = runTest {
        val exercise1 = Exercise(
            id = 1L,
            name = "Incline Dumbbell Press",
            description = "Upper chest pressing movement",
            muscleGroup = "Chest",
            equipment = "Dumbbells",
            difficulty = "Intermediate"
        )

        val exercise2 = Exercise(
            id = 2L,
            name = "Weighted Pull-Up",
            description = "Back vertical pull",
            muscleGroup = "Back",
            equipment = "Pull-up Bar",
            difficulty = "Advanced"
        )

        val nowEpoch = 1705000000000L
        val startTime = Instant.ofEpochMilli(nowEpoch)
        val endTime = Instant.ofEpochMilli(nowEpoch + 4200000L) // 70 minutes

        val sets1 = listOf(
            WorkoutSet(id = 1, workoutExerciseId = 10, setNumber = 1, weight = 24.0, reps = 12, rpe = 6.0, restSeconds = 90, completed = true, setType = SetType.WARMUP),
            WorkoutSet(id = 2, workoutExerciseId = 10, setNumber = 2, weight = 34.0, reps = 10, rpe = 8.5, restSeconds = 120, completed = true, setType = SetType.NORMAL),
            WorkoutSet(id = 3, workoutExerciseId = 10, setNumber = 3, weight = 38.0, reps = 7, rpe = 9.5, restSeconds = 120, completed = true, setType = SetType.NORMAL),
            WorkoutSet(id = 4, workoutExerciseId = 10, setNumber = 4, weight = 22.0, reps = 15, rpe = 10.0, restSeconds = 60, completed = true, setType = SetType.DROP)
        )

        val sets2 = listOf(
            WorkoutSet(id = 5, workoutExerciseId = 11, setNumber = 1, weight = 20.0, reps = 6, rpe = 9.0, restSeconds = 180, completed = true, setType = SetType.NORMAL),
            WorkoutSet(id = 6, workoutExerciseId = 11, setNumber = 2, weight = 20.0, reps = 5, rpe = 10.0, restSeconds = 180, completed = true, setType = SetType.FAILURE)
        )

        val originalWorkout = Workout(
            id = 42L,
            date = startTime,
            startTime = startTime,
            endTime = endTime,
            duration = 4200L,
            notes = "Intense Upper Body Hypertrophy Session",
            completed = true
        )

        val originalWorkoutWithDetails = WorkoutWithDetails(
            workout = originalWorkout,
            exercises = listOf(
                WorkoutExerciseWithSets(
                    workoutExercise = WorkoutExercise(id = 10, workoutId = 42, exerciseId = 1, orderIndex = 0),
                    exercise = exercise1,
                    sets = sets1
                ),
                WorkoutExerciseWithSets(
                    workoutExercise = WorkoutExercise(id = 11, workoutId = 42, exerciseId = 2, orderIndex = 1),
                    exercise = exercise2,
                    sets = sets2
                )
            )
        )

        // Step 1: Export to JSON
        val exportedJson = exporter.exportToJson(listOf(originalWorkoutWithDetails))
        assertTrue(exportedJson.isNotBlank())

        // Step 2: Parse back via WorkoutDataImporter
        val parseResult = importer.parseJson(exportedJson)
        assertTrue(parseResult.isSuccess)
        val importedData = parseResult.getOrThrow()
        assertEquals(1, importedData.workouts.size)

        val parsedWorkout = importedData.workouts[0]
        assertEquals(originalWorkout.id, parsedWorkout.workout.id)
        assertEquals(originalWorkout.duration, parsedWorkout.workout.duration)
        assertEquals(originalWorkout.notes, parsedWorkout.workout.notes)
        assertEquals(originalWorkout.completed, parsedWorkout.workout.completed)
        assertEquals(2, parsedWorkout.exercises.size)

        // Check Exercise 1 fidelity
        val parsedEx1 = parsedWorkout.exercises[0]
        assertEquals("Incline Dumbbell Press", parsedEx1.exercise.name)
        assertEquals(4, parsedEx1.sets.size)
        assertEquals(SetType.WARMUP, parsedEx1.sets[0].setType)
        assertEquals(24.0, parsedEx1.sets[0].weight, 0.001)
        assertEquals(12, parsedEx1.sets[0].reps)
        assertEquals(SetType.DROP, parsedEx1.sets[3].setType)
        assertEquals(22.0, parsedEx1.sets[3].weight, 0.001)
        assertEquals(15, parsedEx1.sets[3].reps)

        // Check Exercise 2 fidelity
        val parsedEx2 = parsedWorkout.exercises[1]
        assertEquals("Weighted Pull-Up", parsedEx2.exercise.name)
        assertEquals(2, parsedEx2.sets.size)
        assertEquals(SetType.NORMAL, parsedEx2.sets[0].setType)
        assertEquals(SetType.FAILURE, parsedEx2.sets[1].setType)
        assertEquals(10.0, parsedEx2.sets[1].rpe, 0.001)

        // Step 3: Mock DAO responses for repository import
        val workoutEntitySlot = slot<WorkoutEntity>()
        val exercisesWithSetsSlot = slot<List<Pair<WorkoutExerciseEntity, List<WorkoutSetEntity>>>>()

        coEvery { workoutDao.getWorkoutsByDateDirect(nowEpoch) } returns emptyList()
        coEvery { exerciseDao.getByName("Incline Dumbbell Press") } returns ExerciseEntity(
            id = 1L,
            name = "Incline Dumbbell Press",
            description = "Upper chest pressing movement",
            muscleGroup = "Chest",
            equipment = "Dumbbells",
            difficulty = "Intermediate"
        )
        coEvery { exerciseDao.getByName("Weighted Pull-Up") } returns ExerciseEntity(
            id = 2L,
            name = "Weighted Pull-Up",
            description = "Back vertical pull",
            muscleGroup = "Back",
            equipment = "Pull-up Bar",
            difficulty = "Advanced"
        )
        coEvery {
            workoutDao.importSingleWorkoutTransaction(
                capture(workoutEntitySlot),
                capture(exercisesWithSetsSlot)
            )
        } returns 101L

        // Step 4: Import via WorkoutRepository
        val importResult = repository.importWorkouts(importedData.workouts)
        assertTrue(importResult.isSuccess)
        val stats = importResult.getOrThrow()
        assertEquals(1, stats.workoutsImported)
        assertEquals(0, stats.workoutsSkipped)
        assertEquals(6, stats.setsImported)

        // Step 5: Assert database transaction captured exact fidelity
        val capturedWorkout = workoutEntitySlot.captured
        assertEquals(nowEpoch, capturedWorkout.date)
        assertEquals(4200L, capturedWorkout.duration)
        assertEquals("Intense Upper Body Hypertrophy Session", capturedWorkout.notes)
        assertTrue(capturedWorkout.completed)
        assertEquals("COMPLETED", capturedWorkout.status)

        val capturedExercises = exercisesWithSetsSlot.captured
        assertEquals(2, capturedExercises.size)

        // Verify Set types in DB entity format
        val capturedSets1 = capturedExercises[0].second
        assertEquals(4, capturedSets1.size)
        assertEquals(SetType.WARMUP.ordinal, capturedSets1[0].setType)
        assertEquals(SetType.DROP.ordinal, capturedSets1[3].setType)

        val capturedSets2 = capturedExercises[1].second
        assertEquals(2, capturedSets2.size)
        assertEquals(SetType.NORMAL.ordinal, capturedSets2[0].setType)
        assertEquals(SetType.FAILURE.ordinal, capturedSets2[1].setType)
    }
}
