package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.UserProfileDao
import com.gymcoach.app.data.local.entity.UserProfileEntity
import com.gymcoach.app.domain.model.UserProfile
import com.gymcoach.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserProfileRepositoryImpl @Inject constructor(
    private val userProfileDao: UserProfileDao
) : UserProfileRepository {

    override fun getUserProfile(): Flow<UserProfile?> {
        return userProfileDao.getUserProfile(1L).map { it?.toDomain() }
    }

    override suspend fun saveUserProfile(profile: UserProfile) {
        userProfileDao.insertOrUpdateProfile(profile.toEntity())
    }
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