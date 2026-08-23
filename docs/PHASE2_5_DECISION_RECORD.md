## CI

**Current state: FAILING on both branches.**

**Phase 3/p0-stabilization (SHA a24c16702a98e7425bb614dbb195dea205e34fcc):**
- Lint: FAIL — compilation errors (PersonalRecordDao, BodyMeasurementDao, FavoriteExerciseDao, SeedData files)
- Test: FAIL — same compilation errors + test setup issues
- Build: FAIL — same compilation errors
- Workflow: `.github/workflows/android-build.yml` rebuilt with real `lintDebug`/`testDebugUnitTest`/`assembleDebug` gates; results posted to issue #9; artifacts uploaded
- Evidence: issue #[ci-run-report] GymCoach CI results records all FAIL; latest run comments capture compiler lines

**Phase 2/production-hardening (SHA b514938380f81752ab84f80d938b884058730aa5):**
- CI status unverifiable (403 on check_runs via MCP token)
- PR #11 is DRAFT; known compile blockers listed in body (ProgressViewModel duplicate, BodyMeasurementDao fix, ExerciseSubstitutionDao fix, VolumeCalculator SetWithContext, navigation wiring)
- Mergeable state: `unstable` (failing/pending required checks, not `dirty`/`blocked` from conflicts)

**Required before merge:**
1. Fix all compilation blockers (C1 duplicate ViewModel, C3 migration crashes, H4 seeder dupes, etc.)
2. Re-run CI on green
3. Verify all workflow jobs pass with artifacts