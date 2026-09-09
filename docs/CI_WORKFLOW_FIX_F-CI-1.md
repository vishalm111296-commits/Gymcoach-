# CI Workflow Fix Required — F-CI-1

The `.github/workflows/android-build.yml` cannot be updated via the GitHub
MCP API because it requires the `workflows` OAuth scope.

## Manual step required

To complete F-CI-1, apply the following change to `.github/workflows/android-build.yml`:

### 1. Remove `ANDROID_NDK_VERSION` env var (no longer needed)

```yaml
# Remove this line from the env: block:
  ANDROID_NDK_VERSION: "21.4.7075529"
```

### 2. Rename the `test` job to `unit-tests`

Change `name: Unit Tests` and the job key `test:` to `unit-tests:`.

### 3. Add the release build verification job after `unit-tests:`

```yaml
  release-build-verify:
    runs-on: ubuntu-latest
    name: Release Build Verify (R8)
    needs: build

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          distribution: ${{ env.JAVA_DISTRIBUTION }}
          java-version: ${{ env.JAVA_VERSION }}
          cache: gradle

      - name: Setup Android SDK
        uses: android-actions/setup-android@v2

      - name: Install Android SDK packages
        run: |
          yes | sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
          yes | sdkmanager --licenses

      - name: Create CI keystore for R8 verification
        run: |
          mkdir -p keystore
          keytool -genkeypair \
            -keystore keystore/release.jks \
            -alias gymcoach \
            -keyalg RSA -keysize 2048 -validity 1 \
            -storepass ci_store_pass -keypass ci_key_pass \
            -dname "CN=CI, OU=CI, O=CI, L=CI, S=CI, C=US" 2>/dev/null || true
          echo "KEYSTORE_PATH=keystore/release.jks" >> $GITHUB_ENV
          echo "KEYSTORE_PASSWORD=ci_store_pass" >> $GITHUB_ENV
          echo "KEY_ALIAS=gymcoach" >> $GITHUB_ENV
          echo "KEY_PASSWORD=ci_key_pass" >> $GITHUB_ENV

      - name: Build Release AAB (R8 verification)
        run: |
          chmod +x ./gradlew
          ./gradlew bundleRelease --stacktrace

      - name: Verify Release AAB
        run: |
          ls -lh ./app/build/outputs/bundle/release/app-release.aab

      - name: Upload Release AAB
        uses: actions/upload-artifact@v4
        with:
          name: gymcoach-release-aab
          path: ./app/build/outputs/bundle/release/
          retention-days: 7
          if-no-files-found: error
```

### 4. Update `create-release` job dependencies

```yaml
  create-release:
    needs: [ build, unit-tests, release-build-verify ]
```

## Rationale

Without this job, R8 shrinkage failures and ProGuard rule gaps go undetected
until someone manually runs `bundleRelease`. The throwaway keystore means R8
can run in CI without the production keystore being present.
