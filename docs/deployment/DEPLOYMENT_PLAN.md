# Deployment and Release Plan

The configuration, signing protocols, and build pipelines required to package and publish the GymCoach Android Application.

---

## 1. Google Play Console Setup & Android App Bundle (AAB)

To optimize download size and handle dynamic delivery, GymCoach is distributed exclusively as an Android App Bundle (`.aab`).

### AAB Structure & Asset Optimization
* Use Gradle's `bundle` tasks (`:app:bundleRelease`) to generate the package.
* Resources are optimized via `minifyEnabled = true` (R8 shrinking) and `shrinkResources = true` in `build.gradle.kts` to prune unused layouts, drawables, and strings.

---

## 2. Keystore Signing & Credential Storage

For release builds, the package must be signed using a secure upload keystore.

### Keystore Configuration
1. **Keystore Generation**:
   Generates a secure PKCS12 keystore file (`release.jks`):
   ```bash
   keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias gymcoach-key
   ```
2. **Environment Variables**:
   Credentials must never be hardcoded in `build.gradle.kts`. Use environment variables injected at build time (locally via `local.properties` or on CI via GitHub Actions secrets):
   * `KEYSTORE_PASSWORD`: Password for the keystore file.
   * `KEY_ALIAS`: Alias name (`gymcoach-key`).
   * `KEY_PASSWORD`: Password for the specific key alias.

3. **Gradle Integration**:
   ```kotlin
   signingConfigs {
       create("release") {
           storeFile = file(System.getenv("ANDROID_KEYSTORE_PATH") ?: "release.jks")
           storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
           keyAlias = System.getenv("ANDROID_KEY_ALIAS")
           keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
       }
   }
   buildTypes {
       release {
           signingConfig = signingConfigs.getByName("release")
           isMinifyEnabled = true
           isShrinkResources = true
           proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
       }
   }
   ```

---

## 3. Pre-flight Verification Checklist

Prior to promoting any build to the Google Play Store (Internal, Alpha, or Production track), execute this sequence:

- [ ] **Version Codes**: Increment `versionCode` (integer) and `versionName` (semantic version, e.g. `1.0.0`) in `app/build.gradle.kts`.
- [ ] **Pre-flight Room Migrations**: Run migration test suites. Confirm that schema hash matches `GymCoachDatabase` schema exports.
- [ ] **ProGuard Rule Validation**: Generate a release build and execute a smoketest on a physical device. Verify that JSON serialization (Gson/KotlinX) and Room database accesses are not obfuscated/broken by R8.
- [ ] **Dependency Audit**: Scan dependencies for deprecated APIs and known security vulnerabilities.
- [ ] **Release Notes**: Update Google Play store listing changelog templates with localized release notes.
