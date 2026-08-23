package com.gymcoach.app.core.program

import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VolumeCalculatorTest {

    private lateinit var calculator: VolumeCalculator

    @Before
    fun setUp() {
        calculator = VolumeCalculator()
    }

    // --- Helper ---

    private fun makeSet(
        completed: Boolean = true,
        setType: Int = 0,
        weight: Double = 50.0,
        reps: Int = 10
    ) = WorkoutSetEntity(
        id = 0,
        workoutExerciseId = 0,
        setNumber = 1,
        weight = weight,
        reps = reps,
        rpe = 7.0,
        restSeconds = 90,
        completed = completed,
        setType = setType
    )

    private fun makeContext(
        set: WorkoutSetEntity = makeSet(),
        exerciseId: Long = 1L,
        workoutDate: Long = System.currentTimeMillis()
    ) = VolumeCalculator.SetWithContext(set, exerciseId, workoutDate)

    private fun muscleMap(vararg assignments: Pair<Long, List<VolumeCalculator.MuscleAssignment>>): Map<Long, List<VolumeCalculator.MuscleAssignment>> =
        assignments.toMap()

    // --- Weekly Volume Tests ---

    @Test
    fun `empty sets produces zero volume`() {
        val balance = calculator.calculateWeeklyVolume(
            completedSets = emptyList(),
            exerciseMuscleMap = emptyMap()
        )
        assertEquals(0, balance.latVolume.weeklySets)
        assertEquals(VolumeCalculator.VolumeStatus.INSUFFICIENT, balance.latVolume.status)
    }

    @Test
    fun `uncompleted sets are excluded`() {
        val sets = listOf(
            makeContext(set = makeSet(completed = false), exerciseId = 1L)
        )
        val map = muscleMap(1L to listOf(VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY)))
        val balance = calculator.calculateWeeklyVolume(sets, map)
        assertEquals(0, balance.latVolume.weeklySets)
    }

    @Test
    fun `warmup sets are excluded (setType != 0)`() {
        val sets = listOf(
            makeContext(set = makeSet(setType = 1), exerciseId = 1L) // WARMUP
        )
        val map = muscleMap(1L to listOf(VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY)))
        val balance = calculator.calculateWeeklyVolume(sets, map)
        assertEquals(0, balance.latVolume.weeklySets)
    }

    @Test
    fun `completed normal sets count correctly`() {
        val sets = listOf(
            makeContext(set = makeSet(), exerciseId = 1L),
            makeContext(set = makeSet(), exerciseId = 1L),
            makeContext(set = makeSet(), exerciseId = 1L)
        )
        val map = muscleMap(1L to listOf(VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY)))
        val balance = calculator.calculateWeeklyVolume(sets, map)
        assertEquals(3, balance.latVolume.weeklySets)
        assertEquals(3, balance.latVolume.directSets)
    }

    @Test
    fun `multiple muscles per exercise distribute correctly`() {
        val sets = listOf(
            makeContext(set = makeSet(), exerciseId = 1L)
        )
        val map = muscleMap(
            1L to listOf(
                VolumeCalculator.MuscleAssignment("Chest", VolumeCalculator.MuscleRole.PRIMARY),
                VolumeCalculator.MuscleAssignment("Triceps", VolumeCalculator.MuscleRole.SECONDARY)
            )
        )
        val balance = calculator.calculateWeeklyVolume(sets, map)
        assertEquals(1, balance.bicepsVolume.weeklySets) // 0
        assertEquals(1, balance.tricepsVolume.weeklySets)
    }

    @Test
    fun `unknown exercise ID produces no volume`() {
        val sets = listOf(
            makeContext(set = makeSet(), exerciseId = 99L)
        )
        val map = muscleMap(1L to listOf(VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY)))
        val balance = calculator.calculateWeeklyVolume(sets, map)
        assertEquals(0, balance.latVolume.weeklySets)
    }

    // --- Classification Tests ---

    @Test
    fun `classification thresholds are correct`() {
        // < 10 = INSUFFICIENT
        val low = calculator.calculateWeeklyVolume(
            (1..9).map { makeContext(set = makeSet(), exerciseId = 1L) },
            muscleMap(1L to listOf(VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY)))
        )
        assertEquals(VolumeCalculator.VolumeStatus.INSUFFICIENT, low.latVolume.status)

        // 10-13 = MODERATE
        val mod = calculator.calculateWeeklyVolume(
            (1..12).map { makeContext(set = makeSet(), exerciseId = 1L) },
            muscleMap(1L to listOf(VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY)))
        )
        assertEquals(VolumeCalculator.VolumeStatus.MODERATE, mod.latVolume.status)

        // 14-17 = OPTIMAL
        val opt = calculator.calculateWeeklyVolume(
            (1..15).map { makeContext(set = makeSet(), exerciseId = 1L) },
            muscleMap(1L to listOf(VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY)))
        )
        assertEquals(VolumeCalculator.VolumeStatus.OPTIMAL, opt.latVolume.status)
    }

    // --- V-Taper Balance Tests ---

    @Test
    fun `vtaper balance good when primary and secondary high`() {
        val balance = VolumeCalculator.TrainingBalance(
            latVolume = VolumeCalculator.MuscleVolume("Lats", 18, 18, 0, VolumeCalculator.VolumeStatus.OPTIMAL),
            lateralDeltVolume = VolumeCalculator.MuscleVolume("Lateral Deltoid", 16, 16, 0, VolumeCalculator.VolumeStatus.OPTIMAL),
            rearDeltVolume = VolumeCalculator.MuscleVolume("Rear Deltoid", 14, 14, 0, VolumeCalculator.VolumeStatus.OPTIMAL),
            upperChestVolume = VolumeCalculator.MuscleVolume("Upper Chest", 14, 14, 0, VolumeCalculator.VolumeStatus.OPTIMAL),
            upperBackVolume = VolumeCalculator.MuscleVolume("Upper Back", 14, 14, 0, VolumeCalculator.VolumeStatus.OPTIMAL),
            bicepsVolume = VolumeCalculator.MuscleVolume("Biceps", 14, 14, 0, VolumeCalculator.VolumeStatus.OPTIMAL),
            tricepsVolume = VolumeCalculator.MuscleVolume("Triceps", 14, 14, 0, VolumeCalculator.VolumeStatus.OPTIMAL),
            quadricepsVolume = VolumeCalculator.MuscleVolume("Quadriceps", 14, 14, 0, VolumeCalculator.VolumeStatus.OPTIMAL),
            hamstringsVolume = VolumeCalculator.MuscleVolume("Hamstrings", 14, 14, 0, VolumeCalculator.VolumeStatus.OPTIMAL),
            glutesVolume = VolumeCalculator.MuscleVolume("Glutes", 14, 14, 0, VolumeCalculator.VolumeStatus.OPTIMAL),
            calvesVolume = VolumeCalculator.MuscleVolume("Calves", 14, 14, 0, VolumeCalculator.VolumeStatus.OPTIMAL),
            coreVolume = VolumeCalculator.MuscleVolume("Core", 10, 10, 0, VolumeCalculator.VolumeStatus.MODERATE)
        )
        val vtaper = calculator.calculateVtaperBalance(balance)
        assertTrue(vtaper.overallBalance.contains("Good"))
    }

    @Test
    fun `vtaper balance low when primary muscles neglected`() {
        val balance = VolumeCalculator.TrainingBalance(
            latVolume = VolumeCalculator.MuscleVolume("Lats", 4, 4, 0, VolumeCalculator.VolumeStatus.INSUFFICIENT),
            lateralDeltVolume = VolumeCalculator.MuscleVolume("Lateral Deltoid", 4, 4, 0, VolumeCalculator.VolumeStatus.INSUFFICIENT),
            rearDeltVolume = VolumeCalculator.MuscleVolume("Rear Deltoid", 14, 14, 0, VolumeCalculator.VolumeStatus.OPTIMAL),
            upperChestVolume = VolumeCalculator.MuscleVolume("Upper Chest", 14, 14, 0, VolumeCalculator.VolumeStatus.OPTIMAL),
            upperBackVolume = VolumeCalculator.MuscleVolume("Upper Back", 14, 14, 0, VolumeCalculator.VolumeStatus.OPTIMAL),
            bicepsVolume = VolumeCalculator.MuscleVolume("Biceps", 14, 14, 0, VolumeCalculator.VolumeStatus.OPTIMAL),
            tricepsVolume = VolumeCalculator.MuscleVolume("Triceps", 14, 14, 0, VolumeCalculator.VolumeStatus.OPTIMAL),
            quadricepsVolume = VolumeCalculator.MuscleVolume("Quadriceps", 14, 14, 0, VolumeCalculator.VolumeStatus.OPTIMAL),
            hamstringsVolume = VolumeCalculator.MuscleVolume("Hamstrings", 14, 14, 0, VolumeCalculator.VolumeStatus.OPTIMAL),
            glutesVolume = VolumeCalculator.MuscleVolume("Glutes", 14, 14, 0, VolumeCalculator.VolumeStatus.OPTIMAL),
            calvesVolume = VolumeCalculator.MuscleVolume("Calves", 14, 14, 0, VolumeCalculator.VolumeStatus.OPTIMAL),
            coreVolume = VolumeCalculator.MuscleVolume("Core", 14, 14, 0, VolumeCalculator.VolumeStatus.OPTIMAL)
        )
        val vtaper = calculator.calculateVtaperBalance(balance)
        assertTrue(vtaper.overallBalance.contains("Low"))
    }

    // --- TrainingBalance.asList ---

    @Test
    fun `asList returns all 12 muscle groups`() {
        val balance = VolumeCalculator.TrainingBalance(
            latVolume = VolumeCalculator.MuscleVolume("Lats", 0, 0, 0, VolumeCalculator.VolumeStatus.INSUFFICIENT),
            lateralDeltVolume = VolumeCalculator.MuscleVolume("Lateral Deltoid", 0, 0, 0, VolumeCalculator.VolumeStatus.INSUFFICIENT),
            rearDeltVolume = VolumeCalculator.MuscleVolume("Rear Deltoid", 0, 0, 0, VolumeCalculator.VolumeStatus.INSUFFICIENT),
            upperChestVolume = VolumeCalculator.MuscleVolume("Upper Chest", 0, 0, 0, VolumeCalculator.VolumeStatus.INSUFFICIENT),
            upperBackVolume = VolumeCalculator.MuscleVolume("Upper Back", 0, 0, 0, VolumeCalculator.VolumeStatus.INSUFFICIENT),
            bicepsVolume = VolumeCalculator.MuscleVolume("Biceps", 0, 0, 0, VolumeCalculator.VolumeStatus.INSUFFICIENT),
            tricepsVolume = VolumeCalculator.MuscleVolume("Triceps", 0, 0, 0, VolumeCalculator.VolumeStatus.INSUFFICIENT),
            quadricepsVolume = VolumeCalculator.MuscleVolume("Quadriceps", 0, 0, 0, VolumeCalculator.VolumeStatus.INSUFFICIENT),
            hamstringsVolume = VolumeCalculator.MuscleVolume("Hamstrings", 0, 0, 0, VolumeCalculator.VolumeStatus.INSUFFICIENT),
            glutesVolume = VolumeCalculator.MuscleVolume("Glutes", 0, 0, 0, VolumeCalculator.VolumeStatus.INSUFFICIENT),
            calvesVolume = VolumeCalculator.MuscleVolume("Calves", 0, 0, 0, VolumeCalculator.VolumeStatus.INSUFFICIENT),
            coreVolume = VolumeCalculator.MuscleVolume("Core", 0, 0, 0, VolumeCalculator.VolumeStatus.INSUFFICIENT)
        )
        assertEquals(12, balance.asList().size)
    }
}
