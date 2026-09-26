package com.gymcoach.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private val DarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = SurfaceDeep,
    primaryContainer = CyanAccent,
    onPrimaryContainer = SurfaceDeep,
    secondary = AmberAccent,
    onSecondary = SurfaceDeep,
    secondaryContainer = AmberAccent,
    onSecondaryContainer = SurfaceDeep,
    tertiary = Emerald,
    onTertiary = SurfaceDeep,
    background = SurfaceDeep,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = ErrorRed,
    onError = TextPrimary,
    errorContainer = ErrorRed,
    onErrorContainer = TextPrimary
)

@Composable
fun GymCoachTheme(content: @Composable () -> Unit) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            @Suppress("DEPRECATION")
            window.statusBarColor = SurfaceDeep.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = SurfaceDeep.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }
    // Note: Typography and Shapes will be supplied correctly when we define them.
    MaterialTheme(colorScheme = colorScheme, typography = GymCoachTypography, content = content)
}
