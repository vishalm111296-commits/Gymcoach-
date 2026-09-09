# GymCoach Bug Register — Phase 2 Correction Pass
**Created:** 2026-09-09  
**Phase:** 2 Workout UX + System Hardening  
**Branch:** phase5-recovery-verified | **HEAD:** 368df01

---

## Bug Status Legend
- **OPEN** — Discovered, not yet addressed
- **IN_PROGRESS** — Currently being fixed
- **FIXED** — Code changed, awaiting verification
- **VERIFIED** — Fix confirmed by test/regression
- **BLOCKED_BY_INFRA** — Cannot verify due to infrastructure (ARM64 AAPT2, no device)
- **BLOCKED_BY_ENV** — Environment limitation

---

## Infrastructure Blockers (Not Application Bugs)

### INFRA-001: ARM64 AAPT2 Incompatibility
- **Type:** Infrastructure
- **Severity:** BLOCKER (for local execution)
- **Area:** Build System
- **Reproduction:** Run `./gradlew assembleDebug` on ARM64 Termux proot-distro
- **Expected:** Clean debug build
- **Actual:** "AAPT2 aapt2-8.2.2-10154469-linux Daemon startup failed" / "Illegal instruction"
- **Root Cause:** AGP 8.2.2 downloads x86_64 aapt2 binary; no ARM64 Linux aapt2 published by Google. Both SDK build-tools/34.0.0/aapt2 and AGP-cached aapt2 are x86_64 (verified via `readelf -h`).
- **Fix:** Use GitHub Actions (ubuntu-latest x86_64) as authoritative CI; document limitation in CURRENT_STATUS.md
- **Regression Test:** CI workflow executes `assembleDebug` + `testDebugUnitTest` + `lintDebug`
- **Verification:** CI passes on x86_64
- **Status:** BLOCKED_BY_INFRA — Root cause identified; CI is authoritative

### INFRA-002: No Physical Android Device
- **Type:** Infrastructure
- **Severity:** BLOCKER (for runtime validation)
- **Area:** Camera / MediaPipe / Device Testing
- **Reproduction:** Attempt to run CameraPreviewScreen on physical device
- **Expected:** Rep counting + form feedback works
- **Actual:** No device available in environment
- **Root Cause:** No Android device/emulator available
- **Fix:** Defer physical validation to device testing phase; mark all camera features RUNTIME_UNVERIFIED
- **Regression Test:** Unit tests for FormAnalyzer state machine; integration test mock
- **Verification:** Call graph audited; lifecycle issues fixed; status honestly documented
- **Status:** BLOCKED_BY_INFRA — Physical validation deferred

---

## Application P0 Bugs (Actual GymCoach Defects)

### APP-001: Missing Program Screen (Navigation Target)
- **Type:** Application Defect
- **Severity:** P0
- **Area:** Navigation / UI
- **Reproduction:** Tap Program icon in bottom navigation
- **Expected:** Program screen with persisted program data (name, split, days, V-taper focus, start workout CTA)
- **Actual:** Navigates to Exercise List screen (incorrect route)
- **Root Cause:** 
  - `BottomNavigation.kt:33` - `BottomNavItem("program", "Program", Icons.Filled.CalendarMonth)`
  - `GymCoachNavHost.kt` - NO `composable("program")` route defined
  - `HomeViewModel.kt:66` - `onViewProgram` navigates to `Routes.EXERCISE_LIST`
  - Full program data exists in DB (`ProgramEntity`, `ProgramDayEntity`, `ProgramExerciseEntity`) and is used by `HomeViewModel`
- **Fix:** 
  1. Create `ProgramViewModel` consuming `ProgramRepository`
  2. Create `ProgramScreen` composable with real persisted data
  3. Add `composable("program")` route to `GymCoachNavHost`
  4. Update `HomeViewModel.onViewProgram` to navigate to "program"
- **Regression Test:** Navigation integration test; UI test for program display with real data
- **Verification:** Program tab shows persisted program with all required fields; start workout navigates to session
- **Status:** VERIFIED — Fixed in Phase 1 (ProgramScreen.kt, ProgramViewModel.kt, VolumeBar.kt, GymCoachNavHost.kt route added, CI Run 34348295533)

