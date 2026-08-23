package com.gymcoach.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.gymcoach.app.ui.theme.GymCoachTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GymCoachTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val prefs = remember { getSharedPreferences("gymcoach_prefs", MODE_PRIVATE) }
                    val onboardingComplete = prefs.getBoolean("onboarding_complete", false)
                    val startDest = if (onboardingComplete) Routes.HOME else Routes.ONBOARDING

                    GymCoachNavHost(
                        navController = navController,
                        startDestination = startDest
                    )
                }
            }
        }
    }
}
