package com.gymcoach.app.presentation.progress.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.gymcoach.app.presentation.progress.ProgressPoint
import java.util.Date

@Composable
fun StrengthLineChart(
    data: List<ProgressPoint>,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas

        val paddingLeft = 8f
        val paddingBottom = 8f
        val chartWidth = size.width - paddingLeft
        val chartHeight = size.height - paddingBottom

        val values = data.map { it.value }
        val minVal = values.min()
        val maxVal = values.max()
        val range = (maxVal - minVal).coerceAtLeast(1.0)

        for (i in 0..3) {
            val y = chartHeight - (chartHeight * i / 3f)
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        val path = Path()
        val stepX = chartWidth / (data.size - 1).toFloat()

        data.forEachIndexed { index, point ->
            val x = paddingLeft + index * stepX
            val y = chartHeight - ((point.value - minVal) / range * chartHeight).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        data.forEachIndexed { index, point ->
            val x = paddingLeft + index * stepX
            val y = chartHeight - ((point.value - minVal) / range * chartHeight).toFloat()
            drawCircle(color = lineColor, radius = 4f, center = Offset(x, y))
        }
    }
}

@Composable
fun ExerciseSelector(
    selectedExercise: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedExercise,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "\u25bc",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listOf(
                "Bench Press", "Squat", "Deadlift", "Overhead Press",
                "Barbell Row", "Pull-Up", "Dumbbell Curl", "Tricep Pushdown"
            ).forEach { exercise ->
                DropdownMenuItem(
                    text = { Text(exercise) },
                    onClick = {
                        onSelect(exercise)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun VolumeLineChart(
    data: List<Pair<Date, Double>>,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas

        val paddingLeft = 8f
        val paddingBottom = 8f
        val chartWidth = size.width - paddingLeft
        val chartHeight = size.height - paddingBottom

        val values = data.map { it.second }
        val minVal = values.min()
        val maxVal = values.max()
        val range = (maxVal - minVal).coerceAtLeast(1.0)

        for (i in 0..3) {
            val y = chartHeight - (chartHeight * i / 3f)
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        val path = Path()
        val stepX = chartWidth / (data.size - 1).toFloat()

        data.forEachIndexed { index, (_, volume) ->
            val x = paddingLeft + index * stepX
            val y = chartHeight - ((volume - minVal) / range * chartHeight).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        data.forEachIndexed { index, (_, volume) ->
            val x = paddingLeft + index * stepX
            val y = chartHeight - ((volume - minVal) / range * chartHeight).toFloat()
            drawCircle(color = lineColor, radius = 4f, center = Offset(x, y))
        }
    }
}
