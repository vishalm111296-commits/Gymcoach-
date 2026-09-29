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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

@Composable
fun PlateCalculatorDialog(
    targetWeight: Double,
    barWeight: Double = 20.0,
    weightUnit: WeightUnit = WeightUnit.KG,
    onDismiss: () -> Unit
) {
    val isImperial = weightUnit == WeightUnit.LBS
    val initialBarWeight = if (isImperial && barWeight == 20.0) 45.0 else barWeight
    var selectedBarWeight by remember(weightUnit) { mutableDoubleStateOf(initialBarWeight) }
    val presets = if (isImperial) BARBELL_PRESETS_IMPERIAL else BARBELL_PRESETS_METRIC
    val availablePlates = if (isImperial) PlateCalculator.STANDARD_IMPERIAL_PLATES else PlateCalculator.STANDARD_METRIC_PLATES

    // Memoize: only recalculate when weight inputs actually change
    val breakdown = remember(targetWeight, selectedBarWeight, availablePlates) {
        PlateCalculator.calculatePlates(targetWeight, selectedBarWeight, availablePlates)
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
                                onClick = { selectedBarWeight = preset.weight },
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

                // Visual Barbell Sleeve Representation
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
                                .padding(horizontal = GymCoachSpacing.sm),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            // Shaft line — metal bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                                .border(0.5.dp, Color.White.copy(alpha = 0.4f), GymCoachShapes.xs)
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
                            .padding(GymCoachSpacing.lg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (targetWeight <= selectedBarWeight)
                                "Use empty barbell (${selectedBarWeight}${weightUnit.code})"
                            else
                                "No additional plates needed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GymCoachColors.TextSecondary
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm)
                    ) {
                        breakdown.platesPerSide.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(GymCoachShapes.sm)
                                    .background(GymCoachColors.SurfaceDeep)
                                    .border(GymCoachBorders.subtle, GymCoachShapes.sm)
                                    .padding(horizontal = GymCoachSpacing.md, vertical = GymCoachSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(item.hexColor))
                                            .border(1.dp, GymCoachColors.BorderSubtle, CircleShape)
                                    )
                                    Spacer(Modifier.width(GymCoachSpacing.md))
                                    Text(
                                        text = "${item.plateWeight} ${weightUnit.code} plate",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = GymCoachColors.TextPrimary
                                    )
                                }

                                Text(
                                    text = "× ${item.count}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GymCoachColors.Primary
                                )
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
            TextButton(onClick = onDismiss) {
                Text(
                    "Close",
                    fontWeight = FontWeight.Bold,
                    color = GymCoachColors.TextSecondary
                )
            }
        }
    )
}
