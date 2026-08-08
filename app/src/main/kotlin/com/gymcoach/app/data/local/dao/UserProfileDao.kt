package com.gymcoach.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gymcoach.app.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(userProfile: UserProfileEntity)

    @Update
    suspend fun updateUserProfile(userProfile: UserProfileEntity)

    @Query("SELECT * FROM user_profile WHERE id = :userId")
    fun getUserProfile(userId: Long): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = :userId AND age >= :minAge AND age <= :maxAge")
    fun getFilteredUserProfiles(userId: Long, minAge: Int, maxAge: Int): Flow<List<UserProfileEntity>>

    @Query("DELETE FROM user_profile WHERE id = :userId")
    suspend fun deleteUserProfile(userId: Long)
}
