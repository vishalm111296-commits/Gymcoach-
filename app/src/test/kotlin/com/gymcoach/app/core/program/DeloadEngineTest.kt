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

    @Test
    fun `stagnation plateau sorts scrambled dates correctly`() {
        val baseTime = Instant.now()
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

        // Pass workouts in scrambled order: w2, w3, w1
        val status = deloadEngine.evaluateDeloadNeed(
            workoutHistory = listOf(w2, w3, w1),
            readinessScores = listOf(70, 75),
            currentWeekInBlock = 2
        )

        assertTrue("Deload should be recommended despite scrambled order", status.isDeloadRecommended)
        assertEquals(DeloadReason.STAGNATION_PLATEAU, status.reason)
    }

    @Test
    fun `progressive volume does not trigger stagnation plateau`() {
        val baseTime = Instant.now()
        val w1 = createWorkoutWithDetails(
            workoutId = 1,
            date = baseTime.minusSeconds(86400 * 5),
            exercises = listOf(createExerciseWithSets(sets = listOf(createWorkoutSet(1, 100.0, 10)))) // vol 1000
        )
        val w2 = createWorkoutWithDetails(
            workoutId = 2,
            date = baseTime.minusSeconds(86400 * 3),
            exercises = listOf(createExerciseWithSets(sets = listOf(createWorkoutSet(1, 110.0, 10)))) // vol 1100
        )
        val w3 = createWorkoutWithDetails(
            workoutId = 3,
            date = baseTime.minusSeconds(86400 * 1),
            exercises = listOf(createExerciseWithSets(sets = listOf(createWorkoutSet(1, 120.0, 10)))) // vol 1200
        )

        val status = deloadEngine.evaluateDeloadNeed(
            workoutHistory = listOf(w1, w2, w3),
            readinessScores = listOf(75, 80),
            currentWeekInBlock = 2
        )

        assertFalse("Deload should NOT be recommended for progressive volume", status.isDeloadRecommended)
        assertNull(status.reason)
    }

    @Test
    fun `mixed readiness history triggers fatigue accumulation when recent window average is low`() {
        // Overall average: (85 + 75 + 45 + 50 + 48) / 5 = 60.6 (above 55 threshold)
        // But recent 3 scores: (45 + 50 + 48) / 3 = 47.66 (below 55 threshold)
        val mixedScores = listOf(85, 75, 45, 50, 48)
        val status = deloadEngine.evaluateDeloadNeed(
            workoutHistory = emptyList(),
            readinessScores = mixedScores,
            currentWeekInBlock = 2
        )

        assertTrue(status.isDeloadRecommended)
        assertEquals(DeloadReason.FATIGUE_ACCUMULATION, status.reason)
        assertTrue(status.coachingAdvice.contains("accumulated fatigue"))
    }

    @Test
    fun `applyDeloadToWorkout handles various set counts with odd numbers and sequential reindexing`() {
        val ex1Sets = listOf(createWorkoutSet(1, 100.0, 10)) // 1 set -> 1
        val ex2Sets = (1..3).map { createWorkoutSet(it, 80.0, 8) } // 3 sets -> round(1.5) = 2
        val ex3Sets = (1..5).map { createWorkoutSet(it, 60.0, 10) } // 5 sets -> round(2.5) = 3
        val ex4Sets = (1..7).map { createWorkoutSet(it, 40.0, 12) } // 7 sets -> round(3.5) = 4

        val originalWorkout = createWorkoutWithDetails(
            exercises = listOf(
                createExerciseWithSets(1, "Squat", ex1Sets),
                createExerciseWithSets(2, "Bench", ex2Sets),
                createExerciseWithSets(3, "Row", ex3Sets),
                createExerciseWithSets(4, "Curl", ex4Sets)
            )
        )

        val deloaded = deloadEngine.applyDeloadToWorkout(originalWorkout)

        assertEquals(1, deloaded.exercises[0].sets.size)
        assertEquals(listOf(1), deloaded.exercises[0].sets.map { it.setNumber })

        assertEquals(2, deloaded.exercises[1].sets.size)
        assertEquals(listOf(1, 2), deloaded.exercises[1].sets.map { it.setNumber })

        assertEquals(3, deloaded.exercises[2].sets.size)
        assertEquals(listOf(1, 2, 3), deloaded.exercises[2].sets.map { it.setNumber })

        assertEquals(4, deloaded.exercises[3].sets.size)
        assertEquals(listOf(1, 2, 3, 4), deloaded.exercises[3].sets.map { it.setNumber })
    }

    @Test
    fun `exact 55 readiness boundary conditions distinguish optimal recovery from fatigue accumulation`() {
        val statusExact55 = deloadEngine.evaluateDeloadNeed(
            workoutHistory = emptyList(),
            readinessScores = listOf(55, 55, 55),
            currentWeekInBlock = 2
        )
        assertFalse("Readiness at exactly 55 threshold should NOT trigger fatigue accumulation deload", statusExact55.isDeloadRecommended)
        assertNull(statusExact55.reason)

        val statusSub55 = deloadEngine.evaluateDeloadNeed(
            workoutHistory = emptyList(),
            readinessScores = listOf(54, 55, 55),
            currentWeekInBlock = 2
        )
        assertTrue("Readiness average < 55 should trigger fatigue accumulation deload", statusSub55.isDeloadRecommended)
        assertEquals(DeloadReason.FATIGUE_ACCUMULATION, statusSub55.reason)
    }

    @Test
    fun `strict evaluation priority order enforces user reset over mesocycle over fatigue over plateau`() {
        val baseTime = Instant.now()
        val stagnantWorkouts = listOf(
            createWorkoutWithDetails(1, baseTime.minusSeconds(86400 * 3), listOf(createExerciseWithSets(sets = listOf(createWorkoutSet(1, 100.0, 10))))),
            createWorkoutWithDetails(2, baseTime.minusSeconds(86400 * 2), listOf(createExerciseWithSets(sets = listOf(createWorkoutSet(1, 100.0, 10))))),
            createWorkoutWithDetails(3, baseTime.minusSeconds(86400 * 1), listOf(createExerciseWithSets(sets = listOf(createWorkoutSet(1, 100.0, 9)))))
        )
        val lowReadiness = listOf(40, 45, 50)

        // 1. User reset priority over everything (even week >= 4, low readiness, stagnation)
        val resetStatus = deloadEngine.evaluateDeloadNeed(
            workoutHistory = stagnantWorkouts,
            readinessScores = lowReadiness,
            currentWeekInBlock = 5,
            userRequestedReset = true
        )
        assertEquals(DeloadReason.RECOVERY_RESET, resetStatus.reason)

        // 2. Mesocycle week priority over low readiness and stagnation
        val mesoStatus = deloadEngine.evaluateDeloadNeed(
            workoutHistory = stagnantWorkouts,
            readinessScores = lowReadiness,
            currentWeekInBlock = 4,
            userRequestedReset = false
        )
        assertEquals(DeloadReason.PLANNED_MESOCYCLE_END, mesoStatus.reason)

        // 3. Low readiness priority over stagnation plateau (when week < 4)
        val fatigueStatus = deloadEngine.evaluateDeloadNeed(
            workoutHistory = stagnantWorkouts,
            readinessScores = lowReadiness,
            currentWeekInBlock = 2,
            userRequestedReset = false
        )
        assertEquals(DeloadReason.FATIGUE_ACCUMULATION, fatigueStatus.reason)
    }

    @Test
    fun `workout history out of order is sorted chronologically for stagnation plateau detection`() {
        val baseTime = Instant.now()
        val w1 = createWorkoutWithDetails(
            workoutId = 1,
            date = baseTime.minusSeconds(86400 * 5),
            exercises = listOf(createExerciseWithSets(sets = listOf(createWorkoutSet(1, 100.0, 10))))
        )
        val w2 = createWorkoutWithDetails(
            workoutId = 2,
            date = baseTime.minusSeconds(86400 * 3),
            exercises = listOf(createExerciseWithSets(sets = listOf(createWorkoutSet(1, 100.0, 10))))
        )
        val w3 = createWorkoutWithDetails(
            workoutId = 3,
            date = baseTime.minusSeconds(86400 * 1),
            exercises = listOf(createExerciseWithSets(sets = listOf(createWorkoutSet(1, 100.0, 9))))
        )

        val scrambledHistory = listOf(w2, w3, w1)

        val status = deloadEngine.evaluateDeloadNeed(
            workoutHistory = scrambledHistory,
            readinessScores = listOf(70, 75),
            currentWeekInBlock = 2
        )
        assertTrue("Chronologically sorted workouts should trigger stagnation plateau", status.isDeloadRecommended)
        assertEquals(DeloadReason.STAGNATION_PLATEAU, status.reason)
    }

    @Test
    fun `applyDeloadToWorkout handles custom 2_5kg increments and zero load bodyweight exercises`() {
        val originalWorkout = createWorkoutWithDetails(
            exercises = listOf(
                createExerciseWithSets(
                    exerciseId = 1,
                    name = "Barbell Squat",
                    sets = listOf(
                        createWorkoutSet(1, 82.5, 8),
                        createWorkoutSet(2, 82.5, 8)
                    )
                ),
                createExerciseWithSets(
                    exerciseId = 2,
                    name = "Pull-up",
                    sets = listOf(
                        createWorkoutSet(1, 0.0, 12),
                        createWorkoutSet(2, 0.0, 10)
                    )
                )
            )
        )

        val deloaded = deloadEngine.applyDeloadToWorkout(originalWorkout, weightIncrement = 2.5)

        assertEquals(1, deloaded.exercises[0].sets.size)
        assertEquals(75.0, deloaded.exercises[0].sets[0].weight, 0.001)

        assertEquals(1, deloaded.exercises[1].sets.size)
        assertEquals(0.0, deloaded.exercises[1].sets[0].weight, 0.001)
    }

    @Test
    fun `applyDeloadToWorkout scales set counts systematically across 0 to 6 sets and re-indexes set numbers`() {
        for (originalCount in 0..6) {
            val sets = (1..originalCount).map { setNum ->
                createWorkoutSet(setNumber = setNum, weight = 100.0, reps = 10)
            }
            val workout = createWorkoutWithDetails(
                exercises = listOf(
                    createExerciseWithSets(exerciseId = 1L, name = "Squat", sets = sets)
                )
            )
            val deloaded = deloadEngine.applyDeloadToWorkout(workout)
            val scaledSets = deloaded.exercises[0].sets

            val expectedCount = when (originalCount) {
                0 -> 0
                1 -> 1
                2 -> 1
                3 -> 2
                4 -> 2
                5 -> 3
                6 -> 3
                else -> originalCount / 2
            }
            assertEquals("Scaled set count for $originalCount original sets", expectedCount, scaledSets.size)

            // Verify sequential 1..N set re-indexing
            scaledSets.forEachIndexed { idx, set ->
                assertEquals("Set number should be 1-indexed and sequential", idx + 1, set.setNumber)
                assertEquals(90.0, set.weight, 0.001) // 100.0 * 0.90
            }
        }
    }

    @Test
    fun `roundToNearestIncrement handles custom increments and boundary inputs`() {
        // 0.5kg increment (default)
        assertEquals(91.0, deloadEngine.roundToNearestIncrement(91.2, 0.5), 0.001)
        assertEquals(91.5, deloadEngine.roundToNearestIncrement(91.3, 0.5), 0.001)

        // 1.0kg increment
        assertEquals(74.0, deloadEngine.roundToNearestIncrement(74.4, 1.0), 0.001)
        assertEquals(75.0, deloadEngine.roundToNearestIncrement(74.6, 1.0), 0.001)

        // 2.5kg increment
        assertEquals(80.0, deloadEngine.roundToNearestIncrement(81.2, 2.5), 0.001)
        assertEquals(82.5, deloadEngine.roundToNearestIncrement(81.3, 2.5), 0.001)

        // 5.0kg increment
        assertEquals(100.0, deloadEngine.roundToNearestIncrement(102.4, 5.0), 0.001)
        assertEquals(105.0, deloadEngine.roundToNearestIncrement(102.6, 5.0), 0.001)

        // Invalid increment or weight (<= 0.0) returns unchanged weight
        assertEquals(53.25, deloadEngine.roundToNearestIncrement(53.25, 0.0), 0.001)
        assertEquals(53.25, deloadEngine.roundToNearestIncrement(53.25, -1.0), 0.001)
        assertEquals(0.0, deloadEngine.roundToNearestIncrement(0.0, 2.5), 0.001)
        assertEquals(-10.0, deloadEngine.roundToNearestIncrement(-10.0, 2.5), 0.001)
    }

    @Test
    fun `stagnation plateau evaluation accurately distinguishes plateau from progress and handles small histories`() {
        val baseTime = Instant.now()

        fun makeSession(daysAgo: Long, totalVolumeWeight: Double): WorkoutWithDetails {
            return createWorkoutWithDetails(
                workoutId = daysAgo,
                date = baseTime.minusSeconds(daysAgo * 86400),
                exercises = listOf(
                    createExerciseWithSets(
                        sets = listOf(
                            createWorkoutSet(setNumber = 1, weight = totalVolumeWeight, reps = 1)
                        )
                    )
                )
            )
        }

        // Case 1: Less than 3 workouts -> No plateau
        val shortHistory = listOf(makeSession(2, 1000.0), makeSession(1, 1000.0))
        val statusShort = deloadEngine.evaluateDeloadNeed(workoutHistory = shortHistory, currentWeekInBlock = 1)
        assertFalse(statusShort.isDeloadRecommended)

        // Case 2: Progressing volume (1000 -> 1100 -> 1200) -> No plateau
        val progressingHistory = listOf(
            makeSession(3, 1000.0),
            makeSession(2, 1100.0),
            makeSession(1, 1200.0)
        )
        val statusProgressing = deloadEngine.evaluateDeloadNeed(workoutHistory = progressingHistory, currentWeekInBlock = 1)
        assertFalse(statusProgressing.isDeloadRecommended)

        // Case 3: Fluctuating volume (1000 -> 1200 -> 1100) -> No plateau (1200 was a peak)
        val fluctuatingHistory = listOf(
            makeSession(3, 1000.0),
            makeSession(2, 1200.0),
            makeSession(1, 1100.0)
        )
        val statusFluctuating = deloadEngine.evaluateDeloadNeed(workoutHistory = fluctuatingHistory, currentWeekInBlock = 1)
        assertFalse(statusFluctuating.isDeloadRecommended)

        // Case 4: Equal plateau across 3 sessions (1000 -> 1000 -> 1000) -> Triggers Plateau
        val plateauHistory = listOf(
            makeSession(3, 1000.0),
            makeSession(2, 1000.0),
            makeSession(1, 1000.0)
        )
        val statusPlateau = deloadEngine.evaluateDeloadNeed(workoutHistory = plateauHistory, currentWeekInBlock = 1)
        assertTrue(statusPlateau.isDeloadRecommended)
        assertEquals(DeloadReason.STAGNATION_PLATEAU, statusPlateau.reason)

        // Case 5: Regressing volume across 3 sessions (1200 -> 1100 -> 1000) -> Triggers Plateau
        val regressingHistory = listOf(
            makeSession(3, 1200.0),
            makeSession(2, 1100.0),
            makeSession(1, 1000.0)
        )
        val statusRegressing = deloadEngine.evaluateDeloadNeed(workoutHistory = regressingHistory, currentWeekInBlock = 1)
        assertTrue(statusRegressing.isDeloadRecommended)
        assertEquals(DeloadReason.STAGNATION_PLATEAU, statusRegressing.reason)
    }

    @Test
    fun `isLowReadinessSustained boundary transitions around threshold 55`() {
        // Threshold exactly 55: not below threshold -> false
        val exactly55 = deloadEngine.evaluateDeloadNeed(
            workoutHistory = emptyList(),
            readinessScores = listOf(55, 55, 55),
            currentWeekInBlock = 1
        )
        assertFalse("Readiness at exactly 55 does not trigger deload", exactly55.isDeloadRecommended)

        // Just below threshold (54): triggers fatigue accumulation deload
        val below55 = deloadEngine.evaluateDeloadNeed(
            workoutHistory = emptyList(),
            readinessScores = listOf(54, 54, 54),
            currentWeekInBlock = 1
        )
        assertTrue("Readiness at 54 triggers deload", below55.isDeloadRecommended)
        assertEquals(DeloadReason.FATIGUE_ACCUMULATION, below55.reason)

        // Trailing 3 average below 55 (historical high, recent fatigue)
        val trailingFatigue = deloadEngine.evaluateDeloadNeed(
            workoutHistory = emptyList(),
            readinessScores = listOf(90, 85, 50, 52, 53),
            currentWeekInBlock = 1
        )
        assertTrue("Trailing fatigue below 55 triggers deload", trailingFatigue.isDeloadRecommended)
        assertEquals(DeloadReason.FATIGUE_ACCUMULATION, trailingFatigue.reason)

        // Trailing 3 recovered (historical fatigue, recent recovery)
        val trailingRecovered = deloadEngine.evaluateDeloadNeed(
            workoutHistory = emptyList(),
            readinessScores = listOf(40, 45, 60, 65, 70),
            currentWeekInBlock = 1
        )
        assertFalse("Trailing recovery prevents deload", trailingRecovered.isDeloadRecommended)
    }

    @Test
    fun `stagnation plateau volume calculation filters out warmup and incomplete sets`() {
        val baseTime = Instant.now()
        // Workout 1: 1 normal completed set: 100kg x 10 = 1000kg
        val w1 = createWorkoutWithDetails(
            workoutId = 1,
            date = baseTime.minusSeconds(86400 * 3),
            exercises = listOf(
                createExerciseWithSets(
                    sets = listOf(
                        createWorkoutSet(1, 100.0, 10, completed = true, setType = SetType.NORMAL)
                    )
                )
            )
        )
        // Workout 2: 1 warmup set (80kg x 10) + 1 normal completed set (100kg x 10) = 1000kg
        val w2 = createWorkoutWithDetails(
            workoutId = 2,
            date = baseTime.minusSeconds(86400 * 2),
            exercises = listOf(
                createExerciseWithSets(
                    sets = listOf(
                        createWorkoutSet(1, 80.0, 10, completed = true, setType = SetType.WARMUP),
                        createWorkoutSet(2, 100.0, 10, completed = true, setType = SetType.NORMAL)
                    )
                )
            )
        )
        // Workout 3: 1 incomplete normal set (100kg x 10) + 1 normal completed set (100kg x 9) = 900kg
        val w3 = createWorkoutWithDetails(
            workoutId = 3,
            date = baseTime.minusSeconds(86400 * 1),
            exercises = listOf(
                createExerciseWithSets(
                    sets = listOf(
                        createWorkoutSet(1, 100.0, 10, completed = false, setType = SetType.NORMAL),
                        createWorkoutSet(2, 100.0, 9, completed = true, setType = SetType.NORMAL)
                    )
                )
            )
        )

        val status = deloadEngine.evaluateDeloadNeed(
            workoutHistory = listOf(w1, w2, w3),
            currentWeekInBlock = 2
        )

        // Volumes: 1000 -> 1000 -> 900 => triggers STAGNATION_PLATEAU
        assertTrue(status.isDeloadRecommended)
        assertEquals(DeloadReason.STAGNATION_PLATEAU, status.reason)
    }

    @Test
    fun `zero volume bodyweight workouts safely suppress stagnation plateau trigger`() {
        val baseTime = Instant.now()
        // 3 consecutive workouts with only bodyweight exercises (weight = 0.0 -> volume = 0.0)
        val w1 = createWorkoutWithDetails(
            workoutId = 1,
            date = baseTime.minusSeconds(86400 * 3),
            exercises = listOf(createExerciseWithSets(sets = listOf(createWorkoutSet(1, 0.0, 10))))
        )
        val w2 = createWorkoutWithDetails(
            workoutId = 2,
            date = baseTime.minusSeconds(86400 * 2),
            exercises = listOf(createExerciseWithSets(sets = listOf(createWorkoutSet(1, 0.0, 10))))
        )
        val w3 = createWorkoutWithDetails(
            workoutId = 3,
            date = baseTime.minusSeconds(86400 * 1),
            exercises = listOf(createExerciseWithSets(sets = listOf(createWorkoutSet(1, 0.0, 10))))
        )

        val status = deloadEngine.evaluateDeloadNeed(
            workoutHistory = listOf(w1, w2, w3),
            currentWeekInBlock = 2
        )

        // volumes.all { it > 0.0 } is FALSE -> plateau is NOT triggered
        assertFalse(status.isDeloadRecommended)
        assertNull(status.reason)
    }

    @Test
    fun `applyDeloadToWorkout preserves non-weight set metadata across scaled sets`() {
        val originalSets = listOf(
            WorkoutSet(
                id = 10L, workoutExerciseId = 1L, setNumber = 1,
                weight = 120.0, reps = 6, rpe = 9.5, restSeconds = 180,
                completed = true, setType = SetType.NORMAL
            ),
            WorkoutSet(
                id = 11L, workoutExerciseId = 1L, setNumber = 2,
                weight = 120.0, reps = 6, rpe = 9.0, restSeconds = 180,
                completed = true, setType = SetType.NORMAL
            )
        )
        val workout = createWorkoutWithDetails(
            exercises = listOf(createExerciseWithSets(exerciseId = 101L, sets = originalSets))
        )

        val deloaded = deloadEngine.applyDeloadToWorkout(workout, weightIncrement = 2.5)
        assertEquals(1, deloaded.exercises[0].sets.size)
        val set = deloaded.exercises[0].sets[0]

        // Scaled weight: 120 * 0.9 = 108.0 -> rounded to nearest 2.5 is 107.5
        assertEquals(107.5, set.weight, 0.001)
        assertEquals(1, set.setNumber)
        // Metadata preserved
        assertEquals(6, set.reps)
        assertEquals(9.5, set.rpe, 0.001)
        assertEquals(180, set.restSeconds)
        assertTrue(set.completed)
        assertEquals(SetType.NORMAL, set.setType)
    }

    @Test
    fun `fatigue accumulation coaching advice includes exact integer truncated recent readiness average`() {
        // Average of recent 3: (50 + 52 + 55) / 3 = 157 / 3 = 52.333 -> 52
        val scores = listOf(80, 50, 52, 55)
        val status = deloadEngine.evaluateDeloadNeed(
            workoutHistory = emptyList(),
            readinessScores = scores,
            currentWeekInBlock = 2
        )
        assertTrue(status.isDeloadRecommended)
        assertEquals(DeloadReason.FATIGUE_ACCUMULATION, status.reason)
        assertTrue(status.coachingAdvice.contains("recent average: 52/100"))

        // Single score: 49
        val singleStatus = deloadEngine.evaluateDeloadNeed(
            workoutHistory = emptyList(),
            readinessScores = listOf(49),
            currentWeekInBlock = 1
        )
        assertTrue(singleStatus.coachingAdvice.contains("recent average: 49/100"))
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
