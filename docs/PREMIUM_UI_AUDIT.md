# GymCoach — Comprehensive UI/UX Pre-Redesign Audit

**Date:** 2026-09-18  
**Scope:** 14 Screens and Core Subsystems  
**Target:** Visual/Product-Quality Transformation to Premium Fitness Coaching System

---

## 1. Executive Summary & Design System Foundations

GymCoach has solid closed-loop progression, autoregulation, offline Room persistence, and pose analysis logic. However, visual and tactile fidelity suffers from:
1. **Inconsistent Component Density & Padding:** Cards vary widely between generic Material 3 defaults and custom cards with arbitrary paddings.
2. **Text Wrapping Glitches:** Fixed badges like "Advanced" wrap into vertical characters (`Ad \n vanc \n ed`) on standard displays.
3. **Ghost Media Controls:** Exercise details previously displayed empty ExoPlayer video views with `0:00 / 0:00` even when no playable video was present.
4. **Active Workout Friction:** Inputs look like generic forms rather than a tactile, glanceable lifting terminal answering: *"What do I lift right now?"*
5. **Dashboard Hierarchy Flatness:** Home screen gave equal visual weight to historical charts, V-taper, and the immediate actionable daily workout.

---

## 2. Screen-by-Screen UI Audit (14 Screens)

### 1. Home Dashboard (`HomeDashboardScreen.kt`)
1. **Current Hierarchy:** Greeting header -> Loading / Empty / Today's Workout Card -> Readiness Card -> Weekly Consistency -> Coach Insight -> V-taper Focus Card.
2. **Primary User Goal:** Immediately answer *"What should I train today and why?"*
3. **Most Important Action:** `[ START WORKOUT ]` on Today's Workout card.
4. **Secondary Actions:** View Program, Log Readiness, View V-taper details, Navigate tabs.
5. **Information Overload:** Moderate; V-taper and readiness can compete if unranked.
6. **Empty State:** `EmptyProgramCard` prompting the user to select or generate a program.
7. **Loading State:** Centered `CircularProgressIndicator`.
8. **Error State:** Falls back to empty/fallback program view gracefully.
9. **Visual Hierarchy:** Needs a dominant hero workout card with clear duration, exercise count, and readiness context.
10. **Typography:** Needs high-contrast display titles and subdued secondary metadata.
11. **Spacing:** Standardize to `GymCoachSpacing` scale (12dp/16dp/20dp).
12. **Component Consistency:** Unify cards with `GymCoachColors.SurfaceCard` and `GymCoachBorders.subtle`.
13. **Navigation:** Bottom navigation integration with haptics and tab indicators.
14. **Accessibility:** Proper content descriptions on readiness gauge and start buttons.
15. **Animation:** Smooth transitions for readiness score and today workout hero entry.
16. **Responsiveness:** Fluid column constraints that fit comfortably on compact screens without vertical crowding.

---

### 2. Active Workout Logging (`WorkoutSessionScreen.kt`)
1. **Current Hierarchy:** Workout title & elapsed timer -> Recovery advisory (if low) -> Active Rest Timer -> Exercise Cards list -> Add Exercise / Complete Workout action bar.
2. **Primary User Goal:** Log current set quickly and effortlessly between heavy lifts with chalked hands.
3. **Most Important Action:** Toggle set completion (`[✓ DONE]`) and start rest timer.
4. **Secondary Actions:** Add set, adjust weight/reps/RPE, open plate calculator, camera form check, plate calculator.
5. **Information Overload:** High if all set inputs and notes are expanded simultaneously.
6. **Empty State:** Empty workout card with "Add Exercise" button.
7. **Loading State:** Instantaneous local Room observation.
8. **Error State:** Error banner for invalid weight/rep inputs.
9. **Visual Hierarchy:** Exercise Name -> Target (Why this weight?) -> Previous Performance -> Set Rows (Weight, Reps, RPE, Checkmark).
10. **Typography:** Monospace/tabular numerical figures for weights/reps, distinct bold headers.
11. **Spacing:** Compact rows (8dp gap, 44dp minimum touch target).
12. **Component Consistency:** Cohesive dark slate inputs with subtle borders and glowing green completion states.
13. **Navigation:** Back button prompts discard/pause confirmation; Finish prompts save.
14. **Accessibility:** Touch targets >= 48dp, TalkBack labels for set checkmarks and set type buttons.
15. **Animation:** Tactile bouncy spring on set completion checkmark, smooth progress bar on rest timer.
16. **Responsiveness:** Keyboard padding avoids obscuring active inputs.

