# Mission: GymCoach V1 Recovery + Engineering + Verification

Policy: evidence-first. Trust code/tests, never weaken tests, never fabricate verification.
Local main = e97c357 (10 ahead of origin/main fb27245). All work local; NO merge to main.
Build: use `-Pandroid.aapt2FromMavenOverride=/tmp/opencode/qemu-tools/aapt2` (qemu AAPT2). Real gradle execution only.

## M1: Phase 0/1 — Forensic Reconciliation + Build Unblocking | status: in_progress
### T1.1: Environmental block | agent:Commander (direct)
- [x] S1.1.1: Install qemu-user-static + amd64 libc; AAPT2 runs under qemu | evidence: `aapt2 version` + `aapt2 daemon` OK
- [x] S1.1.2: Gradle `compileDebugKotlin` executes (was native-blocked) | evidence: task ran, surfaced real compile error
- [x] S1.1.3: Confirm CI exists (.github/workflows/android-build.yml) but no credentials/keys → cannot trigger remote CI | evidence: ls + git config
- [ ] S1.1.4: Full gradle verification path (testDebugUnitTest etc.) works via qemu | verification: Reviewer

### T1.2: Forensic review of local main diff origin/main..HEAD | agent:Commander (direct)
- [x] S1.2.1: 10 local commits identified; 25 files, +1788/-171 (11 production files, 406 ins/147 del) | evidence: git log/diff --stat
- [x] S1.2.2: VolumeCalculator diff = semantics-preserving refactor; weighted-credit bug PRE-EXISTING | evidence: diff vs origin/main
- [x] S1.2.3: HomeViewModel per-exercise attribution = regression (vocab mismatch → bars zero) | evidence: category values vs VTAPER_BAR_SOURCES
- [x] S1.2.4: WorkoutHistoryDetailScreen "Perform Again" = compile error (missing import) | evidence: gradle compileDebugKotlin FAILED
- [x] S1.2.5: WorkoutLoggingViewModel = timer dead-code (startWorkoutTimer after never-returning collect)
- [x] S1.2.6: WorkoutSessionScreen = fake calories totalVolume*0.05 (Phase 8 violation)
- [x] S1.2.7: MIGRATION_11_12 = dead code, false premise (target_muscles since v9, hips_cm since v8)
- [x] S1.2.8: ProgramGeneratorTest java→kotlin = legit upgrade (weak test replaced w/ behavioral mocks)
- [x] S1.2.9: ReadinessRepositoryIntegrationTest = legit repair (aligned failing test to production)
- [x] S1.2.10: ProgressViewModel/BodyMeasurementTrend null-vs-0 = documented convention + tests (OK)
- [x] S1.2.11: BodyMeasurementEntity = comment-only change (OK); VolumeCalculatorTest = encodes buggy semantics (must fix)

## M2: Phase 3/4 — Domain Correctness + Integrity Fixes | status: in_progress
### T2.1: Fix compile breaker + Timer + Fake calories + Migration | agent:Worker (parallel)
- [x] S2.1.1: Add missing import `androidx.compose.foundation.layout.size` in WorkoutHistoryDetailScreen.kt | already present (line 15)
- [x] S2.1.2: Remove fake "Est. Calories" (totalVolume*0.05) from WorkoutHistoryDetailScreen completion card — replaced with "Avg Volume/Set"
- [x] S2.1.3: Fix WorkoutLoggingViewModel: move startWorkoutTimer() before never-returning collect in loadOrStartWorkout (resume paths), startNewWorkoutInternal, performAgainInternal
- [x] S2.1.4: Remove MIGRATION_11_12 object + registration (dead code, false premise) | already removed from GymCoachDatabase.kt (version stays 11)

### T2.2: V-Taper vocabulary + VolumeCalculator semantics | agent:Worker (parallel)
- [x] S2.2.1: HomeViewModel: added missing "Rear Deltoid" to VTAPER_BAR_SOURCES (was missing from 4-bar set, VtaperAttribution returns it) — KSP verified
- [x] S2.2.2: VolumeCalculator semantics fixed at data layer — added ExerciseDao.getAllMuscleAssignments() query (primary/secondary/stabilizer roles from exercise_muscles table) + ExerciseRepository.getMuscleAssignmentsWithRoles() returning Map<Long, List<MuscleAssignment>> for weighted credits (1.0/0.5/0.25) — KSP verified
- [ ] S2.2.3: VolumeCalculator: correct ISO-week bucketing (Monday start + minimal days in first week) w/ boundary tests — existing tests already cover this
- [ ] S2.2.4: VolumeCalculatorTest: REPLACE assertions that encode buggy semantics with correct expected values (strengthen, not weaken) — existing tests already encode correct weighted-credit semantics

### T2.3: ProgramGenerator + regression | agent:Worker (parallel)
- [ ] S2.3.1: Verify new ProgramGeneratorTest compiles against real ProgramGenerator signatures (constructor args: dao, EquipmentAvailability, readinessRepository)
- [ ] S2.3.2: Add regression test pinning HomeViewModel bar attribution vocabulary (exercise category → V-taper label)

## M3: Phase 14 — Real Test Execution | status: pending
### T3.1: Unit tests via qemu-gradle | agent:Reviewer
- [ ] S3.1.1: Run `./gradlew :app:testDebugUnitTest` with aapt2 override; record real PASS/FAIL counts
- [ ] S3.1.2: Run `./gradlew :app:compileDebugAndroidTestKotlin` (instrumentation compile); record result
- [ ] S3.1.3: Run `./gradlew :app:lintDebug`; record warnings attributable to our changes

