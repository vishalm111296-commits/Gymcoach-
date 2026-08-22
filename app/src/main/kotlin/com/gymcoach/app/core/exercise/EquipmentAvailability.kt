package com.gymcoach.app.core.exercise

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EquipmentAvailability @Inject constructor() {

    private val GYM_EQUIPMENT = setOf(
        "Barbell", "Dumbbell", "Kettlebell", "EZ Bar", "Trap Bar",
        "Cable Machine", "Smith Machine", "Leg Press", "Hack Squat",
        "Leg Extension", "Leg Curl", "Pec Deck", "Lat Pulldown",
        "Seated Row", "Chest Press Machine", "Shoulder Press Machine",
        "Dip Station", "Pull-up Bar", "Bench", "Incline Bench",
        "Decline Bench", "Preacher Bench", "Cable Crossover",
        "Functional Trainer", "Landmine", "Safety Squat Bar",
        "Swiss Bar", "Bulgarian Bag", "Battle Rope", "Sled"
    )

    private val HOME_EQUIPMENT = setOf(
        "Dumbbell", "Kettlebell", "Resistance Band", "Bodyweight",
        "Pull-up Bar", "Dip Station", "Bench", "Floor",
        "Adjustable Dumbbell", "Doorway Pull-up Bar",
        "Suspension Trainer", "Foam Roller"
    )

    private val USER_EQUIPMENT = setOf(
        "Dumbbell", "Bodyweight", "Flat Bench"
    )

    fun getAvailableEquipment(equipmentType: String): Set<String> {
        return when (equipmentType.lowercase()) {
            "gym" -> GYM_EQUIPMENT
            "home" -> HOME_EQUIPMENT + USER_EQUIPMENT
            "custom" -> USER_EQUIPMENT
            else -> USER_EQUIPMENT
        }
    }

    fun isAvailable(equipment: String, equipmentType: String): Boolean {
        val available = getAvailableEquipment(equipmentType)
        return equipment in available || equipment == "Bodyweight"
    }
}