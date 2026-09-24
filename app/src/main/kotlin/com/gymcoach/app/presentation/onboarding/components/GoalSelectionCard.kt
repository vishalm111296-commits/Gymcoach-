package com.gymcoach.app.presentation.onboarding.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes

/**
 * Large selection card for onboarding steps.
 * Selected: Primary border + background tint. Unselected: BorderSubtle.
 */
@Composable
fun GoalSelectionCard(
    goal: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .clip(GymCoachShapes.md)
            .clickable(onClick = onClick),
        shape = GymCoachShapes.md,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) GymCoachColors.Primary.copy(alpha = 0.15f) else GymCoachColors.SurfaceCard
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) GymCoachColors.Primary else GymCoachColors.BorderSubtle
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (isSelected) GymCoachColors.Primary else GymCoachColors.SurfaceInput,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconFor(goal),
                    contentDescription = null,
                    tint = if (isSelected) GymCoachColors.TextPrimary else GymCoachColors.TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = goal,
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

private fun iconFor(goal: String): ImageVector = when (goal) {
    "Strength" -> Icons.Filled.FitnessCenter
    "Fat Loss" -> Icons.Filled.LocalFireDepartment
    "Muscle Gain" -> Icons.AutoMirrored.Filled.TrendingUp
    "General Fitness" -> Icons.Filled.Favorite
    else -> Icons.Filled.AccessibilityNew // V-Taper Hypertrophy
}
