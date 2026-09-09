package com.gymcoach.app.domain.model

import com.gymcoach.app.data.local.dao.WorkoutWithStats as DaoWorkoutWithStats
import com.gymcoach.app.data.local.entity.WorkoutEntity
import com.gymcoach.app.data.local.entity.WorkoutExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutMappingTest {

    @Test
    fun `Workout default values`() {
        val now = Instant.now()
        val workout = Workout(
            date = now,
            startTime = now,
            endTime = now,
            duration = 3600,
            notes = "Test notes",
            completed = true
        )

        assertEquals(0L, workout.id)
        assertEquals("NOT_STARTED", workout.status)
        assertEquals(now, workout.date)
        assertEquals(now, workout.startTime)
        assertEquals(now, workout.endTime)
        assertEquals(3600L, workout.duration)
        assertEquals("Test notes", workout.notes)
        assertTrue(workout.completed)
    }

    @Test
    fun `Workout custom status and equality`() {
        val now = Instant.ofEpochMilli(1700000000000L)
        val workout1 = Workout(
            id = 42L,
            date = now,
            startTime = now,
            endTime = now.plusSeconds(1800),
            duration = 1800L,
            notes = "Leg day",
            completed = false,
            status = "IN_PROGRESS"
        )

        val workout2 = workout1.copy()
        val workout3 = workout1.copy(status = "COMPLETED")

        assertEquals(workout1, workout2)
        assertEquals("IN_PROGRESS", workout1.status)
        assertNotEquals(workout1, workout3)
    }

    @Test
    fun `WorkoutExercise default values and equality`() {
        val exercise1 = WorkoutExercise(
            workoutId = 10L,
            exerciseId = 100L,
            orderIndex = 1
        )

        assertEquals(0L, exercise1.id)
        assertEquals(10L, exercise1.workoutId)
        assertEquals(100L, exercise1.exerciseId)
        assertEquals(1, exercise1.orderIndex)

        val exercise2 = WorkoutExercise(
            id = 5L,
            workoutId = 10L,
            exerciseId = 100L,
            orderIndex = 1
        )
        assertNotEquals(exercise1, exercise2)
    }

    @Test
    fun `WorkoutSet default values and toEntity conversion`() {
        val workoutSet = WorkoutSet(
            workoutExerciseId = 20L,
            setNumber = 1,
            weight = 80.5,
            reps = 10,
            rpe = 8.0,
            restSeconds = 90,
            completed = true
        )

        assertEquals(0L, workoutSet.id)
        assertEquals(SetType.NORMAL, workoutSet.setType)

        val entity = workoutSet.toEntity()

        assertEquals(0L, entity.id)
        assertEquals(20L, entity.workoutExerciseId)
        assertEquals(1, entity.setNumber)
        assertEquals(80.5, entity.weight, 0.001)
        assertEquals(10, entity.reps)
        assertEquals(8.0, entity.rpe, 0.001)
        assertEquals(90, entity.restSeconds)
        assertTrue(entity.completed)
        assertEquals(SetType.NORMAL.ordinal, entity.setType)
    }

    @Test
    fun `WorkoutSet non-default SetType toEntity mapping`() {
        val warmupSet = WorkoutSet(
            id = 15L,
            workoutExerciseId = 20L,
            setNumber = 1,
            weight = 40.0,
            reps = 12,
            rpe = 5.0,
            restSeconds = 60,
            completed = true,
            setType = SetType.WARMUP
        )

        val dropSet = warmupSet.copy(setType = SetType.DROP)
        val failureSet = warmupSet.copy(setType = SetType.FAILURE)

        assertEquals(SetType.WARMUP.ordinal, warmupSet.toEntity().setType)
        assertEquals(SetType.DROP.ordinal, dropSet.toEntity().setType)
        assertEquals(SetType.FAILURE.ordinal, failureSet.toEntity().setType)
    }

    @Test
    fun `WorkoutSet toEntity and back domain mapping consistency`() {
        val originalDomainSet = WorkoutSet(
            id = 99L,
            workoutExerciseId = 123L,
            setNumber = 3,
            weight = 100.0,
            reps = 5,
            rpe = 9.5,
            restSeconds = 180,
            completed = true,
            setType = SetType.FAILURE
        )

        val entity = originalDomainSet.toEntity()
        val mappedBackDomainSet = WorkoutSet(
            id = entity.id,
            workoutExerciseId = entity.workoutExerciseId,
            setNumber = entity.setNumber,
            weight = entity.weight,
            reps = entity.reps,
            rpe = entity.rpe,
            restSeconds = entity.restSeconds,
            completed = entity.completed,
            setType = SetType.values().getOrElse(entity.setType) { SetType.NORMAL }
        )

        assertEquals(originalDomainSet, mappedBackDomainSet)
    }

    @Test
    fun `SetType invalid ordinal fallback in mapping`() {
        val invalidOrdinal = 999
        val fallbackSetType = SetType.values().getOrElse(invalidOrdinal) { SetType.NORMAL }
        assertEquals(SetType.NORMAL, fallbackSetType)
    }

    @Test
    fun `WorkoutEntity toDomain and Workout toEntity mapping symmetry`() {
        val dateMillis = 1710000000000L
        val startMillis = 1710000100000L
        val endMillis = 1710003700000L

        val workoutEntity = WorkoutEntity(
            id = 7L,
            date = dateMillis,
            startTime = startMillis,
            endTime = endMillis,
            duration = 3600L,
            notes = "Upper body focus",
            completed = true,
            status = "COMPLETED"
        )

        val domainWorkout = Workout(
            id = workoutEntity.id,
            date = Instant.ofEpochMilli(workoutEntity.date),
            startTime = Instant.ofEpochMilli(workoutEntity.startTime),
            endTime = Instant.ofEpochMilli(workoutEntity.endTime),
            duration = workoutEntity.duration,
            notes = workoutEntity.notes,
            completed = workoutEntity.completed,
            status = workoutEntity.status
        )

        val reMappedEntity = WorkoutEntity(
            id = domainWorkout.id,
            date = domainWorkout.date.toEpochMilli(),
            startTime = domainWorkout.startTime.toEpochMilli(),
            endTime = domainWorkout.endTime.toEpochMilli(),
            duration = domainWorkout.duration,
            notes = domainWorkout.notes,
            completed = domainWorkout.completed,
            status = domainWorkout.status
        )

        assertEquals(workoutEntity, reMappedEntity)
        assertEquals(Instant.ofEpochMilli(dateMillis), domainWorkout.date)
        assertEquals(Instant.ofEpochMilli(startMillis), domainWorkout.startTime)
        assertEquals(Instant.ofEpochMilli(endMillis), domainWorkout.endTime)
    }

    @Test
    fun `WorkoutExerciseEntity toDomain mapping`() {
        val entity = WorkoutExerciseEntity(
            id = 12L,
            workoutId = 34L,
            exerciseId = 56L,
            orderIndex = 2
        )

        val domain = WorkoutExercise(
            id = entity.id,
            workoutId = entity.workoutId,
            exerciseId = entity.exerciseId,
            orderIndex = entity.orderIndex
        )

        assertEquals(12L, domain.id)
        assertEquals(34L, domain.workoutId)
        assertEquals(56L, domain.exerciseId)
        assertEquals(2, domain.orderIndex)
    }

    @Test
    fun `Dao WorkoutWithStats to domain WorkoutWithStats mapping`() {
        val dateMillis = 1715000000000L
        val daoStats = DaoWorkoutWithStats(
            id = 100L,
            date = dateMillis,
            startTime = dateMillis,
            endTime = dateMillis + 3600000L,
            duration = 3600L,
            notes = "Push session",
            completed = true,
            status = "COMPLETED",
            volume = 4500.0,
            setCount = 15,
            repCount = 120,
            exerciseCount = 4
        )

        val domainStats = WorkoutWithStats(
            id = daoStats.id,
            date = Instant.ofEpochMilli(daoStats.date),
            startTime = Instant.ofEpochMilli(daoStats.startTime),
            endTime = Instant.ofEpochMilli(daoStats.endTime),
            duration = daoStats.duration,
            notes = daoStats.notes,
            completed = daoStats.completed,
            status = daoStats.status,
            volume = daoStats.volume,
            setCount = daoStats.setCount,
            repCount = daoStats.repCount,
            exerciseCount = daoStats.exerciseCount
        )

        assertEquals(100L, domainStats.id)
        assertEquals(Instant.ofEpochMilli(dateMillis), domainStats.date)
        assertEquals(4500.0, domainStats.volume, 0.001)
        assertEquals(15, domainStats.setCount)
        assertEquals(120, domainStats.repCount)
        assertEquals(4, domainStats.exerciseCount)
        assertEquals("NOT_STARTED", WorkoutWithStats(
            id = 1L,
            date = Instant.EPOCH,
            startTime = Instant.EPOCH,
            endTime = Instant.EPOCH,
            duration = 0,
            notes = "",
            completed = false,
            volume = 0.0,
            setCount = 0,
            repCount = 0,
            exerciseCount = 0
        ).status)
    }

    @Test
    fun `WorkoutWithDetails structure construction`() {
        val workout = Workout(
            id = 1L,
            date = Instant.EPOCH,
            startTime = Instant.EPOCH,
            endTime = Instant.EPOCH,
            duration = 0L,
            notes = "",
            completed = false
        )

        val exercise = Exercise(
            id = 10L,
            name = "Dumbbell Bench Press",
            description = "Chest exercise",
            muscleGroup = "CHEST",
            equipment = "DUMBBELL",
            difficulty = "INTERMEDIATE",
            secondaryMuscles = "TRICEPS",
            instructions = "Press up",
            tips = "Keep core tight",
            commonMistakes = "Flaring elbows",
            safetyNotes = "Use spotter if heavy",
            recommendedRepRange = "8-12",
            recommendedRestTime = "90",
            estimatedCalories = 100,
            category = "STRENGTH",
            tags = "chest,dumbbell",
            isFavorite = true,
            lastViewed = 0L,
            vtaperLat = 0,
            vtaperLateralDelt = 0,
            vtaperUpperChest = 10,
            vtaperRearDelt = 0,
            movementPattern = "PUSH",
            imageUrl = "",
            videoUrl = "",
            animationUrl = "",
            setupInstructions = "",
            executionInstructions = "",
            breathingInstructions = "",
            tempoGuidance = "",
            beginnerVariantId = null,
            advancedVariantId = null
        )

        val workoutExercise = WorkoutExercise(id = 100L, workoutId = 1L, exerciseId = 10L, orderIndex = 0)
        val set = WorkoutSet(id = 1000L, workoutExerciseId = 100L, setNumber = 1, weight = 24.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true)

        val exerciseWithSets = WorkoutExerciseWithSets(workoutExercise, exercise, listOf(set))
        val details = WorkoutWithDetails(workout, listOf(exerciseWithSets))

        assertEquals(workout, details.workout)
        assertEquals(1, details.exercises.size)
        assertEquals("Dumbbell Bench Press", details.exercises[0].exercise.name)
        assertEquals(1, details.exercises[0].sets.size)
        assertEquals(24.0, details.exercises[0].sets[0].weight, 0.001)
    }
}
