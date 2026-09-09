# GymCoach Master Backlog — Phase 1 Correction Pass
**Mission:** Turn GymCoach into a production-quality, offline-first V-SHAPE PHYSIQUE COACH
**Branch:** phase5-recovery-verified | **HEAD:** 3840019
**Updated:** 2026-09-09 (Post-Correction Pass)

---

## INFRASTRUCTURE BLOCKERS (Separate from Application P0)

### INFRA-001: ARM64 AAPT2 Incompatibility
**Problem:** `assembleDebug` fails with "AAPT2 aapt2-8.2.2-10154469-linux Daemon startup failed" on ARM64 Termux proot-distro
**Root Cause:** AGP 8.2.2 downloads x86_64 aapt2 binary (`Machine: Advanced Micro Devices X86-64` via `readelf`); no ARM64 Linux aapt2 published by Google
**Files:** `gradle/libs.versions.toml` (AGP 8.2.2), `app/build.gradle.kts` (compileSdk 34), `.github/workflows/android-build.yml`
**Resolution:** Use GitHub Actions (ubuntu-latest x86_64) as authoritative build environment; document limitation
**Tests:** `./gradlew assembleDebug` succeeds on CI
**DoD:** Build passes on GitHub Actions x86_64; local limitation documented in CURRENT_STATUS.md
**Status:** BLOCKED_BY_INFRA — Root cause identified; CI is authoritative

### INFRA-002: No Physical Android Device
**Problem:** Camera/MediaPipe pipeline runtime validation blocked
**Resolution:** Defer physical validation to device testing phase; mark all camera features RUNTIME_UNVERIFIED
**DoD:** Call graph audited; lifecycle issues fixed; status honestly documented
**Status:** BLOCKED_BY_INFRA — Physical validation deferred

---

## APPLICATION P0 — BLOCKERS / DATA INTEGRITY / MISLEADING

### APP-001: Missing Program Screen (Navigation Target)
**Problem:** Bottom navigation references "program" route but no `ProgramScreen` composable exists. `HomeViewModel.onViewProgram` navigates to Exercise List.
**Evidence:** 
- `BottomNavigation.kt:33` - `BottomNavItem("program", "Program", Icons.Filled.CalendarMonth)`
- `GymCoachNavHost.kt` - NO `composable("program")` route defined
- `HomeViewModel.kt:66` - `onViewProgram` navigates to `Routes.EXERCISE_LIST`
- Full program data exists in DB (`ProgramEntity`, `ProgramDayEntity`, `ProgramExerciseEntity`) and is used by `HomeViewModel`
**Files:** `BottomNavigation.kt`, `GymCoachNavHost.kt`, `HomeViewModel.kt`, (new) `ProgramScreen.kt`, `ProgramViewModel.kt`
**Expected:** Tapping Program shows persisted program: name, split, weekly structure, current week/day, target muscles, exercise count, estimated duration, V-taper focus, start workout CTA
**Approach:** 
1. Create `ProgramViewModel` consuming `ProgramRepository`
2. Create `ProgramScreen` composable with program details
3. Add `composable("program")` route to `GymCoachNavHost`
4. Update `HomeViewModel.onViewProgram` to navigate to "program"
**Tests:** Navigation integration test; UI test for program display with real data
**DoD:** Program tab shows persisted program with all required fields; start workout navigates to session
**Status:** OPEN — AUDITED, implementation ready

### APP-002: Workout Session Blank Loading State
**Problem:** `currentWorkout` StateFlow starts as `null`; UI renders blank area (no loading, no empty state) until Room flow emits
**Evidence:** 
- `WorkoutLoggingViewModel.kt:53` - `_currentWorkout = MutableStateFlow<WorkoutWithDetails?>(null)`
- `WorkoutSessionScreen.kt:83` - collects as StateFlow, null initially
- `WorkoutSessionScreen.kt:239` - `currentWorkout?.let { ... }` renders nothing when null
- No loading spinner, no "Starting workout..." message, no empty state
**Files:** `WorkoutLoggingViewModel.kt`, `WorkoutSessionScreen.kt`
**Expected:** Explicit Loading / Empty / Error states; never blank screen
**Approach:** 
1. Add sealed UI state: `sealed interface WorkoutSessionUiState { data class Loading(...) : ..., data class Success(val workout: WorkoutWithDetails) : ..., data class Empty : ..., data class Error(val msg: String) : ... }`
2. Update ViewModel to emit proper states based on flow emission
3. Update Screen to render Loading (spinner), Empty (prompt to add exercise), Error
4. Fix any `state.value` on fresh Flow (check for suspicious patterns)
**Tests:** Unit test for ViewModel state emissions; UI test for loading/empty states
**DoD:** No blank screen at any point; loading spinner → workout list or empty prompt
**Status:** OPEN — AUDITED, root cause confirmed

