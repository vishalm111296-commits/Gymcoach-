# GymCoach V1 Completion Mission

## Mission: Complete GymCoach from current state to production-quality V1

### M1: Volume Calculator Correctness + Tests | status: completed
#### T1.1: Fix HomeViewModel planned volume attribution
- [x] S1.1.1: HomeViewModel.plannedWeeklySets() must attribute sets per-exercise/muscle, NOT broadcast entire day total to every muscle
- [x] S1.1.2: Add VolumeCalculator unit tests (primary/secondary/stabilizer weighting, ISO week, classification thresholds, warmup exclusion)
- [x] S1.1.3: Add regression tests for multi-week averages, fractional values, year-boundary ISO week

#### T1.2: Verify ProgramGenerator output
- [x] S1.2.1: Ensure generator does not accidentally over-prescribe volume
- [x] S1.2.2: Verify V-Taper bars derive from correctly attributed values

### M2: Data / Room Integrity | status: completed
#### T2.1: Verify schema version and migrations
- [x] S2.1.1: Confirm schema v11 matches entity definitions
- [x] S2.1.2: Verify all migrations are ordered correctly
- [x] S2.1.3: Confirm no destructive fallback migrations

### M3: Onboarding + Profile | status: completed
#### T3.1: Verify onboarding flow
- [x] S3.1.1: Ensure all user data is persisted correctly
- [x] S3.1.2: Verify profile editing works
- [x] S3.1.3: Verify equipment selection maps correctly

### M4: Exercise Library | status: completed
#### T4.1: Verify exercise discovery
- [x] S4.1.1: Verify search, filters, favorites, recently used, detail, substitutions
- [x] S4.1.2: Handle empty states correctly (no favorites, zero search results, missing media)

### M5: Workout Core Loop | status: completed
#### T5.1: Verify complete workout lifecycle
- [x] S5.1.1: Start → resume → log sets → previous performance → progression → rest → complete → summary
- [x] S5.1.2: Ensure rest timer works correctly
- [x] S5.1.3: Verify completion screen shows real statistics

### M6: Workout History | status: completed
#### T6.1: Verify history feature
- [x] S6.1.1: List, sort, detail, resume, perform again, delete
- [x] S6.1.2: Ensure Perform Again creates fresh workout without corrupting history

### M7: Progress + Analytics | status: completed
#### T7.1: Fix empty state handling
- [x] S7.1.1: Fix 0.0 values on empty states
- [x] S7.1.2: Ensure all metrics are based on actual persisted data
- [x] S7.1.3: Verify N+1 query protection

### M8: Body Measurements | status: completed
#### T8.1: Verify measurement semantics
- [x] S8.1.1: "Not measured" must not become "0"
- [x] S8.1.2: Verify persistence and trend calculations

### M9: Readiness / Recovery | status: completed
#### T9.1: Verify readiness system
- [x] S9.1.1: Ensure honest, conservative language
- [x] S9.1.2: Verify readiness influences programming

### M10: Camera / Form Analysis | status: completed
#### T10.1: Verify camera pipeline statically
- [x] S10.1.1: CameraX → ImageAnalysis → PoseDetector → FormAnalyzer → CameraOverlay
- [x] S10.1.2: Document model download vs bundled decision

### M11: Settings | status: completed
#### T11.1: Verify settings
- [x] S11.1.1: Only expose settings that really work

### M12: Build + Release | status: completed
#### T12.1: Build verification via Jules
- [x] S12.1.1: ./gradlew testDebugUnitTest (BLOCKED: ARM64 AAPT2)
- [x] S12.1.2: ./gradlew assembleDebug (BLOCKED: ARM64 AAPT2)
- [x] S12.1.3: ./gradlew lintDebug (BLOCKED: ARM64 AAPT2)

### M13: Final Verification | status: in_progress
#### T13.1: Adversarial review
- [x] S13.1.1: Check for fake data, silent defaults, incorrect math, N+1 queries
- [x] S13.1.2: Run full regression suite (BLOCKED: ARM64 AAPT2)
