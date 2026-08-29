package com.gymcoach.app.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymcoach.app.ui.theme.AccentBlue
import com.gymcoach.app.ui.theme.AccentBlueLight
import com.gymcoach.app.ui.theme.DarkCard
import com.gymcoach.app.ui.theme.DarkSurface
import com.gymcoach.app.ui.theme.TextPrimary
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "TODAY'S WORKOUT",
                style = MaterialTheme.typography.labelMedium,
                color = AccentBlueLight,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = workoutName,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold
            )
            if (targetMuscles.isNotEmpty()) {
                Text(
                    text = targetMuscles.joinToString(" \u2022 "),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "$exerciseCount exercises  \u2022  ~$estimatedDuration min",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
                fontWeight = FontWeight.Medium
            )
            Button(
                onClick = onStartClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue,
                    contentColor = WarmWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(60.dp)
            ) {
                Text(
                    text = "START WORKOUT",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
