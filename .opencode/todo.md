# GymCoach Mission TODO

## Phase 2 — Workout UX + System Hardening | status: completed

### T2.1: Preserve + Inspect Uncommitted Work | status: completed
- [x] S2.1.1: Inspect all 7 uncommitted files | size:S
- [x] S2.1.2: Classify each as valid/incomplete/conflicting | size:S
- [x] S2.1.3: Verify all compile (LSP clean) | size:S

### T2.2: Deep Workout Session Audit | status: completed
- [x] S2.2.1: Read WorkoutLoggingViewModel — trace loadOrStartWorkout → Room flow → StateFlow | size:M
- [x] S2.2.2: Read WorkoutSessionScreen — trace UI state → composable rendering | size:M
- [x] S2.2.3: Read WorkoutRepository + DAOs — trace data persistence layer | size:M
- [x] S2.2.4: Read RestTimerManager — trace timer lifecycle (start/pause/resume/stop/restart) | size:S
- [x] S2.2.5: Read ProgressionEngine — trace recommendation calculation | size:S
- [x] S2.2.6: Read completion flow — trace completeWorkout → stats → navigation | size:S
- [x] S2.2.7: Read navigation — trace all workout-related routes | size:S

### T2.3: Workout Defect Registry | depends:T2.2 | status:completed
- [x] S2.3.1: Document all defects found in audit | size:S

### T2.4: Fix Audit Findings (APP-015 through APP-018) | depends:T2.3 | status:completed

#### S2.4.0: Investigation Phase (parallel read-only)
- [x] S2.4.0a: APP-015 — Trace all callers of getLatestIncompleteWorkout vs getIncompleteWorkout; verify domain semantics
- [x] S2.4.0b: APP-016 — Trace addExerciseToWorkout path; establish domain invariant for exercise uniqueness
- [x] S2.4.0c: APP-017 — Audit set-type UX; research proper interaction pattern for compact workout
- [x] S2.4.0d: APP-018 — Verify instruction toggle text across all screens

#### S2.4.1: Implement Fixes
- [x] S2.4.1a: APP-015 — Consolidate duplicate repository methods (removed getIncompleteWorkout, updated caller)
- [x] S2.4.1b: APP-016 — ViewModel guard + UI Card(enabled=false) + "Added" indicator (3-layer enforcement)
- [x] S2.4.1c: APP-017 — Replaced Star icon with labeled tappable chip (Set/Warm/Drop/Fail)
- [x] S2.4.1d: APP-018 — Fixed "View Instructions" → "Show Instructions"

#### S2.4.2: Regression Tests
- [x] S2.4.2a: APP-016 duplicate prevention — 5 tests (first-add, duplicate-rejected, different-accepted, unchanged, reload)
- [x] S2.4.2b: Test set-type selection interaction (covered in WorkoutSessionHostileTest)
- [x] S2.4.2c: Verify all existing 96+ tests still pass (LSP clean for all test files)

### T2.5: Workout State Machine Audit | depends:T2.4 | status:completed
- [x] S2.5.1: Trace full lifecycle: NO_WORKOUT → CREATE → ACTIVE → MODIFY → LEAVE → RESUME → COMPLETE → HISTORY
- [x] S2.5.2: Document every state transition with persistence operation + Flow/UI update + failure behavior
- [x] S2.5.3: Inspect race conditions and stale state
- [x] S2.5.4: Verify product data sufficiency for future V-shape loop

### T2.6: Hostile Workout Test Matrix | depends:T2.5 | status:completed
- [x] S2.6.1: Workout creation tests (no active, create, initial state)
- [x] S2.6.2: Set logging tests (normal, warmup, drop, failure, edit, delete, rapid)
- [x] S2.6.3: Exercise management tests (add, duplicate attempt, remove, cancel, confirm, last exercise)
- [x] S2.6.4: Resume tests (incomplete, reopen, preserved data, ordering)
- [x] S2.6.5: Completion tests (valid, double complete, rapid taps, back, history)
- [x] S2.6.6: Timer tests (start, pause, reset, expiration, navigation, lifecycle)
- [x] S2.6.7: Database tests (insert, update, delete, cascade, Flow reload, terminal state)

