package com.gymcoach.app.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymcoach.app.ui.theme.AccentBlue
import com.gymcoach.app.ui.theme.DarkSurface
import com.gymcoach.app.ui.theme.MuscleActive
import com.gymcoach.app.ui.theme.MuscleRest
import com.gymcoach.app.ui.theme.TextSecondary
import com.gymcoach.app.ui.theme.TextTertiary
import com.gymcoach.app.ui.theme.DarkCard
import com.gymcoach.app.ui.theme.WarmWhite

/** Transparent volume metric - no composite "score". */
data class VtaperMuscleData(
    val label: String,
    val current: Int, // planned sets this week
    val target: Int
)

/**
 * Horizontal volume bars for the V-taper priority muscles.
 * MuscleActive fill for achieved, MuscleRest track for remaining.
 */
@Composable
fun VtaperFocusCard(
    muscleData: List<VtaperMuscleData>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "V-TAPER FOCUS",
                style = MaterialTheme.typography.labelMedium,
                color = AccentBlue,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Planned weekly sets vs optimal band ($TARGET_SETS_LABEL)",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
                modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
            )
            muscleData.forEach { data ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = data.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.width(100.dp)
                    )
                    LinearProgressIndicator(
                        progress = {
                            if (data.target > 0) {
                                (data.current.toFloat() / data.target).coerceIn(0f, 1f)
                            } else 0f
                        },
                        color = MuscleActive,
                        trackColor = MuscleRest,
                        modifier = Modifier
                            .weight(1f)
                            .size(height = 10.dp, width = 0.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )
                    Text(
                        text = "${data.current} / ${data.target}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = WarmWhite,
                        modifier = Modifier.width(50.dp)
                    )
                }
            }
        }
    }
}

private const val TARGET_SETS_LABEL = "14 sets"
