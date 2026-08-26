package com.gymcoach.app.ui

import android.os.Bundle
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
import javax.inject.Inject
import android.content.SharedPreferences

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GymCoachTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val onboardingComplete = sharedPreferences.getBoolean("onboarding_complete", false)
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