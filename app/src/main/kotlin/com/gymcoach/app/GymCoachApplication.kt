package com.gymcoach.app

import android.app.Application
import com.gymcoach.app.core.exercise.ExerciseSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class GymCoachApplication : Application() {

    @Inject lateinit var exerciseSeeder: ExerciseSeeder

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            exerciseSeeder.seedIfNeeded()
        }
    }
}
