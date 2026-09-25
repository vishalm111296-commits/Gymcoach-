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

    @Test
    fun testLongStreakMilestonesUnlockIronConsistencyAndTitaniumHabit() {
        // Create 12 consecutive weekly workouts
        val workouts = mutableListOf<WorkoutWithStats>()
        for (i in 0 until 12) {
            val date = now.minus(i * 7L, ChronoUnit.DAYS)
            workouts.add(createWorkout(date.toEpochMilli()))
        }

        val report = engine.calculateReport(workouts, emptyList(), weeklyTarget = 1, zoneId = zoneId)
        assertEquals(12, report.currentWeeklyStreak)
        assertEquals(12, report.longestWeeklyStreak)

        val ironBadge = report.badges.find { it.id == "streak_4w" }
        assertNotNull(ironBadge)
        assertTrue(ironBadge!!.isUnlocked)
        assertEquals(4, ironBadge.currentProgress)

        val titaniumBadge = report.badges.find { it.id == "streak_12w" }
        assertNotNull(titaniumBadge)
        assertTrue(titaniumBadge!!.isUnlocked)
        assertEquals(12, titaniumBadge.currentProgress)
    }

    @Test
    fun testAthleteLevelProgressionAcrossMultipleLevels() {
        // 15 completed workouts = 1500 XP
        val workouts = mutableListOf<WorkoutWithStats>()
        for (i in 0 until 15) {
            workouts.add(createWorkout(now.minus(i.toLong(), ChronoUnit.DAYS).toEpochMilli()))
        }

        // 60 normal sets = 600 XP -> Total XP = 2100 -> Level 3 (2000 XP threshold), 100 XP into level
        val sets = mutableListOf<VolumeCalculator.SetWithContext>()
        for (i in 0 until 60) {
            sets.add(createSet(weight = 80.0, reps = 8, setType = 0))
        }

        val report = engine.calculateReport(workouts, sets, weeklyTarget = 4, zoneId = zoneId)
        assertEquals(15, report.totalWorkouts)
        assertEquals(60, report.totalSets)
        assertEquals(3, report.athleteLevel)
        assertEquals(100, report.currentXp)
        assertEquals(1000, report.xpForNextLevel)
    }

    @Test
    fun testUncompletedWorkoutsAreStrictlyExcluded() {
        // 5 uncompleted workouts
        val workouts = listOf(
            createWorkout(now.toEpochMilli(), isCompleted = false),
            createWorkout(now.minus(1L, ChronoUnit.DAYS).toEpochMilli(), isCompleted = false)
        )

        val report = engine.calculateReport(workouts, emptyList(), weeklyTarget = 1, zoneId = zoneId)
        assertEquals(0, report.totalWorkouts)
        assertEquals(0, report.currentWeeklyStreak)
        assertEquals(0, report.longestWeeklyStreak)
        assertEquals(1, report.athleteLevel)
        assertEquals(0, report.currentXp)

        val firstRep = report.badges.find { it.id == "first_rep" }
        assertFalse(firstRep?.isUnlocked == true)
        assertEquals(0, firstRep?.currentProgress)
    }

    @Test
    fun testEmptyWorkoutsAndSetsReturnCleanBaselineReport() {
        val report = engine.calculateReport(emptyList(), emptyList(), weeklyTarget = 3, zoneId = zoneId)
        assertEquals(0, report.totalWorkouts)
        assertEquals(0, report.totalSets)
        assertEquals(0.0, report.totalVolumeKg, 0.001)
        assertEquals(0, report.currentWeeklyStreak)
        assertEquals(0, report.longestWeeklyStreak)
        assertEquals(0, report.currentWeekWorkoutsCompleted)
        assertEquals(3, report.weeklyTarget)
        assertEquals(1, report.athleteLevel)
        assertEquals(0, report.currentXp)
        assertTrue(report.badges.none { it.isUnlocked })
    }

    @Test
    fun testVolumeMilestoneExactBoundaries() {
        // Test 99,999 kg (locked) vs 100,000 kg (unlocked)
        val sets99k = listOf(createSet(weight = 999.99, reps = 100)) // 99,999 kg
        val r99k = engine.calculateReport(listOf(createWorkout(now.toEpochMilli())), sets99k, weeklyTarget = 1, zoneId = zoneId)
        val badge99k = r99k.badges.find { it.id == "volume_100k" }
        assertNotNull(badge99k)
        assertFalse(badge99k!!.isUnlocked)
        assertEquals(99999, badge99k.currentProgress)

        val sets100k = listOf(createSet(weight = 1000.0, reps = 100)) // 100,000 kg
        val r100k = engine.calculateReport(listOf(createWorkout(now.toEpochMilli())), sets100k, weeklyTarget = 1, zoneId = zoneId)
        val badge100k = r100k.badges.find { it.id == "volume_100k" }
        assertNotNull(badge100k)
        assertTrue(badge100k!!.isUnlocked)
        assertEquals(100000, badge100k.currentProgress)
    }

    @Test
    fun testCenturionMilestoneExactBoundaries() {
        // 99 normal completed sets -> locked
        val sets99 = (1..99).map { createSet(weight = 50.0, reps = 10) }
        val r99 = engine.calculateReport(listOf(createWorkout(now.toEpochMilli())), sets99, weeklyTarget = 1, zoneId = zoneId)
        val badge99 = r99.badges.find { it.id == "centurion" }
        assertNotNull(badge99)
        assertFalse(badge99!!.isUnlocked)
        assertEquals(99, badge99.currentProgress)

        // 100 normal completed sets -> unlocked
        val sets100 = (1..100).map { createSet(weight = 50.0, reps = 10) }
        val r100 = engine.calculateReport(listOf(createWorkout(now.toEpochMilli())), sets100, weeklyTarget = 1, zoneId = zoneId)
        val badge100 = r100.badges.find { it.id == "centurion" }
        assertNotNull(badge100)
        assertTrue(badge100!!.isUnlocked)
        assertEquals(100, badge100.currentProgress)
    }

    @Test
    fun testXpLevelThresholdTransitions() {
        // Level 1: 0 - 999 XP
        // Level 2: 1000 - 1999 XP
        // 9 completed workouts = 900 XP + 9 normal sets = 990 XP
        val workouts9 = (1..9).map { createWorkout(now.minus(it.toLong(), ChronoUnit.DAYS).toEpochMilli()) }
        val sets9 = (1..9).map { createSet(50.0, 10) }
        val r990 = engine.calculateReport(workouts9, sets9, weeklyTarget = 1, zoneId = zoneId)
        assertEquals(1, r990.athleteLevel)
        assertEquals(990, r990.currentXp)
        assertEquals(1000, r990.xpForNextLevel)

        // Adding 1 set = +10 XP -> 1000 XP -> Level 2, 0 current XP
        val sets10 = (1..10).map { createSet(50.0, 10) }
        val r1000 = engine.calculateReport(workouts9, sets10, weeklyTarget = 1, zoneId = zoneId)
        assertEquals(2, r1000.athleteLevel)
        assertEquals(0, r1000.currentXp)
        assertEquals(1000, r1000.xpForNextLevel)
    }

    @Test
    fun testDeloadDiscipleCaseInsensitiveMatching() {
        val uppercaseWorkout = listOf(createWorkout(now.toEpochMilli(), notes = "LIGHT DELOAD SESSION"))
        val rUpper = engine.calculateReport(uppercaseWorkout, emptyList(), weeklyTarget = 1, zoneId = zoneId)
        assertTrue(rUpper.badges.find { it.id == "deload_disciple" }?.isUnlocked == true)

        val mixedcaseWorkout = listOf(createWorkout(now.toEpochMilli(), notes = "Planned DeLoad Week"))
        val rMixed = engine.calculateReport(mixedcaseWorkout, emptyList(), weeklyTarget = 1, zoneId = zoneId)
        assertTrue(rMixed.badges.find { it.id == "deload_disciple" }?.isUnlocked == true)
    }

    @Test
    fun testStreakBadgesExactThresholdBoundaries() {
        // 3 consecutive weeks -> streak_4w locked (progress 3/4)
        val workouts3w = (0..2).map { createWorkout(now.minus(it * 7L, ChronoUnit.DAYS).toEpochMilli()) }
        val r3w = engine.calculateReport(workouts3w, emptyList(), weeklyTarget = 1, zoneId = zoneId)
        val badge4wLocked = r3w.badges.find { it.id == "streak_4w" }
        assertNotNull(badge4wLocked)
        assertFalse(badge4wLocked!!.isUnlocked)
        assertEquals(3, badge4wLocked.currentProgress)

        // 4 consecutive weeks -> streak_4w unlocked
        val workouts4w = (0..3).map { createWorkout(now.minus(it * 7L, ChronoUnit.DAYS).toEpochMilli()) }
        val r4w = engine.calculateReport(workouts4w, emptyList(), weeklyTarget = 1, zoneId = zoneId)
        val badge4wUnlocked = r4w.badges.find { it.id == "streak_4w" }
        assertNotNull(badge4wUnlocked)
        assertTrue(badge4wUnlocked!!.isUnlocked)
        assertEquals(4, badge4wUnlocked.currentProgress)

        // 11 consecutive weeks -> streak_12w locked (progress 11/12)
        val workouts11w = (0..10).map { createWorkout(now.minus(it * 7L, ChronoUnit.DAYS).toEpochMilli()) }
        val r11w = engine.calculateReport(workouts11w, emptyList(), weeklyTarget = 1, zoneId = zoneId)
        val badge12wLocked = r11w.badges.find { it.id == "streak_12w" }
        assertNotNull(badge12wLocked)
        assertFalse(badge12wLocked!!.isUnlocked)
        assertEquals(11, badge12wLocked.currentProgress)

        // 12 consecutive weeks -> streak_12w unlocked
        val workouts12w = (0..11).map { createWorkout(now.minus(it * 7L, ChronoUnit.DAYS).toEpochMilli()) }
        val r12w = engine.calculateReport(workouts12w, emptyList(), weeklyTarget = 1, zoneId = zoneId)
        val badge12wUnlocked = r12w.badges.find { it.id == "streak_12w" }
        assertNotNull(badge12wUnlocked)
        assertTrue(badge12wUnlocked!!.isUnlocked)
        assertEquals(12, badge12wUnlocked.currentProgress)
    }

    @Test
    fun testUncompletedWorkoutsIgnoredAcrossAllMetrics() {
        val uncompleted = (1..5).map {
            createWorkout(now.minus(it * 7L, ChronoUnit.DAYS).toEpochMilli(), isCompleted = false)
        }
        val report = engine.calculateReport(uncompleted, emptyList(), weeklyTarget = 1, zoneId = zoneId)
        assertEquals(0, report.totalWorkouts)
        assertEquals(0, report.currentWeeklyStreak)
        assertEquals(0, report.longestWeeklyStreak)
        assertEquals(1, report.athleteLevel)
        assertEquals(0, report.currentXp)
        assertFalse(report.badges.find { it.id == "first_rep" }!!.isUnlocked)
    }

    @Test
    fun testSetTypeFilteringInXpAndCenturion() {
        val workout = listOf(createWorkout(now.toEpochMilli()))
        // 10 warmup sets (setType = 1) -> not normal sets, so 0 normal sets
        val warmupSets = (1..10).map { createSet(weight = 50.0, reps = 10, isCompleted = true, setType = 1) }
        val rWarmup = engine.calculateReport(workout, warmupSets, weeklyTarget = 1, zoneId = zoneId)
        assertEquals(0, rWarmup.totalSets)
        // 1 workout * 100 XP + 0 sets * 10 = 100 XP
        assertEquals(100, rWarmup.currentXp)
        assertEquals(0, rWarmup.badges.find { it.id == "centurion" }!!.currentProgress)

        // 10 normal sets (setType = 0) -> +100 XP
        val normalSets = (1..10).map { createSet(weight = 50.0, reps = 10, isCompleted = true, setType = 0) }
        val rNormal = engine.calculateReport(workout, normalSets, weeklyTarget = 1, zoneId = zoneId)
        assertEquals(10, rNormal.totalSets)
        assertEquals(200, rNormal.currentXp)
        assertEquals(10, rNormal.badges.find { it.id == "centurion" }!!.currentProgress)
    }

    @Test
    fun testHeatMapSpanAndAggregation() {
        val todayStr = dateFormatter.format(now)
        val w1 = createWorkout(now.toEpochMilli())
        val w2 = createWorkout(now.plusSeconds(3600).toEpochMilli()) // Same day second workout
        val ancientWorkout = createWorkout(now.minus(400, ChronoUnit.DAYS).toEpochMilli()) // Out of 365 day window

        val report = engine.calculateReport(listOf(w1, w2, ancientWorkout), emptyList(), weeklyTarget = 1, zoneId = zoneId)
        // 365 days ago to today inclusive = 366 dates
        assertEquals(366, report.heatMapData.size)
        assertEquals(2, report.heatMapData[todayStr])
    }

    @Test
    fun testIsoYearBoundaryCrossingPreservesStreakContinuity() {
        // Schedule across 2023 W51, 2023 W52, 2024 W01, 2024 W02 (now is 2024-01-08)
        val wWeek2 = createWorkout(now.toEpochMilli()) // 2024-01-08
        val wWeek1 = createWorkout(now.minus(7L, ChronoUnit.DAYS).toEpochMilli()) // 2024-01-01
        val wWeek52 = createWorkout(now.minus(14L, ChronoUnit.DAYS).toEpochMilli()) // 2023-12-25
        val wWeek51 = createWorkout(now.minus(21L, ChronoUnit.DAYS).toEpochMilli()) // 2023-12-18

        val report = engine.calculateReport(
            listOf(wWeek2, wWeek1, wWeek52, wWeek51),
            emptyList(),
            weeklyTarget = 1,
            zoneId = zoneId
        )

        assertEquals("Streak across year transition should be 4", 4, report.currentWeeklyStreak)
        assertEquals("Longest streak across year transition should be 4", 4, report.longestWeeklyStreak)
    }

    @Test
    fun testBadgeProgressClampingAtMaxValues() {
        // 15 workouts -> first_rep unlocked with exact epoch
        val firstEpoch = now.minus(100L, ChronoUnit.DAYS).toEpochMilli()
        val workouts = (0..14).map {
            createWorkout(firstEpoch + (it * 86400000L))
        }

        // 250 normal completed sets (> 100 max for centurion)
        val sets = (1..250).map { createSet(weight = 1000.0, reps = 1) } // 250,000 kg (> 100,000 max)

        val report = engine.calculateReport(workouts, sets, weeklyTarget = 1, zoneId = zoneId)

        val firstRep = report.badges.find { it.id == "first_rep" }!!
        assertTrue(firstRep.isUnlocked)
        assertEquals(1, firstRep.currentProgress)
        assertEquals(firstEpoch, firstRep.unlockedDateEpochMilli)

        val centurion = report.badges.find { it.id == "centurion" }!!
        assertTrue(centurion.isUnlocked)
        assertEquals(100, centurion.currentProgress) // Clamped at maxProgress 100

        val volume100k = report.badges.find { it.id == "volume_100k" }!!
        assertTrue(volume100k.isUnlocked)
        assertEquals(100000, volume100k.currentProgress) // Clamped at maxProgress 100000
    }

    @Test
    fun testHighTierAthleteLevelProgressionAndXpRollover() {
        // 45 workouts = 4500 XP
        val workouts = (1..45).map { createWorkout(now.minus(it.toLong(), ChronoUnit.DAYS).toEpochMilli()) }
        // 450 normal sets = 4500 XP -> Total = 9000 XP
        val sets = (1..450).map { createSet(50.0, 10) }

        val report9000 = engine.calculateReport(workouts, sets, weeklyTarget = 1, zoneId = zoneId)
        // Level = (9000 / 1000) + 1 = 10
        assertEquals(10, report9000.athleteLevel)
        assertEquals(0, report9000.currentXp)
        assertEquals(1000, report9000.xpForNextLevel)

        // Add 1 normal set (+10 XP) -> Total = 9010 XP -> Level 10, 10 XP
        val setsPlusOne = sets + createSet(50.0, 10)
        val report9010 = engine.calculateReport(workouts, setsPlusOne, weeklyTarget = 1, zoneId = zoneId)
        assertEquals(10, report9010.athleteLevel)
        assertEquals(10, report9010.currentXp)
    }

    @Test
    fun testSameDayMultipleWorkoutsCountTowardWeeklyTargetAndHeatMap() {
        val todayMs = now.toEpochMilli()
        val workouts = listOf(
            createWorkout(todayMs),
            createWorkout(todayMs + 3600000L),
            createWorkout(todayMs + 7200000L),
            createWorkout(todayMs + 10800000L)
        )

        val report = engine.calculateReport(workouts, emptyList(), weeklyTarget = 4, zoneId = zoneId)

        assertEquals("4 workouts on same day should satisfy weeklyTarget=4", 1, report.currentWeeklyStreak)
        assertEquals(4, report.currentWeekWorkoutsCompleted)
        assertEquals(4, report.totalWorkouts)
        val todayStr = dateFormatter.format(now)
        assertEquals(4, report.heatMapData[todayStr])
    }
}

