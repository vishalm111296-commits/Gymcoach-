package com.gymcoach.app.presentation.workout.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachSpacing

private const val RingSizeDp = 220

@Composable
fun RestTimerOverlay(
    timeRemaining: Int,
    totalTime: Int,
    nextSet: String,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onAddFifteen: () -> Unit,
    onSubtractFifteen: () -> Unit,
    modifier: Modifier = Modifier,
    isPaused: Boolean = false
) {
    val targetProgress = if (totalTime > 0) (timeRemaining.toFloat() / totalTime).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
        label = "RestProgress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GymCoachColors.PureDark.copy(alpha = 0.97f))
            .padding(GymCoachSpacing.xxl),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(RingSizeDp.dp),
                    strokeWidth = 8.dp,
                    color = GymCoachColors.Primary,
                    trackColor = GymCoachColors.SurfaceElevated
                )
                Text(
                    text = formatCountdown(timeRemaining),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = GymCoachColors.TextPrimary
                )
            }

            Spacer(Modifier.height(GymCoachSpacing.xxl))

            Text(
                text = "NEXT SET: ${nextSet.uppercase()}",
                style = MaterialTheme.typography.labelLarge,
                color = GymCoachColors.TextSecondary
            )

            Spacer(Modifier.height(GymCoachSpacing.xxxl))

            Row(
                horizontalArrangement = Arrangement.spacedBy(GymCoachSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimerControlButton(
                    label = if (isPaused) "RESUME" else "PAUSE",
                    onClick = if (isPaused) onResume else onPause,
                    emphasized = true
                )
                TimerControlButton(label = "SKIP", onClick = onSkip)
            }

            Spacer(Modifier.height(GymCoachSpacing.md))

            Row(
                horizontalArrangement = Arrangement.spacedBy(GymCoachSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimerControlButton(label = "+15s", onClick = onAddFifteen)
                TimerControlButton(label = "-15s", onClick = onSubtractFifteen)
            }
        }
    }
}

@Composable
private fun TimerControlButton(
    label: String,
    onClick: () -> Unit,
    emphasized: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp).width(120.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (emphasized) GymCoachColors.Primary else GymCoachColors.SurfaceElevated,
            contentColor = if (emphasized) GymCoachColors.TextPrimary else GymCoachColors.TextSecondary
        )
    ) {
        Text(text = label, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatCountdown(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
