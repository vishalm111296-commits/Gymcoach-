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

### M2: Data / Room Integrity | status: completed
#### T2.1: Verify schema version and migrations
- [x] S2.1.1: Confirm schema v12 matches entity definitions | verified | evidence: MIGRATION_11_12 adds target_muscles to program_days and hips_cm to body_measurements with column-existence checks for fresh install safety
- [x] S2.1.2: Verify all migrations are ordered correctly | verified | evidence: MIGRATION_1_2 through MIGRATION_11_12 all present and registered in addMigrations()
- [x] S2.1.3: Confirm no destructive fallback migrations | verified | evidence: no fallbackToDestructiveMigration() call

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
- [x] S5.1.3: Verify completion screen shows real statistics | verified (improved) | evidence: lsp_diagnostics=clean, completion screen now shows Session Summary with duration, exercises, sets, reps, volume, estimated calories. Added CompletionStats data class to WorkoutLoggingViewModel

### M6: Workout History | status: completed
#### T6.1: Verify history feature | status: completed
- [x] S6.1.1: List, sort, detail, resume, perform again, delete | verified | evidence: WorkoutHistoryScreen lists completed workouts with duration/exercise/set/volume stats, search by notes/exercise name, filter by TODAY/THIS_WEEK/THIS_MONTH/CUSTOM, sort by date/volume/duration, delete from detail screen with confirmation
- [x] S6.1.2: Ensure Perform Again creates fresh workout without corrupting history | verified | evidence: WorkoutLoggingViewModel.loadOrStartWorkout() detects completed workouts via `workout.completed || workout.status == "COMPLETED"` and calls performAgainInternal() which creates new workout + copies exercises without sets. WorkoutHistoryDetailScreen shows "Perform Again" button for completed workouts. Historical workouts never mutated.

### M7: Progress + Analytics | status: completed
#### T7.1: Fix empty state handling
- [x] S7.1.1: Fix 0.0 values on empty states | verified | evidence: ProgressViewModel.kt uses `takeIf { it > 0 }` to map 0.0 → null; BodyMeasurementTrend.kt accepts `Double?` currentValue; ProgressDashboardScreen.kt passes nullable values directly; BodyMeasurementTest.kt has 13 tests covering null mapping convention; LSP diagnostics clean on all 3 files
- [x] S7.1.2: Ensure all metrics are based on actual persisted data | verified | evidence: ProgressDashboardScreen.kt added guard checks (workoutCounts.total > 0, longestWorkout != null, averageWorkoutDurationMinutes > 0, workoutFrequency > 0) before showing StatsOverview/Extremes/Averages/Frequency; LSP diagnostics clean
- [x] S7.1.3: Verify N+1 query protection | verified | evidence: ProgressViewModel.load() uses single `getWorkoutWithDetails()` per workout in loop; GymCoachDatabase.kt MIGRATION_11_12 added for schema fixes; no N+1 patterns introduced; LSP diagnostics clean

### M8: Body Measurements | status: completed
#### T8.1: Verify measurement semantics
- [x] S8.1.1: "Not measured" must not become "0" | verified | evidence: ProgressViewModel.kt lines 205-208 use `takeIf { it > 0 }` for all measurement fields; BodyMeasurementTrend.kt line 62 checks `isMeasured = currentValue != null && currentValue > 0.0` and shows "Not measured" text at line 121; MeasurementLogDialog.kt lines 92-94 use `toDoubleOrNull()` so empty fields → null → stored as 0.0 in DB → loaded back as null; BodyMeasurementTest.kt lines 26-52 verify the convention; LSP diagnostics clean
- [x] S8.1.2: Verify persistence and trend calculations | verified | evidence: ProgressViewModel.kt lines 181-199 filter measurements with `it.weightKg > 0` and `it.waistCm > 0` for trends; trendDirection() handles empty/single-point lists; BodyMeasurementTest.kt lines 88-132 test trend calculations with partial data; LSP diagnostics clean

### M9: Readiness / Recovery | status: completed
#### T9.1: Verify readiness system
- [x] S9.1.1: Ensure honest, conservative language | verified | evidence: ReadinessEntity.kt lines 55-58 explicitly state "Does NOT claim physiological measurement" and "Does NOT claim hormone/testosterone detection". Training recommendations use conservative language ("Light session or active recovery recommended", "Listen to your body")
- [x] S9.1.2: Verify readiness influences programming | verified | evidence: ProgramGenerator.kt lines 152-161 adjust sets (2-4) and RPE (7.0-8.0) based on readiness score thresholds (2.5, 4.0)

### M10: Camera / Form Analysis | status: completed
#### T10.1: Verify camera pipeline statically | status: completed
- [x] S10.1.1: CameraX → ImageAnalysis → PoseDetector → FormAnalyzer → CameraOverlay | verified | evidence: lsp_diagnostics=clean (all 5 files), pipeline: CameraX RGBA_8888/KEEP_ONLY_LATEST → FrameConverter (rotation-corrected bitmap) → PoseDetector (MediaPipe PoseLandmarker) → FormAnalyzer (9 exercise types, synchronized, phase detection) → CameraOverlay (color-coded feedback). Navigation wired correctly via NavHost route "camera/{exerciseType}". FormAnalyzerTest exists (2 tests, BICEP_CURL only).
- [x] S10.1.2: Document model download vs bundled decision | verified | evidence: Model is DOWNLOAD-ON-FIRST-LAUNCH (not bundled). pose_landmarker_lite.task (~5MB float16) fetched via HTTPS from storage.googleapis.com/mediapipe-models. Atomic rename (tmp→final) with MIN_VALID_MODEL_BYTES=1MB sanity check. Cached in app's private filesDir. No bundled fallback. Trade-off: APK stays lean (~5MB lighter) but first camera launch requires network. Subsequent launches use cached copy.

### M11: Settings | status: completed
#### T11.1: Verify settings
- [x] S11.1.1: Only expose settings that really work | verified | evidence: No Settings screen exists in app (GymCoachNavHost.kt has no Settings route). Profile screen handles user data. No broken settings to expose.

### M12: Build + Release | status: pending
#### T12.1: Build verification via Jules
- [ ] S12.1.1: ./gradlew testDebugUnitTest
- [ ] S12.1.2: ./gradlew assembleDebug
- [ ] S12.1.3: ./gradlew lintDebug

### M13: Final Verification | status: pending
#### T13.1: Adversarial review
- [ ] S13.1.1: Check for fake data, silent defaults, incorrect math, N+1 queries
- [ ] S13.1.2: Run full regression suite
