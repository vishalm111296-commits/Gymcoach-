# Phase 0 — Forensic Baseline Report

Mission: Bring GymCoach to production-quality V1 via 20 gated phases. This report is the Phase 0 audit deliverable (AUDIT-ONLY; zero production code changed).

## 1. Baseline
- Repo: `vishalm111296-commits/Gymcoach-` | branch `main` | SHA `fb272457585643578caec17b354856b3c37af7fd`
- Tree clean; Android app; 109 Kotlin files under app/src/main/kotlin
- Host: Ubuntu 24.04 arm64 (Termux proot-distro on Android), 8 cores, 7.3GB RAM, 55GB free
- Toolchain: JDK 17.0.20, Gradle wrapper 8.4, AGP 8.2.2, Kotlin 1.9.22, KSP 1.9.22-1.0.17, Hilt 2.50, Room 2.6.1, Compose BOM 2024.02.00, minSdk 26, target/compileSdk 34

## 2. GitHub State
- 27 open PRs (incl. #50-#13). Phase-19 reconciliation candidates: #13 (SQL-inj seeder), #15/#18/#22/#28 (equipment typing), #16 (VolumeCalculator tests), #19/#21/#23 (security hardening), #20/#17 (perf), #40 (Perform Again), #41-#50 (V1 reconcile + V-taper + refactors). NONE merged to main at baseline.
- CI red: last 5 workflow runs (Sep 2–5 2026) failed; last success Aug 30 2026.
- 38 open issues (sampled, not all triaged).

## 3. Source-of-Truth Contradictions Resolved
1. DB version: HANDOFF says v2, MASTER_COMPLETION says 1→5 → ACTUAL `GymCoachDatabase` v11 with exported schemas 1.json–11.json.
2. Test count: FINAL_RELEASE_AUDIT says 148/148, RELEASE_READINESS says 73 → ACTUAL 78 unit + 36 androidTest @Test = 114 (source-counted).
3. "V1 released" claims → CI failing + confirmed defects present; not releasable.
4. Offline-first spec → manifest has INTERNET + PoseDetector downloads 5MB model at first launch (HTTPS-only).

## 4. Confirmed Priority Defects (source-verified)
- [Phase-1 gate] HomeViewModel.plannedWeeklySets broadcasts whole-day sets to every target muscle (`split(',').forEach { result[muscle] += daySets }`).
- [Phase-1 gate] VolumeCalculator.isoWeekKey uses Calendar.WEEK_OF_YEAR w/o ISO week-year (New-Year break).
- [Phase-1 gate] TrainingBalance built from raw cross-week sums, classify() thresholds expect weekly sets → over-classification.
- [Test-compile breaker] Duplicate `ProgramGeneratorTest` FQCN (stale 20-line fake in app/src/test/java + real 8-test in kotlin).
- [UI] "0.0%%" (ProgressDashboardScreen:263); fake "Est. Calories = volume*0.05" (2 sites).
- [Arch] Dead ExerciseVideoPlayer; no Perform Again; history-detail Edit → active session mismatch; `.first()` sync flow read in detail VM.

## 5. Sound Foundations to PRESERVE
- Room v11 migrations real (waiting on migration-id cross-check); batched anti-N+1 DAO queries; ProgressionEngine + PRDetector sound; CameraX pipeline correct (KEEP_ONLY_LATEST, buffer release, cleanup); transactional idempotent seeder; Material3 shell; Hilt DI wired.

## 6. Ground-Truth Build & Test Status
- PENDING: toolchain install in progress (SDK + NDK 23.1.7779620 downloading). Once up: `./gradlew testDebugUnitTest` (expect duplicate-class failure), `assembleDebug`, `lintDebug`. Results appended as evidence and upgrade UNVERIFIED cells in completion matrix.

## 7. Matrix & Backlog
See `completion-matrix.md` (25 features, evidence-based). Phase backlog in `todo.md` (Phase 1 gate decomposed into defect fixes + 8 regression-test categories).

## 8. GitHub Research Findings (Planner-verified, 2026-09-06)
- **No standalone open issues** (only issues#1 = CI lint-fail report). Work is PR-tracked; 30 open PRs sampled (#13…#50); all unmerged.
- **External validation of Phase-1 P0 bug**: PR#44 body states exactly "Fix P0 (Day-Level Broadcasting in HomeViewModel): plannedWeeklySets() now attributes set volume per individual exercise" — corroborates mission-flagged defect.
- **PR#16** (VolumeCalculator tests, mergeable=clean, +151/−16) is a merge-ready candidate for Phase 1 (do not merge blindly; reconcile w/ our fix).
- **PR#40** Perform Again fully implemented (+1351/−54, 10 commits) — Phase 6 should port/review rather than reimplement.
- **PR#50** (Phase 5 Program Engine, mergeable, state=unstable) and **#42/#43/#44/#48/#49** V-taper family — Phase 19 reconciliation targets.
- **CI**: 5 consecutive failures (Sep 2–5), including 3 DIRECT-to-main failures (Sep 2); last main success 2026-09-02T17:19 (run 33660309210). Definitively not releasable statement.
- Overlap/churn families: equipment-type ×5 (P9), Phase-2 dashboard ×3, audit/docs ×5 → reconciliation needed (Phase 19).