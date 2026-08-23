package com.gymcoach.app.core.data.seed

/**
 * Arms + Core. Biceps/triceps get direct work AFTER the pressing/pulling
 * volume they already receive indirectly. Core carries anti-rotation +
 * anti-extension patterns (not just flexion) for real-world transfer.
 */
object SeedDataArmsCore {

    val ARMS = listOf(
        SeedExercise(
            name = "Dumbbell Biceps Curl",
            description = "Standard standing curl; bread-and-butter elbow flexion.",
            muscleGroup = "Arms", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "Forearms", category = "pull",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "elbow_flexion",
            muscles = listOf(MuscleRef("Biceps", "primary"), MuscleRef("Forearms", "secondary")),
            recommendedRepRange = "8-15", recommendedRestTime = "60s",
            instructions = "Curl both dumbbells without swinging, supinate slightly at the top, lower over 2-3 seconds.",
            tips = "Pin elbows to your ribs; no shoulder drift forward."
        ),
        SeedExercise(
            name = "Alternating Dumbbell Curl",
            description = "One-arm-at-a-time curl allowing stricter focus and slight overload per arm.",
            muscleGroup = "Arms", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "Forearms", category = "pull",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "elbow_flexion",
            muscles = listOf(MuscleRef("Biceps", "primary"), MuscleRef("Forearms", "secondary")),
            recommendedRepRange = "8-12 per side", recommendedRestTime = "60s"
        ),
        SeedExercise(
            name = "Incline Dumbbell Curl",
            description = "Bench-reclined curl placing biceps in a long-length stretch — superior stimulus position.",
            muscleGroup = "Arms", equipment = "Dumbbell + Flat Bench", difficulty = "Intermediate",
            secondaryMuscles = "", category = "pull",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "elbow_flexion",
            muscles = listOf(MuscleRef("Biceps", "primary")),
            recommendedRepRange = "8-12", recommendedRestTime = "75s",
            instructions = "Sit reclined on the bench, arms hanging straight down behind torso, curl without letting elbows travel forward."
        ),
        SeedExercise(
            name = "Hammer Curl",
            description = "Neutral-grip curl emphasizing brachialis and forearm depth alongside the biceps.",
            muscleGroup = "Arms", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "", category = "pull",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "elbow_flexion",
            muscles = listOf(MuscleRef("Biceps", "primary"), MuscleRef("Forearms", "secondary")),
            recommendedRepRange = "8-15", recommendedRestTime = "60s"
        ),
        SeedExercise(
            name = "Concentration Curl",
            description = "Seated single-arm curl braced against the inner thigh for peak isolation.",
            muscleGroup = "Arms", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "", category = "pull",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "elbow_flexion",
            muscles = listOf(MuscleRef("Biceps", "primary")),
            recommendedRepRange = "10-15 per side", recommendedRestTime = "45-60s"
        ),
        SeedExercise(
            name = "Dumbbell Skull Crusher",
            description = "Classic lying triceps extension loading the long head through elbow extension.",
            muscleGroup = "Arms", equipment = "Dumbbell + Flat Bench", difficulty = "Intermediate",
            secondaryMuscles = "", category = "push",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "elbow_extension",
            muscles = listOf(MuscleRef("Triceps", "primary")),
            recommendedRepRange = "8-12", recommendedRestTime = "75s",
            instructions = "Lie on bench, dumbbells overhead, bend elbows lowering heads beside your ears, extend back up without moving upper arms.",
            tips = "Keep upper arms vertical-ish; slight forearm angle toward the head protects the elbows."
        ),
        SeedExercise(
            name = "Overhead Dumbbell Triceps Extension",
            description = "Overhead stretch-position triceps work; best long-head builder available.",
            muscleGroup = "Arms", equipment = "Dumbbell", difficulty = "Intermediate",
            secondaryMuscles = "Core", category = "push",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "elbow_extension",
            muscles = listOf(MuscleRef("Triceps", "primary"), MuscleRef("Core", "stabilizer")),
            recommendedRepRange = "10-15", recommendedRestTime = "75s"
        ),
        SeedExercise(
            name = "Single-Arm Overhead Triceps Extension",
            description = "Unilateral version exposing and fixing left-right triceps asymmetry.",
            muscleGroup = "Arms", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "", category = "push",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "elbow_extension",
            muscles = listOf(MuscleRef("Triceps", "primary")),
            recommendedRepRange = "10-12 per side", recommendedRestTime = "60s"
        ),
        SeedExercise(
            name = "Close-Grip Push-Up",
            description = "Hands under shoulders pushing elbows tight — triceps-dominant pressing.",
            muscleGroup = "Arms", equipment = "Bodyweight", difficulty = "Intermediate",
            secondaryMuscles = "Chest, Front Deltoid", category = "push",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 1, vtaperRearDelt = 0,
            movementPattern = "horizontal_push",
            muscles = listOf(MuscleRef("Triceps", "primary"), MuscleRef("Chest", "secondary")),
            recommendedRepRange = "8-15", recommendedRestTime = "60s"
        ),
        SeedExercise(
            name = "Bench Dip",
            description = "Hands-behind dip on flat bench; scalable triceps volume from easy to hard leverage.",
            muscleGroup = "Arms", equipment = "Bodyweight + Flat Bench", difficulty = "Beginner",
            secondaryMuscles = "Front Deltoid", category = "push",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "vertical_push",
            muscles = listOf(MuscleRef("Triceps", "primary"), MuscleRef("Front Deltoid", "secondary")),
            recommendedRepRange = "8-15", recommendedRestTime = "60s",
            safetyNotes = "Stop short of shoulder discomfort at the bottom; keep hips close to bench.",
            instructions = "Heels on floor, hands on bench edge, lower until elbows hit ~90 degrees, press back up."
        )
    )

