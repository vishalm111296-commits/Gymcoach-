# GymCoach UX Quality Checklist
This list acts as a checklist for the redesign team. Items are derived directly from the audited codebase of GymCoach @ 320d9905. Each line represents a specific UX/UI defect, behavior gap, or violation of target standards.

## Theme & Tokens
1. Stock unconfigured `darkColorScheme` is used; needs a custom dark charcoal palette (`Theme.kt` line 19).
2. Stock unconfigured `lightColorScheme` is used; needs custom light/dark theme pair or dark-only reinforcement (`Theme.kt` line 20).
3. Dynamic wallpaper-based styling is enabled; blocks dark charcoal target design (`Theme.kt` line 24).
4. System splash screen uses a light background style; causes flash on startup (`res/values/themes.xml` line 2).
5. All UI strings are hardcoded in Composables; needs complete extraction to `strings.xml`.
6. Spacing dimensions are hardcoded arbitrarily (2/4/8/10/12/16/20/24/32/40/48/80dp); needs a standard token grid.
7. Red (error color) is used decoratively for difficulty flame icons; must be errors/destructive actions only (`ExerciseItemCard.kt` line 77).
8. Red (error color) is used for "Common Mistakes" and "Safety Notes" headers; must use standard warnings/accent (`ExerciseDetailScreen.kt` lines 181, 196).

## Exercise List Screen
9. Screen lacks a scaffold, top bar, or consistent structure; needs standard container matching other screens (`ExerciseListScreen.kt` line 41).
10. Low information density per exercise card; needs thumbnail, primary muscle group indicators, and last-logged summary (`ExerciseItemCard.kt` line 24).
11. Horizontal scroll for categories lacks visual cut-offs or navigation depth; needs sticky groups or tab highlights (`ExerciseListScreen.kt` line 72).
12. Difficulty filter chips in the sheet will overflow and clip when items increase; needs wrapping layout (`ExerciseListScreen.kt` line 144).
13. Screen has no empty-state representation when searches return 0 items; needs illustration + query clear action (`ExerciseListScreen.kt` line 108).
14. Screen has no loading skeleton or placeholder when exercises load; needs standard shimmer indicator (`ExerciseListScreen.kt` line 108).
15. Discoverability of History, Progress, and Camera options is very poor; needs bottom navigation shell (`ExerciseListScreen.kt` lines 86-98).

## Exercise Detail Screen
16. Section details lack structured layouts; needs card blocks instead of raw vertical list of details (`ExerciseDetailScreen.kt` line 76).
17. Detail row renders the icon trailing at the bottom rather than leading on the side (`ExerciseDetailScreen.kt` line 243).
18. Screen lacks any media; needs video player integration or muscle map diagrams (`ExerciseDetailScreen.kt` line 67).
19. Exercise video player component exists but is completely unused; needs integration on detail screen (`components/ExerciseVideoPlayer.kt`).
20. Screen has no call to action (CTA); needs "Log Workout with this Exercise" or similar action button (`ExerciseDetailScreen.kt` line 67).
21. Screen has no personal record (PR) or user history indicator; needs PR data box (`ExerciseDetailScreen.kt` line 67).

