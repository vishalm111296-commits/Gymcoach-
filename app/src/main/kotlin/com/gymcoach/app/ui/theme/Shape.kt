package com.gymcoach.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/**
 * Shape — Corner radius tokens.
 *
 * M3 Shapes mapped to GymCoach scale:
 * - extraSmall → 8dp (chips, buttons, small cards)
 * - small → 8dp (same as extraSmall for consistency)
 * - medium → 12dp (standard cards, dialogs)
 * - large → 16dp (large cards, bottom sheets)
 * - extraLarge → 16dp (same as large)
 *
 * Referenced via MaterialTheme.shapes in composables.
 */
val GymCoachShapes = Shapes(
    extraSmall = RoundedCornerShape(Dimens.ShapeCornerSmall),
    small = RoundedCornerShape(Dimens.ShapeCornerSmall),
    medium = RoundedCornerShape(Dimens.ShapeCornerMedium),
    large = RoundedCornerShape(Dimens.ShapeCornerLarge),
    extraLarge = RoundedCornerShape(Dimens.ShapeCornerLarge)
)