# GymCoach Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete GymCoach as a production-ready Android fitness application with real persistence, business logic, navigation, workout logging, progression, analytics, camera form analysis, and all critical user journeys verified.

**Architecture:** Clean Architecture + MVVM with Hilt DI, Room persistence (v10), Jetpack Compose UI, CameraX + MediaPipe for form analysis. Build on existing v10 database with complete migration chain. Complete missing features: exercise seeding (200+), camera frame wiring, workout history, UI tests, security hardening, release config.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Hilt, Room, CameraX, MediaPipe Tasks Vision, Media3 ExoPlayer, Coroutines/Flow, Gradle Kotlin DSL, Min SDK 26, Target SDK 34.

**Spec:** docs/GYMCOACH_MASTER_SPEC.md, docs/MASTER_COMPLETION_SPEC.md

## Global Constraints

- Database version: 10 (current @Database version), exportSchema = true, fallbackToDestructiveMigration = false
- Migration chain: MIGRATION_1_2 → MIGRATION_2_3 → MIGRATION_3_4 → MIGRATION_4_5 → MIGRATION_5_6 → MIGRATION_6_7 → MIGRATION_7_8 → MIGRATION_8_9 → MIGRATION_9_10 (all registered)
- Schema files: Must generate v8.json, v9.json, v10.json in app/schemas/
- All metrics mathematically proven from actual workout data — NO fake data
- Recovery guidance uses conservative language only
- Evidence-based programming per ACSM 2026
- Version: 1.0.0, versionCode: 1 for release
- Min SDK 26, Target SDK 34, Compile SDK 34, JVM target 17

---

### Task 1: Generate Missing Room Schema Files (v8, v9, v10)

**Files:**
- Create: `app/schemas/com.gymcoach.app.data.local.database.GymCoachDatabase/8.json`
- Create: `app/schemas/com.gymcoach.app.data.local.database.GymCoachDatabase/9.json`
- Create: `app/schemas/com.gymcoach.app.data.local.database.GymCoachDatabase/10.json`

**Interfaces:**
- Consumes: GymCoachDatabase.kt @Database(version = 10, exportSchema = true)
- Produces: Schema JSON files for MigrationTestHelper validation

- [ ] **Step 1: Build project to generate schemas**

```bash
./gradlew :app:kspDebugKotlin --stacktrace
```

Expected: Schema files generated at app/schemas/.../8.json, 9.json, 10.json

- [ ] **Step 2: Verify schema files exist and are valid JSON**

```bash
cat app/schemas/com.gymcoach.app.data.local.database.GymCoachDatabase/8.json | jq .
cat app/schemas/com.gymcoach.app.data.local.database.GymCoachDatabase/9.json | jq .
cat app/schemas/com.gymcoach.app.data.local.database.GymCoachDatabase/10.json | jq .
```

Expected: Valid JSON with formatVersion 1, database version matching, entities array

- [ ] **Step 3: Commit schema files**

```bash
git add app/schemas/com.gymcoach.app.data.local.database.GymCoachDatabase/8.json \
        app/schemas/com.gymcoach.app.data.local.database.GymCoachDatabase/9.json \
        app/schemas/com.gymcoach.app.data.local.database.GymCoachDatabase/10.json
git commit -m "chore: add Room schema exports v8, v9, v10 for migration testing"
```

---

### Task 2: Fix and Run Migration Tests for Full Chain (1→10)

**Files:**
- Modify: `app/src/androidTest/java/com/gymcoach/app/data/local/database/RoomMigrationTest.kt`
- Test: `app/src/androidTest/java/com/gymcoach/app/data/local/database/RoomMigrationTest.kt`

**Interfaces:**
- Consumes: All 9 migration objects from GymCoachDatabase.kt, schema files 1.json-10.json
- Produces: Passing migration tests for each step

- [ ] **Step 1: Write failing test for migration 1→2**

