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

    @Test
    fun testCustomBarWeightEZBar() {
        // 35 kg on 10 kg EZ-curl bar = 12.5 kg per side -> 1x10kg + 1x2.5kg
        val result = PlateCalculator.calculatePlates(targetWeight = 35.0, barWeight = 10.0)
        assertEquals(12.5, result.weightPerSide, 0.001)
        assertEquals(0.0, result.remainder, 0.001)
        assertEquals(2, result.platesPerSide.size)
        assertEquals(10.0, result.platesPerSide[0].plateWeight, 0.001)
        assertEquals(2.5, result.platesPerSide[1].plateWeight, 0.001)
    }

    @Test
    fun testNonExactWeightWithRemainder() {
        // 61.0 kg on 20 kg bar = 20.5 kg per side -> 1x20kg per side (40kg plates + 20kg bar = 60kg), remainder 1.0kg total
        val result = PlateCalculator.calculatePlates(targetWeight = 61.0, barWeight = 20.0)
        assertEquals(20.5, result.weightPerSide, 0.001)
        assertEquals(1.0, result.remainder, 0.001)
        assertEquals(1, result.platesPerSide.size)
        assertEquals(20.0, result.platesPerSide[0].plateWeight, 0.001)
    }

    @Test
    fun testTargetWeightEqualsBarWeight() {
        val result = PlateCalculator.calculatePlates(targetWeight = 20.0, barWeight = 20.0)
        assertEquals(0.0, result.weightPerSide, 0.001)
        assertEquals(0.0, result.remainder, 0.001)
        assertTrue(result.platesPerSide.isEmpty())
    }

    @Test
    fun testTargetWeightZeroOrNegative() {
        val resultZero = PlateCalculator.calculatePlates(targetWeight = 0.0, barWeight = 20.0)
        assertEquals(0.0, resultZero.weightPerSide, 0.001)
        assertEquals(0.0, resultZero.remainder, 0.001)
        assertTrue(resultZero.platesPerSide.isEmpty())

        val resultNegative = PlateCalculator.calculatePlates(targetWeight = -50.0, barWeight = 20.0)
        assertEquals(0.0, resultNegative.weightPerSide, 0.001)
        assertEquals(0.0, resultNegative.remainder, 0.001)
        assertTrue(resultNegative.platesPerSide.isEmpty())
    }

    @Test
    fun testCustomImperialPlateInventory() {
        // Standard US plates: 45, 35, 25, 10, 5, 2.5 lbs
        val imperialPlates = listOf(
            45.0 to 0xFF000000,
            35.0 to 0xFF000000,
            25.0 to 0xFF000000,
            10.0 to 0xFF000000,
            5.0 to 0xFF000000,
            2.5 to 0xFF000000
        )
        // 225 lbs on 45 lb bar = 90 lbs per side -> 2x45 lbs
        val result = PlateCalculator.calculatePlates(
            targetWeight = 225.0,
            barWeight = 45.0,
            availablePlates = imperialPlates
        )
        assertEquals(90.0, result.weightPerSide, 0.001)
        assertEquals(0.0, result.remainder, 0.001)
        assertEquals(1, result.platesPerSide.size)
        assertEquals(45.0, result.platesPerSide[0].plateWeight, 0.001)
        assertEquals(2, result.platesPerSide[0].count)
    }

    @Test
    fun testHeavyDeadliftLoad() {
        // 300 kg on 20 kg bar = 140 kg per side -> 5x25kg (125kg) + 1x15kg (15kg) = 140kg
        val result = PlateCalculator.calculatePlates(targetWeight = 300.0, barWeight = 20.0)
        assertEquals(140.0, result.weightPerSide, 0.001)
        assertEquals(0.0, result.remainder, 0.001)
        assertEquals(2, result.platesPerSide.size)
        assertEquals(25.0, result.platesPerSide[0].plateWeight, 0.001)
        assertEquals(5, result.platesPerSide[0].count)
        assertEquals(15.0, result.platesPerSide[1].plateWeight, 0.001)
        assertEquals(1, result.platesPerSide[1].count)
    }

    @Test
    fun testEmptyAvailablePlatesList() {
        val result = PlateCalculator.calculatePlates(
            targetWeight = 100.0,
            barWeight = 20.0,
            availablePlates = emptyList()
        )
        assertEquals(40.0, result.weightPerSide, 0.001)
        assertEquals(80.0, result.remainder, 0.001)
        assertTrue(result.platesPerSide.isEmpty())
    }

    @Test
    fun testSpecialtyBarsSafetySquatAndWomensBar() {
        // 25kg Safety Squat Bar: 125kg total -> 50kg/side -> 2x25kg plates per side
        val ssb = PlateCalculator.calculatePlates(targetWeight = 125.0, barWeight = 25.0)
        assertEquals(50.0, ssb.weightPerSide, 0.001)
        assertEquals(0.0, ssb.remainder, 0.001)
        assertEquals(1, ssb.platesPerSide.size)
        assertEquals(25.0, ssb.platesPerSide[0].plateWeight, 0.001)
        assertEquals(2, ssb.platesPerSide[0].count)

        // 15kg Women's Olympic Bar: 50kg total -> 17.5kg/side -> 1x15kg + 1x2.5kg
        val womensBar = PlateCalculator.calculatePlates(targetWeight = 50.0, barWeight = 15.0)
        assertEquals(17.5, womensBar.weightPerSide, 0.001)
        assertEquals(0.0, womensBar.remainder, 0.001)
        assertEquals(2, womensBar.platesPerSide.size)
        assertEquals(15.0, womensBar.platesPerSide[0].plateWeight, 0.001)
        assertEquals(1, womensBar.platesPerSide[0].count)
        assertEquals(2.5, womensBar.platesPerSide[1].plateWeight, 0.001)
        assertEquals(1, womensBar.platesPerSide[1].count)
    }

    @Test
    fun testMicroLoadingFractionalPlates() {
        // Inventory includes 0.5kg and 0.25kg fractional change plates
        val fractionalPlates = PlateCalculator.STANDARD_METRIC_PLATES + listOf(
            0.5 to 0xFFCCCCCC,
            0.25 to 0xFFEEEEEE
        )

        // Target: 61.5kg on 20kg bar -> 20.75kg per side -> 1x20kg + 1x0.5kg + 1x0.25kg
        val result = PlateCalculator.calculatePlates(
            targetWeight = 61.5,
            barWeight = 20.0,
            availablePlates = fractionalPlates
        )

        assertEquals(20.75, result.weightPerSide, 0.001)
        assertEquals(0.0, result.remainder, 0.001)
        assertEquals(3, result.platesPerSide.size)
        assertEquals(20.0, result.platesPerSide[0].plateWeight, 0.001)
        assertEquals(0.5, result.platesPerSide[1].plateWeight, 0.001)
        assertEquals(0.25, result.platesPerSide[2].plateWeight, 0.001)
    }

    @Test
    fun testPlateInventoryWithoutHeavyPlatesUsesMultiplesOfSmallerPlates() {
        val lightPlatesOnly = listOf(
            10.0 to 0xFF388E3C,
            5.0 to 0xFFFFFFFF,
            2.5 to 0xFF212121,
            1.25 to 0xFF9E9E9E
        )

        // 70kg on 20kg bar = 25kg/side -> 2x10kg + 1x5kg per side
        val result = PlateCalculator.calculatePlates(
            targetWeight = 70.0,
            barWeight = 20.0,
            availablePlates = lightPlatesOnly
        )

        assertEquals(25.0, result.weightPerSide, 0.001)
        assertEquals(0.0, result.remainder, 0.001)
        assertEquals(2, result.platesPerSide.size)
        assertEquals(10.0, result.platesPerSide[0].plateWeight, 0.001)
        assertEquals(2, result.platesPerSide[0].count)
        assertEquals(5.0, result.platesPerSide[1].plateWeight, 0.001)
        assertEquals(1, result.platesPerSide[1].count)
    }

    @Test
    fun testMicroPlatesWithUnevenRemainderCalculation() {
        val fractionalPlates = PlateCalculator.STANDARD_METRIC_PLATES + listOf(
            0.5 to 0xFFCCCCCC,
            0.25 to 0xFFEEEEEE
        )

        // Target: 61.1kg on 20kg bar -> 20.55kg per side -> 1x20kg (leaves 0.55) + 1x0.5kg (leaves 0.05) -> remainder 0.05 * 2 = 0.1kg
        val result = PlateCalculator.calculatePlates(
            targetWeight = 61.1,
            barWeight = 20.0,
            availablePlates = fractionalPlates
        )

        assertEquals(20.55, result.weightPerSide, 0.001)
        assertEquals(0.1, result.remainder, 0.001)
        assertEquals(2, result.platesPerSide.size)
        assertEquals(20.0, result.platesPerSide[0].plateWeight, 0.001)
        assertEquals(1, result.platesPerSide[0].count)
        assertEquals(0.5, result.platesPerSide[1].plateWeight, 0.001)
        assertEquals(1, result.platesPerSide[1].count)
    }

    @Test
    fun testIwfHexColorsStrictlyPreservedInPlateCounts() {
        // Target: 157.5kg on 20kg bar = 137.5kg plates -> 68.75kg per side
        // Per side: 2x25kg (50kg) + 1x15kg (15kg) + 1x2.5kg (2.5kg) + 1x1.25kg (1.25kg) = 68.75kg
        val result = PlateCalculator.calculatePlates(targetWeight = 157.5, barWeight = 20.0)

        assertEquals(68.75, result.weightPerSide, 0.001)
        assertEquals(0.0, result.remainder, 0.001)
        assertEquals(4, result.platesPerSide.size)

        // 25kg Red
        assertEquals(25.0, result.platesPerSide[0].plateWeight, 0.001)
        assertEquals(2, result.platesPerSide[0].count)
        assertEquals(0xFFD32F2FL, result.platesPerSide[0].hexColor)

        // 15kg Yellow
        assertEquals(15.0, result.platesPerSide[1].plateWeight, 0.001)
        assertEquals(1, result.platesPerSide[1].count)
        assertEquals(0xFFFBC02DL, result.platesPerSide[1].hexColor)

        // 2.5kg Black
        assertEquals(2.5, result.platesPerSide[2].plateWeight, 0.001)
        assertEquals(1, result.platesPerSide[2].count)
        assertEquals(0xFF212121L, result.platesPerSide[2].hexColor)

        // 1.25kg Chrome/Silver
        assertEquals(1.25, result.platesPerSide[3].plateWeight, 0.001)
        assertEquals(1, result.platesPerSide[3].count)
        assertEquals(0xFF9E9E9EL, result.platesPerSide[3].hexColor)
    }

    @Test
    fun testUnsortedInputPlateInventoryIsProperlyOrderedDescending() {
        // Caller supplies scrambled plates
        val scrambledPlates = listOf(
            5.0 to 0xFFFFFFFFL,
            25.0 to 0xFFD32F2FL,
            1.25 to 0xFF9E9E9EL,
            15.0 to 0xFFFBC02DL,
            20.0 to 0xFF1976D2L,
            10.0 to 0xFF388E3CL,
            2.5 to 0xFF212121L
        )

        // Target: 100kg on 20kg bar = 40kg per side -> greedy must pick 1x25kg + 1x15kg
        val result = PlateCalculator.calculatePlates(
            targetWeight = 100.0,
            barWeight = 20.0,
            availablePlates = scrambledPlates
        )

        assertEquals(40.0, result.weightPerSide, 0.001)
        assertEquals(0.0, result.remainder, 0.001)
        assertEquals(2, result.platesPerSide.size)
        assertEquals(25.0, result.platesPerSide[0].plateWeight, 0.001)
        assertEquals(1, result.platesPerSide[0].count)
        assertEquals(15.0, result.platesPerSide[1].plateWeight, 0.001)
        assertEquals(1, result.platesPerSide[1].count)
    }

    @Test
    fun testSubBarWeightBoundaryReturnsCleanEmptyPlates() {
        val justBelow = PlateCalculator.calculatePlates(targetWeight = 19.99, barWeight = 20.0)
        assertEquals(19.99, justBelow.totalWeight, 0.001)
        assertEquals(20.0, justBelow.barWeight, 0.001)
        assertEquals(0.0, justBelow.weightPerSide, 0.001)
        assertEquals(0.0, justBelow.remainder, 0.001)
        assertTrue(justBelow.platesPerSide.isEmpty())
    }

    @Test
    fun testExtremeLoad400KgBarbellPlateAllocation() {
        // 400kg on 20kg bar = 380kg total plates -> 190kg per side
        // Per side: 7x25kg (175kg) + 1x15kg (15kg) = 190kg
        val result = PlateCalculator.calculatePlates(targetWeight = 400.0, barWeight = 20.0)

        assertEquals(400.0, result.totalWeight, 0.001)
        assertEquals(20.0, result.barWeight, 0.001)
        assertEquals(190.0, result.weightPerSide, 0.001)
        assertEquals(0.0, result.remainder, 0.001)
        assertEquals(2, result.platesPerSide.size)
        assertEquals(25.0, result.platesPerSide[0].plateWeight, 0.001)
        assertEquals(7, result.platesPerSide[0].count)
        assertEquals(15.0, result.platesPerSide[1].plateWeight, 0.001)
        assertEquals(1, result.platesPerSide[1].count)
    }
}
