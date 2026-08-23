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
import com.gymcoach.app.presentation.home.HomeDashboardScreen
import com.gymcoach.app.presentation.list.ExerciseListScreen
import com.gymcoach.app.presentation.onboarding.OnboardingScreen
import com.gymcoach.app.presentation.profile.ProfileScreen
import com.gymcoach.app.presentation.progress.ProgressDashboardScreen
import com.gymcoach.app.presentation.workout.WorkoutSessionScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val EXERCISE_LIST = "exercise_list"
    const val EXERCISE_DETAIL = "exercise_detail/{exerciseId}"
    const val WORKOUT_HISTORY = "workout_history"
    const val WORKOUT_HISTORY_DETAIL = "workout_history_detail/{workoutId}"
    const val WORKOUT_SESSION = "workout_session?workoutId={workoutId}"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"
    const val CAMERA = "camera"

    fun exerciseDetail(exerciseId: Long) = "exercise_detail/$exerciseId"
    fun workoutHistoryDetail(workoutId: Long) = "workout_history_detail/$workoutId"
    fun workoutSession(workoutId: Long? = null) = if (workoutId != null) "workout_session?workoutId=$workoutId" else "workout_session"
}

@Composable
fun GymCoachNavHost(
    navController: NavHostController,
    startDestination: String = Routes.ONBOARDING
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeDashboardScreen(
                onStartWorkout = { navController.navigate(Routes.workoutSession()) },
                onViewProgram = { navController.navigate(Routes.EXERCISE_LIST) },
                onNavigateToProgress = { navController.navigate(Routes.PROGRESS) },
                onNavigateToProfile = { navController.navigate(Routes.PROFILE) }
            )
        }

        composable(Routes.EXERCISE_LIST) {
            ExerciseListScreen(
                onExerciseClick = { exerciseId ->
                    navController.navigate(Routes.exerciseDetail(exerciseId))
                },
                onHistoryClick = { navController.navigate(Routes.WORKOUT_HISTORY) },
                onProgressClick = { navController.navigate(Routes.PROGRESS) },
                onCameraClick = { navController.navigate(Routes.CAMERA) }
            )
        }

        composable(
            route = Routes.EXERCISE_DETAIL,
            arguments = listOf(
                navArgument("exerciseId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getLong("exerciseId") ?: return@composable
            ExerciseDetailScreen(
                exerciseId = exerciseId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.WORKOUT_HISTORY) {
            WorkoutHistoryScreen(
                onBackClick = { navController.popBackStack() },
                onDetailClick = { workoutId ->
                    navController.navigate(Routes.workoutHistoryDetail(workoutId))
                },
                onResumeWorkout = { workoutId ->
                    navController.navigate(Routes.workoutSession(workoutId))
                },
                onNewWorkout = { navController.navigate(Routes.workoutSession()) }
            )
        }

        composable(
            route = Routes.WORKOUT_HISTORY_DETAIL,
            arguments = listOf(
                navArgument("workoutId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getLong("workoutId") ?: return@composable
            WorkoutHistoryDetailScreen(
                workoutId = workoutId,
                onBackClick = { navController.popBackStack() },
                onEditClick = { id ->
                    navController.navigate(Routes.workoutSession(id))
                }
            )
        }

        composable(
            route = Routes.WORKOUT_SESSION,
            arguments = listOf(
                navArgument("workoutId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val arg = backStackEntry.arguments?.getLong("workoutId") ?: -1L
            val workoutId = if (arg == -1L) null else arg
            WorkoutSessionScreen(
                onBackClick = { navController.popBackStack() },
                workoutId = workoutId
            )
        }

        composable(Routes.PROGRESS) {
            ProgressDashboardScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.CAMERA) {
            CameraPreviewScreen()
        }
    }
}
