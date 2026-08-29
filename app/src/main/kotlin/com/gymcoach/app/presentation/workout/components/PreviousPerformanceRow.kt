package com.gymcoach.app.presentation.workout.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymcoach.app.ui.theme.DarkSurfaceVariant
import com.gymcoach.app.ui.theme.TextSecondary
import com.gymcoach.app.ui.theme.TextTertiary

/** One recorded set from a previous session. */
data class SetData(val weightKg: Double, val reps: Int)

/** Last session performance for an exercise. */
data class LastSessionData(val date: String, val sets: List<SetData>)

@Composable
fun PreviousPerformanceRow(
    lastSession: LastSessionData?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .padding(vertical = 10.dp, horizontal = 12.dp)
    ) {
        if (lastSession == null || lastSession.sets.isEmpty()) {
            Text(
                text = "No previous performance recorded.",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                fontWeight = FontWeight.Medium
            )
        } else {
            Column {
                Text(
                    text = "LAST TIME • ${lastSession.date.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                val summary = lastSession.sets.joinToString(" • ") { "${formatWeight(it.weightKg)}kg × ${it.reps}" }
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun formatWeight(weightKg: Double): String =
    if (weightKg == weightKg.toLong().toDouble()) weightKg.toLong().toString() else weightKg.toString()