### APP-002: Workout Session Blank Loading State
- **Type:** Application Defect
- **Severity:** P0
- **Area:** Workout Session UI / State Management
- **Reproduction:** Navigate to WorkoutSessionScreen (new workout)
- **Expected:** Loading spinner → workout list or empty prompt to add exercise
- **Actual:** Blank screen with only bottom buttons ("Add Exercise", "Complete Workout") — enormous unused vertical space
- **Root Cause:** 
  - `WorkoutLoggingViewModel.kt:53` - `private val _currentWorkout = MutableStateFlow<WorkoutWithDetails?>(null)`
  - `WorkoutSessionScreen.kt:83` - collects as StateFlow, null initially
  - `WorkoutSessionScreen.kt:239` - `currentWorkout?.let { ... }` renders nothing when null
  - No Loading state, no Empty state
- **Fix:** 
  1. Add sealed UI state: `sealed interface WorkoutSessionUiState { data class Loading(...) : ..., data class Success(val workout: WorkoutWithDetails) : ..., data class Empty : ..., data class Error(val msg: String) : ... }`
  2. Update ViewModel to emit proper states
  3. Update Screen to render Loading (spinner), Empty (prompt to add exercise), Error
- **Regression Test:** Unit test for ViewModel state emissions; UI test for loading/empty states
- **Verification:** No blank screen at any point; loading spinner → workout list or empty prompt
- **Status:** VERIFIED — Fixed in Phase 1 (WorkoutLoggingViewModel sealed interface + WorkoutSessionScreen loading/empty states, CI Run 34348295533)

### APP-003: Set/Exercise Deletion Without Confirmation
- **Type:** Application Defect
- **Severity:** P0
- **Area:** Workout Session UI / Data Integrity
- **Reproduction:** Swipe left on a set row; tap exercise remove icon (X)
- **Expected:** Confirmation dialog or undo snackbar
- **Actual:** Immediate deletion
- **Root Cause:** 
  - `WorkoutSessionScreen.kt:646-651` - `SwipeToDismissBox` calls `onRemoveSet(index)` on `confirmValueChange` immediately
  - `WorkoutSessionScreen.kt:536` - `IconButton(onClick = onRemoveExercise)` for exercise removal
  - Workout deletion in History HAS confirmation dialog ✅
- **Fix:** 
  1. Add confirmation dialog for set deletion (swipe → show dialog → confirm → delete)
  2. Add confirmation dialog for exercise removal (IconButton → show dialog → confirm → delete)
  3. Ensure DB transaction completes before navigation
- **Regression Test:** UI test for delete confirmation; integration test for cascade delete
- **Verification:** Swipe shows confirmation; cancel preserves set; confirm deletes and updates UI
- **Status:** VERIFIED — Fixed in Phase 1 (pendingDeleteSetIndex + AlertDialog for sets; showRemoveExerciseDialog + AlertDialog for exercise removal, CI Run 34348295533)

### APP-004: Unbounded Progression Escalation
- **Type:** Application Defect
- **Severity:** P0
- **Area:** Progression Engine / Volume Management
- **Reproduction:** Consistently hit top reps on bodyweight/equipment-limited exercise for multiple sessions
- **Expected:** Sensible caps on sets/reps
- **Actual:** Unbounded escalation: 3×8-12 → 4×8-14 → 5×8-16 → 6×8-18 → 7×8-20...
- **Root Cause:** `ProgressionEngine.kt:76-90` - adds 1 set AND extends rep range by 2 with no caps
- **Fix:** 
  1. Analyze behavior: what happens at cap? (maintain, deload, periodize?)
  2. Define cap semantics (MAX_SETS_PER_EXERCISE=5, MAX_REPS=20 as starting point)
  3. Cap `newSets = (targetSets + 1).coerceAtMost(MAX_SETS_PER_EXERCISE)`
  4. Cap `newReps = (targetRepsMax + 2).coerceAtMost(MAX_REPS)`
  5. Write regression test for 10-session escalation scenario
- **Regression Test:** Test equipment-limited progression for 10 sessions → verify caps enforced
- **Verification:** Progression recommendations never exceed defined caps; escalation test passes
- **Status:** VERIFIED — Fixed in Phase 1 (MAX_SETS=5, MAX_REPS=20 caps with coerceAtMost(), 3 regression tests, CI Run 34348295533)

---

## Application P1 Bugs

### APP-005: Weekly Trend Shows "0.0%%"
- **Type:** Application Defect
- **Severity:** P1
- **Area:** Progress Dashboard
- **Reproduction:** Open ProgressDashboardScreen with no workout data
- **Expected:** "—" or "Stable" for trend
- **Actual:** Shows "0.0%%" (line 280 in ProgressDashboardScreen.kt)
- **Root Cause:** `trendSymbol` fallback uses `"\u2022 0.0%%"` instead of meaningful placeholder
- **Fix:** Change line 280 fallback to `"—"` or `"Stable"`
- **Regression Test:** UI test with empty database; verify Weekly Trend card
- **Verification:** Zero database → Weekly Trend shows "—"
- **Status:** VERIFIED — Fixed in Phase 1 (em-dash "—" fallback, CI Run 34348295533)

