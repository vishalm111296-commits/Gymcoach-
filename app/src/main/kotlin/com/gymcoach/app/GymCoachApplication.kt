package com.gymcoach.app

import android.app.Application
import com.gymcoach.app.core.di.ApplicationScope
import com.gymcoach.app.core.exercise.ExerciseSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class GymCoachApplication : Application() {

    @Inject
    lateinit var exerciseSeeder: ExerciseSeeder

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // Idempotent: no-ops when the library is already seeded.
        applicationScope.launch {
            exerciseSeeder.seedIfNeeded()
        }
    }
}