---

### 3. Exercise Library (`ExerciseListScreen.kt`)
1. **Current Hierarchy:** TopBar (Library title, count, icons) -> Search bar & filter trigger -> Category chips -> Exercise LazyColumn -> Bottom Navigation.
2. **Primary User Goal:** Find exercises by muscle, equipment, or name; inspect animation and difficulty.
3. **Most Important Action:** Tap exercise card to view technique details.
4. **Secondary Actions:** Favorite toggle, filter by difficulty/equipment/pattern, custom exercise creation.
5. **Information Overload:** Moderate; excessive badges can clutter cards.
6. **Empty State:** "No exercises found" with quick reset filter button.
7. **Loading State:** Fast SQLite query; skeleton cards when refreshing.
8. **Error State:** Empty state with retry/reset.
9. **Visual Hierarchy:** Bold exercise title -> Muscle pill & equipment -> Difficulty badge & Animation indicator.
10. **Typography:** 16sp bold title, 11sp semi-bold tags.
11. **Spacing:** 10dp card spacing, 16dp horizontal margins.
12. **Component Consistency:** Standardized `ExerciseItemCard` across library, picker, and builder.
13. **Navigation:** Direct navigation to Exercise Detail and bottom bar destinations.
14. **Accessibility:** Accessible touch targets for favorite icon and filter chips.
15. **Animation:** Favorite heart spring animation, smooth chip selection.
16. **Responsiveness:** Single-line difficulty badge prevents "Ad-vanc-ed" vertical fragmentation.

---

### 4. Exercise Detail (`ExerciseDetailScreen.kt`)
1. **Current Hierarchy:** TopBar -> Exercise Title & Muscle/Equipment tags -> Hero Media Section (Stickman Animation / Video / Stylized Anatomy Profile) -> Action Buttons (Form Check, Analytics) -> Quick Specs -> Technique & Coaching Cues -> Common Mistakes -> Substitutes.
2. **Primary User Goal:** Learn correct form, biomechanical cues, and technical parameters for an exercise.
3. **Most Important Action:** Watch form animation or start live camera form check.
4. **Secondary Actions:** Favorite, view progress analytics, select substitution.
5. **Information Overload:** High if all text is presented as plain walls; structured cards solve this.
6. **Empty State:** N/A (loaded by ID); displays fallback anatomy card if no animation/video exists.
7. **Loading State:** Centered progress indicator while loading Room record.
8. **Error State:** "Exercise not found" back button navigation.
9. **Visual Hierarchy:** Hero preview -> Action buttons -> Structured cue cards.
10. **Typography:** Section headers in 16sp SemiBold, coaching cues with checkmark icons.
11. **Spacing:** 16dp card spacing, clean 12dp internal paddings.
12. **Component Consistency:** Matches `GymCoachColors.SurfaceCard` and `GymCoachBorders.subtle`.
13. **Navigation:** Back to list/workout, forward to CameraPreview or ProgressionAnalytics.
14. **Accessibility:** Video and animation players have clear play/pause controls and content descriptions.
15. **Animation:** Frame-by-frame stickman vector rendering with speed controls.
16. **Responsiveness:** Scrollable column supporting small screens and large foldables.

---

### 5. Program Overview (`ProgramDetailScreen.kt`)
1. **Current Hierarchy:** Active Program banner (Name, split, days/week) -> Days list with exercises -> Program Generator / Builder CTA.
2. **Primary User Goal:** Understand weekly routine structure and which workout is next.
3. **Most Important Action:** Start today's scheduled day or generate new routine.
4. **Secondary Actions:** Edit program, reorder exercises, inspect day details.
5. **Information Overload:** Moderate; collapsible days keep overview clean.
6. **Empty State:** "No active program" card with [ Generate Program ] or [ Create Custom Routine ].
7. **Loading State:** Skeleton program card while Flow resolves.
8. **Error State:** Friendly recovery card allowing new program generation.
9. **Visual Hierarchy:** Program metadata card -> Day cards -> Exercise list inside days.
10. **Typography:** Split names in bold, day names in primary text.
11. **Spacing:** 12dp between days, 8dp between exercises.
12. **Component Consistency:** Consistent cards and chips with the rest of the application.
13. **Navigation:** Bottom nav + back navigation.
14. **Accessibility:** Proper click labeling on program days.
15. **Animation:** Expansion animation on day accordion.
16. **Responsiveness:** Handles 3, 4, 5, or 6-day splits seamlessly.