### APP-006: Missing Instructional Media (All Exercises)
- **Type:** Application Defect
- **Severity:** P1
- **Area:** Exercise Library
- **Reproduction:** Open any ExerciseDetailScreen
- **Expected:** User can learn exercise form from real media (video/GIF/WebM)
- **Actual:** Only typography placeholder (first letter in colored box)
- **Root Cause:** All exercise JSON assets lack `image_url`, `video_url`, `animation_url`; seeder stores null
- **Fix:** 
  1. Create MEDIA_INVENTORY.md (DONE)
  2. Prioritize V-taper critical exercises (Lats, LatDelt, RearDelt, UpperChest) for media acquisition
  3. Keep text fallback robust; do not insert fake URLs
- **Regression Test:** Media presence check in seeder; UI shows media when available
- **Verification:** Top 25 V-taper critical exercises have real assets; placeholders remain for rest
- **Status:** OPEN — AUDITED, inventory created

### APP-007: No Media Inventory
- **Type:** Application Defect
- **Severity:** P1
- **Area:** Exercise Library / Process
- **Reproduction:** Check which exercises have/need media
- **Expected:** Inventory document tracking media status per exercise
- **Actual:** No inventory exists
- **Fix:** MEDIA_INVENTORY.md created with all 150+ exercises, V-taper priority classification, and acquisition phases
- **Regression Test:** Inventory generation script runs; output validated
- **Verification:** `docs/audit/MEDIA_INVENTORY.md` with all exercises flagged
- **Status:** FIXED — Inventory created

---

## Application P2 Bugs

### APP-008: No Tablet/Foldable Dual-Pane Layouts
- **Type:** Application Defect
- **Severity:** P2
- **Area:** Responsive UI
- **Reproduction:** Run on tablet/emulator with wide screen
- **Expected:** Master-detail layouts, navigation rail
- **Actual:** All screens use `fillMaxWidth()`; spreads excessively
- **Status:** OPEN — Not audited in detail

### APP-009: No Centralized Design Tokens
- **Type:** Application Defect
- **Severity:** P2
- **Area:** Design System
- **Reproduction:** grep for hardcoded dp/colors in Compose files
- **Expected:** Centralized tokens for spacing, colors, shapes, typography, elevation
- **Actual:** Custom colors in `Color.kt` but no spacing scale, shape scale, elevation system
- **Status:** OPEN — Not audited in detail

### APP-010: Missing Warm-Up Set Generation
- **Type:** Application Defect
- **Severity:** P2
- **Area:** Domain / Program Generation
- **Reproduction:** Start workout with heavy working sets
- **Expected:** Suggested warm-up sets (40%/60%/80% of working weight)
- **Actual:** No warm-up generation
- **Status:** OPEN — Not audited in detail

### APP-011: Camera Lifecycle Audit Needed
- **Type:** Application Defect
- **Severity:** P2
- **Area:** Camera / ML
- **Reproduction:** Open CameraPreviewScreen, rotate, background, return
- **Expected:** No camera leaks, no recomposition rebinding, clean executor shutdown
- **Actual:** Code exists but lifecycle audit pending
- **Audit Points:**
  - `CameraPreviewScreen.kt`: CameraProvider bound in `AndroidView` factory (recomposition risk)
  - `PoseDetector` created in LaunchedEffect, closed in DisposableEffect
  - `ExecutorService` created in `remember`, shutdown in DisposableEffect
  - FrameConverter reuses bitmap buffers correctly
- **Status:** OPEN — Code review pending

---

## Application P3 Enhancements

### APP-012: Light Theme Support
- **Type:** Enhancement
- **Severity:** P3
- **Area:** Theme
- **Status:** OPEN

### APP-013: Health Connect Integration
- **Type:** Enhancement
- **Severity:** P3
- **Area:** Readiness
- **Status:** OPEN

### APP-014: Plate Calculator
- **Type:** Enhancement
- **Severity:** P3
- **Area:** Utilities
- **Status:** OPEN

---

## Phase 2 Workout Session Audit Findings (2026-09-09)

