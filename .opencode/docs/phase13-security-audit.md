# Phase 13 — Security & Release-Signing Audit (GymCoach)

Date: 2026-09-06
Branch: phase5-recovery-verified (HEAD b5fa19c)
Agent: Planner (read-only forensic audit)
Method: direct file inspection + git-tree scan. No files modified.

---

## 1. Release Signing Config | **FAIL (release blocked)**

**Evidence:**
- `app/build.gradle.kts:66-75` — `signingConfigs.create("release")` reads `KEYSTORE_PATH` from env or `keystore/release.jks` (default), `storePassword`/`keyPassword` default to empty string `""`.
- `build.gradle.kts:22-24` — keystore props loaded from `rootProject.file("local.properties")`.
- `local.properties:1` — contains ONLY `sdk.dir=/root/android-sdk`. **No KEYSATORE_PATH / passwords.**
- `gradle.properties` — no keystore props (grep: none).
- `keystore/` directory — **MISSING** (`ls -la keystore/` → "keystore dir MISSING"). Referenced `keystore/release.jks` does not exist.
- `.gitignore:18,19,21` — correctly excludes `*.jks`, `*.keystore`, and `keystore/`.

**Verdict:** `assembleRelease` would either (a) fail because `storeFile` points to a nonexistent `keystore/release.jks`, or (b) if `storeFile` resolves, produce a build that is NOT signable because `storePassword`/`keyPassword` default to `""` (AGP throws "Keystore was tampered with, or password was incorrect"). **Release is not buildable/signed as-is.** This is expected for a repo with no CI credentials — it is a *known gap* documented in the build script, not a leaked secret. Severity: **P1 (release blocked, no secret exposure)**.

**Note on history:** the `git log -p` output I grepped shows the *repo itself* previously had a `storePassword = keystoreProperties["storePassword"]` line in older git history (evidence the signing approach has been consistent) — no secrets committed.

---

## 2. Secrets in Repo / Git History | **PASS**

**Evidence:**
- `git ls-files | grep -iE "keystore|\.jks|\.env|secret|\.properties"` → only `gradle.properties` (wrapper props, no secrets) and `gradle/wrapper/gradle-wrapper.properties`.
- `git log --all -p` scan for `API_KEY|secret|password=|AKIA|BEGIN RSA|BEGIN PRIVATE` → **no credentials.** The only matches were the build-file *comments* ("storePassword = ..." appears as doc comments in historical diff lines, not real values).
- `.gitignore:11` excludes `local.properties` (holds sdk.dir + would hold secrets if present).
- No `.env` file exists.
- Grep across `app/src/main` for `imageUrl`/secrets → no API keys/tokens.

**Verdict:** No secrets (keystores, passwords, API keys, tokens, cert keys) are committed. PASS. Severity: PASS.

---

## 3. AndroidManifest — Component Exposure | **PASS (all locked down)**

