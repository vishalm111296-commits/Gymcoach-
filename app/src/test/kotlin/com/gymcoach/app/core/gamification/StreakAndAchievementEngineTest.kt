package com.gymcoach.app.core.gamification

import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import com.gymcoach.app.domain.model.WorkoutWithStats
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class StreakAndAchievementEngineTest {

    private lateinit var engine: StreakAndAchievementEngine
    private val zoneId = ZoneId.of("UTC")
    private val now = Instant.parse("2024-01-08T12:00:00Z") // A fixed Monday
    private val clock = Clock.fixed(now, zoneId)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(zoneId)

    @Before
    fun setup() {
        engine = StreakAndAchievementEngine(clock)
    }

    private fun createWorkout(dateMs: Long, isCompleted: Boolean = true, notes: String = ""): WorkoutWithStats {
        return WorkoutWithStats(
            id = 1L,
            date = Instant.ofEpochMilli(dateMs),
            startTime = Instant.ofEpochMilli(dateMs),
            endTime = Instant.ofEpochMilli(dateMs + 3600000),
            duration = 3600000,
            notes = notes,
            completed = isCompleted,
            volume = 1000.0,
            setCount = 10,
            repCount = 100,
            exerciseCount = 5
        )
    }

    private fun createSet(weight: Double, reps: Int, isCompleted: Boolean = true, setType: Int = 0): VolumeCalculator.SetWithContext {
        return VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(
                id = 1L,
                workoutExerciseId = 1L,
                setNumber = 1,
                weight = weight,
                reps = reps,
                rpe = 8.0,
                restSeconds = 60,
                completed = isCompleted,
                setType = setType
            ),
            exerciseId = 1L,
            workoutDate = now.toEpochMilli()
        )
    }

    @Test
    fun testStreakCalculation_continuousWeeks() {
        // Create 4 workouts over 4 consecutive weeks (1 per week)
        // With weeklyTarget=1 for testing
        val workouts = mutableListOf<WorkoutWithStats>()
        for (i in 0 until 4) {
            val date = now.minus(i * 7L, ChronoUnit.DAYS)
            workouts.add(createWorkout(date.toEpochMilli()))
        }

        val report = engine.calculateReport(workouts, emptyList(), weeklyTarget = 1, zoneId = zoneId)

        assertEquals("Current streak should be 4", 4, report.currentWeeklyStreak)
        assertEquals("Longest streak should be 4", 4, report.longestWeeklyStreak)
    }

    @Test
    fun testStreakCalculation_brokenStreak() {
        // Create 3 workouts over 3 weeks, gap of 2 weeks, then 1 workout this week
        val workouts = mutableListOf<WorkoutWithStats>()
        workouts.add(createWorkout(now.toEpochMilli())) // This week
        workouts.add(createWorkout(now.minus(21L, ChronoUnit.DAYS).toEpochMilli()))
        workouts.add(createWorkout(now.minus(28L, ChronoUnit.DAYS).toEpochMilli()))
        workouts.add(createWorkout(now.minus(35L, ChronoUnit.DAYS).toEpochMilli()))

        val report = engine.calculateReport(workouts, emptyList(), weeklyTarget = 1, zoneId = zoneId)

        assertEquals("Current streak should be 1", 1, report.currentWeeklyStreak)
        assertEquals("Longest streak should be 3", 3, report.longestWeeklyStreak)
    }

    @Test
    fun testStreakCalculation_currentWeekNotMetButStreakAlive() {
        // 2 workouts in prev week, 0 in current week. Target is 2.
        val workouts = mutableListOf<WorkoutWithStats>()
        val prevWeek = now.minus(7L, ChronoUnit.DAYS)
        workouts.add(createWorkout(prevWeek.toEpochMilli()))
        workouts.add(createWorkout(prevWeek.plus(1L, ChronoUnit.DAYS).toEpochMilli()))

        val report = engine.calculateReport(workouts, emptyList(), weeklyTarget = 2, zoneId = zoneId)

        assertEquals("Current streak should be 1", 1, report.currentWeeklyStreak)
    }

    @Test
    fun testStreakCalculation_currentWeekNotMetStreakBroken() {
        // 1 workout in prev week, 0 in current week. Target is 2.
        val workouts = mutableListOf<WorkoutWithStats>()
        val prevWeek = now.minus(7L, ChronoUnit.DAYS)
        workouts.add(createWorkout(prevWeek.toEpochMilli()))

        val report = engine.calculateReport(workouts, emptyList(), weeklyTarget = 2, zoneId = zoneId)

        assertEquals("Current streak should be 0", 0, report.currentWeeklyStreak)
    }

    @Test
    fun testXpAndLevelCalculation() {
        val workouts = listOf(createWorkout(now.toEpochMilli()), createWorkout(now.toEpochMilli()))
        // 2 workouts * 100 XP = 200 XP

        val sets = listOf(
            createSet(weight = 100.0, reps = 10, setType = 0), // Normal set, completed = +10 XP
            createSet(weight = 100.0, reps = 10, setType = 1), // Warmup set = 0 XP
            createSet(weight = 100.0, reps = 10, isCompleted = false) // Not completed = 0 XP
        )
        // Total XP = 200 + 10 = 210

        val report = engine.calculateReport(workouts, sets, weeklyTarget = 1, zoneId = zoneId)

        assertEquals("Total sets (normal completed) should be 1", 1, report.totalSets)
        assertEquals("XP should be 210", 210, report.currentXp)
        assertEquals("Level should be 1", 1, report.athleteLevel)
    }

    @Test
    fun testBadgeUnlocks() {
        // 1 deload workout, heavy volume
        val workouts = listOf(createWorkout(now.toEpochMilli(), notes = "Taking a deload week"))

        // 100+ sets, 100,000+ volume
        val sets = mutableListOf<VolumeCalculator.SetWithContext>()
        for (i in 0 until 100) {
            sets.add(createSet(weight = 100.0, reps = 10)) // 1000 volume per set
        }

        val report = engine.calculateReport(workouts, sets, weeklyTarget = 1, zoneId = zoneId)

        val firstRep = report.badges.find { it.id == "first_rep" }
        assertTrue("First rep should be unlocked", firstRep?.isUnlocked == true)

        val centurion = report.badges.find { it.id == "centurion" }
        assertTrue("Centurion should be unlocked", centurion?.isUnlocked == true)

        val volume100k = report.badges.find { it.id == "volume_100k" }
        assertTrue("100k volume should be unlocked", volume100k?.isUnlocked == true)

        val deload = report.badges.find { it.id == "deload_disciple" }
        assertTrue("Deload disciple should be unlocked", deload?.isUnlocked == true)

        val ironConsistency = report.badges.find { it.id == "streak_4w" }
        assertFalse("Iron consistency should NOT be unlocked", ironConsistency?.isUnlocked == true)
    }

    @Test
    fun testHeatmapData() {
        val workouts = listOf(
            createWorkout(now.toEpochMilli()),
            createWorkout(now.toEpochMilli()), // 2 workouts today
            createWorkout(now.minus(1L, ChronoUnit.DAYS).toEpochMilli()) // 1 workout yesterday
        )

        val report = engine.calculateReport(workouts, emptyList(), weeklyTarget = 1, zoneId = zoneId)

        val todayStr = dateFormatter.format(now)
        val yesterdayStr = dateFormatter.format(now.minus(1L, ChronoUnit.DAYS))

        assertEquals("Heatmap should have 366 entries (leap year possible, but generally 366 including today)", 366, report.heatMapData.size)
        assertEquals("Today should have 2 workouts", 2, report.heatMapData[todayStr])
        assertEquals("Yesterday should have 1 workout", 1, report.heatMapData[yesterdayStr])

        val twoDaysAgoStr = dateFormatter.format(now.minus(2L, ChronoUnit.DAYS))
        assertEquals("Two days ago should have 0 workouts", 0, report.heatMapData[twoDaysAgoStr])
    }
}
