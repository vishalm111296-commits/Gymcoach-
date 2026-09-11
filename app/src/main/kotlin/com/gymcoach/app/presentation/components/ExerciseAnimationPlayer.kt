package com.gymcoach.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymcoach.app.core.animation.AnimationController
import com.gymcoach.app.core.animation.AnimationPhase
import com.gymcoach.app.core.animation.ExerciseAnimationDefinition
import com.gymcoach.app.core.animation.SkeletalRenderer
import com.gymcoach.app.core.animation.rememberAnimationController

@Composable
fun ExerciseAnimationPlayer(
    definition: ExerciseAnimationDefinition,
    modifier: Modifier = Modifier,
    controller: AnimationController = rememberAnimationController(definition)
) {
    val frame = controller.currentFrame
    val phase = controller.currentPhase
    val cue = controller.currentCue

    val primaryColor = MaterialTheme.colorScheme.primary
    val jointColor = MaterialTheme.colorScheme.tertiary
    val equipmentColor = MaterialTheme.colorScheme.secondary

    val phaseBadgeColor by animateColorAsState(
        targetValue = when (phase) {
            AnimationPhase.SETUP -> MaterialTheme.colorScheme.secondary
            AnimationPhase.START -> MaterialTheme.colorScheme.primary
            AnimationPhase.ECCENTRIC -> Color(0xFFFFA000) // Amber
            AnimationPhase.BOTTOM -> Color(0xFFE91E63) // Pink/Red peak tension
            AnimationPhase.CONCENTRIC -> Color(0xFF4CAF50) // Green drive
            AnimationPhase.END -> MaterialTheme.colorScheme.primary
        },
        label = "phaseColor"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row: Phase badge + speed + loop
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Phase Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = phaseBadgeColor.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, phaseBadgeColor)
                ) {
                    Text(
                        text = phase.displayName.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = phaseBadgeColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Controls: Speed toggle & Loop toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Speed chip
                    FilterChip(
                        selected = controller.speed < 1.0f,
                        onClick = {
                            val newSpeed = if (controller.speed == 1.0f) 0.5f else 1.0f
                            controller.setPlaybackSpeed(newSpeed)
                        },
                        label = {
                            Text(
                                text = if (controller.speed < 1.0f) "0.5x Slow" else "1.0x",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Speed",
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        modifier = Modifier.height(28.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )

                    Spacer(Modifier.width(6.dp))

                    // Loop toggle
                    IconButton(
                        onClick = { controller.setLoop(!controller.isLooping) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Loop",
                            tint = if (controller.isLooping) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Canvas Display Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                if (frame != null) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        SkeletalRenderer.drawSkeleton(
                            drawScope = this,
                            frame = frame,
                            perspective = definition.perspective,
                            primaryColor = primaryColor,
                            jointColor = jointColor,
                            equipmentColor = equipmentColor
                        )
                    }
                } else {
                    Text(
                        text = "Loading demonstration...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                // Subtle watermark / badge indicating vector demo
                Text(
                    text = "ILLUSTRATIVE FORM DEMO",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color.Gray.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }

            // Form Cue Text
            if (cue.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = cue,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(Modifier.height(6.dp))

            // Scrubber Slider
            Slider(
                value = controller.progress,
                onValueChange = {
                    controller.pause()
                    controller.seekTo(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                )
            )

            // Playback Control Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Replay
                IconButton(onClick = { controller.replay() }) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Replay",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Previous Step (Keyframe)
                IconButton(onClick = { controller.previousStep() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Phase Step",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Play / Pause Main Button
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(52.dp)
                ) {
                    IconButton(
                        onClick = { controller.togglePlayPause() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (controller.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (controller.isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Next Step (Keyframe)
                IconButton(onClick = { controller.nextStep() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Phase Step",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Step Mode indicator / toggle
                TextButton(
                    onClick = {
                        if (controller.isStepMode) {
                            controller.play()
                        } else {
                            controller.pause()
                            controller.nextStep()
                        }
                    }
                ) {
                    Text(
                        text = if (controller.isStepMode) "Smooth" else "Step",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (controller.isStepMode) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        }
    }
}
