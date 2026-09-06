# Sync Issues (Unresolved Only)

## SYNC-4
- Severity: HIGH
- Files: GymCoachDatabase.kt (@Database version = 11) + MIGRATION_11_12
- Problem: Database version is still 11 but MIGRATION_11_12 was added. The migration will NEVER run because the @Database(version = 11) was not bumped to 12. This means the schema fixes (adding target_muscles to program_days, adding hips_cm to body_measurements) will NOT be applied on existing installs.
- Fix: Change @Database(version = 11) to @Database(version = 12) and export a new 12.json schema.
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

### SYNC-5 ✅ RESOLVED
- BodyMeasurementEntity nullable vs non-nullable: Worker reverted to non-nullable Double = 0.0 fields, ProgressViewModel.saveMeasurement uses `?: 0.0` to convert null → 0.0, load() uses `takeIf { it > 0 }` to convert 0.0 → null for UI. Convention is consistent across entity/ViewModel/UI layers.
