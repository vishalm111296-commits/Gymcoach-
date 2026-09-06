# GymCoach Forensic Baseline Context

## Repository State
- **Remote**: https://github.com/vishalm111296-commits/Gymcoach-.git
- **Branch**: main (10 commits ahead of origin/main)
- **Current HEAD SHA**: e97c357
- **Latest commit**: `chore: update status and sync issues`
- **Working tree**: Clean

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

## Current Status

### M8 Body Measurement Null Handling — COMPLETE ✅
Convention: `0.0` in DB = "not measured". ViewModel maps `0.0 → null` via `takeIf { it > 0 }`. UI shows "Not measured" for null.

| File | Status | LSP |
|------|--------|-----|
| `BodyMeasurementEntity.kt` | ✅ Non-nullable `Double = 0.0` (matches schema v11) | Clean |
| `ProgressViewModel.kt` | ✅ `takeIf { it > 0 }` on load, `?: 0.0` on save | Clean |
| `BodyMeasurementTrend.kt` | ✅ Nullable `currentValue: Double?`, "Not measured" empty state | Clean |
| `ProgressDashboardScreen.kt` | ✅ Passes nullable values directly | Clean |
| `BodyMeasurementTest.kt` | ✅ 17 unit tests for null handling convention | Clean |

### Other Completed M2-M11 Items
All milestones M2-M11 verified and committed (see git log for details).

## Pending Tasks
- None currently. All M8 work is committed and verified.
- User has not yet requested next work.

## Known Bugs/Issues (verified from source)
1. **HomeViewModel planned volume attribution**: Broadcasts entire day's total set count to every target muscle instead of per-exercise attribution.
2. **VolumeCalculator has no dedicated unit tests** — needs regression tests.
3. **Exercise media**: All placeholder (typography-based "A" boxes).
4. **Camera runtime**: Cannot be verified without physical device.
5. **Build environment**: Cannot run locally due to AAPT2 x86_64 on ARM64.

## Recent Fixes
1. Fixed WorkoutHistoryDetail delete confirmation never showing
2. Fixed ReadinessRepositoryIntegrationTest assertion mismatch
3. Fixed ProgramGeneratorTest missing readinessRepository property
4. Removed duplicate ProgramGeneratorTest in src/test/java
