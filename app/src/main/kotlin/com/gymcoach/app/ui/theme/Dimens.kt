package com.gymcoach.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Dimens — Spacing and sizing tokens.
 *
 * Based on 4dp base unit (Material Design 3 spacing scale).
 * All committed UI files should migrate to these tokens mechanically.
 */
object Dimens {
    // Spacing scale (4dp base unit)
    val SpacingXs = 4.dp   // Micro gaps, icon padding
    val SpacingSm = 8.dp   // Small gaps, chip spacing, internal padding
    val SpacingMd = 12.dp  // Medium gaps, list item padding
    val SpacingLg = 16.dp  // Screen padding, card padding (standard)
    val SpacingXl = 24.dp  // Section gaps
    val Spacing2xl = 32.dp // Large section gaps

    // Screen-level
    val ScreenPadding = 16.dp

    // Shape corner radii (referenced by Shape.kt)
    val ShapeCornerSmall = 8.dp
    val ShapeCornerMedium = 12.dp
    val ShapeCornerLarge = 16.dp
}