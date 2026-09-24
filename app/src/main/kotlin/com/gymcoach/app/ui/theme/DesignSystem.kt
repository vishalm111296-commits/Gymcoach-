package com.gymcoach.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Cohesive design tokens for GymCoach.
 * Establishes a disciplined, high-contrast dark palette, geometric spacing scale,
 * standardized corner radii, and subtle borders.
 */
object GymCoachSpacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val xxxl: Dp = 32.dp
    val huge: Dp = 40.dp
    val colossal: Dp = 48.dp
}

object GymCoachShapes {
    val xs = RoundedCornerShape(6.dp)
    val sm = RoundedCornerShape(10.dp)
    val md = RoundedCornerShape(14.dp)
    val lg = RoundedCornerShape(18.dp)
    val xl = RoundedCornerShape(24.dp)
    val pill = RoundedCornerShape(999.dp)
    val Card = lg
}

object GymCoachColors {
    // Deep layered surfaces for visual depth
    val PureDark = Color(0xFF0D111A)
    val SurfaceDeep = Color(0xFF131926)
    val SurfaceCard = Color(0xFF1A2234)
    val SurfaceCardElevated = Color(0xFF222C42)
    val SurfaceInput = Color(0xFF161E2E)

    // Borders and dividers
    val BorderSubtle = Color(0xFF26324B)
    val BorderLight = Color(0xFF374668)

    // Primary Accents
    val Primary = Color(0xFF6C63FF)
    val PrimaryLight = Color(0xFF8B85FF)
    val PrimaryDark = Color(0xFF4E45D9)
    val PrimaryGlow = Color(0x336C63FF)

    // Secondary Accent
    val CyanAccent = Color(0xFF00E5FF)
    val GoldAccent = Color(0xFFFFB300)

    // Text Hierarchy
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFFCBD5E1)
    val TextMuted = Color(0xFF94A3B8)

    // Status Colors
    val Success = Color(0xFF10B981)
    val SuccessBg = Color(0xFF064E3B)
    val Warning = Color(0xFFF59E0B)
    val WarningBg = Color(0xFF78350F)
    val Danger = Color(0xFFEF4444)
    val DangerBg = Color(0xFF7F1D1D)

    // Animation & Biomechanical Phases
    val PhaseSetup = Color(0xFF38BDF8)     // Sky blue
    val PhaseEccentric = Color(0xFFFBBF24) // Amber
    val PhaseBottom = Color(0xFFF43F5E)    // Rose
    val PhaseConcentric = Color(0xFF34D399)// Emerald
    val PhaseEnd = Color(0xFFA78BFA)       // Violet

    // Gradients
    val PrimaryGradient = Brush.horizontalGradient(
        colors = listOf(Primary, Color(0xFF8B85FF))
    )
    val SurfaceCardGradient = Brush.verticalGradient(
        colors = listOf(SurfaceCardElevated, SurfaceCard)
    )
}

object GymCoachBorders {
    val subtle = BorderStroke(1.dp, GymCoachColors.BorderSubtle)
    val light = BorderStroke(1.dp, GymCoachColors.BorderLight)
    val primary = BorderStroke(1.5.dp, GymCoachColors.Primary.copy(alpha = 0.5f))
    val success = BorderStroke(1.dp, GymCoachColors.Success.copy(alpha = 0.4f))
    fun subtleBorder() = subtle
}

object GymCoachMotion {
    const val durationFast = 150
    const val durationMedium = 250
    const val durationSlow = 400
}

