package com.gymcoach.app.ui.theme

import androidx.compose.ui.graphics.Color

// Premium Deep Dark Mode
val DarkBackground = Color(0xFF0F0F14)
val DarkSurface = Color(0xFF181820)
val DarkSurfaceVariant = Color(0xFF23232D)
val DarkCard = Color(0xFF1E1E28)

// Text Colors
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA0A0AB)
val TextTertiary = Color(0xFF6B6B78)

// Electric Accents
val AccentBlue = Color(0xFF4A4DFF) // Electric blue
val AccentBlueLight = Color(0xFF8B8EFF)
val AccentBlueDark = Color(0xFF3335B2)
val AccentNeonGreen = Color(0xFF00E676) // High contrast green for primary actions

// State colors
val SuccessGreen = Color(0xFF00C853)
val WarningAmber = Color(0xFFFFB300)
val ErrorRed = Color(0xFFFF3B30)
val InfoBlue = Color(0xFF2196F3)

// Workout-specific
val RestTimerBg = Color(0xFF23232D)
val SetComplete = SuccessGreen
val SetCurrent = AccentBlue
val PRHighlight = Color(0xFFFFD700)
val MuscleActive = AccentBlue
val MuscleRest = DarkSurfaceVariant

// Volume chart
val VolumeChartLine = AccentBlue
val VolumeChartFill = Color(0x334A4DFF)
val VolumeChartGrid = Color(0xFF2A2A35)

// Aliases
val WarmWhite = TextPrimary
val AccentBlueDim = AccentBlue.copy(alpha = 0.15f)
