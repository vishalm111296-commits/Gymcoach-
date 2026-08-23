package com.gymcoach.app.core.data.seed

/** Arms + Core — completes the home-equipment exercise library. */
object SeedDataArmsCore {

    val ARMS = listOf(
        SeedExercise(
            name = "Dumbbell Curl",
            description = "Standard supinated curl; the base biceps mass builder.",
            muscleGroup = "Arms", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "Forearms", category = "pull",
            movementPattern = "elbow_flexion",
            muscles = listOf(MuscleRef("Biceps", "primary"), MuscleRef("Forearms", "secondary")),
            recommendedRepRange = "8-15", recommendedRestTime = "60s"
        ),
        SeedExercise(
            name = "Hammer Curl",
            description = "Neutral-grip curl biasing brachialis/forearm thickness.",
            muscleGroup = "Arms", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "Biceps", category = "pull",
            movementPattern = "elbow_flexion",
            muscles = listOf(MuscleRef("Biceps", "primary"), MuscleRef("Forearms", "primary")),
            recommendedRepRange = "8-15", recommendedRestTime = "60s"
        ),
        SeedExercise(
            name = "Concentration Curl",
            description = "Seated single-arm curl with elbow braced on thigh — strictest isolation, strong mind-muscle tool.",
            muscleGroup = "Arms", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "", category = "pull",
            movementPattern = "elbow_flexion",
            muscles = listOf(MuscleRef("Biceps", "primary")),
            recommendedRepRange = "10-15 per side", recommendedRestTime = "45-60s"
        ),
        SeedExercise(
            name = "Spider Curl",
            description = "Chest-supported curl on incline-angled bench edge; peak-tension at the top of the range.",
            muscleGroup = "Arms", equipment = "Dumbbell + Flat Bench", difficulty = "Intermediate",
            secondaryMuscles = "", category = "pull",
            movementPattern = "elbow_flexion",
            muscles = listOf(MuscleRef("Biceps", "primary")),
            recommendedRepRange = "10-15", recommendedRestTime = "60s"
        ),
        SeedExercise(
            name = "Cross-Body Hammer Curl",
            description = "Curl across torso toward opposite shoulder targeting the long head.",
            muscleGroup = "Arms", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "Forearms", category = "pull",
            movementPattern = "elbow_flexion",
            muscles = listOf(MuscleRef("Biceps", "primary"), MuscleRef("Forearms", "stabilizer")),
            recommendedRepRange = "10-12 per side", recommendedRestTime = "45-60s"
        ),
        SeedExercise(
            name = "Zottman Curl",
            description = "Curl up supinated, rotate, lower pronated — trains biceps and forearms in one rep.",
            muscleGroup = "Arms", equipment = "Dumbbell", difficulty = "Intermediate",
            secondaryMuscles = "Forearms", category = "pull",
            movementPattern = "elbow_flexion",
            muscles = listOf(MuscleRef("Biceps", "primary"), MuscleRef("Forearms", "primary")),
            recommendedRepRange = "8-12", recommendedRestTime = "60s"
        ),
        SeedExercise(
            name = "Overhead Triceps Extension",
            description = "Single-dumbbell overhead extension placing triceps long head under stretch.",
            muscleGroup = "Arms", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "", category = "push",
            movementPattern = "elbow_extension",
            muscles = listOf(MuscleRef("Triceps", "primary")),
            recommendedRepRange = "10-15", recommendedRestTime = "60s"
        ),
        SeedExercise(
            name = "Dumbbell Skull Crusher",
            description = "Lying extension lowering dumbbells beside the ears; classic medial/lateral head work.",
            muscleGroup = "Arms", equipment = "Dumbbell + Flat Bench", difficulty = "Beginner",
            secondaryMuscles = "", category = "push",
            movementPattern = "elbow_extension",
            muscles = listOf(MuscleRef("Triceps", "primary")),
            recommendedRepRange = "10-15", recommendedRestTime = "60s"
        ),
        SeedExercise(
            name = "Bench Dip",
            description = "Hands on flat bench, bodyweight dip; scalable via foot elevation and tempo.",
            muscleGroup = "Arms", equipment = "Bodyweight + Flat Bench", difficulty = "Beginner",
            secondaryMuscles = "Front Deltoid, Chest", category = "push",
            movementPattern = "elbow_extension",
            muscles = listOf(MuscleRef("Triceps", "primary"), MuscleRef("Front Deltoid", "secondary")),
            recommendedRepRange = "8-15", recommendedRestTime = "60s",
            tips = "Keep shoulders down away from ears; stop depth if shoulder discomfort appears."
        ),
        SeedExercise(
            name = "Triceps Kickback",
            description = "Hinged arm-back extension with peak contraction at full lockout.",
            muscleGroup = "Arms", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "", category = "push",
            movementPattern = "elbow_extension",
            muscles = listOf(MuscleRef("Triceps", "primary")),
            recommendedRepRange = "12-15 per side", recommendedRestTime = "45s"
        ),
        SeedExercise(
            name = "Wrist Curl",
            description = "Seated forearm flexor work off the knees.",
            muscleGroup = "Arms", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "", category = "pull",
            movementPattern = "wrist",
            muscles = listOf(MuscleRef("Forearms", "primary")),
            recommendedRepRange = "15-25", recommendedRestTime = "30-45s"
        )
    )

