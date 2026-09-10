package com.gymcoach.app.core.program

import com.gymcoach.app.core.assessment.VShapeAssessment
import java.util.Locale

// TrainingBalance, MuscleVolume and VolumeStatus are nested in VolumeCalculator.
// Imported here explicitly so the engine stays independent of the Room-backed
// calculator's surrounding dependencies; the real nested types are used in CI.
import com.gymcoach.app.core.program.VolumeCalculator.MuscleVolume
import com.gymcoach.app.core.program.VolumeCalculator.TrainingBalance
import com.gymcoach.app.core.program.VolumeCalculator.VolumeStatus

/**
 * Adaptive programming engine: converts the V-shape assessment plus the
 * current weekly training balance into concrete, conservative program actions.
 *
 * All thresholds, wording and recommendations are PRODUCT HEURISTICS for
 * fitness tracking guidance. They are NOT clinical diagnoses, medical
 * assessments, or evidence-based anthropometric standards. Do not use for
 * medical decision-making.
 *
 * Rule set (deterministic, in output order):
 *  1. VOLUME_SHIFT for every V-taper muscle whose weeklyVolume < 10
 *     (INSUFFICIENT evidence band): add 2-4 sets/week.
 *  2. DELOAD for every V-taper muscle whose weeklyVolume > 21
 *     (EXCESSIVE evidence band): reduce 2-4 sets/week or take a light week.
 *  3. If no shift/deload applies to the five V-taper muscles AND the
 *     V-taper training scores are both >= 3.0 (optimal): LOAD_BUMP.
 *  4. VARIATION when stallWeeks >= 3 (no observed progression for 3+ weeks).
 *  5. If the action list is still empty: a single BALANCED maintenance action.
 *
 * The assessment level (including NOT_ENOUGH_DATA) never suppresses program
 * actions; it is carried for context and future (Phase 8) outcome coupling.
 */
class AdaptiveProgramEngine {

    data class AdaptiveProgramAction(
        val type: AdaptiveActionType,
        val title: String,
        val detail: String,
        val muscleName: String? = null
    )

    enum class AdaptiveActionType {
        VOLUME_SHIFT, DELOAD, LOAD_BUMP, VARIATION, BALANCED
    }

    /** Weekly volume below this evidence floor triggers VOLUME_SHIFT. */
    private val INSUFFICIENT_BELOW = 10.0

    /** Weekly volume above this evidence ceiling triggers DELOAD. */
    private val EXCESSIVE_ABOVE = 21.0

    /** Stall weeks at or above this threshold trigger a VARIATION action. */
    private val STALL_WEEKS_THRESHOLD = 3

    private data class MuscleVolumeRef(
        val name: String,
        val weeklyVolume: Double,
        val status: VolumeStatus
    )

    fun adapt(
        assessment: VShapeAssessment,
        balance: TrainingBalance,
        stallWeeks: Int
    ): List<AdaptiveProgramAction> {
        require(stallWeeks >= 0) { "stallWeeks must be >= 0" }
        val actions = mutableListOf<AdaptiveProgramAction>()

        // V-taper primary muscles (lats + lateral delts) and secondary muscles
        // (rear delts + upper chest + upper back), in fixed display order.
        val vTaperMuscles = listOf(
            MuscleVolumeRef("Lats", balance.latVolume.weeklyVolume, balance.latVolume.status),
            MuscleVolumeRef("Lateral Deltoid", balance.lateralDeltVolume.weeklyVolume, balance.lateralDeltVolume.status),
            MuscleVolumeRef("Rear Deltoid", balance.rearDeltVolume.weeklyVolume, balance.rearDeltVolume.status),
            MuscleVolumeRef("Upper Chest", balance.upperChestVolume.weeklyVolume, balance.upperChestVolume.status),
            MuscleVolumeRef("Upper Back", balance.upperBackVolume.weeklyVolume, balance.upperBackVolume.status)
        )

        // Rule 1: volume shifts for underloaded V-taper muscles.
        // Rule 2: deloads for overreached V-taper muscles.
        for (muscle in vTaperMuscles) {
            if (muscle.weeklyVolume < INSUFFICIENT_BELOW) {
                actions += AdaptiveProgramAction(
                    type = AdaptiveActionType.VOLUME_SHIFT,
                    title = "Add ${muscle.name} volume",
                    detail = "Weekly ${muscle.name} volume (${formatVolume(muscle.weeklyVolume)}) is below the " +
                        "evidence band (10-18 sets/week). Add 2-4 sets spread across sessions.",
                    muscleName = muscle.name
                )
            } else if (muscle.weeklyVolume > EXCESSIVE_ABOVE) {
                actions += AdaptiveProgramAction(
                    type = AdaptiveActionType.DELOAD,
                    title = "Reduce ${muscle.name} volume",
                    detail = "Weekly ${muscle.name} volume (${formatVolume(muscle.weeklyVolume)}) is above the " +
                        "evidence band. Reduce 2-4 sets/week or take a light week.",
                    muscleName = muscle.name
                )
            }
        }

        // Rule 3: load bump only when no V-taper muscle needs a shift or deload.
        if (actions.isEmpty()) {
            val primaryScore = (balance.latVolume.status.ordinal + balance.lateralDeltVolume.status.ordinal) / 2.0
            val secondaryScore = (balance.rearDeltVolume.status.ordinal +
                balance.upperChestVolume.status.ordinal + balance.upperBackVolume.status.ordinal) / 3.0
            if (primaryScore >= 3.0 && secondaryScore >= 3.0) {
                actions += AdaptiveProgramAction(
                    type = AdaptiveActionType.LOAD_BUMP,
                    title = "Progress load on balanced muscles",
                    detail = "All V-taper muscles are in or near the evidence band; " +
                        "increase load when all sets hit the top of the rep range."
                )
            }
        }

        // Rule 4: variation after a stall (regardless of other actions).
        if (stallWeeks >= STALL_WEEKS_THRESHOLD) {
            actions += AdaptiveProgramAction(
                type = AdaptiveActionType.VARIATION,
                title = "Try a variation after a stall",
                detail = "No progression for 3+ weeks; swap one stalled exercise for a " +
                    "similar movement to reintroduce a stimulus."
            )
        }

        // Rule 5: guaranteed non-empty result.
        if (actions.isEmpty()) {
            actions += AdaptiveProgramAction(
                type = AdaptiveActionType.BALANCED,
                title = "Maintain current program",
                detail = "All V-taper muscles are in or near the evidence band (10-18 sets/week)."
            )
        }

        return actions
    }

    private fun formatVolume(volume: Double): String =
        String.format(Locale.US, "%.1f", volume)
}