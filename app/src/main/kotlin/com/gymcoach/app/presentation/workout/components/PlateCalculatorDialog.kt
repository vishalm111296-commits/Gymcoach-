package com.gymcoach.app.presentation.workout.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymcoach.app.core.preferences.WeightUnit
import com.gymcoach.app.core.progression.PlateCalculator
import com.gymcoach.app.ui.theme.GymCoachBorders
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes
import com.gymcoach.app.ui.theme.GymCoachSpacing

data class BarbellPreset(
    val name: String,
    val weight: Double,
    val description: String
)

val BARBELL_PRESETS_METRIC = listOf(
    BarbellPreset("Olympic", 20.0, "Standard 20kg"),
    BarbellPreset("Technique", 15.0, "Women / Junior 15kg"),
    BarbellPreset("EZ-Curl", 10.0, "Bicep / Tricep 10kg"),
    BarbellPreset("Light Bar", 5.0, "Light / Fixed 5kg")
)

val BARBELL_PRESETS_IMPERIAL = listOf(
    BarbellPreset("Olympic", 45.0, "Standard 45 lbs"),
    BarbellPreset("Women's", 35.0, "Women's / Junior 35 lbs"),
    BarbellPreset("EZ-Curl", 25.0, "Bicep / Tricep 25 lbs"),
    BarbellPreset("Technique", 15.0, "Light / Technique 15 lbs")
)

