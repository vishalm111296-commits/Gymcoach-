package com.gymcoach.app.core.di

import com.gymcoach.app.data.repository.ExerciseRepositoryImpl
import com.gymcoach.app.data.repository.WorkoutRepositoryImpl
import com.gymcoach.app.data.repository.AnalyticsRepositoryImpl
import com.gymcoach.app.domain.repository.ExerciseRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import com.gymcoach.app.domain.repository.AnalyticsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindExerciseRepository(
        impl: ExerciseRepositoryImpl
    ): ExerciseRepository

    @Binds
    @Singleton
    abstract fun bindWorkoutRepository(
        impl: WorkoutRepositoryImpl
    ): WorkoutRepository

    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(
        impl: AnalyticsRepositoryImpl
    ): AnalyticsRepository
}