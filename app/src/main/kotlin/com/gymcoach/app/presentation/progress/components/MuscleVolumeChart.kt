package com.gymcoach.app.presentation.progress.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymcoach.app.presentation.progress.MuscleVolumeData
import com.gymcoach.app.ui.theme.DarkSurface
import com.gymcoach.app.ui.theme.MuscleActive
import com.gymcoach.app.ui.theme.MuscleRest
import com.gymcoach.app.ui.theme.TextPrimary
import com.gymcoach.app.ui.theme.TextSecondary

private const val BAR_LABEL_WIDTH_DP = 84
private const val BAR_VALUE_WIDTH_DP = 56

/**
 * Horizontal bars per muscle group: achieved sets (MuscleActive) over the
 * remaining track (MuscleRest), with the weekly target range drawn as a
 * subtle overlay. Rows are tappable.
 */
@Composable
fun MuscleVolumeChart(
    muscleData: List<MuscleVolumeData>,
    modifier: Modifier = Modifier,
    onMuscleClick: ((String) -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "MUSCLE VOLUME",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                color = TextSecondary
            )

            if (muscleData.isEmpty()) {
                Text(
                    text = "No sets logged this week",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                muscleData.forEach { muscle ->
                    MuscleBar(muscle = muscle, onMuscleClick = onMuscleClick)
                }
            }
        }
    }
}

@Composable
private fun MuscleBar(
    muscle: MuscleVolumeData,
    onMuscleClick: ((String) -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onMuscleClick != null) {
                onMuscleClick?.invoke(muscle.muscleName)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = muscle.muscleName.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(BAR_LABEL_WIDTH_DP.dp)
        )

        Spacer(Modifier.width(8.dp))

        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
        ) {
            val denom = maxOf(muscle.targetMax, muscle.currentSets).coerceAtLeast(1)
            val w = size.width
            val h = size.height
            val radius = CornerRadius(h / 2f)

            // Rest track
            drawRoundRect(color = MuscleRest, topLeft = Offset.Zero, size = Size(w, h), cornerRadius = radius)

            // Achieved sets
            val fraction = (muscle.currentSets.toFloat() / denom).coerceIn(0f, 1f)
            if (fraction > 0f) {
                val activeWidth = (w * fraction).coerceAtLeast(h)
                drawRoundRect(color = MuscleActive, topLeft = Offset.Zero, size = Size(activeWidth, h), cornerRadius = radius)
            }

            // Target range overlay
            val minFraction = (muscle.targetMin.toFloat() / denom).coerceIn(0f, 1f)
            val maxFraction = (muscle.targetMax.toFloat() / denom).coerceIn(0f, 1f)
            val bandStart = w * minFraction
            val bandWidth = (w * maxFraction - bandStart).coerceAtLeast(0f)
            if (bandWidth > 0f) {
                drawRoundRect(
                    color = TextPrimary.copy(alpha = 0.10f),
                    topLeft = Offset(bandStart, 0f),
                    size = Size(bandWidth, h),
                    cornerRadius = radius
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = "${muscle.currentSets}/${muscle.targetMax} sets",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(BAR_VALUE_WIDTH_DP.dp)
        )
    }
}
