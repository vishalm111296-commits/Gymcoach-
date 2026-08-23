package com.gymcoach.app.core.program

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavioral contract for weekly muscle-volume accounting and the V-taper
 * scoring rules. Uses the compile-safe CompletedSetEntry input.
 */
class VolumeCalculatorTest {

    private val calculator = VolumeCalculator()

    private fun entry(exerciseId: Long, date: Long, completed: Boolean = true, setType: Int = 0) =
        VolumeCalculator.CompletedSetEntry(
            exerciseId = exerciseId,
            date = date,
            completed = completed,
            setType = setType
        )

    /** N distinct exercise ids, each with `muscle` as its single primary mover. */
    private fun primaryOnlyMap(muscle: String, count: Int): Map<Long, List<VolumeCalculator.MuscleAssignment>> =
        (1L..count).associateWith { listOf(VolumeCalculator.MuscleAssignment(muscle, VolumeCalculator.MuscleRole.PRIMARY)) }

    private fun latStatus(count: Int): VolumeCalculator.MuscleVolume {
        val balance = calculator.calculateWeeklyVolume(
            completedSets = (1L..count).map { entry(it, date = 0L) },
            exerciseMuscleMap = primaryOnlyMap("Lats", count)
        )
        return balance.latVolume
    }

    // ---- Evidence-band classification ----

    @Test fun `nine weekly sets are insufficient`() = assertEquals(VolumeCalculator.VolumeStatus.INSUFFICIENT, latStatus(9).status)
    @Test fun `ten weekly sets reach moderate band`() = assertEquals(VolumeCalculator.VolumeStatus.MODERATE, latStatus(10).status)
    @Test fun `thirteen weekly sets stay moderate`() = assertEquals(VolumeCalculator.VolumeStatus.MODERATE, latStatus(13).status)
    @Test fun `fourteen weekly sets reach optimal band`() = assertEquals(VolumeCalculator.VolumeStatus.OPTIMAL, latStatus(14).status)
    @Test fun `seventeen weekly sets stay optimal`() = assertEquals(VolumeCalculator.VolumeStatus.OPTIMAL, latStatus(17).status)
    @Test fun `eighteen weekly sets enter high band`() = assertEquals(VolumeCalculator.VolumeStatus.HIGH, latStatus(18).status)
    @Test fun `twentyone weekly sets stay high`() = assertEquals(VolumeCalculator.VolumeStatus.HIGH, latStatus(21).status)
    @Test fun `twentytwo weekly sets flag excessive`() = assertEquals(VolumeCalculator.VolumeStatus.EXCESSIVE, latStatus(22).status)

    // ---- Set filtering ----

    @Test
    fun `incomplete and warmup sets never count toward volume`() {
        val sets = listOf(
            entry(1, 0L, completed = false),
            entry(2, 0L, completed = true, setType = 1),
            entry(3, 0L, completed = true, setType = 0)
        )
        val balance = calculator.calculateWeeklyVolume(
            completedSets = sets,
            exerciseMuscleMap = mapOf(
                1L to listOf(VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY)),
                2L to listOf(VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY)),
                3L to listOf(VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.PRIMARY))
            )
        )
        assertEquals(1, balance.latVolume.directSets)
    }

    @Test
    fun `empty history yields all-insufficient balance without crashing`() {
        val balance = calculator.calculateWeeklyVolume(emptyList(), emptyMap())
        assertTrue(balance.asList().all { it.status == VolumeCalculator.VolumeStatus.INSUFFICIENT })
        assertEquals(0, balance.asList().sumOf { it.weeklySets })
    }

    // ---- Role weighting is observable via direct vs indirect split ----

    @Test
    fun `secondary role lands in indirect bucket not direct`() {
        val sets = listOf(entry(1, 0L))
        val balance = calculator.calculateWeeklyVolume(
            completedSets = sets,
            exerciseMuscleMap = mapOf(
                1L to listOf(
                    VolumeCalculator.MuscleAssignment("Lats", VolumeCalculator.MuscleRole.SECONDARY),
                    VolumeCalculator.MuscleAssignment("Biceps", VolumeCalculator.MuscleRole.PRIMARY)
                )
            )
        )
        assertEquals(0, balance.latVolume.directSets)
        assertEquals(1, balance.latVolume.indirectSets)
        assertEquals(1, balance.bicepsVolume.directSets)
        assertEquals(0, balance.bicepsVolume.indirectSets)
    }

    // ---- V-taper aggregation ----

    @Test
    fun `good v-taper requires optimal primaries and strong secondaries`() {
        fun balance(lat: Int, lateralDelt: Int, rearDelt: Int, upperChest: Int, upperBack: Int) =
            calculator.calculateWeeklyVolume(
                completedSets = emptyList(),
                exerciseMuscleMap = emptyMap()
            ).let { _ ->
                // Build directly via classification of synthetic counts:
                val vol = { n: Int ->
                    VolumeCalculator.MuscleVolume("m", n, n, 0, classifyPublic(n))
                }
                VolumeCalculator.TrainingBalance(
                    latVolume = vol(lat),
                    lateralDeltVolume = vol(lateralDelt),
                    rearDeltVolume = vol(rearDelt),
                    upperChestVolume = vol(upperChest),
                    upperBackVolume = vol(upperBack),
                    bicepsVolume = vol(0), tricepsVolume = vol(0),
                    quadricepsVolume = vol(0), hamstringsVolume = vol(0),
                    glutesVolume = vol(0), calvesVolume = vol(0), coreVolume = vol(0)
                )
            }

        val good = calculator.calculateVtaperBalance(balance(16, 16, 16, 16, 16))
        assertEquals("Good V-taper volume distribution", good.overallBalance)

        val moderate = calculator.calculateVtaperBalance(balance(16, 16, 11, 11, 11))
        assertEquals("Moderate V-taper focus", moderate.overallBalance)

        val low = calculator.calculateVtaperBalance(balance(9, 9, 9, 9, 9))
        assertEquals("Low V-taper volume", low.overallBalance)
    }

    // Mirror of the private classifier so aggregate tests can build volumes.
    private fun classifyPublic(sets: Int): VolumeCalculator.VolumeStatus = when {
        sets < 10 -> VolumeCalculator.VolumeStatus.INSUFFICIENT
        sets < 14 -> VolumeCalculator.VolumeStatus.MODERATE
        sets < 18 -> VolumeCalculator.VolumeStatus.OPTIMAL
        sets < 22 -> VolumeCalculator.VolumeStatus.HIGH
        else -> VolumeCalculator.VolumeStatus.EXCESSIVE
    }
}
