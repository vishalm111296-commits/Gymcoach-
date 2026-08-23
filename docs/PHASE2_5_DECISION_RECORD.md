## UNRESOLVED ISSUES

1. **MIGRATION_4_5 entity/DB alignment** — 6 tables have column-level mismatches. Existing users on v4→5 will hit `IllegalStateException: Migration didn't properly handle...`
2. **Duplicate ProgressViewModel** — Two different `ProgressUiState` + `ProgressViewModel` definitions in same package; branch cannot compile until one is removed/merged.
3. **ProgramGenerator vocabulary mismatch** — Seeder stores lowercase/comma-joined equipment tokens; generator checks Title Case singletons; ALL 68 exercises filtered out → zero-exercise programs.
4. **Room DB unencrypted** — Body measurements and workout history persist in plaintext SQLite. CRITICAL for health data privacy.
5. **No signing config** — Release builds are unsigned; cannot ship to Play Store.
6. **VolumeCalculator set counting** — PR #10 fixes the exercise-not-sets bug but has: missing `w.completed = 1` filter in DAO, ordinal collapse, weeklySets avg vs directSets cumulative in same row, secondary/stabilizer credits hard-coded 0.
7. **Test coverage gaps** — No migration tests, no navigation tests, no UI tests, VolumeCalculatorTest has broken assertion.
8. **Onboarding → program flow** — Generated programs have zero exercises (C2); "Start Workout" navigates to blank session without pre-seeding today's program day.