---

### 6. Program Builder (`ProgramDetailScreen.kt` Custom Routine Sheet)
1. **Current Hierarchy:** Program name input -> Day tabs/list -> Exercise list per day -> Add Exercise picker -> Save button.
2. **Primary User Goal:** Build or customize an individualized training schedule.
3. **Most Important Action:** Add exercises and Save routine.
4. **Secondary Actions:** Remove exercise, adjust target sets/reps, reorder days.
5. **Information Overload:** Can become tedious without an intuitive picker.
6. **Empty State:** "Add your first exercise to Day 1" prompt.
7. **Loading State:** Instantaneous local state.
8. **Error State:** Validation highlighting empty routine name or empty days.
9. **Visual Hierarchy:** Top save button -> Day selector -> Exercise items with delete/reorder handles.
10. **Typography:** Clean input labels and clear day chips.
11. **Spacing:** 12dp spacing, sticky save action.
12. **Component Consistency:** Uses design system inputs and action buttons.
13. **Navigation:** Sheet dismiss with discard check.
14. **Accessibility:** Explicit remove labels for screen readers.
15. **Animation:** List reordering and add transitions.
16. **Responsiveness:** Fully usable inside ModalBottomSheet on all aspect ratios.

---

### 7. Progress Dashboard (`ProgressDashboardScreen.kt`)
1. **Current Hierarchy:** Date/period filter -> Consistency stats -> Volume chart -> PR highlights -> Muscle distribution -> Body measurements.
2. **Primary User Goal:** Review progress, progressive overload trends, and physical changes over time.
3. **Most Important Action:** Log body measurement or tap exercise for individual progression chart.
4. **Secondary Actions:** Filter time window (1M, 3M, 6M, 1Y, All), view PR history.
5. **Information Overload:** High if multiple charts render without clear sectioning.
6. **Empty State:** "No workout data yet — complete workouts to see volume and strength trends."
7. **Loading State:** Smooth progress indicators.
8. **Error State:** Graceful zero-state handling in custom Canvas charts.
9. **Visual Hierarchy:** Summary metrics (Sessions, Volume, PRs) -> Volume Overload Chart -> PR List -> Measurement trends.
10. **Typography:** Bold metric numbers (28sp bold) with subtle unit labels.
11. **Spacing:** 16dp spacing between cards.
12. **Component Consistency:** Charts share palette (`AccentBlue`, `CyanAccent`, `SurfaceCard`).
13. **Navigation:** Bottom bar + deep links to exercise history.
14. **Accessibility:** Accessible tabular data equivalents for visual Canvas charts.
15. **Animation:** Smooth line drawing and bar height transitions.
16. **Responsiveness:** Horizontal scrolling or responsive widths on charts.

---

### 8. Profile & Settings (`ProfileScreen.kt`)
1. **Current Hierarchy:** Profile Header (Avatar/Name/Level) -> Physical Stats (Weight, Height, Age) -> Training Preferences (Split, Equipment) -> App Settings (Units, Sound/Haptics) -> Data Management (Export/Import) -> About.
2. **Primary User Goal:** Configure physical profile, training preferences, and manage data.
3. **Most Important Action:** Edit user stats or export/import backup.
4. **Secondary Actions:** Change theme, toggle rest timer sounds, view app version.
5. **Information Overload:** Low; organized into clear categorized card groups.
6. **Empty State:** Default guest profile with "Edit Profile" prompt.
7. **Loading State:** Instant Flow emission from DataStore/Room.
8. **Error State:** In-place input validation on edit bottom sheet.
9. **Visual Hierarchy:** Grouped section cards with icon headers.
10. **Typography:** Category titles in 14sp SemiBold, values in TextPrimary.
11. **Spacing:** 12dp between preference rows, 16dp between cards.
12. **Component Consistency:** Switch, dialog, and chip styling align with design system.
13. **Navigation:** Bottom navigation destination.
14. **Accessibility:** Switches have linked labels; touch targets exceed 48dp.
15. **Animation:** Modal sheet entry and edit transitions.
16. **Responsiveness:** Fits comfortably on compact displays.

