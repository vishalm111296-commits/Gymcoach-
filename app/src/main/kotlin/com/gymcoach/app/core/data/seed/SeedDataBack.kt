package com.gymcoach.app.core.data.seed

/**
 * Back / pulling. This category was completely absent from the old dataset —
 * it carries the highest V-taper weight (lats). Lat scores are ONLY assigned
 * to true pulling/extension movements per audit honesty rules.
 */
object SeedDataBack {

    val BACK = listOf(
        SeedExercise(
            name = "One-Arm Dumbbell Row",
            description = "Bench-supported single-arm row; the primary dumbbell lat builder.",
            muscleGroup = "Back", equipment = "Dumbbell + Flat Bench", difficulty = "Beginner",
            secondaryMuscles = "Upper Back, Biceps, Rear Deltoid", category = "pull",
            vtaperLat = 10, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 3,
            movementPattern = "horizontal_pull",
            muscles = listOf(MuscleRef("Lats", "primary"), MuscleRef("Upper Back", "secondary"), MuscleRef("Biceps", "secondary"), MuscleRef("Rear Deltoid", "secondary")),
            recommendedRepRange = "8-12 per side", recommendedRestTime = "90s",
            instructions = "One hand and same-side knee on bench, flat back. Pull dumbbell to hip, driving elbow past ribs, lower fully.",
            tips = "Pull toward your HIP, not your armpit, to bias lats over upper back.",
            setupInstructions = "Staggered stance, free foot planted wide for stability.",
            executionInstructions = "Initiate with shoulder blade depression, then elbow drive."
        ),
        SeedExercise(
            name = "Two-Arm Dumbbell Row",
            description = "Hip-hinged bilateral row allowing heavy loading of the whole back.",
            muscleGroup = "Back", equipment = "Dumbbell", difficulty = "Intermediate",
            secondaryMuscles = "Upper Back, Biceps, Lower Back", category = "pull",
            vtaperLat = 8, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 3,
            movementPattern = "horizontal_pull",
            muscles = listOf(MuscleRef("Lats", "primary"), MuscleRef("Upper Back", "secondary"), MuscleRef("Lower Back", "stabilizer"), MuscleRef("Biceps", "secondary")),
            recommendedRepRange = "8-12", recommendedRestTime = "90-120s"
        ),
        SeedExercise(
            name = "Chest-Supported Dumbbell Row",
            description = "Prone on flat bench — removes momentum and lower-back strain for strict upper-back work.",
            muscleGroup = "Back", equipment = "Dumbbell + Flat Bench", difficulty = "Beginner",
            secondaryMuscles = "Rear Deltoid, Biceps", category = "pull",
            vtaperLat = 6, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 5,
            movementPattern = "horizontal_pull",
            muscles = listOf(MuscleRef("Upper Back", "primary"), MuscleRef("Lats", "secondary"), MuscleRef("Rear Deltoid", "secondary")),
            recommendedRepRange = "10-15", recommendedRestTime = "75-90s"
        ),
        SeedExercise(
            name = "Wide-Elbow Dumbbell Row",
            description = "Elbows flared row shifting emphasis to rhomboids/mid-traps and rear delts.",
            muscleGroup = "Back", equipment = "Dumbbell", difficulty = "Intermediate",
            secondaryMuscles = "Rear Deltoid, Lats", category = "pull",
            vtaperLat = 2, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 6,
            movementPattern = "horizontal_pull",
            muscles = listOf(MuscleRef("Upper Back", "primary"), MuscleRef("Rear Deltoid", "secondary"), MuscleRef("Lats", "stabilizer")),
            recommendedRepRange = "10-15", recommendedRestTime = "75s"
        ),
        SeedExercise(
            name = "Renegade Row",
            description = "Plank-position alternating row combining back work with serious anti-rotation core demand.",
            muscleGroup = "Back", equipment = "Dumbbell", difficulty = "Advanced",
            secondaryMuscles = "Core, Upper Back, Triceps", category = "pull",
            vtaperLat = 6, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 3,
            movementPattern = "horizontal_pull",
            muscles = listOf(MuscleRef("Lats", "primary"), MuscleRef("Core", "stabilizer"), MuscleRef("Upper Back", "secondary")),
            recommendedRepRange = "6-10 per side", recommendedRestTime = "90s",
            tips = "Feet wide, hips square to floor; reduce weight if hips rotate."
        ),
        SeedExercise(
            name = "Dumbbell Pullover",
            description = "Overhead-to-hip arc emphasizing the long head of the lats through shoulder extension.",
            muscleGroup = "Back", equipment = "Dumbbell + Flat Bench", difficulty = "Intermediate",
            secondaryMuscles = "Chest, Triceps", category = "pull",
            vtaperLat = 8, vtaperLateralDelt = 0, vtaperUpperChest = 1, vtaperRearDelt = 0,
            movementPattern = "shoulder_extension",
            muscles = listOf(MuscleRef("Lats", "primary"), MuscleRef("Chest", "secondary"), MuscleRef("Triceps", "stabilizer")),
            recommendedRepRange = "10-15", recommendedRestTime = "75s",
            instructions = "Dumbbell held in both hands above chest, arms mostly straight, lower in an arc behind head until lat stretch, pull back over face to hips.",
            tips = "Keep ribcage down; stop at your comfortable end range."
        ),
        SeedExercise(
            name = "Straight-Arm Dumbbell Pullback",
            description = "Hinged straight-arm extension behind body isolating lat contraction without biceps.",
            muscleGroup = "Back", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "Triceps, Lower Back", category = "pull",
            vtaperLat = 7, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "shoulder_extension",
            muscles = listOf(MuscleRef("Lats", "primary"), MuscleRef("Triceps", "stabilizer")),
            recommendedRepRange = "12-15 per side", recommendedRestTime = "60s"
        ),
        SeedExercise(
            name = "Inverted Row (Table)",
            description = "Bodyweight horizontal pull under a sturdy table or fixed bar-height surface — the key vertical-pull substitute at home.",
            muscleGroup = "Back", equipment = "Bodyweight", difficulty = "Intermediate",
            secondaryMuscles = "Upper Back, Rear Deltoid, Biceps, Core", category = "pull",
            vtaperLat = 9, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 5,
            movementPattern = "horizontal_pull",
            muscles = listOf(MuscleRef("Lats", "primary"), MuscleRef("Upper Back", "secondary"), MuscleRef("Biceps", "secondary"), MuscleRef("Core", "stabilizer")),
            recommendedRepRange = "6-15", recommendedRestTime = "90s",
            instructions = "Lie beneath a very sturdy table edge, grip it, body straight heel-to-head. Pull chest to the edge, control down.",
            tips = "Verify the surface cannot tip or slide BEFORE loading. Test with partial weight first. Walk feet out to increase difficulty; elevate feet on bench to progress further."
        ),
        SeedExercise(
            name = "Superman",
            description = "Prone back extension strengthening erectors and building posterior-chain resilience.",
            muscleGroup = "Back", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "Glutes, Upper Back", category = "pull",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "spinal_extension",
            muscles = listOf(MuscleRef("Lower Back", "primary"), MuscleRef("Glutes", "secondary"), MuscleRef("Upper Back", "secondary")),
            recommendedRepRange = "12-20", recommendedRestTime = "45-60s"
        ),
        SeedExercise(
            name = "Reverse Snow Angel",
            description = "Prone sweeping arm pattern through full shoulder extension-to-flexion arc for rear delts and lower traps.",
            muscleGroup = "Back", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "Lower Back", category = "pull",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 7,
            movementPattern = "scapular",
            muscles = listOf(MuscleRef("Upper Back", "primary"), MuscleRef("Rear Deltoid", "primary"), MuscleRef("Traps", "secondary")),
            recommendedRepRange = "8-12 slow reps", recommendedRestTime = "45s"
        ),
        SeedExercise(
            name = "Dumbbell Good Morning",
            description = "Hinge pattern loading hamstrings and spinal erectors with light dumbbells.",
            muscleGroup = "Back", equipment = "Dumbbell", difficulty = "Intermediate",
            secondaryMuscles = "Hamstrings, Glutes", category = "legs",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "hinge",
            muscles = listOf(MuscleRef("Hamstrings", "primary"), MuscleRef("Lower Back", "secondary"), MuscleRef("Glutes", "secondary")),
            recommendedRepRange = "10-15", recommendedRestTime = "90s",
            tips = "Keep neutral spine; range stops when hamstrings run out — never force depth."
        )
    )
}
