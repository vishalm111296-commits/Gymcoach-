package com.gymcoach.app.presentation.components

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymcoach.app.core.animation.AnimationController
import com.gymcoach.app.core.animation.AnimationPhase
import com.gymcoach.app.core.animation.ExerciseAnimationDefinition
import com.gymcoach.app.core.animation.SkeletalRenderer
import com.gymcoach.app.core.animation.rememberAnimationController
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes
import com.gymcoach.app.ui.theme.GymCoachSpacing

@Composable
fun ExerciseAnimationPlayer(
    definition: ExerciseAnimationDefinition,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    autoPlay: Boolean = true
) {
    val context = LocalContext.current
    val isReducedMotion = remember {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (e: Exception) {
            false
        }
    }

    val controller = rememberAnimationController(
        definition = definition,
        autoPlay = autoPlay && !isReducedMotion
    )

    var showTrajectory by rememberSaveable { mutableStateOf(true) }
    var showAngles by rememberSaveable { mutableStateOf(true) }

    val phase = controller.currentPhase
    val cue = controller.currentCue
    val currentFrame = controller.currentFrame

    val primaryColor = GymCoachColors.Primary
    val jointColor = GymCoachColors.CyanAccent
    val equipmentColor = GymCoachColors.GoldAccent

    val phaseBadgeColor by animateColorAsState(
        targetValue = when (phase) {
            AnimationPhase.SETUP -> GymCoachColors.CyanAccent
            AnimationPhase.START -> GymCoachColors.Primary
            AnimationPhase.ECCENTRIC -> GymCoachColors.Warning
            AnimationPhase.BOTTOM -> GymCoachColors.Danger
            AnimationPhase.CONCENTRIC -> GymCoachColors.Success
            AnimationPhase.END -> GymCoachColors.Primary
        },
        label = "phaseColor"
    )

    val playButtonScale by animateFloatAsState(
        targetValue = if (controller.isPlaying) 1.0f else 1.08f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "playButtonScale"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = GymCoachShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = GymCoachColors.SurfaceCardElevated
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 12.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row: Phase badge + speed + trajectory toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Phase Badge
                Surface(
                    shape = GymCoachShapes.sm,
                    color = phaseBadgeColor.copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, phaseBadgeColor)
                ) {
                    Text(
                        text = phase.displayName.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = phaseBadgeColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Controls: Speed toggle & ROM Trajectory toggle & Loop toggle
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Trajectory toggle
                    IconButton(
                        onClick = { showTrajectory = !showTrajectory },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = "Toggle ROM Trajectory",
                            tint = if (showTrajectory) GymCoachColors.CyanAccent else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Angle toggle (if angle specs exist)
                    if (definition.angleSpecs.isNotEmpty()) {
                        IconButton(
                            onClick = { showAngles = !showAngles },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SquareFoot,
                                contentDescription = "Toggle Joint Angles",
                                tint = if (showAngles) GymCoachColors.GoldAccent else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Speed chip
                    FilterChip(
                        selected = controller.speed != 1.0f,
                        onClick = {
                            val nextSpeed = when (controller.speed) {
                                1.0f -> 0.5f
                                0.5f -> 1.5f
                                else -> 1.0f
                            }
                            controller.setPlaybackSpeed(nextSpeed)
                        },
                        label = {
                            Text(
                                text = "${controller.speed}x",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Speed",
                                modifier = Modifier.size(13.dp)
                            )
                        },
                        modifier = Modifier.height(28.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )

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

            Spacer(Modifier.height(10.dp))

            // Canvas Display Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (compact) 1.35f else 1.25f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    val frame = currentFrame
                    if (frame != null) {
                        SkeletalRenderer.drawSkeleton(
                            drawScope = this,
                            frame = frame,
                            perspective = definition.perspective,
                            primaryColor = primaryColor,
                            jointColor = jointColor,
                            equipmentColor = equipmentColor,
                            trajectoryPath = definition.trajectoryPath,
                            showTrajectory = showTrajectory,
                            showAngles = showAngles
                        )
                    }
                }

                // Biomechanical Angle Overlay Badges
                if (showAngles && currentFrame != null && currentFrame.angleReadouts.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        currentFrame.angleReadouts.forEach { readout ->
                            Surface(
                                shape = GymCoachShapes.xs,
                                color = GymCoachColors.SurfaceDeep.copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GymCoachColors.GoldAccent.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "${readout.label}: ${readout.angleDegrees.toInt()}°",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = GymCoachColors.GoldAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Watermark / Perspective badge
                Text(
                    text = "${definition.perspective.name} VIEW • BIOMECHANICAL FORM",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = Color.Gray.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }

            // Form Cue Text
            if (cue.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = cue,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (compact) 13.sp else 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(Modifier.height(4.dp))

            // Scrubber Slider
            Slider(
                value = controller.progress,
                onValueChange = {
                    controller.pause()
                    controller.seekTo(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .semantics {
                        contentDescription = "Animation progress: ${(controller.progress * 100).toInt()}%"
                    },
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
                IconButton(onClick = { controller.replay() }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Replay",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Previous Step (Keyframe)
                IconButton(onClick = { controller.previousStep() }, modifier = Modifier.size(40.dp)) {
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
                    modifier = Modifier
                        .size(if (compact) 44.dp else 50.dp)
                        .graphicsLayer {
                            scaleX = playButtonScale
                            scaleY = playButtonScale
                        }
                ) {
                    IconButton(
                        onClick = { controller.togglePlayPause() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (controller.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (controller.isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Next Step (Keyframe)
                IconButton(onClick = { controller.nextStep() }, modifier = Modifier.size(40.dp)) {
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
