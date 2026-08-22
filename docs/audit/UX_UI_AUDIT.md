# GymCoach UX/UI Audit — Code-Based
Repo: vishalm111266-beep/GymCoach @ main (320d9905). Static analysis of Compose source. No screenshots.
Scope: 6 screens + components + theme + nav.

## 0. Foundation: Theme Is Stock Template

`app/src/main/kotlin/com/gymcoach/app/ui/theme/Theme.kt` (whole file, 33 lines):
- `private val DarkColorScheme = darkColorScheme()` — default M3 palette. No brand colors exist.
- `private val LightColorScheme = lightColorScheme()` — default M3 baseline purple.
- `dynamicColor: Boolean = true` → on Android 12+, wallpaper-derived palette overrides everything. Brand is whatever the user's wallpaper is.
- `typography = Typography()` — default M3 type scale. No font customization.
- Target design system (charcoal bg, warm white text, blue/violet accent, green positive, amber warn, red=errors-only) is **implemented nowhere**.

`app/src/main/res/values/colors.xml`: only `black`/`white`.
`app/src/main/res/values/themes.xml`: `parent="android:Theme.Material.Light.NoActionBar"` — LIGHT platform theme; guarantees white flash/splash behind a dark Compose UI.

Consequence: every screen below inherits random/dynamic colors; "color usage" judgments reference what the code *requests* (`primary`, `error`, `surfaceVariant`), which resolves to M3 baseline or wallpaper colors.

---

## 1. ExerciseListScreen
File: `app/src/main/kotlin/com/gymcoach/app/presentation/list/ExerciseListScreen.kt`

1.1 Visual hierarchy — Flat Column: search field, tab row, list. No Scaffold, no LargeTopAppBar, no section headers. All cards identical gray-on-gray (`surfaceContainerLow`); nothing signals category transitions.
1.2 Information density — LOW. `ExerciseItemCard` shows name + muscleGroup + difficulty only. One exercise ≈ 2 short lines in a full-width card + 8dp outer padding (`Modifier.fillMaxWidth().padding(8.dp)`) → ~40% wasted card surface. No equipment, no last-used date, no PR, no thumbnail.
1.3 Empty states — ABSENT. `viewModel.exercises` starts `emptyList()`; filtered-to-zero results render infinite blank. No "no results for X".
1.4 Loading states — ABSENT. No spinner/skeleton before first emission.
1.5 Error states — ABSENT. No error surface anywhere in this screen.
1.6 Touch targets — 3 IconButtons (`CameraAlt`, `History`, `Insights`) jammed onto the same row as `ScrollableTabRow` (default 48dp but visually cramped, no labels); FilterChip default height 32dp — below 48dp recommendation.
1.7 Color — difficulty icon `tint = MaterialTheme.colorScheme.error` in `ExerciseItemCard` (red flame on EVERY card): red used decoratively, violates "red = errors/destructive only". Accent otherwise unused; no positive/amber semantics.
1.8 Defects:
- Filter sheet: difficulty chips in a non-scrolling single `Row` (`horizontalArrangement = Arrangement.spacedBy(8.dp)`) → overflows off-screen with >3–4 options. Equipment row scrolls; difficulty row doesn't.
- "Clear Filters" resets values but leaves sheet open; no visual selected-filter summary.
- Trailing `Spacer(Modifier.height(32.dp))` hard-coded as nav-bar inset hack.
- Comment admits it: "// Equipment options can be long… We'll use a simple horizontal scroll".
1.9 Nav pattern — global destinations (History/Progress/Camera) are 3 unlabeled icons appended to a category tab row. Discoverability ~zero; violates "strong hierarchy"; needs bottom navigation or drawer.

## 2. ExerciseDetailScreen
File: `app/src/main/kotlin/com/gymcoach/app/presentation/detail/ExerciseDetailScreen.kt`

