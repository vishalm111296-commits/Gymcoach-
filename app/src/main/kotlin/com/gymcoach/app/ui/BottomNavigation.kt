package com.gymcoach.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.gymcoach.app.ui.theme.AccentBlue
import com.gymcoach.app.ui.theme.DarkBackground
import com.gymcoach.app.ui.theme.TextTertiary

private data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

private val BOTTOM_NAV_ITEMS = listOf(
    BottomNavItem(Routes.HOME, "Home", Icons.Filled.Home),
    BottomNavItem(Routes.workoutSession(), "Workout", Icons.Filled.FitnessCenter),
    BottomNavItem(Routes.EXERCISE_LIST, "Program", Icons.Filled.CalendarMonth),
    BottomNavItem(Routes.PROGRESS, "Progress", Icons.AutoMirrored.Filled.TrendingUp),
    BottomNavItem(Routes.PROFILE, "Profile", Icons.Filled.Person)
)

/**
 * Icon-only bottom navigation. Active tab AccentBlue, inactive TextTertiary.
 */
@Composable
fun GymCoachBottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(color = DarkBackground, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BOTTOM_NAV_ITEMS.forEach { item ->
                val active = currentRoute == item.route
                IconButton(onClick = { onNavigate(item.route) }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (active) AccentBlue else TextTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
