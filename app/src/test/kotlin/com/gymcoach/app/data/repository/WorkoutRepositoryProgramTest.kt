package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.ProgramDayDao
import com.gymcoach.app.data.local.dao.ProgramExerciseDao
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.entity.ProgramDayEntity
import com.gymcoach.app.data.local.entity.ProgramExerciseEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class WorkoutRepositoryProgramTest {

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
    fun `createWorkoutFromProgramDay returns null if program day is not found`() = runTest {
        coEvery { programDayDao.getById(1L) } returns null

        val result = repository.createWorkoutFromProgramDay(1L)

        assertNull(result)
        coVerify(exactly = 0) { programExerciseDao.getByDayId(any()) }
        coVerify(exactly = 0) { workoutDao.createWorkoutFromProgramDayTransaction(any(), any()) }
    }

    @Test
    fun `createWorkoutFromProgramDay delegates to transaction if program day is found`() = runTest {
        val programDay = ProgramDayEntity(id = 1L, programId = 100L, dayNumber = 1, name = "Push Day", targetMuscles = "Chest", isRestDay = false)
        val programExercises = listOf(
            ProgramExerciseEntity(id = 10L, programDayId = 1L, exerciseId = 50L, orderIndex = 0, sets = 3, targetReps = "10", restSeconds = 60)
        )

        coEvery { programDayDao.getById(1L) } returns programDay
        every { programExerciseDao.getByDayId(1L) } returns flowOf(programExercises)
        coEvery { workoutDao.createWorkoutFromProgramDayTransaction(programDay, programExercises) } returns 99L

        val result = repository.createWorkoutFromProgramDay(1L)

        assertEquals(99L, result)
        coVerify(exactly = 1) { workoutDao.createWorkoutFromProgramDayTransaction(programDay, programExercises) }
    }

    @Test
    fun `createWorkoutFromProgramDay handles empty exercises list safely`() = runTest {
        val emptyProgramDay = ProgramDayEntity(id = 2L, programId = 100L, dayNumber = 2, name = "Active Mobility", targetMuscles = "Core", isRestDay = false)
        coEvery { programDayDao.getById(2L) } returns emptyProgramDay
        every { programExerciseDao.getByDayId(2L) } returns flowOf(emptyList())
        coEvery { workoutDao.createWorkoutFromProgramDayTransaction(emptyProgramDay, emptyList()) } returns 101L

        val result = repository.createWorkoutFromProgramDay(2L)

        assertEquals(101L, result)
        coVerify(exactly = 1) { workoutDao.createWorkoutFromProgramDayTransaction(emptyProgramDay, emptyList()) }
    }

    @Test
    fun `createWorkoutFromProgramDay handles rest day flag properly`() = runTest {
        val restDay = ProgramDayEntity(id = 3L, programId = 100L, dayNumber = 3, name = "Rest & Recovery", targetMuscles = "", isRestDay = true)
        coEvery { programDayDao.getById(3L) } returns restDay
        every { programExerciseDao.getByDayId(3L) } returns flowOf(emptyList())
        coEvery { workoutDao.createWorkoutFromProgramDayTransaction(restDay, emptyList()) } returns 102L

        val result = repository.createWorkoutFromProgramDay(3L)

        assertEquals(102L, result)
        coVerify(exactly = 1) { workoutDao.createWorkoutFromProgramDayTransaction(restDay, emptyList()) }
    }
}
