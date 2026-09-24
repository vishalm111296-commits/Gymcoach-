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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.gymcoach.app.core.progression.PlateCalculator
import com.gymcoach.app.ui.theme.GymCoachColors

data class BarbellPreset(
    val name: String,
    val weight: Double,
    val description: String
)

val BARBELL_PRESETS = listOf(
    BarbellPreset("Olympic", 20.0, "Standard 20kg"),
    BarbellPreset("Technique", 15.0, "Women / Junior 15kg"),
    BarbellPreset("EZ-Curl", 10.0, "Bicep / Tricep 10kg"),
    BarbellPreset("Light Bar", 5.0, "Light / Fixed 5kg")
)

@Composable
fun PlateCalculatorDialog(
    targetWeight: Double,
    barWeight: Double = 20.0,
    onDismiss: () -> Unit
) {
    var selectedBarWeight by remember { mutableDoubleStateOf(barWeight) }
    val breakdown = PlateCalculator.calculatePlates(targetWeight, selectedBarWeight)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = "Barbell Plate Calculator",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Plate Calculator",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Barbell Selection Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Barbell Type:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                                        text = "${preset.name} (${preset.weight.toInt()}kg)",
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
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Load", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${breakdown.totalWeight} kg",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Bar Weight", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${breakdown.barWeight} kg",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Per Side", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${breakdown.weightPerSide} kg",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Visual Barbell Sleeve Representation
                if (breakdown.platesPerSide.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Barbell Sleeve Preview (1 Side):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GymCoachColors.SurfaceDeep)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            // Collar
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height(48.dp)
                                    .background(Color.Gray, RoundedCornerShape(2.dp))
                            )
                            // Shaft line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .background(Color.DarkGray)
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
                                            item.plateWeight >= 25.0 -> 56.dp
                                            item.plateWeight >= 20.0 -> 52.dp
                                            item.plateWeight >= 15.0 -> 46.dp
                                            item.plateWeight >= 10.0 -> 40.dp
                                            item.plateWeight >= 5.0 -> 34.dp
                                            item.plateWeight >= 2.5 -> 28.dp
                                            else -> 24.dp
                                        }
                                        val plateWidth = when {
                                            item.plateWeight >= 20.0 -> 12.dp
                                            item.plateWeight >= 10.0 -> 10.dp
                                            else -> 8.dp
                                        }
                                        Box(
                                            modifier = Modifier
                                                .width(plateWidth)
                                                .height(plateHeight)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color(item.hexColor))
                                                .border(0.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
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
                    fontWeight = FontWeight.SemiBold
                )

                if (breakdown.platesPerSide.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (targetWeight <= selectedBarWeight) "Use empty barbell (${selectedBarWeight}kg)" else "No additional plates needed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        breakdown.platesPerSide.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(item.hexColor))
                                            .border(1.dp, Color.Gray, CircleShape)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = "${item.plateWeight} kg plate",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Text(
                                    text = "× ${item.count}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                if (breakdown.remainder > 0.0) {
                    Text(
                        text = "Remainder unachievable: ${breakdown.remainder} kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}
