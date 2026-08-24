package com.gymcoach.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymcoach.app.data.local.entity.FavoriteExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteExerciseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favorite: FavoriteExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(favorites: List<FavoriteExerciseEntity>)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_exercises WHERE exercise_id = :exerciseId)")
    fun isFavorite(exerciseId: Long): Flow<Boolean>

    @Query("SELECT * FROM favorite_exercises ORDER BY added_at DESC")
    fun getAll(): Flow<List<FavoriteExerciseEntity>>

    @Query("SELECT * FROM favorite_exercises WHERE exercise_id = :exerciseId")
    suspend fun getByExerciseId(exerciseId: Long): FavoriteExerciseEntity?

    @Query("DELETE FROM favorite_exercises WHERE exercise_id = :exerciseId")
    suspend fun deleteByExerciseId(exerciseId: Long): Int

    @Query("DELETE FROM favorite_exercises")
    suspend fun clearAll(): Int
}