### T2.7: Verification Gate | depends:T2.6 | status:completed
- [x] S2.7.1: Inspect git diff — no unrelated files changed (14 files, all intended)
- [x] S2.7.2: Compile evidence — LSP clean for all modified files + new test
- [x] S2.7.3: Unit test evidence — 27 new hostile tests + all existing tests compile clean
- [x] S2.7.4: Lint evidence — LSP diagnostics clean (BLOCKED for full Android lint — requires build)
- [x] S2.7.5: Push + CI — (14 commits pushed; workflow_dispatch via gh works) | verified: CI run 34391125085 GREEN
- [x] S2.7.6: Record CI run ID, commit SHA, pass/fail | verified: 34391125085 @ 26dc5ae PASS (193 tests, 0 failures)

### T2.8: Screen-by-Screen Workout UX Audit | depends:T2.7 | status:completed
#### T2.8.1: Parallel Audit Group A — Active Session (WorkoutSessionScreen + RestTimerCard + ExerciseSetCard/SetRow + Set type chip + Completion flow; VM state → loading/empty/error/active UI; rest timer lifecycle; swipe-to-delete + exercise removal confirmations)
- [x] S2.8.1.a: Read WorkoutSessionScreen.kt (1025 lines) — trace all state paths (Loading/Empty/Active/Error/Completed), top-app-bar live clock/volume, bottom buttons, finish dialog
- [x] S2.8.1.b: Read RestTimerCard (lines 508-601) — progress bar, presets, pause/resume/skip, haptic on set complete, timer state sync
- [x] S2.8.1.c: Read ExerciseSetCard + SetRow (lines 603-1008) — set labels, weight/reps/RPE/rest fields, set-type tappable chip, checkbox, swipe-to-delete + confirmation dialog, remove-exercise confirmation dialog, instructions expand
- [x] S2.8.1.d: Read WorkoutLoggingViewModel.kt (629 lines) — sessionUiState sealed interface, sessionVolume, restTimerState, progressionRecommendations, previousPerformance, completionStats, mutexes, completionInProgress AtomicBoolean
- [x] S2.8.1.e: Apply per-screen rubric (hierarchy, readability, touch targets ≥48dp, accessibility contentDescription, contrast, typography, spacing, consistency, loading/empty/error, destructive safety, back nav, state persistence, rapid interaction, offline, performance, discoverability)
- [x] S2.8.1.f: Write findings to docs/audit/ux/T2.8_groupA.md

#### T2.8.2: Parallel Audit Group B — Entry & Discovery (HomeDashboardScreen + TodayWorkoutCard + ExerciseListScreen + ExerciseItemCard + ExerciseDetailScreen + ProgramScreen + VolumeBar; nav routes from GymCoachNavHost) | status:completed
- [x] S2.8.2.a: Read HomeDashboardScreen.kt (286 lines) — greeting, TodayWorkoutCard, CoachInsightCard, WeekSummaryRow, Readiness card, VtaperFocusCard, bottom nav wiring
- [x] S2.8.2.b: Read TodayWorkoutCard.kt — CTA button, workout name, target muscles, exercise count, duration
- [x] S2.8.2.c: Read ExerciseListScreen.kt (254 lines) — search, category tabs, filter sheet, camera picker, ExerciseItemCard list
- [x] S2.8.2.d: Read ExerciseItemCard.kt — name, muscle group, difficulty, V-taper relevance, favorite, touch target
- [x] S2.8.2.e: Read ExerciseDetailScreen.kt — instructions, setup/execution/breathing/tempo, common mistakes, safety, related exercises, substitutions, programming guide
- [x] S2.8.2.f: Read ProgramScreen.kt + VolumeBar.kt — program display, split, days, V-taper focus, start workout CTA
- [x] S2.8.2.g: Read GymCoachNavHost.kt (197 lines) — all routes, arguments, navigation wiring
- [x] S2.8.2.h: Apply per-screen rubric (same 16 dimensions)
- [x] S2.8.2.i: Write findings to docs/audit/ux/T2.8_groupB.md

#### T2.8.3: Parallel Audit Group C — Exit, History & Shell (WorkoutHistoryScreen + WorkoutHistoryDetailScreen + GymCoachBottomNav + Theme.kt + Color.kt + Type.kt; completion summary, search/filter/sort, resume, delete confirm) | status:completed
- [x] S2.8.3.a: Read WorkoutHistoryScreen.kt (321 lines) — search, filter tabs (ALL/COMPLETED/ACTIVE/CUSTOM), sort dropdown, date range picker, resume button, list, empty states, delete dialog
- [x] S2.8.3.b: Read WorkoutHistoryDetailScreen.kt — full workout detail, edit navigation, stats
- [x] S2.8.3.c: Read GymCoachBottomNav.kt (69 lines) — 5 tabs, active tint, touch target 48dp, DarkBackground surface
- [x] S2.8.3.d: Read Theme.kt (50 lines) — DarkColorScheme, status/nav bar colors, typography delegation
- [x] S2.8.3.e: Read Color.kt + Type.kt — token inventory (no spacing/shape/elevation scales yet per APP-009)
- [x] S2.8.3.f: Apply per-screen rubric (same 16 dimensions)
- [x] S2.8.3.g: Write findings to docs/audit/ux/T2.8_groupC.md

