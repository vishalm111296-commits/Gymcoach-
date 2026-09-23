package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.dao.WorkoutTemplateDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.TemplateExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutTemplateEntity
import com.gymcoach.app.domain.model.TemplateExercise
import com.gymcoach.app.domain.model.WorkoutTemplate
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class WorkoutTemplateRepositoryTest {

    private lateinit var workoutTemplateDao: WorkoutTemplateDao
    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var repository: WorkoutTemplateRepositoryImpl

    private val templateEntity = WorkoutTemplateEntity(
        id = 10L,
        name = "Push Day",
        description = "Chest and Triceps",
        isArchived = false,
        version = 1,
        createdAt = 1000L,
        updatedAt = 1000L
    )

    private val templateExerciseEntity = TemplateExerciseEntity(
        id = 100L,
        templateId = 10L,
        exerciseId = 1L,
        orderIndex = 0,
        targetSets = 3,
        targetReps = "10",
        targetWeightKg = 80.0,
        targetRpe = 8.0,
        restSeconds = 90,
        notes = "Focus on form"
    )

    private val exerciseEntity = ExerciseEntity(
        id = 1L,
        name = "Bench Press",
        description = "Chest exercise",
        muscleGroup = "Chest",
        equipment = "Barbell",
        difficulty = "Intermediate"
    )

    @Before
    fun setup() {
        workoutTemplateDao = mockk(relaxed = true)
        workoutDao = mockk(relaxed = true)
        exerciseDao = mockk(relaxed = true)
        repository = WorkoutTemplateRepositoryImpl(workoutTemplateDao, workoutDao, exerciseDao)
    }

    @Test
    fun `getActiveTemplates returns mapped templates with batch fetched exercises`() = runTest {
        every { workoutTemplateDao.getActiveTemplates() } returns flowOf(listOf(templateEntity))
        every { workoutTemplateDao.getTemplateExercises(10L) } returns flowOf(listOf(templateExerciseEntity))
        coEvery { exerciseDao.getByIdsSync(listOf(1L)) } returns listOf(exerciseEntity)

        val result = repository.getActiveTemplates().first()

        assertEquals(1, result.size)
        val template = result.first()
        assertEquals(10L, template.id)
        assertEquals("Push Day", template.name)
        assertEquals(1, template.exercises.size)
        val ex = template.exercises.first()
        assertEquals("Bench Press", ex.exerciseName)
        assertEquals("Chest", ex.muscleGroup)
        assertEquals("Barbell", ex.equipment)
    }

    @Test
    fun `getTemplateById returns mapped template when found`() = runTest {
        every { workoutTemplateDao.getTemplateById(10L) } returns flowOf(templateEntity)
        every { workoutTemplateDao.getTemplateExercises(10L) } returns flowOf(listOf(templateExerciseEntity))
        coEvery { exerciseDao.getByIdsSync(listOf(1L)) } returns listOf(exerciseEntity)

        val result = repository.getTemplateById(10L).first()

        assertNotNull(result)
        assertEquals("Push Day", result?.name)
        assertEquals(1, result?.exercises?.size)
    }

    @Test
    fun `getTemplateById returns null when template not found`() = runTest {
        every { workoutTemplateDao.getTemplateById(999L) } returns flowOf(null)

        val result = repository.getTemplateById(999L).first()

        assertNull(result)
    }

    @Test
    fun `saveTemplate delegates to saveTemplateAtomic with correct arguments`() = runTest {
        coEvery { workoutTemplateDao.saveTemplateAtomic(any(), any(), any()) } returns 10L

        val domainTemplate = WorkoutTemplate(
            id = 10L,
            name = "Push Day",
            description = "Chest and Triceps",
            isArchived = false,
            version = 1,
            createdAt = 1000L,
            updatedAt = 1000L,
            exercises = emptyList()
        )
        val exercises = listOf(
            TemplateExercise(
                id = 100L,
                templateId = 10L,
                exerciseId = 1L,
                exerciseName = "Bench Press",
                muscleGroup = "Chest",
                equipment = "Barbell",
                orderIndex = 0,
                targetSets = 3,
                targetReps = "10",
                targetWeightKg = 80.0,
                targetRpe = 8.0,
                restSeconds = 90,
                notes = "Focus on form"
            )
        )

        val id = repository.saveTemplate(domainTemplate, exercises)

        assertEquals(10L, id)
        coVerify(exactly = 1) {
            workoutTemplateDao.saveTemplateAtomic(
                match { it.id == 10L && it.name == "Push Day" },
                match { it.size == 1 && it.first().exerciseId == 1L },
                isUpdate = true
            )
        }
    }

    @Test
    fun `duplicateTemplate delegates to duplicateTemplateAtomic`() = runTest {
        coEvery { workoutTemplateDao.duplicateTemplateAtomic(10L, any()) } returns 11L

        val duplicatedId = repository.duplicateTemplate(10L)

        assertEquals(11L, duplicatedId)
        coVerify(exactly = 1) { workoutTemplateDao.duplicateTemplateAtomic(10L, any()) }
    }

    @Test
    fun `archiveTemplate updates entity with isArchived true`() = runTest {
        coEvery { workoutTemplateDao.archiveTemplate(10L, any()) } returns 1

        repository.archiveTemplate(10L)

        coVerify(exactly = 1) {
            workoutTemplateDao.archiveTemplate(10L, any())
        }
    }
}
