package com.gymcoach.app.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun LoadingState(modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
