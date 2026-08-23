package com.gymcoach.app.core.data.seed

/**
 * Lightweight seed-time models. Resolved to Room entities by ExerciseSeeder
 * (muscle/equipment names are resolved to auto-generated IDs at insert time).
 *
 * Equipment vocabulary is constrained to what the target user owns:
 * "Dumbbell", "Bodyweight", "Flat Bench" (audit 2026-08-22).
 */
data class SeedMuscle(
    val name: String,
    val displayName: String,
    val bodyRegion: String
)

data class SeedEquipment(
    val name: String,
    val displayName: String,
    val category: String
)

data class SeedExercise(
    val name: String,
    val description: String,
    val muscleGroup: String,          // broad display group: Chest/Back/Shoulders/Legs/Arms/Core
    val equipment: String,            // one of: Dumbbell / Bodyweight / Flat Bench / Dumbbell + Flat Bench
    val difficulty: String,           // Beginner / Intermediate / Advanced
    val secondaryMuscles: String = "",
    val instructions: String = "",
    val tips: String = "",
    val recommendedRepRange: String = "",
    val recommendedRestTime: String = "",
    val category: String = "",        // push / pull / legs / core
    // V-taper relevance 0-10. Lats scored ONLY for true pulling movements.
    val vtaperLat: Int = 0,
    val vtaperLateralDelt: Int = 0,
    val vtaperUpperChest: Int = 0,
    val vtaperRearDelt: Int = 0,
    val movementPattern: String = "", // horizontal_push / vertical_pull / hinge / ...
    // Muscle assignments resolved by name at seed time.
    val muscles: List<MuscleRef> = emptyList(),
    val setupInstructions: String = "",
    val executionInstructions: String = ""
)

data class MuscleRef(val name: String, val role: String) // role: primary|secondary|stabilizer

object SeedReference {

    /**
     * Canonical muscle taxonomy. The 12 names below MUST match the exact keys
     * used by VolumeCalculator.TrainingBalance (Lats, Lateral Deltoid, ...).
     */
    val MUSCLES = listOf(
        SeedMuscle("Lats", "Latissimus Dorsi", "Back"),
        SeedMuscle("Upper Back", "Upper Back (Rhomboids/Mid-Traps)", "Back"),
        SeedMuscle("Lower Back", "Erector Spinae", "Back"),
        SeedMuscle("Traps", "Trapezius", "Back"),
        SeedMuscle("Lateral Deltoid", "Lateral Deltoid", "Shoulders"),
        SeedMuscle("Front Deltoid", "Anterior Deltoid", "Shoulders"),
        SeedMuscle("Rear Deltoid", "Posterior Deltoid", "Shoulders"),
        SeedMuscle("Chest", "Pectoralis Major", "Chest"),
        SeedMuscle("Upper Chest", "Clavicular Pectoralis", "Chest"),
        SeedMuscle("Biceps", "Biceps Brachii", "Arms"),
        SeedMuscle("Triceps", "Triceps Brachii", "Arms"),
        SeedMuscle("Forearms", "Forearm Flexors/Extensors", "Arms"),
        SeedMuscle("Quadriceps", "Quadriceps Femoris", "Legs"),
        SeedMuscle("Hamstrings", "Hamstrings", "Legs"),
        SeedMuscle("Glutes", "Gluteus Maximus/Medius", "Legs"),
        SeedMuscle("Calves", "Gastrocnemius/Soleus", "Legs"),
        SeedMuscle("Adductors", "Hip Adductors", "Legs"),
        SeedMuscle("Core", "Abdominals/Deep Core", "Core")
    )

    /** User's actual equipment inventory — nothing else is offered in programs. */
    val EQUIPMENT = listOf(
        SeedEquipment("Dumbbell", "Dumbbells", "Free Weights"),
        SeedEquipment("Flat Bench", "Flat Workout Bench", "Benches"),
        SeedEquipment("Bodyweight", "Bodyweight Only", "None")
    )
}
