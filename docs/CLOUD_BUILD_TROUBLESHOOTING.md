# Cloud Build Troubleshooting Guide

This guide covers common failures encountered when building GymCoach via GitHub Actions and how to fix them.

## 1. Build Fails (General Compilation Error)

**Symptom**: The "Build Debug APK" step fails with a red 'X'.
**Cause**: Usually a Kotlin syntax error, missing import, or unresolved reference.
**Fix**:
1. Check the logs for the exact file and line number.
2. Fix the code in the repository.
3. Commit and push the change to trigger a new build.

## 2. Gradle Fails (Configuration Error)

**Symptom**: The "Setup Gradle" or initial Gradle execution fails.
**Cause**: Incorrect syntax in `build.gradle.kts`, `settings.gradle.kts`, or `libs.versions.toml`.
**Fix**:
1. Verify the Gradle file syntax (especially Kotlin DSL syntax).
2. Ensure plugin versions are compatible with the Gradle version (8.2.2/8.8).

## 3. Dependency Download Fails

**Symptom**: Logs show "Could not resolve..." or "Failed to fetch...".
**Cause**: Network issue reaching Maven Central/Google repository, or an incorrect version number in `libs.versions.toml`.
**Fix**:
1. If it's a network timeout, re-run the failed job.
2. Verify the dependency version exists in the respective repository.

## 4. MediaPipe / CameraX Dependency Fails

**Symptom**: Specific failure resolving MediaPipe or CameraX libraries.
**Cause**: Missing repository declaration (e.g., `google()`, `mavenCentral()`) in `settings.gradle.kts`.
**Fix**:
Ensure `settings.gradle.kts` has the correct repositories configured under `dependencyResolutionManagement`.

## 5. Out of Memory (OOM) Error

**Symptom**: Build process terminates unexpectedly, logs mention "Java heap space" or "GC overhead limit exceeded".
**Cause**: The Gradle daemon ran out of memory allocated to it by the GitHub Actions runner.
**Fix**:
Add or update `gradle.properties` in the repository root:
`org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8`

## 6. Workflow Timeout

**Symptom**: Workflow runs for hours and then gets cancelled.
**Cause**: A process (like a test or lint task) hung indefinitely.
**Fix**:
1. Add a `timeout-minutes: 30` to the build job in `.github/workflows/android-build.yml`.
2. Disable hanging tasks (e.g., skip tests if they are known to freeze).

## 7. Artifact Missing / APK Missing

**Symptom**: Workflow succeeds, but no artifact is available to download.
**Cause**: The `upload-artifact` action path doesn't match the actual APK output path.
**Fix**:
Verify the path in `.github/workflows/android-build.yml`:
`path: ./app/build/outputs/apk/debug/`

## 8. Permission Denied (gradlew)

**Symptom**: Logs show `/bin/sh: 1: ./gradlew: Permission denied`.
**Cause**: The `gradlew` file lost its executable permission when committed to Git.
**Fix**:
Run this command locally or via Termux before pushing:
`git update-index --chmod=+x gradlew`

## 9. Repository Upload Failed (Phone Only)

**Symptom**: Web browser crashes or times out while uploading files.
**Cause**: Uploading a full Android project via browser is resource-intensive.
**Fix**:
Use Termux and Git commands (see `docs/FIRST_BUILD.md`) instead of the browser upload.

## 10. GitHub Quota Exceeded

**Symptom**: Workflow fails to start, saying "Action minutes exceeded".
**Cause**: Used up the free tier of GitHub Actions minutes (2000 mins/month for private repos).
**Fix**:
1. Make the repository Public (free Actions for public repos).
2. Or wait until the next billing cycle.
3. Or purchase additional minutes.
