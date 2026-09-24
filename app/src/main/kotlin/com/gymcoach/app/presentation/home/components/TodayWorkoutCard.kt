package com.gymcoach.app.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymcoach.app.ui.theme.GymCoachBorders
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes

/**
 * Premium Hero Workout Card for Home Dashboard.
 * Serves as the primary focal point: answers "What should I do today?" with clear metrics
 * and a high-contrast action CTA.
 */
@Composable
fun TodayWorkoutCard(
    workoutName: String,
    targetMuscles: List<String>,
    exerciseCount: Int,
    estimatedDuration: Int,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(GymCoachShapes.lg)
            .border(GymCoachBorders.primary, GymCoachShapes.lg),
        shape = GymCoachShapes.lg,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCardElevated)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pill header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val dotTransition = rememberInfiniteTransition(label = "liveDotPulse")
                    val dotAlpha by dotTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dotAlpha"
                    )
                    val dotScale by dotTransition.animateFloat(
                        initialValue = 0.85f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dotScale"
                    )

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = dotScale
                                scaleY = dotScale
                                alpha = dotAlpha
                            }
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(GymCoachColors.Primary)
                    )
                    Text(
                        text = "TODAY'S WORKOUT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = GymCoachColors.Primary
                    )
                }

                if (targetMuscles.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(GymCoachShapes.xs)
                            .background(GymCoachColors.SurfaceCard)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = targetMuscles.firstOrNull() ?: "",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = GymCoachColors.TextSecondary
                        )
                    }
                }
            }

            // Workout Name
            Text(
                text = workoutName,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = GymCoachColors.TextPrimary
            )

            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = GymCoachColors.TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "$exerciseCount exercises",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = GymCoachColors.TextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = GymCoachColors.TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "~$estimatedDuration min",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = GymCoachColors.TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            val btnInteractionSource = remember { MutableInteractionSource() }
            val isPressed by btnInteractionSource.collectIsPressedAsState()
            val btnScale by animateFloatAsState(
                targetValue = if (isPressed) 0.95f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "startBtnScale"
            )

            // High-impact Start CTA
            Button(
                onClick = onStartClick,
                interactionSource = btnInteractionSource,
                shape = GymCoachShapes.md,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GymCoachColors.Primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .graphicsLayer {
                        scaleX = btnScale
                        scaleY = btnScale
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "START WORKOUT",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