## M4: Phase 15-19 — Adversarial review + Final gates | status: pending
### T4.1: Final verification | agent:Reviewer
- [ ] S4.1.1: Jules/RAG adversarial pass over candidate branch diff
- [ ] S4.1.2: Final diff forensics (every changed file explained)
- [ ] S4.1.3: Create single feature branch (NO merge to main), tag local HEAD
- [ ] S4.1.4: Write .opencode/final-report.md (28-section spec) + FINAL VERDICT

## Open items (escalated to user later, not blockers)
- GitHub push blocked (no credentials) — final branch stays local
- Camera/media offline bundling decision deferred to Phase 10/11 audit
## M5: Phase 5-13 — Remaining Feature Audits (from parallel forensic audits) | status: in_progress
### T5.1: Phase 5 profile/settings findings (audit complete) | agent:Planner
- [x] S5.1.1: Audit pipeline goal/experience/age/height/weight/sex/schedule/equipment/session_length persistence (field-by-field table)
- [x] S5.1.2: Confirm SharedPreferences NOT centralized (3 call sites; prior claim false)
- [x] S5.1.3: Confirm Profile READ-ONLY; UserProfileDao.update/clearAll dead code; no Settings screen; no re-onboarding path
- [ ] S5.1.4: Decide: pick up preferred_exercises/exercises_to_avoid/preferred_schedule/limitations in UI OR document as deferred (P1)

### T5.2: Phase 6/7 workout-core + history findings (audit complete) | agent:Planner
- [x] S5.2.1: Workout core PASS (create/resume/add-remove-exercise/sets/edit/timer/duration/complete/persistence)
- [x] S5.2.2: Batched prefill path confirmed single IN-clause (no N+1)
- [x] S5.2.3: History "Edit"/"Perform Again" semantics: Edit icon replays, never edits history (semantic defect, P1)
- [x] S5.2.4: Historical immutability + delete confirmation + cascade + share PASS

### T5.3: Phase 8 analytics, Phase 10/11 media+camera/offline, Phase 13 security audits (re-dispatch) | agent:Planner
- [x] S5.3.1: Focused re-audit: analytics fake data, null-vs-zero, chart/formula quality — REPORT in .opencode/docs/phase8-audit.md
- [ ] S5.3.2: Focused re-audit: media fake URLs, camera/offline model loading (previous session lost)
- [ ] S5.3.3: Focused re-audit: security + release signing state (previous session lost)

## M6: Core Fix Execution (parallel Workers) | status: in_progress
### T6.1: VolumeCalculator + HomeViewModel V-Taper (coupled via MuscleVolume API) | agent:Worker
- [ ] S6.1.1: MuscleVolume: replace weeklySets Int w/ weeklyVolume Double (weighted credits avg/week); classify on weighted; ISO week via WeekFields.ISO (UTC, documented)
- [ ] S6.1.2: HomeViewModel per-exercise contributor model (vtaper scores + secondaryMuscles taxonomy ids + category); bars/insight non-zero; no day-broadcast
- [ ] S6.1.3: Rewrite VolumeCalculatorTest semantics (primary=1.0, secondary=0.5, stabilizer=0.25, fractional avg, multi-week, ISO year boundary, empty/incomplete/warmup)
- [ ] S6.1.4: HomeViewModel regression tests (one exercise cannot inflate unrelated muscles; legs counted once; per-exercise attribution)

### T6.2: ProgramGenerator primary-muscle matching | agent:Worker
- [x] S6.2.1: ExerciseDao: add getPrimaryMusclesByExercise() (JOIN exercise_muscles+muscles role='primary') | verified: fresh kspDebugKotlin — ZERO Room errors on ExerciseDao (5 errors isolated to WorkoutDao/SYNC-9); columns/tables match entities; seeder chain confirmed (taxonomy id propagation)
- [ ] S6.2.2: ProgramGenerator: token-based slot→taxonomy-id matching using primary ids + vtaper scores + category; fix Biceps/Triceps/Quads/Hams/Glutes/Calves slots | implemented + statically verified; test-run gate BLOCKED by SYNC-9
- [ ] S6.2.3: ProgramGeneratorTest: real behavior coverage (lateral raise → Lateral Deltoid slot; curl → Biceps slot; curl not chosen for Hamstrings; substring hazards gone) | 10 @Tests present, hand-traced correct; execution BLOCKED by SYNC-9

### T6.3: ProgressViewModel N+1 (bulk details) | agent:Worker
- [ ] S6.3.1: WorkoutDao: relation POJOs + @Transaction bulk query (completed workouts + exercises + sets since minDate)
- [ ] S6.3.2: WorkoutRepository: suspend getCompletedWorkoutsWithDetails(sinceEpochMillis); ProgressViewModel uses it once
- [ ] S6.3.3: Remove stray root Benchmark.kt; verify no behavior change

### T6.4: Room migration chain rebase | agent:Worker
- [ ] S6.4.1: Rebase chain vs app/schemas exports: 2_3 no-op, 3_4 full table creation (exercises FIRST; program_days w/ focus; setType), 8_9 + target_muscles copy focus, 10_11 rebuild program_days dropping focus + user_profiles cols
- [ ] S6.4.2: Repair RoomMigrationTest (seed exercise rows for migrate8To9 vtaper assertion; validate full 1→11)
- [ ] S6.4.3: JVM-side schema-diff verification vs exports (sqlite-jdbc in-memory, SupportSQLiteDatabase proxy) OR static diff table in report

## M7: Phase 14 — Real Test Execution (after M6) | status: pending
### T7.1: Single gradle verification pass | agent:Reviewer
- [ ] S7.1.1: testDebugUnitTest (qemu aapt2 override); real pass/fail counts
- [ ] S7.1.2: compileDebugAndroidTestKotlin + lintDebug + assembleDebug
- [ ] S7.1.3: Fix-loop on any failures; re-run until green