2.1 Hierarchy — pure text stack: Description → 4 DetailRows → Instructions/Tips/Mistakes/Safety walls. Section titles `titleMedium + Bold + primary`; body `bodyLarge`. Uniform → no scannable hierarchy.
2.2 Density — TEXT-HEAVY, ZERO MEDIA. No image/GIF/video. `components/ExerciseVideoPlayer.kt` EXISTS but is dead code (0 references — verified by reading all call sites + repo search). No muscle diagram.
2.3 Empty state — "Exercise not found." plain `bodyLarge` text, no action.
2.4 Loading — ABSENT: `exercise == null` renders nothing under the bar; title falls back to literal "Exercise".
2.5 Errors — none (repository failure silently blank).
2.6 Touch targets — no interactive elements besides back button. No CTA at all: cannot "Log this exercise" / "Start" from detail.
2.7 Color — "Common Mistakes" + "Safety Notes" headers tinted `error` (red) — borderline semantic misuse; body stays `onSurface`. DetailRow icons tinted `primary`.
2.8 Defect — `DetailRow` renders icon BELOW the value text:
```
Text(label); Spacer; Text(value); Spacer; Icon(...)   // icon trails like a stray glyph
```
Icon belongs beside the label. Looks broken.
2.9 Defect — `ExerciseDetailViewModel` declared inside the Screen file; not UX but signals screen was built fast.
2.10 Missing per brief: personal records for this exercise, history snippet, form-analysis entry point.

## 3. WorkoutSessionScreen
File: `app/src/main/kotlin/com/gymcoach/app/presentation/workout/WorkoutSessionScreen.kt`

3.1 LARGE EMPTY SPACE — confirmed:
- While loading (`currentWorkout == null`) the `LazyColumn` body renders nothing → full blank screen. `CircularProgressIndicator` is IMPORTED (line ~46) and NEVER USED — proof loading state was planned, never built.
- Stray filler: `item { Spacer(Modifier.height(16.dp)) }` unconditional blank item inside the LazyColumn.
- New workout with 0 exercises = blank canvas + two bottom buttons. No guidance, no quick-start suggestions.
3.2 Resume pattern — exists but inverted/weak: `loadOrStartWorkout(null)` silently hijacks into `getLatestIncompleteWorkout()` (WorkoutLoggingViewModel.kt) — user tapping "start workout" resumes an old one with no notice. In History screen resume is one lonely centered `Button("Resume Workout")`. No banner/card showing paused workout name/age/progress.
3.3 Rest timer card — rendered INSIDE the LazyColumn as an item with its own extra `padding(horizontal = 16.dp, vertical = 8.dp)` on top of list `contentPadding = PaddingValues(16.dp)` → double-inset, misaligned with cards below. Timer counts DOWN into `LinearProgressIndicator` (`timeRemaining / totalDuration`) so the bar empties — acceptable but combined with `primaryContainer` bg + `onPrimaryContainer` progress it's monochrome noise. `PlayArrow` icon reused for both "Rest" label AND resume action.
3.4 Table misalignment — header weights `0.15/0.2/0.2/0.15/0.15 + Spacer(40dp)` vs row weights `0.18/0.18/0.13/0.13 + Row(56dp)`. Columns DO NOT align with their labels.
3.5 Touch targets — VIOLATIONS: `Checkbox(modifier = Modifier.size(24.dp))`, set-type `IconButton(modifier = Modifier.size(24.dp))` with 16dp `Star` icon. Half the 48dp minimum, mid-workout, sweaty hands. `Star` icon for "cycle set type" is semantically meaningless.
3.6 Destructive UX — `SwipeToDismissBox` deletes a set INSTANTLY on swipe either direction, no undo, no confirmation; `rememberSwipeToDismissBoxState` created per-index inside `forEachIndexed` → state reuse risk when list mutates. Remove-exercise is a bare `Close` IconButton — one tap nukes exercise + all its sets.
3.7 Completion flow — "Complete Workout" and "Add Exercise" have equal `weight(1f)` and near-equal prominence; finishing is the higher-stakes action yet visually identical. Post-completion screen = bare "Workout Complete!" + "Go Back" — no summary (duration/volume/PRs) of the thing you just did.
3.8 Errors — generic `AlertDialog(title="Error", text=rawExceptionMessage)`.
3.9 Session timer — `formatDuration(elapsedSeconds)` has MINUTE granularity: first 59s of a workout the header reads "0m". Dead feedback for an active session.
3.10 Rest auto-start — `toggleSetCompletion` force-starts a 90s rest (`defaultRestSeconds = 90`, hard-coded) on every check; no setting, no skip-first, no per-exercise default surfaced in UI.
3.11 Picker — exercise chooser is an `AlertDialog` with plain LazyColumn: no search, no filters, no muscle-group grouping, awkward for 100+ exercises.

