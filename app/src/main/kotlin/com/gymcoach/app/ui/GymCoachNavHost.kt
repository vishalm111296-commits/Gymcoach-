package com.gymcoach.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
    NavHost(navController, startDestination) {
        composable(Routes.EXERCISE_LIST) {
            ExerciseListScreen(onBackClick = { navController.popBackStack() })
        }
        composable(
            route = Routes.EXERCISE_DETAIL,
            arguments = listOf(navArgument("exerciseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val exerciseId = backStackEntry.getLong("exerciseId", -1)
            if (exerciseId != -1) {
                ExerciseDetailScreen(exerciseId = exerciseId, onBackClick = { navController.popBackStack() })
            }
        }
        composable(Routes.WORKOUT_HISTORY) {
            WorkoutHistoryScreen(onBackClick = { navController.popBackStack() })
        }
        composable(
            route = Routes.WORKOUT_HISTORY_DETAIL,
            arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
        ) { backStackEntry ->
            val workoutId = backStackEntry.getLong("workoutId", -1)
            if (workoutId != -1) {
                WorkoutHistoryDetailScreen(workoutId = workoutId, onBackClick = { navController.popBackStack() })
            }
        }
        composable(
            route = Routes.WORKOUT_SESSION,
            arguments = listOf(navArgument("workoutId") { type = NavType.LongType, defaultValue = "" })
        ) { backStackEntry ->
            val workoutId = backStackEntry.getString("workoutId")?.toLongOrNull()
            WorkoutSessionScreen(
                onBackClick = { navController.popBackStack() },
                workoutId = workoutId
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
            ProfileScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Routes.PR) {
            PRScreen(onBackClick = { navController.popBackStack() })
        }
        composable(
            route = Routes.WORKOUT_SUMMARY,
            arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
        ) { backStackEntry ->
            val workoutId = backStackEntry.getLong("workoutId", -1)
            if (workoutId != -1) {
                WorkoutSummaryScreen(
                    workoutId = workoutId,
                    onFinish = { navController.popBackStack(Routes.EXERCISE_LIST, inclusive = false) }
                )
            }
        }
    }
}