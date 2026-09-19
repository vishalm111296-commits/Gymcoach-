package com.gymcoach.app.core.analytics

import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MuscleBalanceAnalyzerTest {

    private val analyzer = MuscleBalanceAnalyzer()

    @Test
    fun `test optimal balance calculation`() {
        // Quad 100, Ham 100 (Ratio 1.0)
        // Push 100, Pull 100 (Ratio 1.0)
        // Biceps 100, Triceps 100 (Ratio 1.0)

        val sets = listOf(
            createSet(1, 100.0, 1),
            createSet(2, 100.0, 1),
            createSet(3, 100.0, 1),
            createSet(4, 100.0, 1),
            createSet(5, 100.0, 1),
            createSet(6, 100.0, 1)
        )

        val map = mapOf(
            1L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_QUADRICEPS, VolumeCalculator.MuscleRole.PRIMARY)),
            2L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_HAMSTRINGS, VolumeCalculator.MuscleRole.PRIMARY)),
            3L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_CHEST, VolumeCalculator.MuscleRole.PRIMARY)),
            4L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BACK, VolumeCalculator.MuscleRole.PRIMARY)),
            5L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BICEPS, VolumeCalculator.MuscleRole.PRIMARY)),
            6L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_TRICEPS, VolumeCalculator.MuscleRole.PRIMARY))
        )

        val report = analyzer.analyzeBalance(sets, map)

        assertEquals(100, report.overallBalanceScore)
        assertEquals(BalanceStatus.OPTIMAL, report.quadHamstringRatio.status)
        assertEquals(BalanceStatus.OPTIMAL, report.pushPullRatio.status)
        assertEquals(BalanceStatus.OPTIMAL, report.bicepsTricepsRatio.status)
        assertTrue(report.correctivePrescriptions.isEmpty())
    }

    @Test
    fun `test quad-heavy detection and corrective suggestion`() {
        // Quad 120, Ham 100 (Ratio 1.2 > 1.15)
        val sets = listOf(
            createSet(1, 120.0, 1),
            createSet(2, 100.0, 1)
        )
        val map = mapOf(
            1L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_QUADRICEPS, VolumeCalculator.MuscleRole.PRIMARY)),
            2L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_HAMSTRINGS, VolumeCalculator.MuscleRole.PRIMARY))
        )

        val report = analyzer.analyzeBalance(sets, map)

        assertEquals(BalanceStatus.MODERATE_IMBALANCE, report.quadHamstringRatio.status)
        assertTrue(report.overallBalanceScore < 100)

        val prescription = report.correctivePrescriptions.find { it.targetMuscle == "Hamstrings" }
        assertTrue(prescription != null)
        assertTrue(prescription?.exerciseName?.contains("Nordic") == true || prescription?.exerciseName?.contains("Romanian") == true)
    }

    @Test
    fun `test push-heavy detection and corrective suggestion`() {
        // Push 110, Pull 100 (Ratio 1.1 > 1.0)
        val sets = listOf(
            createSet(1, 110.0, 1),
            createSet(2, 100.0, 1)
        )
        val map = mapOf(
            1L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_CHEST, VolumeCalculator.MuscleRole.PRIMARY)),
            2L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BACK, VolumeCalculator.MuscleRole.PRIMARY))
        )

        val report = analyzer.analyzeBalance(sets, map)

        assertEquals(BalanceStatus.MODERATE_IMBALANCE, report.pushPullRatio.status)

        val prescription = report.correctivePrescriptions.find { it.targetMuscle == "Pull" }
        assertTrue(prescription != null)
        assertTrue(prescription?.exerciseName?.contains("Face Pulls") == true)
    }

    @Test
    fun `test tricep-heavy detection`() {
        // Triceps 110, Biceps 100 (Ratio 100/110 = 0.909) - Wait, Bi/Tri ratio.
        // Biceps 100, Triceps 110. Ratio: 100/110 = 0.909.
        // Range is 0.85 to 1.05. This is optimal. Let's make it 100/120 = 0.83 (Imbalance)
        val sets = listOf(
            createSet(1, 100.0, 1),
            createSet(2, 120.0, 1)
        )
        val map = mapOf(
            1L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BICEPS, VolumeCalculator.MuscleRole.PRIMARY)),
            2L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_TRICEPS, VolumeCalculator.MuscleRole.PRIMARY))
        )

        val report = analyzer.analyzeBalance(sets, map)

        assertEquals(BalanceStatus.MODERATE_IMBALANCE, report.bicepsTricepsRatio.status)

        val prescription = report.correctivePrescriptions.find { it.targetMuscle == "Biceps" }
        assertTrue(prescription != null)
        assertTrue(prescription?.exerciseName?.contains("Biceps accessory") == true)
    }

    @Test
    fun `test empty and zero volume`() {
        val report = analyzer.analyzeBalance(emptyList(), emptyMap())

        assertEquals(100, report.overallBalanceScore)
        assertEquals(BalanceStatus.OPTIMAL, report.pushPullRatio.status)
        assertEquals(BalanceStatus.OPTIMAL, report.quadHamstringRatio.status)
        assertEquals(BalanceStatus.OPTIMAL, report.bicepsTricepsRatio.status)
    }

    @Test
    fun `test severe imbalance scoring`() {
        // Quad 200, Ham 50 -> ratio 4.0
        val sets = listOf(
            createSet(1, 200.0, 1),
            createSet(2, 50.0, 1)
        )
        val map = mapOf(
            1L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_QUADRICEPS, VolumeCalculator.MuscleRole.PRIMARY)),
            2L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_HAMSTRINGS, VolumeCalculator.MuscleRole.PRIMARY))
        )

        val report = analyzer.analyzeBalance(sets, map)
        assertEquals(BalanceStatus.SEVERE_IMBALANCE, report.quadHamstringRatio.status)
        assertEquals(80, report.overallBalanceScore) // 100 - 20
    }

    private fun createSet(exerciseId: Long, weight: Double, reps: Int): VolumeCalculator.SetWithContext {
        return VolumeCalculator.SetWithContext(
            set = WorkoutSetEntity(
                id = 0,
                workoutExerciseId = 0,
                setNumber = 1,
                weight = weight,
                reps = reps,
                rpe = 8.0,
                restSeconds = 60,
                completed = true,
                setType = 0
            ),
            exerciseId = exerciseId,
            workoutDate = System.currentTimeMillis()
        )
    }
}