## 4. WorkoutHistoryScreen
File: `app/src/main/kotlin/com/gymcoach/app/presentation/history/WorkoutHistoryScreen.kt`

4.1 DOUBLE PADDING BUG — outer `Column(...).padding(padding)` AND `LazyColumn(modifier = Modifier.weight(1f).padding(padding).padding(horizontal = 16.dp))`: Scaffold insets applied TWICE to the list → oversized dead margins on every side. Empty-state branch adds `.padding(padding)` a THIRD time.
4.2 Layout defect — Sort `IconButton` sits in its own full-width `Box(wrapContentSize(TopEnd))` row below tabs: a stranded right-aligned gear row wasting ~48dp height. Belongs in TopAppBar `actions`.
4.3 Card identity — `HistoryWorkoutCard` title = `workout.notes.ifBlank { "Workout" }`: free-text notes become the record TITLE, then the SAME notes render again below (`maxLines = 2`). Every untitled workout is called "Workout" — list of identical cards. No weekday/date prominence, no completion badge for incomplete sessions.
4.4 Empty state — present but minimal: single text line, no illustration/CTA ("Start your first workout").
4.5 Loading/error — ABSENT (list flow starts empty; looks identical to "no workouts").
4.6 Destructive styling — delete confirmation `confirmButton = Button(onClick={confirmDelete})` DEFAULT colors: the destructive action is styled as primary affirmative. Same in detail screen. Red required.
4.7 Resume — lone centered button (§3.2). CUSTOM filter tab opens DatePickerDialog even when already selected; no chip showing active custom range; active sort invisible until dropdown opened.
4.8 Positives (keep): search + tabs + sort + date-range exist; `key = { it.id }` used; empty-vs-search-empty differentiated copy.

## 5. WorkoutHistoryDetailScreen
File: `app/src/main/kotlin/com/gymcoach/app/presentation/history/WorkoutHistoryDetailScreen.kt`

5.1 CRITICAL LOAD BUG — `loadWorkout()`:
```kotlin
workoutRepository.getWorkoutWithDetails(workoutId)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null).value
```
Reads `.value` of a JUST-created StateFlow → initial `null` captured synchronously; UI receives `isLoading=false, workout=null` → `state.workout?.let { … }` renders NOTHING. Blank screen below app bar (only Edit/Delete still work). Race-dependent; effectively broken detail view.
5.2 Header — same notes-as-title defect (§4.3); `StatItem(label="Notes", value=workout.notes)` shoves unlimited-length notes into a horizontal stat row → overflow/breaks layout.
5.3 Table — header says "Weight/Reps/RPE/Rest(s)" but RPE cell prints `"RPE ${"%.1f".format(rpe)}"` (redundant prefix), completed marker is text-glyph `"✓"` in a fixed 24dp slot; incomplete sets get blank space — column of ghosts.
5.4 Edit semantics — toolbar `Edit` navigates to `Routes.workoutSession(id)` i.e. the LIVE session screen: "editing history" restarts/resumes a completed workout as active session. Conceptually wrong destination.
5.5 Delete — `confirmDelete()` launches async delete then `onBackClick()` fires IMMEDIATELY: pop happens before delete commits (crash/delete-fail window). Confirm button not error-red.
5.6 Unused/dead: `showDeleteConfirmation` StateFlow collected but ignored (screen re-collects `deleteTarget` inline); imports `Canvas/Path/Stroke*` unused — leftover chart code copied in.
5.7 Duration formatting — `formatDuration` drops seconds: any workout < 60s renders "0m".

