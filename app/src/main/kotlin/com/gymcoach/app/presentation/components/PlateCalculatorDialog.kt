package com.gymcoach.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gymcoach.app.core.util.PlateCalculator
import com.gymcoach.app.domain.model.Plate

@Composable
fun PlateCalculatorDialog(
    targetWeight: Double,
    barWeight: Double,
    inventory: List<Plate>,
    onDismiss: () -> Unit
) {
    val result by rememberCollectable(targetWeight, barWeight, inventory) {
        PlateCalculator.calculate(targetWeight, barWeight, inventory)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Plate Calculator")
        },
        text = {
            if (result == null) {
                Text("Cannot achieve exact weight.")
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Bar weight: ${barWeight} kg",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Target: ${targetWeight} kg",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        result.plates.forEach {
                            Text(
                                text = "${it.count} x ${it.weight} kg per side",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Total plates: ${result.plates.sumOf { it.count }}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun rememberCollectable(
    targetWeight: Double,
    barWeight: Double,
    inventory: List<Plate>
): State<PlateResult?> {
    return remember(targetWeight, barWeight, inventory) {
        derivedStateOf {
            PlateCalculator.calculate(targetWeight, barWeight, inventory)
        }
    }
}

class PlateResult(
    val plates: List<PlateCombo>,
    val totalPlates: Int
)

class PlateCombo(
    val weight: Double,
    val count: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlateCombo) return false
        return weight == other.weight && count == other.count
    }

    override fun hashCode(): Int {
        var result = weight.hashCode()
        result = 31 * result + count
        return result
    }
}