val BARBELL_PRESETS = BARBELL_PRESETS_METRIC

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlateCalculatorDialog(
    targetWeight: Double,
    barWeight: Double = 20.0,
    weightUnit: WeightUnit = WeightUnit.KG,
    onDismiss: () -> Unit,
    onWeightSelected: ((Double) -> Unit)? = null,
    onBarWeightChange: ((Double) -> Unit)? = null
) {
    val isImperial = weightUnit == WeightUnit.LBS
    val initialBarWeight = when {
        isImperial && barWeight == 20.0 -> 45.0
        !isImperial && barWeight == 45.0 -> 20.0
        else -> barWeight
    }
    var selectedBarWeight by rememberSaveable(weightUnit, barWeight) { mutableDoubleStateOf(initialBarWeight) }
    var currentTargetWeight by rememberSaveable(targetWeight, weightUnit) { mutableDoubleStateOf(targetWeight) }

    val presets = if (isImperial) BARBELL_PRESETS_IMPERIAL else BARBELL_PRESETS_METRIC
    val availablePlates = if (isImperial) PlateCalculator.STANDARD_IMPERIAL_PLATES else PlateCalculator.STANDARD_METRIC_PLATES

    // Recalculate plate breakdown based on interactive target weight & selected bar
    val breakdown = remember(currentTargetWeight, selectedBarWeight, availablePlates) {
        PlateCalculator.calculatePlates(currentTargetWeight, selectedBarWeight, availablePlates)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GymCoachColors.SurfaceDeep,
        tonalElevation = 0.dp,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = "Barbell Plate Calculator",
                    tint = GymCoachColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(GymCoachSpacing.sm))
                Text(
                    text = "Plate Calculator",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GymCoachColors.TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
                verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.md)
            ) {
                // Interactive Target Weight Steppers
                Card(
                    colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCardElevated),
                    border = GymCoachBorders.subtle,
                    shape = GymCoachShapes.sm
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Target Weight:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = GymCoachColors.TextSecondary
                            )
                            val displayWeight = if (currentTargetWeight % 1.0 == 0.0) {
                                currentTargetWeight.toInt().toString()
                            } else {
                                currentTargetWeight.toString()
                            }
                            Text(
                                text = "$displayWeight ${weightUnit.code}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GymCoachColors.Primary
                            )
                        }

                        val stepSmall = if (isImperial) 5.0 else 2.5
                        val stepLarge = if (isImperial) 10.0 else 5.0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    currentTargetWeight = (currentTargetWeight - stepLarge).coerceAtLeast(selectedBarWeight)
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f).height(34.dp),
                                shape = GymCoachShapes.xs,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GymCoachColors.BorderSubtle)
                            ) {
                                Text("-$stepLarge", style = MaterialTheme.typography.labelSmall, color = GymCoachColors.TextPrimary)
                            }
                            OutlinedButton(
                                onClick = {
                                    currentTargetWeight = (currentTargetWeight - stepSmall).coerceAtLeast(selectedBarWeight)
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f).height(34.dp),
                                shape = GymCoachShapes.xs,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GymCoachColors.BorderSubtle)
                            ) {
                                Text("-$stepSmall", style = MaterialTheme.typography.labelSmall, color = GymCoachColors.TextPrimary)
                            }
                            OutlinedButton(
                                onClick = {
                                    currentTargetWeight = currentTargetWeight + stepSmall
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f).height(34.dp),
                                shape = GymCoachShapes.xs,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GymCoachColors.BorderSubtle)
                            ) {
                                Text("+$stepSmall", style = MaterialTheme.typography.labelSmall, color = GymCoachColors.Primary)
                            }
                            OutlinedButton(
                                onClick = {
                                    currentTargetWeight = currentTargetWeight + stepLarge
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f).height(34.dp),
                                shape = GymCoachShapes.xs,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GymCoachColors.BorderSubtle)
                            ) {
                                Text("+$stepLarge", style = MaterialTheme.typography.labelSmall, color = GymCoachColors.Primary)
                            }
                        }
                    }
                }

                // Barbell Selection Chips
                Column(verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.xs)) {
                    Text(
                        text = "Barbell Type:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GymCoachColors.TextSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm)
                    ) {
                        presets.forEach { preset ->
                            val isSelected = selectedBarWeight == preset.weight
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedBarWeight = preset.weight
                                    onBarWeightChange?.invoke(preset.weight)
                                },
                                label = {
                                    Text(
                                        text = "${preset.name} (${preset.weight.toInt()}${weightUnit.code})",
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
                }

                // Summary Load Card
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
                            Text(
                                "Total Load",
                                style = MaterialTheme.typography.labelSmall,
                                color = GymCoachColors.TextSecondary
                            )
                            Text(
                                "${breakdown.totalWeight} ${weightUnit.code}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GymCoachColors.Primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Bar Weight",
                                style = MaterialTheme.typography.labelSmall,
                                color = GymCoachColors.TextSecondary
                            )
                            Text(
                                "${breakdown.barWeight} ${weightUnit.code}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = GymCoachColors.TextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Per Side",
                                style = MaterialTheme.typography.labelSmall,
                                color = GymCoachColors.TextSecondary
                            )
                            Text(
                                "${breakdown.weightPerSide} ${weightUnit.code}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GymCoachColors.Primary
                            )
                        }
                    }
                }

                // Visual Barbell Sleeve Representation (Wrapped in horizontalScroll for 6+ plates without clipping)
                if (breakdown.platesPerSide.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.xs)) {
                        Text(
                            text = "Barbell Sleeve Preview (1 Side):",
                            style = MaterialTheme.typography.labelSmall,
                            color = GymCoachColors.TextSecondary
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(GymCoachShapes.sm)
                                .background(GymCoachColors.SurfaceDeep)
                                .border(GymCoachBorders.subtle, GymCoachShapes.sm)
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = GymCoachSpacing.sm),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            // Shaft line — metal bar
                            Box(
                                modifier = Modifier
                                    .width(420.dp)
                                    .height(8.dp)
                                    .background(GymCoachColors.SurfaceElevated)
                            )
                            // Collar — visible lock end
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height(48.dp)
                                    .background(GymCoachColors.BorderSubtle, GymCoachShapes.xs)
                            )
                            // Stacked Plates from inside to outside
                            Row(
                                modifier = Modifier.padding(start = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                breakdown.platesPerSide.forEach { item ->
                                    repeat(item.count) {
                                        val plateHeight = when {
                                            item.plateWeight >= (if (isImperial) 45.0 else 25.0) -> 56.dp
                                            item.plateWeight >= (if (isImperial) 35.0 else 20.0) -> 52.dp
                                            item.plateWeight >= (if (isImperial) 25.0 else 15.0) -> 46.dp
                                            item.plateWeight >= 10.0 -> 40.dp
                                            item.plateWeight >= 5.0 -> 34.dp
                                            item.plateWeight >= 2.5 -> 28.dp
                                            else -> 24.dp
                                        }
                                        val plateWidth = when {
                                            item.plateWeight >= (if (isImperial) 35.0 else 20.0) -> 12.dp
                                            item.plateWeight >= 10.0 -> 10.dp
                                            else -> 8.dp
                                        }
                                        Box(
                                            modifier = Modifier
                                                .width(plateWidth)
                                                .height(plateHeight)
                                                .clip(GymCoachShapes.xs)
                                                .background(Color(item.hexColor))
                                                .border(0.5.dp, GymCoachColors.BorderSubtle, GymCoachShapes.xs)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Plates required per side:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = GymCoachColors.TextPrimary
                )

                if (breakdown.platesPerSide.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(GymCoachSpacing.md),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentTargetWeight <= selectedBarWeight)
                                "Use empty barbell (${selectedBarWeight.toInt()}${weightUnit.code})"
                            else
                                "No additional plates needed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GymCoachColors.TextSecondary
                        )
                    }
                } else {
                    // Space-efficient compact plate badges/chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        breakdown.platesPerSide.forEach { item ->
                            Surface(
                                shape = GymCoachShapes.sm,
                                color = GymCoachColors.SurfaceCardElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GymCoachColors.BorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(item.hexColor))
                                            .border(0.5.dp, GymCoachColors.BorderSubtle, CircleShape)
                                    )
                                    Text(
                                        text = "${item.plateWeight}${weightUnit.code}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = GymCoachColors.TextPrimary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(GymCoachShapes.xs)
                                            .background(GymCoachColors.Primary.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "× ${item.count}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                            color = GymCoachColors.Primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (breakdown.remainder > 0.0) {
                    Text(
                        text = "Remainder unachievable: ${breakdown.remainder} ${weightUnit.code}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GymCoachColors.ErrorRed
                    )
                }
            }
        },
        confirmButton = {
            if (onWeightSelected != null && currentTargetWeight != targetWeight) {
                Button(
                    onClick = {
                        onWeightSelected(currentTargetWeight)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GymCoachColors.Primary),
                    shape = GymCoachShapes.sm
                ) {
                    val displayWeight = if (currentTargetWeight % 1.0 == 0.0) currentTargetWeight.toInt().toString() else currentTargetWeight.toString()
                    Text("Apply ($displayWeight${weightUnit.code})", fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(
                        "Close",
                        fontWeight = FontWeight.Bold,
                        color = GymCoachColors.TextSecondary
                    )
                }
            }
        },
        dismissButton = if (onWeightSelected != null && currentTargetWeight != targetWeight) {
            {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = GymCoachColors.TextSecondary)
                }
            }
        } else null
    )
}

@Composable
fun PlateCalculatorDialog(
    initialTargetWeight: Double,
    weightUnit: WeightUnit,
    onDismiss: () -> Unit,
    onWeightSelected: (Double) -> Unit,
    onBarWeightChange: ((Double) -> Unit)? = null
) {
    PlateCalculatorDialog(
        targetWeight = initialTargetWeight,
        weightUnit = weightUnit,
        onDismiss = onDismiss,
        onWeightSelected = onWeightSelected,
        onBarWeightChange = onBarWeightChange
    )
}

