package com.gymcoach.app.core.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WarmupCalculatorTest {

    @Test
    fun testStandardBenchPressWarmup100kg() {
        val plan = WarmupCalculator.calculateWarmupPlan(
            workingWeight = 100.0,
            barWeight = 20.0,
            plateStep = 2.5
        )

        assertEquals(100.0, plan.workingWeight, 0.001)
        assertEquals(20.0, plan.barWeight, 0.001)
        assertEquals(4, plan.sets.size)

        // Set 1: Empty bar (20kg) x 10 reps
        val set1 = plan.sets[0]
        assertEquals(1, set1.setNumber)
        assertEquals(20.0, set1.weight, 0.001)
        assertEquals(10, set1.reps)

        // Set 2: 50% = 50kg x 5 reps
        val set2 = plan.sets[1]
        assertEquals(2, set2.setNumber)
        assertEquals(50.0, set2.weight, 0.001)
        assertEquals(5, set2.reps)

        // Set 3: 70% = 70kg x 3 reps
        val set3 = plan.sets[2]
        assertEquals(3, set3.setNumber)
        assertEquals(70.0, set3.weight, 0.001)
        assertEquals(3, set3.reps)

        // Set 4: 85% = 85kg x 1 rep
        val set4 = plan.sets[3]
        assertEquals(4, set4.setNumber)
        assertEquals(85.0, set4.weight, 0.001)
        assertEquals(1, set4.reps)

        assertTrue(plan.estimatedDurationMinutes in 5..8)
    }

    @Test
    fun testHeavySquatWarmup160kgIncludesPotentiationSet() {
        val plan = WarmupCalculator.calculateWarmupPlan(
            workingWeight = 160.0,
            barWeight = 20.0,
            plateStep = 2.5
        )

        // Should include 5 sets: bar, 50% (80kg), 70% (112.5kg), 85% (135kg), 92% (147.5kg)
        assertEquals(5, plan.sets.size)
        val set5 = plan.sets.last()
        assertEquals(5, set5.setNumber)
        assertEquals(1, set5.reps)
        assertTrue(set5.weight >= 145.0 && set5.weight <= 150.0)
        assertEquals("Post-activation potentiation", set5.purpose)
    }

    @Test
    fun testWorkingWeightEqualsBarWeight() {
        val plan = WarmupCalculator.calculateWarmupPlan(
            workingWeight = 20.0,
            barWeight = 20.0
        )

        assertEquals(1, plan.sets.size)
        assertEquals(20.0, plan.sets[0].weight, 0.001)
        assertEquals(10, plan.sets[0].reps)
    }

    @Test
    fun testRoundingToPlateSteps() {
        val rounded = WarmupCalculator.roundToStep(61.8, 20.0, 2.5)
        // 61.8 - 20 = 41.8 / 2.5 = 16.72 -> round is 17 -> 17*2.5 = 42.5 + 20 = 62.5
        assertEquals(62.5, rounded, 0.001)

        // Below minWeight returns minWeight
        assertEquals(20.0, WarmupCalculator.roundToStep(15.0, 20.0, 2.5), 0.001)
        assertEquals(20.0, WarmupCalculator.roundToStep(20.0, 20.0, 2.5), 0.001)
    }

    @Test
    fun testWorkingWeightLessThanBarWeightReturnsSingleSet() {
        val plan = WarmupCalculator.calculateWarmupPlan(
            workingWeight = 15.0,
            barWeight = 20.0
        )
        assertEquals(1, plan.sets.size)
        assertEquals(20.0, plan.sets[0].weight, 0.001)
        assertEquals(10, plan.sets[0].reps)
        assertEquals(2, plan.estimatedDurationMinutes)
    }

    @Test
    fun testHeavyThresholdBoundary139kgVs140kg() {
        val plan139 = WarmupCalculator.calculateWarmupPlan(
            workingWeight = 139.0,
            barWeight = 20.0,
            plateStep = 2.5
        )
        // Under 140kg should have at most 4 sets (no 92% potentiation set)
        assertEquals(4, plan139.sets.size)
        assertTrue(plan139.sets.none { it.percentage == 0.92 })

        val plan140 = WarmupCalculator.calculateWarmupPlan(
            workingWeight = 140.0,
            barWeight = 20.0,
            plateStep = 2.5
        )
        // 140kg should have 5 sets including 92% potentiation set
        assertEquals(5, plan140.sets.size)
        assertEquals(0.92, plan140.sets.last().percentage, 0.001)
        assertEquals("Post-activation potentiation", plan140.sets.last().purpose)
    }

    @Test
    fun testCustomBarWeightAndPlateStep() {
        // 15kg women's Olympic bar with 1.25kg plate steps for 80kg squat
        val plan = WarmupCalculator.calculateWarmupPlan(
            workingWeight = 80.0,
            barWeight = 15.0,
            plateStep = 1.25
        )
        assertEquals(15.0, plan.barWeight, 0.001)
        assertEquals(15.0, plan.sets[0].weight, 0.001)
        assertTrue(plan.sets.size >= 3)
        // Verify every set weight respects the 1.25 step from the 15kg base
        for (s in plan.sets) {
            val plateWeight = s.weight - 15.0
            val remainder = Math.round(plateWeight * 100.0) % 125L
            assertEquals(0L, remainder)
        }
    }

    @Test
    fun testLightWorkingWeightSuppressesSubBarSets() {
        val plan = WarmupCalculator.calculateWarmupPlan(
            workingWeight = 30.0,
            barWeight = 20.0,
            plateStep = 2.5
        )
        // Set 1: Empty Bar (20kg) x 10
        // 50% = 15kg (suppressed <= 20)
        // 70% = 21kg -> rounded to 20kg (suppressed <= 20)
        // 85% = 25.5kg -> rounded to 25.0kg (included)
        assertEquals(2, plan.sets.size)
        assertEquals(20.0, plan.sets[0].weight, 0.001)
        assertEquals(10, plan.sets[0].reps)
        assertEquals(25.0, plan.sets[1].weight, 0.001)
        assertEquals(1, plan.sets[1].reps)
        assertEquals(0.85, plan.sets[1].percentage, 0.001)
    }

    @Test
    fun testLargePlateStepGranularity() {
        // 5.0kg plate steps on 120kg working load
        val plan = WarmupCalculator.calculateWarmupPlan(
            workingWeight = 120.0,
            barWeight = 20.0,
            plateStep = 5.0
        )
        assertTrue(plan.sets.size >= 4)
        for (s in plan.sets) {
            val addedWeight = s.weight - 20.0
            val remainder = Math.round(addedWeight * 100.0) % 500L
            assertEquals(0L, remainder)
        }
    }
}
