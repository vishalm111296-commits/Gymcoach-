package com.gymcoach.app.presentation.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymcoach.app.ui.theme.GymCoachBorders
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes

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

        AnimatedVisibility(
            visible = !formFeedback.isNullOrBlank(),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (onApplyReps != null && repCount > 0) 96.dp else 80.dp)
        ) {
            formFeedback?.let { feedback ->
                FormFeedbackView(feedback = feedback)
            }
        }

        if (onApplyReps != null && repCount > 0) {
            Button(
                onClick = { onApplyReps(repCount) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
                    .semantics { contentDescription = "Finish and apply $repCount completed reps" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GymCoachColors.Primary,
                    contentColor = GymCoachColors.TextPrimary
                ),
                shape = GymCoachShapes.pill
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
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
    Surface(
        modifier = modifier
            .clip(GymCoachShapes.Card)
            .border(GymCoachBorders.subtleBorder(), GymCoachShapes.Card)
            .semantics { contentDescription = "Completed reps count: $count" },
        color = GymCoachColors.PureDark.copy(alpha = 0.85f),
        shape = GymCoachShapes.Card
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "REPS",
                color = GymCoachColors.TextMuted,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = "$count",
                color = GymCoachColors.Primary,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun FormFeedbackView(feedback: String, modifier: Modifier = Modifier) {
    val statusColor = FeedbackColors.forText(feedback)
    val statusIcon = FeedbackColors.iconForText(feedback)

    Surface(
        modifier = modifier
            .padding(horizontal = 24.dp)
            .clip(GymCoachShapes.Card)
            .border(1.dp, statusColor.copy(alpha = 0.5f), GymCoachShapes.Card)
            .semantics { contentDescription = "Form feedback: $feedback" },
        color = GymCoachColors.PureDark.copy(alpha = 0.90f),
        shape = GymCoachShapes.Card
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = feedback,
                color = statusColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private object FeedbackColors {
    private val goodExact = setOf(
        "good form", "perfect", "great", "excellent", "keep it up",
        "good rep", "good depth", "good plank position", "good hinge position",
        "good height, lower with control", "plank hold complete", "full range of motion"
    )

    private val warnExact = setOf(
        "straighten back", "slow down", "go deeper", "control descent",
        "don't lock out", "full range", "lower the weight",
        "extend arm more", "go lower", "lower your chest",
        "keep body straight", "press weight up", "almost there",
        "raise arms to shoulder height", "keep raising", "pull to your torso",
        "extend arms forward", "squeeze core, keep straight",
        "lower hips or raise up", "push hips back",
        "lower the bar", "press up"
    )

    private fun isGood(normalized: String): Boolean {
        return normalized in goodExact ||
            normalized.startsWith("good") ||
            normalized.startsWith("hold for") ||
            normalized.contains("complete") ||
            normalized.contains("perfect")
    }

    private fun isInfo(normalized: String): Boolean {
        return normalized.startsWith("start") ||
            normalized.startsWith("ready") ||
            normalized.startsWith("prepare") ||
            normalized.contains("setup")
    }

    private fun isWarn(normalized: String): Boolean {
        return normalized in warnExact ||
            normalized.startsWith("keep") ||
            normalized.startsWith("raise") ||
            normalized.startsWith("press") ||
            normalized.startsWith("lower") ||
            normalized.startsWith("extend") ||
            normalized.startsWith("pull") ||
            normalized.startsWith("push") ||
            normalized.startsWith("go") ||
            normalized.startsWith("almost")
    }

    fun forText(feedback: String): Color {
        val normalized = feedback.trim().lowercase()
        return when {
            isGood(normalized) -> GymCoachColors.Success
            isInfo(normalized) -> GymCoachColors.Primary
            isWarn(normalized) -> GymCoachColors.Warning
            else -> GymCoachColors.Danger
        }
    }

    fun iconForText(feedback: String): ImageVector {
        val normalized = feedback.trim().lowercase()
        return when {
            isGood(normalized) -> Icons.Filled.CheckCircle
            isInfo(normalized) -> Icons.Filled.Info
            isWarn(normalized) -> Icons.Filled.Warning
            else -> Icons.Filled.Error
        }
    }
}