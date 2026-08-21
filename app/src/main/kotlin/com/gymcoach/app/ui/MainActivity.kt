package com.gymcoach.app.ui

import android.os.Bundle
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
        super.onCreate(savedInstanceState)
        setContent {
            val prefs = getSharedPreferences("GymCoachSettings", MODE_PRIVATE)
            val darkMode = prefs.getBoolean("dark_mode", false)
            GymCoachTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GymCoachNavHost(navController = rememberNavController())
                }
            }
        }
    }
}
