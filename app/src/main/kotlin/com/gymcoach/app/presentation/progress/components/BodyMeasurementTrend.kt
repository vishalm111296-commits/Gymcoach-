package com.gymcoach.app.presentation.progress.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymcoach.app.presentation.progress.TrendDirection
import com.gymcoach.app.presentation.progress.TrendPoint
import com.gymcoach.app.ui.theme.DarkSurface
import com.gymcoach.app.ui.theme.SuccessGreen
import com.gymcoach.app.ui.theme.TextPrimary
import com.gymcoach.app.ui.theme.TextSecondary
import com.gymcoach.app.ui.theme.WarningAmber
import com.gymcoach.app.ui.theme.VolumeChartGrid

/**
 * Compact measurement card (bodyweight, waist, ...) with current value,
 * trend arrow and mini sparkline.
 *
 * goodWhenDown flips which direction counts as healthy (e.g. waist should
 * shrink, bodyweight usually should grow). Red stays reserved for errors;
 * adverse movement shows amber.
 */
@Composable
fun BodyMeasurementTrend(
    label: String,
    currentValue: Double,
    unit: String,
    trend: TrendDirection,
    dataPoints: List<TrendPoint>,
    modifier: Modifier = Modifier,
    goodWhenDown: Boolean = false
) {
    val trendColor = when {
        trend == TrendDirection.STABLE -> TextSecondary
        (trend == TrendDirection.DOWN) == goodWhenDown -> SuccessGreen
        else -> WarningAmber
    }
    val trendIcon: ImageVector = when (trend) {
        TrendDirection.UP -> Icons.AutoMirrored.Filled.TrendingUp
        TrendDirection.DOWN -> Icons.AutoMirrored.Filled.TrendingDown
        TrendDirection.STABLE -> Icons.AutoMirrored.Filled.TrendingFlat
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = TextSecondary
                )
                Icon(
                    imageVector = trendIcon,
                    contentDescription = trend.name.lowercase(),
                    tint = trendColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "%.1f".format(currentValue),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }

            Spacer(Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                if (dataPoints.size < 2) {
                    val midY = size.height / 2f
                    drawLine(
                        color = VolumeChartGrid,
                        start = Offset(0f, midY),
                        end = Offset(size.width, midY),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                } else {
                    val values = dataPoints.map { it.value }
                    val minValue = values.min()
                    val maxValue = values.max()
                    val range = (maxValue - minValue).coerceAtLeast(1e-6)
                    val stepX = size.width / (dataPoints.size - 1)
                    val path = Path()
                    dataPoints.forEachIndexed { index, point ->
                        val x = index * stepX
                        val y = (size.height * ((maxValue - point.value) / range)).toFloat()
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = trendColor,
                        style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }
    }
}
