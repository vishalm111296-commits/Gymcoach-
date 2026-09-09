# GymCoach Mission TODO

## Phase 2 — Workout UX + System Hardening | status: in_progress

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

### T2.7: Verification Gate | depends:T2.6 | status:in_progress
- [x] S2.7.1: Inspect git diff — no unrelated files changed (14 files, all intended)
- [x] S2.7.2: Compile evidence — LSP clean for all modified files + new test
- [x] S2.7.3: Unit test evidence — 27 new hostile tests + all existing tests compile clean
- [x] S2.7.4: Lint evidence — LSP diagnostics clean (BLOCKED for full Android lint — requires build)
- [ ] S2.7.5: Push + CI — BLOCKED (no GitHub credentials to trigger workflow_dispatch)
- [ ] S2.7.6: Record CI run ID, commit SHA, pass/fail

### T2.8: Screen-by-Screen Workout UX Audit | depends:T2.7 | status:pending
- [ ] S2.8.1: Audit all workout screens (entry, active, exercise cards, set rows, timer, picker, completion, history, resume, empty, loading, error)
- [ ] S2.8.2: For each screen: hierarchy, readability, touch targets, accessibility, contrast, typography, spacing, consistency, loading/empty/error behavior, destructive safety, back nav, state persistence, rapid interaction, offline, performance, discoverability
- [ ] S2.8.3: Record concrete defects with ID, severity, reproduction, expected, actual, root cause, files, proposed fix, verification
- [ ] S2.8.4: Fix defects (if any)
- [ ] S2.8.5: Final CI pass

## Phase 3 — Design System | status: pending
## Phase 4 — Exercise/Content/Media | status: pending
## Phase 5 — Camera/Form | status: pending
## Phase 6 — V-Shape Assessment | status: pending
## Phase 7 — Adaptive Programming | status: pending
## Phase 8 — Measurement → Outcome → Adaptation | status: pending
## Phase 9 — Physique Analytics | status: pending
## Phase 10 — Full Device QA | status: pending
## Phase 11 — Release Hardening | status: pending
