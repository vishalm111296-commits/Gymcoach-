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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check

@Composable
fun CameraOverlay(
    repCount: Int,
    formFeedback: String?,
    onApplyReps: ((Int) -> Unit)? = null,
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
            val bottomPadding = if (onApplyReps != null && repCount > 0) 96.dp else 80.dp
            FormFeedbackView(
                feedback = formFeedback,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomPadding)
            )
        }

        if (onApplyReps != null && repCount > 0) {
            Button(
                onClick = { onApplyReps(repCount) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFCCFF00),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Apply Reps",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Finish & Apply Reps ($repCount)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
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