package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao
) : ExerciseRepository {

    override fun getAllExercises(): Flow<List<Exercise>> {
        return exerciseDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFilteredExercises(muscle: String?, difficulty: String?, equipment: String?): Flow<List<Exercise>> {
        return exerciseDao.getFilteredExercises(muscle, difficulty, equipment).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchExercises(query: String): Flow<List<Exercise>> {
        return exerciseDao.searchExercises(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getExerciseById(id: Long): Flow<Exercise?> {
        return exerciseDao.getById(id).map { it?.toDomain() }
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
        estimatedCalories = estimatedCalories,
        category = category,
        tags = tags,
        isFavorite = isFavorite,
        lastViewed = lastViewed,
        // V-taper relevance scores (0-10)
        vtaperLat = vtaperLat,
        vtaperLateralDelt = vtaperLateralDelt,
        vtaperUpperChest = vtaperUpperChest,
        vtaperRearDelt = vtaperRearDelt,
        // Movement pattern
        movementPattern = movementPattern,
        // Media (nullable, architecture-ready)
        imageUrl = imageUrl,
        videoUrl = videoUrl,
        animationUrl = animationUrl,
        // Instructions
        setupInstructions = setupInstructions,
        executionInstructions = executionInstructions,
        breathingInstructions = breathingInstructions,
        tempoGuidance = tempoGuidance,
        // Progression variants
        beginnerVariantId = beginnerVariantId,
        advancedVariantId = advancedVariantId
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
        estimatedCalories = estimatedCalories,
        category = category,
        tags = tags,
        isFavorite = isFavorite,
        lastViewed = lastViewed,
        // V-taper relevance scores (0-10)
        vtaperLat = vtaperLat,
        vtaperLateralDelt = vtaperLateralDelt,
        vtaperUpperChest = vtaperUpperChest,
        vtaperRearDelt = vtaperRearDelt,
        // Movement pattern
        movementPattern = movementPattern,
        // Media (nullable, architecture-ready)
        imageUrl = imageUrl,
        videoUrl = videoUrl,
        animationUrl = animationUrl,
        // Instructions
        setupInstructions = setupInstructions,
        executionInstructions = executionInstructions,
        breathingInstructions = breathingInstructions,
        tempoGuidance = tempoGuidance,
        // Progression variants
        beginnerVariantId = beginnerVariantId,
        advancedVariantId = advancedVariantId
    )
}
