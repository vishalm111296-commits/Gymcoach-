# Training Engine Specification

## 1. Schema Definitions

### Exercise Schema
```kotlin
enum class MuscleGroup {
    CHEST, BACK, QUADS, HAMSTRINGS, SHOULDERS, BICEPS, TRICEPS, ABS, CALVES, FOREARMS, GLUTES
}

enum class EquipmentType {
    BARBELL, DUMBBELL, KETTLEBELL, MACHINE, CABLE, BODYWEIGHT, BAND
}

data class Exercise(
    val id: Long,
    val name: String,
    val primaryMuscleGroup: MuscleGroup,
    val secondaryMuscleGroups: List<MuscleGroup> = emptyList(),
    val equipment: EquipmentType,
    val alternativeExerciseIds: List<Long> = emptyList()
)
```

### Workout Schema
```kotlin
data class Workout(
    val id: Long,
    val date: java.time.Instant,
    val completed: Boolean,
    val notes: String = ""
)
```

### WorkoutSet Schema
```kotlin
enum class SetType {
    WARMUP, WORKING, DROP
}

data class WorkoutSet(
    val id: Long,
    val workoutExerciseId: Long,
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val rir: Int?, // Reps in Reserve (0 to 10). Null if not tracked.
    val setType: SetType = SetType.WORKING,
    val completed: Boolean
)
```

---

## 2. Deterministic Algorithms

### 2.1 RIR-Based Progressive Overload
Computes recommendation for next session based on target Reps in Reserve (RIR).
- **Inputs**:
  - `actualWeight: Double`
  - `actualReps: Int`
  - `actualRIR: Int`
  - `targetRIR: Int`
- **Logic**:
  ```kotlin
  fun calculateNextSession(
      actualWeight: Double,
      actualReps: Int,
      actualRIR: Int,
      targetRIR: Int
  ): Pair<Double, Int> {
      val rirDiff = actualRIR - targetRIR
      return when {
          rirDiff > 1 -> Pair(actualWeight * 1.05, actualReps) // Too easy: +5% weight
          rirDiff < -1 -> Pair(actualWeight * 0.95, actualReps) // Too hard: -5% weight
          else -> Pair(actualWeight, actualReps + 1) // On target: +1 rep
      }
  }
  ```

### 2.2 Muscle-Group Volume Attribution
Calculates volume counts per muscle group.
- **Inputs**: List of working sets (`setType == SetType.WORKING` or `SetType.DROP`) and their exercises.
- **Logic**:
  - Direct sets: `1.0` set credit to `primaryMuscleGroup`.
  - Indirect sets: `0.5` set credit to each `secondaryMuscleGroups`.
  ```kotlin
  fun attributeVolume(sets: List<Pair<WorkoutSet, Exercise>>): Map<MuscleGroup, Double> {
      val volumeMap = mutableMapOf<MuscleGroup, Double>()
      sets.filter { it.first.completed && it.first.setType != SetType.WARMUP }.forEach { (set, exercise) ->
          volumeMap[exercise.primaryMuscleGroup] = volumeMap.getOrDefault(exercise.primaryMuscleGroup, 0.0) + 1.0
          exercise.secondaryMuscleGroups.forEach { secondary ->
              volumeMap[secondary] = volumeMap.getOrDefault(secondary, 0.0) + 0.5
          }
      }
      return volumeMap
  }
  ```

### 2.3 PR Detection
Determines if a completed set is a Personal Record (PR).
- **Inputs**: Completed `WorkoutSet` with `exerciseId`, list of historical bests `List<PersonalRecord>`.
- **Logic**:
  - A set is a PR if `weight > max_weight_for_reps(exerciseId, reps)` or `estimated1RM > max_historical_1RM(exerciseId)`.
  ```kotlin
  fun isPR(set: WorkoutSet, historicalSets: List<WorkoutSet>): Boolean {
      if (!set.completed) return false
      val sameRepSets = historicalSets.filter { it.reps == set.reps && it.completed }
      val weightPR = sameRepSets.isEmpty() || set.weight > sameRepSets.maxOf { it.weight }
      
      val set1RM = calculate1RM(set.weight, set.reps, set.rir)
      val historical1RMs = historicalSets.map { calculate1RM(it.weight, it.reps, it.rir) }
      val estimated1RMPR = historical1RMs.isEmpty() || set1RM > historical1RMs.max()
      
      return weightPR || estimated1RMPR
  }
  ```

### 2.4 Estimated 1RM (e1RM)
Calculates estimated 1-Repetition Maximum.
- **Formula**: Modified Epley formula incorporating RIR.
- **Logic**:
  - `effectiveReps = reps + (rir ?: 0)`
  - `e1RM = weight * (1.0 + (effectiveReps / 30.0))`
  ```kotlin
  fun calculate1RM(weight: Double, reps: Int, rir: Int?): Double {
      val effectiveReps = reps + (rir ?: 0)
      return if (effectiveReps <= 0) 0.0 else weight * (1.0 + (effectiveReps / 30.0))
  }
  ```

### 2.5 Equipment Filtration
Filters candidate exercises based on available equipment.
- **Inputs**: `List<Exercise>`, `Set<EquipmentType>` (user's selected/available equipment).
- **Logic**:
  ```kotlin
  fun filterExercises(exercises: List<Exercise>, available: Set<EquipmentType>): List<Exercise> {
      return exercises.filter { it.equipment in available }
  }
  ```

### 2.6 Workout Swap Engine
Swaps an exercise in a workout with a valid alternative.
- **Inputs**: `exerciseToSwap: Exercise`, `availableExercises: List<Exercise>`, `availableEquipment: Set<EquipmentType>`.
- **Logic**:
  - Find all exercises `E` where `E.primaryMuscleGroup == exerciseToSwap.primaryMuscleGroup` AND `E.equipment in availableEquipment`.
  - Rank matches prioritizing those listed in `exerciseToSwap.alternativeExerciseIds`.
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
