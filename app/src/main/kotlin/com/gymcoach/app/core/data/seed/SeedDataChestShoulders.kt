package com.gymcoach.app.core.data.seed

import com.gymcoach.app.core.data.seed.MuscleRef
import com.gymcoach.app.core.data.seed.SeedExercise

/**
 * Chest + Shoulders. Equipment: Dumbbell / Bodyweight / Flat Bench ONLY.
 * V-taper honesty rules (audit 2026-08-22 + Agent-C audit 2026-08-23):
 * presses/flyes get lat=0; upper-chest scores only where a genuine clavicular bias exists.
 */
object SeedDataChestShoulders {

    val CHEST = listOf(
        SeedExercise(
            name = "Dumbbell Bench Press",
            description = "Horizontal press lying on a flat bench, pressing two dumbbells from chest level to lockout.",
            muscleGroup = "Chest", equipment = "Dumbbell + Flat Bench", difficulty = "Beginner",
            secondaryMuscles = "Triceps, Front Deltoid", category = "push",
            vtaperLat = 0, vtaperLateralDelt = 2, vtaperUpperChest = 3, vtaperRearDelt = 0,
            movementPattern = "horizontal_push",
            muscles = listOf(MuscleRef("Chest", "primary"), MuscleRef("Front Deltoid", "secondary"), MuscleRef("Triceps", "secondary")),
            recommendedRepRange = "6-12", recommendedRestTime = "90-120s",
            instructions = "Lie flat on bench with dumbbells at chest. Press up until arms extend, lower under control to chest level.",
            tips = "Keep wrists stacked over elbows; do not flare elbows beyond 45-60 degrees.",
            setupInstructions = "Sit on bench, dumbbells resting on thighs; kick back to lie down.",
            executionInstructions = "Lower dumbbells until elbows reach ~90 degrees, press up without locking hard."
        ),
        SeedExercise(
            name = "Dumbbell Floor Press",
            description = "Floor-limited bench press; triceps-friendly range that spares the shoulders.",
            muscleGroup = "Chest", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "Triceps, Front Deltoid", category = "push",
            vtaperLat = 0, vtaperLateralDelt = 1, vtaperUpperChest = 2, vtaperRearDelt = 0,
            movementPattern = "horizontal_push",
            muscles = listOf(MuscleRef("Chest", "primary"), MuscleRef("Triceps", "secondary")),
            recommendedRepRange = "8-12", recommendedRestTime = "90s",
            instructions = "Lie on floor, knees bent. Press dumbbells up, lower until upper arms touch floor, pause, press again."
        ),
        SeedExercise(
            name = "Reverse-Grip Dumbbell Bench Press",
            description = "Flat-bench press with palms facing you — shifts emphasis toward clavicular (upper) fibers.",
            muscleGroup = "Chest", equipment = "Dumbbell + Flat Bench", difficulty = "Intermediate",
            secondaryMuscles = "Front Deltoid, Biceps", category = "push",
            vtaperLat = 0, vtaperLateralDelt = 2, vtaperUpperChest = 8, vtaperRearDelt = 0,
            movementPattern = "horizontal_push",
            muscles = listOf(MuscleRef("Upper Chest", "primary"), MuscleRef("Chest", "secondary"), MuscleRef("Front Deltoid", "secondary")),
            recommendedRepRange = "8-12", recommendedRestTime = "90s",
            instructions = "Grip dumbbells with palms facing your face, press as usual keeping wrists neutral relative to forearm.",
            tips = "Start light — grip is the limiting factor. Keep thumbs wrapped around handle."
        ),
        SeedExercise(
            name = "Dumbbell Fly",
            description = "Flat-bench fly isolating pectoral stretch and adduction.",
            muscleGroup = "Chest", equipment = "Dumbbell + Flat Bench", difficulty = "Beginner",
            secondaryMuscles = "Front Deltoid", category = "push",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 1, vtaperRearDelt = 0,
            movementPattern = "horizontal_push",
            muscles = listOf(MuscleRef("Chest", "primary"), MuscleRef("Front Deltoid", "stabilizer")),
            recommendedRepRange = "10-15", recommendedRestTime = "60-90s",
            instructions = "Arms slightly bent, open dumbbells out wide until a chest stretch, hug back together along an arc.",
            tips = "Imagine hugging a barrel; weight travels in an arc, not straight lines."
        ),
        SeedExercise(
            name = "Single-Arm Dumbbell Press",
            description = "Unilateral flat-bench press challenging anti-rotation core stability.",
            muscleGroup = "Chest", equipment = "Dumbbell + Flat Bench", difficulty = "Intermediate",
            secondaryMuscles = "Core, Triceps", category = "push",
            vtaperLat = 0, vtaperLateralDelt = 2, vtaperUpperChest = 2, vtaperRearDelt = 0,
            movementPattern = "horizontal_push",
            muscles = listOf(MuscleRef("Chest", "primary"), MuscleRef("Core", "stabilizer"), MuscleRef("Triceps", "secondary")),
            recommendedRepRange = "8-12 per side", recommendedRestTime = "75s",
            instructions = "Lie flat holding one dumbbell at chest. Press to lockout while bracing against rotation; lower under control.",
            tips = "Keep hips and shoulders square to the ceiling; resist the twist."
        ),
        SeedExercise(
            name = "Push-Up",
            description = "Fundamental bodyweight horizontal push; scalable via tempo and leverage.",
            muscleGroup = "Chest", equipment = "Bodyweight", difficulty = "Beginner",
            secondaryMuscles = "Triceps, Front Deltoid, Core", category = "push",
            vtaperLat = 0, vtaperLateralDelt = 1, vtaperUpperChest = 2, vtaperRearDelt = 0,
            movementPattern = "horizontal_push",
            muscles = listOf(MuscleRef("Chest", "primary"), MuscleRef("Triceps", "secondary"), MuscleRef("Core", "stabilizer")),
            recommendedRepRange = "8-20", recommendedRestTime = "60s"
        ),
        SeedExercise(
            name = "Deficit Push-Up",
            description = "Hands elevated on books/dumbbells for deeper stretch — stronger pec stimulus than standard push-ups.",
            muscleGroup = "Chest", equipment = "Bodyweight", difficulty = "Intermediate",
            secondaryMuscles = "Triceps, Front Deltoid", category = "push",
            vtaperLat = 0, vtaperLateralDelt = 1, vtaperUpperChest = 3, vtaperRearDelt = 0,
            movementPattern = "horizontal_push",
            muscles = listOf(MuscleRef("Chest", "primary"), MuscleRef("Front Deltoid", "secondary"), MuscleRef("Triceps", "secondary")),
            recommendedRepRange = "8-15", recommendedRestTime = "60-90s"
        ),
        SeedExercise(
            name = "Feet-Elevated Push-Up",
            description = "Feet on flat bench — increases load and biases upper chest and front delts.",
            muscleGroup = "Chest", equipment = "Bodyweight + Flat Bench", difficulty = "Intermediate",
            secondaryMuscles = "Front Deltoid, Triceps", category = "push",
            vtaperLat = 0, vtaperLateralDelt = 2, vtaperUpperChest = 7, vtaperRearDelt = 0,
            movementPattern = "horizontal_push",
            muscles = listOf(MuscleRef("Upper Chest", "primary"), MuscleRef("Chest", "secondary"), MuscleRef("Front Deltoid", "secondary"), MuscleRef("Core", "stabilizer")),
            recommendedRepRange = "6-15", recommendedRestTime = "90s"
        ),
        SeedExercise(
            name = "Diamond Push-Up",
            description = "Narrow hand position maximizing triceps involvement.",
            muscleGroup = "Chest", equipment = "Bodyweight", difficulty = "Intermediate",
            secondaryMuscles = "Chest, Front Deltoid", category = "push",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 1, vtaperRearDelt = 0,
            movementPattern = "horizontal_push",
            muscles = listOf(MuscleRef("Triceps", "primary"), MuscleRef("Chest", "secondary")),
            recommendedRepRange = "6-15", recommendedRestTime = "60s"
        ),
        SeedExercise(
            name = "Weighted Push-Up",
            description = "Plate or dumbbell on upper back for progressive overload once bodyweight sets exceed 20 reps.",
            muscleGroup = "Chest", equipment = "Dumbbell + Bodyweight", difficulty = "Advanced",
            secondaryMuscles = "Triceps, Core", category = "push",
            vtaperLat = 0, vtaperLateralDelt = 1, vtaperUpperChest = 2, vtaperRearDelt = 0,
            movementPattern = "horizontal_push",
            muscles = listOf(MuscleRef("Chest", "primary"), MuscleRef("Triceps", "secondary"), MuscleRef("Core", "stabilizer")),
            recommendedRepRange = "6-12", recommendedRestTime = "120s"
        )
    )

