package com.gymcoach.app.core.home

import com.gymcoach.app.domain.model.Exercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for VtaperAttribution — per-exercise volume attribution to the
 * balance/dashboard muscle names using only real repository metadata.
 */
class VtaperAttributionTest {

    private fun exercise(
        muscleGroup: String,
        vtaperLat: Int = 0,
        vtaperLateralDelt: Int = 0,
        vtaperRearDelt: Int = 0,
        vtaperUpperChest: Int = 0,
        secondaryMuscles: String = ""
    ) = Exercise(
        id = 1L,
        name = "test exercise",
        description = "",
        muscleGroup = muscleGroup,
        equipment = "",
        difficulty = "",
        secondaryMuscles = secondaryMuscles,
        vtaperLat = vtaperLat,
        vtaperLateralDelt = vtaperLateralDelt,
        vtaperRearDelt = vtaperRearDelt,
        vtaperUpperChest = vtaperUpperChest
    )

    @Test
    fun pullUpLikeAttributedToLatsBicepsAndUpperBack() {
        // Pull-up-like: high lat relevance + biceps as secondary taxon
        val ex = exercise("back", vtaperLat = 10, secondaryMuscles = "biceps, forearms")
        val muscles = VtaperAttribution.contributors(ex)

        assertTrue("Expected Lats", muscles.contains("Lats"))
        assertTrue("Expected Biceps", muscles.contains("Biceps"))
        assertTrue("Expected Upper Back (back group)", muscles.contains("Upper Back"))
        assertFalse("No Hamstrings for a pull-up", muscles.contains("Hamstrings"))
    }

    @Test
    fun lateralRaiseLikeAttributedToLateralDeltoidOnly() {
        // Lateral-raise-like: lateral delt relevance only, no other metadata
        val ex = exercise("shoulders", vtaperLateralDelt = 10)
        assertEquals(listOf("Lateral Deltoid"), VtaperAttribution.contributors(ex))
    }

    @Test
    fun squatLikeAttributedToLegsHamstringsAndCore() {
        // Squat-like: legs group; hamstrings and abs as secondary taxa
        val ex = exercise("legs", secondaryMuscles = "hamstrings, abs, lower_back")
        val muscles = VtaperAttribution.contributors(ex)

        assertTrue("Expected Legs bar", muscles.contains("Legs"))
        assertTrue("Expected Hamstrings", muscles.contains("Hamstrings"))
        assertTrue("Expected Core via abs", muscles.contains("Core"))
        assertFalse("No Biceps for a squat", muscles.contains("Biceps"))
    }

    @Test
    fun benchPressLikeAttributedToUpperChest() {
        // Bench-press-like: upper chest relevance, chest group
        val ex = exercise("chest", vtaperUpperChest = 7)
        assertEquals(listOf("Upper Chest"), VtaperAttribution.contributors(ex))
    }

    @Test
    fun coreExerciseAttributedToCore() {
        val ex = exercise("core")
        val muscles = VtaperAttribution.contributors(ex)

        assertTrue("Expected Core", muscles.contains("Core"))
    }

    @Test
    fun emptySecondaryAddsNoSpuriousMuscles() {
        // No secondary taxa → only group-driven contributors, no phantom muscles
        val ex = exercise("legs")
        assertEquals(listOf("Legs"), VtaperAttribution.contributors(ex))
    }

    @Test
    fun coreSourceDoesNotDuplicateCoreEntry() {
        // muscleGroup "core" AND an abs taxon must still yield "Core" only once
        val ex = exercise("core", secondaryMuscles = "abs, deep_core")
        val muscles = VtaperAttribution.contributors(ex)

        assertEquals(1, muscles.count { it == "Core" })
    }

    @Test
    fun allSecondaryTaxonTokensAttributed() {
        val ex = exercise(
            "legs",
            vtaperLat = 3,
            secondaryMuscles = "quadriceps, triceps, glutes, calves"
        )
        val muscles = VtaperAttribution.contributors(ex)

        assertTrue("Expected Lats (vtaperLat)", muscles.contains("Lats"))
        assertTrue("Expected Quadriceps", muscles.contains("Quadriceps"))
        assertTrue("Expected Triceps", muscles.contains("Triceps"))
        assertTrue("Expected Glutes", muscles.contains("Glutes"))
        assertTrue("Expected Calves", muscles.contains("Calves"))
        assertTrue("Expected Legs bar", muscles.contains("Legs"))
        assertTrue("Expected distinct entries", muscles.distinct().size == muscles.size)
    }

    @Test
    fun noSecondaryAndZeroVtaperScoresYieldsEmptyList() {
        // No vtaper relevance scores, no recognized taxon, no group-driven bar
        val ex = exercise("full_body")
        assertTrue("Expected no contributors", VtaperAttribution.contributors(ex).isEmpty())
    }

    @Test
    fun deepCoreSecondaryOnNonCoreCategoryStillCreditsCore() {
        // "deep_core" is a core taxon; it must credit Core even when the primary
        // muscle group is a non-core category (e.g., back).
        val ex = exercise("back", secondaryMuscles = "deep_core")
        val muscles = VtaperAttribution.contributors(ex)

        assertTrue("Expected Core via deep_core taxon", muscles.contains("Core"))
        assertTrue("Expected Upper Back (back group)", muscles.contains("Upper Back"))
        assertEquals(1, muscles.count { it == "Core" })
    }
}