## 6. ProgressDashboardScreen
File: `app/src/main/kotlin/com/gymcoach/app/presentation/progress/ProgressDashboardScreen.kt`

6.1 GRID OF ZERO CARDS — confirmed. `StatsOverview` = 5 Rows × 2 `StatCard`s = TEN identical `surfaceVariant` gray cards (Workouts/Today/Week/Month/Exercises/Sets/Reps/Volume/Time/Est.Calories). Fresh install: ten big bold ZEROS. No hero metric, no grouping, no conditional hiding.
6.2 FORMATTING BUG — exact string found:
```kotlin
val trendSymbol = when {
    state.weeklyTrend > 0 -> "▲ +%.1f%%".format(state.weeklyTrend)
    state.weeklyTrend < 0 -> "▼ %.1f%%".format(state.weeklyTrend)
    else -> "• 0.0%%"          // ← BUG: no .format(), %% stays literal
}
```
Zero-trend users see literal **“• 0.0%%”**. Also ▲/▼ glyphs carry ALL semantics — no green/red differentiation (and brief wants green=positive, amber=warn here).
6.3 FABRICATED METRIC — `StatCard("Est. Calories", "%.0f".format(totalVolume * 0.05))` — invented coefficient presented as data. Decorative/fake; remove or model properly.
6.4 MISLEADING LABELS — `calculateWorkoutFrequency` returns `weekly.size` (# of weeks WITH data), displayed as "Weekly Workouts" (reads as workouts-per-week). `weeklyTrend` compares only last 2 buckets.
6.5 CHART — hand-rolled Canvas `VolumeLineChart`: NO axis labels, NO value labels, NO date ticks, NO touch inspection; y-range min→max normalizes away absolute level (flat 3-week decline looks identical to flatline at 0). Dots radius `4f` raw px. Grid = 3 unlabeled hairlines.
6.6 LIST-AS-DASHBOARD — Weekly/Monthly summaries and "Top Exercises" each render EVERY entry as its own full-width gray `SummaryRow` card → hundreds of identical stacked cards; monthly grows forever. Section titled "Top Exercises" actually renders `muscleGroupDistribution` (muscle groups, mislabeled).
6.7 Empty handling — good instinct: per-section `EmptyPlaceholder("No volume data yet")` text. But plain gray text; dashboard-as-a-whole still leads with the zero-grid instead of an onboarding/empty dashboard state.
6.8 Loading/Error — centered spinner; error = raw `state.error!!` text in red. `refresh()` EXISTS in VM but NO retry button anywhere — unrecoverable error state.
6.9 Extremes row — if only `longestWorkout` exists, its card sits alone at half width; both-null still prints "Workout Extremes" header over nothing.

## 7. CameraPreviewScreen + CameraOverlay
Files: `app/src/main/kotlin/com/gymcoach/app/presentation/camera/CameraPreviewScreen.kt`, `CameraOverlay.kt`

7.1 CameraOverlay IS DEAD CODE — 0 references (NavHost routes CAMERA → `CameraPreviewScreen()` only; CameraPreviewScreen never composes `CameraOverlay`). Rep counter + form feedback UI exists but unreachable.
7.2 Hardcoded colors bypassing theme — `FeedbackColors`: `Color(0xFF4CAF50)` green, `Color(0xFFFFC107)` amber, `Color(0xFFEF5350)` red, chosen by EXACT string match against English phrases (`feedback.trim().lowercase() in good`) — any analyzer phrasing change silently flips feedback to red (else branch). Fragile + off-token.
7.3 Permission UX — denied/revoked state: blank black box. No rationale, no settings deep-link, no retry (launcher fires once via DisposableEffect).
7.4 Chrome — no TopAppBar, no close/cancel button, no route back except system gesture. Full-screen dead end.
7.5 Bug — `ProcessCameraProvider.getInstance(...).addListener(...)` runs INSIDE `AndroidView.update {}` → re-binds camera on EVERY recomposition; `unbindAll()` churn. Also `LocalLifecycleOwner` deprecated import path; unused trailing DisposableEffect.

## 8. Shared Components
- `ExerciseItemCard.kt` — see §1.2/§1.7. `difficulty` mapped to red flame regardless of level (beginner = same red as advanced). No elevation/tonal variation; outer `.padding(8.dp)` fights list `spacedBy(8.dp)` → uneven 16/8 rhythm.
- `ExerciseVideoPlayer.kt` — dead component (0 usages); busy-wait poller `while(true){ delay(200ms) }`; would be the media hook detail screen lacks.
- Repeated private duplicates: `SectionHeader`, `SetRow`, `StatItem`, `formatDate/formatDuration` re-implemented in history + progress files → drift risk (already diverged: session SetRow vs detail SetRow).

## 9. Global / Navigation
`ui/GymCoachNavHost.kt`: startDestination = list; hub-and-spoke via 3 unlabeled icons (§1.9); no bottom bar, no FAB convention, no shared Scaffold → inconsistent insets (list/detail-no-scaffold vs others Scaffold). `strings.xml` essentially unused — ALL copy hard-coded in composables (i18n impossible). Status-bar/edge-to-edge unmanaged. Dark/light follows system with stock schemes (§0) → app appearance unpredictable per device.

## 10. Verdict: Redesign vs Incremental

| Screen | Verdict |
|---|---|
| Theme (`Theme.kt`) | **REWRITE** (foundation; blocks everything) |
| WorkoutSessionScreen | **COMPLETE REDESIGN** (blank states, 24dp targets, misaligned table, rest-timer placement, completion flow) |
| ProgressDashboardScreen | **COMPLETE REDESIGN** (zero-grid, fake calories, unlabeled chart, %% bug, card-per-row lists) |
| ExerciseListScreen | **COMPLETE REDESIGN** (nav shell, density, empty/loading, filter sheet) |
| CameraPreviewScreen | **COMPLETE REDESIGN** (chrome, permission, wire overlay) |
| ExerciseDetailScreen | Incremental (add media, fix DetailRow icon order, add CTA/PR, loading state) |
| WorkoutHistoryScreen | Incremental (fix double-padding, move sort, real titles, red delete, loading) |
| WorkoutHistoryDetailScreen | Incremental BUT fix critical §5.1 load bug first |
| Components | Extract shared SectionHeader/SetRow/stat primitives; delete or wire VideoPlayer/CameraOverlay |

## 11. Required Improvements (mapping to user brief Phases 14–16)
- P14 foundation: implement charcoal/warm-white/blue-violet token set in `darkColorScheme(...)` explicitly; disable dynamic color; warm-white `onBackground`; accent ONLY on primary actions/highlights; green=positive deltas/PRs/completed; amber=rest-timer/warnings/streak risk; red strictly errors/destructive; replace `Material.Light.NoActionBar` with dark splash-safe theme.
- P14 spacing/type: define spacing scale (4/8/12/16/24/32) — current mix of 4/8/12/16/24/32/40/48 ad hoc; type ramp with restrained Bold (Bold currently on ~15 styles); min body 14sp, table labels ≥12sp.
- P14 touch: ≥48dp for checkbox/set-type/swipe alternatives; replace 24dp controls (§3.5).
- P15 screens: Session = persistent rest-timer bar (not list item), aligned editable grid, undo-snackbar deletes, summary completion screen, visible loading, resume banner. Dashboard = hero metric + ≤4 KPIs, labeled chart w/ date axis, PR highlights, empty-dashboard onboarding; kill Est.Calories fake stat; fix "• 0.0%%". List = dense 2-line rows w/ meta chips + green/amber difficulty, bottom-nav shell, loading shimmer, true empty state. Detail = media (wire VideoPlayer or images), icon-left DetailRows, Log-this-exercise CTA, this-exercise PR block. Camera = overlay wiring, permission UI, close button.
- P16 polish: strings.xml extraction, consistent destructive-red dialogs, retry affordance on errors, seconds-accurate session timer, shared component library.
