# GymCoach - Premium Dark Fitness Design System

## 1. Visual Strategy & Aesthetic Identity
GymCoach adopts a **Premium Dark Athletic** aesthetic. 
*   **Theme Principle**: Ultra-low ambient light emission to prevent eye strain in dark gym environments. High-performance high-contrast accent highlights (Volt Yellow/Neon Green) to draw attention to critical actions (e.g., ticking timer, set completion).
*   **Visual Depth**: Material 3 elevation models are simulated via dark translucent glass overlays, subtle borders, and background color gradations (deep navy-grays) rather than traditional dropshadows.

---

## 2. Design Tokens

### A. Color Palette
```kotlin
// Brand Primaries & High Performance Accents
val NeonVolt = Color(0xFFD4FF00)       // Critical actions, timer highlights, active states
val GraphiteDeep = Color(0xFF0F0F12)   // Main screen background
val CarbonGray = Color(0xFF1C1C24)     // Base card & sheet container surface
val SlateMuted = Color(0xFF2E2E3A)     // Text field inputs & divider borders

// Semantic & Status Tokens
val EnergyAmber = Color(0xFFFF9E00)    // Drop sets, caution notices, secondary priorities
val FailureCrimson = Color(0xFFFF3B30)  // Failure sets, delete actions, system errors
val RecoveryBlue = Color(0xFF007AFF)   // Rest timer active, warmup sets

// Typography Contrast Scales
val PureWhite = Color(0xFFFFFFFF)      // Primary header text
val SilverText = Color(0xFFE5E5EA)     // Subtitles, descriptive paragraphs
val CharcoalText = Color(0xFF8E8E93)   // Placeholder labels, unit markers, disabled states
```

### B. Typography Scales
```kotlin
val DisplayLarge = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 34.sp,
    lineHeight = 42.sp,
    letterSpacing = (-0.5).sp
)
val SectionHeader = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    letterSpacing = 0.25.sp
)
val ActiveLabel = TextStyle(
    fontFamily = FontFamily.Monospace, // Monospace prevents layout shifting during ticking timers/logs
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 22.sp
)
```

---

## 3. Component Architecture & Specifications

### A. Workout Set Logger Component
To maximize single-handed interaction, the logging card operates on a clear column-span structure:
```
┌────────────────────────────────────────────────────────┐
│ EXERCISE TITLE                                 [Delete]│
│ [Badges: Chest · Intermediate]                         │
├───────┬────────────┬────────────┬───────────┬──────────┤
│ SET   │ WEIGHT(kg) │ REPS       │ RPE       │ STATUS   │
├───────┼────────────┼────────────┼───────────┼──────────┤
│ 1 (N) │ [ 80.0   ] │ [   8    ] │ [  8.5  ] │  [  x ]  │
│ 2 (D) │ [ 70.0   ] │ [  10    ] │ [  9.0  ] │  [ [x]]  │
└───────┴────────────┴────────────┴───────────┴──────────┘
```
*   **Set Indicators**: `W` (Warmup - Blue), `N` (Normal - White), `D` (Drop - Amber), `F` (Failure - Crimson). Tap indicator label directly cycles the set type.
*   **Active Keyboard Forwarding**: When completing weight input, pressing keyboard IME Action Next forwards keyboard focus immediately to Reps, and subsequently to RPE.

### B. Persistent Rest Timer HUD
*   **Architecture**: Detached from scrolling lists. Placed at the bottom viewport position, spanning full width.
*   **Interactive Controls**: Fast rest modification buttons (`+30s` / `-30s`), pause/resume toggle, and a skip action.
*   **Visual States**:
    *   *Normal (Ticking)*: Glow animation around recovery blue container progress bar.
    *   *Under 10s (Alert)*: Progress bar color transitions to Neon Volt.
    *   *Completed*: Short high-intensity haptic vibration sequence, container fades out gracefully.

### C. Previous Performance Ghost Display
*   **Visual Design**: Rendered as a background placeholder or light gray subtitle directly beneath active logging inputs.
*   **Text Representation**: `Prev: 80kg x 8`
*   **Logic**: Pulls historical set data matching the workout template sequence or last recorded session database ID.

---

## 4. Mobile UX Flows & Transitions
1.  **Start Workout**: Smooth slide-up transition from bottom navigation. Focus automatically targets first exercise's initial weight input field.
2.  **Toggle Set Completion**: Haptic feedback click. Rest timer launches as sticky bottom layout if rest duration is specified. Keyboard cursor hides.
3.  **Finish Workout**: Displays dialog requiring confirmation. Triggers slide-left transition into the Workout Summary screen.
