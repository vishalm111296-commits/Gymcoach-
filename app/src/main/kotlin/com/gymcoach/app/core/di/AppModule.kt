package com.gymcoach.app.core.di

import android.content.Context
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.dao.MeasurementDao
import com.gymcoach.app.data.local.dao.UserProfileDao
import com.gymcoach.app.data.local.dao.GoalDao
import com.gymcoach.app.data.local.database.GymCoachDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): GymCoachDatabase =
        GymCoachDatabase.getDatabase(ctx)

    @Provides
    @Singleton
    fun provideExerciseDao(database: GymCoachDatabase): ExerciseDao = database.exerciseDao()

    @Provides
    @Singleton
    fun provideWorkoutDao(database: GymCoachDatabase): WorkoutDao = database.workoutDao()

    @Provides
    @Singleton
    fun provideMeasurementDao(database: GymCoachDatabase): MeasurementDao = database.measurementDao()

    @Provides
    @Singleton
    fun provideUserProfileDao(database: GymCoachDatabase): UserProfileDao = database.userProfileDao()

    @Provides
    @Singleton
    fun provideGoalDao(database: GymCoachDatabase): GoalDao = database.goalDao()
}