```kotlin
@Test
fun testMigration1to2() {
    val db = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
        GymCoachDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory())
    db.createDatabase("test_db", 1).close()
    val migratedDb = db.runMigrationsAndValidate("test_db", 2, true, MIGRATION_1_2)
    assertNotNull(migratedDb)
}
```

- [ ] **Step 2: Run test to verify it fails (no schema v1 for helper)**

Run: `./gradlew :app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.gymcoach.app.data.local.database.RoomMigrationTest#testMigration1to2 --stacktrace`

Expected: FAIL - schema 1.json exists but migration may not preserve data correctly

- [ ] **Step 3: Add tests for migrations 2→3, 3→4, 4→5, 5→6, 6→7, 7→8, 8→9, 9→10**

```kotlin
@Test
fun testMigration2to3() { /* similar pattern with MIGRATION_2_3 */ }
@Test
fun testMigration3to4() { /* similar pattern with MIGRATION_3_4 */ }
@Test
fun testMigration4to5() { /* similar pattern with MIGRATION_4_5 */ }
@Test
fun testMigration5to6() { /* similar pattern with MIGRATION_5_6 */ }
@Test
fun testMigration6to7() { /* similar pattern with MIGRATION_6_7 */ }
@Test
fun testMigration7to8() { /* similar pattern with MIGRATION_7_8 */ }
@Test
fun testMigration8to9() { /* similar pattern with MIGRATION_8_9 */ }
@Test
fun testMigration9to10() { /* similar pattern with MIGRATION_9_10 */ }
```

- [ ] **Step 4: Run all migration tests**

Run: `./gradlew :app:connectedAndroidTest --stacktrace`

Expected: All 9 migration tests PASS

- [ ] **Step 5: Commit test updates**

```bash
git add app/src/androidTest/java/com/gymcoach/app/data/local/database/RoomMigrationTest.kt
git commit -m "test: add migration tests for full 1→10 chain"
```

---

### Task 3: Expand Exercise Seed Data to 200+ Exercises

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/core/exercise/ExerciseSeeder.kt`
- Create: `app/src/main/assets/exercises/v1_exercises.json` (versioned seed data)
- Modify: `app/src/main/kotlin/com/gymcoach/app/data/local/database/GymCoachDatabase.kt` (seed version tracking)

**Interfaces:**
- Consumes: ExerciseEntity schema with all vtaper/detail fields
- Produces: 200+ exercises seeded on first launch with full metadata

- [ ] **Step 1: Create versioned JSON seed file with 200+ exercises**

```json
{
  "version": 1,
  "exercises": [
    {
      "name": "Bench Press",
      "description": "Lie on a bench and press the barbell from your chest to full arm extension.",
      "muscleGroup": "Chest",
      "equipment": "Barbell",
      "difficulty": "Intermediate",
      "secondaryMuscles": "Triceps, Shoulders",
      "instructions": "1. Lie flat on bench. 2. Grip bar slightly wider than shoulder width. 3. Lower bar to mid-chest. 4. Press bar up.",
      "tips": "Keep feet flat on floor.",
      "commonMistakes": "Bouncing bar off chest.",
      "safetyNotes": "Use a spotter.",
      "recommendedRepRange": "8-12",
      "recommendedRestTime": "90s",
      "estimatedCalories": 15,
      "category": "Powerlifting",
      "tags": "Push, Upper Body",
      "isFavorite": false,
      "lastViewed": 0,
      "vtaperLat": 0,
      "vtaperLateralDelt": 2,
      "vtaperUpperChest": 7,
      "vtaperRearDelt": 1,
      "movementPattern": "horizontal_push",
      "imageUrl": null,
      "videoUrl": null,
      "animationUrl": null,
      "setupInstructions": "Set bench flat. Load barbell. Set safety pins.",
      "executionInstructions": "Unrack bar. Lower to mid-chest with control. Press up explosively.",
      "breathingInstructions": "Inhale on descent, exhale on press.",
      "tempoGuidance": "2-0-1-0",
      "beginnerVariantId": null,
      "advancedVariantId": null
    }
    // ... 199 more exercises covering all muscle groups
  ]
}
```

- [ ] **Step 2: Update ExerciseSeeder.kt to read from JSON asset**

```kotlin
object ExerciseSeeder {
    private const val SEED_VERSION_KEY = "exercise_seed_version"
    private const val CURRENT_SEED_VERSION = 1
    
