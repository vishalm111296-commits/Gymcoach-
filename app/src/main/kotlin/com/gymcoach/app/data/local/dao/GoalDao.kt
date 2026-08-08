package com.gymcoach.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gymcoach.app.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Insert
    suspend fun insertGoal(goal: GoalEntity)

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY createdAt DESC")
    fun getGoalsForUser(userId: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE userId = :userId AND status = :status ORDER BY targetDate ASC")
    fun getGoalsByStatus(userId: String, status: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE userId = :userId AND goalType = :type ORDER BY targetDate ASC")
    fun getGoalsByType(userId: String, type: String): Flow<List<GoalEntity>>

    @Query("SELECT COUNT(*) FROM goals WHERE userId = :userId AND status != 'COMPLETED'")
    fun getActiveGoalsCount(userId: String): Flow<Int>

    @Query("SELECT * FROM goals WHERE id = :goalId")
    fun getGoalById(goalId: String): Flow<GoalEntity?>

    @Query("DELETE FROM goals WHERE userId = :userId AND status = 'COMPLETED'")
    suspend fun deleteCompletedGoals(userId: String)
}