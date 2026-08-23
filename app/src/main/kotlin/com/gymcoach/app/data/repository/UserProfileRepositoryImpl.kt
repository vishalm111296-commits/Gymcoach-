package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.UserProfileDao
import com.gymcoach.app.data.local.entity.UserProfileEntity
import com.gymcoach.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserProfileRepositoryImpl @Inject constructor(
    private val userProfileDao: UserProfileDao
) : UserProfileRepository {
    override fun getLatestProfile(): Flow<UserProfileEntity?> = userProfileDao.getLatestProfile()

    override suspend fun saveProfile(profile: UserProfileEntity): Long = userProfileDao.insert(profile)
}