    fun seedIfNeeded(context: Context, db: SupportSQLiteDatabase) {
        val prefs = context.getSharedPreferences("gymcoach_seeder", Context.MODE_PRIVATE)
        val seededVersion = prefs.getInt(SEED_VERSION_KEY, 0)
        if (seededVersion >= CURRENT_SEED_VERSION) return
        
        val json = context.assets.open("exercises/v${CURRENT_SEED_VERSION}_exercises.json").readText()
        val seedData = parseSeedJson(json)
        insertExercises(db, seedData)
        
        prefs.edit().putInt(SEED_VERSION_KEY, CURRENT_SEED_VERSION).apply()
    }
}
```

- [ ] **Step 3: Call seeder from GymCoachDatabase.onCreate callback**

```kotlin
.addCallback(object : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        ExerciseSeeder.seedIfNeeded(context, db)
    }
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        ExerciseSeeder.seedIfNeeded(context, db)
    }
})
```

- [ ] **Step 4: Run app and verify 200+ exercises seeded**

Run: `./gradlew :app:assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk`

Verify: Exercise list shows 200+ exercises with all metadata fields populated

- [ ] **Step 5: Commit seed data and seeder updates**

```bash
git add app/src/main/assets/exercises/v1_exercises.json
git add app/src/main/kotlin/com/gymcoach/app/core/exercise/ExerciseSeeder.kt
git add app/src/main/kotlin/com/gymcoach/app/data/local/database/GymCoachDatabase.kt
git commit -m "feat: seed 200+ exercises with full vtaper/detail metadata from JSON asset"
```

---

### Task 4: Wire CameraX ImageAnalysis to FormAnalyzer

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/camera/CameraPreviewScreen.kt`
- Modify: `app/src/main/kotlin/com/gymcoach/app/core/ml/PoseDetector.kt`
- Modify: `app/src/main/kotlin/com/gymcoach/app/core/ml/FormAnalyzer.kt`

**Interfaces:**
- Consumes: CameraX ImageAnalysis use case, MediaPipe PoseDetector, FormAnalyzer
- Produces: Live pose detection results fed to FormAnalyzer, UI feedback via overlay

- [ ] **Step 1: Add ImageAnalysis use case to CameraPreviewScreen**

```kotlin
// In CameraPreviewScreen.kt, inside AndroidView interop
val imageAnalysis = ImageAnalysis.Builder()
    .setTargetRotation(viewFinder.display.rotation)
    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
    .build()
    .also { it.setAnalyzer(cameraExecutor, imageProxy -> {
        val bitmap = imageProxy.toBitmap() // Use BitmapUtils
        val pose = poseDetector.detect(bitmap)
        bitmap.recycle()
        imageProxy.close()
        if (pose != null) {
            val result = formAnalyzer.analyze(pose, System.currentTimeMillis())
            // Post result to ViewModel/Compose state
            uiState.value = uiState.value.copy(formResult = result)
        }
    }) }
cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
```

- [ ] **Step 2: Ensure PoseDetector uses MediaPipe Tasks Vision correctly**

```kotlin
// In PoseDetector.kt - verify against MediaPipe 0.10.9 API
private val poseLandmarker = PoseLandmarker.createFromOptions(
    context,
    PoseLandmarker.PoseLandmarkerOptions.builder()
        .setRunningMode(RunningMode.LIVE_STREAM)
        .setNumPoses(1)
        .setMinPoseDetectionConfidence(0.5f)
        .setMinPosePresenceConfidence(0.5f)
        .setMinTrackingConfidence(0.5f)
        .setResultListener { result, image, timestampMs ->
            // Callback with PoseLandmarkerResult
        }
        .build()
)
```

