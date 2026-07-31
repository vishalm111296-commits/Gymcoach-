package com.gymcoach.app.presentation.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CameraOverlay(
    repCount: Int,
    formFeedback: String?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        RepCountDisplay(
            count = repCount,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
        )

        if (!formFeedback.isNullOrBlank()) {
            FormFeedbackView(
                feedback = formFeedback,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            )
        }
    }
}

@Composable
fun RepCountDisplay(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Reps: $count",
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FormFeedbackView(feedback: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = feedback,
            color = FeedbackColors.forText(feedback),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private object FeedbackColors {
    private val good = setOf("good form", "perfect", "great", "excellent", "keep it up")
    private val warn = setOf("straighten back", "slow down", "go deeper", "control descent",
        "don't lock out", "full range", "lower the weight")

    fun forText(feedback: String): Color = when {
        feedback.trim().lowercase() in good -> Color(0xFF4CAF50)
        feedback.trim().lowercase() in warn -> Color(0xFFFFC107)
        else -> Color(0xFFEF5350)
    }
}