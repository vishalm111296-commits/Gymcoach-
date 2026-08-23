# Phase 2 Implementation Evidence

**Date:** 2026-08-23 · **Branch:** `p0/stabilization` · **Gate PR:** #12
Directive-mandated record of library-API decisions made during P0, with sources.
Context7 MCP was consulted during earlier sessions for Room migration semantics;
this document consolidates the decisions and their verification status.

## Room migrations (v6 → v7)

- **Decision:** `ALTER TABLE workouts ADD COLUMN status TEXT NOT NULL DEFAULT 'NOT_STARTED'`
  plus three UPDATE backfills, registered via `addMigrations(...)`.
  No `fallbackToDestructiveMigration()` anywhere (security P0 stays closed).
- **Contract:** entity column carries
  `@ColumnInfo(defaultValue = "NOT_STARTED")`. Room validates post-migration
  schema identity against the entity hash; the DDL default string must equal
  the annotation default exactly or upgrade throws `IllegalStateException`.
  This equality is asserted by review here and MUST be covered by an
  instrumented `MigrationTestHelper` test (androidTest) — CI runs JVM-only
  (`testDebugUnitTest`), so execution requires a device/emulator run.
- **Backfill truth table:** completed=1 → COMPLETED · completed=0 ∧ has
  exercises → ACTIVE · completed=0 ∧ empty → ABANDONED.
  Rationale: legacy eager-create bug manufactured empty rows that auto-resumed
  forever ("phantom Resume Workout").

## Compose layout

- **Decision:** Progress dashboard uses one scrollable `Column` with plain
  `Row`s / `forEach` rows. The historical crash (nested lazy containers inside
  a scrollable parent → `IllegalStateException: Vertically scrollable component
  was measured with an infinity maximum height constraint`) is structurally
  impossible in the current tree — verified by reading `ProgressDashboardScreen`
  at HEAD; no lazy container sits inside the vertical-scroll Column.

## State machine

- **Decision:** explicit enum persisted as TEXT. PAUSED exists in the domain
  model but rest-timer pause remains in-memory session state (RestTimerManager);
  persisting PAUSED adds schema churn without user value today. Documented in
  `WorkoutStatus` KDoc with the transition table.

## Honesty contracts as executable tests

- Presses (horizontal/vertical push patterns) assert `vtaperLat == 0`.
- Equipment tokens restricted to the user inventory
  {Dumbbell, Bodyweight, Flat Bench}.
- Rep ranges machine-parseable (no prose like "12-20 slow").
- Calorie estimation REMOVED from Progress (volume×0.05 was fabricated data);
  replaced by real derived metric Volume/Set.

## Known limitations recorded

1. CI result unreadable via current PAT (check-runs + combined-status both 403).
   Build verdict needs human confirmation on PR #12 before merge.
2. Migration execution test pending instrumentation run (see above).
3. Discard button not yet wired into WorkoutSessionScreen UI (VM API +
   persistence-scope ready); residual risk closed instead at query level
   (resume requires content), making the missing button non-blocking for P0.
