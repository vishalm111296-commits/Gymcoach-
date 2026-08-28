package com.gymcoach.app.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymcoach.app.ui.theme.AccentBlue
import com.gymcoach.app.ui.theme.DarkSurface
import com.gymcoach.app.ui.theme.MuscleActive
import com.gymcoach.app.ui.theme.TextSecondary
import com.gymcoach.app.ui.theme.TextTertiary
import com.gymcoach.app.ui.theme.WarmWhite

/**
 * Today's scheduled session with a full-width start CTA.
 */
@Composable
fun TodayWorkoutCard(
    workoutName: String,
    targetMuscles: List<String>,
    exerciseCount: Int,
    estimatedDuration: Int, // minutes
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "TODAY'S WORKOUT",
                style = MaterialTheme.typography.labelSmall,
                color = AccentBlue,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = workoutName,
                style = MaterialTheme.typography.headlineSmall,
                color = WarmWhite,
                fontWeight = FontWeight.Bold
            )
            if (targetMuscles.isNotEmpty()) {
                Text(
                    text = targetMuscles.joinToString(" \u2022 "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Text(
                text = "$exerciseCount exercises \u2022 ~$estimatedDuration min",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary
            )
            Button(
                onClick = onStartClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue,
                    contentColor = WarmWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(56.dp)
            ) {
                Text(
                    text = "START WORKOUT",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun CompletedWorkoutCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Workout Complete",
                tint = MuscleActive,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "WORKOUT COMPLETE",
                style = MaterialTheme.typography.labelSmall,
                color = MuscleActive,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Great job today!",
                style = MaterialTheme.typography.headlineSmall,
                color = WarmWhite,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Take time to recover. Your next session is scheduled.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
