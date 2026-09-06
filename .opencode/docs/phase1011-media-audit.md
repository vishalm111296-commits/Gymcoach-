# Phase 10/11 Audit: Exercise Media + Camera + Offline Model Loading

- Branch: `phase5-recovery-verified`, HEAD `b5fa19c`
- Date: 2026-09-06
- Method: READ-ONLY forensic grep/file inspection (no files modified)
- Scope: Phase 10 (exercise media) + Phase 11 (camera/offline loading)

---

## Summary Verdict

| # | Item | Status | Severity |
|---|------|--------|----------|
| 1 | Exercise media URLs populated? | PASS (never populated — fields are null) | INFO |
| 2 | Media rendering: does anything actually render an image/video? | **PARTIAL** — no real media renders; a **Typography placeholder** ("Hero Image via Typography") is shown instead. Coil 2.5.0 declared but **never used**. | P2 (UX polish gap, not a fake-data lie) |
| 3 | Fake/placeholder URLs (e.g. example.com, exercise-specific dummy links) in seed/entity/code? | **PASS** — ZERO fake URLs found. 139/139 seed entries have no image/video/animation URL at all. | - |
| 4 | Coil/Glide network download for media URLs | **N/A** — Coil declared but unused; no AsyncImage anywhere; no media download. | - |
| 5 | Camera "photos for workout/progress photos" feature | **PASS — no such feature exists.** The only camera is a REAL CameraX form-analysis camera. No fake photo-attachment affordance anywhere. | - |
| 6 | Camera implementation real or fake? | **PASS — REAL CameraX** (PreviewView, ProcessCameraProvider, ImageAnalysis RGBA_8888, front lens). Wired: ExerciseList camera icon → ModalBottomSheet exercise picker → CameraPreviewScreen. | - |
| 7 | Offline model loading claims | **PARTIAL** — pose model is NOT bundled (download-on-first-launch via HTTPS from storage.googleapis.com). Offline first-launch = feature unavailable with a graceful `ModelErrorView` + Retry. Subsequent launches reuse cached file. Everything else (readiness, program generator, exercise DB) is genuinely local (Room/assets). | P2 (network dependency for camera feature; mitigated by error+retry UI) |
| 8 | Other network dependencies | **PASS — InterNET used ONLY by PoseDetector.** No OkHttp/Retrofit/ktor. network_security_config blocks cleartext. | - |
| 9 | Bundled media (drawable/raw/assets media) | **PASS — none claimed.** Only launcher icons exist. | - |

---

## Detail with File:Line Evidence

### 1. Exercise media fields are ALL null — no fake URLs (PASS, INFO)

The domain/entity architecture carries media fields:

- `app/src/main/kotlin/com/gymcoach/app/domain/model/Exercise.kt:30-32` — `imageUrl: String? = null`, `videoUrl: String? = null`, `animationUrl: String? = null`
- `app/src/main/kotlin/com/gymcoach/app/data/local/entity/ExerciseEntity.kt:35-37` — same nullable columns (`image_url`, `video_url`, `animation_url`)

**Seed data verification (all 139 exercise entries across 16 JSON files):**
```
total exercise entries: 139
NO media URLs in any seed JSON        ← image_url/video_url/animation_url absent from every entry
```
The asset JSONs contain fields such as `imageUrl`, `videoUrl`, `animationUrl` **only as absent keys** — verified programmatically (`app/src/main/assets/exercises/*.json`). Note: the Exercise `domain` model defines these as "media (nullable, architecture-ready)" — they are genuinely never populated, so **no fake/placeholder "https://example.com/..." URLs exist anywhere**. This is honest (no fabricated media), and the fields are an architecture placeholder only.

`ExerciseRepositoryImpl.kt:76-78, 116-118` and `WorkoutRepositoryImpl.kt:268-270` pass-through the (always-null) values — mapping is dead but harmless.

**Verdict: PASS — no fake placeholder URLs in code, seed, or entity.**

### 2. Media rendering — nothing renders real media; Typography placeholder instead (PARTIAL, P2)

