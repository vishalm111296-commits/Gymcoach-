package com.gymcoach.app.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gymcoach.app.ui.GymCoachBottomNav
import com.gymcoach.app.ui.GymCoachNavHost
import com.gymcoach.app.ui.Routes

@Composable
fun GymCoachAppShell(
    startDestination: String = Routes.ONBOARDING,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Screens where bottom navigation should be hidden
    val hideBottomNavRoutes = listOf(
        Routes.ONBOARDING,
        Routes.WORKOUT_SESSION,
        Routes.CAMERA
    )

    val shouldShowBottomNav = currentRoute != null &&
        !hideBottomNavRoutes.any { currentRoute.startsWith(it.substringBefore("?").substringBefore("/")) }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (shouldShowBottomNav) {
                // Map logical tabs back to actual routes
                GymCoachBottomNav(
                    currentRoute = mapRouteToTab(currentRoute),
                    onNavigate = { tabRoute ->
                        val actualRoute = mapTabToRoute(tabRoute)
                        if (currentRoute != actualRoute) {
                            navController.navigate(actualRoute) {
                                popUpTo(Routes.HOME) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            GymCoachNavHost(
                navController = navController,
                startDestination = startDestination
            )
        }
    }
}

private fun mapRouteToTab(currentRoute: String?): String? {
    return when {
        currentRoute == null -> null
        currentRoute == Routes.HOME -> "home"
        currentRoute.startsWith("workout_history") -> "workout"
        currentRoute.startsWith("exercise_list") || currentRoute.startsWith("exercise_detail") -> "program"
        currentRoute.startsWith(Routes.PROGRESS) -> "progress"
        currentRoute.startsWith(Routes.PROFILE) -> "profile"
        else -> null
    }
}

private fun mapTabToRoute(tab: String): String {
    return when (tab) {
        "home" -> Routes.HOME
        "workout" -> Routes.WORKOUT_HISTORY
        "program" -> Routes.EXERCISE_LIST
        "progress" -> Routes.PROGRESS
        "profile" -> Routes.PROFILE
        else -> Routes.HOME
    }
}