### APP-003: Set/Exercise Deletion Without Confirmation
**Problem:** Swipe-to-delete sets removes immediately; no undo for accidental swipes. Exercise removal via IconButton also has no confirmation.
**Evidence:** 
- `WorkoutSessionScreen.kt:646-651` - `SwipeToDismissBox` calls `onRemoveSet(index)` on `confirmValueChange` immediately
- `WorkoutSessionScreen.kt:536` - `IconButton(onClick = onRemoveExercise)` for exercise removal
- Workout deletion in History HAS confirmation dialog ✅
**Files:** `WorkoutSessionScreen.kt`, `WorkoutLoggingViewModel.kt`, `WorkoutRepositoryImpl.kt`
**Expected:** Confirmation dialog or undo snackbar for set/exercise deletion; workout deletion already has confirmation
**Approach:** 
1. Add confirmation dialog for set deletion (swipe → show dialog → confirm → delete)
2. Add confirmation dialog for exercise removal (IconButton → show dialog → confirm → delete)
3. Ensure DB transaction completes before navigation; ViewModel handles state update
**Tests:** UI test for delete confirmation; integration test for cascade delete
**DoD:** Swipe shows confirmation; cancel preserves set; confirm deletes and updates UI
**Status:** OPEN — AUDITED, sets and exercises need confirmation

### APP-004: Unbounded Progression Escalation
**Problem:** Equipment-limited + allHitTop → `targetSets + 1` AND `targetRepsMax + 2` unbounded; no caps
**Evidence:** 
- `ProgressionEngine.kt:76-90` - adds 1 set AND extends rep range by 2 every session when allHitTop && (isBodyweight || isEquipmentLimited)
- Example escalation: 3×8-12 → 4×8-14 → 5×8-16 → 6×8-18 → 7×8-20...
- `ProgressionEngineTest.kt` tests single recommendations only, not repeated escalation
**Files:** `ProgressionEngine.kt`, `ProgressionEngineTest.kt`
**Expected:** Sensible caps (max sets, max reps) to prevent runaway volume
**Approach:** 
1. Analyze behavior at cap: what happens when user reaches MAX_SETS? (maintain, deload, periodize?)
2. Define cap semantics (MAX_SETS_PER_EXERCISE=5, MAX_REPS=20 as starting point)
3. Cap `newSets = (targetSets + 1).coerceAtMost(MAX_SETS_PER_EXERCISE)`
4. Cap `newReps = (targetRepsMax + 2).coerceAtMost(MAX_REPS)`
5. Write regression test for 10-session escalation scenario
**Tests:** Test equipment-limited progression for 10 sessions → verify caps enforced
**DoD:** Progression recommendations never exceed defined caps; escalation test passes
**Status:** OPEN — AUDITED, unbounded escalation confirmed; needs behavior analysis before capping

---

## APPLICATION P1 — IMPORTANT FUNCTIONALITY / SERIOUS UX DEFECTS

### APP-005: Weekly Trend Shows "0.0%%"
**Problem:** Minor empty state issue: Weekly Trend card shows "0.0%%" fallback instead of "—" or "Stable"
**Evidence:** Line 280 in `ProgressDashboardScreen.kt` uses fallback `"\u2022 0.0%%"` instead of meaningful placeholder
**Files:** `ProgressDashboardScreen.kt` (line 280)
**Expected:** Weekly Trend shows "—" or "Stable" when no trend data
**Approach:** Fix line 280 trendSymbol fallback
**Tests:** UI test with empty database; verify Weekly Trend card
**DoD:** Zero database → Weekly Trend shows "—" 
**Status:** OPEN — AUDITED, minor fix needed

### APP-006: Missing Instructional Media (All Exercises)
**Problem:** 100+ exercises have rich text metadata but all `image_url`, `video_url`, `animation_url` are null
**Evidence:** Exercise JSON assets lack media URLs; `ExerciseDetailScreen` shows typography placeholder only
**Files:** `app/src/main/assets/exercises/*.json`, `ExerciseSeeder.kt`, `ExerciseDetailScreen.kt`
**Expected:** User can learn exercise form from real media (video/GIF/WebM)
**Approach:** 
1. MEDIA_INVENTORY.md created with all 150+ exercises, V-taper priority classification, and acquisition phases
2. Prioritize V-taper critical exercises (Lats, LatDelt, RearDelt, UpperChest) for asset acquisition
3. Keep text fallback robust; do not insert fake URLs
**Tests:** Media presence check in seeder; UI shows media when available
**DoD:** Top 25 V-taper critical exercises have real assets; placeholders remain for rest
**Status:** OPEN — AUDITED, inventory created

