package com.gymcoach.app.presentation.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymcoach.app.core.animation.AnimationRepository
import com.gymcoach.app.core.animation.ExerciseAnimationDefinition
import com.gymcoach.app.ui.theme.AccentBlue
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes
import com.gymcoach.app.ui.theme.GymCoachSpacing
import com.gymcoach.app.ui.theme.TextPrimary
import com.gymcoach.app.ui.theme.TextSecondary

/**
 * Contained modal bottom sheet for inspecting exercise technique and biomechanical animations
 * directly within active workout sessions or library lists without disrupting logging.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseTechniqueBottomSheet(
    exerciseName: String,
    muscleGroup: String = "",
    instructions: String = "",
    animationRepository: AnimationRepository,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var animationDefinition by remember { mutableStateOf<ExerciseAnimationDefinition?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(exerciseName) {
        isLoading = true
        animationDefinition = animationRepository.getAnimation(exerciseName = exerciseName)
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GymCoachColors.SurfaceDeep,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Exercise title, muscle pill, dismiss button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = TextPrimary
                    )
                    if (muscleGroup.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(GymCoachShapes.xs)
                                .background(AccentBlue.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = muscleGroup.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = AccentBlue
                            )
                        }
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Technique Guide",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Animation Player or Loading / Fallback
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentBlue)
                    }
                }
                animationDefinition != null -> {
                    ExerciseAnimationPlayer(
                        definition = animationDefinition!!,
                        compact = true,
                        autoPlay = true
                    )
                }
                else -> {
                    // Fallback visual coaching card
                    Surface(
                        shape = GymCoachShapes.md,
                        color = GymCoachColors.SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GymCoachColors.BorderSubtle)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Form Guide Available via Standard Cues",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Form Instructions & Technique Cues
            if (instructions.isNotBlank()) {
                Surface(
                    shape = GymCoachShapes.md,
                    color = GymCoachColors.SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GymCoachColors.BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = GymCoachColors.GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Execution Instructions",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = instructions,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
