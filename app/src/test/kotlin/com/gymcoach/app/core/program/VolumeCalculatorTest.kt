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

    @Test
    fun `calculateVtaperBalance reports good moderate and low ratings accurately`() {
        val weekDate = 1700000000000L

        // Helper to generate N sets for a given exercise ID
        fun makeSets(exerciseId: Long, count: Int): List<VolumeCalculator.SetWithContext> {
            return (1..count).map { i ->
                VolumeCalculator.SetWithContext(
                    set = WorkoutSetEntity(id = (exerciseId * 100 + i), workoutExerciseId = exerciseId, setNumber = i, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
                    exerciseId = exerciseId,
                    workoutDate = weekDate
                )
            }
        }

        val mapAll = mapOf(
            1L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BACK, VolumeCalculator.MuscleRole.PRIMARY)),
            2L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_LATERAL_DELT, VolumeCalculator.MuscleRole.PRIMARY)),
            3L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_REAR_DELT, VolumeCalculator.MuscleRole.PRIMARY)),
            4L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_CHEST, VolumeCalculator.MuscleRole.PRIMARY)),
            5L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_UPPER_BACK, VolumeCalculator.MuscleRole.PRIMARY))
        )

        // 1. Good V-taper: 15 sets (OPTIMAL, level 3) across all 5 groups -> primary = 3.0, secondary = 3.0
        val setsGood = makeSets(1L, 15) + makeSets(2L, 15) + makeSets(3L, 15) + makeSets(4L, 15) + makeSets(5L, 15)
        val balanceGood = volumeCalculator.calculateWeeklyVolume(setsGood, mapAll)
        val vtaperGood = volumeCalculator.calculateVtaperBalance(balanceGood)
        assertEquals(3.0, vtaperGood.primaryScore, 0.01)
        assertEquals(3.0, vtaperGood.secondaryScore, 0.01)
        assertEquals("Good V-taper volume distribution", vtaperGood.overallBalance)

        // 2. Moderate V-taper: 19 sets (HIGH, level 2) for Back & Lat Delt; 0 sets for secondary -> primary = 2.0, secondary = 0.0
        val setsMod = makeSets(1L, 19) + makeSets(2L, 19)
        val balanceMod = volumeCalculator.calculateWeeklyVolume(setsMod, mapAll)
        val vtaperMod = volumeCalculator.calculateVtaperBalance(balanceMod)
        assertEquals(2.0, vtaperMod.primaryScore, 0.01)
        assertEquals(0.0, vtaperMod.secondaryScore, 0.01)
        assertEquals("Moderate V-taper focus", vtaperMod.overallBalance)

        // 3. Low V-taper: 5 sets (INSUFFICIENT, level 0)
        val setsLow = makeSets(1L, 5)
        val balanceLow = volumeCalculator.calculateWeeklyVolume(setsLow, mapAll)
        val vtaperLow = volumeCalculator.calculateVtaperBalance(balanceLow)
        assertEquals("Low V-taper volume", vtaperLow.overallBalance)
    }

    @Test
    fun `muscle name variations like Lats case-insensitively map to Back volume`() {
        val weekDate = 1700000000000L
        val set1 = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 1, workoutExerciseId = 1, setNumber = 1, weight = 60.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
            exerciseId = 1L,
            workoutDate = weekDate
        )
        val set2 = VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(id = 2, workoutExerciseId = 2, setNumber = 1, weight = 70.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
            exerciseId = 2L,
            workoutDate = weekDate
        )

        val muscleMap = mapOf(
            1L to listOf(VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY)),
            2L to listOf(VolumeCalculator.MuscleAssignment("lats", VolumeCalculator.MuscleRole.PRIMARY))
        )

        val balance = volumeCalculator.calculateWeeklyVolume(listOf(set1, set2), muscleMap)
        assertEquals("Both Lats and lats assignments must map to Back volume", 2, balance.backVolume.directSets)
        assertEquals(2, balance.backVolume.totalSets)
        assertEquals(2, balance.backVolume.weeklySets)
    }

    @Test
    fun `volume status boundaries test exact threshold transitions`() {
        val weekDate = 1700000000000L
        fun setsForCount(count: Int): List<VolumeCalculator.SetWithContext> {
            return (1..count).map { i ->
                VolumeCalculator.SetWithContext(
                    set = WorkoutSetEntity(id = i.toLong(), workoutExerciseId = 1L, setNumber = i, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
                    exerciseId = 1L,
                    workoutDate = weekDate
                )
            }
        }
        val muscleMap = mapOf(
            1L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_CHEST, VolumeCalculator.MuscleRole.PRIMARY))
        )

        // sets < 10 -> INSUFFICIENT
        assertEquals(VolumeCalculator.VolumeStatus.INSUFFICIENT, volumeCalculator.calculateWeeklyVolume(setsForCount(9), muscleMap).upperChestVolume.status)
        // 10..13 -> MODERATE
        assertEquals(VolumeCalculator.VolumeStatus.MODERATE, volumeCalculator.calculateWeeklyVolume(setsForCount(10), muscleMap).upperChestVolume.status)
        assertEquals(VolumeCalculator.VolumeStatus.MODERATE, volumeCalculator.calculateWeeklyVolume(setsForCount(13), muscleMap).upperChestVolume.status)
        // 14..17 -> OPTIMAL
        assertEquals(VolumeCalculator.VolumeStatus.OPTIMAL, volumeCalculator.calculateWeeklyVolume(setsForCount(14), muscleMap).upperChestVolume.status)
        assertEquals(VolumeCalculator.VolumeStatus.OPTIMAL, volumeCalculator.calculateWeeklyVolume(setsForCount(17), muscleMap).upperChestVolume.status)
        // 18..21 -> HIGH
        assertEquals(VolumeCalculator.VolumeStatus.HIGH, volumeCalculator.calculateWeeklyVolume(setsForCount(18), muscleMap).upperChestVolume.status)
        assertEquals(VolumeCalculator.VolumeStatus.HIGH, volumeCalculator.calculateWeeklyVolume(setsForCount(21), muscleMap).upperChestVolume.status)
        // >= 22 -> EXCESSIVE
        assertEquals(VolumeCalculator.VolumeStatus.EXCESSIVE, volumeCalculator.calculateWeeklyVolume(setsForCount(22), muscleMap).upperChestVolume.status)
    }

    @Test
    fun `multi exercise secondary credit aggregation accurately calculates multi week triceps volume`() {
        val week1Date = 1700000000000L
        val week2Date = week1Date + (7 * 24 * 60 * 60 * 1000L)

        fun makeWeekSets(startDate: Long, baseId: Long): List<VolumeCalculator.SetWithContext> {
            val benchSets = (1..6).map { i ->
                VolumeCalculator.SetWithContext(
                    set = WorkoutSetEntity(id = baseId + i, workoutExerciseId = 1L, setNumber = i, weight = 80.0, reps = 8, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
                    exerciseId = 1L,
                    workoutDate = startDate
                )
            }
            val ohpSets = (1..4).map { i ->
                VolumeCalculator.SetWithContext(
                    set = WorkoutSetEntity(id = baseId + 10 + i, workoutExerciseId = 2L, setNumber = i, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
                    exerciseId = 2L,
                    workoutDate = startDate
                )
            }
            val triSets = (1..5).map { i ->
                VolumeCalculator.SetWithContext(
                    set = WorkoutSetEntity(id = baseId + 20 + i, workoutExerciseId = 3L, setNumber = i, weight = 30.0, reps = 12, rpe = 8.0, restSeconds = 60, completed = true, setType = 0),
                    exerciseId = 3L,
                    workoutDate = startDate
                )
            }
            return benchSets + ohpSets + triSets
        }

        val allSets = makeWeekSets(week1Date, 100L) + makeWeekSets(week2Date, 200L)
        val muscleMap = mapOf(
            1L to listOf(
                VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_CHEST, VolumeCalculator.MuscleRole.PRIMARY),
                VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_TRICEPS, VolumeCalculator.MuscleRole.SECONDARY)
            ),
            2L to listOf(
                VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_LATERAL_DELT, VolumeCalculator.MuscleRole.PRIMARY),
                VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_TRICEPS, VolumeCalculator.MuscleRole.SECONDARY)
            ),
            3L to listOf(
                VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_TRICEPS, VolumeCalculator.MuscleRole.PRIMARY)
            )
        )

        val balance = volumeCalculator.calculateWeeklyVolume(allSets, muscleMap)

        assertEquals(10, balance.tricepsVolume.directSets)
        assertEquals(20, balance.tricepsVolume.indirectSets)
        assertEquals(30, balance.tricepsVolume.totalSets)
        assertEquals(10, balance.tricepsVolume.weeklySets)
        assertEquals(VolumeCalculator.VolumeStatus.MODERATE, balance.tricepsVolume.status)
    }

    @Test
    fun `calculateVtaperBalance boundary conditions distinguish low moderate and good scores`() {
        val weekDate = 1700000000000L
        fun makeSets(exerciseId: Long, count: Int): List<VolumeCalculator.SetWithContext> {
            return (1..count).map { i ->
                VolumeCalculator.SetWithContext(
                    set = WorkoutSetEntity(id = (exerciseId * 1000 + i), workoutExerciseId = exerciseId, setNumber = i, weight = 50.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true, setType = 0),
                    exerciseId = exerciseId,
                    workoutDate = weekDate
                )
            }
        }
        val mapAll = mapOf(
            1L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BACK, VolumeCalculator.MuscleRole.PRIMARY)),
            2L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_LATERAL_DELT, VolumeCalculator.MuscleRole.PRIMARY)),
            3L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_REAR_DELT, VolumeCalculator.MuscleRole.PRIMARY)),
            4L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_CHEST, VolumeCalculator.MuscleRole.PRIMARY)),
            5L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_UPPER_BACK, VolumeCalculator.MuscleRole.PRIMARY))
        )

        // Case 1: Primary >= 3.0 (level 3 OPTIMAL for both Back & Lat Delt = 3.0), but Secondary < 2.0 (MODERATE 1, MODERATE 1, INSUFFICIENT 0 -> (1+1+0)/3 = 0.67)
        val setsPrimaryOptimalSecondaryLow = makeSets(1L, 15) + makeSets(2L, 15) + makeSets(3L, 10) + makeSets(4L, 10) + makeSets(5L, 5)
        val balance1 = volumeCalculator.calculateWeeklyVolume(setsPrimaryOptimalSecondaryLow, mapAll)
        val vtaper1 = volumeCalculator.calculateVtaperBalance(balance1)
        assertEquals(3.0, vtaper1.primaryScore, 0.01)
        assertTrue("Secondary score should be < 2.0", vtaper1.secondaryScore < 2.0)
        assertEquals("Moderate V-taper focus", vtaper1.overallBalance)

        // Case 2: Primary < 2.0 (MODERATE 1, INSUFFICIENT 0 -> 0.5) even if Secondary is OPTIMAL (level 3 across all 3 muscles)
        val setsPrimaryLowSecondaryOptimal = makeSets(1L, 10) + makeSets(2L, 5) + makeSets(3L, 15) + makeSets(4L, 15) + makeSets(5L, 15)
        val balance2 = volumeCalculator.calculateWeeklyVolume(setsPrimaryLowSecondaryOptimal, mapAll)
        val vtaper2 = volumeCalculator.calculateVtaperBalance(balance2)
        assertTrue("Primary score should be < 2.0", vtaper2.primaryScore < 2.0)
        assertEquals(3.0, vtaper2.secondaryScore, 0.01)
        assertEquals("Low V-taper volume", vtaper2.overallBalance)
    }
}
