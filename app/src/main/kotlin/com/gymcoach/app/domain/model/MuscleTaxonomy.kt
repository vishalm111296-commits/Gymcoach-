package com.gymcoach.app.domain.model

object CanonicalMuscleTaxonomy {
    private val CANONICAL_MAP = mapOf(
        "latissimus_dorsi" to "Lats",
        "lats" to "Lats",
        "back" to "Lats",
        "lateral_deltoid" to "Lateral Deltoid",
        "side_deltoid" to "Lateral Deltoid",
        "rear_deltoid" to "Rear Deltoid",
        "upper_chest" to "Upper Chest",
        "mid_chest" to "Upper Chest",
        "lower_chest" to "Upper Chest",
        "chest" to "Upper Chest",
        "upper_back" to "Upper Back",
        "biceps" to "Biceps",
        "brachialis" to "Biceps",
        "triceps" to "Triceps",
        "quadriceps" to "Quadriceps",
        "hamstrings" to "Hamstrings",
        "glutes" to "Glutes",
        "calves" to "Calves",
        "abs" to "Core",
        "obliques" to "Core",
        "deep_core" to "Core",
        "core" to "Core"
    )

    fun mapToCategory(rawName: String): String {
        val key = rawName.lowercase().trim().replace(" ", "_")
        return CANONICAL_MAP[key] ?: rawName.replace("_", " ").split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }
}