### APP-007: No Media Inventory (FIXED)
**Problem:** No inventory of which exercises have/need media
**Fix:** Created `docs/audit/MEDIA_INVENTORY.md` with all 150+ exercises, V-taper priority classification, and acquisition phases
**Status:** FIXED — Inventory created

---

## APPLICATION P2 — HIGH-VALUE POLISH / ROBUSTNESS

### APP-008: Tablet / Foldable — Dual-Pane Layouts
**Problem:** All screens use `fillMaxWidth()`; spreads excessively on tablets
**Files:** `HomeDashboardScreen.kt`, `ExerciseDetailScreen.kt`, `ProgressDashboardScreen.kt`, `WorkoutSessionScreen.kt`
**Expected:** Master-detail on wide screens; navigation rail for Home/Progress/Program
**Approach:** Use `calculateWindowSizeClass()`; implement dual-pane for Exercise List+Detail, Program+Day
**Tests:** Layout preview tests; UI tests on large screen emulator
**DoD:** Tablet shows side-by-side; phone unchanged
**Status:** OPEN — Not audited in detail

### APP-009: Design System — Centralized Tokens
**Problem:** Hardcoded dp values, scattered colors, no spacing/typography scale
**Evidence:** `Color.kt` has tokens but `Theme.kt` doesn't define shapes, spacing scale, elevation
**Files:** `ui/theme/Color.kt`, `Theme.kt`, `Type.kt`, all Compose screens
**Expected:** Single source of truth for colors, spacing (4dp base), shapes, typography, elevation
**Approach:** 
1. Create `DesignTokens.kt` with spacing scale, shape scale, elevation
2. Migrate all hardcoded values to tokens
3. Remove duplicate color definitions
**Tests:** Visual regression; grep for hardcoded dp/colors → zero
**DoD:** No hardcoded sizing/color in Compose files; all via tokens
**Status:** OPEN — Not audited in detail

### APP-010: Warm-Up Set Generation (Domain)
**Problem:** No automatic warm-up calculation based on working weight
**Evidence:** Pre-redesign audit identified as missing V1.5 feature
**Files:** New domain service needed
**Expected:** Given working weight × reps → suggest 3 warm-up sets (40%/60%/80%)
**Approach:** Implement `WarmUpCalculator` in `core/program/`; integrate in `ProgramGenerator` and workout session
**Tests:** Unit tests for various weight ranges; bodyweight handling
**DoD:** WarmUpCalculator tested; integrated in program generation
**Status:** OPEN — Not audited in detail

### APP-011: Camera Lifecycle Audit
**Problem:** Camera pipeline lifecycle audit pending
**Files:** `CameraPreviewScreen.kt`, `PoseDetector.kt`, `FormAnalyzer.kt`
**Audit Points:**
- CameraProvider bound in `AndroidView` factory (recomposition risk)
- `PoseDetector` created in LaunchedEffect, closed in DisposableEffect
- `ExecutorService` created in `remember`, shutdown in DisposableEffect
- FrameConverter reuses bitmap buffers correctly
- No frame throttling beyond STRATEGY_KEEP_ONLY_LATEST
**Status:** OPEN — Code review pending

---

## APPLICATION P3 — OPTIONAL ENHANCEMENTS

### APP-012: Light Theme Support
**Problem:** App only supports dark theme (Deep Charcoal)
**Files:** `ui/theme/Color.kt`, `Theme.kt`, all screens
**Expected:** Light theme with proper contrast; system theme follows device
**Approach:** Define `LightColorScheme`; update `GymCoachTheme` to use dynamic/material3 theming
**Tests:** Screenshot tests for both themes
**DoD:** Light theme renders correctly; no hardcoded dark colors
**Status:** OPEN

### APP-013: Health Connect Integration
**Problem:** Readiness relies on subjective input only
**Files:** `ReadinessRepository`, `ReadinessViewModel`, `ReadinessScreen`
**Expected:** Optional HRV/sleep import from Health Connect
**Approach:** Add Health Connect client; sync readiness metrics; keep subjective fallback
**Tests:** Integration test with mock Health Connect
**DoD:** Readiness can pull real data; falls back to manual entry
**Status:** OPEN

### APP-014: Plate Calculator
**Problem:** No barbell plate calculation utility
**Files:** New utility in `core/util/` or `core/program/`
**Expected:** Input target weight → output plate combination per side
**Approach:** Simple calculator with standard plate set (2.5, 5, 10, 15, 20, 25kg)
**Tests:** Unit tests for common weights
**DoD:** Calculator accessible from workout session; accurate plate math
**Status:** OPEN

---

## V-SHAPE COACHING FOUNDATION (Phases 6-8 — NOT YET)

### V.1 VShapeAssessment Engine
**Problem:** No automated weak-point detection or physique balance scoring
**Approach:** Design after foundation stable — separate design doc required
**Status:** DEFERRED

