package com.gymcoach.app.core.di

import com.gymcoach.app.data.local.dao.BodyMeasurementDao
import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.ExerciseMuscleDao
import com.gymcoach.app.data.local.dao.ExerciseSubstitutionDao
import com.gymcoach.app.data.local.dao.ProgramDao
import com.gymcoach.app.data.local.dao.ProgramDayDao
import com.gymcoach.app.data.local.dao.ProgramExerciseDao
import com.gymcoach.app.data.local.dao.ReadinessDao
import com.gymcoach.app.data.local.dao.UserProfileDao
import com.gymcoach.app.data.local.dao.WorkoutDao
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
    fun provideDatabase(@ApplicationContext ctx: android.content.Context): GymCoachDatabase =
        GymCoachDatabase.create(ctx)

    @Provides
    @Singleton
    fun provideExerciseDao(database: GymCoachDatabase): ExerciseDao = database.exerciseDao()

    @Provides
    @Singleton
    fun provideWorkoutDao(database: GymCoachDatabase): WorkoutDao = database.workoutDao()

    @Provides
    @Singleton
    fun provideReadinessDao(database: GymCoachDatabase): ReadinessDao = database.readinessDao()

    @Provides
    @Singleton
    fun provideUserProfileDao(database: GymCoachDatabase): UserProfileDao = database.userProfileDao()

    @Provides
    @Singleton
    fun provideProgramDao(database: GymCoachDatabase): ProgramDao = database.programDao()

    @Provides
    @Singleton
    fun provideProgramDayDao(database: GymCoachDatabase): ProgramDayDao = database.programDayDao()

    @Provides
    @Singleton
    fun provideProgramExerciseDao(database: GymCoachDatabase): ProgramExerciseDao =
        database.programExerciseDao()

    @Provides
    @Singleton
    fun provideBodyMeasurementDao(database: GymCoachDatabase): BodyMeasurementDao =
        database.bodyMeasurementDao()

    @Provides
    @Singleton
    fun provideExerciseMuscleDao(database: GymCoachDatabase): ExerciseMuscleDao =
        database.exerciseMuscleDao()

    @Provides
    @Singleton
    fun provideExerciseSubstitutionDao(database: GymCoachDatabase): ExerciseSubstitutionDao =
        database.exerciseSubstitutionDao()

    // ExerciseRepository and WorkoutRepository are bound via @Binds in RepositoryModule.
}
