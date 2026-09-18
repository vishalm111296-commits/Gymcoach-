package com.gymcoach.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymcoach.app.ui.theme.AccentBlue
import com.gymcoach.app.ui.theme.AccentBlueLight
import com.gymcoach.app.ui.theme.DarkBackground
import com.gymcoach.app.ui.theme.DarkSurface
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.TextTertiary

private data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

private val BOTTOM_NAV_ITEMS = listOf(
    BottomNavItem("home", "Home", Icons.Filled.Home),
    BottomNavItem("workout", "Workout", Icons.Filled.FitnessCenter),
    BottomNavItem("exercise_list", "Exercises", Icons.Filled.Search),
    BottomNavItem("program_detail", "Program", Icons.Filled.CalendarMonth),
    BottomNavItem("progress", "Progress", Icons.AutoMirrored.Filled.TrendingUp),
    BottomNavItem("profile", "Profile", Icons.Filled.Person)
)

/**
 * Premium bottom navigation bar with elevated surface, subtle top border,
 * haptic feedback, active pill highlight, and accessible 48dp+ touch targets.
 */
@Composable
fun GymCoachBottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = 1.dp,
            color = GymCoachColors.BorderSubtle
        )

        Surface(
            color = DarkSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BOTTOM_NAV_ITEMS.forEach { item ->
                    val active = currentRoute == item.route
                    val iconTint by animateColorAsState(
                        targetValue = if (active) AccentBlueLight else TextTertiary,
                        animationSpec = tween(durationMillis = 200),
                        label = "iconTint"
                    )
                    val bgColor by animateColorAsState(
                        targetValue = if (active) AccentBlue.copy(alpha = 0.16f) else Color.Transparent,
                        animationSpec = tween(durationMillis = 200),
                        label = "bgColor"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!active) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onNavigate(item.route)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = iconTint
                            )
                            if (active) {
                                Spacer(Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .size(width = 12.dp, height = 2.dp)
                                        .clip(CircleShape)
                                        .background(AccentBlue)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
