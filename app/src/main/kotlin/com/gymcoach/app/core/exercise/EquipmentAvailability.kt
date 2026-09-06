package com.gymcoach.app.core.exercise

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Equipment availability by access level.
 * Compound requirements are comma- or plus-delimited and require every item.
 */
@Singleton
class EquipmentAvailability @Inject constructor() {

    private val gymEquipment = setOf(
        "barbell", "dumbbell", "kettlebell", "ez bar", "trap bar",
        "cable", "smith machine", "leg press", "hack squat",
        "leg extension", "leg curl", "pec deck", "lat pulldown",
        "seated row", "chest press machine", "shoulder press machine",
        "dip station", "pull-up bar", "bench", "incline bench",
        "decline bench", "preacher bench", "cable crossover",
        "functional trainer", "landmine", "safety squat bar", "swiss bar",
        "bulgarian bag", "battle rope", "sled", "bodyweight"
    )

    /** Generic home profile for users with additional home equipment. */
    private val homeEquipment = setOf(
        "dumbbell", "kettlebell", "resistance band", "bodyweight", "pull-up bar",
        "dip station", "floor", "adjustable dumbbell", "doorway pull-up bar",
        "suspension trainer", "foam roller"
    )

    /** Strict V-taper profile: dumbbells + bodyweight only. */
    private val dumbbellBodyweightEquipment = setOf(
        "dumbbell", "adjustable dumbbell", "bodyweight", "floor"
    )

    private val bodyweightOnly = setOf("bodyweight", "floor")

    fun getAvailableEquipment(equipmentType: String): Set<String> = when (equipmentType.trim().lowercase()) {
        "gym" -> gymEquipment
        "home" -> homeEquipment
        "dumbbell_bodyweight", "dumbbell+bodyweight", "limited_home", "default" -> dumbbellBodyweightEquipment
        "custom", "bodyweight", "bodyweight_only" -> bodyweightOnly
        else -> homeEquipment
    }

    fun isAvailable(equipment: String, equipmentType: String): Boolean {
        val normalized = equipment.trim().lowercase().replace("_", " ")
        if (normalized.isBlank() || normalized == "bodyweight") return true
        return normalized in getAvailableEquipment(equipmentType)
    }

    /** True when one or more required non-bodyweight items are unavailable. */
    fun isLimited(equipment: String, equipmentType: String): Boolean {
        val requirements = equipment
            .replace("+", ",")
            .split(",")
            .map { it.trim().lowercase().replace("_", " ") }
            .filter { it.isNotBlank() && it != "bodyweight" }
        return requirements.any { !isAvailable(it, equipmentType) }
    }
}
