package com.gymcoach.app.ui

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.gymcoach.app.ui.theme.GymCoachTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            GymCoachTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val prefs = getSharedPreferences("gymcoach_prefs", Context.MODE_PRIVATE)
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