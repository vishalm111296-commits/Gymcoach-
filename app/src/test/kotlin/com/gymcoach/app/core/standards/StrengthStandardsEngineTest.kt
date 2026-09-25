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

    @Test
    fun `evaluateProfile identifies ELITE across all Big 4 lifts`() {
        val bw = 100.0
        val maxes = mapOf(
            "Squat" to 230.0,       // 2.3x >= 2.25 (Elite)
            "Bench Press" to 180.0, // 1.8x >= 1.75 (Elite)
            "Deadlift" to 280.0,    // 2.8x >= 2.75 (Elite)
            "Overhead Press" to 115.0 // 1.15x >= 1.1 (Elite)
        )

        val profile = engine.evaluateProfile(bw, maxes)
        assertEquals(StrengthTier.ELITE, profile.overallTier)
        assertTrue(profile.percentile >= 98)
        assertEquals(230.0 + 180.0 + 280.0 + 115.0, profile.totalBig4Kg, 0.01)

        for (lift in profile.liftStandards) {
            assertEquals(StrengthTier.ELITE, lift.tier)
            assertNull(lift.nextTier)
            assertNull(lift.nextTierKg)
            assertEquals(100, lift.progressToNextTierPct)
        }
        assertTrue(profile.coachingRecommendation.isNotEmpty())
    }

    @Test
    fun `evaluateProfile handles negative bodyweight safely`() {
        val profile = engine.evaluateProfile(-75.0, mapOf("Squat" to 100.0))
        assertEquals(StrengthTier.UNTRAINED, profile.overallTier)
        assertTrue(profile.liftStandards.isEmpty())
        assertEquals(0.0, profile.totalBig4Kg, 0.001)
    }

    @Test
    fun `evaluateProfile calculates progress from UNTRAINED to NOVICE`() {
        val bw = 100.0
        // Squat Novice: 0.8x (80kg). A 40kg squat is 50% of the way to Novice
        val maxes = mapOf(
            "Squat" to 40.0,
            "Bench Press" to 1.0,
            "Deadlift" to 1.0,
            "Overhead Press" to 1.0
        )

        val profile = engine.evaluateProfile(bw, maxes)
        val squatStandard = profile.liftStandards.find { it.exerciseName == "Squat" }!!
        assertEquals(StrengthTier.UNTRAINED, squatStandard.tier)
        assertEquals(StrengthTier.NOVICE, squatStandard.nextTier)
        assertEquals(80.0, squatStandard.nextTierKg!!, 0.01)
        assertEquals(50, squatStandard.progressToNextTierPct)
    }

    @Test
    fun `evaluateProfile verifies exact threshold transitions for Squat Deadlift and Overhead Press`() {
        val bw = 100.0

        // Squat boundaries: 0.8x (80kg), 1.2x (120kg), 1.75x (175kg), 2.25x (225kg)
        val squatUntrained = engine.evaluateProfile(bw, mapOf("Squat" to 79.9, "Bench Press" to 1.0, "Deadlift" to 1.0, "Overhead Press" to 1.0))
        assertEquals(StrengthTier.UNTRAINED, squatUntrained.liftStandards.find { it.exerciseName == "Squat" }?.tier)

        val squatNovice = engine.evaluateProfile(bw, mapOf("Squat" to 80.0, "Bench Press" to 1.0, "Deadlift" to 1.0, "Overhead Press" to 1.0))
        assertEquals(StrengthTier.NOVICE, squatNovice.liftStandards.find { it.exerciseName == "Squat" }?.tier)

        val squatIntermediate = engine.evaluateProfile(bw, mapOf("Squat" to 120.0, "Bench Press" to 1.0, "Deadlift" to 1.0, "Overhead Press" to 1.0))
        assertEquals(StrengthTier.INTERMEDIATE, squatIntermediate.liftStandards.find { it.exerciseName == "Squat" }?.tier)

        val squatAdvanced = engine.evaluateProfile(bw, mapOf("Squat" to 175.0, "Bench Press" to 1.0, "Deadlift" to 1.0, "Overhead Press" to 1.0))
        assertEquals(StrengthTier.ADVANCED, squatAdvanced.liftStandards.find { it.exerciseName == "Squat" }?.tier)

        val squatElite = engine.evaluateProfile(bw, mapOf("Squat" to 225.0, "Bench Press" to 1.0, "Deadlift" to 1.0, "Overhead Press" to 1.0))
        assertEquals(StrengthTier.ELITE, squatElite.liftStandards.find { it.exerciseName == "Squat" }?.tier)

        // Deadlift boundaries: 1.0x (100kg), 1.5x (150kg), 2.2x (220kg), 2.75x (275kg)
        val dlUntrained = engine.evaluateProfile(bw, mapOf("Deadlift" to 99.9, "Squat" to 1.0, "Bench Press" to 1.0, "Overhead Press" to 1.0))
        assertEquals(StrengthTier.UNTRAINED, dlUntrained.liftStandards.find { it.exerciseName == "Deadlift" }?.tier)

        val dlNovice = engine.evaluateProfile(bw, mapOf("Deadlift" to 100.0, "Squat" to 1.0, "Bench Press" to 1.0, "Overhead Press" to 1.0))
        assertEquals(StrengthTier.NOVICE, dlNovice.liftStandards.find { it.exerciseName == "Deadlift" }?.tier)

        val dlElite = engine.evaluateProfile(bw, mapOf("Deadlift" to 275.0, "Squat" to 1.0, "Bench Press" to 1.0, "Overhead Press" to 1.0))
        assertEquals(StrengthTier.ELITE, dlElite.liftStandards.find { it.exerciseName == "Deadlift" }?.tier)

        // Overhead Press boundaries: 0.4x (40kg), 0.6x (60kg), 0.85x (85kg), 1.1x (110kg)
        val ohpUntrained = engine.evaluateProfile(bw, mapOf("Overhead Press" to 39.9, "Squat" to 1.0, "Bench Press" to 1.0, "Deadlift" to 1.0))
        assertEquals(StrengthTier.UNTRAINED, ohpUntrained.liftStandards.find { it.exerciseName == "Overhead Press" }?.tier)

        val ohpNovice = engine.evaluateProfile(bw, mapOf("Overhead Press" to 40.0, "Squat" to 1.0, "Bench Press" to 1.0, "Deadlift" to 1.0))
        assertEquals(StrengthTier.NOVICE, ohpNovice.liftStandards.find { it.exerciseName == "Overhead Press" }?.tier)

        val ohpElite = engine.evaluateProfile(bw, mapOf("Overhead Press" to 110.0, "Squat" to 1.0, "Bench Press" to 1.0, "Deadlift" to 1.0))
        assertEquals(StrengthTier.ELITE, ohpElite.liftStandards.find { it.exerciseName == "Overhead Press" }?.tier)
    }

    @Test
    fun `percentile calculation clamps correctly across boundary tier values`() {
        val bw = 100.0
        // All Untrained (1kg each -> tier 0): totalTierValue = 0 -> percentile = 0
        val pUntrained = engine.evaluateProfile(bw, mapOf("Squat" to 1.0, "Bench Press" to 1.0, "Deadlift" to 1.0, "Overhead Press" to 1.0))
        assertEquals(0, pUntrained.percentile)

        // All Novice (80kg Squat, 60kg Bench, 100kg Deadlift, 40kg OHP -> tier 1 each -> total = 4):
        // base = 4/16 * 100 = 25% -> in [20, 50]
        val pNovice = engine.evaluateProfile(bw, mapOf("Squat" to 80.0, "Bench Press" to 60.0, "Deadlift" to 100.0, "Overhead Press" to 40.0))
        assertEquals(25, pNovice.percentile)

        // All Intermediate (120kg, 90kg, 150kg, 60kg -> tier 2 each -> total = 8):
        // base = 8/16 * 100 = 50% -> in [50, 80]
        val pIntermediate = engine.evaluateProfile(bw, mapOf("Squat" to 120.0, "Bench Press" to 90.0, "Deadlift" to 150.0, "Overhead Press" to 60.0))
        assertEquals(50, pIntermediate.percentile)

        // All Advanced (175kg, 130kg, 220kg, 85kg -> tier 3 each -> total = 12):
        // base = 12/16 * 100 = 75% -> coerced to [80, 95] -> 80%
        val pAdvanced = engine.evaluateProfile(bw, mapOf("Squat" to 175.0, "Bench Press" to 130.0, "Deadlift" to 220.0, "Overhead Press" to 85.0))
        assertEquals(80, pAdvanced.percentile)

        // All Elite (225kg, 175kg, 275kg, 110kg -> tier 4 each -> total = 16):
        // base = 16/16 * 100 = 100% -> coerced to [95, 99] -> 99%
        val pElite = engine.evaluateProfile(bw, mapOf("Squat" to 225.0, "Bench Press" to 175.0, "Deadlift" to 275.0, "Overhead Press" to 110.0))
        assertEquals(99, pElite.percentile)
    }

    @Test
    fun `evaluateProfile with zero oneRepMax flags incomplete profile and Untrained overall`() {
        val bw = 80.0
        // Squat, Bench, and Deadlift are high, but Overhead Press is 0.0
        val maxes = mapOf(
            "Squat" to 160.0,
            "Bench Press" to 120.0,
            "Deadlift" to 200.0,
            "Overhead Press" to 0.0
        )
        val profile = engine.evaluateProfile(bw, maxes)

        assertEquals(StrengthTier.UNTRAINED, profile.overallTier)
        assertNull(profile.strongestLift)
        assertNull(profile.weakestLift)
        assertTrue(profile.coachingRecommendation.contains("Record a 1RM for all Big 4 lifts"))
    }

    @Test
    fun `testFractionalBig4TotalWeightAccumulationAndBodyweightRatios`() {
        val bw = 75.0
        val maxes = mapOf(
            "Squat" to 142.5,
            "Bench Press" to 102.5,
            "Deadlift" to 185.0,
            "Overhead Press" to 67.5
        )

        val profile = engine.evaluateProfile(bw, maxes)
        assertEquals(497.5, profile.totalBig4Kg, 0.001)

        val squat = profile.liftStandards.find { it.exerciseName == "Squat" }!!
        assertEquals(142.5 / 75.0, squat.bodyweightRatio, 0.001)

        val bench = profile.liftStandards.find { it.exerciseName == "Bench Press" }!!
        assertEquals(102.5 / 75.0, bench.bodyweightRatio, 0.001)
    }

    @Test
    fun `testTierAdvanceResetsProgressPercentageToZeroForNextTier`() {
        val bw = 100.0
        // Squat: Novice is 80kg (0.8x), Intermediate is 120kg (1.2x), Advanced is 175kg (1.75x)

        // At 110kg (in Novice): progress = (110 - 80) / (120 - 80) * 100 = 75%
        val p110 = engine.evaluateProfile(bw, mapOf("Squat" to 110.0, "Bench Press" to 1.0, "Deadlift" to 1.0, "Overhead Press" to 1.0))
        val squat110 = p110.liftStandards.find { it.exerciseName == "Squat" }!!
        assertEquals(StrengthTier.NOVICE, squat110.tier)
        assertEquals(75, squat110.progressToNextTierPct)

        // At 120kg (advances to Intermediate): progress to Advanced = (120 - 120) / (175 - 120) * 100 = 0%
        val p120 = engine.evaluateProfile(bw, mapOf("Squat" to 120.0, "Bench Press" to 1.0, "Deadlift" to 1.0, "Overhead Press" to 1.0))
        val squat120 = p120.liftStandards.find { it.exerciseName == "Squat" }!!
        assertEquals(StrengthTier.INTERMEDIATE, squat120.tier)
        assertEquals(StrengthTier.ADVANCED, squat120.nextTier)
        assertEquals(175.0, squat120.nextTierKg!!, 0.001)
        assertEquals(0, squat120.progressToNextTierPct)
    }

    @Test
    fun `testCompletelyOmittedMapKeysHandledAsZeroAndIncomplete`() {
        val bw = 80.0
        // Pass only Squat; Bench, Deadlift, OHP keys absent from map
        val maxes = mapOf("Squat" to 150.0)

        val profile = engine.evaluateProfile(bw, maxes)
        assertEquals(StrengthTier.UNTRAINED, profile.overallTier)
        assertNull(profile.strongestLift)
        assertNull(profile.weakestLift)
        assertTrue(profile.coachingRecommendation.contains("Record a 1RM for all Big 4 lifts"))
        assertEquals(4, profile.liftStandards.size) // All 4 entries created with 0.0 for missing
        assertEquals(150.0, profile.totalBig4Kg, 0.001)
    }

    @Test
    fun `testSpecificLaggingLiftRecommendationNaming`() {
        val bw = 80.0
        // Squat, Bench, Deadlift are Advanced, but Bench Press is lowest in progression
        val maxes = mapOf(
            "Squat" to 160.0,        // 2.0x -> Advanced
            "Bench Press" to 75.0,   // 0.9375x -> Intermediate (lagging!)
            "Deadlift" to 190.0,     // 2.375x -> Advanced
            "Overhead Press" to 70.0 // 0.875x -> Advanced
        )

        val profile = engine.evaluateProfile(bw, maxes)
        assertEquals("Bench Press", profile.weakestLift)
        assertTrue(profile.coachingRecommendation.contains("Your Bench Press is currently lagging behind"))
    }
}