# CI Workflow Fix — F-CI-1

The `.github/workflows/android-build.yml` file cannot be modified via the GitHub
MCP API because it requires the `workflows` OAuth scope.

## What needs to change

Replace the entire content of `.github/workflows/android-build.yml` with the
version in `docs/CI_WORKFLOW_FIX_F-CI-1.md`, which adds:

1. `release-build-verify` job — runs `bundleRelease` with a throwaway CI keystore
   to catch R8 shrinkage failures and ProGuard rule gaps on every PR
2. Proper artifact uploads for lint and unit-test reports
3. Removes `ANDROID_NDK_VERSION` env var (NDK no longer needed after F-BUILD-1)
4. Renames `test` job to `unit-tests`
5. Gates `create-release` on `build + unit-tests + release-build-verify`

## How to apply

```bash
git checkout main
cp docs/CI_WORKFLOW_FIX_F-CI-1.md .github/workflows/android-build.yml  # then review and edit
# or use GitHub UI: navigate to .github/workflows/android-build.yml -> Edit -> paste content
git add .github/workflows/android-build.yml
git commit -m "ci(android): F-CI-1 add release-build-verify job"
git push
```

## Full replacement content

```yaml
name: Android CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]
  workflow_dispatch:

env:
  JAVA_VERSION: "17"
  JAVA_DISTRIBUTION: "temurin"
  ANDROID_SDK_VERSION: "34"
  ANDROID_BUILD_TOOLS_VERSION: "34.0.0"

jobs:
  build:
    runs-on: ubuntu-latest
    name: Build Debug APK
    steps:
      - uses: actions/checkout@v4
        with: { fetch-depth: 0 }
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 17, cache: gradle }
      - uses: android-actions/setup-android@v2
      - run: yes | sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" && yes | sdkmanager --licenses
      - run: chmod +x ./gradlew && ./gradlew assembleDebug --stacktrace
      - run: ls -lh ./app/build/outputs/apk/debug/app-debug.apk
      - uses: actions/upload-artifact@v4
        with: { name: gymcoach-debug-apk, path: ./app/build/outputs/apk/debug/, retention-days: 7, if-no-files-found: error }

  android-lint:
    runs-on: ubuntu-latest
    name: Android Lint
    needs: build
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 17, cache: gradle }
      - uses: android-actions/setup-android@v2
      - run: chmod +x ./gradlew && ./gradlew lintDebug --stacktrace
      - if: always()
        uses: actions/upload-artifact@v4
        with: { name: lint-report, path: ./app/build/reports/lint-results/, if-no-files-found: ignore, retention-days: 7 }

  unit-tests:
    runs-on: ubuntu-latest
    name: Unit Tests
    needs: build
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 17, cache: gradle }
      - uses: android-actions/setup-android@v2
      - run: chmod +x ./gradlew && ./gradlew testDebugUnitTest --stacktrace
      - if: always()
        uses: actions/upload-artifact@v4
        with: { name: unit-test-report, path: ./app/build/reports/tests/, if-no-files-found: ignore, retention-days: 7 }

  release-build-verify:
    runs-on: ubuntu-latest
    name: Release Build Verify (R8)
    needs: build
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 17, cache: gradle }
      - uses: android-actions/setup-android@v2
      - run: yes | sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" && yes | sdkmanager --licenses
      - name: Create CI keystore
        run: |
          mkdir -p keystore
          keytool -genkeypair -keystore keystore/release.jks -alias gymcoach \
            -keyalg RSA -keysize 2048 -validity 1 \
            -storepass ci_store_pass -keypass ci_key_pass \
            -dname "CN=CI,O=CI,C=US" 2>/dev/null || true
          echo "KEYSTORE_PATH=keystore/release.jks" >> $GITHUB_ENV
          echo "KEYSTORE_PASSWORD=ci_store_pass" >> $GITHUB_ENV
          echo "KEY_ALIAS=gymcoach" >> $GITHUB_ENV
          echo "KEY_PASSWORD=ci_key_pass" >> $GITHUB_ENV
      - run: chmod +x ./gradlew && ./gradlew bundleRelease --stacktrace
      - run: ls -lh ./app/build/outputs/bundle/release/app-release.aab
      - uses: actions/upload-artifact@v4
        with: { name: gymcoach-release-aab, path: ./app/build/outputs/bundle/release/, retention-days: 7, if-no-files-found: error }

  create-release:
    runs-on: ubuntu-latest
    name: Create GitHub Release (tag-triggered)
    needs: [ build, unit-tests, release-build-verify ]
    if: startsWith(github.ref, 'refs/tags/v')
    steps:
      - uses: actions/checkout@v4
      - uses: actions/download-artifact@v4
        with: { name: gymcoach-debug-apk, path: ./build/artifacts }
      - uses: softprops/action-gh-release@v2
        with: { files: ./build/artifacts/app-debug.apk, generate_release_notes: true }
```
