# RELEASE_CHECKLIST.md

## Prerequisites
- [x] Java 17+ installed and configured (Temurin OpenJDK 17).
- [x] Android SDK API 36 / 34 installed.
- [x] Android SDK Build-tools 35.0.0 installed.
- [x] Gradle wrapper 8.11.1 executable (`chmod +x gradlew`).

## Build Verification
- [x] Gradle sync is successful with AGP 8.9.1.
- [x] `./gradlew assembleDebug` succeeds with no errors.
- [x] `./gradlew assembleRelease` succeeds with 0 errors (produces unsigned APK when secrets are absent).
- [x] `./gradlew bundleRelease` succeeds with 0 errors (produces valid AAB with `classes.dex` and `AndroidManifest.xml`).

## Testing & Verification
- [x] `./gradlew test` passes — 167 JVM Unit Tests PASS (100% in CI Runs 34701084546, 34701773741, 34726944722, 34729172347).
- [x] `./gradlew connectedAndroidTest` passes — 39/39 unfiltered connected tests PASS on Android 14 (API 34) emulator in CI (Runs 34701084546, 34726944722, 34729172347).
- [x] Android Lint passes — 0 errors, 0 code/resource warnings (CI Runs 34701084546, 34726944722, 34729172347).
- [ ] Manual testing on physical device is complete — BLOCKED (0 connected hardware devices via ADB).

## Functional Review
- [x] Exercise Library displays and functions correctly with category filters, search, and ExoPlayer video/animation player toggle (`ExerciseVideoPlayer.kt`).
- [x] Workout Session logging works (atomic set insertion, reps, weight, RPE, rest timer).
- [x] Rest Timer functions correctly with pure Kotlin state machine and `specialUse` FGS.
- [x] Camera Preview launches, binds to lifecycle, and includes accessible back navigation.
- [x] Form Analysis displays real-time feedback with MediaPipe Pose Landmarker.
- [x] Progress Dashboard displays accurate strength, volume, and body measurement charts (truthful metrics, zero arbitrary calorie multipliers).
- [x] Workout History detail supports in-place note editing via Room `updateWorkout` dialog, cleanly isolated from `performAgain` workout duplication.
- [x] Navigation works between all 12 core screens without dead-ends, with `Routes.WORKOUT_LEGACY` defensive alias.
- [x] Source hygiene verified (0 duplicate imports, 0 TODOs/FIXMEs, 0 debug logs).
- [x] Backup and data extraction rules aligned with `gymcoach.db`.

## Release Readiness
- [x] `app/build.gradle.kts` versionCode (2) and versionName ("1.0.0") configured for release candidate.
- [x] ProGuard/R8 rules configured in `proguard-rules.pro` with clean minification.
- [ ] Release build signed with production Keystore — BLOCKED (Production secrets not provisioned in GitHub Secrets).
- [x] Release APK generated (`gymcoach-release-unsigned-apk` / `app-release-unsigned.apk`).
- [x] Release AAB generated (`gymcoach-release-unsigned-aab` / `app-release.aab`).
- [ ] Google Play Console FGS `specialUse` declaration submitted & approved — SUBMISSION-READY DOSSIER PREPARED.
