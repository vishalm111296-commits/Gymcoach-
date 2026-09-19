package com.gymcoach.app.core.share

import com.gymcoach.app.core.progression.PRDetector
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.model.SetType
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutExercise
import com.gymcoach.app.domain.model.WorkoutExerciseWithSets
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutWithDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class WorkoutShareCardBuilderTest {

    private lateinit var builder: WorkoutShareCardBuilder
    private val now: Instant = Instant.parse("2026-09-19T10:00:00Z")

    @Before
    fun setUp() {
        builder = WorkoutShareCardBuilder()
    }

    private fun createExercise(id: Long, name: String, muscleGroup: String): Exercise {
        return Exercise(
            id = id,
            name = name,
            description = "Test exercise",
            muscleGroup = muscleGroup,
            equipment = "Barbell",
            difficulty = "Intermediate"
        )
    }

    private fun createWorkoutWithDetails(
        id: Long = 1L,
        duration: Long = 3600L,
        notes: String = "Chest & Back Attack",
        exercises: List<WorkoutExerciseWithSets>
    ): WorkoutWithDetails {
        val workout = Workout(
            id = id,
            date = now,
            startTime = now,
            endTime = now.plusSeconds(duration),
            duration = duration,
            notes = notes,
            completed = true
        )
        return WorkoutWithDetails(workout = workout, exercises = exercises)
    }

    @Test
    fun `buildShareData with standard workout calculates total sets, reps, volume, and top muscles`() {
        val bench = createExercise(1L, "Barbell Bench Press", "Chest")
        val benchSets = listOf(
            WorkoutSet(id = 1, workoutExerciseId = 1, setNumber = 1, weight = 80.0, reps = 10, rpe = 7.0, restSeconds = 90, completed = true, setType = SetType.NORMAL),
            WorkoutSet(id = 2, workoutExerciseId = 1, setNumber = 2, weight = 90.0, reps = 8, rpe = 8.0, restSeconds = 90, completed = true, setType = SetType.NORMAL),
            WorkoutSet(id = 3, workoutExerciseId = 1, setNumber = 3, weight = 100.0, reps = 5, rpe = 9.0, restSeconds = 120, completed = true, setType = SetType.NORMAL)
        )

        val row = createExercise(2L, "Barbell Row", "Back")
        val rowSets = listOf(
            WorkoutSet(id = 4, workoutExerciseId = 2, setNumber = 1, weight = 70.0, reps = 10, rpe = 7.5, restSeconds = 90, completed = true, setType = SetType.NORMAL),
            WorkoutSet(id = 5, workoutExerciseId = 2, setNumber = 2, weight = 80.0, reps = 8, rpe = 8.5, restSeconds = 90, completed = true, setType = SetType.NORMAL)
        )

        val ohp = createExercise(3L, "Overhead Press", "Shoulders")
        val ohpSets = listOf(
            WorkoutSet(id = 6, workoutExerciseId = 3, setNumber = 1, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = SetType.NORMAL)
        )

        val exercises = listOf(
            WorkoutExerciseWithSets(WorkoutExercise(id = 1, workoutId = 1, exerciseId = 1, orderIndex = 0), bench, benchSets),
            WorkoutExerciseWithSets(WorkoutExercise(id = 2, workoutId = 1, exerciseId = 2, orderIndex = 1), row, rowSets),
            WorkoutExerciseWithSets(WorkoutExercise(id = 3, workoutId = 1, exerciseId = 3, orderIndex = 2), ohp, ohpSets)
        )

        val workoutWithDetails = createWorkoutWithDetails(exercises = exercises)

        val cardData = builder.buildShareData(workoutWithDetails)

        // Verifications
        assertEquals("Chest & Back Attack", cardData.workoutTitle)
        assertEquals(6, cardData.totalSets)
        // Reps: 10 + 8 + 5 + 10 + 8 + 10 = 51
        assertEquals(51, cardData.totalReps)
        // Volume: (80*10) + (90*8) + (100*5) + (70*10) + (80*8) + (50*10) = 800 + 720 + 500 + 700 + 640 + 500 = 3860.0
        assertEquals(3860.0, cardData.totalVolumeKg, 0.001)

        // Top muscles ranking: Chest (3 sets), Back (2 sets), Shoulders (1 set)
        assertEquals(listOf("Chest", "Back", "Shoulders"), cardData.topMuscles)

        // Exercise summary details
        assertEquals(3, cardData.exercises.size)
        val benchSummary = cardData.exercises[0]
        assertEquals("Barbell Bench Press", benchSummary.exerciseName)
        assertEquals("100.0 kg × 5 reps", benchSummary.bestSetSummary)
        assertEquals(3, benchSummary.totalSetsCount)
        assertFalse(benchSummary.isPr)
    }

    @Test
    fun `PR detection flags isPr true for matching records`() {
        val bench = createExercise(1L, "Barbell Bench Press", "Chest")
        val benchSets = listOf(
            WorkoutSet(id = 1, workoutExerciseId = 1, setNumber = 1, weight = 120.0, reps = 5, rpe = 9.5, restSeconds = 120, completed = true, setType = SetType.NORMAL)
        )

        val squat = createExercise(2L, "Barbell Squat", "Legs")
        val squatSets = listOf(
            WorkoutSet(id = 2, workoutExerciseId = 2, setNumber = 1, weight = 140.0, reps = 5, rpe = 9.0, restSeconds = 120, completed = true, setType = SetType.NORMAL)
        )

        val exercises = listOf(
            WorkoutExerciseWithSets(WorkoutExercise(id = 1, workoutId = 10L, exerciseId = 1L, orderIndex = 0), bench, benchSets),
            WorkoutExerciseWithSets(WorkoutExercise(id = 2, workoutId = 10L, exerciseId = 2L, orderIndex = 1), squat, squatSets)
        )

        val workoutWithDetails = createWorkoutWithDetails(id = 10L, exercises = exercises)

        val matchingPr = PRDetector.PersonalRecord(
            exerciseId = 1L,
            exerciseName = "Barbell Bench Press",
            type = PRDetector.PRType.WEIGHT,
            value = 120.0,
            details = "120.0kg",
            date = now,
            workoutId = 10L
        )

        // PR for an exercise NOT in the workout
        val unrelatedPr = PRDetector.PersonalRecord(
            exerciseId = 99L,
            exerciseName = "Deadlift",
            type = PRDetector.PRType.WEIGHT,
            value = 200.0,
            details = "200.0kg",
            date = now,
            workoutId = 10L
        )

        // PR with non-matching date (e.g. 7 days prior)
        val oldPr = PRDetector.PersonalRecord(
            exerciseId = 2L,
            exerciseName = "Barbell Squat",
            type = PRDetector.PRType.WEIGHT,
            value = 135.0,
            details = "135.0kg",
            date = now.minus(7, ChronoUnit.DAYS),
            workoutId = 5L
        )

        val cardData = builder.buildShareData(
            workout = workoutWithDetails,
            personalRecords = listOf(matchingPr, unrelatedPr, oldPr)
        )

        assertEquals(1, cardData.prCount)
        val benchSummary = cardData.exercises.first { it.exerciseName == "Barbell Bench Press" }
        val squatSummary = cardData.exercises.first { it.exerciseName == "Barbell Squat" }

        assertTrue("Bench Press should be flagged as PR", benchSummary.isPr)
        assertFalse("Squat should NOT be flagged as PR due to date mismatch", squatSummary.isPr)
        assertEquals("Personal Records Broken", cardData.motivationalQuote)
    }

    @Test
    fun `empty workout edge case handles 0 sets and 0 reps gracefully`() {
        val emptyWorkout = createWorkoutWithDetails(
            id = 99L,
            duration = 0L,
            notes = "",
            exercises = emptyList()
        )

        val cardData = builder.buildShareData(emptyWorkout)

        assertEquals("Workout", cardData.workoutTitle)
        assertEquals(0, cardData.totalSets)
        assertEquals(0, cardData.totalReps)
        assertEquals(0.0, cardData.totalVolumeKg, 0.001)
        assertEquals(0, cardData.prCount)
        assertTrue(cardData.topMuscles.isEmpty())
        assertTrue(cardData.exercises.isEmpty())
        assertEquals("0m", cardData.durationFormatted)
        assertEquals("Stay Consistent, Stay Strong", cardData.motivationalQuote)
    }

    @Test
    fun `buildFormattedShareText contains volume, duration, exercise names, and GymCoach footer`() {
        val deadlift = createExercise(1L, "Conventional Deadlift", "Back")
        val deadliftSets = listOf(
            WorkoutSet(id = 1, workoutExerciseId = 1, setNumber = 1, weight = 150.0, reps = 5, rpe = 8.5, restSeconds = 120, completed = true, setType = SetType.NORMAL),
            WorkoutSet(id = 2, workoutExerciseId = 1, setNumber = 2, weight = 160.0, reps = 3, rpe = 9.0, restSeconds = 150, completed = true, setType = SetType.NORMAL)
        )
        val exercises = listOf(
            WorkoutExerciseWithSets(WorkoutExercise(id = 1, workoutId = 1, exerciseId = 1, orderIndex = 0), deadlift, deadliftSets)
        )

        val workoutWithDetails = createWorkoutWithDetails(
            duration = 45 * 60L,
            notes = "Heavy Pull Session",
            exercises = exercises
        )

        val cardData = builder.buildShareData(workoutWithDetails)
        val shareText = builder.buildFormattedShareText(cardData)

        // Verifications required by spec:
        // 1. Contains volume
        assertTrue("Share text must contain volume", shareText.contains("1,230.0 kg") || shareText.contains("1230"))
        // 2. Contains duration
        assertTrue("Share text must contain duration", shareText.contains("45m"))
        // 3. Contains exercise names
        assertTrue("Share text must contain exercise names", shareText.contains("Conventional Deadlift"))
        // 4. Contains GymCoach footer
        assertTrue("Share text must contain GymCoach footer", shareText.contains("GymCoach"))
    }

    @Test
    fun `motivational quote selects Titan Volume Unlocked when volume exceeds 10000kg`() {
        val squat = createExercise(1L, "Back Squat", "Legs")
        val heavySets = (1..10).map { i ->
            WorkoutSet(id = i.toLong(), workoutExerciseId = 1, setNumber = i, weight = 120.0, reps = 10, rpe = 8.5, restSeconds = 90, completed = true)
        } // 10 * 120 * 10 = 12,000 kg

        val workout = createWorkoutWithDetails(
            exercises = listOf(
                WorkoutExerciseWithSets(WorkoutExercise(id = 1, workoutId = 1, exerciseId = 1, orderIndex = 0), squat, heavySets)
            )
        )

        val cardData = builder.buildShareData(workout)

        assertEquals(12000.0, cardData.totalVolumeKg, 0.001)
        assertEquals("Titan Volume Unlocked", cardData.motivationalQuote)
    }

    @Test
    fun `uncompleted sets are ignored from volume and sets calculations`() {
        val bench = createExercise(1L, "Bench Press", "Chest")
        val sets = listOf(
            WorkoutSet(id = 1, workoutExerciseId = 1, setNumber = 1, weight = 100.0, reps = 5, rpe = 8.0, restSeconds = 90, completed = true),
            WorkoutSet(id = 2, workoutExerciseId = 1, setNumber = 2, weight = 100.0, reps = 5, rpe = 8.0, restSeconds = 90, completed = false)
        )

        val workout = createWorkoutWithDetails(
            exercises = listOf(
                WorkoutExerciseWithSets(WorkoutExercise(id = 1, workoutId = 1, exerciseId = 1, orderIndex = 0), bench, sets)
            )
        )

        val cardData = builder.buildShareData(workout)

        assertEquals(1, cardData.totalSets)
        assertEquals(5, cardData.totalReps)
        assertEquals(500.0, cardData.totalVolumeKg, 0.001)
        assertEquals(1, cardData.exercises[0].totalSetsCount)
    }
}
