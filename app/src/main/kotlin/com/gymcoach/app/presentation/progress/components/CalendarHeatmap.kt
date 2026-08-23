package com.gymcoach.app.presentation.progress.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymcoach.app.ui.theme.AccentBlue
import com.gymcoach.app.ui.theme.DarkBackground
import com.gymcoach.app.ui.theme.DarkSurface
import com.gymcoach.app.ui.theme.TextSecondary
import java.time.DayOfWeek
import java.time.LocalDate

private const val HEATMAP_WEEKS = 12
private const val CELL_SIZE_DP = 14

/**
 * GitHub-style contribution grid: last 12 weeks, Monday-first columns.
 * AccentBlue cells mark completed workout days; rest days sit on DarkSurface
 * against the DarkBackground card so they stay visible.
 */
@Composable
fun CalendarHeatmap(
    workoutDays: Set<LocalDate>,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now()
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONSISTENCY",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    color = TextSecondary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(AccentBlue)
                    )
                    Text(
                        text = "Workout",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.padding(top = 14.dp))

            val startMonday = remember(today) {
                today.with(DayOfWeek.MONDAY).minusWeeks((HEATMAP_WEEKS - 1).toLong())
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { dayOfWeek ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(HEATMAP_WEEKS) { week ->
                            val date = startMonday.plusWeeks(week.toLong()).plusDays(dayOfWeek.toLong())
                            val isWorkoutDay = date in workoutDays
                            Box(
                                modifier = Modifier
                                    .size(CELL_SIZE_DP.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isWorkoutDay) AccentBlue else DarkSurface)
                            )
                        }
                    }
                }
            }
        }
    }
}
