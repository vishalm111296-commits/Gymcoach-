package com.gymcoach.app.core.program

import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VolumeCalculatorTest {

    private lateinit var volumeCalculator: VolumeCalculator

    @Before
    fun setUp() {
        volumeCalculator = VolumeCalculator()
    }

    @Test
    fun `empty set list returns zero volume across all muscles`() {
        val balance = volumeCalculator.calculateWeeklyVolume(emptyList(), emptyMap())
        assertTrue("All weekly sets should be 0", balance.asList().all { it.weeklySets == 0 })
        assertTrue("All statuses should be INSUFFICIENT", balance.asList().all { it.status == VolumeCalculator.VolumeStatus.INSUFFICIENT })
    }

    @Test
    fun `uncompleted sets and warmups are excluded from volume`() {
        val warmupSet = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 1, workoutExerciseId = 10, setNumber = 1, weight = 20.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 1),
            exerciseId = 100,
            workoutDate = System.currentTimeMillis()
        )
        val uncompletedSet = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 2, workoutExerciseId = 10, setNumber = 2, weight = 30.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = false, setType = 0),
            exerciseId = 100,
            workoutDate = System.currentTimeMillis()
        )
        // Use canonical MUSCLE_BACK constant to avoid future mismatch
        val muscleMap = mapOf(
            100L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BACK, VolumeCalculator.MuscleRole.PRIMARY))
        )

        val balance = volumeCalculator.calculateWeeklyVolume(listOf(warmupSet, uncompletedSet), muscleMap)
        // Fix: field renamed latVolume -> backVolume (F-TAXONOMY-1)
        assertEquals("Back weekly sets should be 0", 0, balance.backVolume.weeklySets)
    }

    @Test
    fun `completed normal sets add weighted credit`() {
        val completedSet = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 1, workoutExerciseId = 10, setNumber = 1, weight = 20.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
            exerciseId = 100,
            workoutDate = System.currentTimeMillis()
        )
        val muscleMap = mapOf(
            100L to listOf(
                VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BACK, VolumeCalculator.MuscleRole.PRIMARY),
                VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BICEPS, VolumeCalculator.MuscleRole.SECONDARY)
            )
        )

        val balance = volumeCalculator.calculateWeeklyVolume(listOf(completedSet), muscleMap)
        // Fix: field renamed latVolume -> backVolume (F-TAXONOMY-1)
        assertEquals("Back direct sets should be 1", 1, balance.backVolume.directSets)
        assertEquals("Biceps indirect sets should be 1", 1, balance.bicepsVolume.indirectSets)
    }

    @Test
    fun `back exercises with MUSCLE_BACK key are counted in backVolume`() {
        // Regression test for F-TAXONOMY-1: exercises tagged muscleGroup="Back"
        // (the canonical seed-data string) must flow to backVolume, not be lost.
        val set = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 3, workoutExerciseId = 20, setNumber = 1, weight = 60.0, reps = 8, rpe = 8.0, restSeconds = 120, completed = true, setType = 0),
            exerciseId = 200,
            workoutDate = System.currentTimeMillis()
        )
        val muscleMap = mapOf(
            200L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BACK, VolumeCalculator.MuscleRole.PRIMARY))
        )

        val balance = volumeCalculator.calculateWeeklyVolume(listOf(set), muscleMap)
        assertEquals("Back direct sets must be 1 for a completed back exercise", 1, balance.backVolume.directSets)
        assertEquals("Back weekly sets must be 1", 1, balance.backVolume.weeklySets)
        assertTrue("Back status must not be INSUFFICIENT with 1 set", balance.backVolume.status == VolumeCalculator.VolumeStatus.INSUFFICIENT)
    }

    @Test
    fun `calculateVtaperBalance provides expected status summary`() {
        val balance = volumeCalculator.calculateWeeklyVolume(emptyList(), emptyMap())
        val vtaper = volumeCalculator.calculateVtaperBalance(balance)
        assertEquals("Low V-taper volume", vtaper.overallBalance)
    }

    @Test
    fun `direct and indirect sets across primary secondary stabilizer roles are correctly counted`() {
        val completedSet1 = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 1, workoutExerciseId = 10, setNumber = 1, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
            exerciseId = 100,
            workoutDate = 1700000000000L
        )
        val completedSet2 = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 2, workoutExerciseId = 10, setNumber = 2, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
            exerciseId = 100,
            workoutDate = 1700000000000L
        )
        val muscleMap = mapOf(
            100L to listOf(
                VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY),
                VolumeCalculator.MuscleAssignment("Biceps", VolumeCalculator.MuscleRole.SECONDARY),
                VolumeCalculator.MuscleAssignment("Core", VolumeCalculator.MuscleRole.STABILIZER)
            )
        )

        val balance = volumeCalculator.calculateWeeklyVolume(listOf(completedSet1, completedSet2), muscleMap)
        assertEquals("Back direct sets should be 2", 2, balance.backVolume.directSets)
        assertEquals("Back indirect sets should be 0", 0, balance.backVolume.indirectSets)
        assertEquals("Lats alias direct sets should be 2", 2, balance.latVolume.directSets)
        assertEquals("Lats alias indirect sets should be 0", 0, balance.latVolume.indirectSets)
        assertEquals("Biceps direct sets should be 0", 0, balance.bicepsVolume.directSets)
        assertEquals("Biceps indirect sets should be 2", 2, balance.bicepsVolume.indirectSets)
        assertEquals("Core direct sets should be 0", 0, balance.coreVolume.directSets)
        assertEquals("Core indirect sets should be 2", 2, balance.coreVolume.indirectSets)
    }

    @Test
    fun `multi week set contexts correctly aggregate weekly volume averages`() {
        val week1Date = 1700000000000L
        val week2Date = week1Date + (7 * 24 * 60 * 60 * 1000L)

        val setWeek1 = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 1, workoutExerciseId = 10, setNumber = 1, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
            exerciseId = 100,
            workoutDate = week1Date
        )
        val set1Week2 = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 2, workoutExerciseId = 20, setNumber = 1, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
            exerciseId = 100,
            workoutDate = week2Date
        )
        val set2Week2 = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 3, workoutExerciseId = 20, setNumber = 2, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
            exerciseId = 100,
            workoutDate = week2Date
        )
        val muscleMap = mapOf(
            100L to listOf(
                VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY)
            )
        )

        val balance = volumeCalculator.calculateWeeklyVolume(listOf(setWeek1, set1Week2, set2Week2), muscleMap)
        assertEquals("Back total direct sets across weeks", 3, balance.backVolume.directSets)
        assertEquals("Back total indirect sets across weeks", 0, balance.backVolume.indirectSets)
        assertEquals("Back weekly rate across 2 weeks (3 sets / 2 weeks = 1.5 -> 2)", 2, balance.backVolume.weeklySets)
        assertEquals("Back total sets across weeks", 3, balance.backVolume.totalSets)
        assertEquals("Lats alias total direct sets across weeks", 3, balance.latVolume.directSets)
        assertEquals("Lats alias total indirect sets across weeks", 0, balance.latVolume.indirectSets)
        assertEquals("Lats alias weekly rate across 2 weeks", 2, balance.latVolume.weeklySets)
        assertEquals("Lats alias total sets across weeks", 3, balance.latVolume.totalSets)
    }

    @Test
    fun `isoWeekKey correctly groups dates across year boundaries into same ISO week`() {
        val zone = java.time.ZoneOffset.UTC
        // 2024-12-30 (Mon) to 2025-01-05 (Sun) is ISO week 1 of week-based year 2025
        val monDec30 = java.time.LocalDate.of(2024, 12, 30).atStartOfDay(zone).toInstant().toEpochMilli()
        val tueDec31 = java.time.LocalDate.of(2024, 12, 31).atStartOfDay(zone).toInstant().toEpochMilli()
        val wedJan01 = java.time.LocalDate.of(2025, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val sunJan05 = java.time.LocalDate.of(2025, 1, 5).atStartOfDay(zone).toInstant().toEpochMilli()
        val monJan06 = java.time.LocalDate.of(2025, 1, 6).atStartOfDay(zone).toInstant().toEpochMilli()

        val keyDec30 = volumeCalculator.isoWeekKey(monDec30, zone)
        val keyDec31 = volumeCalculator.isoWeekKey(tueDec31, zone)
        val keyJan01 = volumeCalculator.isoWeekKey(wedJan01, zone)
        val keyJan05 = volumeCalculator.isoWeekKey(sunJan05, zone)
        val keyJan06 = volumeCalculator.isoWeekKey(monJan06, zone)

        assertEquals("Dec 30 2024 should be 202501", 202501, keyDec30)
        assertEquals("Dec 31 2024 should be 202501", 202501, keyDec31)
        assertEquals("Jan 01 2025 should be 202501", 202501, keyJan01)
        assertEquals("Jan 05 2025 should be 202501", 202501, keyJan05)
        assertEquals("Jan 06 2025 should be 202502", 202502, keyJan06)
    }

    @Test
    fun `isoWeekKey separates Sunday from following Monday`() {
        val zone = java.time.ZoneOffset.UTC
        val sunDec29 = java.time.LocalDate.of(2024, 12, 29).atStartOfDay(zone).toInstant().toEpochMilli()
        val monDec30 = java.time.LocalDate.of(2024, 12, 30).atStartOfDay(zone).toInstant().toEpochMilli()

        val keySun = volumeCalculator.isoWeekKey(sunDec29, zone)
        val keyMon = volumeCalculator.isoWeekKey(monDec30, zone)

        assertEquals("Sunday Dec 29 2024 belongs to 202452", 202452, keySun)
        assertEquals("Monday Dec 30 2024 belongs to 202501", 202501, keyMon)
    }

    @Test
    fun `calendar weeks divides volume over total calendar span even with empty weeks`() {
        val week1Date = 1700000000000L
        val sets = (1..8).map { i ->
            VolumeCalculator.SetWithContext(
                set = WorkoutSetEntity(id = i.toLong(), workoutExerciseId = 10, setNumber = i, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
                exerciseId = 100,
                workoutDate = week1Date
            )
        }
        val muscleMap = mapOf(
            100L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BACK, VolumeCalculator.MuscleRole.PRIMARY))
        )

        // Without totalCalendarWeeks: single active week returns total sets (8)
        val activeBalance = volumeCalculator.calculateWeeklyVolume(sets, muscleMap)
        assertEquals(8, activeBalance.backVolume.weeklySets)
        assertEquals(8, activeBalance.backVolume.totalSets)

        // With totalCalendarWeeks = 4: spans 4 calendar weeks with 3 empty weeks (8 / 4 = 2)
        val calendarBalance = volumeCalculator.calculateWeeklyVolume(sets, muscleMap, totalCalendarWeeks = 4)
        assertEquals(2, calendarBalance.backVolume.weeklySets)
        assertEquals(8, calendarBalance.backVolume.totalSets)
    }

    @Test
    fun `empty weeks with calendar weeks returns zero volume across all muscles`() {
        val balance = volumeCalculator.calculateWeeklyVolume(emptyList(), emptyMap(), totalCalendarWeeks = 4)
        assertTrue("All weekly sets should be 0", balance.asList().all { it.weeklySets == 0 })
        assertTrue("All total sets should be 0", balance.asList().all { it.totalSets == 0 })
        assertTrue("All statuses should be INSUFFICIENT", balance.asList().all { it.status == VolumeCalculator.VolumeStatus.INSUFFICIENT })
    }

    @Test
    fun `multi week transition with calendar weeks preserves correct total and rate`() {
        val week1Date = 1700000000000L
        val week2Date = week1Date + (7 * 24 * 60 * 60 * 1000L)
        val setsWeek1 = (1..4).map { i ->
            VolumeCalculator.SetWithContext(
                set = WorkoutSetEntity(id = i.toLong(), workoutExerciseId = 10, setNumber = i, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
                exerciseId = 100,
                workoutDate = week1Date
            )
        }
        val setsWeek2 = (5..8).map { i ->
            VolumeCalculator.SetWithContext(
                set = WorkoutSetEntity(id = i.toLong(), workoutExerciseId = 20, setNumber = i - 4, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
                exerciseId = 100,
                workoutDate = week2Date
            )
        }
        val muscleMap = mapOf(
            100L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BACK, VolumeCalculator.MuscleRole.PRIMARY))
        )

        // 8 total sets across 2 active weeks in a 4-week window -> 8 / 4 = 2 sets/week
        val balance = volumeCalculator.calculateWeeklyVolume(setsWeek1 + setsWeek2, muscleMap, totalCalendarWeeks = 4)
        assertEquals(2, balance.backVolume.weeklySets)
        assertEquals(8, balance.backVolume.totalSets)
        assertEquals(8, balance.backVolume.directSets)
    }
}
