package com.gymcoach.app.presentation.workout.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymcoach.app.core.progression.OneRepMaxCalculator
import com.gymcoach.app.core.progression.WarmupCalculator
import com.gymcoach.app.ui.theme.AccentBlue
import com.gymcoach.app.ui.theme.GymCoachBorders
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes
import com.gymcoach.app.ui.theme.GymCoachSpacing
import com.gymcoach.app.ui.theme.TextPrimary
import com.gymcoach.app.ui.theme.TextSecondary

@Composable
fun WarmupCalculatorDialog(
    exerciseName: String,
    targetWeight: Double,
    barWeight: Double = 20.0,
    onInsertWarmupSets: ((List<WarmupCalculator.WarmupSetProtocol>) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var selectedBarWeight by remember { mutableDoubleStateOf(barWeight) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Warmup Protocol, 1 = 1RM & Zones
    var inserted by remember { androidx.compose.runtime.mutableStateOf(false) }

    val warmupPlan = remember(targetWeight, selectedBarWeight) {
        WarmupCalculator.calculateWarmupPlan(targetWeight, selectedBarWeight)
    }

    val oneRepMaxProfile = remember(targetWeight) {
        OneRepMaxCalculator.calculateProfile(targetWeight, reps = 5)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Whatshot,
                    contentDescription = "Warm-Up Protocol",
                    tint = GymCoachColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Warm-Up & 1RM Suite",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
                verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.md)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = GymCoachColors.SurfaceDeep,
                    contentColor = GymCoachColors.Primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = GymCoachColors.Primary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Warm-Up Ramp",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "1RM & Zones",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }

                if (selectedTab == 0) {
                    // --- Warmup Tab ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm)
                    ) {
                        // Barbell Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BARBELL_PRESETS.forEach { preset ->
                                val isSelected = selectedBarWeight == preset.weight
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedBarWeight = preset.weight },
                                    label = {
                                        Text(
                                            "${preset.name} (${preset.weight.toInt()}kg)",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GymCoachColors.Primary.copy(alpha = 0.25f),
                                        selectedLabelColor = GymCoachColors.Primary
                                    )
                                )
                            }
                        }

                        // Target Summary Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = GymCoachColors.SurfaceCardElevated
                            ),
                            border = GymCoachBorders.subtle,
                            shape = GymCoachShapes.sm
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(GymCoachSpacing.md),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Target Work Load", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text(
                                        "${warmupPlan.workingWeight} kg",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = GymCoachColors.Primary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Est. Warm-up Time", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text(
                                        "~${warmupPlan.estimatedDurationMinutes} min",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GymCoachColors.CyanAccent
                                    )
                                }
                            }
                        }

                        // Warm-up sets list
                        warmupPlan.sets.forEach { ws ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(GymCoachShapes.sm)
                                    .background(GymCoachColors.SurfaceDeep)
                                    .border(GymCoachBorders.subtle, GymCoachShapes.sm)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(GymCoachColors.Primary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "W${ws.setNumber}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = GymCoachColors.Primary
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "${ws.weight} kg × ${ws.reps} reps",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = ws.purpose,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Text(
                                    text = "${ws.restSeconds}s rest",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = GymCoachColors.CyanAccent
                                )
                            }
                        }
                    }
                } else {
                    // --- 1RM & Zones Tab ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = GymCoachColors.SurfaceCardElevated
                            ),
                            border = GymCoachBorders.subtle,
                            shape = GymCoachShapes.sm
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(GymCoachSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Average Estimated 1RM", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    "${oneRepMaxProfile.average1RM} kg",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GymCoachColors.GoldAccent
                                )
                                Text(
                                    "Epley: ${oneRepMaxProfile.epley1RM}kg  •  Brzycki: ${oneRepMaxProfile.brzycki1RM}kg",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Text(
                            "Percentage Training Zones",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        oneRepMaxProfile.zones.forEach { zone ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(GymCoachShapes.sm)
                                    .background(GymCoachColors.SurfaceDeep)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "${zone.percentage}% (${zone.weight} kg)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = zone.trainingGoal,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                                Text(
                                    text = "${zone.repRange} reps",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GymCoachColors.Primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onInsertWarmupSets != null && selectedTab == 0) {
                    Button(
                        onClick = {
                            onInsertWarmupSets(warmupPlan.sets)
                            inserted = true
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GymCoachColors.Primary),
                        shape = GymCoachShapes.sm,
                        enabled = !inserted && warmupPlan.sets.isNotEmpty()
                    ) {
                        Icon(
                            if (inserted) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (inserted) "Added" else "Add Warm-Up Sets")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = GymCoachColors.TextSecondary)
                }
            }
        }
    )
}
