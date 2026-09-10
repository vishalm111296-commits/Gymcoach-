package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.ExerciseMuscleDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.domain.model.Exercise
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseRepositoryMappingTest {

    @Test
    fun `exercise mapping preserves all non-default fields in both directions`() {
        val originalEntity = ExerciseEntity(
            id = 42L,
            name = "Incline Dumbbell Press",
            description = "Chest exercise",
            muscleGroup = "Upper Chest",
            equipment = "Dumbbell",
            difficulty = "Intermediate",
            secondaryMuscles = "Triceps, Front Deltoid",
            instructions = "Lie on incline bench",
            tips = "Keep elbows at 45 degrees",
            commonMistakes = "Flaring elbows",
            safetyNotes = "Use spotter if heavy",
            recommendedRepRange = "8-12",
            recommendedRestTime = "90",
            estimatedCalories = 150,
            category = "Pressing",
            tags = "hypertrophy,push",
            isFavorite = true,
            lastViewed = 1700000000000L,
            vtaperLat = 2,
            vtaperLateralDelt = 4,
            vtaperUpperChest = 9,
            vtaperRearDelt = 1,
            movementPattern = "Horizontal Push",
            imageUrl = "https://example.com/image.jpg",
            videoUrl = "https://example.com/video.mp4",
            animationUrl = "https://example.com/anim.gif",
            setupInstructions = "Set bench to 30 degrees",
            executionInstructions = "Press dumbbells upward",
            breathingInstructions = "Inhale down, exhale up",
            tempoGuidance = "3-0-1-0",
            beginnerVariantId = 101L,
            advancedVariantId = 102L
        )

        val exerciseDao: ExerciseDao = mockk(relaxed = true)
        val exerciseMuscleDao: ExerciseMuscleDao = mockk(relaxed = true)
        val repository = ExerciseRepositoryImpl(exerciseDao, exerciseMuscleDao)

        // Convert entity to domain via reflection invocation on instance method
        val toDomainMethod = ExerciseRepositoryImpl::class.java.getDeclaredMethod("toDomain", ExerciseEntity::class.java)
        toDomainMethod.isAccessible = true
        val domain = toDomainMethod.invoke(repository, originalEntity) as Exercise

        assertEquals(42L, domain.id)
        assertEquals("Incline Dumbbell Press", domain.name)
        assertEquals("Chest exercise", domain.description)
        assertEquals("Upper Chest", domain.muscleGroup)
        assertEquals("Dumbbell", domain.equipment)
        assertEquals("Intermediate", domain.difficulty)
        assertEquals("Triceps, Front Deltoid", domain.secondaryMuscles)
        assertEquals("Lie on incline bench", domain.instructions)
        assertEquals("Keep elbows at 45 degrees", domain.tips)
        assertEquals("Flaring elbows", domain.commonMistakes)
        assertEquals("Use spotter if heavy", domain.safetyNotes)
        assertEquals("8-12", domain.recommendedRepRange)
        assertEquals("90", domain.recommendedRestTime)
        assertEquals(150, domain.estimatedCalories)
        assertEquals("Pressing", domain.category)
        assertEquals("hypertrophy,push", domain.tags)
        assertEquals(true, domain.isFavorite)
        assertEquals(1700000000000L, domain.lastViewed)
        assertEquals(2, domain.vtaperLat)
        assertEquals(4, domain.vtaperLateralDelt)
        assertEquals(9, domain.vtaperUpperChest)
        assertEquals(1, domain.vtaperRearDelt)
        assertEquals("Horizontal Push", domain.movementPattern)
        assertEquals("https://example.com/image.jpg", domain.imageUrl)
        assertEquals("https://example.com/video.mp4", domain.videoUrl)
        assertEquals("https://example.com/anim.gif", domain.animationUrl)
        assertEquals("Set bench to 30 degrees", domain.setupInstructions)
        assertEquals("Press dumbbells upward", domain.executionInstructions)
        assertEquals("Inhale down, exhale up", domain.breathingInstructions)
        assertEquals("3-0-1-0", domain.tempoGuidance)
        assertEquals(101L, domain.beginnerVariantId)
        assertEquals(102L, domain.advancedVariantId)

        // Convert domain back to entity
        val toEntityMethod = ExerciseRepositoryImpl::class.java.getDeclaredMethod("toEntity", Exercise::class.java)
        toEntityMethod.isAccessible = true
        val entityBack = toEntityMethod.invoke(repository, domain) as ExerciseEntity

        assertEquals(originalEntity, entityBack)
    }
}
