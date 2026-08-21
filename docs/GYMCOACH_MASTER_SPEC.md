# GYMCOACH MASTER SPECIFICATION

## 1. Product Scope
GymCoach is a specialized **Hypertrophy Coach** and workout tracking application designed for intermediate to advanced lifters, prioritizing structured muscle growth over generic fitness logging.

### Hypertrophy Coach vs. Basic Logger
* **Basic Logger (Generic)**: Tracks simple weight/reps history, static workout templates, raw volume load, and general bodyweight.
* **Hypertrophy Coach (GymCoach)**:
  * **Structured Progression**: Embeds a deterministic RIR-based progressive overload algorithm targeting working sets with proximity-to-failure tracking.
  * **Scientific Volume Attribution**: Tracks hard working sets per muscle group, allocating $1.0$ set to primary muscles and $0.5$ set to secondary muscles.
  * **Aesthetic-Driven Adjustments**: Implements a V-Taper program adaptability algorithm adjusting training volumes dynamically based on the Adonis ratio.
  * **Zero-friction single-handed logging**: Optimized custom data-entry UX with auto-forwarding focus, persistent rest-timer overlays, and historical performance ghosts.
  * **Pure local & offline execution**: Zero networks, cloud accounts, analytics, or subscriptions.

---

## 2. Design Tokens
Premium dark athletic theme, specifically designed for low ambient light emission in gym environments with high-contrast accent highlights.

```kotlin
// Brand Primaries & High Performance Accents
val GraphiteDeep = Color(0xFF121212)   // Main screen background
val CarbonGray = Color(0xFF1E1E1E)     // Base card & sheet container surface
val NeonVolt = Color(0xFFCCFF00)       // Critical actions, timer highlights, active states
val SlateMuted = Color(0xFF2E2E3A)     // Text field inputs & divider borders

// Semantic & Status Tokens
val EnergyAmber = Color(0xFFFF9E00)    // Drop sets, caution notices, secondary priorities
val FailureCrimson = Color(0xFFFF3B30)  // Failure sets, delete actions, system errors
val RecoveryBlue = Color(0xFF007AFF)   // Rest timer active, warmup sets

// Typography Contrast Scales
val PureWhite = Color(0xFFFFFFFF)      // Primary header text
val SilverText = Color(0xFFE5E5EA)     // Subtitles, descriptive paragraphs
val CharcoalText = Color(0xFF8E8E93)   // Placeholder labels, unit markers, disabled states
```

---

## 3. Training Science
All programming and calculations in GymCoach are grounded in peer-reviewed exercise science.

* **ACSM (American College of Sports Medicine) 2026 Guidelines**:
  * **Frequency**: Novice (2-3 days total-body/week), Intermediate (3-4 days split/week), Advanced (4-6 days split/week).
  * **Intensity & Volume**: Hypertrophy focus utilizes $60\% - 80\%$ of 1-Repetition Maximum (1RM) for $8 - 12$ repetitions per set, targeting $2 - 4$ sets per exercise (aiming for $10 - 20$ hard sets per muscle group weekly for hypertrophy).
  * **Rest Periods**: $2 - 3$ minutes for compound multi-joint movements; $1 - 2$ minutes for isolation movements.
* **Pelland et al. 2026 Guidelines**:
  * **Proximity to Failure**: Focuses on working sets performed within $0 - 4$ Reps in Reserve (RIR) to optimize motor unit recruitment.
  * **Systemic Fatigue Mitigation**: Avoids chronic absolute failure ($0$ RIR / RPE 10) on compound lifts to manage central nervous system fatigue while prescribing higher proximity to failure on isolation work.

---

## 4. Progressive Overload Algorithm
Deterministic progressive overload rules calculated at the set-by-set or session-by-session level based on Reps in Reserve (RIR).

### RIR-Based Progression Logic
```kotlin
fun calculateNextSession(
    actualWeight: Double,
    actualReps: Int,
    actualRIR: Int,
    targetRIR: Int
): Pair<Double, Int> {
    val rirDiff = actualRIR - targetRIR
    return when {
        rirDiff > 1 -> Pair(actualWeight * 1.05, actualReps) // Too easy: increase weight 5%
        rirDiff < -1 -> Pair(actualWeight * 0.95, actualReps) // Too hard: decrease weight 5%
        else -> Pair(actualWeight, actualReps + 1) // On target: add 1 rep
    }
}
```

