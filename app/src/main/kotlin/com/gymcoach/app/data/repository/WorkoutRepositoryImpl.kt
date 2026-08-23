package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.ExerciseDao
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutEntity
import com.gymcoach.app.data.local.entity.WorkoutExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutExercise
import com.gymcoach.app.domain.model.WorkoutExerciseWithSets
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutStatus
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
import java.time.Instant
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao
) : WorkoutRepository {

    override fun getAllWorkouts(): Flow<List<Workout>> {
        return workoutDao.getAllWorkouts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getWorkoutWithDetails(workoutId: Long): Flow<WorkoutWithDetails?> {
        return workoutDao.getWorkoutById(workoutId).flatMapLatest { workoutEntity ->
            if (workoutEntity == null) {
                flowOf(null)
            } else {
                val workout = workoutEntity.toDomain()
                workoutDao.getExercisesForWorkout(workoutId).flatMapLatest { exerciseEntities ->
                    if (exerciseEntities.isEmpty()) {
                        flowOf(WorkoutWithDetails(workout, emptyList()))
                    } else {
                        val flows = exerciseEntities.map { we ->
                            val exerciseFlow = exerciseDao.getById(we.exerciseId).map { entity ->
                                entity?.toDomain()
                            }
                            val setsFlow = workoutDao.getSetsForExercise(we.id).map { sets ->
                                sets.map { it.toDomain() }
                            }
                            combine(exerciseFlow, setsFlow) { exercise, sets ->
                                if (exercise != null) {
                                    WorkoutExerciseWithSets(we.toDomain(), exercise, sets)
                                } else {
                                    null
                                }
                            }
                        }
                        combine(flows) { list ->
                            WorkoutWithDetails(workout, list.filterNotNull())
                        }
                    }
                }
            }
        }
    }

    /**
     * v7 semantics: returns only ACTIVE workouts. Legacy `completed=0` zombies
     * were backfilled to ABANDONED by MIGRATION_6_7 and never surface here,
     * which is what kills the phantom "Resume Workout" behavior.
     */
    override suspend fun getLatestIncompleteWorkout(): Workout? {
        return workoutDao.getActiveWorkout()?.toDomain()
    }

    @Deprecated(
        "Duplicate of getLatestIncompleteWorkout; kept so existing callers compile",
        ReplaceWith("getLatestIncompleteWorkout()")
    )
    override suspend fun getIncompleteWorkout(): Workout? = getLatestIncompleteWorkout()

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
        return workoutDao.insertWorkoutExercise(
            WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exerciseId, orderIndex = orderIndex)
        )
    }

    override suspend fun removeExerciseFromWorkout(workoutExerciseId: Long) {
        val entity = workoutDao.getWorkoutExerciseById(workoutExerciseId)
        entity?.let { workoutDao.deleteWorkoutExercise(it) }
    }

    override suspend fun addSetToExercise(workoutExerciseId: Long, set: WorkoutSet): Long {
        return workoutDao.insertWorkoutSet(set.toEntity().copy(workoutExerciseId = workoutExerciseId))
    }

    override suspend fun updateSet(set: WorkoutSet) {
        workoutDao.updateWorkoutSet(set.toEntity())
    }

    override suspend fun deleteSet(setId: Long) {
        val entity = workoutDao.getWorkoutSetById(setId)
        entity?.let { workoutDao.deleteWorkoutSet(it) }
    }

    override fun getCompletedWorkouts(): Flow<List<WorkoutWithStats>> {
        return workoutDao.getCompletedWorkoutsWithStats().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getWorkoutsInDateRange(startDate: Long, endDate: Long): Flow<List<WorkoutWithStats>> {
        return workoutDao.getWorkoutsInDateRangeWithStats(startDate, endDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getWorkoutsByVolumeDesc(): Flow<List<WorkoutWithStats>> {
        return workoutDao.getCompletedWorkoutsWithStatsByVolumeDesc().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getWorkoutsByVolumeAsc(): Flow<List<WorkoutWithStats>> {
        return workoutDao.getCompletedWorkoutsWithStatsByVolumeAsc().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getWorkoutsByDurationDesc(): Flow<List<WorkoutWithStats>> {
        return workoutDao.getCompletedWorkoutsWithStatsByDurationDesc().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getWorkoutsByDurationAsc(): Flow<List<WorkoutWithStats>> {
        return workoutDao.getCompletedWorkoutsWithStatsByDurationAsc().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun searchWorkouts(query: String): List<WorkoutWithStats> {
        return workoutDao.searchWorkouts(query).map { it.toDomain() }
    }
}

// Entity -> Domain mappers
private fun WorkoutEntity.toDomain() = Workout(
    id = id,
    date = Instant.ofEpochMilli(date),
    startTime = Instant.ofEpochMilli(startTime),
    endTime = Instant.ofEpochMilli(endTime),
    duration = duration,
    notes = notes,
    completed = completed,
    status = WorkoutStatus.fromString(status, completed)
)

private fun Workout.toEntity() = WorkoutEntity(
    id = id,
    date = date.toEpochMilli(),
    startTime = startTime.toEpochMilli(),
    endTime = endTime.toEpochMilli(),
    duration = duration,
    notes = notes,
    completed = completed,
    status = status.name
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
    setType = com.gymcoach.app.domain.model.SetType.values().getOrElse(setType) { com.gymcoach.app.domain.model.SetType.NORMAL }
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

private fun ExerciseEntity.toDomain() = Exercise(
    id = id,
    name = name,
    description = description,
    muscleGroup = muscleGroup,
    equipment = equipment,
    difficulty = difficulty
)

private fun com.gymcoach.app.data.local.dao.WorkoutWithStats.toDomain() = com.gymcoach.app.domain.model.WorkoutWithStats(
    id = id,
    date = Instant.ofEpochMilli(date),
    startTime = Instant.ofEpochMilli(startTime),
    endTime = Instant.ofEpochMilli(endTime),
    duration = duration,
    notes = notes,
    completed = completed,
    volume = volume ?: 0.0,
    setCount = setCount,
    repCount = repCount ?: 0,
    exerciseCount = exerciseCount
)