    val CORE = listOf(
        SeedExercise(
            name = "Plank",
            description = "Foundational anti-extension hold; quality over duration.",
            muscleGroup = "Core", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "Glutes", category = "core",
            movementPattern = "anti_extension",
            muscles = listOf(MuscleRef("Core", "primary"), MuscleRef("Glutes", "stabilizer")),
            recommendedRepRange = "20-60s hold", recommendedRestTime = "45s",
            tips = "Squeeze glutes and brace as if about to be poked; stop when form breaks."
        ),
        SeedExercise(
            name = "Side Plank",
            description = "Anti-lateral-flexion staple for obliques and hip stability.",
            muscleGroup = "Core", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "Glutes", category = "core",
            movementPattern = "anti_lateral_flexion",
            muscles = listOf(MuscleRef("Core", "primary"), MuscleRef("Glutes", "stabilizer")),
            recommendedRepRange = "20-45s per side", recommendedRestTime = "45s"
        ),
        SeedExercise(
            name = "Dead Bug",
            description = "Supinated opposite-arm/leg lower teaching ribcage-pelvis control.",
            muscleGroup = "Core", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "", category = "core",
            movementPattern = "anti_extension",
            muscles = listOf(MuscleRef("Core", "primary")),
            recommendedRepRange = "8-12 per side", recommendedRestTime = "45s"
        ),
        SeedExercise(
            name = "Bird Dog",
            description = "Quadruped opposite reach building anti-rotation control and spinal endurance.",
            muscleGroup = "Core", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "Glutes", category = "core",
            movementPattern = "anti_rotation",
            muscles = listOf(MuscleRef("Core", "primary"), MuscleRef("Glutes", "stabilizer")),
            recommendedRepRange = "6-10 per side slow", recommendedRestTime = "45s"
        ),
        SeedExercise(
            name = "Hollow Body Hold",
            description = "Gymnastics-grade anti-extension hold; the base for advanced core strength.",
            muscleGroup = "Core", equipment = "Bodyweight", difficulty = "Intermediate",
            secondaryMuscles = "", category = "core",
            movementPattern = "anti_extension",
            muscles = listOf(MuscleRef("Core", "primary")),
            recommendedRepRange = "15-40s hold", recommendedRestTime = "60s"
        ),
        SeedExercise(
            name = "Reverse Crunch",
            description = "Hip-flexion-dominant crunch biasing lower abdominals without neck strain.",
            muscleGroup = "Core", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "", category = "core",
            movementPattern = "spinal_flexion",
            muscles = listOf(MuscleRef("Core", "primary")),
            recommendedRepRange = "10-20", recommendedRestTime = "45s"
        ),
        SeedExercise(
            name = "Bicycle Crunch",
            description = "Rotational crunch combining oblique and rectus loading.",
            muscleGroup = "Core", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "", category = "core",
            movementPattern = "spinal_flexion",
            muscles = listOf(MuscleRef("Core", "primary")),
            recommendedRepRange = "12-20 slow", recommendedRestTime = "45s"
        ),
        SeedExercise(
            name = "Russian Twist",
            description = "Seated rotational hold-and-turn; add a dumbbell for load once controlled.",
            muscleGroup = "Core", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "", category = "core",
            movementPattern = "rotation",
            muscles = listOf(MuscleRef("Core", "primary")),
            recommendedRepRange = "16-24 total taps", recommendedRestTime = "45s",
            tips = "Rotate from mid-back, not lumbar; keep chest tall."
        ),
        SeedExercise(
            name = "Weighted Plank Drag-Through",
            description = "Dumbbell dragged side-to-side under plank position — anti-rotation plus shoulder stability.",
            muscleGroup = "Core", equipment = "Dumbbell + Bodyweight", difficulty = "Advanced",
            secondaryMuscles = "", category = "core",
            movementPattern = "anti_rotation",
            muscles = listOf(MuscleRef("Core", "primary"), MuscleRef("Lats", "stabilizer")),
            recommendedRepRange = "6-10 per side", recommendedRestTime = "60s"
        ),
        SeedExercise(
            name = "V-Sit Hold",
            description = "Open hip-angle static hold progressing hollow-body strength.",
            muscleGroup = "Core", equipment = "Bodyweight", difficulty = "Intermediate",
            secondaryMuscles = "", category = "core",
            movementPattern = "anti_extension",
            muscles = listOf(MuscleRef("Core", "primary")),
            recommendedRepRange = "15-30s hold", recommendedRestTime = "60s"
        )
    )
}
