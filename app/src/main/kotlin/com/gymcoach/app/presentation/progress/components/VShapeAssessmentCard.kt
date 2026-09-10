package com.gymcoach.app.presentation.progress.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymcoach.app.core.assessment.VShapeAssessmentCalculator
import com.gymcoach.app.core.assessment.VShapeLevel
import com.gymcoach.app.core.assessment.VShapeMorphology
import com.gymcoach.app.ui.theme.AccentBlue
import com.gymcoach.app.ui.theme.DarkSurface
import com.gymcoach.app.ui.theme.TextPrimary
import com.gymcoach.app.ui.theme.TextSecondary

/**
 * V-Shape Assessment card. Shows the morphology level derived from the latest
 * body measurements and the current V-taper training balance.
 *
 * NOTE: V-shape level thresholds and insight wording are product heuristics
 * (training-through-athletic-build guidance), not clinical claims.
 */
@Composable
fun VShapeAssessmentCard(
    morphology: VShapeMorphology,
    level: VShapeLevel,
    insights: List<String>,
    overallBalance: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "V-SHAPE ASSESSMENT",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                color = TextSecondary
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = level.label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AccentBlue
            )

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                MetricCell(
                    label = "SHOULDER / WAIST",
                    value = if (morphology.hasMeasurements) {
                        VShapeAssessmentCalculator.formatRatio(morphology.shoulderWaistRatio)
                    } else {
                        "N/A"
                    },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(16.dp))
                MetricCell(
                    label = "TRAINING BALANCE",
                    value = overallBalance,
                    modifier = Modifier.weight(1f)
                )
            }

            if (insights.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Column {
                    insights.forEach { insight ->
                        Text(
                            text = "\u2022  $insight",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.sp,
            color = TextSecondary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}