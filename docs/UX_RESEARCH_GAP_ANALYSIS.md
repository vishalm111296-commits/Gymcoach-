# GymCoach - UX Research & Gap Analysis

## Overview
This document synthesizes UX patterns, feature sets, and UI layouts from industry-leading fitness applications (Strong, Hevy, Alpha Progression, Fitbod, FitNotes) and evaluates GymCoach Version 1.0 against these standards. 

## 1. Exercise Library
**Competitor Benchmark:** Rich visual lists with thumbnails, sticky alphabetical/category headers, multi-select pill filters, "Favorites" quick-toggles, and clear visual hierarchy for muscle groups vs. equipment.
**Current GymCoach:** Basic scrolling list of cards. Single-select bottom sheet filters. No images.
**Missing:** 
- Image placeholders / Thumbnails
- Color-coded badges for difficulty/muscle
- Favorites quick-toggle on list items
- Multi-select filtering
- Sticky category headers
**Priority:** High | **Difficulty:** Low | **ETA:** 2 hours

## 2. Exercise Detail Screen
**Competitor Benchmark:** Hero image/video header, interactive muscle heatmaps, 1RM progression charts, historical performance logs (previous sets), and "Replace/Alternative" suggestions.
**Current GymCoach:** Text-heavy scrollable column with basic DetailRow components.
**Missing:**
- Hero image placeholder
- Visual muscle targeting (SVG or colored chips)
- Previous performance history / 1RM chart integration
- "Add to Workout" direct action
- Variations / Alternatives
**Priority:** Medium | **Difficulty:** Medium | **ETA:** 3 hours

## 3. Workout Session (The Core Loop)
**Competitor Benchmark:** Highly optimized for one-handed gym use. Collapsible exercise groups, swipe-to-delete sets, inline "Previous Performance" ghost text, set types (Warm-up, Drop set, Failure), plate calculators, and persistent floating rest timers.
**Current GymCoach:** Static list of rows. No collapsing. Rest timer is a static card injected into the list. No swipe gestures. No set types.
**Missing:**
- Collapsible exercise cards
- Swipe-to-delete sets with Undo Snackbar
- Set types (Warm-up, Normal, Drop, Failure)
- Ghost text showing previous weight/reps
- Floating, persistent Rest Timer
- Finish confirmation & Workout Summary Dialog
- Plate Calculator
**Priority:** Critical | **Difficulty:** High | **ETA:** 6-8 hours

## 4. Workout History
**Competitor Benchmark:** Calendar view, weekly activity rings, expandable workout cards, quick "Duplicate/Perform Again" action, and social sharing.
**Current GymCoach:** Flat chronological list with basic filters.
**Missing:**
- Calendar / Monthly timeline view
- "Perform Again" (Duplicate) action
- Swipe actions (Delete/Share)
- Visual thumbnails of exercises performed
**Priority:** Medium | **Difficulty:** Medium | **ETA:** 4 hours

## 5. Progress & Statistics
**Competitor Benchmark:** Interactive graphs with timeframe toggles (1M, 3M, 6M, 1Y, All), 1RM trendlines, muscle recovery heatmaps, and gamified streak/achievement badges.
**Current GymCoach:** Basic Canvas line chart and static text grids.
**Missing:**
- Interactive chart libraries (e.g., Vico or MPAndroidChart equivalents in Compose)
- Achievements engine & UI
- Streak visualizer
- Muscle distribution pie/bar charts
**Priority:** High | **Difficulty:** High | **ETA:** 6 hours

## 6. Settings & General UX
**Competitor Benchmark:** Material 3 dynamic theming, dark mode toggles, unit switching (kg/lbs), auto-rest timer defaults, haptic feedback, and data export.
**Current GymCoach:** No settings module. No haptics.
**Missing:**
- Complete Settings Module (Data store / Preferences)
- Haptic feedback (Vibrations on rest timer complete / set complete)
- Sound alerts
- Empty state illustrations
**Priority:** Medium | **Difficulty:** Low | **ETA:** 3 hours

---

# Implementation Roadmap (Prioritized)

1. **Phase 1: The Premium Workout Experience (Workout Session)**
   *Impact: Highest. This is where users spend 90% of their time.*
   - Implement Collapsible Exercise Cards.
   - Implement Swipe-to-delete for sets.
   - Upgrade Rest Timer to a persistent/floating UI with Pause/Skip.
   - Implement Finish Workout confirmation & Summary Dialog.
   - Add haptic feedback for set completion.

2. **Phase 2: Exercise Library & Detail Polish**
   *Impact: High. First impressions matter.*
   - Add visual placeholders (Hero images, thumbnails).
   - Implement color-coded Muscle/Difficulty badges.
   - Add "Favorite" toggle.

3. **Phase 3: Settings & Preferences**
   *Impact: Medium. Essential for production apps.*
   - Build Settings screen (Units, Theme, Timer defaults).

4. **Phase 4: History & Progress Upgrades**
   *Impact: Medium.*
   - Add "Perform Again" to History.
   - Add Pie charts for Muscle Distribution.

