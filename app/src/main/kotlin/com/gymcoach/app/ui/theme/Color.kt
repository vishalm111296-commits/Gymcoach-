package com.gymcoach.app.ui.theme

import androidx.compose.ui.graphics.Color

// Deep charcoal backgrounds
val DarkBackground = Color(DesignTokens.DarkBackground)
val DarkSurface = Color(DesignTokens.DarkSurface)
val DarkSurfaceVariant = Color(DesignTokens.DarkSurfaceVariant)
val DarkCard = Color(DesignTokens.DarkCard)

// Warm white primary text
val TextPrimary = Color(DesignTokens.TextPrimary)
val TextSecondary = Color(DesignTokens.TextSecondary)
val TextTertiary = Color(DesignTokens.TextTertiary)

// Restrained blue/violet accent
val AccentBlue = Color(DesignTokens.AccentBlue)
val AccentBlueLight = Color(DesignTokens.AccentBlueLight)
val AccentBlueDark = Color(DesignTokens.AccentBlueDark)

// Semantic color tokens (derived from DesignTokens — single source of truth)
/** Primary action button container — white text achieves 6.10:1 contrast */
val PrimaryActionContainer = Color(DesignTokens.PrimaryActionContainer)
/** Success state container — white text achieves 4.70:1 contrast (fixes APP-021) */
val SuccessContainer = Color(DesignTokens.SuccessContainer)
/** Error state container — white text achieves 6.53:1 contrast (vs ErrorRed 3.9:1) */
val ErrorContainerDark = Color(DesignTokens.ErrorContainerDark)
/** Brand accent text for on-dark use — achieves 5.5:1 on DarkBackground */
val BrandAccentText = AccentBlueLight
/** Surface container token for elevation layering */
val SurfaceContainerToken = DarkCard

// State colors (legacy — kept for backward compatibility)
val SuccessGreen = Color(0xFF4CAF50)
val WarningAmber = Color(0xFFFFB300)
val ErrorRed = Color(0xFFFF5252)
val InfoBlue = Color(0xFF2196F3)

// Workout-specific
val RestTimerBg = Color(0xFF2D2D44)
val SetComplete = Color(0xFF4CAF50)
val PRHighlight = Color(0xFFFFD700)
val MuscleActive = Color(0xFF6C63FF)
val MuscleRest = Color(0xFF3A3A5C)

// Volume chart
val VolumeChartLine = Color(0xFF6C63FF)
val VolumeChartFill = Color(0x336C63FF)
val VolumeChartGrid = Color(0xFF2A2A44)

// Onboarding/home aliases mapped onto the main palette
val WarmWhite = TextPrimary
val AccentBlueDim = AccentBlue.copy(alpha = 0.15f)
