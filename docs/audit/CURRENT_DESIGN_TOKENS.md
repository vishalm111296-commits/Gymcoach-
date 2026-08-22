# Current Design Tokens vs Target — GymCoach @ 320d9905

## Colors — CURRENT
Source: `app/src/main/kotlin/com/gymcoach/app/ui/theme/Theme.kt`
| Token | Current Value | Origin |
|---|---|---|
| DarkColorScheme | `darkColorScheme()` — unconfigured stock values | Jetpack Compose SDK |
| LightColorScheme | `lightColorScheme()` — unconfigured stock values | Jetpack Compose SDK |
| Dynamic Theme | Enabled by default (`dynamicColor = true`) | Wallpaper-derived (varies per user) |
| Hardcoded | `good`: Color(0xFF4CAF50)<br>`warn`: Color(0xFFFFC107)<br>`error`: Color(0xFFEF5350) | `camera/CameraOverlay.kt` (FeedbackColors) |
| System Splash | `android:Theme.Material.Light.NoActionBar` | `res/values/themes.xml` (forces light splash) |

### Color Gaps & Violations
1. **No Brand Theme**: Charcoal, warm-white, and blue/violet accent tokens do not exist in the code.
2. **Dynamic Color Hijack**: System dynamic color overrides app-intent design entirely.
3. **Improper Red (Error) Usage**:
   - Red used for "Difficulty" text and icons on EVERY advanced exercise item in list (`ExerciseItemCard.kt` line 77).
   - Red used for "Common Mistakes" and "Safety Notes" headers in Detail screen (`ExerciseDetailScreen.kt` lines 181, 196).
   - Red should be reserved *strictly* for destructive actions (Delete confirmation buttons) and system errors.
4. **Hardcoded Color Off-Tokens**: Camera overlay bypasses M3 theme system entirely with hardcoded ARGB values.

## Colors — TARGET SYSTEM
- **Background**: Deep charcoal (e.g., `#121212` or `#1E1E1E`).
- **Primary Text**: Warm white (e.g., `#F5F5F5` or `#ECEFF1`).
- **Primary Action Accent**: Restrained blue/violet (e.g., `#3F51B5` / `#6200EE`).
- **Positive (Semantic)**: Green (e.g., `#4CAF50` / `#81C784`).
- **Warning (Semantic)**: Amber (e.g., `#FFC107` / `#FFD54F`).
- **Destructive/Error**: Red (e.g., `#E53935`).

---

## Typography — CURRENT
Source: `app/src/main/kotlin/com/gymcoach/app/ui/theme/Theme.kt`
- Currently instantiates standard M3 typography stack (`typography = Typography()`).
- No custom typefaces or weight configurations loaded.
- Component-level overrides use ad-hoc weights (`FontWeight.Bold` used ~15 times on `titleMedium` / `headlineMedium` / `bodyLarge` / `labelSmall`).
- Double percent-sign formatting bug in Progress screen (`ProgressDashboardScreen.kt` line 262): `"• 0.0%%"` string is written as-is, escaping the second percent sign but skipping `.format()`, displaying raw `0.0%%`.

## Typography — TARGET SYSTEM
- **Primary Typeface**: System default sans-serif (Roboto/Inter).
- **Hierarchy Rules**:
  - Restrain Bold weight. Use medium or semi-bold for secondary items to preserve visual hierarchy.
  - Heading styles must consistently map to UI sections.
  - Minimum body copy: 14sp.
  - Table and detail labels: ≥12sp.

---

## Spacing & Dimensions — CURRENT
Source: Composable layouts
- Standard spacing uses hardcoded values scattered in `Spacer` and `padding` modifiers.
- Values used: `2.dp`, `4.dp`, `8.dp`, `10.dp`, `12.dp`, `16.dp`, `20.dp`, `24.dp`, `32.dp`, `40.dp`, `48.dp`, `80.dp`. No centralized spacing scale tokens.
- Touch target violations: Checkboxes and toggle buttons styled as small as `24.dp` size (`WorkoutSessionScreen.kt` lines 504, 513).
- Padding bugs: Scaffold padding applied multiple times (`WorkoutHistoryScreen.kt` line 80 & 166).

## Spacing & Dimensions — TARGET SYSTEM
- Centralize dimension tokens in a theme helper or object (e.g., `Grid_4 = 4.dp`, `Grid_8 = 8.dp`, etc.).
- **Touch Targets**: All interactive elements (clicks, checks, deletes, swaps) must guarantee a minimum bounding box of 48×48dp (per WCAG 2.1).
- **Spacing**: Generous but controlled margins (16dp baseline, 24dp section breaks, 8dp card internals). No double-padding stack errors.