## Workout Session Screen
22. Blank empty screen renders during initial VM loading when `currentWorkout` is null; needs progress loading state (`WorkoutSessionScreen.kt` line 107).
23. Elapsed workout duration displays at minute granularity ("0m" for first 60 seconds); needs seconds indicator (`WorkoutSessionScreen.kt` line 112).
24. Rest timer card uses inner and outer margins that break alignment with workout cards (`WorkoutSessionScreen.kt` line 133).
25. Rest timer progress bar colors are monochrome, lacking semantic distinction; needs warning/amber colors (`WorkoutSessionScreen.kt` line 163).
26. Swipe-to-dismiss deletes sets instantly with no confirmation or undo snackbar; needs undo/toast protection (`WorkoutSessionScreen.kt` line 348).
27. Touch target size for checkbox completion is 24dp; needs minimum 48dp bounding box (`WorkoutSessionScreen.kt` line 504).
28. Touch target size for set type selection is 24dp; needs minimum 48dp bounding box (`WorkoutSessionScreen.kt` line 513).
29. Star icon is used for set-type selection, carrying no clear semantic meaning; needs descriptive text or badge (`WorkoutSessionScreen.kt` line 527).
30. Grid columns do not align vertically with table headers because of mismatched weight values (`WorkoutSessionScreen.kt` lines 338-344 vs 457-463).
31. "Add Exercise" and "Complete Workout" buttons have identical visual priority; needs primary visual distinction on complete (`WorkoutSessionScreen.kt` lines 219-238).
32. Workout notes field has no visual bounds, looking like raw text; needs clean container layout (`WorkoutSessionScreen.kt` lines 205-213).
33. Completion screen shows a generic "Workout Complete!" text and single back button; needs workout stats summary page (`WorkoutSessionScreen.kt` line 75).

## Workout History Screen
34. Scaffold padding is applied twice, creating excessive blank space around list; needs flat container hierarchy (`WorkoutHistoryScreen.kt` lines 80, 166).
35. Sort dropdown trigger sits in a standalone full-width row, wasting vertical layout space; needs to be moved to app bar action (`WorkoutHistoryScreen.kt` line 125).
36. Card title falls back to "Workout" when notes are blank, resulting in lists of identical titles; needs index or weekday naming (`WorkoutHistoryScreen.kt` line 258).
37. Paused/incomplete workout banner is a single button with no metadata; needs descriptive stats of the paused session (`WorkoutHistoryScreen.kt` line 147).
38. History screen delete button matches the default primary confirmation style; needs destructive-red styling (`WorkoutHistoryScreen.kt` line 214).

## Workout History Detail Screen
39. Detail view loads blank due to synchronous StateFlow value reading bug; needs state collection observer (`WorkoutHistoryDetailViewModel.kt` line 52).
40. Edit workout icon launches the active workout session, which is conceptually confusing; needs edit-metadata flow (`WorkoutHistoryDetailScreen.kt` line 122).
41. StatItem notes display overflows horizontally when text is long; needs wrap or full-width notes card (`WorkoutHistoryDetailScreen.kt` line 290).

## Progress Dashboard Screen
42. Dash shows a grid of 10 gray cards displaying 0 values on new install; needs hero summary card and empty dashboard states (`ProgressDashboardScreen.kt` line 316).
43. "Weekly Trend" formatting error displays a raw double percent sign `0.0%%` on no-change trend (`ProgressDashboardScreen.kt` line 262).
44. "Est. Calories" is computed with an arbitrary raw scale factor `0.05 * volume`; needs real model calculation or removal (`ProgressDashboardScreen.kt` line 440).
45. Canvas volume chart has no scales, labels, or axis ticks; needs full axis labels and touch details (`ProgressDashboardScreen.kt` line 386).
46. "Top Exercises" section title is mislabeled, as it displays raw muscle group distributions (`ProgressDashboardScreen.kt` line 292).
47. Dashboard summary rows stack infinitely as simple lists; needs collapsible views or paging (`ProgressDashboardScreen.kt` line 273).
48. Screen has no reload action button on loading error states; needs retry CTA (`ProgressDashboardScreen.kt` line 203).

## Camera & Overlay
49. CameraOverlay UI is dead code and never composed on the camera screen; needs to be wired (`CameraPreviewScreen.kt` line 23).
50. Camera permission rejection leaves the screen as a blank black box with no warning; needs rationale dialogue and link to system settings (`CameraPreviewScreen.kt` line 43).
51. Camera preview binds and resets on every recomposition; needs lifecycle provider lifecycle hooks (`CameraPreviewScreen.kt` line 58).
52. Hardcoded colors for feedback tags are based on fragile string lookups; needs theme token color assignments (`CameraOverlay.kt` line 80).
53. Camera view lacks any top bar or close action to return to previous screens; needs back button chrome overlay (`CameraPreviewScreen.kt` line 49).
