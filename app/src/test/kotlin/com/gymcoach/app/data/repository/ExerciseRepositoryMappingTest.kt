package com.gymcoach.app.data.repository

import com.gymcoach.app.core.program.VolumeCalculator.MuscleRole
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.dao.MuscleAssignmentRow
import com.gymcoach.app.domain.model.Exercise
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExerciseRepositoryMappingTest {

    private lateinit var dao: ExerciseDao
    private lateinit var repo: ExerciseRepositoryImpl

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        repo = ExerciseRepositoryImpl(dao)
    }

    @Test
    fun test_domainToEntityAndBackRoundTripAllFields() = runTest {
        val entity = ExerciseEntity(
            id = 42L,
            name = "Test Exercise",
            description = "A test description",
            muscleGroup = "Chest",
            equipment = "dumbbell,bench",
            difficulty = "intermediate",
            secondaryMuscles = "triceps,front_deltoid",
            instructions = "Full instructions text",
            tips = "Helpful tips",
            commonMistakes = "Common mistakes to avoid",
            safetyNotes = "Safety notes",
            recommendedRepRange = "8-12",
            recommendedRestTime = "120",
            estimatedCalories = 150,
            category = "chest",
            tags = "push,compound",
            isFavorite = true,
            lastViewed = 1700000000000L,
            vtaperLat = 5,
            vtaperLateralDelt = 3,
            vtaperUpperChest = 7,
            vtaperRearDelt = 2,
            movementPattern = "horizontal_push",
            imageUrl = "https://example.com/image.png",
            videoUrl = "https://example.com/video.mp4",
            animationUrl = "https://example.com/anim.gif",
            setupInstructions = "Setup instructions",
            executionInstructions = "Execution instructions",
            breathingInstructions = "Breathing instructions",
            tempoGuidance = "3-1-2-0",
            beginnerVariantId = 10L,
            advancedVariantId = 20L
        )

        every { dao.getAll() } returns flowOf(listOf(entity))

        val result = repo.getAllExercises().first()

        assertEquals(1, result.size)
        val domain = result[0]

        assertEquals(entity.id, domain.id)
        assertEquals(entity.name, domain.name)
        assertEquals(entity.description, domain.description)
        assertEquals(entity.muscleGroup, domain.muscleGroup)
        assertEquals(entity.equipment, domain.equipment)
        assertEquals(entity.difficulty, domain.difficulty)
        assertEquals(entity.secondaryMuscles, domain.secondaryMuscles)
        assertEquals(entity.instructions, domain.instructions)
        assertEquals(entity.tips, domain.tips)
        assertEquals(entity.commonMistakes, domain.commonMistakes)
        assertEquals(entity.safetyNotes, domain.safetyNotes)
        assertEquals(entity.recommendedRepRange, domain.recommendedRepRange)
        assertEquals(entity.recommendedRestTime, domain.recommendedRestTime)
        assertEquals(entity.estimatedCalories, domain.estimatedCalories)
        assertEquals(entity.category, domain.category)
        assertEquals(entity.tags, domain.tags)
        assertEquals(entity.isFavorite, domain.isFavorite)
        assertEquals(entity.lastViewed, domain.lastViewed)
        assertEquals(entity.vtaperLat, domain.vtaperLat)
        assertEquals(entity.vtaperLateralDelt, domain.vtaperLateralDelt)
        assertEquals(entity.vtaperUpperChest, domain.vtaperUpperChest)
        assertEquals(entity.vtaperRearDelt, domain.vtaperRearDelt)
        assertEquals(entity.movementPattern, domain.movementPattern)
        assertEquals(entity.imageUrl, domain.imageUrl)
        assertEquals(entity.videoUrl, domain.videoUrl)
        assertEquals(entity.animationUrl, domain.animationUrl)
        assertEquals(entity.setupInstructions, domain.setupInstructions)
        assertEquals(entity.executionInstructions, domain.executionInstructions)
        assertEquals(entity.breathingInstructions, domain.breathingInstructions)
        assertEquals(entity.tempoGuidance, domain.tempoGuidance)
        assertEquals(entity.beginnerVariantId, domain.beginnerVariantId)
        assertEquals(entity.advancedVariantId, domain.advancedVariantId)

        val captureSlot = slot<ExerciseEntity>()
        repo.addExercise(domain)

        coVerify {
            dao.insert(capture(captureSlot))
        }

        val captured = captureSlot.captured
        assertEquals(entity.id, captured.id)
        assertEquals(entity.name, captured.name)
        assertEquals(entity.description, captured.description)
        assertEquals(entity.muscleGroup, captured.muscleGroup)
        assertEquals(entity.equipment, captured.equipment)
        assertEquals(entity.difficulty, captured.difficulty)
        assertEquals(entity.secondaryMuscles, captured.secondaryMuscles)
        assertEquals(entity.instructions, captured.instructions)
        assertEquals(entity.tips, captured.tips)
        assertEquals(entity.commonMistakes, captured.commonMistakes)
        assertEquals(entity.safetyNotes, captured.safetyNotes)
        assertEquals(entity.recommendedRepRange, captured.recommendedRepRange)
        assertEquals(entity.recommendedRestTime, captured.recommendedRestTime)
        assertEquals(entity.estimatedCalories, captured.estimatedCalories)
        assertEquals(entity.category, captured.category)
        assertEquals(entity.tags, captured.tags)
        assertEquals(entity.isFavorite, captured.isFavorite)
        assertEquals(entity.lastViewed, captured.lastViewed)
        assertEquals(entity.vtaperLat, captured.vtaperLat)
        assertEquals(entity.vtaperLateralDelt, captured.vtaperLateralDelt)
        assertEquals(entity.vtaperUpperChest, captured.vtaperUpperChest)
        assertEquals(entity.vtaperRearDelt, captured.vtaperRearDelt)
        assertEquals(entity.movementPattern, captured.movementPattern)
        assertEquals(entity.imageUrl, captured.imageUrl)
        assertEquals(entity.videoUrl, captured.videoUrl)
        assertEquals(entity.animationUrl, captured.animationUrl)
        assertEquals(entity.setupInstructions, captured.setupInstructions)
        assertEquals(entity.executionInstructions, captured.executionInstructions)
        assertEquals(entity.breathingInstructions, captured.breathingInstructions)
        assertEquals(entity.tempoGuidance, captured.tempoGuidance)
        assertEquals(entity.beginnerVariantId, captured.beginnerVariantId)
        assertEquals(entity.advancedVariantId, captured.advancedVariantId)
    }

    @Test
    fun test_unknownRoleSkippedNotThrown() = runTest {
        val validRow = MuscleAssignmentRow(
            exerciseId = 1L,
            muscleName = "biceps",
            role = "primary"
        )
        val unknownRow = MuscleAssignmentRow(
            exerciseId = 1L,
            muscleName = "mystery_muscle",
            role = "mystery_role"
        )

        every { dao.getAllMuscleAssignments() } returns listOf(validRow, unknownRow)

        val result = repo.getMuscleAssignmentsWithRoles()

        assertNotNull(result[1L])
        assertEquals(1, result[1L]!!.size)
        assertEquals("biceps", result[1L]!![0].muscleName)
        assertEquals(MuscleRole.PRIMARY, result[1L]!![0].role)
    }

    @Test
    fun test_knownRolesParsedCorrectly() = runTest {
        val primaryRow = MuscleAssignmentRow(
            exerciseId = 1L,
            muscleName = "biceps",
            role = "primary"
        )
        val secondaryRow = MuscleAssignmentRow(
            exerciseId = 2L,
            muscleName = "triceps",
            role = "secondary"
        )
        val stabilizerRow = MuscleAssignmentRow(
            exerciseId = 3L,
            muscleName = "core",
            role = "stabilizer"
        )
        val mixedCaseRow = MuscleAssignmentRow(
            exerciseId = 4L,
            muscleName = "forearms",
            role = "PrImArY"
        )

        every { dao.getAllMuscleAssignments() } returns listOf(primaryRow, secondaryRow, stabilizerRow, mixedCaseRow)

        val result = repo.getMuscleAssignmentsWithRoles()

        assertEquals(MuscleRole.PRIMARY, result[1L]!![0].role)
        assertEquals(MuscleRole.SECONDARY, result[2L]!![0].role)
        assertEquals(MuscleRole.STABILIZER, result[3L]!![0].role)
        assertEquals(MuscleRole.PRIMARY, result[4L]!![0].role)
    }

    @Test
    fun test_searchBlankQueryReturnsEmptyWithoutDaoCall() = runTest {
        val result = repo.searchExercises("   ").first()

        assertTrue(result.isEmpty())

        coVerify(exactly = 0) { dao.searchExercises(any()) }
    }

    @Test
    fun test_searchNonBlankPassesThrough() = runTest {
        val entity = ExerciseEntity(
            id = 1L,
            name = "Squat",
            description = "Leg exercise",
            muscleGroup = "Legs",
            equipment = "barbell",
            difficulty = "advanced"
        )

        every { dao.searchExercises("squat") } returns flowOf(listOf(entity))

        val result = repo.searchExercises("squat").first()

        assertEquals(1, result.size)
        assertEquals("Squat", result[0].name)

        coVerify { dao.searchExercises("squat") }
    }
}