    val SHOULDERS = listOf(
        SeedExercise(
            name = "Dumbbell Lateral Raise",
            description = "The primary lateral-deltoid builder — highest-leverage V-taper shoulder width exercise.",
            muscleGroup = "Shoulders", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "Traps", category = "push",
            vtaperLat = 0, vtaperLateralDelt = 10, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "abduction",
            muscles = listOf(MuscleRef("Lateral Deltoid", "primary"), MuscleRef("Traps", "secondary")),
            recommendedRepRange = "12-20", recommendedRestTime = "60s",
            instructions = "Raise arms out to sides to shoulder height with slight elbow bend, lead with elbows, lower slowly.",
            tips = "Stop at shoulder height; pinky slightly higher than thumb on the way up."
        ),
        SeedExercise(
            name = "Seated Dumbbell Shoulder Press",
            description = "Vertical press for overall delt mass; seated on bench or floor to remove leg drive.",
            muscleGroup = "Shoulders", equipment = "Dumbbell + Flat Bench", difficulty = "Beginner",
            secondaryMuscles = "Triceps, Lateral Deltoid", category = "push",
            vtaperLat = 0, vtaperLateralDelt = 5, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "vertical_push",
            muscles = listOf(MuscleRef("Front Deltoid", "primary"), MuscleRef("Lateral Deltoid", "secondary"), MuscleRef("Triceps", "secondary")),
            recommendedRepRange = "6-12", recommendedRestTime = "90-120s"
        ),
        SeedExercise(
            name = "Arnold Press",
            description = "Rotational press sweeping front-to-lateral delt through the rep.",
            muscleGroup = "Shoulders", equipment = "Dumbbell + Flat Bench", difficulty = "Intermediate",
            secondaryMuscles = "Triceps", category = "push",
            vtaperLat = 0, vtaperLateralDelt = 6, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "vertical_push",
            muscles = listOf(MuscleRef("Front Deltoid", "primary"), MuscleRef("Lateral Deltoid", "secondary"), MuscleRef("Triceps", "secondary")),
            recommendedRepRange = "8-12", recommendedRestTime = "90s"
        ),
        SeedExercise(
            name = "Bent-Over Reverse Fly",
            description = "Hip-hinged rear-delt isolation — critical posture balance and V-taper detail work.",
            muscleGroup = "Shoulders", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "Upper Back", category = "pull",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 9,
            movementPattern = "reverse_fly",
            muscles = listOf(MuscleRef("Rear Deltoid", "primary"), MuscleRef("Upper Back", "secondary")),
            recommendedRepRange = "12-20", recommendedRestTime = "60s",
            instructions = "Hinge to ~45 degrees or parallel, raise dumbbells out wide with soft elbows, squeeze shoulder blades.",
            tips = "Lead with pinkies slightly up; use light weight and strict form."
        ),
        SeedExercise(
            name = "Head-Supported Rear Delt Raise",
            description = "Chest-on-bench rear delt raise removing lower-back strain of bent-over position.",
            muscleGroup = "Shoulders", equipment = "Dumbbell + Flat Bench", difficulty = "Beginner",
            secondaryMuscles = "Upper Back", category = "pull",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 9,
            movementPattern = "reverse_fly",
            muscles = listOf(MuscleRef("Rear Deltoid", "primary"), MuscleRef("Upper Back", "secondary")),
            recommendedRepRange = "12-20", recommendedRestTime = "60s"
        ),
        SeedExercise(
            name = "Prone Y-Raise",
            description = "Face-down on bench raising arms into a Y — trains lower traps and rotator cuff health.",
            muscleGroup = "Shoulders", equipment = "Bodyweight + Flat Bench", difficulty = "Beginner",
            secondaryMuscles = "Upper Back", category = "pull",
            vtaperLat = 0, vtaperLateralDelt = 1, vtaperUpperChest = 0, vtaperRearDelt = 6,
            movementPattern = "scapular",
            muscles = listOf(MuscleRef("Upper Back", "primary"), MuscleRef("Rear Deltoid", "secondary"), MuscleRef("Traps", "secondary")),
            recommendedRepRange = "10-15", recommendedRestTime = "45-60s"
        ),
        SeedExercise(
            name = "Pike Push-Up",
            description = "Bodyweight vertical push loading shoulders through an overhead arc.",
            muscleGroup = "Shoulders", equipment = "Bodyweight", difficulty = "Intermediate",
            secondaryMuscles = "Triceps, Upper Chest", category = "push",
            vtaperLat = 0, vtaperLateralDelt = 4, vtaperUpperChest = 2, vtaperRearDelt = 0,
            movementPattern = "vertical_push",
            muscles = listOf(MuscleRef("Front Deltoid", "primary"), MuscleRef("Triceps", "secondary")),
            recommendedRepRange = "6-12", recommendedRestTime = "90s"
        ),
        SeedExercise(
            name = "Elevated Pike Push-Up",
            description = "Feet on bench increases overhead loading angle toward handstand-strength range.",
            muscleGroup = "Shoulders", equipment = "Bodyweight + Flat Bench", difficulty = "Advanced",
            secondaryMuscles = "Triceps", category = "push",
            vtaperLat = 0, vtaperLateralDelt = 5, vtaperUpperChest = 2, vtaperRearDelt = 0,
            movementPattern = "vertical_push",
            muscles = listOf(MuscleRef("Front Deltoid", "primary"), MuscleRef("Triceps", "secondary")),
            recommendedRepRange = "5-10", recommendedRestTime = "120s"
        ),
        SeedExercise(
            name = "Dumbbell Shrug",
            description = "Vertical trap development; simple and effective with heavy dumbbells.",
            muscleGroup = "Shoulders", equipment = "Dumbbell", difficulty = "Beginner",
            secondaryMuscles = "", category = "pull",
            vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "scapular",
            muscles = listOf(MuscleRef("Traps", "primary")),
            recommendedRepRange = "12-20", recommendedRestTime = "60s"
        ),
        SeedExercise(
            name = "Dumbbell Upright Row",
            description = "Compound trap/lateral-delt pull; performed to sternum height only.",
            muscleGroup = "Shoulders", equipment = "Dumbbell", difficulty = "Intermediate",
            secondaryMuscles = "Traps, Biceps", category = "pull",
            vtaperLat = 0, vtaperLateralDelt = 6, vtaperUpperChest = 0, vtaperRearDelt = 0,
            movementPattern = "abduction",
            muscles = listOf(MuscleRef("Lateral Deltoid", "primary"), MuscleRef("Traps", "secondary"), MuscleRef("Biceps", "secondary")),
            recommendedRepRange = "10-15", recommendedRestTime = "60s",
            safetyNotes = "Stop at sternum height; skip if shoulder impingement history."
        )
    )
}
