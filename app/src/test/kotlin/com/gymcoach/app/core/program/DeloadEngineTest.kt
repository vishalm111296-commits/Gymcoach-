package com.gymcoach.app.core.program

import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.model.SetType
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutExercise
import com.gymcoach.app.domain.model.WorkoutExerciseWithSets
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutWithDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class DeloadEngineTest {

    private lateinit var deloadEngine: DeloadEngine

    @Before
    fun setUp() {
        deloadEngine = DeloadEngine()
    }

    @Test
    fun `test mesocycle trigger when week is 4 or higher`() {
        val statusWeek4 = deloadEngine.evaluateDeloadNeed(
            workoutHistory = emptyList(),
            readinessScores = emptyList(),
            currentWeekInBlock = 4
        )
        assertTrue("Deload should be recommended at week 4", statusWeek4.isDeloadRecommended)
        assertEquals(DeloadReason.PLANNED_MESOCYCLE_END, statusWeek4.reason)
        assertEquals(4, statusWeek4.accumulatedWeeks)
        assertEquals(50, statusWeek4.volumeReductionPercent)
        assertEquals(10, statusWeek4.intensityReductionPercent)
        assertTrue(statusWeek4.coachingAdvice.contains("progressive overload"))

        val statusWeek6 = deloadEngine.evaluateDeloadNeed(
            workoutHistory = emptyList(),
            readinessScores = emptyList(),
            currentWeekInBlock = 6
        )
        assertTrue("Deload should be recommended at week 6", statusWeek6.isDeloadRecommended)
        assertEquals(DeloadReason.PLANNED_MESOCYCLE_END, statusWeek6.reason)
        assertEquals(6, statusWeek6.accumulatedWeeks)
    }

    @Test
    fun `test low readiness trigger below 55 sustained fatigue`() {
        // Sustained low readiness in week 2
        val lowScores = listOf(48, 52, 45)
        val status = deloadEngine.evaluateDeloadNeed(
            workoutHistory = emptyList(),
            readinessScores = lowScores,
            currentWeekInBlock = 2
        )
        assertTrue("Deload should be recommended when readiness is below 55", status.isDeloadRecommended)
        assertEquals(DeloadReason.FATIGUE_ACCUMULATION, status.reason)
        assertEquals(2, status.accumulatedWeeks)
        assertEquals(50, status.volumeReductionPercent)
        assertEquals(10, status.intensityReductionPercent)
        assertTrue(status.coachingAdvice.contains("accumulated fatigue"))

        // Single low readiness score
        val singleLowStatus = deloadEngine.evaluateDeloadNeed(
            workoutHistory = emptyList(),
            readinessScores = listOf(40),
            currentWeekInBlock = 1
        )
        assertTrue(singleLowStatus.isDeloadRecommended)
        assertEquals(DeloadReason.FATIGUE_ACCUMULATION, singleLowStatus.reason)
    }

    @Test
    fun `test no-deload case for fresh lifter with high readiness and early block`() {
        val highScores = listOf(80, 85, 90)
        val normalWorkout = createWorkoutWithDetails(
            exercises = listOf(
                createExerciseWithSets(
                    sets = listOf(
                        createWorkoutSet(setNumber = 1, weight = 80.0, reps = 10),
                        createWorkoutSet(setNumber = 2, weight = 80.0, reps = 10)
                    )
                )
            )
        )

        val status = deloadEngine.evaluateDeloadNeed(
            workoutHistory = listOf(normalWorkout),
            readinessScores = highScores,
            currentWeekInBlock = 2
        )

        assertFalse("Deload should NOT be recommended for fresh lifter", status.isDeloadRecommended)
        assertNull("Reason should be null when deload is not needed", status.reason)
        assertEquals(2, status.accumulatedWeeks)
        assertEquals(0, status.volumeReductionPercent)
        assertEquals(0, status.intensityReductionPercent)
        assertTrue(status.coachingAdvice.contains("Continue progressive overload"))
    }

    @Test
    fun `test stagnation plateau triggers deload across consecutive sessions`() {
        val baseTime = Instant.now()
        // 3 consecutive workouts with non-increasing / stagnant volume
        val w1 = createWorkoutWithDetails(
            workoutId = 1,
            date = baseTime.minusSeconds(86400 * 5),
            exercises = listOf(createExerciseWithSets(sets = listOf(createWorkoutSet(1, 100.0, 10)))) // vol 1000
        )
        val w2 = createWorkoutWithDetails(
            workoutId = 2,
            date = baseTime.minusSeconds(86400 * 3),
            exercises = listOf(createExerciseWithSets(sets = listOf(createWorkoutSet(1, 100.0, 10)))) // vol 1000
        )
        val w3 = createWorkoutWithDetails(
            workoutId = 3,
            date = baseTime.minusSeconds(86400 * 1),
            exercises = listOf(createExerciseWithSets(sets = listOf(createWorkoutSet(1, 100.0, 9))))  // vol 900
        )

        val status = deloadEngine.evaluateDeloadNeed(
            workoutHistory = listOf(w1, w2, w3),
            readinessScores = listOf(70, 72),
            currentWeekInBlock = 2
        )

        assertTrue("Deload should be recommended on stagnation plateau", status.isDeloadRecommended)
        assertEquals(DeloadReason.STAGNATION_PLATEAU, status.reason)
        assertTrue(status.coachingAdvice.contains("plateau"))
    }

    @Test
    fun `test recovery reset trigger when explicitly requested`() {
        val status = deloadEngine.evaluateDeloadNeed(
            workoutHistory = emptyList(),
            readinessScores = emptyList(),
            currentWeekInBlock = 1,
            userRequestedReset = true
        )

        assertTrue(status.isDeloadRecommended)
        assertEquals(DeloadReason.RECOVERY_RESET, status.reason)
        assertTrue(status.coachingAdvice.contains("recovery reset"))
    }

    @Test
    fun `test applyDeloadToWorkout scaling verifies set count halved and weights reduced by 10 percent`() {
        // Exercise 1: 4 sets of 100.0 kg
        val setsEx1 = listOf(
            createWorkoutSet(setNumber = 1, weight = 100.0, reps = 10),
            createWorkoutSet(setNumber = 2, weight = 100.0, reps = 10),
            createWorkoutSet(setNumber = 3, weight = 100.0, reps = 10),
            createWorkoutSet(setNumber = 4, weight = 100.0, reps = 10)
        )
        // Exercise 2: 6 sets of 80.0 kg
        val setsEx2 = listOf(
            createWorkoutSet(setNumber = 1, weight = 80.0, reps = 8),
            createWorkoutSet(setNumber = 2, weight = 80.0, reps = 8),
            createWorkoutSet(setNumber = 3, weight = 80.0, reps = 8),
            createWorkoutSet(setNumber = 4, weight = 80.0, reps = 8),
            createWorkoutSet(setNumber = 5, weight = 80.0, reps = 8),
            createWorkoutSet(setNumber = 6, weight = 80.0, reps = 8)
        )

        val originalWorkout = createWorkoutWithDetails(
            exercises = listOf(
                createExerciseWithSets(exerciseId = 1, name = "Barbell Squat", sets = setsEx1),
                createExerciseWithSets(exerciseId = 2, name = "Bench Press", sets = setsEx2)
            )
        )

        val deloadedWorkout = deloadEngine.applyDeloadToWorkout(originalWorkout, weightIncrement = 0.5)

        // Verify Exercise 1: 4 sets halved to 2 sets, 100kg -> 90kg (10% reduction)
        val deloadedEx1 = deloadedWorkout.exercises[0]
        assertEquals("Set count should be halved from 4 to 2", 2, deloadedEx1.sets.size)
        assertEquals(1, deloadedEx1.sets[0].setNumber)
        assertEquals(2, deloadedEx1.sets[1].setNumber)
        assertEquals(90.0, deloadedEx1.sets[0].weight, 0.01)
        assertEquals(90.0, deloadedEx1.sets[1].weight, 0.01)

        // Verify Exercise 2: 6 sets halved to 3 sets, 80kg -> 72kg (10% reduction)
        val deloadedEx2 = deloadedWorkout.exercises[1]
        assertEquals("Set count should be halved from 6 to 3", 3, deloadedEx2.sets.size)
        assertEquals(1, deloadedEx2.sets[0].setNumber)
        assertEquals(2, deloadedEx2.sets[1].setNumber)
        assertEquals(3, deloadedEx2.sets[2].setNumber)
        assertEquals(72.0, deloadedEx2.sets[0].weight, 0.01)
        assertEquals(72.0, deloadedEx2.sets[1].weight, 0.01)
        assertEquals(72.0, deloadedEx2.sets[2].weight, 0.01)
    }

    @Test
    fun `test applyDeloadToWorkout with 2_5kg plate increments`() {
        // Exercise with 80kg: 80 * 0.9 = 72.0 -> rounded to nearest 2.5kg = 72.5kg
        val sets = listOf(
            createWorkoutSet(setNumber = 1, weight = 80.0, reps = 10),
            createWorkoutSet(setNumber = 2, weight = 80.0, reps = 10)
        )
        val workout = createWorkoutWithDetails(
            exercises = listOf(createExerciseWithSets(exerciseId = 1, name = "Deadlift", sets = sets))
        )

        val deloadedWorkout = deloadEngine.applyDeloadToWorkout(workout, weightIncrement = 2.5)
        val deloadedEx = deloadedWorkout.exercises[0]
        assertEquals(1, deloadedEx.sets.size) // 2 sets halved to 1
        assertEquals(72.5, deloadedEx.sets[0].weight, 0.01)
    }

    @Test
    fun `test edge cases empty history and 1-set exercises`() {
        // Edge Case 1: Empty history
        val emptyHistoryStatus = deloadEngine.evaluateDeloadNeed(
            workoutHistory = emptyList(),
            readinessScores = emptyList(),
            currentWeekInBlock = 1
        )
        assertFalse(emptyHistoryStatus.isDeloadRecommended)
        assertNull(emptyHistoryStatus.reason)
        assertEquals(1, emptyHistoryStatus.accumulatedWeeks)

        // Edge Case 2: 1-set exercise should keep 1 set (minimum 1 set, not 0)
        val singleSet = listOf(
            createWorkoutSet(setNumber = 1, weight = 50.0, reps = 12)
        )
        val singleSetWorkout = createWorkoutWithDetails(
            exercises = listOf(createExerciseWithSets(exerciseId = 10, name = "Lateral Raise", sets = singleSet))
        )

        val deloadedSingleSet = deloadEngine.applyDeloadToWorkout(singleSetWorkout)
        val resultSets = deloadedSingleSet.exercises[0].sets
        assertEquals("1-set exercise should maintain minimum 1 set", 1, resultSets.size)
        assertEquals(1, resultSets[0].setNumber)
        assertEquals(45.0, resultSets[0].weight, 0.01) // 50 * 0.9 = 45.0

        // Edge Case 3: Bodyweight exercise (weight = 0.0)
        val bodyweightSets = listOf(
            createWorkoutSet(setNumber = 1, weight = 0.0, reps = 15),
            createWorkoutSet(setNumber = 2, weight = 0.0, reps = 15)
        )
        val bodyweightWorkout = createWorkoutWithDetails(
            exercises = listOf(createExerciseWithSets(exerciseId = 20, name = "Push Up", sets = bodyweightSets))
        )
        val deloadedBodyweight = deloadEngine.applyDeloadToWorkout(bodyweightWorkout)
        val bwSets = deloadedBodyweight.exercises[0].sets
        assertEquals(1, bwSets.size)
        assertEquals(0.0, bwSets[0].weight, 0.01)

        // Edge Case 4: Workout with no exercises
        val emptyWorkout = createWorkoutWithDetails(exercises = emptyList())
        val deloadedEmpty = deloadEngine.applyDeloadToWorkout(emptyWorkout)
        assertTrue(deloadedEmpty.exercises.isEmpty())
    }

    @Test
    fun `test roundToNearestIncrement utility`() {
        assertEquals(90.0, deloadEngine.roundToNearestIncrement(90.0, 0.5), 0.01)
        assertEquals(90.0, deloadEngine.roundToNearestIncrement(90.0, 2.5), 0.01)
        assertEquals(72.0, deloadEngine.roundToNearestIncrement(72.0, 0.5), 0.01)
        assertEquals(72.5, deloadEngine.roundToNearestIncrement(72.0, 2.5), 0.01)
        assertEquals(0.0, deloadEngine.roundToNearestIncrement(0.0, 0.5), 0.01)
        assertEquals(10.0, deloadEngine.roundToNearestIncrement(10.0, 0.0), 0.01)
    }

    private fun createWorkoutWithDetails(
        workoutId: Long = 1L,
        date: Instant = Instant.now(),
        exercises: List<WorkoutExerciseWithSets> = emptyList()
    ): WorkoutWithDetails {
        val workout = Workout(
            id = workoutId,
            date = date,
            startTime = date,
            endTime = date.plusSeconds(3600),
            duration = 3600,
            notes = "Test workout",
            completed = true
        )
        return WorkoutWithDetails(workout = workout, exercises = exercises)
    }

    private fun createExerciseWithSets(
        exerciseId: Long = 101L,
        name: String = "Bench Press",
        sets: List<WorkoutSet>
    ): WorkoutExerciseWithSets {
        val exercise = Exercise(
            id = exerciseId,
            name = name,
            description = "Test exercise",
            muscleGroup = "Chest",
            equipment = "barbell",
            difficulty = "Intermediate"
        )
        val workoutExercise = WorkoutExercise(
            id = 1L,
            workoutId = 1L,
            exerciseId = exerciseId,
            orderIndex = 1
        )
        return WorkoutExerciseWithSets(
            workoutExercise = workoutExercise,
            exercise = exercise,
            sets = sets
        )
    }

    private fun createWorkoutSet(
        setNumber: Int,
        weight: Double,
        reps: Int = 10,
        completed: Boolean = true,
        setType: SetType = SetType.NORMAL
    ): WorkoutSet {
        return WorkoutSet(
            id = setNumber.toLong(),
            workoutExerciseId = 1L,
            setNumber = setNumber,
            weight = weight,
            reps = reps,
            rpe = 8.0,
            restSeconds = 90,
            completed = completed,
            setType = setType
        )
    }
}