### Estimated 1-Repetition Maximum (e1RM)
Calculated using the modified Epley formula incorporating RIR:
$$\text{effectiveReps} = \text{reps} + \text{RIR}$$
$$\text{e1RM} = \text{weight} \times \left(1.0 + \frac{\text{effectiveReps}}{30.0}\right)$$

---

## 5. Exercise Swapping Engine
Calculates candidates and ranks alternative exercises for workout modification.

### Swap Logic
1. **Match Target Muscle**: Finds all candidate exercises where $\text{candidate.primaryMuscleGroup} == \text{target.primaryMuscleGroup}$.
2. **Filter Equipment Availability**: Filters candidate exercises matching the user's available equipment set ($\text{candidate.equipment} \in \text{availableEquipment}$).
3. **Rank Alternatives**: Prioritizes exercises explicitly listed in the target exercise's defined `alternativeExerciseIds` list.
```kotlin
fun findSwaps(
    exerciseToSwap: Exercise,
    availableExercises: List<Exercise>,
    availableEquipment: Set<EquipmentType>
): List<Exercise> {
    val candidates = availableExercises.filter { 
        it.id != exerciseToSwap.id &&
        it.primaryMuscleGroup == exerciseToSwap.primaryMuscleGroup &&
        it.equipment in availableEquipment
    }
    return candidates.sortedByDescending { it.id in exerciseToSwap.alternativeExerciseIds }
}
```

---

## 6. Entity Mappings (V2 Room Schemas)
Current version 5 database schemas defining Room mappings.

### 6.1 Database Entity Schema Definitions

#### `exercises`
* `id` (Long, PK, AutoIncrement)
* `name` (String, Unique)
* `description` (String)
* `muscleGroup` (String)
* `equipment` (String)
* `difficulty` (String)
* `secondaryMuscles` (String)
* `instructions` (String)
* `tips` (String)
* `commonMistakes` (String)
* `safetyNotes` (String)
* `recommendedRepRange` (String)
* `recommendedRestTime` (String)
* `estimatedCalories` (Int)
* `category` (String)
* `tags` (String)
* `isFavorite` (Boolean)
* `lastViewed` (Long)

#### `workouts`
* `id` (Long, PK, AutoIncrement)
* `date` (Long)
* `startTime` (Long)
* `endTime` (Long)
* `duration` (Long)
* `notes` (String)
* `mood` (Int, Nullable)
* `energy` (Int, Nullable)
* `pain` (Int, Nullable)
* `completed` (Boolean)
* `isTemplate` (Boolean)

#### `workout_exercises`
* `id` (Long, PK, AutoIncrement)
* `workoutId` (Long, FK to `workouts.id`, CASCADE)
* `exerciseId` (Long, FK to `exercises.id`, CASCADE)
* `orderIndex` (Int)

#### `workout_sets`
* `id` (Long, PK, AutoIncrement)
* `workoutExerciseId` (Long, FK to `workout_exercises.id`, CASCADE)
* `setNumber` (Int)
* `weight` (Double)
* `reps` (Int)
* `rpe` (Double)
* `restSeconds` (Int)
* `completed` (Boolean)
* `setType` (Int) -> `0 = NORMAL`, `1 = WARMUP`, `2 = DROP`, `3 = FAILURE`

#### `user_profile`
* `id` (Long, PK, Default = 1)
* `name` (String)
* `age` (Int)
* `gender` (String)
* `height` (Double)
* `weight` (Double)
* `goalWeight` (Double)
* `currentGoal` (String)
* `experience` (String)
* `trainingStyle` (String)
* `preferredSplit` (String)
* `activityLevel` (String)
* `weeklyWorkoutGoal` (Int)
* `proteinGoal` (Double)
* `caloriesGoal` (Int)
* `units` (String)
* `avatarUrl` (String)
* `leanBodyMass` (Double)
* `maintenanceCalories` (Int)

#### `measurement_records`
* `id` (Long, PK, AutoIncrement)
* `userId` (String)
* `measurementType` (String) -> `WEIGHT`, `BODY_FAT`, `CHEST`, `WAIST`, etc.
* `value` (Double)
* `unit` (String)
* `date` (Long)
* `notes` (String, Nullable)
* `createdAt` (Long)