**Evidence (app/src/main/AndroidManifest.xml):**
- `<activity .ui.MainActivity android:exported="true">` with `LAUNCHER` intent-filter (line 44-49). Exported=true is **required** for a launcher activity — correct. No malicious intent-filter exposure (only MAIN/LAUNCHER).
- `<service .core.notification.RestTimerNotificationService android:exported="false">` (line 54-57) — exported=false, correct.
- `<receiver .core.notification.RestTimerReceiver android:exported="false">` (line 59-61) — exported=false, correct.
- **No other actionable components.**
- `android:usesCleartextTraffic="false"` (line 29) — cleartext HTTP disabled globally.
- `android:networkSecurityConfig="@xml/network_security_config"` (line 25) — linked to strict config (see #6).
- `android:allowBackup="false"` at application level, plus `dataExtractionRules`/`fullBackupContent` (lines 21-23).

**Configured permissions** (lines 9-14): INTERNET, CAMERA, FOREGROUND_SERVICE, FOREGROUND_SERVICE_SHORT_SERVICE, POST_NOTIFICATIONS. Only CAMERA + FGS are actually exercised; see #7 for permission-request findings.

---

## 4. PendingIntent Security | **PASS (exemplary)**

**Evidence:**
- `RestTimerNotificationService.kt:158-163` — content intent uses `PendingIntent.getActivity(..., PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)`.
- `RestTimerNotificationService.kt:165-171` — action PendingIntents (Complete/Skip/+15/-15/Pause) use `FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE`.
- `RestTimerReceiver.kt:9` — class-level comment: "Exported=false; only the app's own PendingIntents reach it."
- All PendingIntents are **IMMUTABLE** → cannot be mutated by other apps. Correct modern practice (Android 12+).

**Verdict:** PASS. All notification PendingIntents are immutable and point to non-exported components.

---

## 5. Data Exposure / Privacy | **PASS (with one minor note)**

**Evidence:**
- Share sheet: `WorkoutHistoryDetailScreen.kt:359-364` — `Intent.ACTION_SEND` with `EXTRA_TEXT` = formatted workout summary, `EXTRA_SUBJECT` = "My Workout - $date". **Only the user's own workout**, no external data leakage. Uses `createChooser`.
- PII logging: `grep Log.(d|i|e|w)` filtered for weight/password/email/etc → **no PII logged.**
- Backup: `data_extraction_rules.xml` — `cloud-backup` excludes database + seed prefs; `device-transfer` *includes* the database + sharedprefs (enables device-to-device transfer of the user's own data — reasonable). `backup_rules.xml` (legacy full-backup) excludes database + seed prefs.
- `allowBackup="false"` (Manifest line 21) but `dataExtractionRules`/`fullBackupContent` are still present — the combination is slightly redundant (allowBackup=false would disable cloud backup entirely on older APIs), but data-extraction rules govern Android 12+ device transfer. Not a security regression; note only.

**Verdict:** PASS. Share scope = user's own workout; no PII logged; backup rules conservative (cloud-backup excludes DB; device-transfer keeps own data by design).

---

## 6. Network Security Config | **PASS (strict HTTPS-only, no cleartext overrides)**

**Evidence:**
- `network_security_config.xml:1-9` — `<base-config cleartextTrafficPermitted="false">` with only `system` trust anchors. **No domain-specific cleartext exceptions.**
- Combined with Manifest `usesCleartextTraffic="false`.
- Only network client in app: `PoseDetector.kt:60` downloads the MediaPipe pose model from `https://storage.googleapis.com/...` over **HTTPS** (grep of all `http` in kotlin outside mediapipe/storage found zero other offenders).

**Verdict:** PASS. No cleartext permitted to any domain; only outbound calls are over HTTPS.

---

## 7. Runtime Permission UX | **PARTIAL**

**Evidence:**
- CAMERA: `CameraPreviewScreen.kt:80-90` correctly requests runtime CAMERA via `rememberLauncherForActivityResult(RequestPermission())` guarded on `checkSelfPermission` + `LaunchedEffect`.
- **POST_NOTIFICATIONS: no runtime request anywhere** (grep for `requestPermission|NotificationPermission|areNotificationsEnabled` → only the CAMERA launcher). `RestTimerNotificationService` calls `startForeground(...)` at `:146`, but on Android 13+ the channel/badge may not appear if POST_NOTIFICATIONS was never granted. This is a **minor UX/functional gap**, not a security hole (notifications don't expose data beyond the user's own rest timer). Severity: **P2/P3 (INFO)**.

**Verdict:** PARTIAL — CAMERA handled; POST_NOTIFICATIONS not runtime-requested (Android 13+ notification may be silently suppressed).

---

## Summary Table

| # | Item | Verdict | Severity | Evidence |
|---|------|---------|----------|----------|
| 1 | Release signing | **FAIL** (blocked, no secret leaked) | P1 | build.gradle.kts:66-75; keystore/ MISSING; local.properties:1 |
| 2 | Secrets in repo/history | PASS | — | git ls-files + git log -p scan; .gitignore:11,18,19,21 |
| 3 | Component exposure | PASS | — | Manifest:44-61 exported flags |
| 4 | PendingIntent | PASS | — | RestTimerNotificationService.kt:158-171 FLAG_IMMUTABLE |
| 5 | Data exposure/PII | PASS | — | SWHistory:359-364 createChooser; no PII logs; conservative backup |
| 6 | Network security | PASS | — | network_security_config.xml; PoseDetector https |
| 7 | Runtime permissions | **PARTIAL** | P2/P3 | CAMERA requested (CameraPreviewScreen:80-90); POST_NOTIFICATIONS never requested |

---

## Recommendations (deferred, not blockers)
1. **Release signing (P1):** Continue the existing env/local.properties strategy. Ship a real release keystore only via CI secrets; keep `.gitignore` exclusions. Document that `assembleRelease` is intentionally unsigned until `KEYSTORE_PATH`/passwords are provided.
2. **POST_NOTIFICATIONS (P3):** Optionally add a runtime `RequestPermission()` for `Manifest.permission.POST_NOTIFICATIONS` (guarded to SDK 33+) if rest-timer notifications are expected to show on Android 13+.
3. **(INFO)** Consider dropping the redundant `allowBackup="false"` + data-extraction-rules combination for clarity — harmless as-is.
