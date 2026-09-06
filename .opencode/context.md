# GymCoach Forensic Baseline Context

## Repository State
- **Remote**: https://github.com/vishalm111296-commits/Gymcoach-.git
- **Branch**: main (up to date with origin/main)
- **Current HEAD SHA**: fb27245
- **Latest commit**: `fix: display progression recommendations in workout session`
- **Working tree**: Clean (only .opencode/ untracked)
- **Open PRs**: 0

## Environment
- **Platform**: ARM64 Linux (aarch64 proot-distro Ubuntu on Android)
- **JDK**: OpenJDK 17.0.20
- **Android SDK**: /root/android-sdk (API 34, build-tools 34.0.0)
- **Gradle**: 8.4 (wrapper)
- **CRITICAL BLOCKER**: AAPT2 binary is x86_64, no QEMU emulation on ARM64 host. Cannot run Gradle build tasks locally. Must use Jules for build verification.

## Architecture
- **Pattern**: Clean Architecture + MVVM + Hilt DI
- **UI**: Jetpack Compose + Material Design 3
- **Database**: Room v11 (SQLite), schema version 11
- **Camera**: CameraX + MediaPipe Tasks Vision
- **Video**: Media3 ExoPlayer
- **Async**: Kotlin Coroutines + Flow
- **Build**: Gradle Kotlin DSL, KSP, min SDK 26, target SDK 34

## File Counts
- **Main source**: 109 Kotlin files
- **Unit tests**: 9 files
- **Android tests**: 6 files
- **JSON assets**: 17 exercise data files

## Database
- **Current version**: 11
- **Migrations**: MIGRATION_1_2 through MIGRATION_10_11 (all present)
- **Entities**: 19 (exercises, workouts, programs, measurements, profiles, readiness, etc.)
- **Export schema**: Yes (app/schemas/)

## Key Core Logic
- **ProgramGenerator**: Rule-based, frequency/equipment/goal-aware, V-Taper prioritized
- **VolumeCalculator**: PRIMARY=1.0, SECONDARY=0.5, STABILIZER=0.25 weighting with ISO week bucketing
- **ProgressionEngine**: Double progression (weight increase when all sets hit top reps; equipment-limited: add sets/reps)
- **PRDetector**: Weight, Rep, Estimated 1RM (Epley), Volume PRs
- **SubstitutionEngine**: Deterministic exercise substitution by muscle/equipment match
- **FormAnalyzer**: 9 exercise types, MediaPipe pose analysis
- **RestTimerManager**: Countdown with pause/resume, notification service

## Known Bugs/Issues (verified from source)
1. **HomeViewModel planned volume attribution**: Broadcasts entire day's total set count to every target muscle instead of per-exercise attribution. Each muscle gets `daySets` (total of ALL exercises for that day) instead of only the sets for exercises targeting that specific muscle.
2. **VolumeCalculator has no dedicated unit tests** - Phase 1 requires adding regression tests for primary/secondary/stabilizer weighting, fractional averages, ISO week crossing, warmup exclusion, classification thresholds.
3. **Exercise media**: All placeholder (typography-based "A" boxes), no actual instructional media.
4. **Camera runtime**: Cannot be verified without physical device.
5. **Build environment**: Cannot run locally due to AAPT2 x86_64 on ARM64.

## Recent Fixes (by explore agent)
1. Fixed WorkoutHistoryDetail delete confirmation never showing
2. Fixed ReadinessRepositoryIntegrationTest assertion mismatch
3. Fixed ProgramGeneratorTest missing readinessRepository property declaration
4. Removed duplicate ProgramGeneratorTest in src/test/java
