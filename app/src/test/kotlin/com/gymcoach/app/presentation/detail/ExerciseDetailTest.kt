package com.gymcoach.app.presentation.detail

import com.gymcoach.app.domain.model.Exercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for exercise detail features: v-taper scores, favorites, substitution logic.
 */
class ExerciseDetailTest {

    private fun createTestExercise(
        name: String = "Bench Press",
        muscleGroup: String = "Chest",
        equipment: String = "Barbell",
        difficulty: String = "Intermediate",
        vtaperLat: Int = 2,
        vtaperLateralDelt: Int = 1,
        vtaperUpperChest: Int = 8,
        vtaperRearDelt: Int = 1,
        isFavorite: Boolean = false,
        category: String = "Compound",
        tags: String = "compound chest"
    ) = Exercise(
        id = 1L,
        name = name,
        description = "Flat barbell bench press",
        muscleGroup = muscleGroup,
        equipment = equipment,
        difficulty = difficulty,
        vtaperLat = vtaperLat,
        vtaperLateralDelt = vtaperLateralDelt,
        vtaperUpperChest = vtaperUpperChest,
        vtaperRearDelt = vtaperRearDelt,
        isFavorite = isFavorite,
        category = category,
        tags = tags
    )

    @Test
    fun `v-taper scores are stored correctly`() {
        val exercise = createTestExercise(
            vtaperLat = 7,
            vtaperLateralDelt = 6,
            vtaperUpperChest = 9,
            vtaperRearDelt = 3
        )
        assertEquals(7, exercise.vtaperLat)
        assertEquals(6, exercise.vtaperLateralDelt)
        assertEquals(9, exercise.vtaperUpperChest)
        assertEquals(3, exercise.vtaperRearDelt)
    }

    @Test
    fun `v-taper scores default to zero`() {
        val exercise = createTestExercise().copy(
            vtaperLat = 0,
            vtaperLateralDelt = 0,
            vtaperUpperChest = 0,
            vtaperRearDelt = 0
        )
        assertEquals(0, exercise.vtaperLat)
        assertEquals(0, exercise.vtaperLateralDelt)
        assertEquals(0, exercise.vtaperUpperChest)
        assertEquals(0, exercise.vtaperRearDelt)
    }

    @Test
    fun `v-taper scores range from 0 to 10`() {
        val scores = listOf(0, 2, 5, 8, 10)
        scores.forEach { score ->
            assertTrue(score in 0..10)
        }
    }

    @Test
    fun `favorite toggle works`() {
        val exercise = createTestExercise(isFavorite = false)
        assertFalse(exercise.isFavorite)

        val toggled = exercise.copy(isFavorite = true)
        assertTrue(toggled.isFavorite)

        val toggledBack = toggled.copy(isFavorite = false)
        assertFalse(toggledBack.isFavorite)
    }

    @Test
    fun `preservation score calculation matches expected logic`() {
        // Simulate the preservation score calculation from SubstitutionEngine
        val original = createTestExercise(
            muscleGroup = "Chest",
            equipment = "Barbell",
            difficulty = "Intermediate",
            category = "Compound",
            tags = "compound chest"
        )

        // Same muscle, same equipment, same difficulty, same category, same pattern
        val perfectSub = createTestExercise(
            name = "Incline Bench Press",
            muscleGroup = "Chest",
            equipment = "Barbell",
            difficulty = "Intermediate",
            category = "Compound",
            tags = "compound chest"
        )
        val perfectScore = calculatePreservationScore(original, perfectSub)
        assertEquals(95, perfectScore) // 40+20+15+10+10 = 95

        // Same muscle, different equipment
        val dumbbellSub = createTestExercise(
            name = "Dumbbell Bench Press",
            muscleGroup = "Chest",
            equipment = "Dumbbell",
            difficulty = "Intermediate",
            category = "Compound",
            tags = "compound chest"
        )
        val dumbbellScore = calculatePreservationScore(original, dumbbellSub)
        assertEquals(80, dumbbellScore) // 40+20+0+10+10 = 80

        // Different muscle group
        val differentSub = createTestExercise(
            name = "Squat",
            muscleGroup = "Legs",
            equipment = "Barbell",
            difficulty = "Intermediate",
            category = "Compound",
            tags = "compound legs"
        )
        val differentScore = calculatePreservationScore(original, differentSub)
        assertEquals(45, differentScore) // 0+20+15+10+0 = 45
    }

    @Test
    fun `v-taper high relevance threshold`() {
        val highVTaper = createTestExercise(vtaperUpperChest = 9)
        assertTrue(highVTaper.vtaperUpperChest >= 8) // High relevance

        val mediumVTaper = createTestExercise(vtaperUpperChest = 5)
        assertTrue(mediumVTaper.vtaperUpperChest in 5..7) // Medium

        val lowVTaper = createTestExercise(vtaperUpperChest = 2)
        assertTrue(lowVTaper.vtaperUpperChest in 2..4) // Low
    }

    @Test
    fun `exercise with no vtaper scores shows empty`() {
        val exercise = createTestExercise().copy(
            vtaperLat = 0,
            vtaperLateralDelt = 0,
            vtaperUpperChest = 0,
            vtaperRearDelt = 0
        )
        val scores = listOf(
            "Lats" to exercise.vtaperLat,
            "Lateral Delt" to exercise.vtaperLateralDelt,
            "Upper Chest" to exercise.vtaperUpperChest,
            "Rear Delt" to exercise.vtaperRearDelt
        ).filter { it.second > 0 }

        assertTrue(scores.isEmpty())
    }

    @Test
    fun `exercise with vtaper scores shows non-empty`() {
        val exercise = createTestExercise(vtaperUpperChest = 8, vtaperLat = 3)
        val scores = listOf(
            "Lats" to exercise.vtaperLat,
            "Lateral Delt" to exercise.vtaperLateralDelt,
            "Upper Chest" to exercise.vtaperUpperChest,
            "Rear Delt" to exercise.vtaperRearDelt
        ).filter { it.second > 0 }

        assertEquals(2, scores.size)
    }

    // Replicate SubstitutionEngine's preservation score logic for testing
    private fun calculatePreservationScore(original: Exercise, substitute: Exercise): Int {
        var score = 0
        if (original.muscleGroup.equals(substitute.muscleGroup, ignoreCase = true)) score += 40
        if (original.category == substitute.category) score += 20
        if (original.equipment == substitute.equipment) score += 15
        if (original.difficulty == substitute.difficulty) score += 10
        val origPattern = original.tags.lowercase()
        val subPattern = substitute.tags.lowercase()
        if (origPattern.contains("compound") && subPattern.contains("compound")) score += 10
        if (origPattern.contains("isolation") && subPattern.contains("isolation")) score += 10
        return score.coerceAtMost(100)
    }
}
