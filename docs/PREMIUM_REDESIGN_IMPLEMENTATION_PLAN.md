# GYMCOACH PREMIUM REDESIGN IMPLEMENTATION PLAN

This document outlines the strategic plan to transform the GymCoach app from a technically correct foundation into a premium, 5-star personal strength-coaching product. It is grounded entirely on the verified capabilities from the `PRE_REDESIGN_AUDIT.md`.

## 1. DESIGN SYSTEM STRATEGY (Premium Performance App)
The goal is to move away from "Generic Material CRUD" to a "Premium Performance" aesthetic.
*   **Typography:** Bold, condensed headers for data points (PRs, Volume). Highly legible sans-serif for lists and instructions.
*   **Colors:** Deep dark mode default with high-contrast, energetic accents (e.g., neon green/electric blue) for primary actions (Start Workout, Log Set).
*   **Spacing & Shapes:** Tighter spacing in workout logs to reduce scrolling. Use subtle surface elevation rather than heavy borders. Avoid "cards inside cards."
*   **Components:**
    *   Frictionless input fields for sets/reps (large touch targets, numeric keypads by default).
    *   Bottom sheets for non-blocking actions (e.g., exercise substitution).
    *   Premium charts (bezier curves, gradient fills) for Analytics, replacing basic static bars.
*   **Motion & Haptics:** Snappy transitions between workout sets. Heavy haptic feedback when a PR is logged or a set is completed.

## 2. THE CORE USER LOOP (Highest Priority - P0)
The redesign will heavily optimize the following flow:
`Home Dashboard -> Today's Workout -> Start Workout -> Frictionless Set Logging -> Rest Timer -> Complete Workout -> Performance/Progress Review`

### P0 Priority Implementations
1.  **Workout Session/Logging:** MUST be frictionless. The current implementation is clunky.
2.  **Home Dashboard:** Must answer "What should I do today?" immediately.
3.  **Design System Foundation:** Typography, colors, and core components.
4.  **Empty States:** Action-oriented (e.g., "Start your first program" instead of "No data").
5.  **Exercise Library & Detail:** Accommodate future media; clean up the text-heavy UI.

### P1 Priority Implementations
1.  **Readiness Integration:** Feed the existing Readiness score into the Program Generator.
2.  **V-Taper Evolution:** Better visualization of the existing static score data.
3.  **Progress/Analytics:** Implement premium charting.
4.  **Calendar/History:** Clean up the historical view of past workouts.

### P2 Priority (Defer to Later Phases)
1.  Gamification, Health Connect, Richer Recovery (Nutrition/Sleep integration).

### P3 Priority (Defer/Ignore)
1.  Camera/Form Intelligence (High risk, keep deferred until models are robust).
2.  Social/Community (Conflicts with offline-first scope).
3.  Advanced Adaptive AI (Beyond the scope of the current static logic).

## 3. V-TAPER EVOLUTION STRATEGY
GymCoach currently uses a static 0-10 relevance score per exercise. We will not invent fake AI, but we will evolve the feature transparently:
*   **CURRENT (Phase 1 Redesign):** Expose the static V-taper scoring visually. Show users *why* an exercise is chosen based on its existing database score.
*   **NEXT (Phase 2):** Transparent volume analysis. Show a chart comparing total weekly sets of high V-taper exercises vs. low V-taper exercises. (Requires aggregating `WorkoutSetEntity` against `ExerciseEntity.vTaperScore`).
*   **LATER (Phase 3):** Actionable recommendations (e.g., "You missed your lateral raises yesterday. Swap them in today.").
*   **FUTURE:** True adaptive coaching.

## 4. READINESS INTEGRATION
*   **Current:** An isolated survey generating a text string ("Rest day", "Full intensity").
*   **Required Domain Change:** Update `ProgramGenerator` to accept the `ReadinessScore`. If score < 2.5, reduce target sets by 1 for all exercises in today's generated workout.
*   **Future:** Granular RPE/RIR tracking influencing readiness automatically.

