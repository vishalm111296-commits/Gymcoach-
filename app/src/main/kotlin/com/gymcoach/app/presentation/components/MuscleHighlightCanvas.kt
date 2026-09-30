package com.gymcoach.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.gymcoach.app.ui.theme.GymCoachColors

@Composable
fun MuscleHighlightCanvas(
    primaryMuscle: String,
    secondaryMuscles: String,
    modifier: Modifier = Modifier
) {
    val primaryColor = GymCoachColors.Primary
    val secondaryColor = GymCoachColors.CyanAccent

    val primary = primaryMuscle.lowercase()
    val secondaries = secondaryMuscles.lowercase().split(",").map { it.trim() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Basic body outline
        val outlineColor = GymCoachColors.TextSecondary.copy(alpha = 0.3f)
        val outlineStroke = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Head
        drawCircle(
            color = outlineColor,
            radius = w * 0.1f,
            center = Offset(w * 0.5f, h * 0.15f),
            style = outlineStroke
        )
        // Torso
        drawLine(
            color = outlineColor,
            start = Offset(w * 0.5f, h * 0.25f),
            end = Offset(w * 0.5f, h * 0.55f),
            strokeWidth = 2f
        )
        // Shoulders
        drawLine(
            color = outlineColor,
            start = Offset(w * 0.3f, h * 0.25f),
            end = Offset(w * 0.7f, h * 0.25f),
            strokeWidth = 2f
        )
        // Arms
        drawLine(color = outlineColor, start = Offset(w * 0.3f, h * 0.25f), end = Offset(w * 0.2f, h * 0.5f), strokeWidth = 2f)
        drawLine(color = outlineColor, start = Offset(w * 0.7f, h * 0.25f), end = Offset(w * 0.8f, h * 0.5f), strokeWidth = 2f)
        // Legs
        drawLine(color = outlineColor, start = Offset(w * 0.5f, h * 0.55f), end = Offset(w * 0.4f, h * 0.9f), strokeWidth = 2f)
        drawLine(color = outlineColor, start = Offset(w * 0.5f, h * 0.55f), end = Offset(w * 0.6f, h * 0.9f), strokeWidth = 2f)

        // Determine highlighting based on muscles
        fun drawMusclePoint(x: Float, y: Float, isPrimary: Boolean) {
            drawCircle(
                color = if (isPrimary) primaryColor else secondaryColor,
                radius = if (isPrimary) w * 0.08f else w * 0.05f,
                center = Offset(x, y)
            )
        }

        val allMuscles = listOf(primary) + secondaries

        for (m in allMuscles) {
            val isPrimary = (m == primary)
            when {
                m.contains("chest") || m.contains("pec") -> {
                    drawMusclePoint(w * 0.4f, h * 0.3f, isPrimary)
                    drawMusclePoint(w * 0.6f, h * 0.3f, isPrimary)
                }
                m.contains("back") || m.contains("lat") -> {
                    drawMusclePoint(w * 0.4f, h * 0.4f, isPrimary)
                    drawMusclePoint(w * 0.6f, h * 0.4f, isPrimary)
                }
                m.contains("shoulder") || m.contains("delt") -> {
                    drawMusclePoint(w * 0.3f, h * 0.25f, isPrimary)
                    drawMusclePoint(w * 0.7f, h * 0.25f, isPrimary)
                }
                m.contains("bicep") || m.contains("arm") -> {
                    drawMusclePoint(w * 0.25f, h * 0.35f, isPrimary)
                    drawMusclePoint(w * 0.75f, h * 0.35f, isPrimary)
                }
                m.contains("tricep") -> {
                    drawMusclePoint(w * 0.25f, h * 0.4f, isPrimary)
                    drawMusclePoint(w * 0.75f, h * 0.4f, isPrimary)
                }
                m.contains("quad") || m.contains("leg") -> {
                    drawMusclePoint(w * 0.45f, h * 0.7f, isPrimary)
                    drawMusclePoint(w * 0.55f, h * 0.7f, isPrimary)
                }
                m.contains("hamstring") || m.contains("glute") -> {
                    drawMusclePoint(w * 0.45f, h * 0.65f, isPrimary)
                    drawMusclePoint(w * 0.55f, h * 0.65f, isPrimary)
                }
                m.contains("calf") || m.contains("calves") -> {
                    drawMusclePoint(w * 0.42f, h * 0.85f, isPrimary)
                    drawMusclePoint(w * 0.58f, h * 0.85f, isPrimary)
                }
                m.contains("ab") || m.contains("core") -> {
                    drawMusclePoint(w * 0.5f, h * 0.45f, isPrimary)
                }
            }
        }
    }
}
