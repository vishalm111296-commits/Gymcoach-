# GymCoach Data Model & Design Specification

## 1. Database Schema
Room persistence schema mapping workout execution, user profile, measurements, and exercise relationships.

### 1.1 Entities & Tables

#### user_profile
Tracks user physical status, preferences, and weekly targets.
- `id` (Long, PK): Sentinel key (value = 1).
- `name` (String): User's display name.
- `age` (Int): User's age.
- `gender` (String): Male/Female/Other.
- `height` (Double): Height in cm.
- `weight` (Double): Body weight in kg.
- `goalWeight` (Double): Target body weight in kg.
- `experience` (String): BEGINNER, INTERMEDIATE, ADVANCED.
- `trainingStyle` (String): BODYBUILDING, POWERLIFTING, etc.
- `preferredSplit` (String): PPL, UPPER_LOWER, FULL_BODY.
- `weeklyWorkoutGoal` (Int): Target workouts per week.
- `proteinGoal` (Double): Daily protein in grams.
- `caloriesGoal` (Int): Daily calories intake.
- `units` (String): METRIC or IMPERIAL.
- `leanBodyMass` (Double): Computed lean body mass.
- `maintenanceCalories` (Int): Daily maintenance intake.

#### measurement_records
Stores physical measurement entries over time.
- `id` (Long, PK, AutoIncrement).
- `userId` (String): User reference.
- `measurementType` (String): WEIGHT, BODY_FAT, CHEST, WAIST, ARMS, SHOULDERS, HIPS, THIGHS, CALVES, NECK.
- `value` (Double): Dimension value.
- `unit` (String): cm, kg, %.
- `date` (Long): Timestamp of measurement.
- `notes` (String, Optional).
- `createdAt` (Long): Timestamp of record creation.

#### exercises
Static data representing preloaded movements.
- `id` (Long, PK, AutoIncrement).
- `name` (String): Unique exercise name.
- `description` (String).
- `muscleGroup` (String): CHEST, BACK, LEGS, SHOULDERS, ARMS, CORE.
- `equipment` (String): BARBELL, DUMBBELL, MACHINE, CABLE, BODYWEIGHT.
- `difficulty` (String): BEGINNER, INTERMEDIATE, ADVANCED.
- `secondaryMuscles` (String): Comma-separated secondary muscles.
- `instructions` (String): Bulleted steps.
- `tips` (String).
- `commonMistakes` (String).
- `safetyNotes` (String).
- `recommendedRepRange` (String).
- `recommendedRestTime` (String).
- `estimatedCalories` (Int).
- `category` (String).
- `tags` (String).
- `isFavorite` (Boolean).
- `lastViewed` (Long).

#### workouts
Logged training sessions and templates.
- `id` (Long, PK, AutoIncrement).
- `date` (Long): Session date timestamp.
- `startTime` (Long): Epoch start time.
- `endTime` (Long): Epoch end time.
- `duration` (Long): Session duration in seconds.
- `notes` (String).
- `mood` (Int, Optional): User mood scale.
- `energy` (Int, Optional): User energy scale.
- `pain` (Int, Optional): Pain intensity.
- `completed` (Boolean): Completion status.
- `isTemplate` (Boolean): True if template.

#### workout_exercises
Maps exercises to specific workout sessions.
- `id` (Long, PK, AutoIncrement).
- `workoutId` (Long): FK to `workouts.id` (CASCADE).
- `exerciseId` (Long): FK to `exercises.id` (CASCADE).
- `orderIndex` (Int): Display sequence index.

#### workout_sets
Individual set logging with set types.
- `id` (Long, PK, AutoIncrement).
- `workoutExerciseId` (Long): FK to `workout_exercises.id` (CASCADE).
- `setNumber` (Int): Set sequence.
- `weight` (Double): Weight lifted.
- `reps` (Int): Repetitions completed.
- `rpe` (Double): Rating of perceived exertion (1-10).
- `restSeconds` (Int): Set-specific rest time.
- `completed` (Boolean): Completion checkbox state.
- `setType` (Int): 0=NORMAL, 1=WARMUP, 2=DROP, 3=FAILURE.

---

## 2. V-Taper Program-Generation Logic
The V-Taper ratio evaluates aesthetic proportion and alters training volume dynamically.

### 2.1 Metrics Calculation
- **Shoulder-to-Waist Ratio (SWR)**:
  $$\text{SWR} = \frac{\text{Shoulder Circumference (cm)}}{\text{Waist Circumference (cm)}}$$
- **Aesthetic Target**: Adonis Ratio ($\approx 1.618$).

### 2.2 Volume Attribution & Balance Tracking
Attributing working set volumes ($setType \neq 1$) to muscle groups:
- **Primary Muscle**: receives $1.0$ set credit.
- **Secondary Muscles**: receive $0.5$ set credit.

### 2.3 Program Adaptability Algorithm
1. Retrieve latest `SHOULDERS` and `WAIST` measurements.
2. If $\text{SWR} < 1.618$ (Needs shoulder width / lat width):
   - **Shoulders/Lats Adjustment**: Add $+1$ to $+2$ sets to primary shoulder (lateral raises, presses) and upper back (pull-ups, pulldowns) exercises.
   - **Waist/Core Adjustment**: Maintain or decrease direct obliques workload to avoid core thickening.
3. If $\text{SWR} \geq 1.618$ (Target met):
   - Maintain baseline volume across all upper body groups.

---

## 3. Workout-Execution State Flow
Ensures resilience across app crashes, backgrounding, and rotation.

```
       [ Idle ]
          │ (Start New / Resume)
          ▼
   ┌─► [ In-Progress ] ──(Set Complete)──► [ Resting ]
   │      │         ▲                         │
   │      │(Pause)  │(Resume)                 │(Timer End / Skip)
   │      ▼         │                         ▼
   │   [ Paused ] ──┘                 [ Rest Done ]
   │      │                                   │
   └──────┴──────────(Add Set/Log Set)◄───────┘
          │ (Complete Workout)
          ▼
     [ Completed ] ──► [ Summary Screen ]
```

### 3.1 State Persistence & Recovery
- **Current Session Id**: Persisted in `SharedPreferences`.
- **Timer Execution**: Timer uses target epoch time:
  $$\text{Target Epoch} = \text{System Epoch} + \text{Rest Seconds}$$
  Stored in `SharedPreferences`. On relaunch, system compares current epoch to target epoch to restore floating timer state accurately.

---

## 4. Plate Calculator Support
Calculates plate configuration for target weights.

### 4.1 Plate Inventory Model
```kotlin
data class Plate(val weight: Double, val count: Int)
```
Default metric plates (per side): `[25, 20, 15, 10, 5, 2.5, 1.25]`.

### 4.2 Calculation Algorithm
- Input: `targetWeight`, `barWeight`, `inventory`.
- Formula:
  $$\text{Weight Per Side} = \frac{\text{targetWeight} - \text{barWeight}}{2}$$
- Greedy search selects largest plate from inventory where $\text{weight} \leq \text{remainingWeight}$ and count is available, subtracting and repeating.
