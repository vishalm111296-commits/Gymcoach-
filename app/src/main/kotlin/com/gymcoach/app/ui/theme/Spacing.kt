package com.gymcoach.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Spacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val section: Dp = 48.dp,
    val component: Dp = 56.dp, // For standard touch targets (e.g., FAB, tall buttons)
    val bottomNavHeight: Dp = 80.dp // Accommodates standard fab setups
)

val GymCoachSpacing = Spacing()
