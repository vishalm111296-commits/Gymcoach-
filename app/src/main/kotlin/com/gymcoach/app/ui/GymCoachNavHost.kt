package com.gymcoach.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.gymcoach.app.core.ml.ExerciseType
import com.gymcoach.app.ui.theme.DarkBackground
import com.gymcoach.app.presentation.camera.CameraPreviewScreen
import com.gymcoach.app.presentation.detail.ExerciseDetailScreen
import com.gymcoach.app.presentation.history.WorkoutHistoryDetailScreen
import com.gymcoach.app.presentation.history.WorkoutHistoryScreen
import com.gymcoach.app.presentation.home.HomeDashboardScreen
import com.gymcoach.app.presentation.list.ExerciseListScreen
import com.gymcoach.app.presentation.onboarding.OnboardingScreen
import com.gymcoach.app.presentation.profile.ProfileScreen
import com.gymcoach.app.presentation.progress.ProgressDashboardScreen
import com.gymcoach.app.presentation.readiness.ReadinessScreen
import com.gymcoach.app.presentation.workout.WorkoutSessionScreen

object Routes {
    const val HOME = "home"
    const val ONBOARDING = "onboarding"
    const val EXERCISE_LIST = "exercise_list"
    const val EXERCISE_DETAIL = "exercise_detail/{exerciseId}"
    const val WORKOUT_HISTORY = "workout_history"
    const val WORKOUT_HISTORY_DETAIL = "workout_history_detail/{workoutId}"
    const val WORKOUT_SESSION = "workout_session?workoutId={workoutId}"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"
    const val READINESS = "readiness"
    const val CAMERA = "camera/{exerciseType}"

    fun exerciseDetail(exerciseId: Long) = "exercise_detail/$exerciseId"
    fun workoutHistoryDetail(workoutId: Long) = "workout_history_detail/$workoutId"
    fun workoutSession(workoutId: Long? = null) = if (workoutId != null) "workout_session?workoutId=$workoutId" else "workout_session"
    fun camera(exerciseType: ExerciseType) = "camera/${exerciseType.name}"
}

@Composable
fun GymCoachAppShell(
    navController: NavHostController,
    content: @Composable (Modifier) -> Unit
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route

    val topLevelRoutes = listOf(
        Routes.HOME,
        Routes.EXERCISE_LIST,
        Routes.PROGRESS,
        Routes.PROFILE
    )

    val showBottomNav = currentRoute in topLevelRoutes

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            if (showBottomNav) {
                GymCoachBottomNav(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Routes.HOME) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        content(Modifier.padding(innerPadding).fillMaxSize())
    }
}

@Composable
fun GymCoachNavHost(
    navController: NavHostController,
    startDestination: String = Routes.HOME,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
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
                onStartWorkout = {
                    navController.navigate(Routes.workoutSession())
                },
                onViewProgram = {
                    navController.navigate(Routes.EXERCISE_LIST)
                },
                onNavigateToProgress = {
                    navController.navigate(Routes.PROGRESS)
                },
                onNavigateToProfile = {
                    navController.navigate(Routes.PROFILE)
                },
                onNavigateToReadiness = {
                    navController.navigate(Routes.READINESS)
                }
            )
        }

        composable(Routes.EXERCISE_LIST) {
            ExerciseListScreen(
                onExerciseClick = { exerciseId ->
                    navController.navigate(Routes.exerciseDetail(exerciseId))
                },
                onHistoryClick = { navController.navigate(Routes.WORKOUT_HISTORY) },
                onProgressClick = { navController.navigate(Routes.PROGRESS) },
                onCameraClick = { exerciseType ->
                    navController.navigate(Routes.camera(exerciseType))
                }
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

        composable(Routes.READINESS) {
            ReadinessScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.CAMERA,
            arguments = listOf(
                navArgument("exerciseType") {
                    type = NavType.StringType
                    defaultValue = ExerciseType.BICEP_CURL.name
                }
            )
        ) { backStackEntry ->
            val rawType = backStackEntry.arguments?.getString("exerciseType")
                ?: ExerciseType.BICEP_CURL.name
            val exerciseType = ExerciseType.entries.firstOrNull { it.name == rawType }
                ?: ExerciseType.BICEP_CURL
            CameraPreviewScreen(exerciseType = exerciseType)
        }
    }
}