**Audit Scope:** Full data flow trace of workout session (ViewModel → Repository → DAO → Room → Flow → UI)
**Files Audited:** WorkoutLoggingViewModel.kt, WorkoutSessionScreen.kt, WorkoutRepository.kt, WorkoutRepositoryImpl.kt, WorkoutDao.kt, RestTimerManager.kt, ProgressionEngine.kt, Workout.kt, WorkoutEntity.kt, WorkoutExerciseEntity.kt, WorkoutSetEntity.kt, GymCoachNavHost.kt

### Key Findings

**Architecture (STATICALLY VERIFIED):**
- CASCADE deletes properly configured: WorkoutExerciseEntity → WorkoutSetEntity (onDelete=CASCADE)
- Sealed UI state (Loading/Empty/Active/Error) properly implemented
- Room Flow collection with ViewModelScope lifecycle management
- Rest timer scoped to viewModelScope (Singleton correctly uses injected scope)
- Terminal-state guard prevents double-completion
- ProgressionEngine caps (MAX_SETS=5, MAX_REPS=20) enforced with coerceAtMost()

**P2 Issues Found:**

#### APP-015: Duplicate Repository Methods [FIXED]
- **Type:** Code Quality
- **Severity:** P2
- **Area:** Domain Repository
- **Root Cause:** `getLatestIncompleteWorkout()` and `getIncompleteWorkout()` both existed and mapped to the same DAO query (`SELECT * FROM workouts WHERE status = 'ACTIVE' ORDER BY date DESC LIMIT 1`)
- **Impact:** Code duplication, no functional bug
- **Fix:** Removed `getIncompleteWorkout()` from interface, implementation, and DAO. Updated WorkoutHistoryViewModel caller to use `getLatestIncompleteWorkout()`
- **Status:** FIXED — STATICALLY VERIFIED (LSP clean)

#### APP-016: Exercise Picker Allows Duplicate Insertion [FIXED]
- **Type:** Application Defect
- **Severity:** P2
- **Area:** Workout Session UI / Domain Logic
- **Reproduction:** Add "Bench Press" to workout, tap "Add Exercise" again, tap "Bench Press"
- **Expected:** Duplicate rejected; existing exercise list unchanged
- **Actual (before fix):** UI showed "Added" indicator but clicking still inserted duplicate row. `addExerciseToWorkout()` in ViewModel had no duplicate guard.
- **Root Cause:** `WorkoutLoggingViewModel.addExerciseToWorkout()` at line 313 — no check against existing exercises before calling `workoutRepository.addExerciseToWorkout()`. UI `Card(onClick)` always invoked the ViewModel method regardless of `isAlreadyAdded`.
- **Domain Analysis:** WorkoutExerciseEntity has no unique constraint on (workoutId, exerciseId). Domain EXPLICITLY allows duplicate exercises (e.g., different warm-up sets, multiple rows of same movement). Enforcement is at application layer.
- **Fix (3 layers):**
  1. **ViewModel guard** (`WorkoutLoggingViewModel.addExerciseToWorkout()`): early-return if `workout.exercises.any { it.exercise.id == exercise.id }` — prevents insertion even if UI bypassed
  2. **UI guard** (`WorkoutSessionScreen.kt`): `Card(enabled = !isAlreadyAdded)` + onClick checks `!isAlreadyAdded` — makes already-added exercises visually and interactively non-clickable
  3. **Visual indicator**: "Added" label with reduced opacity (retained from prior fix)
- **Regression Tests:** 5 new tests in `WorkoutSessionHostileTest.kt`:
  1. First add succeeds
  2. Second add of same exercise is rejected (repo never called)
  3. Different exercise can still be added after rejection
  4. Existing exercises remain unchanged after rejection
  5. Duplicate prevention works after workout reload
- **Status:** FIXED — STATICALLY VERIFIED (LSP clean for ViewModel + Screen + tests)

#### APP-017: Set Type Cycle Button Uses Star Icon [FIXED]
- **Type:** UX Enhancement
- **Severity:** P2
- **Area:** Workout Session UI
- **Root Cause:** Star icon (`Icons.Default.Star`) used to cycle through set types (Normal → Warmup → Drop → Failure). Non-obvious interaction, cryptic abbreviations (W/D/F).
- **Fix:** Replaced Star icon + abbreviations with labeled tappable chip showing "Set 1"/"Warm"/"Drop"/"Fail". Tappable border with color coding. Chip is explicitly labeled and visually indicates interactivity.
- **Status:** FIXED — STATICALLY VERIFIED (LSP clean)

