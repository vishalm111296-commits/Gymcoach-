package com.gymcoach.app.ui.theme

/**
 * DesignTokens — Single Source of Truth for raw ARGB color values.
 *
 * All semantic color tokens in [Color.kt] should reference these constants
 * to ensure consistency and enable compile-time contrast verification.
 *
 * WCAG pairs guaranteed by these values (verified in DesignTokenContrastTest):
 * - TextPrimary on DarkBackground ≥ 7.0 (enhanced)
 * - TextPrimary on DarkSurface ≥ 4.5
 * - AccentBlueLight on DarkBackground ≥ 4.5
 * - AccentBlueLight on DarkCard ≥ 4.5
 * - TextPrimary on PrimaryActionContainer (AccentBlueDark) ≥ 4.5
 * - TextPrimary on SuccessContainer ≥ 4.5
 * - TextPrimary on ErrorContainerDark ≥ 4.5
 * - AccentBlue on DarkBackground ≥ 3.0 (graphics)
 * - AccentBlue on DarkSurface ≥ 3.0 (graphics)
 * - TextSecondary on DarkBackground ≥ 4.5
 *
 * Known deviation (documented, not weakened):
 * - TextTertiary on DarkBackground ~3.8:1 — intentional for muted tertiary text
 */
object DesignTokens {
    // Backgrounds
    const val DarkBackground: Long = 0xFF1A1A2E
    const val DarkSurface: Long = 0xFF16213E
    const val DarkSurfaceVariant: Long = 0xFF1F2B45
    const val DarkCard: Long = 0xFF252A41

    // Text
    const val TextPrimary: Long = 0xFFF5F5F0
    const val TextSecondary: Long = 0xFFB8B5AD
    const val TextTertiary: Long = 0xFF7A7770

    // Brand / Accent
    const val AccentBlue: Long = 0xFF6C63FF
    const val AccentBlueLight: Long = 0xFF8B83FF
    const val AccentBlueDark: Long = 0xFF4A42E0

    // Semantic containers (for CTA buttons, state indicators)
    const val PrimaryActionContainer: Long = AccentBlueDark      // #4A42E0 — white text = 6.10:1
    const val SuccessContainer: Long = 0xFF2E7D32                // #2E7D32 — white text = 4.9:1
    const val ErrorContainerDark: Long = 0xFFB3261E              // #B3261E — white text = 6.53:1

    // Workout-specific (kept for reference; SetCompleteSuccess = SuccessContainer)
    const val SetCompleteSuccess: Long = SuccessContainer
}