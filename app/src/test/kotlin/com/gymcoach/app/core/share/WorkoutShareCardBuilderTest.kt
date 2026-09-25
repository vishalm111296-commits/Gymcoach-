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

    @Test
    fun `empty workout without exercises returns safe default share card data`() {
        val emptyWorkout = createWorkoutWithDetails(
            id = 10L,
            duration = 0L,
            notes = "",
            exercises = emptyList()
        )

        val cardData = builder.buildShareData(emptyWorkout)
        assertEquals("Workout", cardData.workoutTitle)
        assertEquals(0, cardData.totalSets)
        assertEquals(0, cardData.totalReps)
        assertEquals(0.0, cardData.totalVolumeKg, 0.001)
        assertTrue(cardData.topMuscles.isEmpty())
        assertTrue(cardData.exercises.isEmpty())
        assertEquals("Stay Consistent, Stay Strong", cardData.motivationalQuote)

        val shareText = builder.buildFormattedShareText(cardData)
        assertTrue(shareText.contains("Workout"))
        assertTrue(shareText.contains("0.0 kg"))
        assertTrue(shareText.contains("⚡ Logged with GymCoach"))
    }

    @Test
    fun `duration formatting handles multi-hour and zero duration correctly`() {
        val bench = createExercise(1L, "Bench Press", "Chest")
        val set = listOf(WorkoutSet(id = 1, workoutExerciseId = 1, setNumber = 1, weight = 100.0, reps = 5, rpe = 8.0, restSeconds = 90, completed = true))
        val exercises = listOf(WorkoutExerciseWithSets(WorkoutExercise(id = 1, workoutId = 1, exerciseId = 1, orderIndex = 0), bench, set))

        // 2 hours 15 minutes = 8100 seconds
        val longWorkout = createWorkoutWithDetails(duration = 8100L, exercises = exercises)
        val longCard = builder.buildShareData(longWorkout)
        assertEquals("2h 15m", longCard.durationFormatted)

        // 0 duration
        val zeroWorkout = createWorkoutWithDetails(duration = 0L, exercises = exercises)
        val zeroCard = builder.buildShareData(zeroWorkout)
        assertEquals("0m", zeroCard.durationFormatted)
    }

    @Test
    fun `explicit workoutTitle overrides notes and blank falls back to notes or Workout`() {
        val bench = createExercise(1L, "Bench Press", "Chest")
        val set = listOf(WorkoutSet(id = 1, workoutExerciseId = 1, setNumber = 1, weight = 60.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true))
        val exercises = listOf(WorkoutExerciseWithSets(WorkoutExercise(id = 1, workoutId = 1, exerciseId = 1, orderIndex = 0), bench, set))

        val workout = createWorkoutWithDetails(notes = "Push Day A", exercises = exercises)

        // Custom override
        val customCard = builder.buildShareData(workout, workoutTitle = "Chest Hypertrophy Blast")
        assertEquals("Chest Hypertrophy Blast", customCard.workoutTitle)

        // Fallback to notes when null or blank
        val notesCard = builder.buildShareData(workout, workoutTitle = "   ")
        assertEquals("Push Day A", notesCard.workoutTitle)
    }

    @Test
    fun `selectMotivationalQuote strictly respects the priority hierarchy and exact boundary thresholds`() {
        val bench = createExercise(1L, "Bench Press", "Chest")

        // 1. volume > 10000.0 takes absolute precedence even with PRs and high sets
        val ultraHeavyWorkout = createWorkoutWithDetails(
            exercises = listOf(
                WorkoutExerciseWithSets(
                    WorkoutExercise(1, 1, 1, 0), bench,
                    listOf(WorkoutSet(1, 1, 1, weight = 1000.1, reps = 10, rpe = 10.0, restSeconds = 90, completed = true))
                )
            )
        )
        val ultraCard = builder.buildShareData(ultraHeavyWorkout, personalRecords = listOf(
            PRDetector.PersonalRecord(1L, "Bench Press", PRDetector.PRType.WEIGHT, 1000.0, "1000.0kg", now, 1L)
        ))
        assertEquals("Titan Volume Unlocked", ultraCard.motivationalQuote)

        // 2. Exactly 10000.0 kg (not > 10000.0) with PR -> "Personal Records Broken"
        val exact10kWithPr = createWorkoutWithDetails(
            exercises = listOf(
                WorkoutExerciseWithSets(
                    WorkoutExercise(1, 1, 1, 0), bench,
                    listOf(WorkoutSet(1, 1, 1, weight = 1000.0, reps = 10, rpe = 10.0, restSeconds = 90, completed = true))
                )
            )
        )
        val card10kPr = builder.buildShareData(exact10kWithPr, personalRecords = listOf(
            PRDetector.PersonalRecord(1L, "Bench Press", PRDetector.PRType.WEIGHT, 1000.0, "1000.0kg", now, 1L)
        ))
        assertEquals("Personal Records Broken", card10kPr.motivationalQuote)

        // 3. Exactly 10000.0 kg without PR -> "Heavyweight Champion" (since 10000.0 > 5000.0)
        val card10kNoPr = builder.buildShareData(exact10kWithPr, personalRecords = emptyList())
        assertEquals("Heavyweight Champion", card10kNoPr.motivationalQuote)

        // 4. Exactly 5000.0 kg (not > 5000.0) with 20 sets -> "Iron Will & Relentless Grind"
        val sets20 = (1..20).map {
            WorkoutSet(it.toLong(), 1, it, weight = 25.0, reps = 10, rpe = 7.0, restSeconds = 60, completed = true)
        }
        val workout20Sets = createWorkoutWithDetails(
            exercises = listOf(WorkoutExerciseWithSets(WorkoutExercise(1, 1, 1, 0), bench, sets20))
        )
        val card20Sets = builder.buildShareData(workout20Sets)
        assertEquals(5000.0, card20Sets.totalVolumeKg, 0.001)
        assertEquals("Iron Will & Relentless Grind", card20Sets.motivationalQuote)

        // 5. 19 sets with 4000.0 kg -> "Solid Work in the Iron Temple"
        val sets19 = (1..19).map {
            WorkoutSet(it.toLong(), 1, it, weight = 20.0, reps = 10, rpe = 7.0, restSeconds = 60, completed = true)
        }
        val workout19Sets = createWorkoutWithDetails(
            exercises = listOf(WorkoutExerciseWithSets(WorkoutExercise(1, 1, 1, 0), bench, sets19))
        )
        val card19Sets = builder.buildShareData(workout19Sets)
        assertEquals("Solid Work in the Iron Temple", card19Sets.motivationalQuote)

        // 6. 9 sets with 1000.0 kg -> "Every Rep Counts Towards Greatness"
        val sets9 = (1..9).map {
            WorkoutSet(it.toLong(), 1, it, weight = 10.0, reps = 10, rpe = 7.0, restSeconds = 60, completed = true)
        }
        val workout9Sets = createWorkoutWithDetails(
            exercises = listOf(WorkoutExerciseWithSets(WorkoutExercise(1, 1, 1, 0), bench, sets9))
        )
        val card9Sets = builder.buildShareData(workout9Sets)
        assertEquals("Every Rep Counts Towards Greatness", card9Sets.motivationalQuote)
    }

    @Test
    fun `bestSetSummary selects highest weight and breaks ties by volume load`() {
        val squat = createExercise(1L, "Squat", "Legs")

        // Tie-breaker: two sets at 100kg, one with 5 reps (500kg vol) and one with 8 reps (800kg vol)
        val setsWithTie = listOf(
            WorkoutSet(1, 1, 1, weight = 100.0, reps = 5, rpe = 8.0, restSeconds = 90, completed = true),
            WorkoutSet(2, 1, 2, weight = 100.0, reps = 8, rpe = 9.0, restSeconds = 90, completed = true)
        )
        val workoutTie = createWorkoutWithDetails(
            exercises = listOf(WorkoutExerciseWithSets(WorkoutExercise(1, 1, 1, 0), squat, setsWithTie))
        )
        val cardTie = builder.buildShareData(workoutTie)
        assertEquals("100.0 kg × 8 reps", cardTie.exercises[0].bestSetSummary)

        // Heavyweight priority: 110kg x 1 rep (110kg vol) beats 100kg x 10 reps (1000kg vol)
        val setsHeavy = listOf(
            WorkoutSet(1, 1, 1, weight = 100.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true),
            WorkoutSet(2, 1, 2, weight = 110.0, reps = 1, rpe = 9.5, restSeconds = 90, completed = true)
        )
        val workoutHeavy = createWorkoutWithDetails(
            exercises = listOf(WorkoutExerciseWithSets(WorkoutExercise(1, 1, 1, 0), squat, setsHeavy))
        )
        val cardHeavy = builder.buildShareData(workoutHeavy)
        assertEquals("110.0 kg × 1 reps", cardHeavy.exercises[0].bestSetSummary)
    }

    @Test
    fun `topMuscles is strictly truncated to top 3 even when 5 muscle groups are trained`() {
        val ex1 = createExercise(1, "Bench", "Chest")
        val ex2 = createExercise(2, "Row", "Back")
        val ex3 = createExercise(3, "Squat", "Quads")
        val ex4 = createExercise(4, "OHP", "Shoulders")
        val ex5 = createExercise(5, "Curl", "Biceps")

        fun makeSets(count: Int) = (1..count).map {
            WorkoutSet(it.toLong(), 1, it, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 60, completed = true)
        }

        val exercises = listOf(
            WorkoutExerciseWithSets(WorkoutExercise(1, 1, 1, 0), ex1, makeSets(5)), // Chest: 5
            WorkoutExerciseWithSets(WorkoutExercise(2, 1, 2, 1), ex2, makeSets(4)), // Back: 4
            WorkoutExerciseWithSets(WorkoutExercise(3, 1, 3, 2), ex3, makeSets(3)), // Quads: 3
            WorkoutExerciseWithSets(WorkoutExercise(4, 1, 4, 3), ex4, makeSets(2)), // Shoulders: 2
            WorkoutExerciseWithSets(WorkoutExercise(5, 1, 5, 4), ex5, makeSets(1))  // Biceps: 1
        )

        val workout = createWorkoutWithDetails(exercises = exercises)
        val card = builder.buildShareData(workout)

        assertEquals(3, card.topMuscles.size)
        assertEquals(listOf("Chest", "Back", "Quads"), card.topMuscles)
    }

    @Test
    fun `exercise summary falls back gracefully to planned sets when none are completed`() {
        val bench = createExercise(1L, "Bench Press", "Chest")
        val uncompletedSets = listOf(
            WorkoutSet(1, 1, 1, weight = 80.0, reps = 10, rpe = 7.0, restSeconds = 90, completed = false),
            WorkoutSet(2, 1, 2, weight = 90.0, reps = 8, rpe = 8.0, restSeconds = 90, completed = false)
        )
        val workout = createWorkoutWithDetails(
            exercises = listOf(WorkoutExerciseWithSets(WorkoutExercise(1, 1, 1, 0), bench, uncompletedSets))
        )

        val card = builder.buildShareData(workout)

        assertEquals(0, card.totalSets)
        assertEquals(0, card.totalReps)
        assertEquals(0.0, card.totalVolumeKg, 0.001)

        // Exercise summary reflects planned sets
        assertEquals(1, card.exercises.size)
        assertEquals(2, card.exercises[0].totalSetsCount)
        assertEquals("90.0 kg × 8 reps", card.exercises[0].bestSetSummary)
        assertFalse(card.exercises[0].isPr)
    }
}