- [ ] **Step 3: Verify FormAnalyzer receives live frames and produces results**

Run: Manual device test - open camera screen, perform exercise, verify overlay shows feedback

Expected: Real-time rep counting, phase detection, form cues displayed

- [ ] **Step 4: Handle camera lifecycle (pause/resume/rotate)**

```kotlin
DisposableEffect(lifecycleOwner) {
    onDispose {
        imageAnalysis.clearAnalyzer()
        cameraProvider.unbindAll()
    }
}
```

- [ ] **Step 5: Commit camera integration**

```bash
git add app/src/main/kotlin/com/gymcoach/app/presentation/camera/CameraPreviewScreen.kt
git add app/src/main/kotlin/com/gymcoach/app/core/ml/PoseDetector.kt
git add app/src/main/kotlin/com/gymcoach/app/core/ml/FormAnalyzer.kt
git commit -m "feat: wire CameraX ImageAnalysis to MediaPipe FormAnalyzer for live form feedback"
```

---

### Task 5: Implement Complete Workout History with Filtering/Sorting

**Files:**
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/history/WorkoutHistoryScreen.kt`
- Modify: `app/src/main/kotlin/com/gymcoach/app/presentation/history/WorkoutHistoryViewModel.kt`
- Modify: `app/src/main/kotlin/com/gymcoach/app/data/local/dao/WorkoutDao.kt` (add query methods)

**Interfaces:**
- Consumes: WorkoutRepository, WorkoutDao with new query methods
- Produces: Filterable, sortable workout history list with search

- [ ] **Step 1: Add query methods to WorkoutDao**

```kotlin
@Query("""
    SELECT * FROM workouts 
    WHERE completed = 1 
    AND (:query IS NULL OR :query = '' OR 
         EXISTS (SELECT 1 FROM workout_exercises we 
                 JOIN exercises e ON we.exerciseId = e.id 
                 WHERE we.workoutId = workouts.id 
                 AND e.name LIKE '%' || :query || '%'))
    ORDER BY 
        CASE WHEN :sortBy = 'date_desc' THEN date END DESC,
        CASE WHEN :sortBy = 'date_asc' THEN date END ASC,
        CASE WHEN :sortBy = 'duration_desc' THEN duration END DESC,
        CASE WHEN :sortBy = 'volume_desc' THEN 
            (SELECT SUM(ws.weight * ws.reps) FROM workout_exercises we 
             JOIN workout_sets ws ON we.id = ws.workoutExerciseId 
             WHERE we.workoutId = workouts.id) END DESC
    LIMIT :limit OFFSET :offset
""")
fun searchWorkouts(query: String?, sortBy: String, limit: Int, offset: Int): Flow<List<WorkoutEntity>>

@Query("SELECT COUNT(*) FROM workouts WHERE completed = 1")
fun countCompletedWorkouts(): Flow<Int>
```

- [ ] **Step 2: Update WorkoutHistoryViewModel with filter/sort state**

```kotlin
data class HistoryUiState(
    val workouts: List<WorkoutWithStats> = emptyList(),
    val isLoading: Boolean = false,
    val query: String = "",
    val sortBy: SortOption = SortOption.DATE_DESC,
    val hasMore: Boolean = true,
    val error: String? = null
)

