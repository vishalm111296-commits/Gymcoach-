package com.gymcoach.app.presentation.workout

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class WorkoutSessionScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testCameraOverlayDisplaysApplyRepsButtonAndTriggersCallback() {
        var appliedReps = 0
        composeTestRule.setContent {
            com.gymcoach.app.presentation.camera.CameraOverlay(
                repCount = 12,
                formFeedback = "Good form",
                onApplyReps = { count -> appliedReps = count }
            )
        }

        composeTestRule.onNodeWithText("Finish & Apply Reps (12)").assertIsDisplayed().performClick()
        assert(appliedReps == 12)
    }

    @Test
    fun testExerciseInstructionsExpandableAndCameraAction() {
        var cameraClicked = false
        composeTestRule.setContent {
            ExerciseSetCard(
                exerciseName = "Barbell Squat",
                muscleGroup = "Quads",
                sets = emptyList(),
                previousSets = null,
                lastPerformance = null,
                instructions = "Keep chest up and knees over toes.",
                recommendation = null,
                onAddSet = {},
                onRemoveSet = {},
                onRemoveExercise = {},
                onRepsChange = { _, _ -> },
                onWeightChange = { _, _ -> },
                onRpeChange = { _, _ -> },
                onRestSecondsChange = { _, _ -> },
                onSetTypeChange = { _, _ -> },
                onToggleComplete = {},
                onOpenPlateCalculator = {},
                onCameraClick = { cameraClicked = true }
            )
        }

        composeTestRule.onNodeWithText("View Technique Guide").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("Keep chest up and knees over toes.").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Camera Form Coach").assertIsDisplayed().performClick()
        assert(cameraClicked)
    }

    @Test
    fun testWorkoutCompletionViewDisplaysMetricsAndPRs() {
        val pr = com.gymcoach.app.core.progression.PRDetector.PersonalRecord(
            exerciseId = 1L,
            exerciseName = "Bench Press",
            type = com.gymcoach.app.core.progression.PRDetector.PRType.WEIGHT,
            value = 100.0,
            details = "100.0kg lifted",
            date = java.time.Instant.now(),
            workoutId = 1L
        )

        val totalVolumeKg = 5000.0
        val summary = WorkoutLoggingViewModel.WorkoutSummary(
            workoutId = 1L,
            workoutName = "Leg Day",
            durationSeconds = 3600,
            totalVolumeKg = totalVolumeKg,
            completedSetsCount = 12,
            totalSetsCount = 12,
            exercisesCompletedCount = 4,
            newPRs = listOf(pr)
        )

        var doneClicked = false
        var viewHistoryClicked = false

        composeTestRule.setContent {
            WorkoutCompletionView(
                summary = summary,
                onDone = { doneClicked = true },
                onViewHistoryDetail = { _ -> viewHistoryClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Workout Crushed! 🔥").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total Volume").assertIsDisplayed()

        val expectedVolumeText = "${String.format(Locale.US, "%.0f", totalVolumeKg)}\nkg·reps"
        composeTestRule.onNodeWithText(expectedVolumeText, useUnmergedTree = true).assertIsDisplayed()

        composeTestRule.onNodeWithText("Duration").assertIsDisplayed()
        // 3600 seconds = 01:00:00 (formatDuration converts this depending on logic, check view)
        composeTestRule.onNodeWithText("Sets Completed").assertIsDisplayed()
        composeTestRule.onNodeWithText("12 / 12").assertIsDisplayed()

        composeTestRule.onNodeWithText("Personal Records Broken").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bench Press").assertIsDisplayed()
        composeTestRule.onNodeWithText("100.0kg lifted").assertIsDisplayed()

        composeTestRule.onNodeWithText("Done").performClick()
        assert(doneClicked)

        composeTestRule.onNodeWithText("View Detailed Breakdown").performClick()
        assert(viewHistoryClicked)
    }
}
