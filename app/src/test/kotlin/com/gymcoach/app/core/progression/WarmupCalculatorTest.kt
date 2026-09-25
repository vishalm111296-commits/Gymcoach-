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

    @Test
    fun testIntermediateBarWeightEZCurlBar() {
        val plan = WarmupCalculator.calculateWarmupPlan(
            workingWeight = 50.0,
            barWeight = 10.0,
            plateStep = 1.0
        )
        assertEquals(50.0, plan.workingWeight, 0.001)
        assertEquals(10.0, plan.barWeight, 0.001)
        assertEquals(4, plan.sets.size)

        // Set 1: Empty bar 10kg
        assertEquals(10.0, plan.sets[0].weight, 0.001)
        assertEquals(10, plan.sets[0].reps)

        // Set 2: 50% = 25kg
        assertEquals(25.0, plan.sets[1].weight, 0.001)
        assertEquals(5, plan.sets[1].reps)

        // Set 3: 70% = 35kg
        assertEquals(35.0, plan.sets[2].weight, 0.001)
        assertEquals(3, plan.sets[2].reps)

        // Set 4: 85% = 42.5kg -> added = 32.5kg -> IEEE 754 round-half-to-even rounds to 32.0 -> 10 + 32 = 42.0kg
        assertEquals(42.0, plan.sets[3].weight, 0.001)
        assertEquals(1, plan.sets[3].reps)
    }

    @Test
    fun testUltraHeavyWorkingWeight300kgWarmupProgression() {
        val plan = WarmupCalculator.calculateWarmupPlan(
            workingWeight = 300.0,
            barWeight = 20.0,
            plateStep = 2.5
        )
        assertEquals(5, plan.sets.size)
        val weights = plan.sets.map { it.weight }
        assertEquals(listOf(20.0, 150.0, 210.0, 255.0, 275.0), weights)

        // Verify strictly monotonic progression
        for (i in 0 until weights.size - 1) {
            assertTrue("Warmup sets must strictly increase in load", weights[i] < weights[i + 1])
        }
        assertEquals("Post-activation potentiation", plan.sets.last().purpose)
    }

    @Test
    fun `heavy potentiation boundary strictly transitions between 139_9kg and 140_0kg`() {
        val plan1399 = WarmupCalculator.calculateWarmupPlan(
            workingWeight = 139.9,
            barWeight = 20.0,
            plateStep = 2.5
        )
        assertEquals(4, plan1399.sets.size)
        assertTrue(plan1399.sets.none { it.percentage == 0.92 })

        val plan1400 = WarmupCalculator.calculateWarmupPlan(
            workingWeight = 140.0,
            barWeight = 20.0,
            plateStep = 2.5
        )
        assertEquals(5, plan1400.sets.size)
        val lastSet = plan1400.sets.last()
        assertEquals(0.92, lastSet.percentage, 0.001)
        assertEquals(130.0, lastSet.weight, 0.001) // 140 * 0.92 = 128.8 -> 130.0
        assertEquals("Post-activation potentiation", lastSet.purpose)
    }

    @Test
    fun `estimatedDurationMinutes matches exact ceiling formula of rest and set performance time`() {
        // 100kg bench press: 4 sets
        // Rest: 45 + 60 + 90 + 120 = 315s. Performance: 4 * 20 = 80s. Total: 395s. ceil(395/60) = 7 mins
        val plan100 = WarmupCalculator.calculateWarmupPlan(workingWeight = 100.0, barWeight = 20.0)
        assertEquals(7, plan100.estimatedDurationMinutes)

        // 300kg squat: 5 sets
        // Rest: 45 + 60 + 90 + 120 + 150 = 465s. Performance: 5 * 20 = 100s. Total: 565s. ceil(565/60) = 10 mins
        val plan300 = WarmupCalculator.calculateWarmupPlan(workingWeight = 300.0, barWeight = 20.0)
        assertEquals(10, plan300.estimatedDurationMinutes)

        // Equal to bar weight (20kg): 1 set, 45s rest -> 2 mins
        val plan20 = WarmupCalculator.calculateWarmupPlan(workingWeight = 20.0, barWeight = 20.0)
        assertEquals(2, plan20.estimatedDurationMinutes)
    }

    @Test
    fun `warmup sets strictly exhibit non-increasing rep taper from activation to potentiation`() {
        val plan = WarmupCalculator.calculateWarmupPlan(workingWeight = 180.0, barWeight = 20.0)
        assertEquals(5, plan.sets.size)
        val reps = plan.sets.map { it.reps }

        assertEquals(listOf(10, 5, 3, 1, 1), reps)
        for (i in 0 until reps.size - 1) {
            assertTrue("Reps must monotonically taper or hold", reps[i] >= reps[i + 1])
        }
    }

    @Test
    fun `roundToStep handles fractional micro plate steps with specialty bars`() {
        // 25kg safety squat bar with 0.5kg plate steps
        val target = 82.3
        val rounded = WarmupCalculator.roundToStep(target, 25.0, 0.5)
        // 82.3 - 25 = 57.3 / 0.5 = 114.6 -> round is 115 -> 115 * 0.5 = 57.5 + 25 = 82.5
        assertEquals(82.5, rounded, 0.001)

        // Target below minWeight returns minWeight
        assertEquals(25.0, WarmupCalculator.roundToStep(24.9, 25.0, 0.5), 0.001)
        assertEquals(25.0, WarmupCalculator.roundToStep(10.0, 25.0, 0.5), 0.001)
    }

    @Test
    fun `sequential set number integrity is preserved when intermediate sets are suppressed`() {
        // With 35kg working load, 20kg bar, 2.5kg step:
        // Set 1: Empty bar 20kg (setNumber = 1)
        // 50% = 17.5kg <= 20kg -> suppressed!
        // 70% = 24.5kg -> round to 25.0kg -> added (setNumber = 2)
        // 85% = 29.75kg -> round to 30.0kg -> added (setNumber = 3)
        val plan = WarmupCalculator.calculateWarmupPlan(
            workingWeight = 35.0,
            barWeight = 20.0,
            plateStep = 2.5
        )
        assertEquals(3, plan.sets.size)
        assertEquals(listOf(1, 2, 3), plan.sets.map { it.setNumber })
        assertEquals(listOf(20.0, 25.0, 30.0), plan.sets.map { it.weight })
        assertEquals(listOf(10, 3, 1), plan.sets.map { it.reps })
        assertEquals(listOf(45, 90, 120), plan.sets.map { it.restSeconds })
        // Total rest: 45 + 90 + 120 = 255s. Performance: 3 * 20 = 60s. Total: 315s. ceil(315/60) = 6 min
        assertEquals(6, plan.estimatedDurationMinutes)
    }

    @Test
    fun `heavy load potentiation set is omitted when coarse plate step collides with primer set`() {
        // At 140kg with a coarse 20kg plate step:
        // 85% = 119kg -> (119-20)/20 = 4.95 -> 5 -> 20 + 100 = 120kg (set 4)
        // 92% = 128.8kg -> (128.8-20)/20 = 5.44 -> 5 -> 20 + 100 = 120kg
        // Because weight92 (120kg) is NOT > weight85 (120kg), potentiation set must be suppressed
        val plan = WarmupCalculator.calculateWarmupPlan(
            workingWeight = 140.0,
            barWeight = 20.0,
            plateStep = 20.0
        )
        assertEquals(4, plan.sets.size)
        assertTrue(plan.sets.none { it.percentage == 0.92 })
        val weights = plan.sets.map { it.weight }
        for (i in 0 until weights.size - 1) {
            assertTrue("Loads must strictly increase", weights[i] < weights[i + 1])
        }
        assertEquals(listOf(20.0, 60.0, 100.0, 120.0), weights)
    }

    @Test
    fun `specialty trap bar with microloading steps generates exact ramp and duration`() {
        // 30kg Trap Bar with 0.5kg microloading steps for a 175kg working load
        val plan = WarmupCalculator.calculateWarmupPlan(
            workingWeight = 175.0,
            barWeight = 30.0,
            plateStep = 0.5
        )
        assertEquals(175.0, plan.workingWeight, 0.001)
        assertEquals(30.0, plan.barWeight, 0.001)
        assertEquals(5, plan.sets.size)

        // Set 1: Barbell activation (30kg)
        assertEquals(30.0, plan.sets[0].weight, 0.001)
        assertEquals(10, plan.sets[0].reps)
        assertEquals(45, plan.sets[0].restSeconds)

        // Set 2: 50% = 87.5kg -> added = 57.5 -> /0.5 = 115.0 -> 87.5kg
        assertEquals(87.5, plan.sets[1].weight, 0.001)
        assertEquals(5, plan.sets[1].reps)
        assertEquals(60, plan.sets[1].restSeconds)

        // Set 3: 70% = 122.5kg -> added = 92.5 -> /0.5 = 185.0 -> 122.5kg
        assertEquals(122.5, plan.sets[2].weight, 0.001)
        assertEquals(3, plan.sets[2].reps)
        assertEquals(90, plan.sets[2].restSeconds)

        // Set 4: 85% = 148.75kg -> added = 118.75 -> /0.5 = 237.5 -> round(237.5) is 238.0 -> 30 + 119.0 = 149.0kg
        assertEquals(149.0, plan.sets[3].weight, 0.001)
        assertEquals(1, plan.sets[3].reps)
        assertEquals(120, plan.sets[3].restSeconds)

        // Set 5: 92% = 161.0kg -> added = 131.0 -> /0.5 = 262.0 -> 161.0kg
        assertEquals(161.0, plan.sets[4].weight, 0.001)
        assertEquals(1, plan.sets[4].reps)
        assertEquals(150, plan.sets[4].restSeconds)
        assertEquals("Post-activation potentiation", plan.sets[4].purpose)

        // Rest: 45 + 60 + 90 + 120 + 150 = 465s. Sets: 5 * 20 = 100s. Total = 565s. ceil(565 / 60) = 10 min
        assertEquals(10, plan.estimatedDurationMinutes)
    }

    @Test
    fun `zero and sub-bar working weights gracefully clamp to single activation set`() {
        val testLoads = listOf(0.0, -10.0, 5.0, 15.0, 20.0)
        for (load in testLoads) {
            val plan = WarmupCalculator.calculateWarmupPlan(
                workingWeight = load,
                barWeight = 20.0,
                plateStep = 2.5
            )
            assertEquals(1, plan.sets.size)
            val singleSet = plan.sets.first()
            assertEquals(1, singleSet.setNumber)
            assertEquals(20.0, singleSet.weight, 0.001)
            assertEquals(10, singleSet.reps)
            assertEquals(45, singleSet.restSeconds)
            assertEquals("Barbell activation & mobility", singleSet.purpose)
            assertEquals(1.0, singleSet.percentage, 0.001)
            assertEquals(2, plan.estimatedDurationMinutes)
        }
    }
}


