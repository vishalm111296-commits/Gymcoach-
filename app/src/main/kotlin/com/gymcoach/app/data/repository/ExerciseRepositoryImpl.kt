package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.ExerciseMuscleDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.model.ExerciseMuscleAssignment
import com.gymcoach.app.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val exerciseMuscleDao: ExerciseMuscleDao
) : ExerciseRepository {

    override fun getAllExercises(): Flow<List<Exercise>> {
        return exerciseDao.getAll().mapList { it.toDomain() }
    }

    override fun getFilteredExercises(
        muscle: String?,
        difficulty: String?,
        equipment: String?
    ): Flow<List<Exercise>> {
        return exerciseDao.getFilteredExercises(muscle, difficulty, equipment).mapList { it.toDomain() }
    }

    override fun searchExercises(query: String): Flow<List<Exercise>> {
        return exerciseDao.searchExercises(query).mapList { it.toDomain() }
    }

    override fun getExerciseById(id: Long): Flow<Exercise?> {
        return exerciseDao.getById(id).map { it?.toDomain() }
    }

    override fun getAllExerciseMuscleDetails(): Flow<List<ExerciseMuscleAssignment>> {
        return exerciseMuscleDao.getAllWithDetails().mapList {
            ExerciseMuscleAssignment(
                exerciseId = it.exerciseId,
                muscleName = it.muscleName,
                role = it.role
            )
        }
    }

    override suspend fun addExercise(exercise: Exercise) {
        exerciseDao.insert(exercise.toEntity())
    }

    override suspend fun updateExercise(exercise: Exercise) {
        exerciseDao.update(exercise.toEntity())
    }

    override suspend fun deleteExercise(exercise: Exercise) {
        exerciseDao.delete(exercise.toEntity())
    }

    private fun ExerciseEntity.toDomain() = Exercise(
        id = id,
        name = name,
        description = description,
        muscleGroup = muscleGroup,
        equipment = equipment,
        difficulty = difficulty,
        secondaryMuscles = secondaryMuscles,
        instructions = instructions,
        tips = tips,
        commonMistakes = commonMistakes,
        safetyNotes = safetyNotes,
        recommendedRepRange = recommendedRepRange,
        recommendedRestTime = recommendedRestTime,
        category = category,
        tags = tags,
        movementPattern = movementPattern,
        setupInstructions = setupInstructions,
        executionInstructions = executionInstructions,
        breathingInstructions = breathingInstructions,
        vtaperLat = vtaperLat,
        vtaperLateralDelt = vtaperLateralDelt,
        vtaperUpperChest = vtaperUpperChest,
        vtaperRearDelt = vtaperRearDelt
    )

    private fun Exercise.toEntity() = ExerciseEntity(
        id = id,
        name = name,
        description = description,
        muscleGroup = muscleGroup,
        equipment = equipment,
        difficulty = difficulty,
        secondaryMuscles = secondaryMuscles,
        instructions = instructions,
        tips = tips,
        commonMistakes = commonMistakes,
        safetyNotes = safetyNotes,
        recommendedRepRange = recommendedRepRange,
        recommendedRestTime = recommendedRestTime,
        category = category,
        tags = tags,
        movementPattern = movementPattern,
        setupInstructions = setupInstructions,
        executionInstructions = executionInstructions,
        breathingInstructions = breathingInstructions,
        vtaperLat = vtaperLat,
        vtaperLateralDelt = vtaperLateralDelt,
        vtaperUpperChest = vtaperUpperChest,
        vtaperRearDelt = vtaperRearDelt
    )
}
