package com.gymcoach.app.presentation.workout

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class WorkoutSessionScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

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

        composeTestRule.onNodeWithText("View Instructions").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("Instructions").assertIsDisplayed()
        composeTestRule.onNodeWithText("Keep chest up and knees over toes.").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Camera Form Coach").assertIsDisplayed().performClick()
        assert(cameraClicked)
    }
}