#### APP-018: Instructions Toggle Label Inconsistent [FIXED]
- **Type:** UX Enhancement
- **Severity:** P2
- **Area:** Workout Session UI
- **Root Cause:** `WorkoutSessionScreen.kt:669` — "View Instructions" / "Hide Instructions" text toggles
- **Fix:** Changed "View Instructions" to "Show Instructions". "Hide Instructions" unchanged.
- **Status:** FIXED — STATICALLY VERIFIED (LSP clean)

### Confirmation: Phase 1 Fixes Verified in Deep Audit

The following Phase 1 fixes were verified as working correctly during the deep audit:

| Fix | Verification |
|-----|-------------|
| APP-001: Program Screen | ProgramScreen + ProgramViewModel + VolumeBar + NavHost route — all present and correct |
| APP-002: Loading/Empty States | SessionUiState sealed interface properly handles Loading/Empty/Active/Error |
| APP-003: Confirmation Dialogs | pendingDeleteSetIndex + AlertDialog for sets; showRemoveExerciseDialog + AlertDialog for exercises |
| APP-004: Progression Caps | MAX_SETS=5, MAX_REPS=20 with coerceAtMost() — properly bounded |
| APP-005: Weekly Trend | Em-dash "—" fallback — verified in ProgressDashboardScreen |

---

## Workout State Machine Audit (T2.5) — STATICALLY VERIFIED

**Date:** 2026-09-09
**Scope:** Complete workout lifecycle trace

### State Transitions

| # | Source State | Action | Destination State | Persistence | Flow/UI Update | Failure Behavior | Duplicate-Action |
|---|-------------|--------|-------------------|-------------|----------------|------------------|-----------------|
| 1 | NO_WORKOUT | loadOrStartWorkout(null) | CREATE | query getLatestIncompleteWorkout | If null → create | Exception → Error state | Idempotent |
| 2 | NO_WORKOUT | loadOrStartWorkout(id) with completed workout | CREATE | query getWorkoutWithDetails | if completed → performAgain | Exception → Error state | Creates new copy |
| 3 | NO_WORKOUT | loadOrStartWorkout(id) with active workout | ACTIVE | collect getWorkoutWithDetails Flow | _currentWorkout emits | Exception → Error state | Resumes existing |
| 4 | NO_WORKOUT | startNewWorkout() | ACTIVE | insertWorkout (status=ACTIVE) | Room Flow → _currentWorkout | Exception → Error state | Creates new workout |
| 5 | ACTIVE | addSet(exerciseIndex) | ACTIVE | insertWorkoutSet | Room Flow → sets list | silently ignored | Adds duplicate set |
| 6 | ACTIVE | removeSet(exerciseIndex, setIndex) | ACTIVE | deleteSet (cascade) | Room Flow → sets list | silently ignored | No-op if already removed |
| 7 | ACTIVE | addExerciseToWorkout(exercise) | ACTIVE | insertWorkoutExercise | Room Flow → exercises list | silently ignored | Adds duplicate exercise |
| 8 | ACTIVE | removeExercise(exerciseIndex) | ACTIVE | deleteWorkoutExercise (cascade) | Room Flow → exercises list | silently ignored | No-op if already removed |
| 9 | ACTIVE | updateSetField(...) | ACTIVE | updateWorkoutSet | Room Flow → sets list | silently ignored | Overwrites with same value |
| 10 | ACTIVE | toggleSetCompletion | ACTIVE | updateWorkoutSet | Room Flow + volume calc | silently ignored | Toggles back |
| 11 | ACTIVE | completeWorkout() | COMPLETED | updateWorkout (status=COMPLETED) | _completed = true → summary screen | terminal guard blocks | Guard: "already completed" |
| 12 | ACTIVE | navigate away | LEAVE | (no DB write) | ViewModel.onCleared cancels jobs | Timer stops, data persists | N/A |
| 13 | LEAVE | navigate back to session | ACTIVE | getLatestIncompleteWorkout → collect Flow | _currentWorkout emits | Creates new if none found | Resumes existing |

### Race Condition Analysis

