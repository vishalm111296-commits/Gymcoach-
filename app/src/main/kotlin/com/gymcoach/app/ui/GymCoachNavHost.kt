package com.gymcoach.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gymcoach.app.core.ml.ExerciseType
import com.gymcoach.app.presentation.camera.CameraPreviewScreen
import com.gymcoach.app.presentation.analytics.MuscleBalanceScreen
import com.gymcoach.app.presentation.detail.ExerciseDetailScreen
import com.gymcoach.app.presentation.history.WorkoutHistoryDetailScreen
import com.gymcoach.app.presentation.history.WorkoutHistoryScreen
import com.gymcoach.app.presentation.home.HomeDashboardScreen
import com.gymcoach.app.presentation.list.ExerciseListScreen
import com.gymcoach.app.presentation.onboarding.OnboardingScreen
import com.gymcoach.app.presentation.profile.ProfileScreen
import com.gymcoach.app.presentation.program.ProgramDetailScreen
import com.gymcoach.app.presentation.progress.ProgressDashboardScreen
import com.gymcoach.app.presentation.progress.ProgressionAnalyticsScreen
import com.gymcoach.app.presentation.readiness.ReadinessScreen
import com.gymcoach.app.presentation.body.BodyCompositionScreen
import com.gymcoach.app.presentation.template.WorkoutTemplateScreen
import com.gymcoach.app.presentation.workout.WorkoutSessionScreen
import com.gymcoach.app.presentation.gamification.StreakAndAchievementScreen

object Routes {
    const val HOME = "home"
    const val ONBOARDING = "onboarding"
    const val EXERCISE_LIST = "exercise_list"
    const val EXERCISE_DETAIL = "exercise_detail/{exerciseId}"
    const val WORKOUT_HISTORY = "workout_history"
    const val WORKOUT_HISTORY_DETAIL = "workout_history_detail/{workoutId}"
    const val WORKOUT_SESSION = "workout_session?workoutId={workoutId}"
    const val WORKOUT_LEGACY = "workout"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"
    const val READINESS = "readiness"
    const val PROGRAM_DETAIL = "program_detail"
    const val CAMERA = "camera/{exerciseType}"
    const val TEMPLATES = "templates"
    const val PROGRESSION_ANALYTICS = "progression_analytics/{exerciseId}?exerciseName={exerciseName}"
    const val MUSCLE_BALANCE = "muscle_balance"
    const val STREAKS_AND_ACHIEVEMENTS = "streaks_and_achievements"
    const val V_TAPER_TRANSFORMATION = "v_taper_transformation"
    const val BODY_COMPOSITION = "body_composition"

    fun exerciseDetail(exerciseId: Long) = "exercise_detail/$exerciseId"
    fun workoutHistoryDetail(workoutId: Long) = "workout_history_detail/$workoutId"
    fun workoutSession(workoutId: Long? = null) = if (workoutId != null) "workout_session?workoutId=$workoutId" else "workout_session"
    fun camera(exerciseType: ExerciseType) = "camera/${exerciseType.name}"
    fun progressionAnalytics(exerciseId: Long, exerciseName: String) =
        "progression_analytics/$exerciseId?exerciseName=${java.net.URLEncoder.encode(exerciseName, "UTF-8")}"
}

private const val NAV_TRANSITION_DURATION_MS = 300

