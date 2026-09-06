package com.gymcoach.app.core.home

import com.gymcoach.app.domain.model.Exercise

/**
 * Maps an [Exercise] to the list of balance/dashboard muscle names it contributes
 * volume to, using only real repository metadata.
 *
 * Attribution uses the repository's seeded V-taper relevance scores
 * ([Exercise.vtaperLat] / vtaperLateralDelt / vtaperRearDelt / vtaperUpperChest)
 * and the muscle_taxonomy vocabulary for secondary muscle ids (comma-separated in
 * [Exercise.secondaryMuscles]). Primary-muscle ids are not stored on [Exercise],
 * so non-critical muscles (biceps/triceps/quadriceps/hamstrings/glutes/calves/
 * core) are attributed via secondary taxonomy ids. The "Legs" pseudo-muscle exists
 * only for the dashboard bar; it is not part of the training balance.
 */
object VtaperAttribution {

    private val CORE_TAXONOMY_IDS = setOf("abs", "obliques", "deep_core")

    /** Distinct muscle names this exercise credits, in a stable order. */
    fun contributors(ex: Exercise): List<String> {
        val result = mutableListOf<String>()
        if (ex.vtaperLat > 0) result.add("Lats")
        if (ex.vtaperLateralDelt > 0) result.add("Lateral Deltoid")
        if (ex.vtaperRearDelt > 0) result.add("Rear Deltoid")
        if (ex.vtaperUpperChest > 0) result.add("Upper Chest")
        if (ex.muscleGroup == "back") result.add("Upper Back")

        val secondary = ex.secondaryMuscles.split(',').map { it.trim() }
        if ("biceps" in secondary) result.add("Biceps")
        if ("triceps" in secondary) result.add("Triceps")
        if ("quadriceps" in secondary) result.add("Quadriceps")
        if ("hamstrings" in secondary) result.add("Hamstrings")
        if ("glutes" in secondary) result.add("Glutes")
        if ("calves" in secondary) result.add("Calves")
        if (ex.muscleGroup == "core" || secondary.any { it in CORE_TAXONOMY_IDS }) result.add("Core")
        if (ex.muscleGroup == "legs") result.add("Legs")

        return result.distinct()
    }
}
