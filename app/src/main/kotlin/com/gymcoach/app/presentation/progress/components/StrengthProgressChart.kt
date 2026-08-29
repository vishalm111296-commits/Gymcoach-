package com.gymcoach.app.presentation.progress.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gymcoach.app.presentation.progress.ProgressPoint
import com.gymcoach.app.ui.theme.TextPrimary
import com.gymcoach.app.ui.theme.TextSecondary
import com.gymcoach.app.ui.theme.VolumeChartFill
import com.gymcoach.app.ui.theme.VolumeChartGrid
import com.gymcoach.app.ui.theme.VolumeChartLine

private const val PAD_LEFT = 8f
private const val PAD_RIGHT = 8f
private const val PAD_TOP = 16f
private const val PAD_BOTTOM = 12f

/**
 * Custom Canvas line chart (no external library): best-load-per-date for one
 * exercise, with gradient-free flat fill under the line and horizontal grid.
 */
@Composable
fun StrengthProgressChart(
    exerciseName: String,
    dataPoints: List<ProgressPoint>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = exerciseName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (dataPoints.isNotEmpty()) {
                Text(
                    text = "BEST %.0f kg".format(dataPoints.lastOrNull()?.value ?: 0.0),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = VolumeChartLine
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            if (dataPoints.size < 2) {
                Text(
                    text = "Not enough data yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val chartWidth = size.width - PAD_LEFT - PAD_RIGHT
                    val chartHeight = size.height - PAD_TOP - PAD_BOTTOM

                    val values = dataPoints.map { it.value }
                    val minValue = values.min()
                    val maxValue = values.max()
                    val range = (maxValue - minValue).coerceAtLeast(1.0)

                    fun yFor(value: Double): Float =
                        PAD_TOP + (chartHeight * ((maxValue - value) / range)).toFloat()

                    // Horizontal grid
                    for (i in 0..3) {
                        val y = PAD_TOP + chartHeight * i / 3f
                        drawLine(
                            color = VolumeChartGrid,
                            start = Offset(PAD_LEFT, y),
                            end = Offset(size.width - PAD_RIGHT, y),
                            strokeWidth = 1f
                        )
                    }

                    val stepX = chartWidth / (dataPoints.size - 1)
                    val coordinates = dataPoints.mapIndexed { index, point ->
                        Offset(PAD_LEFT + index * stepX, yFor(point.value))
                    }

                    // Fill under the line
                    val fillPath = Path()
                    coordinates.forEachIndexed { index, c ->
                        if (index == 0) fillPath.moveTo(c.x, c.y) else fillPath.lineTo(c.x, c.y)
                    }
                    coordinates.lastOrNull()?.let { last ->
                        fillPath.lineTo(last.x, size.height - PAD_BOTTOM)
                    }
                    coordinates.firstOrNull()?.let { first ->
                        fillPath.lineTo(first.x, size.height - PAD_BOTTOM)
                    }
                    fillPath.close()
                    drawPath(path = fillPath, color = VolumeChartFill)

                    // Line
                    val linePath = Path()
                    coordinates.forEachIndexed { index, c ->
                        if (index == 0) linePath.moveTo(c.x, c.y) else linePath.lineTo(c.x, c.y)
                    }
                    drawPath(
                        path = linePath,
                        color = VolumeChartLine,
                        style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Points; highlight latest
                    coordinates.forEachIndexed { _, c ->
                        drawCircle(color = VolumeChartLine, radius = 4f, center = c)
                    }
                    coordinates.lastOrNull()?.let { last ->
                        drawCircle(color = VolumeChartLine.copy(alpha = 0.25f), radius = 9f, center = last)
                        drawCircle(color = VolumeChartLine, radius = 5f, center = last)
                        drawCircle(color = TextPrimary, radius = 2f, center = last)
                    }
                }
            }
        }
    }
}
