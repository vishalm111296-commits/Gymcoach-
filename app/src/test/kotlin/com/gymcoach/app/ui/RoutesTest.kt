package com.gymcoach.app.ui

import com.gymcoach.app.core.ml.ExerciseType
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {

    @Test
    fun testRouteConstants() {
        assertEquals("home", Routes.HOME)
        assertEquals("onboarding", Routes.ONBOARDING)
        assertEquals("exercise_list", Routes.EXERCISE_LIST)
        assertEquals("exercise_detail/{exerciseId}", Routes.EXERCISE_DETAIL)
        assertEquals("workout_history", Routes.WORKOUT_HISTORY)
        assertEquals("workout_history_detail/{workoutId}", Routes.WORKOUT_HISTORY_DETAIL)
        assertEquals("workout_session?workoutId={workoutId}", Routes.WORKOUT_SESSION)
        assertEquals("progress", Routes.PROGRESS)
        assertEquals("profile", Routes.PROFILE)
        assertEquals("readiness", Routes.READINESS)
        assertEquals("program_detail", Routes.PROGRAM_DETAIL)
        assertEquals("camera/{exerciseType}", Routes.CAMERA)
    }

    @Test
    fun testExerciseDetailRouteGenerator() {
        assertEquals("exercise_detail/42", Routes.exerciseDetail(42L))
        assertEquals("exercise_detail/1", Routes.exerciseDetail(1L))
    }

    @Test
    fun testWorkoutHistoryDetailRouteGenerator() {
        assertEquals("workout_history_detail/99", Routes.workoutHistoryDetail(99L))
    }

    @Test
    fun testWorkoutSessionRouteGenerator() {
        assertEquals("workout_session", Routes.workoutSession(null))
        assertEquals("workout_session?workoutId=123", Routes.workoutSession(123L))
    }

    @Test
    fun testCameraRouteGenerator() {
        assertEquals("camera/BICEP_CURL", Routes.camera(ExerciseType.BICEP_CURL))
        assertEquals("camera/SQUAT", Routes.camera(ExerciseType.SQUAT))
        assertEquals("camera/LATERAL_RAISE", Routes.camera(ExerciseType.LATERAL_RAISE))
        assertEquals("camera/PUSH_UP", Routes.camera(ExerciseType.PUSH_UP))
    }
}
