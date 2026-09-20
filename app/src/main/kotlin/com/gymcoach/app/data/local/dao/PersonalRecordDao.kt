package com.gymcoach.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gymcoach.app.data.local.entity.PersonalRecordEntity
import com.gymcoach.app.data.local.entity.PersonalRecordWithExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: PersonalRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<PersonalRecordEntity>)

    @Update
    suspend fun update(record: PersonalRecordEntity): Int

    @Query("SELECT * FROM personal_records WHERE exercise_id = :exerciseId ORDER BY one_rep_max_kg DESC")
    fun getByExerciseId(exerciseId: Long): Flow<List<PersonalRecordEntity>>

    @Query("SELECT * FROM personal_records ORDER BY achieved_at DESC")
    fun getAll(): Flow<List<PersonalRecordEntity>>

    @Query("""
        SELECT pr.id, pr.exercise_id, pr.user_id, pr.weight_kg, pr.reps, pr.one_rep_max_kg,
               pr.achieved_at, pr.notes, e.name AS exercise_name
        FROM personal_records pr
        LEFT JOIN exercises e ON pr.exercise_id = e.id
        ORDER BY pr.achieved_at DESC
    """)
    fun getAllWithExerciseName(): Flow<List<PersonalRecordWithExercise>>

    @Query("SELECT * FROM personal_records WHERE id = :id")
    suspend fun getById(id: Long): PersonalRecordEntity?

    @Query("DELETE FROM personal_records WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}