    val CORE = listOf(
        SeedExercise(
            name = "Plank",
            description = "Isometric brace; foundation for anti-extension core strength.",
            muscleGroup = "Core", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "Front Deltoid, Glutes", category = "core",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "core_anti_extension",
            muscles = listOf(MuscleRef("Core", "primary"), MuscleRef("Glutes", "stabilizer")),
            recommendedRepRange = "30-60s hold", recommendedRestTime = "45s"
        ),
        SeedExercise(
            name = "Side Plank",
            description = "Anti-lateral-flexion hold building obliques and hip stability.",
            muscleGroup = "Core", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "Glutes", category = "core",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "core_anti_lateral",
            muscles = listOf(MuscleRef("Core", "primary")),
            recommendedRepRange = "30-45s per side", recommendedRestTime = "45s"
        ),
        SeedExercise(
            name = "Weighted Plank Drag-Through",
            description = "Plank while dragging a dumbbell across the floor — dynamic anti-rotation.",
            muscleGroup = "Core", equipment = "Dumbbell + Bodyweight", difficulty = "Advanced",
            secondaryMuscles = "Lats, Glutes", category = "core",
            vtaperLat = 1, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "core_anti_rotation",
            muscles = listOf(MuscleRef("Core", "primary"), MuscleRef("Lats", "secondary")),
            recommendedRepRange = "6-10 per side", recommendedRestTime = "60s"
        ),
        SeedExercise(
            name = "Dead Bug",
            description = "Supine opposite-arm-leg drop teaching rib-down brace under limb motion.",
            muscleGroup = "Core", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "", category = "core",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "core_anti_extension",
            muscles = listOf(MuscleRef("Core", "primary")),
            recommendedRepRange = "8-12 per side", recommendedRestTime = "45s"
        ),
        SeedExercise(
            name = "Bird Dog",
            description = "Quadruped opposite extension training spinal stability and hip control.",
            muscleGroup = "Core", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "Glutes, Upper Back", category = "core",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "core_stability",
            muscles = listOf(MuscleRef("Core", "primary"), MuscleRef("Glutes", "secondary")),
            recommendedRepRange = "8-10 per side", recommendedRestTime = "45s"
        ),
        SeedExercise(
            name = "Russian Twist",
            description = "Seated rotation with dumbbell tap; oblique-focused dynamic core.",
            muscleGroup = "Core", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "", category = "core",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "core_rotation",
            muscles = listOf(MuscleRef("Core", "primary")),
            recommendedRepRange = "16-24", recommendedRestTime = "45s"
        ),
        SeedExercise(
            name = "Bicycle Crunch",
            description = "Rotational flexion pairing elbow-to-knee; highest rectus+oblique co-activation crunch variant.",
            muscleGroup = "Core", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "", category = "core",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "core_flexion",
            muscles = listOf(MuscleRef("Core", "primary")),
            recommendedRepRange = "12-20", recommendedRestTime = "45s"
        ),
        SeedExercise(
            name = "Reverse Crunch",
            description = "Pelvis-driven flexion emphasizing lower rectus fibers without neck strain.",
            muscleGroup = "Core", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "", category = "core",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "core_flexion",
            muscles = listOf(MuscleRef("Core", "primary")),
            recommendedRepRange = "10-15", recommendedRestTime = "45s"
        ),
        SeedExercise(
            name = "Hollow Body Hold",
            description = "Gymnastics-grade midline hold; strict anti-extension under maximum lever length.",
            muscleGroup = "Core", equipment = "Bodyweight", difficulty = "Advanced",
            secondaryMuscles = "Front Deltoid", category = "core",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "core_anti_extension",
            muscles = listOf(MuscleRef("Core", "primary")),
            recommendedRepRange = "20-40s hold", recommendedRestTime = "60s"
        ),
        SeedExercise(
            name = "V-Up",
            description = "Full-body fold from hollow position; advanced flexion power-endurance.",
            muscleGroup = "Core", equipment = "Bodyweight", difficulty = "Advanced",
            secondaryMuscles = "Hamstrings", category = "core",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "core_flexion",
            muscles = listOf(MuscleRef("Core", "primary")),
            recommendedRepRange = "8-15", recommendedRestTime = "60s"
        )
    )
}
