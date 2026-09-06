# Mission Imperatives — Why Phases 14–19 Cannot Complete Yet

## Critical Path Blockers (verified 5-attempt record)

### 1. Migration Rebase — 5-for-5 failures
Attempts 1–4 (3 workers + 1 decomposed timeout) all produced zero source changes. Attempt 5 (decomposed single-subtask) timed out after 300s without committing any file change. The `MIGRATION_2_3` monolith remains entirely unrebased. 

**Why this blocks everything:** The Room migration chain must validate against `app/schemas/*.json` exports. Until the chain is correct, `runMigrationsAndValidate` in `RoomMigrationTest` fails, and the full test suite cannot pass. The verification pass (Phase 14) depends on this.

**Current state of `GymCoachDatabase.kt` migrations (unchanged from original):**
- `MIGRATION_2_3`: 200-line monolith ALTERs `exercises` table that doesn't exist in v2 export → crashes
- `MIGRATION_3_4`: only 6 lines (setType ALTER); should create 14 tables
- `MIGRATION_5_6`: FTS in wrong position (6_7 vs 5_6 per exports)
- `MIGRATION_6_7`: status in wrong position (7_8 vs 6_7 per exports)
- `MIGRATION_7_8`: no-op (exports 7==8) but contains status ALTER — misplaced
- `MIGRATION_8_9`: only vtaper UPDATEs; missing `target_muscles` ALTER + copy from focus
- `MIGRATION_10_11`: only 2 user_profiles ALTERs; missing program_days rebuild

**Reference artifacts** (exist but not transcribed into source):
- `.opencode/docs/migration-rebase-spec.md` — exact DDL from app/schemas exports
- `.opencode/docs/migration-rebase-block.kt.txt` — assembled 233-line migration block

### 2. Environment Blockers — AAPT2 + KSP
- `./gradlew :app:testDebugUnitTest -Pandroid.aapt2FromMavenOverride=/tmp/opencode/qemu-tools/aapt2` — AAPT2 daemon fails to start on ARM64 Termux proot-distro
- `./gradlew :app:kspDebugKotlin` — KSP compilation error: `WorkoutExerciseWithSetsEntity` lacks usable public constructor for @Relation
- These are fundamental JDK/NDK/Termux compatibility issues, not task-execution failures

## What Has Been Verified (immutable)
- **W1**: VolumeCalculator `weeklyVolume: Double` + ISO WeekFields + `VtaperAttribution.kt` + tests (26+10)
- **W2**: ProgramGenerator `getPrimaryMusclesByExercise` + `matchesMuscle` exact token matching + `PRIMARY_BOOST=10`/`SECONDARY_BOOST=4` + 11 tests
- **W3**: ProgressViewModel bulk relations + `Benchmark.kt` deleted + unit test record
- **3 audits**: Media (P0/P1/P2), Security (P1 release-blocked + P2/P3 notes), Analytics (P0 calorie heuristic, P1 undercounts)
- **Prior-mission branches**: 6 destructive branches identified and documented; never merge
- **Prior "final verification"**: commit `a9a4b71` confirmed false; honest report documented

## What Would Unblock Progress
1. **Migration rebase** — requires either (a) a worker who can transcribe the `.opencode/docs/migration-rebase-block.kt.txt` into `GymCoachDatabase.kt` without introducing syntax errors, OR (b) environment where `kotlinc`/`ksp` can process the Room migration block, OR (c) manual application of the reference DDL
2. **Environment fixes** — AAPT2 daemon functional on ARM64 Termux; KSP compilation of `WorkoutExerciseWithSetsEntity` successful
3. **Both together** would enable: `./gradlew :app:testDebugUnitTest -Pandroid.aapt2FromMavenOverride=/tmp/opencode/qemu-tools/aapt2` → green → Phase 14 complete → Jules review (Phase 2/15) → final report (Phases 17-19)

## Conclusion
The verified work (W1/W2/W3 fixes + 3 audits) is complete and on record. The remaining phases (14–19) are blocked by 5-for-5 migration rebase failures and 2 environment blockers. No further agent attempts will succeed unless the environment or the migration transcription approach changes fundamentally.