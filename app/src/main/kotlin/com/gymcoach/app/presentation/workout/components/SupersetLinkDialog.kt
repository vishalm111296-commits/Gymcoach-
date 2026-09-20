package com.gymcoach.app.presentation.workout.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymcoach.app.domain.model.SupersetGroup
import com.gymcoach.app.ui.theme.GymCoachBorders
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes
import com.gymcoach.app.ui.theme.GymCoachSpacing

@Composable
fun SupersetLinkDialog(
    currentExerciseIndex: Int,
    currentExerciseName: String,
    exercises: List<Pair<Int, String>>, // (index, name) for all exercises
    existingGroup: SupersetGroup?,
    onLink: (targetIndex: Int) -> Unit,
    onUnlink: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GymCoachColors.SurfaceCard,
        titleContentColor = GymCoachColors.TextPrimary,
        iconContentColor = GymCoachColors.Primary,
        icon = { Icon(Icons.Default.Link, contentDescription = null) },
        title = { Text("Superset Pairing") },
        text = {
            Column {
                Text(
                    "Pair '$currentExerciseName' with another exercise to alternate sets with minimal rest.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GymCoachColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(GymCoachSpacing.md))

                if (existingGroup != null) {
                    Surface(
                        shape = GymCoachShapes.sm,
                        color = GymCoachColors.Primary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, GymCoachColors.Primary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(GymCoachSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = null,
                                tint = GymCoachColors.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(GymCoachSpacing.xs))
                            Text(
                                "Linked in ${existingGroup.label}",
                                style = MaterialTheme.typography.labelMedium,
                                color = GymCoachColors.Primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(GymCoachSpacing.sm))
                }

                // Selectable list of other exercises
                val otherExercises = exercises.filter { (idx, _) -> idx != currentExerciseIndex }
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    itemsIndexed(otherExercises) { _, (exIdx, exName) ->
                        val isSelected = selectedIndex == exIdx
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedIndex = exIdx },
                            shape = GymCoachShapes.sm,
                            color = if (isSelected) GymCoachColors.Primary.copy(alpha = 0.15f) else GymCoachColors.SurfaceDeep,
                            border = if (isSelected) BorderStroke(1.dp, GymCoachColors.Primary) else GymCoachBorders.subtleBorder()
                        ) {
                            Row(
                                modifier = Modifier.padding(GymCoachSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedIndex = exIdx },
                                    colors = RadioButtonDefaults.colors(selectedColor = GymCoachColors.Primary)
                                )
                                Spacer(Modifier.width(GymCoachSpacing.xs))
                                Text(
                                    exName,
                                    color = GymCoachColors.TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Spacer(Modifier.height(GymCoachSpacing.xs))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedIndex?.let { onLink(it) } },
                enabled = selectedIndex != null,
                colors = ButtonDefaults.buttonColors(containerColor = GymCoachColors.Primary)
            ) { Text("Pair Exercises") }
        },
        dismissButton = {
            Row {
                if (existingGroup != null) {
                    TextButton(onClick = onUnlink) {
                        Icon(
                            Icons.Default.LinkOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Unlink", color = GymCoachColors.Danger)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = GymCoachColors.TextMuted)
                }
            }
        }
    )
}
