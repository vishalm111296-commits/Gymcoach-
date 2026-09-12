# Google Play Console — Foreground Service (specialUse) Declaration Dossier

**Target Package**: `com.gymcoach.app`  
**Target Service**: `com.gymcoach.app.core.notification.RestTimerNotificationService`  
**Permission Declared**: `android.permission.FOREGROUND_SERVICE_SPECIAL_USE`  
**Foreground Service Type**: `specialUse`  
**Subtype Manifest Property**: `android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE`  
**Subtype Declared Value**: `Workout rest interval countdown during active exercise sessions`  
**Target SDK**: 36 (Android 16) | **Min SDK**: 26 (Android 8.0)  
**Document Purpose**: Production release submission package to fulfill and unblock Google Play Console Gate 8.

---

## 1. Google Play Console Declaration Form Responses

When submitting a release targeting Android 14+ (API 34+) to Google Play Console, policy requires completing the **App Content → Foreground Services Declaration** questionnaire. The following responses must be submitted:

### Question 1: Select the foreground service type(s) your app uses
- **Selection**: `specialUse` (Special Use)

### Question 2: Provide the subtype string declared in your AndroidManifest.xml
- **Declared Subtype Value**:
  ```text
  Workout rest interval countdown during active exercise sessions
  ```

### Question 3: Describe the user-facing feature that requires this foreground service
- **Submission Text**:
  > GymCoach is an offline fitness and strength training tracker. During active workout sessions, athletes track prescribed rest intervals between sets (typically 60 to 180 seconds). The Rest Timer foreground service maintains an active, low-latency countdown stopwatch visible in the Android system notification shade and on the lock screen while the device screen is off or while the user is away from the app. It provides real-time countdown progress and interactive notification action buttons ("+15s", "-15s", "Pause", "Resume", "Skip") allowing athletes with chalk-covered or sweaty hands to adjust their rest interval directly from the lock screen without unlocking the device or re-opening the application. Upon timer completion, the service triggers a brief completion haptic pattern and immediately terminates itself.

### Question 4: Explain why this feature cannot use alternative background execution APIs (e.g., WorkManager, JobScheduler, or AlarmManager)
- **Submission Text**:
  > Alternative background APIs cannot meet the real-time, interactive, and glanceable requirements of an active athletic rest interval:
  > 1. **WorkManager & JobScheduler**: WorkManager is designed for deferrable, batchable work and cannot guarantee sub-second execution intervals or maintain an active, real-time chronometer countdown in the lock screen notification shade. Furthermore, system battery optimizations and Doze mode defer WorkManager tasks, causing rest intervals to drift by minutes.
  > 2. **AlarmManager**: While exact alarms can trigger at the end of an interval, they cannot provide a persistent, continuous 1-second countdown display or support responsive lock screen action controls (+15s / -15s / Pause / Resume) during the rest interval.
  > 3. **Interactive Control & Safety**: The athlete requires continuous glanceability of remaining rest time and one-tap adjustability. A foreground service with `specialUse` is the only Android architecture that reliably maintains active notification countdown state and immediate action broadcast processing across screen-off transitions during a physical training session.

### Question 5: Describe the user initiation and termination lifecycle
- **Submission Text**:
  > - **Initiation**: The service is initiated ONLY upon explicit user action: when an athlete checks off a completed set in the active workout logger.
  > - **Termination**: The service is strictly bounded. It self-terminates immediately when the rest countdown reaches zero, or when the user taps "Skip" or "Cancel" either in the notification shade or inside the app. When terminated, the service immediately calls `stopForeground(STOP_FOREGROUND_REMOVE)` and `stopSelf()`, ensuring zero ongoing battery consumption.

---

## 2. Reviewer Video Demonstration Script & Storyboard

Google Play policy requires a video demonstration showcasing the user-facing functionality and the foreground service notification.

### Video Submission Requirements
- **Duration**: 30 – 60 seconds
- **Format**: MP4 / WebM (1080p recommended)
- **Audio/Captions**: Clear demonstration showing on-screen taps and system notification shade

### Step-by-Step Recording Script
1. **Launch App & Start Workout (0:00 – 0:10)**:
   - Open GymCoach from launcher.
   - Tap "Start Workout" or select a workout session from the dashboard.
2. **Log a Set to Trigger Rest Timer (0:10 – 0:20)**:
   - Enter reps and weight for Set 1.
   - Tap the checkbox to complete the set.
   - Observe the Rest Timer starting (e.g., 90-second countdown overlay appears).
3. **Background App & Lock Screen Notification (0:20 – 0:35)**:
   - Press the Home button to background the application.
   - Pull down the Android notification shade.
   - Show the persistent notification:
     - Title: `Rest Timer`
     - Text: `REST 01:25 — Next: Set 2`
     - Action buttons: `Pause`, `+15s`, `-15s`, `Skip`
4. **Demonstrate Notification Interaction (0:35 – 0:45)**:
   - Tap `+15s` in the notification: show remaining time increases to `01:40`.
   - Tap `Pause`: show timer pauses with text `REST 01:40 (PAUSED)` and action changes to `Resume`.
   - Tap `Resume`: show countdown resumes.
5. **Demonstrate Completion & Auto-Termination (0:45 – 0:55)**:
   - Tap `Skip` (or let countdown reach `00:00`).
   - Show notification immediately dismissing from notification shade.
   - Return to app: show active session resumed and ready for Set 2.

---

## 3. Manifest & Code Implementation Audit

### AndroidManifest.xml Declaration
```xml
<!-- Permission -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Service Declaration -->
<service
    android:name=".core.notification.RestTimerNotificationService"
    android:foregroundServiceType="specialUse"
    android:exported="false">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="Workout rest interval countdown during active exercise sessions" />
</service>
```

### Runtime Promotion & Termination (`RestTimerNotificationService.kt`)
```kotlin
// Runtime promotion with official Android 14+ type
private fun promoteToForeground() {
    val notification = buildNotification()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    } else {
        startForeground(NOTIFICATION_ID, notification)
    }
}

// Immediate termination on completion / skip / cancel
private fun finishTimer(isCompleted: Boolean = false) {
    if (isCompleted) {
        triggerCompletionHaptics()
        stateMachine.complete()
    } else {
        stateMachine.cancel()
    }
    timer?.cancel()
    timer = null
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
}
```

---

## 4. Play Console Operator Action Items

| Step | Action Item | Owner | Gate Status |
| :---: | :--- | :---: | :---: |
| 1 | Navigate to **Google Play Console → App content → Sensitive app permissions → Foreground service permissions**. | Release Engineer | Ready |
| 2 | Select **specialUse** and copy-paste the exact responses from Section 1 above. | Release Engineer | Ready |
| 3 | Record the demonstration video following the script in Section 2 using a screen recorder (e.g. `adb shell screenrecord`). | QA / Release Engineer | Ready |
| 4 | Upload the video to YouTube (Unlisted) or Google Drive (public view link) and paste the URL into the declaration form. | Release Engineer | Ready |
| 5 | Submit for review alongside release bundle `app-release.aab`. | Release Engineer | Pending Submission |
