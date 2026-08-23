## RECOMMENDED FIX ORDER (implementation agents only)

### Priority 1 — Unblock compilation and data loss:

1. **Remove duplicate ProgressViewModel/ProgressUiState** — keep the enhanced version (from main, PR #6), port legacy fields if needed, delete the duplicate
2. **Fix ProgramGenerator equipment filtering** — split seeded comma-joined tokens, map coarse categories/snake_case muscle IDs to generator vocabulary, add test asserting non-empty days for all frequencies incl. 2
3. **Rewrite MIGRATION_4_5** against actual entity schemas; add Room `MigrationTestHelper` tests covering 4→5→6→7; verify data preservation

### Priority 2 — Fix critical architecture:

4. **Reconcile VolumeCalculator** — adopt LoggedSet as canonical DTO; fix: missing `w.completed = 1` filter, ordinal collapse, weeklySets vs directSets mismatch, secondary/stabilizer credit TODO
5. **Fix sorted-index vs raw-index** in workout set editing — pass `set.id` not index
6. **Fix ProgramGenerator frequency=2** — add missing branch to generate correct 2-day program

### Priority 3 — Infrastructure and quality:

7. **Add Room migration tests** — MigrationTestHelper for 1→7 across all tables
8. **Add signing config** — release build configuration
9. **Add test coverage** — navigation, onboarding, migration, VolumeCalculator edge cases, PRDetector scenarios
10. **Encrypt Room DB** — if health data privacy is a requirement, add SQLCipher or platform encryption