# Sync Issues (Unresolved Only)

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
