package com.gymcoach.app.data.local.dao

import androidx.room.*
import com.gymcoach.app.data.local.entity.TemplateExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutTemplateDao {

    @Query("SELECT * FROM workout_templates WHERE is_archived = 0 ORDER BY updated_at DESC")
    fun getActiveTemplates(): Flow<List<WorkoutTemplateEntity>>

    @Query("SELECT * FROM workout_templates WHERE is_archived = 1 ORDER BY updated_at DESC")
    fun getArchivedTemplates(): Flow<List<WorkoutTemplateEntity>>

    @Query("SELECT * FROM workout_templates WHERE id = :id")
    fun getTemplateById(id: Long): Flow<WorkoutTemplateEntity?>

    @Query("SELECT * FROM workout_templates WHERE id = :id")
    suspend fun getTemplateByIdSync(id: Long): WorkoutTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: WorkoutTemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: WorkoutTemplateEntity)

    @Delete
    suspend fun deleteTemplate(template: WorkoutTemplateEntity)

    @Query("DELETE FROM workout_templates WHERE id = :id")
    suspend fun deleteTemplateById(id: Long): Int

    @Query("UPDATE workout_templates SET is_archived = 1, updated_at = :now WHERE id = :id")
    suspend fun archiveTemplate(id: Long, now: Long = System.currentTimeMillis()): Int

    @Query("UPDATE workout_templates SET is_archived = 0, updated_at = :now WHERE id = :id")
    suspend fun unarchiveTemplate(id: Long, now: Long = System.currentTimeMillis()): Int

    // --- Template Exercises ---

    @Query("SELECT * FROM template_exercises WHERE template_id = :templateId ORDER BY order_index ASC")
    fun getTemplateExercises(templateId: Long): Flow<List<TemplateExerciseEntity>>

    @Query("SELECT * FROM template_exercises WHERE template_id = :templateId ORDER BY order_index ASC")
    suspend fun getTemplateExercisesSync(templateId: Long): List<TemplateExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateExercise(exercise: TemplateExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateExercises(exercises: List<TemplateExerciseEntity>): List<Long>

    @Update
    suspend fun updateTemplateExercise(exercise: TemplateExerciseEntity)

    @Delete
    suspend fun deleteTemplateExercise(exercise: TemplateExerciseEntity)

    @Query("DELETE FROM template_exercises WHERE template_id = :templateId")
    suspend fun deleteTemplateExercisesForTemplate(templateId: Long): Int
}
