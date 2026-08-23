package com.gymcoach.app.data.repository

import com.gymcoach.app.core.program.ProgramGenerator
import com.gymcoach.app.data.local.dao.ProgramDayDao
import com.gymcoach.app.data.local.dao.ProgramDao
import com.gymcoach.app.data.local.dao.ProgramExerciseDao
import com.gymcoach.app.data.local.entity.ProgramDayEntity
import com.gymcoach.app.data.local.entity.ProgramEntity
import com.gymcoach.app.data.local.entity.ProgramExerciseEntity
import com.gymcoach.app.domain.repository.ProgramRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class ProgramRepositoryImpl @Inject constructor(
    private val programDao: ProgramDao,
    private val programDayDao: ProgramDayDao,
    private val programExerciseDao: ProgramExerciseDao
) : ProgramRepository {

    override fun getActiveProgram(): Flow<ProgramEntity?> = programDao.getActiveProgram()

    override fun getDaysForProgram(programId: Long): Flow<List<ProgramDayEntity>> =
        programDayDao.getByProgramId(programId)

    override fun getExercisesForDay(dayId: Long): Flow<List<ProgramExerciseEntity>> =
        programExerciseDao.getByDayId(dayId)

    override fun getExercisesForDays(dayIds: List<Long>): Flow<Map<Long, List<ProgramExerciseEntity>>> {
        if (dayIds.isEmpty()) return flowOf(emptyMap())
        return combine(dayIds.map { id -> programExerciseDao.getByDayId(id).map { id to it } }) { pairs ->
            pairs.toMap()
        }
    }

    override suspend fun saveGeneratedProgram(program: ProgramGenerator.GeneratedProgram): Long {
        programDao.getActiveProgram().firstOrNull()?.let { active ->
            programDao.update(active.copy(isActive = false))
        }
        val programId = programDao.insert(
            ProgramEntity(
                name = program.name,
                description = program.description,
                goal = program.goal,
                daysPerWeek = program.frequency,
                isActive = true
            )
        )
        program.days.forEach { day ->
            val dayId = programDayDao.insert(
                ProgramDayEntity(
                    programId = programId,
                    dayNumber = day.dayNumber,
                    name = day.name,
                    focus = day.targetMuscles.joinToString(",")
                )
            )
            programExerciseDao.insertAll(
                day.exercises.mapIndexed { index, exercise ->
                    ProgramExerciseEntity(
                        programDayId = dayId,
                        exerciseId = exercise.exerciseId,
                        orderIndex = index,
                        sets = exercise.targetSets,
                        targetReps = "${exercise.targetRepsMin}-${exercise.targetRepsMax}",
                        restSeconds = exercise.restSeconds
                    )
                }
            )
        }
        return programId
    }
}
