# Phase 3 Baseline Report

## Exact Current State
CURRENT BRANCH: `jules-8697599317840399683-f4a7bc68`
CURRENT HEAD: `7f74071`
WORKTREE STATUS: Clean (all Phase 2 changes committed)

## Phase 2 Commits
The exact Phase 2 implementation was submitted as PR #35 in the branch `phase-2-home-dashboard`, merged into `main` and checked out here.
The commit `7f74071` is the merge commit for Phase 2. The previous commit is the implementation state.
Phase 2 correctly altered `HomeDashboardScreen.kt`, `HomeViewModel.kt`, added readiness components, and updated tests. It successfully decoupled the "Coach Insight" from fake AI terminology and presented true volume data.

## Baseline Verification
Executed on HEAD (`7f74071`):
- `./gradlew testDebugUnitTest` — PASS
- `./gradlew compileDebugAndroidTestKotlin` — PASS
- `./gradlew assembleDebug` — PASS
- `./gradlew lintDebug` — PASS

## Exercise System Read-Only Audit
**Architecture**:
- Database Room entities: `ExerciseEntity`, `ExerciseSubstitutionEntity`, `ExerciseMuscleEntity`, `FavoriteExerciseEntity`.
- DAO -> Repository (`ExerciseRepositoryImpl`) -> UseCase/Direct ViewModel (`ExerciseDetailViewModel`, `ExerciseViewModel`).
- Compose UI: `ExerciseListScreen.kt`, `ExerciseDetailScreen.kt`.

**"A" Placeholder Investigation**:
- `ExerciseDetailScreen` uses `ExerciseVideoPlayer` which (likely) falls back to a placeholder "A" when no media URL or asset is present. No local media bundles currently exist for exercises. The application is offline-first.

**Substitution Percentages Investigation**:
- Managed by `SubstitutionEngine.kt`.
- Formula:
  - Muscle group match: +40
  - Category match: +20
  - Equipment match: +15
  - Difficulty match: +10
  - Compound/Isolation tag match: +10
- Total maxes at 100%. While deterministic, the `85%` score is a composite feature score, not a true biomechanical similarity score. It may be better to replace the percentage in the UI with a transparent label (e.g., "Same muscle & category").

## Data Limitations
- No offline images/videos are bundled in the repository.
- Secondary muscles might require formatting (e.g., `front_deltoid` -> "Front Deltoid").
- Substitution logic relies on `tags`, `category`, and `equipment` strings being accurate.

## Phase 3 Plan
**Phase 3A Library Plan**:
- Improve `ExerciseListScreen` to use Phase 1/2 design language.
- Implement clear filtering for muscles/equipment only where data exists.
- Implement explicit empty states for search and favorites.

**Phase 3B Detail Plan**:
- Remove the "A" visual placeholder; replace with a clean, premium "Media Unavailable" offline-first state.
- Redesign the detail hierarchy.
- Convert `85%` in substitutions to transparent reason labels.
- Safely format secondary muscles.
- Show previous performance using history safely.

**Repair-Chain Protocol**:
All changes will follow: Inspect -> Change -> Compile -> Test -> Verify -> Commit.
