package com.gymcoach.app.core.exercise

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Equipment availability by access level.
 * Equipment names MUST match ExerciseEntity.equipment values in the database seed data.
 *
 * Database seed equipment keys (lowercase):
 * "dumbbell", "bench", "bodyweight", "cable", "machine", "kettlebell", "barbell", etc.
 *
 * Compound equipment uses "," separator (ExerciseSeeder joins asset arrays with
 * joinToString(",")), e.g. "dumbbell,bench". Legacy "+" separators are tolerated.
 */
@Singleton
class EquipmentAvailability @Inject constructor() {

    private val GYM_EQUIPMENT = setOf(
        "barbell", "dumbbell", "kettlebell", "ez bar", "trap bar",
        "cable", "smith machine", "leg press", "hack squat",
        "leg extension", "leg curl", "pec deck", "lat pulldown",
        "seated row", "chest press machine", "shoulder press machine",
        "dip station", "pull-up bar", "bench", "incline bench",
        "decline bench", "preacher bench", "cable crossover",
        "functional trainer", "landmine", "safety squat bar",
        "swiss bar", "bulgarian bag", "battle rope", "sled",
        "bodyweight"
    )

    private val HOME_EQUIPMENT = setOf(
        "dumbbell", "kettlebell", "resistance band", "bodyweight",
        "pull-up bar", "dip station", "bench", "floor",
        "adjustable dumbbell", "doorway pull-up bar",
        "suspension trainer", "foam roller"
    )

    private val CUSTOM_EQUIPMENT = setOf("bodyweight")

    fun getAvailableEquipment(equipmentType: String): Set<String> {
        val typeLower = equipmentType.lowercase().trim()
        return when (typeLower) {
            "gym" -> GYM_EQUIPMENT
            "home" -> HOME_EQUIPMENT
            "custom" -> CUSTOM_EQUIPMENT
            "" -> CUSTOM_EQUIPMENT
            else -> {
                val customSet = typeLower.split(",")
                    .map { it.trim() }
                    .map { token ->
                        when (token) {
                            "flat bench" -> "bench"
                            "adjustable dumbbell" -> "dumbbell"
                            else -> token
                        }
                    }
                    .filter { it.isNotBlank() }
                    .toSet()
                customSet + "bodyweight"
            }
        }
    }

    fun isAvailable(equipment: String, equipmentType: String): Boolean {
        val available = getAvailableEquipment(equipmentType)
        return equipment in available || equipment == "bodyweight"
    }

    /**
     * True when the exercise requires gear outside the user's available set —
     * used by ProgressionEngine to switch from weight progression to rep/set
     * progression when load cannot increase further.
     *
     * Compound requirements like "dumbbell,bench" are satisfied only if
     * every token is available.
     */
    fun isLimited(equipment: String, equipmentType: String): Boolean {
        if (equipment.isBlank()) return false
        return equipment.replace("+", ",")
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() && it != "bodyweight" }
            .any { !isAvailable(it, equipmentType) }
    }
}