---

### 9. Recovery & Readiness (`ReadinessScreen.kt`)
1. **Current Hierarchy:** TopBar -> Readiness gauge / score card -> Component breakdown (Sleep, Soreness, Energy, Stress) -> Historical trend -> Log Readiness FAB.
2. **Primary User Goal:** Check physical readiness to auto-regulate workout intensity and volume.
3. **Most Important Action:** Log today's subjective readiness score.
4. **Secondary Actions:** View recovery tips, check historical readiness correlation.
5. **Information Overload:** Low; focused single-purpose screen.
6. **Empty State:** "No readiness recorded today — log your score to calibrate workout intensity."
7. **Loading State:** Circular loader during initial room query.
8. **Error State:** Clear error message with retry.
9. **Visual Hierarchy:** Large circular or linear readiness score (e.g., 8.5 / 10 "Optimal") -> 4 metric sliders.
10. **Typography:** 32sp bold score, color-coded status badges (Green = Ready, Amber = Moderate, Red = Fatigue).
11. **Spacing:** 16dp standard padding.
12. **Component Consistency:** Sliders and dialogs match dark slate theme.
13. **Navigation:** Back button to Home or Workout.
14. **Accessibility:** Slider values announced with clear verbal labels.
15. **Animation:** Score meter fill animation.
16. **Responsiveness:** Sliders and buttons adapt cleanly to portrait screens.

---

### 10. Workout History (`WorkoutHistoryScreen.kt`)
1. **Current Hierarchy:** TopBar -> Search & Date filter -> Incomplete workout banner -> Workout cards list with date, volume, duration, exercise summary.
2. **Primary User Goal:** Review past training sessions and performance.
3. **Most Important Action:** Tap workout to view detailed set logs.
4. **Secondary Actions:** Resume incomplete workout, export history, filter by date range.
5. **Information Overload:** Moderate; compact summary cards maintain readability.
6. **Empty State:** "Your training history starts here. Complete your first workout to begin."
7. **Loading State:** Centered indicator.
8. **Error State:** Fallback empty message.
9. **Visual Hierarchy:** Date badge -> Routine title -> Key stats (Duration, Volume, Sets) -> Exercises pill list.
10. **Typography:** 16sp bold workout titles, 12sp secondary stats.
11. **Spacing:** 10dp between cards.
12. **Component Consistency:** Consistent card container with `GymCoachColors.SurfaceCard`.
13. **Navigation:** Direct navigation to `WorkoutHistoryDetailScreen`.
14. **Accessibility:** Full session summary provided in content description.
15. **Animation:** Subtle item appearance animation.
16. **Responsiveness:** Works across all standard phone heights.

---

### 11. Workout History Detail (`WorkoutHistoryDetailScreen.kt`)
1. **Current Hierarchy:** TopBar -> Session Overview header -> Exercises performed list with completed sets and weights -> Share / Export action.
2. **Primary User Goal:** Analyze individual sets, weights, and reps achieved during a specific session.
3. **Most Important Action:** Review exercise performance or repeat workout.
4. **Secondary Actions:** Delete workout, share workout summary.
5. **Information Overload:** Low to moderate.
6. **Empty State:** "No sets recorded in this workout."
7. **Loading State:** Centered progress indicator.
8. **Error State:** "Workout not found" banner.
9. **Visual Hierarchy:** Session summary card -> Exercise cards with set tables.
10. **Typography:** Clear tabular layout for sets and weights.
11. **Spacing:** 12dp between exercises, 6dp between sets.
12. **Component Consistency:** Identical set table aesthetic to active workout.
13. **Navigation:** Back navigation.
14. **Accessibility:** High contrast set completion badges.
15. **Animation:** Smooth screen entry.
16. **Responsiveness:** Supports long exercise names without truncation.

