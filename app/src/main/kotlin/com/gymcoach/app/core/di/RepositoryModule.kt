package com.gymcoach.app.core.di

import com.gymcoach.app.data.repository.ExerciseRepositoryImpl
import com.gymcoach.app.data.repository.WorkoutRepositoryImpl
import com.gymcoach.app.data.repository.AnalyticsRepositoryImpl
import com.gymcoach.app.data.repository.ProgramRepositoryImpl
import com.gymcoach.app.data.repository.ReadinessRepositoryImpl
import com.gymcoach.app.data.repository.UserProfileRepositoryImpl
import com.gymcoach.app.domain.repository.ExerciseRepository
import com.gymcoach.app.domain.repository.WorkoutRepository
import com.gymcoach.app.domain.repository.AnalyticsRepository
import com.gymcoach.app.domain.repository.ProgramRepository
import com.gymcoach.app.domain.repository.ReadinessRepository
import com.gymcoach.app.domain.repository.UserProfileRepository
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

    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(
        impl: UserProfileRepositoryImpl
    ): UserProfileRepository

    @Binds
    @Singleton
    abstract fun bindProgramRepository(
        impl: ProgramRepositoryImpl
    ): ProgramRepository

    @Binds
    @Singleton
    abstract fun bindReadinessRepository(
        impl: ReadinessRepositoryImpl
    ): ReadinessRepository
}
