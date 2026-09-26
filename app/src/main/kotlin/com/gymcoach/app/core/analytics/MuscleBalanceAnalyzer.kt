package com.gymcoach.app.core.analytics

import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

enum class BalanceStatus {
    OPTIMAL, MODERATE_IMBALANCE, SEVERE_IMBALANCE
}

data class MuscleRatio(
    val ratioName: String,
    val agonistName: String,
    val antagonistName: String,
    val agonistVolumeKg: Double,
    val antagonistVolumeKg: Double,
    val ratio: Double,
    val status: BalanceStatus,
    val optimalRange: ClosedFloatingPointRange<Double>,
    val statusSummary: String
)

data class CorrectivePrescription(
    val exerciseId: Long?,
    val exerciseName: String,
    val targetMuscle: String,
    val sets: Int,
    val reps: String,
    val rationale: String
)

data class MuscleBalanceReport(
    val overallBalanceScore: Int,
    val pushPullRatio: MuscleRatio,
    val quadHamstringRatio: MuscleRatio,
    val bicepsTricepsRatio: MuscleRatio,
    val correctivePrescriptions: List<CorrectivePrescription>
)

@Singleton
class MuscleBalanceAnalyzer @Inject constructor() {

    fun analyzeBalance(
        completedSets: List<VolumeCalculator.SetWithContext>,
        exerciseMuscleMap: Map<Long, List<VolumeCalculator.MuscleAssignment>>
    ): MuscleBalanceReport {
        var pushVolume = 0.0
        var pullVolume = 0.0
        var quadVolume = 0.0
        var hamVolume = 0.0
        var bicepsVolume = 0.0
        var tricepsVolume = 0.0

        for (ctx in completedSets) {
            if (!ctx.set.completed || ctx.set.setType != 0) continue

            val weight = if (ctx.set.weight > 0) ctx.set.weight else 1.0 // Use 1.0 for bodyweight reps if weight is 0
            val volume = weight * ctx.set.reps

            val assignments = exerciseMuscleMap[ctx.exerciseId] ?: continue
            for (assignment in assignments) {
                // Apply role credit
                val effectiveVolume = volume * assignment.role.credit

                when (assignment.muscleName) {
                    VolumeCalculator.MUSCLE_CHEST, "Anterior Deltoid" -> pushVolume += effectiveVolume
                    VolumeCalculator.MUSCLE_BACK, "Lats", VolumeCalculator.MUSCLE_UPPER_BACK, VolumeCalculator.MUSCLE_REAR_DELT -> pullVolume += effectiveVolume
                    VolumeCalculator.MUSCLE_QUADRICEPS -> quadVolume += effectiveVolume
                    VolumeCalculator.MUSCLE_HAMSTRINGS, VolumeCalculator.MUSCLE_GLUTES -> hamVolume += effectiveVolume
                    VolumeCalculator.MUSCLE_BICEPS -> bicepsVolume += effectiveVolume
                    VolumeCalculator.MUSCLE_TRICEPS -> tricepsVolume += effectiveVolume
                }
            }
        }

        val pushPull = calculateRatio(
            "Push / Pull", "Push", "Pull", pushVolume, pullVolume, 0.80..1.00
        )
        val quadHam = calculateRatio(
            "Quad / Hamstring", "Quad", "Hamstring", quadVolume, hamVolume, 0.90..1.15
        )
        val biTri = calculateRatio(
            "Biceps / Triceps", "Biceps", "Triceps", bicepsVolume, tricepsVolume, 0.85..1.05
        )

        val prescriptions = mutableListOf<CorrectivePrescription>()

        if (quadHam.ratio > 1.15) {
            prescriptions.add(
                CorrectivePrescription(
                    exerciseId = null,
                    exerciseName = "Nordic Hamstring Curls or Romanian Deadlift",
                    targetMuscle = "Hamstrings",
                    sets = 3,
                    reps = "8-12",
                    rationale = "Quad dominant ratio (${"%.2f".format(quadHam.ratio)}) increases ACL risk. Add eccentric hamstring overload."
                )
            )
        }

        if (pushPull.ratio > 1.00 || pushPull.ratio < 0.80) {
             if (pushPull.ratio > 1.0) {
                prescriptions.add(
                    CorrectivePrescription(
                        exerciseId = null,
                        exerciseName = "Face Pulls with External Rotation or Chest Supported Row",
                        targetMuscle = "Pull",
                        sets = 3,
                        reps = "12-15",
                        rationale = "Push dominant ratio (${"%.2f".format(pushPull.ratio)}). Add pulling volume to protect shoulder rotator cuff and posture."
                    )
                )
             } else if (pushPull.ratio < 0.8) {
                 // Pull dominant ratio: encourage balanced horizontal and incline pressing volume
                 prescriptions.add(
                     CorrectivePrescription(
                         exerciseId = null,
                         exerciseName = "Incline Dumbbell Press",
                         targetMuscle = "Push",
                         sets = 3,
                         reps = "8-12",
                         rationale = "Pull dominant ratio (${"%.2f".format(pushPull.ratio)}). Consider adding push volume."
                     )
                 )
             }
        }

        if (biTri.ratio > 1.05 || biTri.ratio < 0.85) {
            val focus = if (biTri.ratio > 1.05) "Triceps" else "Biceps"
            prescriptions.add(
                CorrectivePrescription(
                    exerciseId = null,
                    exerciseName = "$focus accessory",
                    targetMuscle = focus,
                    sets = 3,
                    reps = "10-15",
                    rationale = "Arm imbalance (${"%.2f".format(biTri.ratio)}). Add targeted $focus accessory."
                )
            )
        }

        var totalScore = 100
        val ratios = listOf(pushPull, quadHam, biTri)
        for (r in ratios) {
            if (r.status == BalanceStatus.MODERATE_IMBALANCE) totalScore -= 10
            if (r.status == BalanceStatus.SEVERE_IMBALANCE) totalScore -= 20
        }
        if (totalScore < 0) totalScore = 0

        return MuscleBalanceReport(
            overallBalanceScore = totalScore,
            pushPullRatio = pushPull,
            quadHamstringRatio = quadHam,
            bicepsTricepsRatio = biTri,
            correctivePrescriptions = prescriptions
        )
    }

