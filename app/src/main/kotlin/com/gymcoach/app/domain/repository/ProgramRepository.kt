package com.gymcoach.app.domain.repository

import com.gymcoach.app.core.program.ProgramGenerator
import com.gymcoach.app.data.local.entity.ProgramDayEntity
import com.gymcoach.app.data.local.entity.ProgramExerciseEntity
import com.gymcoach.app.data.local.entity.ProgramEntity
import kotlinx.coroutines.flow.Flow

// ponytail: returns Room entities directly; introduce domain models if presentation starts leaking persistence concerns.

data class CustomRoutineExercise(
    val exerciseId: Long,
    val targetSets: Int = 3,
    val targetReps: String = "8-12",
    val restSeconds: Int = 90
)

data class CustomRoutineDay(
    val dayNumber: Int,
    val name: String,
    val targetMuscles: String,
    val exercises: List<CustomRoutineExercise>
)

interface ProgramRepository {
    fun getActiveProgram(): Flow<ProgramEntity?>
    fun getDaysForProgram(programId: Long): Flow<List<ProgramDayEntity>>
    fun getExercisesForDay(dayId: Long): Flow<List<ProgramExerciseEntity>>
    fun getExercisesForDays(dayIds: List<Long>): Flow<Map<Long, List<ProgramExerciseEntity>>>
    suspend fun saveGeneratedProgram(program: ProgramGenerator.GeneratedProgram): Long
    suspend fun saveCustomRoutine(name: String, description: String, goal: String, days: List<CustomRoutineDay>, setAsActive: Boolean = true): Long
}
