package com.gymcoach.app.presentation.progress.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymcoach.app.domain.repository.MuscleGroupStats
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes
import com.gymcoach.app.ui.theme.GymCoachSpacing

val defaultMuscleColors = listOf(
    GymCoachColors.Primary,
    GymCoachColors.CyanAccent,
    GymCoachColors.GoldAccent,
    GymCoachColors.Success,
    GymCoachColors.PrimaryLight,
    Color(0xFF9D4EDD), // Kinetic Purple Accent
    GymCoachColors.TextMuted
)

@Composable
fun MuscleDistributionPieChart(
    stats: List<MuscleGroupStats>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = GymCoachShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = GymCoachColors.SurfaceCard
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Muscle Group Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val totalReps = stats.sumOf { it.totalReps }

            if (stats.isEmpty() || totalReps == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No muscle volume logged yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Donut/Pie Canvas
                    Canvas(
                        modifier = Modifier
                            .size(140.dp)
                            .padding(8.dp)
                            .semantics {
                                contentDescription = "Muscle distribution donut chart showing workout volume by muscle group"
                            }
                    ) {
                        var startAngle = -90f
                        val strokeWidth = 24.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                        val arcSize = Size(diameter, diameter)

                        stats.forEachIndexed { index, stat ->
                            val sweepAngle = (stat.totalReps.toFloat() / totalReps.toFloat()) * 360f
                            val color = defaultMuscleColors.getOrElse(index) { Color.Gray }

                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth)
                            )
                            startAngle += sweepAngle
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    // Legend
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        stats.take(5).forEachIndexed { index, stat ->
                            val percentage = (stat.totalReps.toDouble() / totalReps.toDouble() * 100.0)
                            val color = defaultMuscleColors.getOrElse(index) { Color.Gray }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Canvas(modifier = Modifier.size(10.dp)) {
                                        drawCircle(color = color)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stat.name.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "%.0f%%".format(percentage),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
