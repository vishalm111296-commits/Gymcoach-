package com.gymcoach.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    // Brand — kept as AccentBlue for graphics/icons (≥3:1); NOT used for text-on-container
    primary = AccentBlue,
    onPrimary = TextPrimary,
    primaryContainer = AccentBlueDark,
    onPrimaryContainer = TextPrimary,

    // Secondary — AccentBlueLight for subtle accents
    secondary = AccentBlueLight,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = TextPrimary,

    // Tertiary — SuccessGreen for success states
    tertiary = SuccessGreen,
    onTertiary = DarkBackground,

    // Background / Surface hierarchy
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,

    // Surface container hierarchy (M3 elevation layering)
    surfaceContainer = DarkCard,
    surfaceContainerHigh = DarkSurfaceVariant,
    surfaceContainerHighest = DarkSurfaceVariant,

    // Outline / divider
    outline = TextTertiary,
    outlineVariant = DarkSurfaceVariant,

    // Error — semantic container for error states (white on #B3261E = 6.53:1)
    error = ErrorRed,
    onError = TextPrimary,
    errorContainer = ErrorContainerDark,
    onErrorContainer = TextPrimary,

    // Elevation tint / scrim / inverse
    surfaceTint = AccentBlueDark,
    scrim = Color(0xCC000000),
    inverseSurface = TextPrimary,
    inverseOnSurface = DarkBackground,
    inversePrimary = AccentBlueLight
)

@Composable
fun GymCoachTheme(content: @Composable () -> Unit) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBackground.toArgb()
            window.navigationBarColor = DarkBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = GymCoachTypography, content = content)
}
