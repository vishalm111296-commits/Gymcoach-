# RELEASE_CHECKLIST.md

## Prerequisites
- [ ] Java 17+ is installed and JAVA_HOME is configured.
- [ ] Android SDK API 34 is installed.
- [ ] Android SDK Build-tools are installed.
- [ ] Gradle wrapper is executable (`chmod +x gradlew`).

## Build Verification
- [ ] Gradle sync is successful.
- [ ] `./gradlew assembleDebug` succeeds with no errors.
- [ ] `./gradlew assembleRelease` succeeds with no errors.

## Testing
- [ ] `./gradlew test` passes (Unit Tests).
- [ ] `./gradlew connectedAndroidTest` passes (UI Tests).
- [ ] Manual testing on physical device is complete.

## Functional Review
- [ ] Exercise Library displays and functions correctly.
- [ ] Workout Session logging works (add exercises, sets, reps, weight).
- [ ] Rest Timer functions and integrates correctly.
- [ ] Camera Preview launches and shows camera feed.
- [ ] Form Analysis displays real-time feedback.
- [ ] Progress Dashboard displays accurate statistics.
- [ ] Navigation works between all screens.

## Release
- [ ] `app/build.gradle.kts` versionCode and versionName updated.
- [ ] ProGuard/R8 rules configured in `proguard-rules.pro`.
- [ ] Release build signed with production Keystore.
- [ ] Release APK generated (`app-release.apk`).
- [ ] Release AAB generated (`app-release.aab`).
