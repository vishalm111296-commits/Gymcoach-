package com.gymcoach.app.presentation.workout.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymcoach.app.core.exercise.SubstitutionEngine
import com.gymcoach.app.ui.theme.GymCoachBorders
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes
import com.gymcoach.app.ui.theme.GymCoachSpacing

@Composable
fun ExerciseSubstitutionDialog(
    currentExerciseName: String,
    substitutes: List<SubstitutionEngine.SubstitutionResult>,
    isLoading: Boolean = false,
    onSelectSubstitute: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Swap Exercise",
                    tint = GymCoachColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Substitute Exercise",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GymCoachColors.TextPrimary
                    )
                    Text(
                        text = "Replace \"$currentExerciseName\"",
                        style = MaterialTheme.typography.labelSmall,
                        color = GymCoachColors.TextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm)
            ) {
                Text(
                    text = "Ranked biomechanical substitutes preserving target muscle engagement and movement patterns:",
                    style = MaterialTheme.typography.bodySmall,
                    color = GymCoachColors.TextSecondary
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = GymCoachColors.Primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else if (substitutes.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceDeep),
                        border = GymCoachBorders.subtle,
                        shape = GymCoachShapes.sm
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Direct Substitutes Found",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = GymCoachColors.TextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Try adding a custom exercise or adjusting equipment access in your profile.",
                                style = MaterialTheme.typography.bodySmall,
                                color = GymCoachColors.TextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(substitutes, key = { it.substitute.id }) { result ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(GymCoachShapes.sm)
                                    .border(GymCoachBorders.subtle, GymCoachShapes.sm)
                                    .clickable { onSelectSubstitute(result.substitute.id) },
                                colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceDeep),
                                shape = GymCoachShapes.sm
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = result.substitute.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = GymCoachColors.TextPrimary
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "${result.substitute.muscleGroup} • ${result.substitute.equipment}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = GymCoachColors.TextSecondary
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${result.preservationScore}% match",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = GymCoachColors.Primary
                                        )
                                        Text(
                                            text = result.reason,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GymCoachColors.TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = GymCoachColors.TextSecondary)
            }
        }
    )
}
