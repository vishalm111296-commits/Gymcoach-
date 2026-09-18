package com.gymcoach.app.ui.theme

import androidx.compose.ui.graphics.Color

// Deep layered dark charcoal/slate backgrounds
val DarkBackground = Color(0xFF0F1420)
val DarkSurface = Color(0xFF171F30)
val DarkSurfaceVariant = Color(0xFF1F2A40)
val DarkCard = Color(0xFF1C2538)

// Crisp typography colors with WCAG AAA contrast
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextTertiary = Color(0xFF64748B)

// Refined violet/indigo accent
val AccentBlue = Color(0xFF6C63FF)
val AccentBlueLight = Color(0xFF8B85FF)
val AccentBlueDark = Color(0xFF4E45D9)

// State colors
val SuccessGreen = Color(0xFF10B981)
val WarningAmber = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFEF4444)
val InfoBlue = Color(0xFF0EA5E9)

// Workout-specific
val RestTimerBg = Color(0xFF1A2234)
val SetComplete = Color(0xFF10B981)
val PRHighlight = Color(0xFFFFB300)
val MuscleActive = Color(0xFF6C63FF)
val MuscleRest = Color(0xFF263248)

// Volume chart
val VolumeChartLine = Color(0xFF6C63FF)
val VolumeChartFill = Color(0x336C63FF)
val VolumeChartGrid = Color(0xFF232D42)

// Onboarding/home aliases mapped onto the main palette
val WarmWhite = TextPrimary
val AccentBlueDim = AccentBlue.copy(alpha = 0.15f)