enum class SortOption { DATE_DESC, DATE_ASC, DURATION_DESC, VOLUME_DESC }
```

- [ ] **Step 3: Implement search, sort, pagination in ViewModel**

```kotlin
fun setQuery(query: String) { _uiState.update { it.copy(query = query, workouts = emptyList()) } }
fun setSortOption(option: SortOption) { _uiState.update { it.copy(sortBy = option, workouts = emptyList()) } }
fun loadMore() { /* append next page */ }
```

- [ ] **Step 4: Update WorkoutHistoryScreen with search bar, sort chips, pull-to-refresh**

```kotlin
// Add TopAppBar with search field
// Add FilterChip row for sort options
// Add pull-to-refresh modifier on LazyColumn
// Show empty state when no workouts match
```

- [ ] **Step 5: Run and verify history screen works end-to-end**

Run: Manual test - complete workouts, verify they appear, test search/sort/pagination

Expected: Smooth scrolling, search filters by exercise name, sort changes order, pagination loads more

- [ ] **Step 6: Commit history implementation**

```bash
git add app/src/main/kotlin/com/gymcoach/app/presentation/history/
git add app/src/main/kotlin/com/gymcoach/app/data/local/dao/WorkoutDao.kt
git commit -m "feat: complete workout history with search, sort, pagination"
```

---

### Task 6: Add Compose UI Tests for Critical User Journeys

**Files:**
- Create: `app/src/androidTest/kotlin/com/gymcoach/app/OnboardingFlowTest.kt`
- Create: `app/src/androidTest/kotlin/com/gymcoach/app/WorkoutFlowTest.kt`
- Create: `app/src/androidTest/kotlin/com/gymcoach/app/NavigationTest.kt`

**Interfaces:**
- Consumes: Compose UI test rules, Hilt test setup
- Produces: Passing UI tests for JOURNEY A, B, C

- [ ] **Step 1: Set up Hilt test runner and Compose test rule**

```kotlin
@HiltAndroidTest
class OnboardingFlowTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()
    
    @Before fun setup() {
        hiltRule.inject()
    }
}
```

- [ ] **Step 2: Test JOURNEY A - Fresh install → onboarding → generate program → complete workout → verify history**

```kotlin
@Test
fun testFullOnboardingToWorkoutJourney() {
    composeRule.setContent { GymCoachNavHost(navController) }
    
    // Verify onboarding screen shown
    composeRule.onNodeWithText("BUILD YOUR V-TAPER").assertExists()
    
    // Complete all 6 steps
    composeRule.onNodeWithText("V-Taper Hypertrophy").performClick()
    composeRule.onNodeWithText("CONTINUE").performClick()
    composeRule.onNodeWithText("Intermediate").performClick()
    composeRule.onNodeWithText("CONTINUE").performClick()
    // ... age, height, weight
    composeRule.onNodeWithText("4").performClick() // days
    composeRule.onNodeWithText("CONTINUE").performClick()
    // ... equipment
    composeRule.onNodeWithText("GENERATE PROGRAM").performClick()
    
    // Verify home dashboard shows today's workout
    composeRule.onNodeWithText("Training Session").assertExists()
    
    // Start workout
    composeRule.onNodeWithText("START WORKOUT").performClick()
    
    // Add exercise, log sets, complete
    composeRule.onNodeWithText("Add Exercise").performClick()
    composeRule.onNodeWithText("Bench Press").performClick()
    composeRule.onNodeWithText("Add Set").performClick()
    // ... fill set data
    composeRule.onNodeWithText("Complete Workout").performClick()
    composeRule.onNodeWithText("Finish").performClick()
    
    // Verify history
    navigateToHistory()
    composeRule.onNodeWithText("Bench Press").assertExists()
}
```

- [ ] **Step 3: Test JOURNEY B - Start workout → kill app → reopen → resume**

```kotlin
@Test
fun testWorkoutResumeAfterProcessDeath() {
    // Start workout, add exercise/set
    // Simulate process death: composeRule.activity.recreate()
    // Verify workout data persisted and can be resumed
}
```

- [ ] **Step 4: Test JOURNEY C - Record measurement → restart → verify persistence**

```kotlin
@Test
fun testMeasurementPersistence() {
    // Navigate to profile, record body measurement
    // Kill and restart app
    // Verify measurement still visible
}
```

- [ ] **Step 5: Run UI tests**

Run: `./gradlew :app:connectedAndroidTest --stacktrace`

Expected: All UI tests PASS

- [ ] **Step 6: Commit UI tests**

```bash
git add app/src/androidTest/kotlin/com/gymcoach/app/*Test.kt
git commit -m "test: add Compose UI tests for critical user journeys A, B, C"
```

---

### Task 7: Configure Release Signing and Security Hardening

**Files:**
- Modify: `app/build.gradle.kts` (signing config, proguard)
- Create: `keystore.properties` (not committed - local only)
- Modify: `app/src/main/AndroidManifest.xml` (security flags)
- Modify: `app/proguard-rules.pro`

**Interfaces:**
- Consumes: Release keystore, Google Play Console requirements
- Produces: Signed release AAB, hardened manifest

- [ ] **Step 1: Add signing config to app/build.gradle.kts**

```kotlin
android {
    signingConfigs {
        create("release") {
            val keystoreProperties = Properties().apply { load(FileInputStream("keystore.properties")) }
            storeFile = File(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

- [ ] **Step 2: Add proguard rules for Hilt, Room, MediaPipe**

```proguard
# proguard-rules.pro
-keep class dagger.hilt.** { *; }
-keep class androidx.room.** { *; }
-keep class com.google.mediapipe.** { *; }
-keepclassmembers class * { @androidx.room.Entity *; }
-keepclassmembers class * { @androidx.room.Dao *; }
-keep class * implements androidx.lifecycle.ViewModel { *; }
```

- [ ] **Step 3: Harden AndroidManifest.xml**

```xml
<application
    android:allowBackup="false"
    android:fullBackupContent="@xml/backup_rules"
    android:networkSecurityConfig="@xml/network_security_config"
    android:usesCleartextTraffic="false"
    ... >
```

- [ ] **Step 4: Create network_security_config.xml and backup_rules.xml**

```xml
<!-- res/xml/network_security_config.xml -->
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>

<!-- res/xml/backup_rules.xml -->
<full-backup-content>
    <exclude domain="sharedpref" path="gymcoach_prefs.xml"/>
    <exclude domain="database" path="gymcoach.db"/>
    <exclude domain="database" path="gymcoach.db-shm"/>
    <exclude domain="database" path="gymcoach.db-wal"/>
</full-backup-content>
```

- [ ] **Step 5: Build release AAB and verify**

Run: `./gradlew :app:bundleRelease --stacktrace`

Expected: Signed AAB at app/build/outputs/bundle/release/app-release.aab

- [ ] **Step 6: Commit security config**

```bash
git add app/build.gradle.kts
git add app/proguard-rules.pro
git add app/src/main/AndroidManifest.xml
git add app/src/main/res/xml/network_security_config.xml
git add app/src/main/res/xml/backup_rules.xml
git commit -m "security: configure release signing, proguard, network security, backup rules"
```

---

### Task 8: Run Full Test Suite and Verify All User Journeys

**Files:**
- Test: All existing test files

**Interfaces:**
- Consumes: Complete test suite (unit, integration, instrumented, UI)
- Produces: Verified PASS for all tests

- [ ] **Step 1: Run unit tests**

Run: `./gradlew :app:testDebugUnitTest --stacktrace`

Expected: All unit tests PASS (RestTimerManager, PRDetector, ProgramGenerator, VolumeCalculator, WorkoutPersistence, ExerciseRepository, ForensicAuditRegression)

- [ ] **Step 2: Run instrumented tests (migrations, database)**

Run: `./gradlew :app:connectedAndroidTest --stacktrace`

Expected: All migration tests PASS, database tests PASS

- [ ] **Step 3: Run UI tests**

Run: `./gradlew :app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.gymcoach.app.OnboardingFlowTest --stacktrace` (repeat for each test class)

Expected: All UI tests PASS for JOURNEY A, B, C

- [ ] **Step 4: Run lint**

Run: `./gradlew :app:lintDebug --stacktrace`

Expected: No errors, warnings acceptable but documented

- [ ] **Step 5: Build debug and release**

Run: `./gradlew :app:assembleDebug :app:bundleRelease --stacktrace`

Expected: Both build successfully, APK and AAB generated

- [ ] **Step 6: Manual device verification of 7 critical journeys**

Journey A: Fresh install → onboarding → program → workout → history
Journey B: Start workout → kill app → reopen → resume
Journey C: Record measurement → restart → verify
Journey D: Generate programs for 2-6 days → verify distribution
Journey E: Existing DB → migration → verify data preserved
Journey F: Browse exercise → substitute → complete workout
Journey G: Camera exercise → permissions → lifecycle → inference → exit → return

Document: PASS/FAIL for each with evidence

- [ ] **Step 7: Commit any test fixes**

```bash
git add -A
git commit -m "fix: resolve test failures from full suite run"
```

---

### Task 9: Final Certification and Production Readiness

**Files:**
- Create: `docs/qa/FINAL_CERTIFICATION_REPORT.md`

**Interfaces:**
- Consumes: All verification evidence from Tasks 1-8
- Produces: Final certification report with PRODUCTION-READY or NOT PRODUCTION-READY status

- [ ] **Step 1: Compile verification evidence**

| Gate | Status | Evidence |
|------|--------|----------|
| Build PASS | | Gradle output |
| Lint PASS | | Lint report |
| Unit tests PASS | | Test report |
| Instrumented tests PASS | | Test report |
| UI tests PASS | | Test report |
| Migration tests PASS | | Test report |
| Data preservation PASS | | Migration test + manual |
| Navigation PASS | | UI test + manual |
| Core journeys PASS | | Manual verification logs |
| Security review PASS | | Security audit checklist |
| Release config verified | | bundleRelease output |
| Independent review PASS | | Code review records |
| Adversarial review PASS | | Skeptic findings + fixes |

- [ ] **Step 2: Document known risks and remaining gaps**

```markdown
## Known Risks
1. MediaPipe pose detection accuracy in low light - mitigated by flash assist
2. Camera permission handling on Android 13+ - tested on API 33, 34
3. Room migration 1→2 has minimal test coverage - schema v1 is basic
```

- [ ] **Step 3: Issue final status**

If all gates PASS: **PRODUCTION-READY**
If any gate FAIL: **NOT PRODUCTION-READY** with exact gaps listed

- [ ] **Step 4: Commit certification report**

```bash
git add docs/qa/FINAL_CERTIFICATION_REPORT.md
git commit -m "docs: final certification report - PRODUCTION-READY"
```

---

## Execution Order

1. Task 1 (schemas) → Task 2 (migration tests) — can run in parallel after schemas generated
2. Task 3 (exercise seeding) — independent, can run in parallel
3. Task 4 (camera wiring) — depends on MediaPipe research
4. Task 5 (workout history) — independent
5. Task 6 (UI tests) — depends on Tasks 1-5 working
6. Task 7 (release/security) — independent, can run in parallel
7. Task 8 (full test run) — depends on Tasks 1-7
8. Task 9 (certification) — depends on Task 8

---

## Success Criteria

- [ ] All 9 migration tests PASS
- [ ] Schema files v8, v9, v10 exist and valid
- [ ] 200+ exercises seeded with full metadata
- [ ] Camera form analysis works live on device
- [ ] Workout history has search, sort, pagination
- [ ] UI tests PASS for JOURNEY A, B, C
- [ ] Release AAB builds and signs correctly
- [ ] Security config: no cleartext, no backup, proguard enabled
- [ ] All 7 critical user journeys verified PASS
- [ ] Final certification: PRODUCTION-READY