package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.ProgramDayDao
import com.gymcoach.app.data.local.dao.ProgramExerciseDao
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutEntity
import com.gymcoach.app.data.local.entity.WorkoutExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.model.SetType
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutExercise
import com.gymcoach.app.domain.model.WorkoutExerciseWithSets
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutWithDetails
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class WorkoutRepositoryImportTest {

    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var programDayDao: ProgramDayDao
    private lateinit var programExerciseDao: ProgramExerciseDao
    private lateinit var repository: WorkoutRepositoryImpl

    private val testTimestamp = 1700000000000L

    private val benchPress = Exercise(
        id = 1L,
        name = "Bench Press",
        description = "Barbell Bench Press",
        muscleGroup = "Chest",
        equipment = "Barbell",
        difficulty = "Intermediate"
    )

    private val benchPressEntity = ExerciseEntity(
        id = 1L,
        name = "Bench Press",
        description = "Barbell Bench Press",
        muscleGroup = "Chest",
        equipment = "Barbell",
        difficulty = "Intermediate"
    )

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
    fun `importWorkouts successfully imports workout with existing exercise`() = runTest {
        val sets = listOf(
            WorkoutSet(id = 0, workoutExerciseId = 0, setNumber = 1, weight = 100.0, reps = 5, rpe = 8.0, restSeconds = 120, completed = true, setType = SetType.NORMAL),
            WorkoutSet(id = 0, workoutExerciseId = 0, setNumber = 2, weight = 100.0, reps = 5, rpe = 8.5, restSeconds = 120, completed = true, setType = SetType.NORMAL)
        )

        val workout = Workout(
            id = 0,
            date = Instant.ofEpochMilli(testTimestamp),
            startTime = Instant.ofEpochMilli(testTimestamp),
            endTime = Instant.ofEpochMilli(testTimestamp + 3600000),
            duration = 3600,
            notes = "Chest Day",
            completed = true
        )

        val workoutWithDetails = WorkoutWithDetails(
            workout = workout,
            exercises = listOf(
                WorkoutExerciseWithSets(
                    workoutExercise = WorkoutExercise(id = 0, workoutId = 0, exerciseId = 1L, orderIndex = 0),
                    exercise = benchPress,
                    sets = sets
                )
            )
        )

        coEvery { workoutDao.getWorkoutsByDateDirect(testTimestamp) } returns emptyList()
        coEvery { exerciseDao.getByName("Bench Press") } returns benchPressEntity
        coEvery { workoutDao.importSingleWorkoutTransaction(any(), any()) } returns 101L

        val result = repository.importWorkouts(listOf(workoutWithDetails))

        assertTrue(result.isSuccess)
        val stats = result.getOrThrow()
        assertEquals(1, stats.workoutsImported)
        assertEquals(0, stats.workoutsSkipped)
        assertEquals(2, stats.setsImported)

        coVerify(exactly = 1) { workoutDao.importSingleWorkoutTransaction(any(), any()) }
    }

    @Test
    fun `importWorkouts creates custom exercise when exercise not in catalog`() = runTest {
        val unknownExercise = Exercise(
            id = 0L,
            name = "Dragon Flag",
            description = "Advanced core movement",
            muscleGroup = "Abs",
            equipment = "Bodyweight",
            difficulty = "Advanced"
        )

        val sets = listOf(
            WorkoutSet(id = 0, workoutExerciseId = 0, setNumber = 1, weight = 0.0, reps = 12, rpe = 9.0, restSeconds = 60, completed = true, setType = SetType.NORMAL)
        )

        val workout = Workout(
            id = 0,
            date = Instant.ofEpochMilli(testTimestamp),
            startTime = Instant.ofEpochMilli(testTimestamp),
            endTime = Instant.ofEpochMilli(testTimestamp + 1800000),
            duration = 1800,
            notes = "Core blast",
            completed = true
        )

        val workoutWithDetails = WorkoutWithDetails(
            workout = workout,
            exercises = listOf(
                WorkoutExerciseWithSets(
                    workoutExercise = WorkoutExercise(id = 0, workoutId = 0, exerciseId = 0L, orderIndex = 0),
                    exercise = unknownExercise,
                    sets = sets
                )
            )
        )

        coEvery { workoutDao.getWorkoutsByDateDirect(testTimestamp) } returns emptyList()
        coEvery { exerciseDao.getByName("Dragon Flag") } returns null
        coEvery { exerciseDao.insert(match { it.name == "Dragon Flag" && it.isCustom }) } returns 999L
        coEvery { workoutDao.importSingleWorkoutTransaction(any(), any()) } returns 102L

        val result = repository.importWorkouts(listOf(workoutWithDetails))

        assertTrue(result.isSuccess)
        val stats = result.getOrThrow()
        assertEquals(1, stats.workoutsImported)
        assertEquals(0, stats.workoutsSkipped)
        assertEquals(1, stats.setsImported)

        coVerify(exactly = 1) { exerciseDao.insert(match { it.name == "Dragon Flag" && it.isCustom }) }
    }

    @Test
    fun `importWorkouts skips duplicate workouts with identical date and exercise composition`() = runTest {
        val sets = listOf(
            WorkoutSet(id = 0, workoutExerciseId = 0, setNumber = 1, weight = 100.0, reps = 5, rpe = 8.0, restSeconds = 120, completed = true, setType = SetType.NORMAL)
        )

        val workout = Workout(
            id = 0,
            date = Instant.ofEpochMilli(testTimestamp),
            startTime = Instant.ofEpochMilli(testTimestamp),
            endTime = Instant.ofEpochMilli(testTimestamp + 3600000),
            duration = 3600,
            notes = "Chest Day",
            completed = true
        )

        val workoutWithDetails = WorkoutWithDetails(
            workout = workout,
            exercises = listOf(
                WorkoutExerciseWithSets(
                    workoutExercise = WorkoutExercise(id = 0, workoutId = 0, exerciseId = 1L, orderIndex = 0),
                    exercise = benchPress,
                    sets = sets
                )
            )
        )

        val existingWorkout = WorkoutEntity(
            id = 50L,
            date = testTimestamp,
            startTime = testTimestamp,
            endTime = testTimestamp + 3600000,
            duration = 3600,
            notes = "Chest Day",
            completed = true,
            status = "COMPLETED"
        )

        val existingExerciseEntity = WorkoutExerciseEntity(
            id = 10L,
            workoutId = 50L,
            exerciseId = 1L,
            orderIndex = 0
        )

        coEvery { workoutDao.getWorkoutsByDateDirect(testTimestamp) } returns listOf(existingWorkout)
        coEvery { exerciseDao.getByName("Bench Press") } returns benchPressEntity
        coEvery { workoutDao.getExercisesForWorkoutDirect(50L) } returns listOf(existingExerciseEntity)

        val result = repository.importWorkouts(listOf(workoutWithDetails))

        assertTrue(result.isSuccess)
        val stats = result.getOrThrow()
        assertEquals(0, stats.workoutsImported)
        assertEquals(1, stats.workoutsSkipped)
        assertEquals(0, stats.setsImported)

        coVerify(exactly = 0) { workoutDao.importSingleWorkoutTransaction(any(), any()) }
    }
}
