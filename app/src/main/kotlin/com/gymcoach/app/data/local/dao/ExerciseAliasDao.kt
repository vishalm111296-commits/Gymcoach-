package com.gymcoach.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymcoach.app.data.local.entity.ExerciseAliasEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseAliasDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(alias: ExerciseAliasEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(aliases: List<ExerciseAliasEntity>)

    @Query("SELECT * FROM exercise_aliases WHERE exercise_id = :exerciseId")
    fun getByExerciseId(exerciseId: Long): Flow<List<ExerciseAliasEntity>>

    @Query("SELECT DISTINCT e.* FROM exercises e INNER JOIN exercise_aliases a ON e.id = a.exercise_id WHERE a.alias LIKE '%' || :query || '%'")
    fun searchByAlias(query: String): Flow<List<com.gymcoach.app.data.local.entity.ExerciseEntity>>

    @Query("DELETE FROM exercise_aliases WHERE exercise_id = :exerciseId")
    suspend fun deleteByExerciseId(exerciseId: Long): Int
}