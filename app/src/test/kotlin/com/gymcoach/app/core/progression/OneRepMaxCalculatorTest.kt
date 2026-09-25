package com.gymcoach.app.core.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OneRepMaxCalculatorTest {

    @Test
    fun testOneRepMaxCalculation() {
        // Bench Press: 100kg for 1 rep -> 1RM is 100kg across all formulas
        val profile = OneRepMaxCalculator.calculateProfile(weight = 100.0, reps = 1)
        assertEquals(100.0, profile.epley1RM, 0.001)
        assertEquals(100.0, profile.brzycki1RM, 0.001)
        assertEquals(100.0, profile.lombardi1RM, 0.001)
        assertEquals(100.0, profile.mayhew1RM, 0.001)
        assertEquals(100.0, profile.wathen1RM, 0.001)
        assertEquals(100.0, profile.average1RM, 0.001)
    }

    @Test
    fun testMultiRepMaxCalculation() {
        // Bench Press: 100kg for 5 reps
        val profile = OneRepMaxCalculator.calculateProfile(weight = 100.0, reps = 5)

        // Epley: 100 * (1 + 5/30) = 116.67
        assertEquals(116.7, profile.epley1RM, 0.1)

        // Brzycki: 100 * (36 / (37 - 5)) = 100 * (36 / 32) = 112.5
        assertEquals(112.5, profile.brzycki1RM, 0.1)

        // Average should be reasonably between 112 and 117
        assertTrue(profile.average1RM in 112.0..118.0)
    }

    @Test
    fun testTrainingZonesGenerated() {
        val profile = OneRepMaxCalculator.calculateProfile(weight = 100.0, reps = 1)
        assertEquals(8, profile.zones.size)

        val zone90 = profile.zones.first { it.percentage == 90 }
        assertEquals(90.0, zone90.weight, 0.001)
        assertEquals("3-4", zone90.repRange)

        val zone80 = profile.zones.first { it.percentage == 80 }
        assertEquals(80.0, zone80.weight, 0.001)
        assertEquals("7-8", zone80.repRange)
    }

    @Test
    fun testZeroAndInvalidInputs() {
        val profile = OneRepMaxCalculator.calculateProfile(weight = 0.0, reps = 0)
        assertEquals(0.0, profile.average1RM, 0.001)
        assertTrue(profile.zones.isEmpty())

        val negativeProfile = OneRepMaxCalculator.calculateProfile(weight = -100.0, reps = -5)
        assertEquals(0.0, negativeProfile.average1RM, 0.001)
        assertTrue(negativeProfile.zones.isEmpty())
    }

    @Test
    fun testDirectFormulasSingleRepIdentity() {
        val weight = 137.5
        assertEquals(weight, OneRepMaxCalculator.epley(weight, 1), 0.001)
        assertEquals(weight, OneRepMaxCalculator.brzycki(weight, 1), 0.001)
        assertEquals(weight, OneRepMaxCalculator.lombardi(weight, 1), 0.001)
        assertEquals(weight, OneRepMaxCalculator.mayhew(weight, 1), 0.001)
        assertEquals(weight, OneRepMaxCalculator.wathen(weight, 1), 0.001)
    }

    @Test
    fun testDirectFormulasZeroAndNegativeInputs() {
        assertEquals(0.0, OneRepMaxCalculator.epley(0.0, 5), 0.001)
        assertEquals(0.0, OneRepMaxCalculator.epley(-50.0, 5), 0.001)
        assertEquals(0.0, OneRepMaxCalculator.epley(100.0, 0), 0.001)
        assertEquals(0.0, OneRepMaxCalculator.epley(100.0, -3), 0.001)

        assertEquals(0.0, OneRepMaxCalculator.brzycki(-50.0, 5), 0.001)
        assertEquals(0.0, OneRepMaxCalculator.brzycki(100.0, -1), 0.001)

        assertEquals(0.0, OneRepMaxCalculator.lombardi(-50.0, 5), 0.001)
        assertEquals(0.0, OneRepMaxCalculator.lombardi(100.0, -1), 0.001)

        assertEquals(0.0, OneRepMaxCalculator.mayhew(-50.0, 5), 0.001)
        assertEquals(0.0, OneRepMaxCalculator.mayhew(100.0, -1), 0.001)

        assertEquals(0.0, OneRepMaxCalculator.wathen(-50.0, 5), 0.001)
        assertEquals(0.0, OneRepMaxCalculator.wathen(100.0, -1), 0.001)
    }

    @Test
    fun testHighRepClampingPreventsDenominatorInstability() {
        // For reps > 15, formulas clamp to 15 to preserve empirical validity
        val epley15 = OneRepMaxCalculator.epley(100.0, 15)
        val epley30 = OneRepMaxCalculator.epley(100.0, 30)
        assertEquals(epley15, epley30, 0.001)
        assertEquals(150.0, epley15, 0.001)

        // Brzycki formula has singular pole at reps=37 (37 - 37 = 0)
        // Clamping to 15 prevents division by zero or negative outputs
        val brzycki15 = OneRepMaxCalculator.brzycki(100.0, 15)
        val brzycki40 = OneRepMaxCalculator.brzycki(100.0, 40)
        assertEquals(brzycki15, brzycki40, 0.001)
        assertTrue(brzycki40 > 0.0)

        val lombardi15 = OneRepMaxCalculator.lombardi(100.0, 15)
        val lombardi25 = OneRepMaxCalculator.lombardi(100.0, 25)
        assertEquals(lombardi15, lombardi25, 0.001)

        val mayhew15 = OneRepMaxCalculator.mayhew(100.0, 15)
        val mayhew50 = OneRepMaxCalculator.mayhew(100.0, 50)
        assertEquals(mayhew15, mayhew50, 0.001)

        val wathen15 = OneRepMaxCalculator.wathen(100.0, 15)
        val wathen50 = OneRepMaxCalculator.wathen(100.0, 50)
        assertEquals(wathen15, wathen50, 0.001)
    }

    @Test
    fun testPlateStepGranularityInTrainingZones() {
        // Test with 5.0kg plate increments
        val profileStep5 = OneRepMaxCalculator.calculateProfile(weight = 100.0, reps = 1, plateStep = 5.0)
        for (zone in profileStep5.zones) {
            val remainder = Math.round(zone.weight * 100.0) % 500L
            assertEquals(0L, remainder)
        }

        // Test with 1.0kg increments
        val profileStep1 = OneRepMaxCalculator.calculateProfile(weight = 100.0, reps = 1, plateStep = 1.0)
        val zone85 = profileStep1.zones.first { it.percentage == 85 }
        assertEquals(85.0, zone85.weight, 0.001)
    }

    @Test
    fun testMonotonicityAcrossRepsOneToFifteen() {
        val weight = 100.0
        for (r in 2..15) {
            assertTrue("Epley must strictly increase with reps", OneRepMaxCalculator.epley(weight, r) > OneRepMaxCalculator.epley(weight, r - 1))
            assertTrue("Brzycki must strictly increase with reps", OneRepMaxCalculator.brzycki(weight, r) > OneRepMaxCalculator.brzycki(weight, r - 1))
            assertTrue("Lombardi must strictly increase with reps", OneRepMaxCalculator.lombardi(weight, r) > OneRepMaxCalculator.lombardi(weight, r - 1))
            assertTrue("Mayhew must strictly increase with reps", OneRepMaxCalculator.mayhew(weight, r) > OneRepMaxCalculator.mayhew(weight, r - 1))
            assertTrue("Wathen must strictly increase with reps", OneRepMaxCalculator.wathen(weight, r) > OneRepMaxCalculator.wathen(weight, r - 1))
        }
    }

    @Test
    fun testTrainingZonesMonotonicOrdering() {
        val profile = OneRepMaxCalculator.calculateProfile(weight = 100.0, reps = 5, plateStep = 2.5)
        assertEquals(8, profile.zones.size)

        val expectedPcts = listOf(95, 90, 85, 80, 75, 70, 65, 60)
        assertEquals(expectedPcts, profile.zones.map { it.percentage })

        for (i in 0 until profile.zones.size - 1) {
            assertTrue(
                "Zone weight must be non-increasing as percentage decreases",
                profile.zones[i].weight >= profile.zones[i + 1].weight
            )
        }
    }

    @Test
    fun testTenRepMathematicalEquivalenceBetweenEpleyAndBrzycki() {
        val weight = 120.0
        val epley10 = OneRepMaxCalculator.epley(weight, 10)
        val brzycki10 = OneRepMaxCalculator.brzycki(weight, 10)
        assertEquals(160.0, epley10, 0.001)
        assertEquals(160.0, brzycki10, 0.001)
        assertEquals(epley10, brzycki10, 0.001)

        val profile = OneRepMaxCalculator.calculateProfile(weight, 10)
        assertEquals(160.0, profile.epley1RM, 0.001)
        assertEquals(160.0, profile.brzycki1RM, 0.001)
    }

    @Test
    fun testMicroLoadingPlateStepsInTrainingZones() {
        val profileStep05 = OneRepMaxCalculator.calculateProfile(weight = 100.0, reps = 1, plateStep = 0.5)
        for (zone in profileStep05.zones) {
            val remainder = Math.round(zone.weight * 100.0) % 50L
            assertEquals(0L, remainder)
        }

        val profileStep025 = OneRepMaxCalculator.calculateProfile(weight = 100.0, reps = 1, plateStep = 0.25)
        for (zone in profileStep025.zones) {
            val remainder = Math.round(zone.weight * 100.0) % 25L
            assertEquals(0L, remainder)
        }
    }
}
