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
    }
}