## 5. COMPETITOR GAP ANALYSIS (Actionable subset)
*   **Granular Workout History (Strong):** GymCoach has the data. We must adapt the UI to display it with the clarity of Strong. (Priority: P1, ADAPT).
*   **Visual Form Guidance (Caliber):** GymCoach lacks media. We must redesign the `ExerciseDetailScreen` to gracefully support placeholder images that can be replaced by real media later. (Priority: P0, ADAPT).
*   **Social/Sharing (Hevy):** IGNORE. Conflicts with offline-first.
*   **True Adaptive AI (Fitbod):** FUTURE. GymCoach's current rule-based generator is not this.

## 6. SCREEN-BY-SCREEN REDESIGN SPEC (Examples)

### Workout Session Screen (P0)
*   **Current Problem:** Clunky logging, poor visual hierarchy.
*   **Preserve:** `WorkoutRepository` persistence, Active workout recovery.
*   **New Hierarchy:** 1. Current Exercise (large font), 2. Previous Performance (context), 3. Set Input Row (massive tap targets), 4. Rest Timer (sticky bottom).
*   **Primary Action:** Log Set (haptic feedback, immediate auto-advance to next set/rest).
*   **Empty/Error State:** N/A (Workout always has at least one exercise).

### Home Dashboard (P0)
*   **Current Problem:** Basic layout, doesn't feel like a "Coach".
*   **Preserve:** Today's workout logic, active workout check.
*   **New Hierarchy:** 1. Hero Card: "Next Workout" or "Resume Workout", 2. V-Taper Priority Visualization, 3. Quick Actions (Log freestyle, Readiness check).
*   **Empty State:** Hero Card: "Generate your first V-Taper Program" -> Navigates to Onboarding.

## 7. ARCHITECTURE PRESERVATION
**CRITICAL RULE:** Do NOT modify `WorkoutRepository`, `ExerciseRepository`, `AnalyticsRepository`, or Room DAOs/Entities to achieve the visual redesign. The domain and data layers are verified (A. KEEP AS-IS). Only UI state mapping within ViewModels and Compose screens will be modified.

## 8. IMPLEMENTATION SEQUENCING

*   **Phase 1: Design System & Core Logging (P0)**
    *   *Objective:* Establish colors/typography and redesign the Workout Logging loop.
    *   *Files:* `Theme.kt`, `Color.kt`, `WorkoutSessionScreen.kt`, `WorkoutLoggingViewModel.kt`.
    *   *Risk:* Low. Pure UI changes.
*   **Phase 2: Home & Onboarding (P0)**
    *   *Objective:* Create the "Premium Coach" feel upon opening the app.
    *   *Files:* `HomeDashboardScreen.kt`, `OnboardingScreen.kt`.
    *   *Risk:* Low.
*   **Phase 3: Progress & Analytics (P1)**
    *   *Objective:* Implement premium charts for PRs and Volume.
    *   *Files:* `ProgressDashboardScreen.kt`, `ProgressViewModel.kt`.
    *   *Risk:* Medium (Charting library integration).
*   **Phase 4: Readiness Integration (P1)**
    *   *Objective:* Connect Readiness to `ProgramGenerator`.
    *   *Files:* `ProgramGenerator.kt`.
    *   *Risk:* Medium (Modifies domain logic).

## 9. DEFINITION OF DONE
The redesign is complete when a user can seamlessly:
1. Complete onboarding.
2. See exactly what they should do today on the Home screen.
3. Log a workout with one-thumb, frictionless interaction.
4. View beautiful, chart-based evidence of their progress and V-taper focus.

## 10. FINAL RECOMMENDATION
*   **WHAT TO BUILD FIRST:** The Workout Session / Set Logging experience.
*   **WHAT TO PRESERVE:** The entire Room database, repositories, and Clean Architecture.
*   **WHAT TO FIX:** Nothing (AndroidTest baseline is fixed).
*   **WHAT TO REDESIGN:** Home, Workout, Progress, and Onboarding UI.
*   **WHAT TO ADD:** A premium charting library and frictionless numeric input components.
*   **WHAT TO DEFER:** AI Camera Form Coach and Social features.
