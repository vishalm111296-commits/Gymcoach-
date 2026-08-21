# GymCoach - UX/UI Audit

## 1. Executive Summary
This audit evaluates GymCoach's existing Android UI/UX screens, interactions, and design patterns. The app uses Material 3 components but lacks cohesive branding, visual depth, and specialized mobile optimization for high-stress, high-friction contexts (e.g., in-gym active lifting).

---

## 2. Screen-by-Screen Breakdown & Usability Friction

### A. Workout Session Screen (`WorkoutSessionScreen.kt`)
*   **Rest Timer Inconsistency**: Injected directly as a top item card inside the scrollable lazy list. Under long lists of exercises, the rest timer scrolls off-screen. Users lose track of rest progress during navigation.
*   **Friction in Workout Logging Inputs**: Outlined text fields for Weight, Reps, RPE, and Rest are dense, uniform, and lack high-contrast focal points. No placeholder labels exist on individual row inputs, forcing users to rely on top-level sticky/header labels which scroll out of view.
*   **Lack of Haptic Context**: Only `onToggleComplete` triggers a generic long-press haptic feedback. Set additions/deletions, weight adjustments, or workout completion events lack tactile confirmation.
*   **Density Overload**: The tabular structure (Set, Weight, Reps, RPE, Rest, complete status, and set type trigger) results in 7 horizontal interactive items on phone screens. Tap targets are narrower than the standard 48x48dp spacing recommendation.
*   **Set Type Obscurity**: Cycling set types is mapped to a generic `Icons.Default.Star` with minimal labeling. The code cycles: Normal -> Warmup -> Drop -> Failure -> Normal.

### B. Exercise Detail Screen (`ExerciseDetailScreen.kt`)
*   **Text-Heavy Layout**: Relies entirely on vertical blocks of text. The description, instructions, tips, common mistakes, and safety notes are visually identical, creating high cognitive load.
*   **Hero Image Missing**: Uses a generic static box with a placeholder icon (`Icons.Default.FitnessCenter`) which lacks premium product branding.
*   **Navigational Friction**: Top bar color uses solid `primaryContainer` directly, creating high-contrast separation from the hero image box, making the layout feel fragmented.

### C. Workout History Screen (`WorkoutHistoryScreen.kt`)
*   **Table Header Overload**: Relies on a hardcoded, ASCII-drawn text line string for the layout alignment headers:
    `"Workout Date         Duration   Sets   Volume      Notes"`
    `"─────────────────  ──────  ────  ────────────  ───────────────────"`
    This layout breaks on narrow screen resolutions or custom dynamic system fonts.
*   **Flat Activity Cards**: No micro-spacing or shadow treatments to separate metadata (duration, exercise count, sets, volume) from card content. Primary color highlights all stat values uniformly, making scanability poor.

### D. Empty States (`EmptyState.kt` & Screen fallbacks)
*   **Generic Fallbacks**: The component utilizes standard `Icons.Filled.Info` and standard gray colors. Fallbacks in `WorkoutHistoryScreen` and `PRScreen` bypass `EmptyState` component entirely, displaying raw, unstyled text items aligned to the screen center.

### E. Theme & Colors (`Theme.kt`)
*   **Color Profile**: Employs generic green (`0xFF2E7D32`) and orange (`0xFFFF6D00`) variants. The colors contain high saturated components that generate significant eye fatigue when viewed under low-light gym conditions.
*   **No Custom Dark System Token**: Default dark colors fallback to generic Material 3 theme properties. Surface colors lack depth layers (elevation overlays).

---

## 3. Heuristic & Accessibility Checklist

| Feature/Metric | Evaluation | Action Required |
| :--- | :--- | :--- |
| **Tap Targets** | Poor (Reps/Weight input columns overlap) | Standardize to 48x48dp minimal target boundaries. |
| **Hierarchy** | Moderate (Screen headers are clear, content is flat) | Restructure card layouts with clear font weights. |
| **Contrast Ratio** | Good on dark mode, weak container elements | Ensure all badge text exceeds 4.5:1 ratio. |
| **Error Recovery** | Good (Alert dialogs block terminal states) | Transition generic alerts to localized inline warnings. |
| **Screen Reading**| Basic (Navigation labels exist, stats lack semantics)| Add content descriptions to progress widgets. |

---

## 4. Immediate High-Priority Fixes
1.  **Detach Rest Timer**: Move from list item to a persistent bottom sheet, floating action button, or sticky overlay header.
2.  **Redesign Set Row Inputs**: Convert generic `OutlinedTextField` elements to structured, borderless input fields with background containers.
3.  **Replace Table Headers**: Use standard Compose `Row` structures with weights rather than ASCII strings in `WorkoutHistoryScreen`.
4.  **Enforce Empty State Reuse**: Standardize all empty displays using the dedicated `EmptyState` layout with customized icons.
