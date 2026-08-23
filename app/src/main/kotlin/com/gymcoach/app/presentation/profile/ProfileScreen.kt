package com.gymcoach.app.presentation.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.gymcoach.app.ui.theme.DarkBackground
import com.gymcoach.app.ui.theme.WarmWhite

@Composable
fun ProfileScreen() {
    Scaffold(
        screenColor = DarkBackground,
        bottomBar = {
            com.gymcoach.app.ui.GymCoachBottomNav(
                currentRoute = "profile",
                onNavigate = { route -> }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DarkBackground)
        ) {
            Text(
                text = "PROFILE SCREEN",
                style = MaterialTheme.typography.headlineMedium,
                color = WarmWhite,
                modifier = Modifier.align(Alignment.Center)
            )
            Text(
                text = "User profile and measurements coming soon",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}