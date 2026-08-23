# P1-A Decision Record: Exercise Search — FTS4 vs FTS5

**Date:** 2026-08-23 · **Status:** DECIDED (research-only; implementation gated behind P0)
**Sources:** developer.android.com Room reference via Context7 (`@Fts4`/`@Fts5`,
`FtsOptions`, `RoomDatabase.Builder`), read 2026-08-23.

## Question

Directive §21 forbids blindly implementing FTS4 just because an old spec said
so. Evaluate FTS4 vs FTS5 against current Room capabilities before building
exercise search.

## Findings

1. **Room supports `@Fts5`** as a first-class entity annotation with
   `contentEntity` external-content mode. In external mode **Room creates and
   manages the content-sync triggers automatically** - writes go to the
   content table only; no manual `INSERT INTO fts(fts) VALUES('rebuild')`
   bookkeeping. Triggers are dropped before migrations and recreated after,
   which also removes our current class of "index went stale" risk.
2. **Availability caveat (decisive):** the Room reference states FTS5
   availability depends on the SQLite driver; on Android,
   `androidx.sqlite.driver.bundled.BundledSQLiteDriver` supports FTS5. GymCoach
   uses the classic framework SQLite path (`Room.databaseBuilder`, minSdk 26),
   where the system SQLite build cannot be assumed to include the FTS5 module
   across the OEM/API matrix. Adopting `@Fts5` therefore implies migrating the
   database driver first - a larger change than P1-A scope warrants.
3. Our existing v7 schema already ships a working FTS4 external-content table
   over `exercises`, rebuilt by `ExerciseSeeder.rebuildSearchIndex()` after
   every seed transaction. The only writer of the `exercises` table is the
   seeder, so the manual-rebuild model is currently sound (skeptic review
   finding #3 was resolved on exactly these grounds).

## Decision

**Keep FTS4 for P1-A.** Build search UX on top of the existing v7
`exercise_fts` table. Re-evaluate `@Fts5` when/if the project adopts
`BundledSQLiteDriver` (or raises minSdk far enough that framework FTS5 can be
guaranteed); at that point migrate via a new version step replacing the
virtual table, never via destructive fallback.

## Design constraints carried into P1-A implementation

- Search spans: exercise name, aliases (`exercise_aliases`), muscle
  (`exercise_muscles`→`muscles.display_name`), equipment
  (`exercise_equipment`→`equipment.display_name`), movement pattern
  (`movement_pattern`). FTS4 covers name/description/category columns;
  alias/muscle/equipment matching joins relational tables alongside the FTS
  match rather than denormalizing them into the index.
- UI state machine: IDLE → LOADING → RESULTS | NO_RESULTS | ERROR.
  NO_RESULTS may only render after a completed query returns zero rows -
  never as a transient state during LOADING (no premature "No exercises").
