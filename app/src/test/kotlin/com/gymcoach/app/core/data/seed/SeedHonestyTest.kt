package com.gymcoach.app.core.data.seed

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0 honesty contract: the V-taper scoring rules and data-format hygiene,
 * enforced so pseudo-science cannot creep back in via a careless edit.
 */
class SeedHonestyTest {

    private val allExercises: List<SeedExercise> =
        SeedDataChestShoulders.CHEST +
            SeedDataChestShoulders.SHOULDERS +
            SeedDataBack.BACK +
            SeedDataLegs.LEGS +
            SeedDataArmsCore.ARMS +
            SeedDataArmsCore.CORE

    private val repRange = Regex("^\\d+(-\\d+)?(s)?( (hold|per side|steps))?$")

    @Test
    fun `presses never claim lat contribution`() {
        // Honesty rule: horizontal/vertical pressing does not build back width.
        val offenders = allExercises.filter { ex ->
            ex.movementPattern in setOf("horizontal_push", "vertical_push") &&
                ex.vtaperLat != 0
        }
        assertTrue("Lat score on press: ${offenders.map { it.name }}", offenders.isEmpty())
    }

    @Test
    fun `v-taper scores are non-negative`() {
        val negative = allExercises.filter { ex ->
            ex.vtaperLat < 0 || ex.vtaperLateralDelt < 0 ||
                ex.vtaperUpperChest < 0 || ex.vtaperRearDelt < 0
        }
        assertTrue("Negative scores: ${negative.map { it.name }}", negative.isEmpty())
    }

    @Test
    fun `rep ranges are machine-parseable`() {
        // Prose like "8-12 slow reps" or "16-24 total taps" breaks the
        // ProgramGenerator's range parsing. Formats allowed: 12, 8-12,
        // 30-60s hold, 10 per side, 20 steps.
        val bad = allExercises.filter { ex -> !repRange.matches(ex.recommendedRepRange.trim()) }
        assertTrue("Unparseable ranges: ${bad.map { it.name to it.recommendedRepRange }}", bad.isEmpty())
    }

    @Test
    fun `every exercise declares a movement pattern`() {
        val blank = allExercises.filter { it.movementPattern.isBlank() }
        assertTrue("Blank patterns: ${blank.map { it.name }}", blank.isEmpty())
    }
}
