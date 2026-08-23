package com.gymcoach.app.core.data.seed

/**
 * Lower body: quads / hamstrings / glutes / calves.
 * All equipment dumbbell/bodyweight/bench per user inventory.
 * V-taper scores are 0 here by definition (lower body) — kept explicit
 * rather than omitted so the honesty contract is visible in code.
 */
object SeedDataLegs {

    val LEGS = listOf(
        SeedExercise(
            name = "Dumbbell Goblet Squat",
            description = "Front-loaded squat with a single dumbbell held at chest — easiest full-depth squat pattern to learn.",
            muscleGroup = "Legs", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "Glutes, Core", category = "legs",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "squat",
            muscles = listOf(MuscleRef("Quadriceps", "primary"), MuscleRef("Glutes", "secondary")),
            recommendedRepRange = "8-15", recommendedRestTime = "90s",
            instructions = "Hold dumbbell vertically at chest. Squat between your knees keeping torso upright until thighs reach parallel or below.",
            tips = "Elbows track inside knees at the bottom."
        ),
        SeedExercise(
            name = "Dumbbell Front Squat",
            description = "Two-dumbbell front rack squat; more quad-biased than goblet and scales heavier.",
            muscleGroup = "Legs", equipment = "Dumbbell", difficulty = "Intermediate",
            secondaryMuscles = "Glutes, Core", category = "legs",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "squat",
            muscles = listOf(MuscleRef("Quadriceps", "primary"), MuscleRef("Glutes", "secondary"), MuscleRef("Core", "stabilizer")),
            recommendedRepRange = "6-12", recommendedRestTime = "120s"
        ),
        SeedExercise(
            name = "Dumbbell Bulgarian Split Squat",
            description = "Rear-foot-elevated split squat using the bench; the highest-yield unilateral leg builder available.",
            muscleGroup = "Legs", equipment = "Dumbbell + Flat Bench", difficulty = "Intermediate",
            secondaryMuscles = "Hamstrings", category = "legs",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "lunge",
            muscles = listOf(MuscleRef("Quadriceps", "primary"), MuscleRef("Glutes", "secondary")),
            recommendedRepRange = "8-12 per side", recommendedRestTime = "90-120s",
            instructions = "Rear foot on bench, dumbbells at sides. Descend until rear knee nears floor and front thigh hits parallel.",
            tips = "Long stance biases glutes of the front leg; short stance biases quads."
        ),
        SeedExercise(
            name = "Dumbbell Reverse Lunge",
            description = "Step-back lunge gentler on the knees than forward lunges with equal hypertrophy value.",
            muscleGroup = "Legs", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "Glutes, Hamstrings", category = "legs",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "lunge",
            muscles = listOf(MuscleRef("Quadriceps", "primary"), MuscleRef("Glutes", "secondary")),
            recommendedRepRange = "10-12 per side", recommendedRestTime = "90s"
        ),
        SeedExercise(
            name = "Dumbbell Walking Lunge",
            description = "Continuous forward lunges for time-under-tension leg development.",
            muscleGroup = "Legs", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "Glutes, Calves", category = "legs",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "lunge",
            muscles = listOf(MuscleRef("Quadriceps", "primary"), MuscleRef("Glutes", "secondary")),
            recommendedRepRange = "20 steps", recommendedRestTime = "90s"
        ),
        SeedExercise(
            name = "Dumbbell Step-Up",
            description = "Step onto the flat bench holding dumbbells; knee-friendly unilateral strength.",
            muscleGroup = "Legs", equipment = "Dumbbell + Flat Bench", difficulty = "Beginner",
            secondaryMuscles = "Glutes, Calves", category = "legs",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "lunge",
            muscles = listOf(MuscleRef("Quadriceps", "primary"), MuscleRef("Glutes", "secondary")),
            recommendedRepRange = "10-12 per side", recommendedRestTime = "75-90s",
            instructions = "Place whole foot on bench, drive through the top foot to stand tall, lower slowly under control.",
            tips = "Do not push off the trailing leg; keep box height at knee or below."
        ),
        SeedExercise(
            name = "Bodyweight Squat",
            description = "Foundational squat pattern for beginners or high-rep finishers.",
            muscleGroup = "Legs", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "Glutes, Core", category = "legs",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "squat",
            muscles = listOf(MuscleRef("Quadriceps", "primary"), MuscleRef("Glutes", "secondary")),
            recommendedRepRange = "15-30", recommendedRestTime = "45-60s"
        ),
        SeedExercise(
            name = "Split Squat",
            description = "Static staggered-stance squat without elevation; bridge from bodyweight to Bulgarian.",
            muscleGroup = "Legs", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "Glutes", category = "legs",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "lunge",
            muscles = listOf(MuscleRef("Quadriceps", "primary"), MuscleRef("Glutes", "secondary")),
            recommendedRepRange = "10-15 per side", recommendedRestTime = "60s"
        ),
        SeedExercise(
            name = "Jump Squat",
            description = "Explosive bodyweight squat jump; power work and conditioning finisher.",
            muscleGroup = "Legs", equipment = "Bodyweight", difficulty = "Intermediate",
            secondaryMuscles = "Calves, Glutes", category = "legs",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "squat",
            muscles = listOf(MuscleRef("Quadriceps", "primary"), MuscleRef("Glutes", "secondary"), MuscleRef("Calves", "secondary")),
            recommendedRepRange = "5-10", recommendedRestTime = "90s",
            safetyNotes = "Land softly with bent knees; stop when height drops off."
        ),
        SeedExercise(
            name = "Romanian Deadlift",
            description = "Hip hinge with dumbbells loading hamstrings through stretch — primary hamstring builder.",
            muscleGroup = "Legs", equipment = "Dumbbell", difficulty = "Intermediate",
            secondaryMuscles = "Glutes, Lower Back", category = "legs",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "hinge",
            muscles = listOf(MuscleRef("Hamstrings", "primary"), MuscleRef("Glutes", "secondary"), MuscleRef("Lower Back", "stabilizer")),
            recommendedRepRange = "8-12", recommendedRestTime = "90-120s",
            instructions = "Soft knees, push hips back sliding dumbbells down your thighs until a deep hamstring stretch, drive hips forward to stand.",
            tips = "The dumbbells stay glued to your legs; spine stays neutral throughout.",
            setupInstructions = "Stand tall, dumbbells at hip crease, feet hip-width.",
            executionInstructions = "Hinge at hips — NOT a squat; shins stay roughly vertical."
        ),
        SeedExercise(
            name = "Single-Leg Romanian Deadlift",
            description = "Unilateral hinge demanding hamstring length plus balance and anti-rotation control.",
            muscleGroup = "Legs", equipment = "Dumbbell", difficulty = "Intermediate",
            secondaryMuscles = "Glutes, Core", category = "legs",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "hinge",
            muscles = listOf(MuscleRef("Hamstrings", "primary"), MuscleRef("Glutes", "stabilizer"), MuscleRef("Core", "stabilizer")),
            recommendedRepRange = "8-12 per side", recommendedRestTime = "75s"
        ),
        SeedExercise(
            name = "Glute Bridge",
            description = "Floor hip extension biasing glutes; regression/finisher for hip-dominant work.",
            muscleGroup = "Legs", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "Hamstrings, Lower Back", category = "legs",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "hip_extension",
            muscles = listOf(MuscleRef("Glutes", "primary"), MuscleRef("Hamstrings", "secondary")),
            recommendedRepRange = "12-20", recommendedRestTime = "60s"
        ),
        SeedExercise(
            name = "Hip Thrust (Bench-Supported)",
            description = "Shoulders on flat bench, loaded hip extension — strongest glute builder in the inventory.",
            muscleGroup = "Legs", equipment = "Dumbbell + Flat Bench", difficulty = "Intermediate",
            secondaryMuscles = "Hamstrings", category = "legs",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "hip_extension",
            muscles = listOf(MuscleRef("Glutes", "primary"), MuscleRef("Hamstrings", "secondary")),
            recommendedRepRange = "8-15", recommendedRestTime = "90s",
            instructions = "Upper back on bench edge, dumbbell over hips, drive hips to ceiling until torso level, squeeze glutes at top."
        ),
        SeedExercise(
            name = "Dumbbell Frog Pump",
            description = "Feet-together wide-knee bridge maximizing glute contraction at end range.",
            muscleGroup = "Legs", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "", category = "legs",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "hip_extension",
            muscles = listOf(MuscleRef("Glutes", "primary")),
            recommendedRepRange = "15-25", recommendedRestTime = "45s"
        ),
        SeedExercise(
            name = "Standing Dumbbell Calf Raise",
            description = "Straight-knee calf raise targeting gastrocnemius; slow tempo with full range.",
            muscleGroup = "Legs", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "", category = "legs",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "calf_raise",
            muscles = listOf(MuscleRef("Calves", "primary")),
            recommendedRepRange = "12-20", recommendedRestTime = "45-60s",
            instructions = "Dumbbells at sides, rise to tiptoes over 2s, pause at top, lower below level of a step if available.",
            tips = "Full stretch at bottom matters more than load for calves."
        ),
        SeedExercise(
            name = "Single-Leg Calf Raise",
            description = "Unilateral calf raise doubling the load per calf; balance demand included free of charge.",
            muscleGroup = "Legs", equipment = "Dumbbell", difficulty = "Intermediate",
            secondaryMuscles = "", category = "legs",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "calf_raise",
            muscles = listOf(MuscleRef("Calves", "primary")),
            recommendedRepRange = "10-15 per side", recommendedRestTime = "45s"
        ),
        SeedExercise(
            name = "Wall Tibialis Raise",
            description = "Back-to-wall shin raise for tibialis anterior — injury-proofing and ankle health.",
            muscleGroup = "Legs", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "", category = "legs",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "calf_raise",
            // Taxonomy gap (documented): target muscle tibialis anterior is not
            // in the 18-muscle model, so this accessory intentionally declares
            // no primary. Contributes zero training volume by design.
            muscles = listOf(MuscleRef("Calves", "stabilizer")),
            recommendedRepRange = "15-25", recommendedRestTime = "30-45s"
        ),
        SeedExercise(
            name = "Dumbbell Curtsy Lunge",
            description = "Diagonal crossover lunge hitting glute medius and adductors alongside quads.",
            muscleGroup = "Legs", equipment = "Dumbbell", difficulty = "Intermediate",
            secondaryMuscles = "Glutes, Quadriceps", category = "legs",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "lunge",
            muscles = listOf(MuscleRef("Glutes", "primary"), MuscleRef("Quadriceps", "secondary")),
            recommendedRepRange = "10-12 per side", recommendedRestTime = "75s"
        )
    )
}
