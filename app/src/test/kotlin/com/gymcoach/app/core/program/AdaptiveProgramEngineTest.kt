package com.gymcoach.app.core.program

import com.gymcoach.app.core.assessment.VShapeAssessmentCalculator
import com.gymcoach.app.core.program.AdaptiveProgramEngine.AdaptiveActionType
import com.gymcoach.app.core.program.VolumeCalculator.MuscleVolume
import com.gymcoach.app.core.program.VolumeCalculator.TrainingBalance
import com.gymcoach.app.core.program.VolumeCalculator.VolumeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveProgramEngineTest {

    private val engine = AdaptiveProgramEngine()

    /** Neutral muscle: MODERATE (12 sets/week) — within evidence band, ordinal 1. */
    private fun neutral(): MuscleVolume =
        MuscleVolume(
            muscleName = "NeutralMuscle",
            weeklyVolume = 12.0,
            directSets = 12,
            indirectSets = 0,
            status = VolumeStatus.MODERATE
        )

    private fun muscle(
        name: String,
        weeklyVolume: Double,
        status: VolumeStatus
    ): MuscleVolume =
        MuscleVolume(
            muscleName = name,
            weeklyVolume = weeklyVolume,
            directSets = weeklyVolume.toInt(),
            indirectSets = 0,
            status = status
        )

    /**
     * Builds a balance where the five V-taper muscles use [lat], [lateralDelt],
     * [rearDelt], [upperChest], [upperBack] and the seven non-V-taper muscles
     * are neutral (no shift, no deload, no score contribution to V-taper).
     */
    private fun balance(
        lat: MuscleVolume,
        lateralDelt: MuscleVolume,
        rearDelt: MuscleVolume,
        upperChest: MuscleVolume,
        upperBack: MuscleVolume
    ): TrainingBalance = TrainingBalance(
        latVolume = lat,
        lateralDeltVolume = lateralDelt,
        rearDeltVolume = rearDelt,
        upperChestVolume = upperChest,
        upperBackVolume = upperBack,
        bicepsVolume = neutral(),
        tricepsVolume = neutral(),
        quadricepsVolume = neutral(),
        hamstringsVolume = neutral(),
        glutesVolume = neutral(),
        calvesVolume = neutral(),
        coreVolume = neutral()
    )

    private fun insufficient(): MuscleVolume =
        muscle("Lats", 8.0, VolumeStatus.INSUFFICIENT)

    private fun optimal(): MuscleVolume =
        muscle("Lats", 16.0, VolumeStatus.OPTIMAL)

    private fun excessive(): MuscleVolume =
        muscle("Lats", 24.0, VolumeStatus.EXCESSIVE)

    /** Assessment with no measurements -> NOT_ENOUGH_DATA level. */
    private fun notEnoughDataAssessment() =
        VShapeAssessmentCalculator.assess(0.0, 0.0, 0.0, 2.0, 2.0)

    /** Balanced in-band, mix of MODERATE/OPTIMAL/HIGH so scores stay < 3.0. */
    private fun perfectBalanceMix(): TrainingBalance = balance(
        lat = muscle("Lats", 12.0, VolumeStatus.MODERATE),
        lateralDelt = muscle("Lateral Deltoid", 16.0, VolumeStatus.OPTIMAL),
        rearDelt = muscle("Rear Deltoid", 20.0, VolumeStatus.HIGH),
        upperChest = muscle("Upper Chest", 16.0, VolumeStatus.OPTIMAL),
        upperBack = muscle("Upper Back", 12.0, VolumeStatus.MODERATE)
    )

    // ------------------------------------------------------------------
    // Rule 1: VOLUME_SHIFT for underloaded (< 10 sets/week) V-taper muscles
    // ------------------------------------------------------------------

    @Test
    fun allVTaperMusclesInsufficient_yieldsFiveVolumeShifts() {
        val b = balance(insufficient(), insufficient(), insufficient(), insufficient(), insufficient())
        val actions = engine.adapt(notEnoughDataAssessment(), b, stallWeeks = 0)

        assertEquals(5, actions.size)
        assertTrue(actions.all { it.type == AdaptiveActionType.VOLUME_SHIFT })
        assertEquals(
            listOf("Lats", "Lateral Deltoid", "Rear Deltoid", "Upper Chest", "Upper Back"),
            actions.map { it.muscleName }
        )
    }

    @Test
    fun singleMuscleInsufficient_yieldsOnlyThatShift() {
        val b = balance(insufficient(), optimal(), optimal(), optimal(), optimal())
        val actions = engine.adapt(notEnoughDataAssessment(), b, stallWeeks = 0)

        assertEquals(1, actions.size)
        assertEquals(AdaptiveActionType.VOLUME_SHIFT, actions[0].type)
        assertEquals("Lats", actions[0].muscleName)
        assertTrue(actions[0].title.contains("Lats"))
        assertTrue(actions[0].detail.contains("8.0"))
        assertTrue(actions[0].detail.contains("10-18"))
    }

    // ------------------------------------------------------------------
    // Rule 2: DELOAD for overreached (> 21 sets/week) V-taper muscles
    // ------------------------------------------------------------------

    @Test
    fun rearDeltExcessive_yieldsDeloadOnly() {
        val b = balance(optimal(), optimal(), excessive(), optimal(), optimal())
        val actions = engine.adapt(notEnoughDataAssessment(), b, stallWeeks = 0)

        assertEquals(1, actions.size)
        assertEquals(AdaptiveActionType.DELOAD, actions[0].type)
        assertEquals("Rear Deltoid", actions[0].muscleName)
        assertTrue(actions[0].detail.contains("24.0"))
    }

    @Test
    fun latExcessive_yieldsDeloadOnly() {
        val b = balance(excessive(), optimal(), optimal(), optimal(), optimal())
        val actions = engine.adapt(notEnoughDataAssessment(), b, stallWeeks = 0)

        assertEquals(1, actions.size)
        assertEquals(AdaptiveActionType.DELOAD, actions[0].type)
        assertEquals("Lats", actions[0].muscleName)
    }

    // ------------------------------------------------------------------
    // Rule 3: LOAD_BUMP when all V-taper muscles in/near band + scores >= 3
    // ------------------------------------------------------------------

    @Test
    fun optimalScoresAndInBandVolumes_yieldsLoadBump() {
        val b = balance(optimal(), optimal(), optimal(), optimal(), optimal())
        val actions = engine.adapt(notEnoughDataAssessment(), b, stallWeeks = 0)

        assertEquals(1, actions.size)
        assertEquals(AdaptiveActionType.LOAD_BUMP, actions[0].type)
        assertEquals(null, actions[0].muscleName)
        assertTrue(actions[0].detail.contains("evidence band"))
    }

    @Test
    fun nearOptimalScoresBelowThree_noLoadBump() {
        // lat MODERATE(1) + lateralDelt OPTIMAL(3) -> primary 2.0; all in band -> BALANCED
        val b = perfectBalanceMix()
        val actions = engine.adapt(notEnoughDataAssessment(), b, stallWeeks = 0)

        assertEquals(1, actions.size)
        assertEquals(AdaptiveActionType.BALANCED, actions[0].type)
    }

    // ------------------------------------------------------------------
    // Rule 4: VARIATION after a stall (3+ weeks)
    // ------------------------------------------------------------------

    @Test
    fun stallThreeWeeks_appendsVariationAfterLoadBump() {
        val b = balance(optimal(), optimal(), optimal(), optimal(), optimal())
        val actions = engine.adapt(notEnoughDataAssessment(), b, stallWeeks = 3)

        assertEquals(2, actions.size)
        assertEquals(AdaptiveActionType.LOAD_BUMP, actions[0].type)
        assertEquals(AdaptiveActionType.VARIATION, actions[1].type)
        assertTrue(actions[1].detail.contains("3+ weeks"))
    }

    @Test
    fun stallTwoWeeks_noVariation() {
        val b = perfectBalanceMix()
        val actions = engine.adapt(notEnoughDataAssessment(), b, stallWeeks = 2)

        assertEquals(1, actions.size)
        assertEquals(AdaptiveActionType.BALANCED, actions[0].type)
    }

    // ------------------------------------------------------------------
    // Rule 5: BALANCED fallback — guaranteed non-empty
    // ------------------------------------------------------------------

    @Test
    fun perfectProgram_yieldsSingleBalanced() {
        val b = perfectBalanceMix()
        val actions = engine.adapt(notEnoughDataAssessment(), b, stallWeeks = 0)

        assertEquals(1, actions.size)
        assertEquals(AdaptiveActionType.BALANCED, actions[0].type)
        assertEquals("Maintain current program", actions[0].title)
    }

    // ------------------------------------------------------------------
    // NOT_ENOUGH_DATA must NOT suppress program actions
    // ------------------------------------------------------------------

    @Test
    fun notEnoughDataAssessment_stillYieldsProgramActions() {
        val assessment = notEnoughDataAssessment()
        assertEquals("NOT_ENOUGH_DATA", assessment.level.name)

        val actions = engine.adapt(assessment, perfectBalanceMix(), stallWeeks = 0)
        assertEquals(1, actions.size)
        assertEquals(AdaptiveActionType.BALANCED, actions[0].type)
    }

    // ------------------------------------------------------------------
    // Deterministic ordering: SHIFT -> DELOAD -> VARIATION -> LOAD_BUMP -> BALANCED
    // ------------------------------------------------------------------

    @Test
    fun mixedConditions_orderingIsShiftThenDeloadThenVariation() {
        val b = balance(insufficient(), optimal(), excessive(), optimal(), optimal())
        val actions = engine.adapt(notEnoughDataAssessment(), b, stallWeeks = 3)

        assertEquals(3, actions.size)
        assertEquals(AdaptiveActionType.VOLUME_SHIFT, actions[0].type)
        assertEquals("Lats", actions[0].muscleName)
        assertEquals(AdaptiveActionType.DELOAD, actions[1].type)
        assertEquals("Rear Deltoid", actions[1].muscleName)
        assertEquals(AdaptiveActionType.VARIATION, actions[2].type)
    }

    @Test
    fun shiftPresent_suppressesLoadBump() {
        // lat insufficient (shift) but other four optimal -> primary score >= 3?
        // ordinal lat INSUFFICIENT=0, lateralDelt OPTIMAL=3 -> primary 1.5; regardless,
        // rule 3 only fires on empty list, so a shift must suppress LOAD_BUMP.
        val b = balance(insufficient(), optimal(), optimal(), optimal(), optimal())
        val actions = engine.adapt(notEnoughDataAssessment(), b, stallWeeks = 0)

        assertEquals(1, actions.size)
        assertEquals(AdaptiveActionType.VOLUME_SHIFT, actions[0].type)
        assertTrue(actions.none { it.type == AdaptiveActionType.LOAD_BUMP })
    }

    // ------------------------------------------------------------------
    // Robustness + formatting
    // ------------------------------------------------------------------

    @Test
    fun nanVolumeInput_doesNotCrash() {
        val nan = muscle("Lats", Double.NaN, VolumeStatus.HIGH)
        val b = balance(nan, optimal(), optimal(), optimal(), optimal())

        // NaN comparisons are all false -> no shift/deload; scores from status ordinals
        // HIGH(2) + OPTIMAL(3) -> primary 2.5 -> no load bump; no stall -> BALANCED
        val actions = engine.adapt(notEnoughDataAssessment(), b, stallWeeks = 0)
        assertEquals(1, actions.size)
        assertEquals(AdaptiveActionType.BALANCED, actions[0].type)
    }

    @Test
    fun negativeVolumeInput_treatedAsInsufficient() {
        val negative = muscle("Lats", -5.0, VolumeStatus.INSUFFICIENT)
        val b = balance(negative, optimal(), optimal(), optimal(), optimal())
        val actions = engine.adapt(notEnoughDataAssessment(), b, stallWeeks = 0)

        assertEquals(1, actions.size)
        assertEquals(AdaptiveActionType.VOLUME_SHIFT, actions[0].type)
        assertEquals("Lats", actions[0].muscleName)
    }

    @Test
    fun localeUsFormatting_singleDecimalPrecision() {
        // 9.5 rounds to "9.5" under Locale.US; ensures no comma decimal separator
        val nineFive = muscle("Lats", 9.5, VolumeStatus.INSUFFICIENT)
        val b = balance(nineFive, optimal(), optimal(), optimal(), optimal())
        val actions = engine.adapt(notEnoughDataAssessment(), b, stallWeeks = 0)

        assertEquals(1, actions.size)
        assertTrue(actions[0].detail.contains("9.5"))
        assertTrue(!actions[0].detail.contains("9,5"))
    }

    @Test
    fun negativeStallWeeks_throwsIllegalArgument() {
        val exception = try {
            engine.adapt(notEnoughDataAssessment(), perfectBalanceMix(), stallWeeks = -1)
            null
        } catch (e: IllegalArgumentException) {
            e
        }
        assertEquals(true, exception != null)
    }
}