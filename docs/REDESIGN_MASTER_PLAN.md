# GymCoach V1 Redesign Master Plan

## REDESIGN PRINCIPLES
- **Preserve Business Logic**: Never rewrite existing working algorithms (V-Taper calculation, Room Schema migrations, Rest Timer Doze compatibility) merely for visual reasons.
- **Improve UX Structure**: Standardize empty states, unify Bottom Navigation, and optimize touch targets across the app.
- **Accessibility & Contrast**: Enhance readability through typography scaling and cohesive Dark Theme implementation.

## PREMIUM V1 REDESIGN SPECIFICATION

### Design System
- **Colors**: Defined primary, secondary, and dynamic surface elements (elevated cards). Ensure semantic matching for success/warning states in the workout completion views.
- **Typography**: Display headlines for major dashboard statistics, semantic numeric displays for PRs/Weights.
- **Shapes & Spacing**: Standardize at 8dp base scaling grids with fully rounded sheet corners.
- **Components**: Construct abstract composables such as `GymCoachButton`, `GymCoachCard`, `GymCoachLoadingState`, `GymCoachTopBar`.

## PHASE ROADMAP
**PHASE 0 — BASELINE FREEZE** (Status: Complete. Main frozen and tests successfully validated securely).
**PHASE 1 — DESIGN SYSTEM** (Files: Theme.kt, Color.kt, Type.kt)
**PHASE 2 — APP SHELL / NAVIGATION**
**PHASE 3 — ONBOARDING**
**PHASE 4 — HOME / COACHING**
**PHASE 5 — WORKOUT EXPERIENCE**
**PHASE 6 — EXERCISE LIBRARY / DETAIL**
**PHASE 7 — ANALYTICS / PROGRESS**
**PHASE 8 — READINESS**
**PHASE 9 — CAMERA / FORM COACH**
**PHASE 10 — POLISH / ACCESSIBILITY**
**PHASE 11 — MEDIA / CONTENT**
**PHASE 12 — FINAL QA / RELEASE**

## DATA SAFETY
- No destructive migrations.
- No dummy mocking replacements for genuine functional states (e.g. `0kg` to signify empty/null).
- `RoomMigrationTest` remains untouched from `1-11`.
