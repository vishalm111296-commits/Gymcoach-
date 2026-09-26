package com.gymcoach.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

data class CustomGymCoachShapes(
    val input: RoundedCornerShape = GymCoachShapes.input,
    val chip: RoundedCornerShape = GymCoachShapes.chip,
    val card: RoundedCornerShape = GymCoachShapes.Card,
    val container: RoundedCornerShape = GymCoachShapes.container,
    val pill: RoundedCornerShape = GymCoachShapes.pill
)

val LocalGymCoachShapes = compositionLocalOf { CustomGymCoachShapes() }
