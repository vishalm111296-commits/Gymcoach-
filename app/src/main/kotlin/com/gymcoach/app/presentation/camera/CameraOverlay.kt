package com.gymcoach.app.presentation.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymcoach.app.core.ml.FeedbackTone

@Composable
fun CameraOverlay(
    repCount: Int,
    formFeedback: String?,
    feedbackTone: FeedbackTone = FeedbackTone.NEUTRAL,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        RepCountDisplay(
            count = repCount,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
        )

        if (!formFeedback.isNullOrBlank()) {
            FormFeedbackView(
                feedback = formFeedback,
                tone = feedbackTone,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
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
            .semantics {
                // Screen readers announce the contextual status ("Reps: 3"),
                // not a bare number, as the count changes.
                liveRegion = LiveRegionMode.Polite
            }
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
fun FormFeedbackView(feedback: String, tone: FeedbackTone, modifier: Modifier = Modifier) {
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
            color = tone.color,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Semantic cue colors. The cue text carries the message; color is additive. */
private val FeedbackTone.color: Color
    get() = when (this) {
        FeedbackTone.GOOD -> Color(0xFF4CAF50)
        FeedbackTone.WARN -> Color(0xFFFFC107)
        FeedbackTone.NEUTRAL -> Color.White
    }