- `app/src/main/kotlin/com/gymcoach/app/presentation/detail/ExerciseDetailScreen.kt:166-184`:
  ```
  // Placeholder Hero Image via Typography
  Box(Modifier.fillMaxWidth().height(200.dp).background(primaryContainer.copy(alpha=0.3f), RoundedCornerShape(24.dp))) {
      Text(text = ex.name.firstOrNull()?.uppercase() ?: "?", style = displayLarge.copy(fontSize=120.sp), ...)
  }
  ```
  The "hero image" is a **colored box containing the first letter** of the exercise name. No `Image` composable renders `ex.imageUrl`.

- **No image-loading library is imported/used anywhere**: grep for `io.coil` returns ZERO matches in `app/src/`. Grep for `AsyncImage|rememberAsyncImagePainter|SubcomposeAsyncImage` returns ZERO matches in `app/src/main`. Coil 2.5.0 is declared in `gradle/libs.versions.toml` (`coil = "2.5.0"`, `coil-compose`) and implemented in `app/build.gradle.kts` (`implementation(libs.coil.compose)`) — **dead dependency** (declared, unused).

- No `painterResource`, no `drawable` usage beyond `ImageVector` icons: `grep "Image|drawable|media"` in `presentation/detail` → only `ImageVector` icon imports (line 49, 528).

**Impact:** On device, no image/video ever renders — the UI shows a nicely-styled letter placeholder. This is a **UX/media gap, NOT a fake-data lie**: the app never claims to display a real photo URL. Severity P2.

### 3. No fake "example.com" media URLs (PASS)

Covered by #1 — verified zero. No hardcoded `https://example.com/x.jpg` or exercise-specific dummy links exist in code, entities, or seed. The previous concern class ("fake media URLs shown as real") is **not present**.

### 4. Coil/Glide network download for media

**N/A** — Coil declared but unused (`grep -rn "coil" app/src --include="*.kt"` → only the libs.toml/build.gradle declaration). No `AsyncImage`, no memory/disk cache setup, no media loading pipeline. Hence **no offline broken-image risk** because no images are loaded at all.

### 5. Camera feature scope (PASS — no fake photo-attachment feature)

Grep for `photo|Photo|attachment|progress picture|before after` across `app/src/main/kotlin` → **zero matches**. There is **no "attach photo to workout" or "progress photos" UI affordance**, so no fake photo feature exists to flag. The only camera in the app is form analysis (below).

### 6. Camera implementation is REAL CameraX (PASS)

- `app/src/main/kotlin/com/gymcoach/app/presentation/camera/CameraPreviewScreen.kt`:
  - Lines 11-18: imports `CameraSelector`, `ImageAnalysis`, `ProcessCameraProvider`, `PreviewView`
  - Line 63: pipeline doc — "CameraX `ImageAnalysis` (RGBA_8888, keep-latest) → upright Bitmap →"
  - Lines 174-214: real binding: `PreviewView(ctx)` → `ProcessCameraProvider.getInstance`, `requireLensFacing(CameraSelector.LENS_FACING_FRONT)`, `bindToLifecycle(...)`, `ImageAnalysis.Builder().setBackpressureStrategy(KEEP_ONLY_LATEST).setOutputImageFormat(RGBA_8888)`
- Navigation wiring: `GymCoachNavHost.kt:33,38,87-88,171-183` — route `camera/{exerciseType}`, `onCameraClick` from `ExerciseListScreen` navigates with an `ExerciseType`.
- Affordance: `presentation/list/ExerciseListScreen.kt:120-121` — `IconButton` with `Icons.Filled.CameraAlt`, contentDescription **"Form Analysis"**; lines 210-232 open a `ModalBottomSheet` ("Which exercise are you doing?") with `ExerciseType.entries` as `FilterChips` that call `onCameraClick(type)`.
- ML chain: `CameraPreviewScreen.kt:111-113` → `FormAnalyzer(exerciseType, defaultFor(exerciseType))`; `PoseDetector` creates MediaPipe `PoseLandmarker` (`PoseDetector.kt:78`, `createFromOptions`).

**Verdict: the camera is a REAL, wired, CameraX-based form-analysis feature — NOT fake.**

### 7. Offline model loading (PARTIAL, P2 — network dependency on camera feature, graceful degradation)

