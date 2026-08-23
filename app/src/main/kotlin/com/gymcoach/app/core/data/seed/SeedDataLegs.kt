package com.gymcoach.app.core.data.seed

/**
 * Legs — guarantees non-zero hamstring/calf coverage with home equipment
 * (fixes audit finding: previous home config yielded 0 hamstring/calf sets).
 */
object SeedDataLegs {

    val LEGS = listOf(
        SeedExercise(
            name = "Goblet Squat",
            description = "Front-loaded squat; easiest squat pattern to learn with excellent depth feedback.",
            muscleGroup = "Legs", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "Glutes, Core", category = "legs",
            movementPattern = "squat",
            muscles = listOf(MuscleRef("Quadriceps", "primary"), MuscleRef("Glutes", "secondary"), MuscleRef("Core", "stabilizer")),
            recommendedRepRange = "8-15", recommendedRestTime = "90s"
        ),
        SeedExercise(
            name = "Dumbbell Front Squat",
            description = "Two-dumbbell rack-position squat for heavier quad loading than goblet allows.",
            muscleGroup = "Legs", equipment = "Dumbbell", difficulty = "Intermediate",
            secondaryMuscles = "Core, Upper Back", category = "legs",
            movementPattern = "squat",
            muscles = listOf(MuscleRef("Quadriceps", "primary"), MuscleRef("Glutes", "primary"), MuscleRef("Core", "stabilizer")),
            recommendedRepRange = "6-12", recommendedRestTime = "120s"
        ),
        SeedExercise(
            name = "Sumo Squat (Dumbbell)",
            description = "Wide-stance dumbbell squat biasing adductors and glutes.",
            muscleGroup = "Legs", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "Quadriceps, Core", category = "legs",
            movementPattern = "squat",
            muscles = listOf(MuscleRef("Glutes", "primary"), MuscleRef("Adductors", "primary"), MuscleRef("Quadriceps", "secondary")),
            recommendedRepRange = "10-15", recommendedRestTime = "75s"
        ),
        SeedExercise(
            name = "Dumbbell Romanian Deadlift",
            description = "The primary home-equipment hamstring builder; loaded stretch through full hinge.",
            muscleGroup = "Legs", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "Glutes, Lower Back", category = "legs",
            movementPattern = "hinge",
            muscles = listOf(MuscleRef("Hamstrings", "primary"), MuscleRef("Glutes", "primary"), MuscleRef("Lower Back", "stabilizer")),
            recommendedRepRange = "8-12", recommendedRestTime = "90-120s",
            instructions = "Soft knees, push hips back sliding dumbbells down thighs until deep hamstring stretch, drive hips forward to stand.",
            tips = "The bar path stays glued to your legs; range is dictated by hamstring flexibility."
        ),
        SeedExercise(
            name = "Single-Leg Dumbbell RDL",
            description = "Unilateral hinge demanding balance; strong glute-medius and hamstring stimulus.",
            muscleGroup = "Legs", equipment = "Dumbbell", difficulty = "Intermediate",
            secondaryMuscles = "Lower Back, Adductors", category = "legs",
            movementPattern = "hinge",
            muscles = listOf(MuscleRef("Hamstrings", "primary"), MuscleRef("Glutes", "primary"), MuscleRef("Adductors", "stabilizer")),
            recommendedRepRange = "8-12 per side", recommendedRestTime = "75s"
        ),
        SeedExercise(
            name = "Bulgarian Split Squat",
            description = "Rear foot on flat bench; brutal unilateral quad/glute builder — highest leg-exercise value per set at home.",
            muscleGroup = "Legs", equipment = "Bodyweight + Flat Bench", difficulty = "Intermediate",
            secondaryMuscles = "Glutes, Hamstrings", category = "legs",
            movementPattern = "lunge",
            muscles = listOf(MuscleRef("Quadriceps", "primary"), MuscleRef("Glutes", "primary"), MuscleRef("Hamstrings", "stabilizer")),
            recommendedRepRange = "8-12 per side", recommendedRestTime = "90-120s"
        ),
        SeedExercise(
            name = "Weighted Bulgarian Split Squat",
            description = "Dumbbells in hands raise intensity once bodyweight sets exceed target reps easily.",
            muscleGroup = "Legs", equipment = "Dumbbell + Flat Bench", difficulty = "Advanced",
            secondaryMuscles = "Hamstrings, Core", category = "legs",
            movementPattern = "lunge",
            muscles = listOf(MuscleRef("Quadriceps", "primary"), MuscleRef("Glutes", "primary")),
            recommendedRepRange = "6-10 per side", recommendedRestTime = "120s"
        ),
        SeedExercise(
            name = "Static Split Squat",
            description = "Floor-based lunge hold position; stepping-stone before Bulgarian variation.",
            muscleGroup = "Legs", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "Glutes, Calves", category = "legs",
            movementPattern = "lunge",
            muscles = listOf(MuscleRef("Quadriceps", "primary"), MuscleRef("Glutes", "secondary")),
            recommendedRepRange = "10-15 per side", recommendedRestTime = "60s"
        ),
        SeedExercise(
            name = "Reverse Lunge",
            description = "Knee-friendly lunge pattern; rear-foot emphasis spares the front knee.",
            muscleGroup = "Legs", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "Glutes, Hamstrings", category = "legs",
            movementPattern = "lunge",
            muscles = listOf(MuscleRef("Quadriceps", "primary"), MuscleRef("Glutes", "secondary")),
            recommendedRepRange = "10-15 per side", recommendedRestTime = "60-75s"
        ),
        SeedExercise(
            name = "Lateral Lunge",
            description = "Frontal-plane lunge hitting adductors and improving hip mobility.",
            muscleGroup = "Legs", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "Glutes, Quadriceps", category = "legs",
            movementPattern = "lunge",
            muscles = listOf(MuscleRef("Adductors", "primary"), MuscleRef("Glutes", "secondary"), MuscleRef("Quadriceps", "secondary")),
            recommendedRepRange = "8-12 per side", recommendedRestTime = "60s"
        ),
        SeedExercise(
            name = "Step-Up (Flat Bench)",
            description = "Explosive single-leg drive onto bench; scales via height of knee drive and tempo.",
            muscleGroup = "Legs", equipment = "Bodyweight + Flat Bench", difficulty = "Beginner",
            secondaryMuscles = "Calves, Core", category = "legs",
            movementPattern = "lunge",
            muscles = listOf(MuscleRef("Quadriceps", "primary"), MuscleRef("Glutes", "primary"), MuscleRef("Calves", "stabilizer")),
            recommendedRepRange = "10-15 per side", recommendedRestTime = "60-75s"
        ),
        SeedExercise(
            name = "Air Squat",
            description = "Foundational bodyweight squat; high-rep conditioning or warm-up tool.",
            muscleGroup = "Legs", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "Core", category = "legs",
            movementPattern = "squat",
            muscles = listOf(MuscleRef("Quadriceps", "primary"), MuscleRef("Glutes", "secondary")),
            recommendedRepRange = "12-25", recommendedRestTime = "45-60s"
        ),
        SeedExercise(
            name = "Jump Squat",
            description = "Power development; use sparingly as a finisher when joints feel good.",
            muscleGroup = "Legs", equipment = "Bodyweight", difficulty = "Intermediate",
            secondaryMuscles = "Calves", category = "legs",
            movementPattern = "squat",
            muscles = listOf(MuscleRef("Quadriceps", "primary"), MuscleRef("Glutes", "secondary"), MuscleRef("Calves", "secondary")),
            recommendedRepRange = "5-12", recommendedRestTime = "90s",
            tips = "Land softly, heels down. Skip if knees are irritated."
        ),
        SeedExercise(
            name = "Towel Hamstring Curl",
            description = "Heels-on-towel sliding curl on smooth floor — direct knee-flexion hamstring work without a machine.",
            muscleGroup = "Legs", equipment = "Bodyweight", difficulty = "Intermediate",
            secondaryMuscles = "Glutes, Lower Back", category = "legs",
            movementPattern = "knee_flexion",
            muscles = listOf(MuscleRef("Hamstrings", "primary"), MuscleRef("Glutes", "secondary")),
            recommendedRepRange = "8-15", recommendedRestTime = "75s",
            instructions = "Lie on back, heels on a towel on a smooth floor. Bridge hips up, slide heels out to near-straight legs, pull heels back under you keeping hips high.",
            tips = "Keep hips lifted throughout to protect lower back; requires smooth flooring. Harder than it looks — control the eccentric fully."
        ),
        SeedExercise(
            name = "Hip Thrust (Shoulders on Bench)",
            description = "Upper back on flat bench, driving hips to full extension — strongest home glute exercise.",
            muscleGroup = "Legs", equipment = "Bodyweight + Flat Bench", difficulty = "Beginner",
            secondaryMuscles = "Hamstrings, Core", category = "legs",
            movementPattern = "hip_extension",
            muscles = listOf(MuscleRef("Glutes", "primary"), MuscleRef("Hamstrings", "secondary")),
            recommendedRepRange = "10-20", recommendedRestTime = "60-90s"
        ),
        SeedExercise(
            name = "Single-Leg Glute Bridge",
            description = "Floor-based unilateral hip extension; progression toward full hip thrusts.",
            muscleGroup = "Legs", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "Hamstrings, Core", category = "legs",
            movementPattern = "hip_extension",
            muscles = listOf(MuscleRef("Glutes", "primary"), MuscleRef("Hamstrings", "secondary")),
            recommendedRepRange = "10-15 per side", recommendedRestTime = "45-60s"
        ),
        SeedExercise(
            name = "Standing Dumbbell Calf Raise",
            description = "Loaded calf work off a step edge for full range; fixes zero-calf coverage in home plans.",
            muscleGroup = "Legs", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "", category = "legs",
            movementPattern = "plantarflexion",
            muscles = listOf(MuscleRef("Calves", "primary")),
            recommendedRepRange = "12-20", recommendedRestTime = "45-60s",
            instructions = "Balls of feet on a step/thick book edge, heels dipping below level, rise to top and pause 1s."
        ),
        SeedExercise(
            name = "Single-Leg Calf Raise",
            description = "Unilateral bodyweight calf raise doubling the load per calf versus two-leg version.",
            muscleGroup = "Legs", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "", category = "legs",
            movementPattern = "plantarflexion",
            muscles = listOf(MuscleRef("Calves", "primary")),
            recommendedRepRange = "12-20 per side", recommendedRestTime = "45s"
        )
    )
}
