# GymCoach V1 REDESIGN MASTER PLAN

## PHASE 0 — BASELINE FREEZE
**OBJECTIVE**: Freeze current feature logic (`main` SHA strictly passes tests and constraints without large regressions).
**STATUS**: Frozen. Repository has verified tests, secure integration maps, Room migrations (1->11) perfectly tested and safely integrated without destructive schemas.

## PHASE 1 — DESIGN SYSTEM
**OBJECTIVE**: Formulate UX UI standards spanning animations, colors, shapes, bottom navigation bar aesthetics.
**FILES/AREAS**: `Theme.kt`, `Color.kt`, `Type.kt`.

## PHASE 2 — NAVIGATION / APP SHELL
**OBJECTIVE**: Build out modern scaffolding to host Bottom Nav, App Bars, Modals securely tracking `NavHost` state dynamically.

## PHASE 3 — ONBOARDING
**OBJECTIVE**: Streamline the entry questionnaire focusing on fast equipment checks and rapid `UserProfileEntity` setup.

## PHASE 4 — HOME / COACHING
**OBJECTIVE**: Redesign the main dashboard focusing on clean card structures reporting 'Today's Workout' directly via real-time analytics.

## PHASE 5 — WORKOUT EXPERIENCE
**OBJECTIVE**: Complete overhaul of the Exercise/Set logging process. Quick add weights, automated Rest Timer popup handling, visual distinction between WARMUP/DROP/FAILURE sets.

## PHASE 6 — EXERCISE LIBRARY & DETAIL
**OBJECTIVE**: Display the exercises mapped perfectly by Equipment bounds with strong search filters. Improve media loading visuals (placeholders -> actual caching structure).

## PHASE 7 — ANALYTICS / PROGRESS
**OBJECTIVE**: Build comprehensive tracking dashboards mapping to the `WorkoutRepositoryImpl` outputs (`getMonthlyVolumes`, `getPersonalRecordMax`, `getTotalVolumeSum`).

## PHASE 8 — READINESS
**OBJECTIVE**: Display visual radar tracking Sleep/Energy/Soreness mapped out over the trailing 7 day `ReadinessEntity` cache.

## PHASE 9 — CAMERA / FORM COACH
**OBJECTIVE**: Improve physical CameraX preview overlays safely dropping unused ImageProxies (`STRATEGY_KEEP_ONLY_LATEST`). Polish landmark visualizations natively.

## PHASE 10 — POLISH / ACCESSIBILITY
**OBJECTIVE**: Validate tap zones, contrast rates across dark/light mode, string abstractions natively globally.

## PHASE 11 — MEDIA / CONTENT
**OBJECTIVE**: Replace placeholders dynamically fetched from the seeder JSON to proper offline/cached media where practical.

## PHASE 12 — FINAL QA / RELEASE
**OBJECTIVE**: Complete runtime executions, secure APK packaging, rigorous doze/process death survivability, formal `RELEASE` launch validation.
