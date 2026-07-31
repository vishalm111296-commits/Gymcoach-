# BUILD_GUIDE.md

## Opening the Project
1. Open Android Studio (Ladybug or later recommended).
2. Select "Open" and navigate to the `GymCoach` project root.
3. Android Studio will automatically recognize the project structure.

## Sync Gradle
1. After opening, Android Studio will prompt to "Sync Project with Gradle Files".
2. If it does not, click the "Sync Project with Gradle Files" button (elephant icon) in the top right toolbar.

## Build Debug
1. Select "Build" -> "Build Bundle(s) / APK(s)" -> "Build APK(s)".
2. The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Build Release
1. Ensure your `signingConfigs` are configured in `app/build.gradle.kts`.
2. Select "Build" -> "Generate Signed Bundle / APK".
3. Follow the wizard to sign your application.

## Run on Physical Device
1. Enable "Developer Options" and "USB Debugging" on your Android device.
2. Connect device via USB.
3. Select your device in the Android Studio device dropdown.
4. Click the "Run" (green arrow) button.

## Execute Tests
1. Right-click the `app/src/test` directory in the Project view.
2. Select "Run 'Tests in 'app''".
3. For UI tests, right-click `app/src/androidTest` and run.

## Generate Artifacts
1. **APK**: "Build" -> "Build Bundle(s) / APK(s)" -> "Build APK(s)".
2. **AAB**: "Build" -> "Build Bundle(s) / APK(s)" -> "Build Bundle(s)".