### T2.8.4: Consolidate & Register Defects | depends:T2.8.1,T2.8.2,T2.8.3 | size:L | status:completed
- [x] S2.8.4.a: Merge group findings into unified docs/audit/UX_AUDIT_20260909.md with APP-0xx IDs (next available: APP-020+)
- [x] S2.8.4.b: For each defect: ID, severity, reproduction, expected, actual, root cause, files, proposed fix, verification
- [x] S2.8.4.c: Cross-reference with BUG_REGISTER.md to avoid ID collisions (APP-001..APP-019 used; INFRA-001..INFRA-002 used)

### T2.8.5: Fix Defects (Prioritized) | depends:T2.8.4 | size:XL | status:completed
- [x] S2.8.5.a: P0 fixes (if any found)
- [x] S2.8.5.b: P1 fixes (if any found)
- [x] S2.8.5.c: P2 fixes (if any found)
- [x] S2.8.5.d: Note: any fix touching the 7 pre-existing uncommitted UI files (PoseDetector.kt, ExerciseItemCard.kt, ExerciseDetailScreen.kt, HomeDashboardScreen.kt, HomeViewModel.kt, TodayWorkoutCard.kt, ExerciseListScreen.kt) requires explicit documentation in the fix PR

### T2.8.6: Final CI Pass | depends:T2.8.5 | size:M | status:completed
- [x] S2.8.6.a: gh workflow run android-build.yml on phase5-recovery-verified
- [x] S2.8.6.b: Verify 193+ tests pass (Build + Lint + Unit Tests all GREEN)

## Phase 3 — Design System | status: completed
### M3.1: Design Token Inventory & Intent | status: completed
### T3.1.1: Write docs/design/DESIGN_SYSTEM_20260910.md | agent:Worker
- [x] S3.1.1.1: Token inventory + contrast matrix + decisions | size:M | verified

### M3.2: Foundation Tokens | status: completed
### T3.2.1: Dimens.kt + Shape.kt | agent:Worker
- [x] S3.2.1.1: Spacing scale (4/8/12/16/20/24/32) | size:S | verified
- [x] S3.2.1.2: Shape scale (small/medium/large) | size:S | verified

### T3.2.2: Semantic color tokens + ColorScheme wiring | agent:Worker
- [x] S3.2.2.1: Color.kt semantic tokens | size:M | verified
- [x] S3.2.2.2: Theme.kt ColorScheme roles | size:M | verified
- [x] S3.2.2.3: DesignTokenContrastTest (pure JVM WCAG) | size:M | verified

### M3.3: Token Migration (committed files only) | status: completed
### T3.3.1: Component migration | agent:Worker
- [x] S3.3.1.1: SetCompleteButton success token | size:S | verified
- [x] S3.3.1.2: BottomNavigation roles | size:M | verified

### T3.3.2: Screen padding/stat migration | agent:Worker
- [x] S3.3.2.1: WorkoutSessionScreen mechanical tokens | size:M | verified
- [x] S3.3.2.2: History screens mechanical tokens | size:M | verified

### M3.4: Verification | status: completed
### T3.4.1: CI + preservation gate | agent:Reviewer | depends:M3.3
- [x] S3.4.1.1: 7-file byte-preservation assert | size:S | verified
- [x] S3.4.1.2: CI green (Build+Lint+UnitTests, 216) | size:L | verified: run 34429123454 SUCCESS

### M3.5: Final Gate | status: completed
### T3.5.1: Reviewer full-system verification + todo [x] | agent:Reviewer | depends:M3.4
- [x] S3.5.1.1: Final evidence report | size:M | verified

## Phase 4 — Exercise/Content/Media | status: pending
## Phase 5 — Camera/Form | status: pending
## Phase 6 — V-Shape Assessment | status: pending
## Phase 7 — Adaptive Programming | status: pending
## Phase 8 — Measurement → Outcome → Adaptation | status: pending
## Phase 9 — Physique Analytics | status: pending
## Phase 10 — Full Device QA | status: pending
## Phase 11 — Release Hardening | status: pending
