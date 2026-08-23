package com.gymcoach.app.domain.repository

import com.gymcoach.app.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun getLatestProfile(): Flow<UserProfileEntity?>
    suspend fun saveProfile(profile: UserProfileEntity): Long
}