| Scenario | Analysis | Risk |
|----------|----------|------|
| Rapid set field updates | Each update is a separate coroutine via viewModelScope.launch. Room handles serialization. UI reflects latest Flow emission. | LOW — sequential DB writes |
| Complete + navigate away | completeWorkout cancels timer job, then launches DB update. If user navigates mid-update, ViewModel.onCleared cancels coroutine. DB write may not complete. | MEDIUM — data loss risk on rapid exit |
| addExercise + removeExercise rapid | Both are separate coroutines. Room Flow re-emits after each. UI may briefly show stale state. | LOW — eventual consistency |
| Rest timer + set completion | toggleSetCompletion starts rest timer. If user rapidly toggles, timer is restarted each time (tickJob?.cancel()). | LOW — timer is idempotent |
| loadOrStartWorkout called twice | LaunchedEffect(workoutId) in WorkoutSessionScreen. If recomposition triggers re-entry, _sessionUiState is set to Loading first, preventing duplicate state. | LOW — guarded by Loading state |

### Product Data Gap

#### APP-019: No Workout-Program Linkage
- **Type:** Data Gap
- **Severity:** P3 (blocks future product loop, not current functionality)
- **Area:** Domain Model / Database Schema
- **Root Cause:** Workout entity has no `programId` or `programDayId` field. When user starts workout from ProgramScreen, the workout is created but not linked to the program.
- **Impact:** Cannot track which program exercises were completed; cannot adapt program based on workout performance; cannot show program progress in analytics.
- **Data currently persisted per workout:**
  - ✅ Exercise performed (via WorkoutExercise → Exercise)
  - ✅ Sets, reps, load, RPE, rest
  - ✅ Set type (Normal/Warmup/Drop/Failure)
  - ✅ Completion state
  - ✅ Date/time, duration
  - ✅ Notes
  - ❌ Program ID (missing)
  - ❌ Program Day ID (missing)
  - ✅ Muscle group attribution (via Exercise.muscleGroup)
  - ✅ Progression outcome (via ProgressionEngine)
  - ✅ Incomplete/abandoned state (status field)
- **Fix:** Add programId and programDayId to WorkoutEntity (requires schema migration)
- **Status:** OPEN — Documented for Phase 7 (Adaptive Programming)

---

## Fixed in Phase 1 (During Audit)

| Bug | Fix Applied |
|-----|-------------|
| Progress Dashboard volume shows "0.0" | Changed to "None" in `ProgressDashboardScreen.kt` (lines 256, 375, 391) |
| Progress Dashboard EmptyPlaceholder | Wrapped in Box with center alignment + alpha |
| BodyMeasurement 0.0 → null conversion | Already implemented in `ProgressViewModel.kt` (lines 208-211) |
| Media Inventory | Created `docs/audit/MEDIA_INVENTORY.md` |

---

## Test Authoring During Phase 1

| Test Suite | Tests | Authored | Executed Locally | Executed in CI |
|------------|-------|----------|------------------|----------------|
| VolumeCalculatorTest | 50 | Pre-existing | 0 (blocked) | 0 (unverified) |
| ProgramGeneratorTest | 10 | Pre-existing | 0 (blocked) | 0 (unverified) |
| ProgressionEngineTest | 6 | Pre-existing | 0 (blocked) | 0 (unverified) |
| VtaperAttributionTest | 12 | Pre-existing | 0 (blocked) | 0 (unverified) |
| RoomMigrationTest | 15+ | Pre-existing | 0 (blocked) | 0 (unverified) |
| **Total** | **~93** | **0 new** | **0** | **0** |

**Tests Added During Phase 1:** 0 (build blocked, but test authoring is still possible)  
**Tests Executed Locally:** 0 (build blocked)  
**Tests Executed in CI:** 0 (no run for current HEAD 3840019)  
**Tests Passed:** 0 (no execution)  
**Tests Failed:** N/A  

---

## Next Actions (Strict Priority Order)

1. **Push to trigger CI** — Get authoritative x86_64 build + test results for current HEAD (3840019)
2. **Fix APP-001** — Implement ProgramScreen with real persisted program data; add route to NavHost
3. **Fix APP-002** — Add sealed UI state (Loading/Success/Empty/Error) to WorkoutLoggingViewModel; render properly in WorkoutSessionScreen
4. **Fix APP-003** — Add confirmation dialogs for swipe-to-delete sets and exercise removal IconButton
5. **Fix APP-004** — Analyze ProgressionEngine cap behavior; define semantics; implement caps with regression test
6. **Fix APP-005** — Change Weekly Trend fallback from "0.0%%" to "—"
7. **Audit Camera Lifecycle** — Verify no camera provider rebinding on recomposition, executor cleanup
8. **Create MEDIA_INVENTORY.md** — ✅ DONE

**Do NOT start Phase 2 (Workout Session UX redesign) until APP-001, APP-002, APP-003 are fixed and CI passes.**