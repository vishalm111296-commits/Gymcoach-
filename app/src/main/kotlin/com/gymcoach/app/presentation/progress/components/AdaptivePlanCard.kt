package com.gymcoach.app.presentation.progress.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymcoach.app.core.program.AdaptiveProgramEngine.AdaptiveActionType
import com.gymcoach.app.core.program.AdaptiveProgramEngine.AdaptiveProgramAction
import com.gymcoach.app.ui.theme.AccentBlue
import com.gymcoach.app.ui.theme.DarkSurface
import com.gymcoach.app.ui.theme.TextPrimary
import com.gymcoach.app.ui.theme.TextSecondary

/**
 * Displays the list of adaptive program actions produced by the
 * [com.gymcoach.app.core.program.AdaptiveProgramEngine].
 *
 * All strings are ASCII. Product-heuristic disclaimer: these are training
 * suggestions, not medical or clinical advice.
 */
@Composable
fun AdaptivePlanCard(
    actions: List<AdaptiveProgramAction>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "ADAPTIVE PLAN",
                style = MaterialTheme.typography.titleMedium,
                color = AccentBlue
            )

            Text(
                text = "Training suggestions based on your volume balance and assessment.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            actions.forEach { action ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "[${action.type.label}] ${action.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = action.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

/** Short ASCII label for each adaptive action type (deterministic, stable). */
private val AdaptiveActionType.label: String
    get() = when (this) {
        AdaptiveActionType.VOLUME_SHIFT -> "SHIFT"
        AdaptiveActionType.DELOAD -> "DELOAD"
        AdaptiveActionType.LOAD_BUMP -> "LOAD"
        AdaptiveActionType.VARIATION -> "VARY"
        AdaptiveActionType.BALANCED -> "MAINTAIN"
    }
