# Sync Issues (Unresolved Only)

## SYNC-4
- Severity: HIGH
- Files: BodyMeasurementEntity.kt ↔ GymCoachDatabase.kt (MIGRATION_11_12)
- Problem: Worker changed BodyMeasurementEntity fields from non-nullable `Double = 0.0` to nullable `Double?`. While the original migration SQL (MIGRATION_2_3) created these columns as nullable (`REAL` without NOT NULL), the Room schema v11 JSON was generated from the old non-nullable entity, so it records them as `notNull: true`. MIGRATION_11_12 only adds columns but doesn't recreate the table. Room's schema validation at startup compares the new entity against the v11 schema and will detect a notNull mismatch.
- Fix: Either (a) Revert BodyMeasurementEntity fields to non-nullable `Double = 0.0` (simplest), OR (b) Add a table recreation migration that drops and recreates body_measurements with nullable columns, copying data.
- Status: pending

## RESOLVED ISSUES (archived)

### SYNC-1 ✅ RESOLVED
- VolumeCalculator.directSetsByMuscle/indirectSetsByMuscle now counts individual sets via for-loop (not groupBy exerciseId)
- Fixed in commit b673d7c + subsequent edit

### SYNC-2 ✅ RESOLVED
- HomeViewModel.plannedWeeklySets() now attributes per-exercise via exerciseMuscleMap
- ExerciseRepository injected and _exerciseMuscleMap populated
- Fixed in commit b673d7c

### SYNC-3 ✅ NOTED (out of scope)
- HomeViewModel.buildTrainingBalance uses "Back" for latVolume key, VolumeCalculator uses "Lats"
- Pre-existing naming inconsistency, not introduced by this fix