- **Pose model is NOT bundled in the APK.** `find app/src -iname "*.task"` → zero results. No pose asset in `assets/` (only `exercises/*.json`).
- **Download on first launch over HTTPS**: `app/src/main/kotlin/com/gymcoach/app/core/ml/PoseDetector.kt`:
  - Line 56: `MODEL_FILE_NAME = "pose_landmarker_lite.task"`
  - Line 59-61: `MODEL_URL = "https://storage.googleapis.com/mediapipe-models/pose_landmarker/..."` (com.google.mediapipe official)
  - Lines 97-124: `ensureModelFile()` — downloads via `HttpURLConnection` (15s connect / 60s read timeouts, redirects followed), validates `MIN_VALID_MODEL_BYTES = 1MB` (line 64), atomic rename to `filesDir/pose_landmarker_lite.task`, caches for future launches.
- **Offline behavior is graceful**: `CameraPreviewScreen.kt:95-105` wraps `PoseDetector.create(context)` in try/catch → `ModelState.Error`; lines 165-168 render `ModelErrorView(message, onRetry={retryKey++})`; `ModelErrorView` at lines 264-278 shows message + **Retry button**. Doc comment at `PoseDetector.kt:21-22` states the intent: "downloaded on first launch and cached in the app's private storage, keeping the APK lean."

**Assessment:** The app's data layer (readiness, program generation, exercise library) is 100% offline (Room + bundled assets). Only the form-analysis camera requires a one-time network download. The UI degrades gracefully offline (error + retry) rather than crashing. This is a **documented, intentional trade-off**, but it is a real network dependency contradicting a strict "fully offline" claim. Severity P2, mitigation present.

### 8. INTERNET permission used ONLY by PoseDetector (PASS)

- `app/src/main/AndroidManifest.xml:12` — `INTERNET`; :13 `CAMERA`; :14-15 foreground services; :16 POST_NOTIFICATIONS
- `grep -rln "HttpURLConnection|URL(|OkHttpClient|Retrofit|ktor"` across `app/src/main` → **only `core/ml/PoseDetector.kt`**
- No OkHttp/Retrofit/ktor in `gradle/libs.versions.toml` or `app/build.gradle.kts`
- `res/xml/network_security_config.xml` — cleartext blocked, system CAs only; `AndroidManifest.xml:25` applies it, `:29` `usesCleartextTraffic="false"`
- Readiness/program-gen offline confirmed: `grep -rln "https://|http://" app/src/main/kotlin` (excluding PoseDetector) → empty

### 9. Bundled media (PASS — none claimed)

- `res/drawable*` → only `ic_launcher_background.xml` + `ic_launcher_foreground.xml` (vector launcher assets)
- `res/raw` → empty; `assets/` → only `exercises/` JSON
- Zero `.png/.jpg/.webp/.gif/.mp4` in `app/src/main/res`
- No `assets/media` directory

**No game thumbnails / exercise imagery / workout videos are claimed anywhere — there is nothing fake to remove, and the media feature is honestly unimplemented (architecture-ready fields only).**

---

## Severity Rollup

- **P0 (blocks release / fake data):** NONE
- **P1:** NONE
- **P2:** 
  1. Exercise detail "hero image" is a Typography letter placeholder; Coil declared-but-unused dead dependency (declared, unused) — either implement real media (Coil from bundled drawables/assets) or remove the dead Coil dependency and label the placeholder honestly.
  2. Pose model requires one-time HTTPS download for form-analysis camera → not strictly offline; graceful error+retry exists.
- **INFO:** media fields (`imageUrl/videoUrl/animationUrl`) are dead architecture placeholders (always null); safe to keep or remove.

## Recommendations (for Commander's decision)

1. **Remove the unused Coil dependency** (`coil-compose` in `libs.versions.toml` + `app/build.gradle.kts`) OR bundle a few exercise images into `res/drawable` and render via Coil — do not leave a declared-but-dead media stack.
2. **Keep the Typography placeholder** (it's honest) but consider adding a caption like "Illustration coming soon" if users expect photos.
3. **Camera/offline:** document in-app that form analysis needs a one-time download (already error+retry UI); consider bundling `pose_landmarker_lite.task` into `assets/` (~3-5MB) to make the camera fully offline — decision deferred to Commander (APK size vs offline guarantee).