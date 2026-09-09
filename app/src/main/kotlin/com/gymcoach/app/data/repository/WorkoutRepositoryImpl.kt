package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutEntity
import com.gymcoach.app.data.local.entity.WorkoutExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import com.gymcoach.app.domain.model.CompletedSetContext
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.model.LastPerformance as DomainLastPerformance
import com.gymcoach.app.domain.model.LastSetData as DomainLastSetData
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutExercise
import com.gymcoach.app.domain.model.WorkoutExerciseWithSets
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutWithDetails
import com.gymcoach.app.domain.model.WorkoutWithStats
import com.gymcoach.app.domain.repository.WorkoutRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao
) : WorkoutRepository {

    private val emptyExercise = Exercise(
        id = 0,
        name = "",
        description = "",
        muscleGroup = "",
        equipment = "",
        difficulty = ""
    )

    override fun getAllWorkouts(): Flow<List<Workout>> {
        return workoutDao.getAllWorkouts().mapList { it.toDomain() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getWorkoutWithDetails(workoutId: Long): Flow<WorkoutWithDetails?> {
        return workoutDao.getWorkoutById(workoutId).flatMapLatest { workoutEntity ->
            if (workoutEntity == null) return@flatMapLatest flowOf(null)

            workoutDao.getExercisesForWorkout(workoutId).flatMapLatest { exerciseEntities ->
                if (exerciseEntities.isEmpty()) {
                    flowOf(WorkoutWithDetails(workoutEntity.toDomain(), emptyList()))
                } else {
                    val exerciseFlows = exerciseEntities.map { weEntity ->
                        combine(
                            exerciseDao.getById(weEntity.exerciseId),
                            workoutDao.getSetsForExercise(weEntity.id)
                        ) { exerciseEntity, setEntities ->
                            WorkoutExerciseWithSets(
                                workoutExercise = weEntity.toDomain(),
                                exercise = exerciseEntity?.toDomain() ?: emptyExercise,
                                sets = setEntities.map { it.toDomain() }
                            )
                        }
                    }
                    combine(exerciseFlows) { array ->
                        WorkoutWithDetails(
                            workout = workoutEntity.toDomain(),
                            exercises = array.toList()
                        )
                    }
                }
            }
        }
    }

    override suspend fun getLatestIncompleteWorkout(): Workout? {
        return workoutDao.getIncompleteWorkout()?.toDomain()
    }

    override suspend fun createWorkout(workout: Workout): Long {
        return workoutDao.insertWorkout(workout.toEntity())
    }

    override suspend fun updateWorkout(workout: Workout) {
        workoutDao.updateWorkout(workout.toEntity())
    }

    override suspend fun deleteWorkout(workoutId: Long) {
        val entity = workoutDao.getWorkoutById(workoutId).first()
        entity?.let { workoutDao.deleteWorkout(it) }
    }

    override suspend fun addExerciseToWorkout(workoutId: Long, exerciseId: Long, orderIndex: Int): Long {
        val entity = WorkoutExerciseEntity(
            workoutId = workoutId,
            exerciseId = exerciseId,
            orderIndex = orderIndex
        )
        return workoutDao.insertWorkoutExercise(entity)
    }

    override suspend fun removeExerciseFromWorkout(workoutExerciseId: Long) {
        val entity = workoutDao.getWorkoutExerciseById(workoutExerciseId)
        entity?.let { workoutDao.deleteWorkoutExercise(it) }
    }

    override suspend fun addSetToExercise(workoutExerciseId: Long, set: WorkoutSet): Long {
        val entity = set.toEntity().copy(workoutExerciseId = workoutExerciseId)
        return workoutDao.insertWorkoutSet(entity)
    }

    override suspend fun updateSet(set: WorkoutSet) {
        workoutDao.updateWorkoutSet(set.toEntity())
    }

    override suspend fun deleteSet(setId: Long) {
        val entity = workoutDao.getWorkoutSetById(setId)
        entity?.let { workoutDao.deleteWorkoutSet(it) }
    }

    // ─── Previous Performance ──────────────────────────────────────────

    override suspend fun getLastPerformanceForExercise(exerciseId: Long): DomainLastPerformance? {
        val daoPerf = workoutDao.getLastPerformanceForExercise(exerciseId) ?: return null
        return DomainLastPerformance(date = daoPerf.date, maxWeight = daoPerf.maxWeight)
    }

    override suspend fun getLastSetsForExercise(exerciseId: Long): List<DomainLastSetData> {
        return workoutDao.getLastSetsForExercise(exerciseId).map {
            DomainLastSetData(
                setNumber = 0,
                reps = it.reps,
                weight = it.weight,
                rpe = it.rpe?.toFloat(),
                restSeconds = it.restSeconds,
                setType = it.setType
            )
        }
    }

    override suspend fun getLastPerformancesForExercises(exerciseIds: List<Long>): Map<Long, DomainLastPerformance> {
        if (exerciseIds.isEmpty()) return emptyMap()
        val results = workoutDao.getLastPerformancesForExercises(exerciseIds)
        return results.associate {
            it.exerciseId to DomainLastPerformance(date = it.date, maxWeight = it.maxWeight)
        }
    }

    override suspend fun getLastSetsForExercises(exerciseIds: List<Long>): Map<Long, List<DomainLastSetData>> {
        if (exerciseIds.isEmpty()) return emptyMap()
        val results = workoutDao.getLastSetsForExercises(exerciseIds)
        return results.groupBy(
            keySelector = { it.exerciseId },
            valueTransform = {
                DomainLastSetData(
                    setNumber = 0,
                    reps = it.reps,
                    weight = it.weight,
                    rpe = it.rpe?.toFloat(),
                    restSeconds = it.restSeconds,
                    setType = it.setType
                )
            }
        )
    }

    // ─── History ───────────────────────────────────────────────────────

    override fun getCompletedWorkouts(): Flow<List<WorkoutWithStats>> {
        return workoutDao.getCompletedWorkoutsWithStats().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCompletedSetsWithContext(startDate: Long?): Flow<List<CompletedSetContext>> {
        return workoutDao.getCompletedSetsWithContext(startDate ?: 0L).map { list ->
            list.map {
                CompletedSetContext(
                    setId = it.id,
                    exerciseId = it.exerciseId,
                    workoutDate = it.workoutDate,
                    weightKg = it.weight,
                    reps = it.reps,
                    rpe = it.rpe?.toFloat(),
                    completed = it.completed,
                    setType = it.setType
                )
            }
        }
    }

    override fun getWorkoutsInDateRange(startDate: Long, endDate: Long): Flow<List<WorkoutWithStats>> {
        return workoutDao.getWorkoutsInDateRangeWithStats(startDate, endDate).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getWorkoutsByVolumeDesc(): Flow<List<WorkoutWithStats>> {
        return workoutDao.getCompletedWorkoutsWithStatsByVolumeDesc().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getWorkoutsByVolumeAsc(): Flow<List<WorkoutWithStats>> {
        return workoutDao.getCompletedWorkoutsWithStatsByVolumeAsc().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getWorkoutsByDurationDesc(): Flow<List<WorkoutWithStats>> {
        return workoutDao.getCompletedWorkoutsWithStatsByDurationDesc().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getWorkoutsByDurationAsc(): Flow<List<WorkoutWithStats>> {
        return workoutDao.getCompletedWorkoutsWithStatsByDurationAsc().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun searchWorkouts(query: String): List<WorkoutWithStats> {
        return workoutDao.searchWorkouts(query).map { it.toDomain() }
    }

    override suspend fun getIncompleteWorkout(): Workout? {
        return workoutDao.getIncompleteWorkout()?.toDomain()
    }

    override suspend fun createWorkoutFromHistory(workoutId: Long): Long? {
        val sourceWorkout = workoutDao.getWorkoutById(workoutId).first() ?: return null
        val sourceExercises = workoutDao.getExercisesForWorkout(workoutId).first()
        val exercisesWithSets = sourceExercises.map { exercise ->
            val sets = workoutDao.getSetsForExercise(exercise.id).first()
            exercise to sets
        }
        return workoutDao.createWorkoutFromHistoryTransaction(sourceWorkout, exercisesWithSets)
    }

    // ─── Domain Mappers ────────────────────────────────────────────────

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

    private fun WorkoutEntity.toDomain() = Workout(
        id = id,
        date = java.time.Instant.ofEpochMilli(date),
        startTime = java.time.Instant.ofEpochMilli(startTime),
        endTime = java.time.Instant.ofEpochMilli(endTime),
        duration = duration,
        notes = notes,
        completed = completed,
        status = status
    )

    private fun Workout.toEntity() = WorkoutEntity(
        id = id,
        date = date.toEpochMilli(),
        startTime = startTime.toEpochMilli(),
        endTime = endTime.toEpochMilli(),
        duration = duration,
        notes = notes,
        completed = completed,
        status = status
    )

    private fun WorkoutExerciseEntity.toDomain() = WorkoutExercise(
        id = id,
        workoutId = workoutId,
        exerciseId = exerciseId,
        orderIndex = orderIndex
    )

    private fun WorkoutSetEntity.toDomain() = WorkoutSet(
        id = id,
        workoutExerciseId = workoutExerciseId,
        setNumber = setNumber,
        weight = weight,
        reps = reps,
        rpe = rpe,
        restSeconds = restSeconds,
        completed = completed,
        setType = com.gymcoach.app.domain.model.SetType.entries.getOrElse(setType) { com.gymcoach.app.domain.model.SetType.NORMAL }
    )

    private fun WorkoutSet.toEntity() = WorkoutSetEntity(
        id = id,
        workoutExerciseId = workoutExerciseId,
        setNumber = setNumber,
        weight = weight,
        reps = reps,
        rpe = rpe,
        restSeconds = restSeconds,
        completed = completed,
        setType = setType.ordinal
    )

    private fun com.gymcoach.app.data.local.dao.WorkoutWithStats.toDomain() = WorkoutWithStats(
        id = id,
        date = java.time.Instant.ofEpochMilli(date),
        startTime = java.time.Instant.ofEpochMilli(startTime),
        endTime = java.time.Instant.ofEpochMilli(endTime),
        duration = duration,
        notes = notes,
        completed = completed,
        status = status,
        volume = volume,
        setCount = setCount,
        repCount = repCount,
        exerciseCount = exerciseCount
    )
}
