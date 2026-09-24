package com.gymcoach.app.presentation.onboarding.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes

/**
 * Equipment options matching ExerciseEntity.equipment values in the database.
 * Names must match exactly what the seed data and ProgramGenerator expect.
 */
private val EQUIPMENT_OPTIONS = listOf(
    "Dumbbell" to "Presses, rows, curls, lateral raises",
    "Flat Bench" to "Bench press, supported rows, step-ups",
    "Pull-up Bar" to "Pull-ups, hangs, direct lat work",
    "Resistance Band" to "Assisted reps and isolation anywhere",
    "Barbell" to "Heavy compounds - squat, bench, deadlift",
    "Cable" to "Constant tension - pulldowns, flyes, pushdowns"
)

/**
 * Equipment checklist. Bodyweight movements are always available and never listed.
 */
@Composable
fun EquipmentChecklist(
    availableEquipment: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EQUIPMENT_OPTIONS.forEach { (name, description) ->
            val checked = name in availableEquipment
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .clip(GymCoachShapes.md)
                    .clickable { onToggle(name) },
                shape = GymCoachShapes.md,
                colors = CardDefaults.cardColors(
                    containerColor = if (checked) GymCoachColors.Primary.copy(alpha = 0.15f) else GymCoachColors.SurfaceCard
                ),
                border = BorderStroke(
                    width = if (checked) 2.dp else 1.dp,
                    color = if (checked) GymCoachColors.Primary else GymCoachColors.BorderSubtle
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { onToggle(name) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = GymCoachColors.Primary,
                            checkmarkColor = GymCoachColors.TextPrimary,
                            uncheckedColor = GymCoachColors.TextMuted
                        )
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            color = GymCoachColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = GymCoachColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}
