package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutEntity
import com.gymcoach.app.data.local.entity.WorkoutExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class WorkoutRepositoryPerformAgainTest {

    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var repository: WorkoutRepositoryImpl

    private val sourceWorkoutId = 100L
    private val newWorkoutId = 200L
    private val newWorkoutExerciseId = 300L

    private val sourceWorkoutEntity = WorkoutEntity(
        id = sourceWorkoutId,
        date = 1600000000000L,
        startTime = 1600000000000L,
        endTime = 1600003600000L,
        duration = 3600L,
        notes = "Leg Day Heavy",
        completed = true,
        status = "COMPLETED"
    )

    private val sourceWorkoutExerciseEntity = WorkoutExerciseEntity(
        id = 10L,
        workoutId = sourceWorkoutId,
        exerciseId = 1L,
        orderIndex = 0
    )

    private val exerciseEntity = ExerciseEntity(
        id = 1L,
        name = "Squat",
        description = "Leg exercise",
        muscleGroup = "Legs",
        equipment = "Barbell",
        difficulty = "Intermediate"
    )

    private val sourceSet1 = WorkoutSetEntity(
        id = 101L,
        workoutExerciseId = 10L,
        setNumber = 1,
        weight = 100.0,
        reps = 5,
        rpe = 8.0,
        restSeconds = 180,
        completed = true,
        setType = 0
    )

    private val sourceSet2 = WorkoutSetEntity(
        id = 102L,
        workoutExerciseId = 10L,
        setNumber = 2,
        weight = 105.0,
        reps = 5,
        rpe = 9.0,
        restSeconds = 180,
        completed = true,
        setType = 0
    )

    @Before
    fun setup() {
        workoutDao = mockk(relaxed = true)
        exerciseDao = mockk(relaxed = true)
        repository = WorkoutRepositoryImpl(workoutDao, exerciseDao)
    }

    @Test
    fun `createWorkoutFromHistory generates new ID and clones structure with fresh completion state`() = runTest {
        every { workoutDao.getWorkoutById(sourceWorkoutId) } returns flowOf(sourceWorkoutEntity)
        every { workoutDao.getExercisesForWorkout(sourceWorkoutId) } returns flowOf(listOf(sourceWorkoutExerciseEntity))
        every { exerciseDao.getById(1L) } returns flowOf(exerciseEntity)
        every { workoutDao.getSetsForExercise(10L) } returns flowOf(listOf(sourceSet1, sourceSet2))

        val insertedWorkoutSlot = slot<WorkoutEntity>()
        coEvery { workoutDao.insertWorkout(capture(insertedWorkoutSlot)) } returns newWorkoutId

        val insertedExerciseSlot = slot<WorkoutExerciseEntity>()
        coEvery { workoutDao.insertWorkoutExercise(capture(insertedExerciseSlot)) } returns newWorkoutExerciseId

        val insertedSets = mutableListOf<WorkoutSetEntity>()
        coEvery { workoutDao.insertWorkoutSet(capture(insertedSets)) } returns 999L

        val resultId = repository.createWorkoutFromHistory(sourceWorkoutId)

        assertEquals(newWorkoutId, resultId)

        // Verify inserted Workout entity
        assertEquals(0L, insertedWorkoutSlot.captured.id) // Room auto-generates
        assertEquals("Leg Day Heavy", insertedWorkoutSlot.captured.notes)
        assertFalse(insertedWorkoutSlot.captured.completed)
        assertEquals("ACTIVE", insertedWorkoutSlot.captured.status)
        assertEquals(0L, insertedWorkoutSlot.captured.duration)

        // Verify inserted Exercise entity
        assertEquals(newWorkoutId, insertedExerciseSlot.captured.workoutId)
        assertEquals(1L, insertedExerciseSlot.captured.exerciseId)
        assertEquals(0, insertedExerciseSlot.captured.orderIndex)

        // Verify inserted Sets
        assertEquals(2, insertedSets.size)
        assertEquals(newWorkoutExerciseId, insertedSets[0].workoutExerciseId)
        assertEquals(100.0, insertedSets[0].weight, 0.01)
        assertEquals(5, insertedSets[0].reps)
        assertFalse(insertedSets[0].completed) // Fresh execution state

        assertEquals(newWorkoutExerciseId, insertedSets[1].workoutExerciseId)
        assertEquals(105.0, insertedSets[1].weight, 0.01)
        assertEquals(5, insertedSets[1].reps)
        assertFalse(insertedSets[1].completed) // Fresh execution state

        // Verify source historical data remained untouched
        coVerify(exactly = 0) { workoutDao.updateWorkout(any()) }
        coVerify(exactly = 0) { workoutDao.deleteWorkout(any()) }
    }

    @Test
    fun `createWorkoutFromHistory returns null when source workout is not found`() = runTest {
        every { workoutDao.getWorkoutById(999L) } returns flowOf(null)

        val resultId = repository.createWorkoutFromHistory(999L)

        assertNull(resultId)
        coVerify(exactly = 0) { workoutDao.insertWorkout(any()) }
    }
}
