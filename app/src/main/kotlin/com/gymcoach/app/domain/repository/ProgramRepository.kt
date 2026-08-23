package com.gymcoach.app.domain.repository

import com.gymcoach.app.core.program.ProgramGenerator
import com.gymcoach.app.data.local.entity.ProgramDayEntity
import com.gymcoach.app.data.local.entity.ProgramEntity
import com.gymcoach.app.data.local.entity.ProgramExerciseEntity
import kotlinx.coroutines.flow.Flow

// ponytail: returns Room entities directly; introduce domain models if presentation starts leaking persistence concerns.
interface ProgramRepository {
    fun getActiveProgram(): Flow<ProgramEntity?>
    fun getDaysForProgram(programId: Long): Flow<List<ProgramDayEntity>>
    fun getExercisesForDay(dayId: Long): Flow<List<ProgramExerciseEntity>>
    fun getExercisesForDays(dayIds: List<Long>): Flow<Map<Long, List<ProgramExerciseEntity>>>
    suspend fun saveGeneratedProgram(program: ProgramGenerator.GeneratedProgram): Long
}
