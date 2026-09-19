package com.gymcoach.app.core.standards

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class StrengthStandardsEngineTest {

    private lateinit var engine: StrengthStandardsEngine

    @Before
    fun setup() {
        engine = StrengthStandardsEngine()
    }

    @Test
    fun `evaluateProfile returns UNTRAINED for bodyweight less than or equal to 0`() {
        val profile = engine.evaluateProfile(0.0, mapOf("Squat" to 100.0))
        assertEquals(StrengthTier.UNTRAINED, profile.overallTier)
        assertTrue(profile.coachingRecommendation.contains("Bodyweight must be greater than 0"))
        assertTrue(profile.liftStandards.isEmpty())
    }

    @Test
    fun `evaluateProfile identifies missing lifts and returns UNTRAINED overall`() {
        val oneRepMaxes = mapOf(
            "Squat" to 100.0,
            "Bench Press" to 80.0
            // Missing Deadlift and OHP
        )
        val profile = engine.evaluateProfile(80.0, oneRepMaxes)

        assertEquals(StrengthTier.UNTRAINED, profile.overallTier)
        assertTrue(profile.coachingRecommendation.contains("Record a 1RM for all Big 4 lifts"))
        assertNull(profile.strongestLift)
        assertNull(profile.weakestLift)
    }

    @Test
    fun `evaluateProfile calculates tiers correctly for Bench Press`() {
        val bw = 80.0
        // Novice: 0.6x (48kg), Intermediate: 0.9x (72kg), Advanced: 1.3x (104kg), Elite: 1.75x (140kg)

        val maxesUntrained = mapOf("Bench Press" to 40.0, "Squat" to 1.0, "Deadlift" to 1.0, "Overhead Press" to 1.0)
        assertEquals(StrengthTier.UNTRAINED, engine.evaluateProfile(bw, maxesUntrained).liftStandards.find { it.exerciseName == "Bench Press" }?.tier)

        val maxesNovice = mapOf("Bench Press" to 50.0, "Squat" to 1.0, "Deadlift" to 1.0, "Overhead Press" to 1.0)
        assertEquals(StrengthTier.NOVICE, engine.evaluateProfile(bw, maxesNovice).liftStandards.find { it.exerciseName == "Bench Press" }?.tier)

        val maxesIntermediate = mapOf("Bench Press" to 80.0, "Squat" to 1.0, "Deadlift" to 1.0, "Overhead Press" to 1.0)
        assertEquals(StrengthTier.INTERMEDIATE, engine.evaluateProfile(bw, maxesIntermediate).liftStandards.find { it.exerciseName == "Bench Press" }?.tier)

        val maxesAdvanced = mapOf("Bench Press" to 110.0, "Squat" to 1.0, "Deadlift" to 1.0, "Overhead Press" to 1.0)
        assertEquals(StrengthTier.ADVANCED, engine.evaluateProfile(bw, maxesAdvanced).liftStandards.find { it.exerciseName == "Bench Press" }?.tier)

        val maxesElite = mapOf("Bench Press" to 150.0, "Squat" to 1.0, "Deadlift" to 1.0, "Overhead Press" to 1.0)
        assertEquals(StrengthTier.ELITE, engine.evaluateProfile(bw, maxesElite).liftStandards.find { it.exerciseName == "Bench Press" }?.tier)
    }

    @Test
    fun `evaluateProfile calculates overall tier, strongest, and weakest lifts correctly`() {
        val bw = 80.0
        val maxes = mapOf(
            "Squat" to 150.0, // 1.875x -> Advanced
            "Bench Press" to 100.0, // 1.25x -> Intermediate
            "Deadlift" to 200.0, // 2.5x -> Advanced
            "Overhead Press" to 40.0 // 0.5x -> Novice
        )

        val profile = engine.evaluateProfile(bw, maxes)

        // Tiers: Squat (3), Bench (2), Deadlift (3), OHP (1). Total = 9. Avg = 2.25 -> round up -> 2 (Intermediate)
        assertEquals(StrengthTier.INTERMEDIATE, profile.overallTier)
        assertEquals("Deadlift", profile.strongestLift) // Deadlift is furthest along in Advanced
        assertEquals("Overhead Press", profile.weakestLift)
        assertEquals(150.0 + 100.0 + 200.0 + 40.0, profile.totalBig4Kg, 0.01)
        assertTrue(profile.coachingRecommendation.contains("Your Overhead Press is currently lagging behind"))
    }

    @Test
    fun `evaluateProfile calculates progress to next tier correctly`() {
        val bw = 100.0
        // Squat Novice: 0.8x (80kg), Intermediate: 1.2x (120kg)
        // Let's test a Squat of 100kg (exactly halfway between 80 and 120)
        val maxes = mapOf(
            "Squat" to 100.0,
            "Bench Press" to 1.0,
            "Deadlift" to 1.0,
            "Overhead Press" to 1.0
        )

        val profile = engine.evaluateProfile(bw, maxes)
        val squatStandard = profile.liftStandards.find { it.exerciseName == "Squat" }!!

        assertEquals(StrengthTier.NOVICE, squatStandard.tier)
        assertEquals(StrengthTier.INTERMEDIATE, squatStandard.nextTier)
        assertEquals(120.0, squatStandard.nextTierKg!!, 0.01)
        assertEquals(50, squatStandard.progressToNextTierPct)
    }
}