---

### 12. Workout Summary / Completion Modal
1. **Current Hierarchy:** Celebration Header ("Workout Complete!") -> Summary Metrics (Total Volume, Duration, Sets Completed, PRs Broken) -> Finish / Share buttons.
2. **Primary User Goal:** Celebrate workout completion and understand session accomplishments.
3. **Most Important Action:** Return to Home or View Workout History.
4. **Secondary Actions:** Share workout summary.
5. **Information Overload:** Very low; high emotional reward.
6. **Empty State:** N/A.
7. **Loading State:** Instantaneous summary calculation.
8. **Error State:** N/A.
9. **Visual Hierarchy:** Trophy/Check icon -> Big metric numbers -> Action buttons.
10. **Typography:** 28sp bold celebration title, 20sp bold stats.
11. **Spacing:** 20dp padding, generous breathing room.
12. **Component Consistency:** Gold PR highlights and emerald success accents.
13. **Navigation:** Navigates cleanly back to Home or History.
14. **Accessibility:** Haptic feedback on completion, screen reader announcements.
15. **Animation:** Bouncy celebration spring, confetti/glow subtle accent.
16. **Responsiveness:** Modal or full-screen card fits standard viewports cleanly.

---

### 13. Rest Timer (`RestTimerCard` / Overlay)
1. **Current Hierarchy:** Circular or linear timer countdown -> Elapsed / Remaining seconds -> Quick adjust (+15s / -15s) -> Preset buttons (30s, 60s, 90s, 120s, 180s) -> Pause/Resume & Skip controls.
2. **Primary User Goal:** Rest for the optimal duration between sets without staring continuously at the phone.
3. **Most Important Action:** Glance at remaining rest, tap Skip when ready.
4. **Secondary Actions:** Add 15s or switch presets.
5. **Information Overload:** Minimal; ultra-clean timer.
6. **Empty State:** Hidden when not running.
7. **Loading State:** Real-time ticking via StateFlow.
8. **Error State:** N/A.
9. **Visual Hierarchy:** Giant remaining seconds (32sp black) -> Progress bar -> Control pills.
10. **Typography:** Tabular figures for countdown numbers.
11. **Spacing:** 10dp internal padding, compact footprint.
12. **Component Consistency:** Primary accent colored progress bar with dark background.
13. **Navigation:** Non-modal; persists in workout view without interrupting logging.
14. **Accessibility:** Vibrates on completion, TalkBack announces timer progress.
15. **Animation:** Smooth continuous linear progress bar.
16. **Responsiveness:** Compact horizontal layout fitting any screen width.

---

### 14. Real-time Form Analysis (`CameraPreviewScreen.kt` & `CameraOverlay.kt`)
1. **Current Hierarchy:** Live camera preview -> MediaPipe skeletal landmarks overlay -> Real-time form cues & angle metrics -> Rep counter -> Exit button.
2. **Primary User Goal:** Receive live visual and audio/haptic feedback on exercise technique.
3. **Most Important Action:** Perform exercise in front of camera and check rep count and feedback banner.
4. **Secondary Actions:** Switch camera (front/back), pause analysis, exit.
5. **Information Overload:** Must keep camera view clear of text clutter.
6. **Empty State:** "Step back until full body is in frame" guidance.
7. **Loading State:** Initializing camera and MediaPipe pose detector.
8. **Error State:** "Camera permission required" or "Pose model initialization failed".
9. **Visual Hierarchy:** Camera feed -> Green/Red skeleton lines -> Floating glassmorphic feedback pill at top.
10. **Typography:** Bold high-contrast HUD text with dark translucent backing.
11. **Spacing:** HUD elements docked to safe screen insets.
12. **Component Consistency:** Translucent dark pill design (`SurfaceDeep` with alpha).
13. **Navigation:** Floating back button to return to workout or exercise detail.
14. **Accessibility:** High-contrast skeleton colors (Neon Cyan and Amber), audio feedback.
15. **Animation:** Real-time 30fps landmark tracking and smooth rep increment bump.
16. **Responsiveness:** Auto-adjusts to portrait orientation with proper aspect ratio scaling.
