package com.gymcoach.app.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymcoach.app.data.local.entity.ReadinessEntity
import com.gymcoach.app.ui.theme.AccentBlue
import com.gymcoach.app.ui.theme.DarkSurface
import com.gymcoach.app.ui.theme.TextSecondary
import com.gymcoach.app.ui.theme.WarmWhite
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardReadinessCard(
    latestReadiness: ReadinessEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Check if readiness is from today
    val isFromToday = latestReadiness?.let {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        it.recordedAt >= today.timeInMillis
    } ?: false

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RECOVERY & READINESS",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentBlue,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                )

                if (isFromToday && latestReadiness != null) {
                    Text(
                        text = "Score: %.1f/5.0 \u2022 %s".format(
                            latestReadiness.readinessScore,
                            if (latestReadiness.isRestDayRecommended) "Rest Recommended" else "Ready"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmWhite,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(
                        text = "Log how you're feeling today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Text(
                text = "→",
                style = MaterialTheme.typography.headlineMedium,
                color = AccentBlue,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
