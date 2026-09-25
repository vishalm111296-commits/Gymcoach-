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

    @Test
    fun `test push pull optimal boundaries`() {
        // Lower optimal boundary: 80 / 100 = 0.80
        val lowerSets = listOf(
            createSet(1, 80.0, 1),
            createSet(2, 100.0, 1)
        )
        val pushPullMap = mapOf(
            1L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_CHEST, VolumeCalculator.MuscleRole.PRIMARY)),
            2L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BACK, VolumeCalculator.MuscleRole.PRIMARY))
        )
        val lowerReport = analyzer.analyzeBalance(lowerSets, pushPullMap)
        assertEquals(BalanceStatus.OPTIMAL, lowerReport.pushPullRatio.status)

        // Upper optimal boundary: 100 / 100 = 1.00
        val upperSets = listOf(
            createSet(1, 100.0, 1),
            createSet(2, 100.0, 1)
        )
        val upperReport = analyzer.analyzeBalance(upperSets, pushPullMap)
        assertEquals(BalanceStatus.OPTIMAL, upperReport.pushPullRatio.status)
    }

    @Test
    fun `test pull dominant moderate and severe imbalances`() {
        val map = mapOf(
            1L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_CHEST, VolumeCalculator.MuscleRole.PRIMARY)),
            2L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BACK, VolumeCalculator.MuscleRole.PRIMARY))
        )

        // Moderate pull dominance: 70 / 100 = 0.70 (0.64 <= ratio < 0.80)
        val modSets = listOf(
            createSet(1, 70.0, 1),
            createSet(2, 100.0, 1)
        )
        val modReport = analyzer.analyzeBalance(modSets, map)
        assertEquals(BalanceStatus.MODERATE_IMBALANCE, modReport.pushPullRatio.status)
        val modPrescription = modReport.correctivePrescriptions.find { it.targetMuscle == "Push" }
        assertTrue(modPrescription != null)
        assertTrue(modPrescription?.exerciseName?.contains("Incline Dumbbell Press") == true)

        // Severe pull dominance: 60 / 100 = 0.60 (< 0.64)
        val sevSets = listOf(
            createSet(1, 60.0, 1),
            createSet(2, 100.0, 1)
        )
        val sevReport = analyzer.analyzeBalance(sevSets, map)
        assertEquals(BalanceStatus.SEVERE_IMBALANCE, sevReport.pushPullRatio.status)
    }

    @Test
    fun `test push dominant severe imbalance`() {
        // Push 125, Pull 100 -> ratio 1.25 > 1.20 (1.00 * 1.2)
        val sets = listOf(
            createSet(1, 125.0, 1),
            createSet(2, 100.0, 1)
        )
        val map = mapOf(
            1L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_CHEST, VolumeCalculator.MuscleRole.PRIMARY)),
            2L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BACK, VolumeCalculator.MuscleRole.PRIMARY))
        )
        val report = analyzer.analyzeBalance(sets, map)
        assertEquals(BalanceStatus.SEVERE_IMBALANCE, report.pushPullRatio.status)
    }

    @Test
    fun `test quad hamstring optimal boundaries and hamstring heavy imbalances`() {
        val map = mapOf(
            1L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_QUADRICEPS, VolumeCalculator.MuscleRole.PRIMARY)),
            2L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_HAMSTRINGS, VolumeCalculator.MuscleRole.PRIMARY))
        )

        // Lower optimal boundary: 90 / 100 = 0.90
        val lowerSets = listOf(
            createSet(1, 90.0, 1),
            createSet(2, 100.0, 1)
        )
        val lowerReport = analyzer.analyzeBalance(lowerSets, map)
        assertEquals(BalanceStatus.OPTIMAL, lowerReport.quadHamstringRatio.status)

        // Upper optimal boundary: 115 / 100 = 1.15
        val upperSets = listOf(
            createSet(1, 115.0, 1),
            createSet(2, 100.0, 1)
        )
        val upperReport = analyzer.analyzeBalance(upperSets, map)
        assertEquals(BalanceStatus.OPTIMAL, upperReport.quadHamstringRatio.status)

        // Hamstring heavy moderate: 80 / 100 = 0.80 (0.72 <= ratio < 0.90)
        val modHamSets = listOf(
            createSet(1, 80.0, 1),
            createSet(2, 100.0, 1)
        )
        val modHamReport = analyzer.analyzeBalance(modHamSets, map)
        assertEquals(BalanceStatus.MODERATE_IMBALANCE, modHamReport.quadHamstringRatio.status)

        // Hamstring heavy severe: 70 / 100 = 0.70 (< 0.72)
        val sevHamSets = listOf(
            createSet(1, 70.0, 1),
            createSet(2, 100.0, 1)
        )
        val sevHamReport = analyzer.analyzeBalance(sevHamSets, map)
        assertEquals(BalanceStatus.SEVERE_IMBALANCE, sevHamReport.quadHamstringRatio.status)
    }

    @Test
    fun `test biceps triceps optimal boundaries and bicep heavy imbalances`() {
        val map = mapOf(
            1L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BICEPS, VolumeCalculator.MuscleRole.PRIMARY)),
            2L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_TRICEPS, VolumeCalculator.MuscleRole.PRIMARY))
        )

        // Lower optimal boundary: 85 / 100 = 0.85
        val lowerSets = listOf(
            createSet(1, 85.0, 1),
            createSet(2, 100.0, 1)
        )
        assertEquals(BalanceStatus.OPTIMAL, analyzer.analyzeBalance(lowerSets, map).bicepsTricepsRatio.status)

        // Upper optimal boundary: 105 / 100 = 1.05
        val upperSets = listOf(
            createSet(1, 105.0, 1),
            createSet(2, 100.0, 1)
        )
        assertEquals(BalanceStatus.OPTIMAL, analyzer.analyzeBalance(upperSets, map).bicepsTricepsRatio.status)

        // Biceps heavy moderate: 115 / 100 = 1.15 (> 1.05, <= 1.26)
        val modBiSets = listOf(
            createSet(1, 115.0, 1),
            createSet(2, 100.0, 1)
        )
        val modBiReport = analyzer.analyzeBalance(modBiSets, map)
        assertEquals(BalanceStatus.MODERATE_IMBALANCE, modBiReport.bicepsTricepsRatio.status)
        val triPrescription = modBiReport.correctivePrescriptions.find { it.targetMuscle == "Triceps" }
        assertTrue(triPrescription != null)
        assertTrue(triPrescription?.exerciseName?.contains("Triceps accessory") == true)

        // Biceps heavy severe: 130 / 100 = 1.30 (> 1.26)
        val sevBiSets = listOf(
            createSet(1, 130.0, 1),
            createSet(2, 100.0, 1)
        )
        val sevBiReport = analyzer.analyzeBalance(sevBiSets, map)
        assertEquals(BalanceStatus.SEVERE_IMBALANCE, sevBiReport.bicepsTricepsRatio.status)
    }

    @Test
    fun `test single muscle dominance with zero antagonist volume`() {
        // Push only (Pull = 0)
        val pushOnlySets = listOf(createSet(1, 100.0, 1))
        val pushOnlyMap = mapOf(1L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_CHEST, VolumeCalculator.MuscleRole.PRIMARY)))
        val pushOnlyReport = analyzer.analyzeBalance(pushOnlySets, pushOnlyMap)
        assertEquals(99.9, pushOnlyReport.pushPullRatio.ratio, 0.001)
        assertEquals(BalanceStatus.SEVERE_IMBALANCE, pushOnlyReport.pushPullRatio.status)

        // Pull only (Push = 0)
        val pullOnlySets = listOf(createSet(2, 100.0, 1))
        val pullOnlyMap = mapOf(2L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BACK, VolumeCalculator.MuscleRole.PRIMARY)))
        val pullOnlyReport = analyzer.analyzeBalance(pullOnlySets, pullOnlyMap)
        assertEquals(0.0, pullOnlyReport.pushPullRatio.ratio, 0.001)
        assertEquals(BalanceStatus.SEVERE_IMBALANCE, pullOnlyReport.pushPullRatio.status)

        // Quad only (Ham = 0)
        val quadOnlySets = listOf(createSet(3, 100.0, 1))
        val quadOnlyMap = mapOf(3L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_QUADRICEPS, VolumeCalculator.MuscleRole.PRIMARY)))
        val quadOnlyReport = analyzer.analyzeBalance(quadOnlySets, quadOnlyMap)
        assertEquals(99.9, quadOnlyReport.quadHamstringRatio.ratio, 0.001)
        assertEquals(BalanceStatus.SEVERE_IMBALANCE, quadOnlyReport.quadHamstringRatio.status)
    }

    @Test
    fun `test set filtering ignores warmup and incomplete sets`() {
        val sets = listOf(
            // Incomplete set should be ignored
            VolumeCalculator.SetWithContext(
                set = WorkoutSetEntity(id = 1, workoutExerciseId = 1, setNumber = 1, weight = 100.0, reps = 10, rpe = 8.0, restSeconds = 60, completed = false, setType = 0),
                exerciseId = 1L,
                workoutDate = System.currentTimeMillis()
            ),
            // Warmup set (setType = 1) should be ignored
            VolumeCalculator.SetWithContext(
                set = WorkoutSetEntity(id = 2, workoutExerciseId = 1, setNumber = 2, weight = 100.0, reps = 10, rpe = 8.0, restSeconds = 60, completed = true, setType = 1),
                exerciseId = 1L,
                workoutDate = System.currentTimeMillis()
            ),
            // Completed normal set should be counted
            createSet(1, 50.0, 2) // 100 kg volume
        )
        val map = mapOf(
            1L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_CHEST, VolumeCalculator.MuscleRole.PRIMARY))
        )
        val report = analyzer.analyzeBalance(sets, map)
        assertEquals(100.0, report.pushPullRatio.agonistVolumeKg, 0.001)
    }

    @Test
    fun `test secondary muscle role credit and bodyweight volume fallback`() {
        // Exercise 1: Secondary chest (0.5 credit)
        // Exercise 2: Bodyweight pullups (weight = 0.0, reps = 10 -> fallback to 1.0 kg * 10 reps = 10 kg)
        val sets = listOf(
            VolumeCalculator.SetWithContext(
                set = WorkoutSetEntity(id = 1, workoutExerciseId = 1, setNumber = 1, weight = 100.0, reps = 10, rpe = 8.0, restSeconds = 60, completed = true, setType = 0),
                exerciseId = 1L,
                workoutDate = System.currentTimeMillis()
            ),
            VolumeCalculator.SetWithContext(
                set = WorkoutSetEntity(id = 2, workoutExerciseId = 2, setNumber = 1, weight = 0.0, reps = 10, rpe = 8.0, restSeconds = 60, completed = true, setType = 0),
                exerciseId = 2L,
                workoutDate = System.currentTimeMillis()
            )
        )
        val map = mapOf(
            1L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_CHEST, VolumeCalculator.MuscleRole.SECONDARY)),
            2L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BACK, VolumeCalculator.MuscleRole.PRIMARY))
        )
        val report = analyzer.analyzeBalance(sets, map)

        // 100 * 10 * 0.5 = 500.0
        assertEquals(500.0, report.pushPullRatio.agonistVolumeKg, 0.001)
        // 1.0 * 10 * 1.0 = 10.0
        assertEquals(10.0, report.pushPullRatio.antagonistVolumeKg, 0.001)
    }

    @Test
    fun `test overall balance score compounding with 3 severe imbalances`() {
        // Only Push, Quad, Biceps (all antagonists 0 -> 3 SEVERE_IMBALANCE -> 100 - 3*20 = 40)
        val sets = listOf(
            createSet(1, 100.0, 1),
            createSet(2, 100.0, 1),
            createSet(3, 100.0, 1)
        )
        val map = mapOf(
            1L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_CHEST, VolumeCalculator.MuscleRole.PRIMARY)),
            2L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_QUADRICEPS, VolumeCalculator.MuscleRole.PRIMARY)),
            3L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BICEPS, VolumeCalculator.MuscleRole.PRIMARY))
        )
        val report = analyzer.analyzeBalance(sets, map)
        assertEquals(BalanceStatus.SEVERE_IMBALANCE, report.pushPullRatio.status)
        assertEquals(BalanceStatus.SEVERE_IMBALANCE, report.quadHamstringRatio.status)
        assertEquals(BalanceStatus.SEVERE_IMBALANCE, report.bicepsTricepsRatio.status)
        assertEquals(40, report.overallBalanceScore)
    }

    @Test
    fun `test exact ratio boundary thresholds for quad-hamstring and push-pull`() {
        // Quad / Hamstring optimal range: 0.90..1.15
        // Severe threshold upper: 1.15 * 1.2 = 1.38
        // Test 1.15 exactly -> OPTIMAL
        val setsOptimal = listOf(createSet(1, 115.0, 1), createSet(2, 100.0, 1))
        val mapQuadHam = mapOf(
            1L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_QUADRICEPS, VolumeCalculator.MuscleRole.PRIMARY)),
            2L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_HAMSTRINGS, VolumeCalculator.MuscleRole.PRIMARY))
        )
        val reportOptimal = analyzer.analyzeBalance(setsOptimal, mapQuadHam)
        assertEquals(BalanceStatus.OPTIMAL, reportOptimal.quadHamstringRatio.status)

        // Test 1.38 exactly -> MODERATE_IMBALANCE
        val setsModerateUpper = listOf(createSet(1, 138.0, 1), createSet(2, 100.0, 1))
        val reportModUpper = analyzer.analyzeBalance(setsModerateUpper, mapQuadHam)
        assertEquals(BalanceStatus.MODERATE_IMBALANCE, reportModUpper.quadHamstringRatio.status)

        // Test 1.39 -> SEVERE_IMBALANCE
        val setsSevereUpper = listOf(createSet(1, 139.0, 1), createSet(2, 100.0, 1))
        val reportSevUpper = analyzer.analyzeBalance(setsSevereUpper, mapQuadHam)
        assertEquals(BalanceStatus.SEVERE_IMBALANCE, reportSevUpper.quadHamstringRatio.status)

        // Lower boundary: 0.90 * 0.8 = 0.72
        // Test 0.75 -> MODERATE_IMBALANCE (between 0.72 and 0.90)
        val setsModerateLower = listOf(createSet(1, 75.0, 1), createSet(2, 100.0, 1))
        val reportModLower = analyzer.analyzeBalance(setsModerateLower, mapQuadHam)
        assertEquals(BalanceStatus.MODERATE_IMBALANCE, reportModLower.quadHamstringRatio.status)

        // Test 0.70 -> SEVERE_IMBALANCE (< 0.72)
        val setsSevereLower = listOf(createSet(1, 70.0, 1), createSet(2, 100.0, 1))
        val reportSevLower = analyzer.analyzeBalance(setsSevereLower, mapQuadHam)
        assertEquals(BalanceStatus.SEVERE_IMBALANCE, reportSevLower.quadHamstringRatio.status)
    }

    @Test
    fun `test multi-imbalance generates concurrent prescriptions for each target muscle`() {
        // Push-heavy (Push 150, Pull 100 -> ratio 1.5 > 1.0 -> Pull prescription)
        // Quad-heavy (Quad 150, Ham 100 -> ratio 1.5 > 1.15 -> Hamstrings prescription)
        // Biceps-heavy (Bi 150, Tri 100 -> ratio 1.5 > 1.05 -> Triceps prescription)
        val sets = listOf(
            createSet(1, 150.0, 1), createSet(2, 100.0, 1),
            createSet(3, 150.0, 1), createSet(4, 100.0, 1),
            createSet(5, 150.0, 1), createSet(6, 100.0, 1)
        )
        val map = mapOf(
            1L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_CHEST, VolumeCalculator.MuscleRole.PRIMARY)),
            2L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BACK, VolumeCalculator.MuscleRole.PRIMARY)),
            3L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_QUADRICEPS, VolumeCalculator.MuscleRole.PRIMARY)),
            4L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_HAMSTRINGS, VolumeCalculator.MuscleRole.PRIMARY)),
            5L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_BICEPS, VolumeCalculator.MuscleRole.PRIMARY)),
            6L to listOf(VolumeCalculator.MuscleAssignment(VolumeCalculator.MUSCLE_TRICEPS, VolumeCalculator.MuscleRole.PRIMARY))
        )

        val report = analyzer.analyzeBalance(sets, map)

        assertEquals(3, report.correctivePrescriptions.size)
        val targets = report.correctivePrescriptions.map { it.targetMuscle }.toSet()
        assertEquals(setOf("Hamstrings", "Pull", "Triceps"), targets)
    }

    @Test
    fun `test empty sets list produces perfect score without prescriptions`() {
        val report = analyzer.analyzeBalance(emptyList(), emptyMap())
        assertEquals(100, report.overallBalanceScore)
        assertEquals(BalanceStatus.OPTIMAL, report.pushPullRatio.status)
        assertEquals(BalanceStatus.OPTIMAL, report.quadHamstringRatio.status)
        assertEquals(BalanceStatus.OPTIMAL, report.bicepsTricepsRatio.status)
        assertTrue(report.correctivePrescriptions.isEmpty())
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

