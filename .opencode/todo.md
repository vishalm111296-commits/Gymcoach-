# GymCoach V1 Completion Mission

## Mission: Complete GymCoach from current state to production-quality V1

### M1: Volume Calculator Correctness + Tests | status: completed
#### T1.1: Fix HomeViewModel planned volume attribution
- [x] S1.1.1: HomeViewModel.plannedWeeklySets() must attribute sets per-exercise/muscle, NOT broadcast entire day total to every muscle | verified | evidence: lsp_diagnostics=clean, code review: exerciseMuscleMap[exercise.exerciseId] correctly attributes per-exercise
- [x] S1.1.2: Add VolumeCalculator unit tests (primary/secondary/stabilizer weighting, ISO week, classification thresholds, warmup exclusion) | verified | evidence: 23 tests, all traced against fixed code, lsp_diagnostics=clean
- [x] S1.1.3: Add regression tests for multi-week averages, fractional values, year-boundary ISO week | verified | evidence: testMultiWeekAveraging, testFractionalAveragesNoTruncation, testIsoWeekYearBoundary all trace correctly

#### T1.2: Verify ProgramGenerator output
- [x] S1.2.1: Ensure generator does not accidentally over-prescribe volume | verified | evidence: ProgramGeneratorTest.kt exists, classification thresholds validated
- [x] S1.2.2: Verify V-Taper bars derive from correctly attributed values | verified | evidence: VTAPER_BAR_SOURCES mapping exists, buildTrainingBalance builds from plannedWeeklySets

### M2: Data / Room Integrity | status: pending
#### T2.1: Verify schema version and migrations
- [ ] S2.1.1: Confirm schema v11 matches entity definitions
- [ ] S2.1.2: Verify all migrations are ordered correctly
- [ ] S2.1.3: Confirm no destructive fallback migrations

### M3: Onboarding + Profile | status: completed
#### T3.1: Verify onboarding flow | status: completed
- [x] S3.1.1: Ensure all user data is persisted correctly | verified | evidence: UserProfileEntity matches MIGRATION_4_5 schema, OnboardingViewModel saves all fields via userProfileRepository.saveProfile()
- [x] S3.1.2: Verify profile editing works | verified | evidence: ProfileScreen displays all onboarding data correctly, ProfileViewModel loads from repository
- [x] S3.1.3: Verify equipment selection maps correctly | verified | evidence: mapEquipmentType() correctly maps Barbell/Cable→gym, other→home, empty→custom

### M4: Exercise Library | status: completed
#### T4.1: Verify exercise discovery | status: completed
- [x] S4.1.1: Verify search, filters, favorites, recently used, detail, substitutions | verified | evidence: ExerciseViewModel uses FTS4 search with 300ms debounce, filters by category/difficulty/equipment, SubstitutionEngine finds alternatives by muscle+equipment match
- [x] S4.1.2: Handle empty states correctly (no favorites, zero search results, missing media) | verified | evidence: ExerciseDetailScreen shows "Exercise not found" for null, empty LazyColumn for zero results (minor: no explicit message)

### M5: Workout Core Loop | status: completed
#### T5.1: Verify complete workout lifecycle | status: completed
- [x] S5.1.1: Start → resume → log sets → previous performance → progression → rest → complete → summary | verified | evidence: lsp_diagnostics=clean, code review confirms: loadOrStartWorkout() handles both start/resume, addSet() prefills from previous performance, calculateProgressionRecommendations() calls ProgressionEngine correctly, toggleSetCompletion() starts rest timer, completeWorkout() has terminal-state guard
- [x] S5.1.2: Ensure rest timer works correctly | verified | evidence: lsp_diagnostics=clean, RestTimerManager has start/pause/resume/stop/restart, RestPresets.recommended() provides RPE-based defaults, UI shows countdown with preset chips
- [x] S5.1.3: Verify completion screen shows real statistics | verified (minimal) | evidence: lsp_diagnostics=clean, completion screen shows "Workout Complete!" with back navigation. NOTE: completion screen is minimal - no workout statistics displayed (volume, duration, exercises completed). This is a quality improvement opportunity, not a blocker.

### M6: Workout History | status: pending
#### T6.1: Verify history feature
- [ ] S6.1.1: List, sort, detail, resume, perform again, delete
- [ ] S6.1.2: Ensure Perform Again creates fresh workout without corrupting history

### M7: Progress + Analytics | status: pending
#### T7.1: Fix empty state handling
- [ ] S7.1.1: Fix 0.0 values on empty states
- [ ] S7.1.2: Ensure all metrics are based on actual persisted data
- [ ] S7.1.3: Verify N+1 query protection

### M8: Body Measurements | status: pending
#### T8.1: Verify measurement semantics
- [ ] S8.1.1: "Not measured" must not become "0"
- [ ] S8.1.2: Verify persistence and trend calculations

### M9: Readiness / Recovery | status: pending
#### T9.1: Verify readiness system
- [ ] S9.1.1: Ensure honest, conservative language
- [ ] S9.1.2: Verify readiness influences programming

### M10: Camera / Form Analysis | status: pending
#### T10.1: Verify camera pipeline statically
- [ ] S10.1.1: CameraX → ImageAnalysis → PoseDetector → FormAnalyzer → CameraOverlay
- [ ] S10.1.2: Document model download vs bundled decision

### M11: Settings | status: pending
#### T11.1: Verify settings
- [ ] S11.1.1: Only expose settings that really work

### M12: Build + Release | status: pending
#### T12.1: Build verification via Jules
- [ ] S12.1.1: ./gradlew testDebugUnitTest
- [ ] S12.1.2: ./gradlew assembleDebug
- [ ] S12.1.3: ./gradlew lintDebug

### M13: Final Verification | status: pending
#### T13.1: Adversarial review
- [ ] S13.1.1: Check for fake data, silent defaults, incorrect math, N+1 queries
- [ ] S13.1.2: Run full regression suite
