package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.UserProfileDao
import com.gymcoach.app.data.local.entity.UserProfileEntity
import com.gymcoach.app.domain.model.UserProfile
import com.gymcoach.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val userProfileDao: UserProfileDao
) : ProfileRepository {

    override fun getUserProfile(): Flow<UserProfile?> {
        return userProfileDao.getUserProfile(1L).map { it?.toDomain() }
    }

    override suspend fun saveUserProfile(profile: UserProfile) {
        userProfileDao.insertOrUpdateProfile(profile.toEntity())
    }

    override suspend fun updateProfile(profile: UserProfile) {
        userProfileDao.updateUserProfile(profile.toEntity())
    }

    override suspend fun deleteProfile() {
        userProfileDao.deleteUserProfile(1L)
    }

    override suspend fun syncProfile() {
        // Offline-first: no network sync needed
    }

    private fun UserProfileEntity.toDomain() = UserProfile(
        id = id,
        name = name,
        age = age,
        gender = gender,
        height = height,
        weight = weight,
        goalWeight = goalWeight,
        currentGoal = currentGoal,
        experience = experience,
        trainingStyle = trainingStyle,
        preferredSplit = preferredSplit,
        activityLevel = activityLevel,
        weeklyWorkoutGoal = weeklyWorkoutGoal,
        proteinGoal = proteinGoal,
        caloriesGoal = caloriesGoal,
        units = units,
        avatarUrl = avatarUrl,
        leanBodyMass = leanBodyMass,
        maintenanceCalories = maintenanceCalories,
        equipment = equipment
    )

    private fun UserProfile.toEntity() = UserProfileEntity(
        id = id,
        name = name,
        age = age,
        gender = gender,
        height = height,
        weight = weight,
        goalWeight = goalWeight,
        currentGoal = currentGoal,
        experience = experience,
        trainingStyle = trainingStyle,
        preferredSplit = preferredSplit,
        activityLevel = activityLevel,
        weeklyWorkoutGoal = weeklyWorkoutGoal,
        proteinGoal = proteinGoal,
        caloriesGoal = caloriesGoal,
        units = units,
        avatarUrl = avatarUrl,
        leanBodyMass = leanBodyMass,
        maintenanceCalories = maintenanceCalories,
        equipment = equipment
    )
}
