package com.gymcoach.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

private val BOTTOM_NAV_ITEMS = listOf(
    BottomNavItem("home", "Home", Icons.Filled.Home),
    BottomNavItem("workout", "Workout", Icons.Filled.FitnessCenter),
    BottomNavItem("program", "Program", Icons.Filled.CalendarMonth),
    BottomNavItem("progress", "Progress", Icons.AutoMirrored.Filled.TrendingUp),
    BottomNavItem("profile", "Profile", Icons.Filled.Person)
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
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
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
                IconButton(
                    onClick = { onNavigate(item.route) },
                    modifier = Modifier
                        .size(48.dp)
                        .semantics {
                            selected = active
                            role = Role.Tab
                        }
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
