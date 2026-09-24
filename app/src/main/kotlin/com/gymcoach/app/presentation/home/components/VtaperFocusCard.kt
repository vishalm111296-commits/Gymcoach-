package com.gymcoach.app.presentation.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymcoach.app.ui.theme.GymCoachBorders
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes

/** Transparent volume metric - no composite "score". */
data class VtaperMuscleData(
    val label: String,
    val current: Int, // planned sets this week
    val target: Int
)

/**
 * Horizontal volume bars for the V-taper priority muscles.
 * Primary fill for achieved, SurfaceInput track for remaining.
 */
@Composable
fun VtaperFocusCard(
    muscleData: List<VtaperMuscleData>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(GymCoachShapes.md)
            .border(GymCoachBorders.subtle, GymCoachShapes.md),
        shape = GymCoachShapes.md,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "V-TAPER FOCUS",
                style = MaterialTheme.typography.labelSmall,
                color = GymCoachColors.Primary,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Planned weekly sets vs optimal band ($TARGET_SETS_LABEL)",
                style = MaterialTheme.typography.bodySmall,
                color = GymCoachColors.TextMuted,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            muscleData.forEach { data ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = data.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = GymCoachColors.TextSecondary,
                        modifier = Modifier.width(110.dp)
                    )
                    val targetFraction = if (data.target > 0) {
                        (data.current.toFloat() / data.target).coerceIn(0f, 1f)
                    } else 0f
                    val animatedBarProgress by animateFloatAsState(
                        targetValue = targetFraction,
                        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
                        label = "vtaperBar_${data.label}"
                    )
                    LinearProgressIndicator(
                        progress = { animatedBarProgress },
                        color = GymCoachColors.Primary,
                        trackColor = GymCoachColors.SurfaceInput,
                        modifier = Modifier
                            .weight(1f)
                            .size(height = 8.dp, width = 0.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = "${data.current}/${data.target}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = GymCoachColors.TextPrimary,
                        modifier = Modifier.width(44.dp)
                    )
                }
            }
        }
    }
}

private const val TARGET_SETS_LABEL = "14 sets"