@Composable
fun GymCoachNavHost(
    navController: NavHostController,
    startDestination: String = Routes.HOME
) {

    val onBottomNavigate: (String) -> Unit = { route ->
        when (route) {
            "home" -> navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = false } }
            "workout" -> navController.navigate(Routes.workoutSession())
            "exercise_list" -> navController.navigate(Routes.EXERCISE_LIST)
            "program_detail" -> navController.navigate(Routes.PROGRAM_DETAIL)
            "progress" -> navController.navigate(Routes.PROGRESS)
            "profile" -> navController.navigate(Routes.PROFILE)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(durationMillis = NAV_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = NAV_TRANSITION_DURATION_MS))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(durationMillis = NAV_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = NAV_TRANSITION_DURATION_MS))
        },
        popEnterTransition = {
            if (initialState.destination.route == Routes.CAMERA) {
                fadeIn(animationSpec = tween(durationMillis = NAV_TRANSITION_DURATION_MS))
            } else {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(durationMillis = NAV_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = NAV_TRANSITION_DURATION_MS))
            }
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(durationMillis = NAV_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = NAV_TRANSITION_DURATION_MS))
        }
    ) {
        composable(
            route = Routes.ONBOARDING,
            enterTransition = {
                fadeIn(animationSpec = tween(durationMillis = NAV_TRANSITION_DURATION_MS))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(durationMillis = NAV_TRANSITION_DURATION_MS))
            }
        ) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            // F-NAV-1: pass onNavigateToExercises so the new Exercises tab works from Home
            HomeDashboardScreen(
                onStartWorkout = { workoutId ->
                    navController.navigate(Routes.workoutSession(workoutId))
                },
                onViewProgram = {
                    navController.navigate(Routes.PROGRAM_DETAIL)
                },
                onNavigateToProgress = {
                    navController.navigate(Routes.PROGRESS)
                },
                onNavigateToProfile = {
                    navController.navigate(Routes.PROFILE)
                },
                onNavigateToReadiness = {
                    navController.navigate(Routes.READINESS)
                },
                onNavigateToExercises = {
                    navController.navigate(Routes.EXERCISE_LIST)
                },
                onNavigateToTemplates = {
                    navController.navigate(Routes.TEMPLATES)
                },
                onNavigateToStreaks = {
                    navController.navigate(Routes.STREAKS_AND_ACHIEVEMENTS)
                },
                onNavigateToVTaper = {
                    navController.navigate(Routes.V_TAPER_TRANSFORMATION)
                }
            )
        }

        composable(Routes.EXERCISE_LIST) {
            ExerciseListScreen(
                onBackClick = { navController.popBackStack() },
                onExerciseClick = { exerciseId ->
                    navController.navigate(Routes.exerciseDetail(exerciseId))
                },
                onHistoryClick = { navController.navigate(Routes.WORKOUT_HISTORY) },
                onProgressClick = { navController.navigate(Routes.PROGRESS) },
                onCameraClick = { exerciseType ->
                    navController.navigate(Routes.camera(exerciseType))
                },
                onNavigateBottomBar = onBottomNavigate
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
                onBackClick = { navController.popBackStack() },
                onViewProgressClick = { exId, exName ->
                    navController.navigate(Routes.progressionAnalytics(exId, exName))
                },
                onCameraClick = { exerciseType -> navController.navigate(Routes.camera(exerciseType)) }
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
                onPerformAgainClick = { newWorkoutId ->
                    navController.navigate(Routes.workoutSession(newWorkoutId))
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
            val appliedReps by backStackEntry.savedStateHandle.getStateFlow<Int?>("applied_reps", null).collectAsState()
            WorkoutSessionScreen(
                onBackClick = { navController.popBackStack() },
                workoutId = workoutId,
                onViewHistoryDetail = { wId -> navController.navigate(Routes.workoutHistoryDetail(wId)) },
                onCameraClick = { exerciseType -> navController.navigate(Routes.camera(exerciseType)) },
                appliedReps = appliedReps,
                onClearAppliedReps = { backStackEntry.savedStateHandle.remove<Int>("applied_reps") }
            )
        }

        // Intentional backward-compatibility and bottom-nav alias for "workout" route
        composable(Routes.WORKOUT_LEGACY) { backStackEntry ->
            val appliedReps by backStackEntry.savedStateHandle.getStateFlow<Int?>("applied_reps", null).collectAsState()
            WorkoutSessionScreen(
                onBackClick = { navController.popBackStack() },
                workoutId = null,
                onViewHistoryDetail = { wId -> navController.navigate(Routes.workoutHistoryDetail(wId)) },
                onCameraClick = { exerciseType -> navController.navigate(Routes.camera(exerciseType)) },
                appliedReps = appliedReps,
                onClearAppliedReps = { backStackEntry.savedStateHandle.remove<Int>("applied_reps") }
            )
        }

        composable(Routes.PROGRESS) {
            ProgressDashboardScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToProgressionAnalytics = { exerciseId, exerciseName ->
                    navController.navigate(Routes.progressionAnalytics(exerciseId, exerciseName))
                },
                onNavigateToMuscleBalance = { navController.navigate(Routes.MUSCLE_BALANCE) },
                onNavigateToStreaks = { navController.navigate(Routes.STREAKS_AND_ACHIEVEMENTS) },
                onNavigateToVTaper = { navController.navigate(Routes.V_TAPER_TRANSFORMATION) },
                onNavigateToBodyComposition = { navController.navigate(Routes.BODY_COMPOSITION) },
                onNavigateBottomBar = onBottomNavigate
            )
        }

        composable(Routes.BODY_COMPOSITION) {
            BodyCompositionScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateBottomBar = onBottomNavigate
            )
        }

        composable(Routes.READINESS) {
            ReadinessScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.PROGRAM_DETAIL) {
            ProgramDetailScreen(
                onBackClick = { navController.popBackStack() },
                onStartWorkout = { workoutId -> navController.navigate(Routes.workoutSession(workoutId)) },
                onNavigateBottomBar = onBottomNavigate
            )
        }

        composable(
            route = Routes.CAMERA,
            arguments = listOf(
                navArgument("exerciseType") {
                    type = NavType.StringType
                    defaultValue = ExerciseType.BICEP_CURL.name
                }
            ),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(durationMillis = NAV_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = NAV_TRANSITION_DURATION_MS))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(durationMillis = NAV_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = NAV_TRANSITION_DURATION_MS))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(durationMillis = NAV_TRANSITION_DURATION_MS))
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(durationMillis = NAV_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = NAV_TRANSITION_DURATION_MS))
            }
        ) { backStackEntry ->
            val rawType = backStackEntry.arguments?.getString("exerciseType")
                ?: ExerciseType.BICEP_CURL.name
            val exerciseType = ExerciseType.entries.firstOrNull { it.name == rawType }
                ?: ExerciseType.BICEP_CURL
            CameraPreviewScreen(
                exerciseType = exerciseType,
                onBackClick = { navController.popBackStack() },
                onApplyReps = { count ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("applied_reps", count)
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.TEMPLATES) {
            WorkoutTemplateScreen(
                onBackClick = { navController.popBackStack() },
                onStartWorkout = { workoutId ->
                    navController.navigate(Routes.workoutSession(workoutId)) {
                        popUpTo(Routes.TEMPLATES) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Routes.PROGRESSION_ANALYTICS,
            arguments = listOf(
                navArgument("exerciseId") { type = NavType.LongType },
                navArgument("exerciseName") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getLong("exerciseId") ?: return@composable
            val exerciseName = backStackEntry.arguments?.getString("exerciseName")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""
            ProgressionAnalyticsScreen(
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.MUSCLE_BALANCE) {
            MuscleBalanceScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.STREAKS_AND_ACHIEVEMENTS) {
            StreakAndAchievementScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.V_TAPER_TRANSFORMATION) {
            com.gymcoach.app.presentation.analytics.VTaperTransformationScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
