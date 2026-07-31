package com.gymcoach.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gymcoach.app.presentation.detail.ExerciseDetailScreen
import com.gymcoach.app.presentation.history.WorkoutHistoryDetailScreen
import com.gymcoach.app.presentation.history.WorkoutHistoryScreen
import com.gymcoach.app.presentation.list.ExerciseListScreen
import com.gymcoach.app.presentation.workout.WorkoutSessionScreen

object Routes {
    const val EXERCISE_LIST = "exercise_list"
    const val EXERCISE_DETAIL = "exercise_detail/{exerciseId}"
    const val WORKOUT_HISTORY = "workout_history"
    const val WORKOUT_HISTORY_DETAIL = "workout_history_detail/{workoutId}"
    const val WORKOUT_SESSION = "workout_session/{workoutId?}"

    fun exerciseDetail(exerciseId: Long) = "exercise_detail/$exerciseId"
    fun workoutHistoryDetail(workoutId: Long) = "workout_history_detail/$workoutId"
    fun workoutSession(workoutId: Long? = null) = if (workoutId != null) "workout_session/$workoutId" else "workout_session"
}

@Composable
fun GymCoachNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.EXERCISE_LIST
    ) {
        composable(Routes.EXERCISE_LIST) {
            ExerciseListScreen(
                onExerciseClick = { exerciseId ->
                    navController.navigate(Routes.exerciseDetail(exerciseId))
                },
                onHistoryClick = { navController.navigate(Routes.WORKOUT_HISTORY) }
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
                }
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
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.WORKOUT_SESSION,
            arguments = listOf(
                navArgument("workoutId") { type = NavType.LongType; nullable = true }
            )
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getLong("workoutId")
            WorkoutSessionScreen(
                onBackClick = { navController.popBackStack() },
                workoutId = workoutId
            )
        }
    }
}