    private fun calculateRatio(
        name: String,
        agonist: String,
        antagonist: String,
        agVol: Double,
        antVol: Double,
        optimalRange: ClosedFloatingPointRange<Double>
    ): MuscleRatio {
        val ratio = if (antVol > 0) agVol / antVol else if (agVol > 0) Double.MAX_VALUE else 1.0

        val severeLower = optimalRange.start * 0.8
        val severeUpper = optimalRange.endInclusive * 1.2
        val epsilon = 1e-6

        val status = when {
            ratio == 1.0 && agVol == 0.0 -> BalanceStatus.OPTIMAL // No data
            ratio in optimalRange -> BalanceStatus.OPTIMAL
            ratio < severeLower - epsilon || ratio > severeUpper + epsilon -> BalanceStatus.SEVERE_IMBALANCE
            else -> BalanceStatus.MODERATE_IMBALANCE
        }

        val summary = when (status) {
            BalanceStatus.OPTIMAL -> "Optimal"
            BalanceStatus.MODERATE_IMBALANCE -> "Moderate Imbalance"
            BalanceStatus.SEVERE_IMBALANCE -> "Severe Imbalance"
        }

        return MuscleRatio(
            ratioName = name,
            agonistName = agonist,
            antagonistName = antagonist,
            agonistVolumeKg = agVol,
            antagonistVolumeKg = antVol,
            ratio = if (ratio == Double.MAX_VALUE) 99.9 else ratio,
            status = status,
            optimalRange = optimalRange,
            statusSummary = summary
        )
    }
}
