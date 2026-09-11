package com.gymcoach.app.core.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlateCalculatorTest {

    @Test
    fun testStandardBarbellWeightCalculation() {
        // 100 kg on a 20 kg bar = 40 kg per side -> 1x25kg + 1x15kg
        val result = PlateCalculator.calculatePlates(targetWeight = 100.0, barWeight = 20.0)

        assertEquals(100.0, result.totalWeight, 0.001)
        assertEquals(20.0, result.barWeight, 0.001)
        assertEquals(40.0, result.weightPerSide, 0.001)
        assertEquals(0.0, result.remainder, 0.001)

        assertEquals(2, result.platesPerSide.size)
        assertEquals(25.0, result.platesPerSide[0].plateWeight, 0.001)
        assertEquals(1, result.platesPerSide[0].count)
        assertEquals(15.0, result.platesPerSide[1].plateWeight, 0.001)
        assertEquals(1, result.platesPerSide[1].count)
    }

    @Test
    fun testTargetWeightLessThanBar() {
        val result = PlateCalculator.calculatePlates(targetWeight = 15.0, barWeight = 20.0)
        assertEquals(0.0, result.weightPerSide, 0.001)
        assertTrue(result.platesPerSide.isEmpty())
        assertEquals(0.0, result.remainder, 0.001)
    }

    @Test
    fun testWeightWithMicroPlates() {
        // 62.5 kg on 20 kg bar = 21.25 kg per side -> 1x20kg + 1x1.25kg
        val result = PlateCalculator.calculatePlates(targetWeight = 62.5, barWeight = 20.0)
        assertEquals(21.25, result.weightPerSide, 0.001)
        assertEquals(0.0, result.remainder, 0.001)

        assertEquals(2, result.platesPerSide.size)
        assertEquals(20.0, result.platesPerSide[0].plateWeight, 0.001)
        assertEquals(1, result.platesPerSide[0].count)
        assertEquals(1.25, result.platesPerSide[1].plateWeight, 0.001)
        assertEquals(1, result.platesPerSide[1].count)
    }
}
