package com.gymcoach.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.gymcoach.app.presentation.camera.CameraPreviewScreen
import com.gymcoach.app.presentation.detail.ExerciseDetailScreen
import com.gymcoach.app.presentation.history.WorkoutHistoryDetailScreen
import com.gymcoach.app.presentation.history.WorkoutHistoryScreen
import com.gymcoach.app.presentation.list.ExerciseListScreen
import com.gymcoach.app.presentation.measurement.screens.MeasurementScreen
import com.gymcoach.app.presentation.progress.ProgressDashboardScreen
import com.gymcoach.app.presentation.profile.ProfileScreen
import com.gymcoach.app.presentation.pr.PRScreen
import com.gymcoach.app.presentation.settings.SettingsScreen
import com.gymcoach.app.presentation.workout.WorkoutSessionScreen
import com.gymcoach.app.presentation.workout.WorkoutSummaryScreen
import androidx.navigation.compose.rememberNavController

object Routes {
    const val EXERCISE_LIST = "exercise_list"
    const val EXERCISE_DETAIL = "exercise_detail/{exerciseId}"
    const val WORKOUT_HISTORY = "workout_history"
    const val WORKOUT_HISTORY_DETAIL = "workout_history_detail/{workoutId}"
    const val WORKOUT_SESSION = "workout_session?workoutId={workoutId}"
    const val PROGRESS = "progress"
    const val MEASUREMENTS = "measurements"
    const val CAMERA = "camera"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val PR = "pr"
    const val WORKOUT_SUMMARY = "workout_summary/{workoutId}"

    fun exerciseDetail(exerciseId: Long) = "exercise_detail/$exerciseId"
    fun workoutHistoryDetail(workoutId: Long) = "workout_history_detail/$workoutId"
    fun workoutSession(workoutId: Long? = null) = if (workoutId != null) "workout_session?workoutId=$workoutId" else "workout_session"
    fun measurements() = MEASUREMENTS
    fun workoutSummary(workoutId: Long) = "workout_summary/$workoutId"
    fun pr() = PR
}

@Composable
fun GymCoachNavHost(
    navController: NavHostController,
    startDestination: String = Routes.EXERCISE_LIST
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val bottomBarRoutes = setOf(
        Routes.EXERCISE_LIST,
        Routes.WORKOUT_HISTORY,
        Routes.PROGRESS,
        Routes.PROFILE
    )

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.EXERCISE_LIST,
                        onClick = {
                            navController.navigate(Routes.EXERCISE_LIST) {
                                popUpTo(Routes.EXERCISE_LIST) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Exercises") },
                        label = { Text("Exercises") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.WORKOUT_HISTORY,
                        onClick = {
                            navController.navigate(Routes.WORKOUT_HISTORY) {
                                popUpTo(Routes.EXERCISE_LIST) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("History") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.PROGRESS,
                        onClick = {
                            navController.navigate(Routes.PROGRESS) {
                                popUpTo(Routes.EXERCISE_LIST) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.Insights, contentDescription = "Progress") },
                        label = { Text("Progress") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.PROFILE,
                        onClick = {
                            navController.navigate(Routes.PROFILE) {
                                popUpTo(Routes.EXERCISE_LIST) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination, modifier = Modifier.padding(innerPadding)) {
        composable(Routes.EXERCISE_LIST) {
            ExerciseListScreen(
                navController = navController,
                onExerciseClick = { navController.navigate(Routes.exerciseDetail(it)) },
                onHistoryClick = { navController.navigate(Routes.WORKOUT_HISTORY) },
                onProgressClick = { navController.navigate(Routes.PROGRESS) },
                onCameraClick = { navController.navigate(Routes.CAMERA) }
            )
        }
        composable(
            route = Routes.EXERCISE_DETAIL,
            arguments = listOf(navArgument("exerciseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getLong("exerciseId", -1L) ?: -1L
            if (exerciseId != -1L) {
                ExerciseDetailScreen(exerciseId = exerciseId, onBackClick = { navController.popBackStack() })
            }
        }
        composable(Routes.WORKOUT_HISTORY) {
            WorkoutHistoryScreen(
                onBackClick = { navController.popBackStack() },
                onDetailClick = { navController.navigate(Routes.workoutHistoryDetail(it)) },
                onResumeWorkout = { navController.navigate(Routes.workoutSession(it)) },
                onNewWorkout = { navController.navigate(Routes.workoutSession()) }
            )
        }
        composable(
            route = Routes.WORKOUT_HISTORY_DETAIL,
            arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getLong("workoutId", -1L) ?: -1L
            if (workoutId != -1L) {
                WorkoutHistoryDetailScreen(workoutId = workoutId, onBackClick = { navController.popBackStack() })
            }
        }
        composable(
            route = Routes.WORKOUT_SESSION,
            arguments = listOf(
                navArgument("workoutId") {
                    // navigation-compose primitive types (Long) cannot be nullable;
                    // -1L is the "no workout" sentinel. Workout ids are Room-autoincrement (positive).
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getLong("workoutId")?.takeIf { it != -1L }
            WorkoutSessionScreen(
                onBackClick = { navController.popBackStack() },
                workoutId = workoutId,
                onWorkoutComplete = { id -> navController.navigate(Routes.workoutSummary(id)) { popUpTo(Routes.workoutSession()) { inclusive = true } } }
            )
        }
        composable(Routes.PROGRESS) {
            ProgressDashboardScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Routes.MEASUREMENTS) {
            MeasurementScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Routes.CAMERA) {
            CameraPreviewScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onBackClick = { navController.popBackStack() },
                onMeasurementsClick = { navController.navigate(Routes.MEASUREMENTS) }
            )
        }
        composable(Routes.PR) {
            PRScreen(onBackClick = { navController.popBackStack() })
        }
        composable(
            route = Routes.WORKOUT_SUMMARY,
            arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getLong("workoutId", -1L) ?: -1L
            if (workoutId != -1L) {
                WorkoutSummaryScreen(
                    workoutId = workoutId,
                    onFinish = { navController.popBackStack(Routes.EXERCISE_LIST, inclusive = false) }
                )
            }
        }
    }
}
}