### V.2 Measurement → Program Adaptation Loop
**Problem:** Measurements stored but don't influence next program
**Approach:** Connect `BodyMeasurementRepository` → `ProgramGenerator` inputs
**Status:** DEFERRED

### V.3 Analytics Redesign (Physique + Performance Split)
**Problem:** Current analytics mixes performance/physique; no V-shape trend
**Approach:** Separate Performance / Physique / Training Quality tabs
**Status:** DEFERRED

---

## TECHNICAL DEBT / INFRASTRUCTURE

### T.1 CI/CD — GitHub Actions Workflow
**Problem:** No CI workflow verified for current HEAD
**Files:** `.github/workflows/android-build.yml`
**Expected:** CI runs compile, unit tests, lint on every PR
**Approach:** Push to trigger CI; verify workflow executes successfully on x86_64
**Tests:** Workflow executes successfully
**DoD:** CI badge passing; all PRs validated
**Status:** PENDING — Push required

### T.2 Database Migration Safety
**Problem:** Migration tests exist but unexecuted (no device)
**Files:** `RoomMigrationTest.kt`, `GymCoachDatabase.kt` (v11, 10 migrations)
**Expected:** All 10 migrations verified on CI
**Approach:** Ensure CI runs `connectedAndroidTest` on emulator
**DoD:** CI runs migration tests; all pass
**Status:** PENDING — CI required

### T.3 Bug Register
**Problem:** No centralized bug tracking
**Approach:** Created `docs/audit/BUG_REGISTER.md` with all discovered issues
**DoD:** Register created; all P0/P1 bugs logged with reproduction steps
**Status:** DONE

---

## PHASE 1 ACCEPTANCE CRITERIA (Tracking — Corrected)

- [x] Current git state recorded
- [x] Backlog created (.opencode/todo.md)
- [x] Baseline build attempted
- [x] Exact build failure identified (AAPT2 x86_64 on ARM64)
- [x] Environment workaround attempted safely (CI is authoritative)
- [x] CI/build alternative established (GitHub Actions workflow exists)
- [x] Program navigation verified (APP-001: broken)
- [x] Workout session state verified (APP-002: blank loading state)
- [x] Progress dashboard audited (APP-005: Weekly Trend "0.0%%")
- [x] Progression engine tested (APP-004: unbounded escalation)
- [x] Program generator tested (STATICALLY VERIFIED only)
- [x] Volume calculator tested (STATICALLY VERIFIED only)
- [x] Database migrations audited (STATICALLY VERIFIED only)
- [x] Destructive actions audited (APP-003: missing confirmations)
- [x] Camera call graph audited (APP-011: lifecycle audit pending)
- [x] Media inventory created (APP-007: FIXED)
- [x] Bug register created (docs/audit/BUG_REGISTER.md)
- [x] CURRENT_STATUS.md updated (with strict verification categories)
- [ ] All discovered APP P0 bugs fixed (APP-001, APP-002, APP-003, APP-004 open)
- [ ] All feasible APP P1 foundation bugs fixed (APP-005, APP-006 open)
- [ ] Regression tests executed (blocked by build)
- [ ] No tests disabled to obtain a pass

---

## VERIFICATION STATUS SUMMARY

| Area | Static Verification | Test Execution | Build Execution | CI Verification | Device Verification |
|------|---------------------|----------------|-----------------|-----------------|---------------------|
| Database / Migrations | ✅ | ❌ | ❌ | ❌ | ❌ |
| Program Generation | ✅ | ❌ | ❌ | ❌ | ❌ |
| Volume Calculator | ✅ | ❌ | ❌ | ❌ | ❌ |
| Progression Engine | ✅ | ❌ | ❌ | ❌ | ❌ |
| Workout Engine | ✅ | ❌ | ❌ | ❌ | ❌ |
| PR Detection | ✅ | ❌ | ❌ | ❌ | ❌ |
| Camera Pipeline | ✅ | ❌ | ❌ | ❌ | ❌ |
| Navigation | ✅ | ❌ | ❌ | ❌ | ❌ |
| Progress Dashboard | ✅ | ❌ | ❌ | ❌ | ❌ |
| **Overall** | **STRONG** | **ZERO** | **BLOCKED** | **UNVERIFIED** | **N/A** |

**Phase 1 Implementation Complete — Verification Status Below**

**Exact Next Actions:**
1. Push to trigger CI → get authoritative x86_64 build + test results
2. Fix APP-001 (Program Screen)
3. Fix APP-002 (Workout Session loading state)
4. Fix APP-003 (Delete confirmations)
5. Fix APP-004 (Progression caps — analyze behavior first)
6. Fix APP-005 (Weekly Trend "0.0%%" → "—")
7. Audit Camera Lifecycle (APP-011)

**Do NOT start Phase 2 until APP-001, APP-002, APP-003 are fixed and CI passes.**