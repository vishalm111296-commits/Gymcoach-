package com.gymcoach.app.presentation.workout.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** One recorded set from a previous session. */
data class SetData(val weightKg: Double, val reps: Int)

/** Last session performance for an exercise. */
data class LastSessionData(val date: String, val sets: List<SetData>)

@Composable
fun PreviousPerformanceRow(
    exerciseName: String,
    lastSession: LastSessionData?,
    modifier: Modifier = Modifier
) {
    val text = if (lastSession == null || lastSession.sets.isEmpty()) {
        "No previous data for this exercise"
    } else {
        val summary = lastSession.sets.joinToString(", ") { "${formatWeight(it.weightKg)}kg × ${it.reps}" }
        "Previous: $summary"
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth()
    )
}

private fun formatWeight(weightKg: Double): String =
    if (weightKg == weightKg.toLong().toDouble()) weightKg.toLong().toString() else weightKg.toString()
