package com.gymcoach.app.presentation.workout.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachSpacing

@Composable
fun WorkoutProgressHeader(
    workoutName: String,
    completedSets: Int,
    totalSets: Int,
    elapsedTime: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GymCoachSpacing.lg, vertical = GymCoachSpacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = workoutName.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = GymCoachColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatElapsedTime(elapsedTime),
                style = MaterialTheme.typography.bodyMedium,
                color = GymCoachColors.TextSecondary
            )
        }

        Spacer(Modifier.height(GymCoachSpacing.sm))

        LinearProgressIndicator(
            progress = { if (totalSets > 0) completedSets.toFloat() / totalSets else 0f },
            modifier = Modifier.fillMaxWidth().heightIn(min = 6.dp),
            color = GymCoachColors.Primary,
            trackColor = GymCoachColors.SurfaceElevated
        )

        Spacer(Modifier.height(GymCoachSpacing.xs))

        Text(
            text = "$completedSets / $totalSets sets",
            style = MaterialTheme.typography.labelSmall,
            color = GymCoachColors.TextSecondary
        )
    }
}

private fun formatElapsedTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
