# GymCoach Foundation Stabilization — Design Spec

## Overview

Stabilize the GymCoach foundation before building vertical slices. This phase ensures the existing codebase is correct, tested, and CI-gated.

## Current State

- **Main**: `1649d639c14e519203fce9be7c0ad67d0a4f491e`
- **Audit branch**: `fix/forensic-audit-bugs` (5 commits ahead, 0 behind, no PR)
- **Database**: Room v8, 18+ entities, migration chain 1→8
- **Tests**: 5 test files exist, 4 of 5 are fake (hardcoded assertions)

## Phase 1: Merge Audit Branch

### What
The `fix/forensic-audit-bugs` branch contains 5 commits with correct fixes:
1. Duplicate ProgressViewModel removed
2. Workout terminal-state guard corrected
3. PR query restricted to COMPLETED workouts
4. Active-workout lookup corrected (status = 'ACTIVE')
5. Monthly calendar grouping corrected (strftime)
6. Status field mapped end-to-end (entity → domain → repository)
7. WorkoutWithStats.status added to domain model

### How
1. Create PR from `fix/forensic-audit-bugs` → `main`
2. Review all changes
3. Merge via GitHub

### Acceptance Criteria
- All 5 commits on main
- No merge conflicts
- CI passes on merged main

## Phase 2: Fix CI

### What
The CI workflow uses system `gradle` instead of `./gradlew` wrapper.

### Changes
- `.github/workflows/android-build.yml`: Replace `gradle assembleDebug` with `./gradlew assembleDebug`
- Ensure test failures produce non-zero exit (already correct)
- Ensure lint failures produce non-zero exit (already correct)

### Acceptance Criteria
- CI uses `./gradlew` for all Gradle commands
- Build failure → workflow failure
- Test failure → workflow failure
- Lint failure → workflow failure

## Phase 3: Replace Fake Tests

### 3.1 ProgramGeneratorTest
**Current**: Hardcoded `val hasBodyweight = true`, tests local constants against itself.
**Fix**: Instantiate real `ProgramGenerator`, call `generate()`, assert on actual returned programs.

Test cases:
- 3-day program generation
- 4-day program generation
- 5-day program generation
- 6-day program generation
- Equipment restrictions (dumbbell-only, bodyweight-only)
- No duplicate exercises within a day
- Muscle group distribution (V-taper emphasis on lats/back/delts)
- Unavailable equipment exclusion

### 3.2 WorkoutPersistenceTest
**Current**: Tests local models without database access.
**Fix**: Use in-memory Room database, test actual DAO operations.

Test cases:
- Insert workout with status NOT_STARTED
- Update workout to ACTIVE
- Update workout to COMPLETED
- Update workout to ABANDONED
- Query ACTIVE workouts (getLatestIncompleteWorkout)
- Query COMPLETED workouts only
- Verify status survives updateWorkout() calls

### 3.3 PRDetectorTest
**Current**: Hardcoded assertions against local constants.
**Fix**: Test actual PR detection logic with real data.

Test cases:
- New PR detected (higher weight than previous)
- No PR when weight is lower
- No PR when weight is equal
- PR filtered to COMPLETED workouts only
- PR excludes abandoned workouts

### 3.4 VolumeCalculatorTest
**Current**: Tests manual math on local constants.
**Fix**: Test actual volume calculation with real data.

Test cases:
- Volume = weight × reps × sets
- Zero weight handling
- Zero reps handling
- Monthly volume grouping by calendar month
- Timezone semantics (UTC grouping)

### 3.5 Room Migration Tests
**Current**: Syntax errors, won't compile.
**Fix**: Correct syntax, ensure migration chain 1→8 works.

## Phase 4: Add Regression Tests

For each of the 6 forensic audit fixes, add a targeted regression test:

1. **Terminal workout guard**: Cannot complete an already-completed workout
2. **PR filtering**: PRs only from COMPLETED workouts
3. **ACTIVE lookup**: Only ACTIVE workouts returned as resumable
4. **Monthly volume**: Correct calendar month grouping
5. **Status mapping**: Status survives entity ↔ domain roundtrip
6. **Duplicate ViewModel**: Single ProgressViewModel instance

## Phase 5: Verify Foundation

1. Run `./gradlew assembleDebug` — must pass
2. Run `./gradlew testDebugUnitTest` — must pass
3. Run `./gradlew lintDebug` — must pass (warnings acceptable, errors not)
4. Fresh code review
5. Lock foundation

## Vertical Slice Roadmap (Post-Foundation)

| Slice | Feature | Key Deliverable |
|-------|---------|-----------------|
| 1 | Profile → Program → Today's Workout | End-to-end onboarding flow |
| 2 | Complete Workout Experience | Fast set logging, rest timer |
| 3 | Progress + PR + Volume | Analytics dashboard |
| 4 | Exercise Discovery | Search, filter, detail |
| 5 | Home Dashboard | "What should I do today?" |
| 6 | Physique Progress | Measurements, photos |
| 7 | Recovery / Readiness | Optional, after core loop works |

## Quality Gates

After each vertical slice:
1. Feature works end-to-end
2. Data persists correctly
3. State survives navigation
4. UI behaves correctly
5. Error states handled
6. Empty states handled
7. Equipment filtering respected
8. No regressions
9. Spec compliance review
10. Code quality review
