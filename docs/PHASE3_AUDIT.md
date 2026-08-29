# PHASE 3 FORENSIC AUDIT: EXERCISE SYSTEM

## Baseline Checks
- `testDebugUnitTest`: PASS
- `compileDebugAndroidTestKotlin`: PASS
- `assembleDebug`: PASS
- `lintDebug`: PASS

## Data Flow
- `ExerciseEntity` maps local data JSON. Includes scores (`vtaperLat`, etc.), `movementPattern`, and media placeholders (`imageUrl`, `videoUrl`).
- `ExerciseDao` handles persistence and FTS table updates (`exercise_fts`). Search uses FTS4.
- `ExerciseRepositoryImpl` wraps DAO methods.
- `ExerciseViewModel` powers `ExerciseListScreen` using `getAllExercises` and `searchExercises`.
- `ExerciseDetailViewModel` powers `ExerciseDetailScreen`, pulling specific exercises and firing `SubstitutionEngine.findSubstitutes`.

## The "A" Image Problem
The `ExerciseDetailScreen` currently renders a generic `Box` containing the first letter of the exercise name (e.g. "A" for Arnold Press). While `ExerciseVideoPlayer` exists, it is not wired up. The database schema has `imageUrl` and `videoUrl`, but local assets are missing or not packaged. For an offline-first app, we must either supply real assets or build a premium visual fallback.

## 85% Similarity Logic
The `SubstitutionEngine` uses a hardcoded point system (`calculatePreservationScore`):
- Same muscle group: +40
- Same category: +20
- Same equipment: +15
- Same difficulty: +10
- Same pattern tag (compound vs isolation): +10
Total = 95% maximum realistic score (sometimes coerced to 100). The UI displays this as a raw percentage, which implies fake precision. The UI needs to be updated to show deterministic reasons (e.g., "Same primary muscle & equipment") instead of an arbitrary "85%".

## Empty States
`ExerciseListScreen` does not handle "No Results Found" gracefully. `ExerciseDetailScreen` defaults to generic Material styling.

## Action Plan (Repair Chain)
1. **Phase 3A**: Redesign `ExerciseListScreen` and `ExerciseItemCard`.
2. **Phase 3B**: Redesign `ExerciseDetailScreen` including a premium fallback for missing media and deterministic substitution reasons.
