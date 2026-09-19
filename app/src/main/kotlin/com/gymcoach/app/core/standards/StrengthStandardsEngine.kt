package com.gymcoach.app.core.standards

import javax.inject.Inject
import kotlin.math.roundToInt

enum class StrengthTier {
    UNTRAINED, NOVICE, INTERMEDIATE, ADVANCED, ELITE
}

data class LiftStandard(
    val exerciseName: String,
    val current1RmKg: Double,
    val bodyweightRatio: Double,
    val tier: StrengthTier,
    val nextTier: StrengthTier?,
    val nextTierKg: Double?,
    val progressToNextTierPct: Int
)

data class OverallStrengthProfile(
    val bodyweightKg: Double,
    val liftStandards: List<LiftStandard>,
    val overallTier: StrengthTier,
    val totalBig4Kg: Double,
    val percentile: Int, // e.g. 85 for top 15%
    val weakestLift: String?,
    val strongestLift: String?,
    val coachingRecommendation: String
)

class StrengthStandardsEngine @Inject constructor() {

    // Bodyweight multipliers for each tier: [Novice, Intermediate, Advanced, Elite]
    // Values less than the Novice multiplier are Untrained.
    private val multipliers = mapOf(
        "Bench Press" to listOf(0.6, 0.9, 1.3, 1.75),
        "Squat" to listOf(0.8, 1.2, 1.75, 2.25),
        "Deadlift" to listOf(1.0, 1.5, 2.2, 2.75),
        "Overhead Press" to listOf(0.4, 0.6, 0.85, 1.1)
    )

    fun evaluateProfile(bodyweightKg: Double, oneRepMaxes: Map<String, Double>): OverallStrengthProfile {
        if (bodyweightKg <= 0.0) {
            return createEmptyProfile(bodyweightKg, "Bodyweight must be greater than 0 to evaluate strength standards.")
        }

        val liftStandards = mutableListOf<LiftStandard>()
        var totalBig4Kg = 0.0
        var totalTierValue = 0

        val targetLifts = listOf("Squat", "Bench Press", "Deadlift", "Overhead Press")
        var allLiftsPresent = true

        for (liftName in targetLifts) {
            val oneRepMax = oneRepMaxes[liftName] ?: 0.0
            val bodyweightRatio = if (bodyweightKg > 0) oneRepMax / bodyweightKg else 0.0
            val liftMultipliers = multipliers[liftName] ?: emptyList()

            var tier = StrengthTier.UNTRAINED
            var nextTier: StrengthTier? = StrengthTier.NOVICE
            var nextTierMultiplier: Double? = liftMultipliers.getOrNull(0)
            var currentTierMultiplier = 0.0

            if (liftMultipliers.isNotEmpty()) {
                if (bodyweightRatio >= liftMultipliers[3]) {
                    tier = StrengthTier.ELITE
                    nextTier = null
                    nextTierMultiplier = null
                    currentTierMultiplier = liftMultipliers[3]
                } else if (bodyweightRatio >= liftMultipliers[2]) {
                    tier = StrengthTier.ADVANCED
                    nextTier = StrengthTier.ELITE
                    nextTierMultiplier = liftMultipliers[3]
                    currentTierMultiplier = liftMultipliers[2]
                } else if (bodyweightRatio >= liftMultipliers[1]) {
                    tier = StrengthTier.INTERMEDIATE
                    nextTier = StrengthTier.ADVANCED
                    nextTierMultiplier = liftMultipliers[2]
                    currentTierMultiplier = liftMultipliers[1]
                } else if (bodyweightRatio >= liftMultipliers[0]) {
                    tier = StrengthTier.NOVICE
                    nextTier = StrengthTier.INTERMEDIATE
                    nextTierMultiplier = liftMultipliers[1]
                    currentTierMultiplier = liftMultipliers[0]
                }
            }

            val nextTierKg = nextTierMultiplier?.times(bodyweightKg)

            val progressToNextTierPct = if (nextTierKg != null && nextTierMultiplier != null) {
                val currentTierKg = currentTierMultiplier * bodyweightKg
                val range = nextTierKg - currentTierKg
                val progress = oneRepMax - currentTierKg
                if (range > 0) {
                    ((progress / range) * 100).roundToInt().coerceIn(0, 100)
                } else {
                    100
                }
            } else {
                100
            }

            liftStandards.add(
                LiftStandard(
                    exerciseName = liftName,
                    current1RmKg = oneRepMax,
                    bodyweightRatio = bodyweightRatio,
                    tier = tier,
                    nextTier = nextTier,
                    nextTierKg = nextTierKg,
                    progressToNextTierPct = progressToNextTierPct
                )
            )

            totalBig4Kg += oneRepMax
            totalTierValue += tier.ordinal
            if (oneRepMax == 0.0) {
                allLiftsPresent = false
            }
        }

        val overallTier = if (allLiftsPresent) {
            StrengthTier.values()[(totalTierValue / 4.0).roundToInt().coerceIn(0, StrengthTier.values().size - 1)]
        } else {
            StrengthTier.UNTRAINED
        }

        val strongestLift = if (allLiftsPresent) liftStandards.maxByOrNull { it.tier.ordinal * 100 + it.progressToNextTierPct }?.exerciseName else null
        val weakestLift = if (allLiftsPresent) liftStandards.minByOrNull { it.tier.ordinal * 100 + it.progressToNextTierPct }?.exerciseName else null

        val percentile = calculatePercentile(overallTier, totalTierValue)

        val recommendation = if (!allLiftsPresent) {
            "Record a 1RM for all Big 4 lifts (Squat, Bench Press, Deadlift, Overhead Press) to get a complete profile."
        } else if (weakestLift != null) {
            "Your $weakestLift is currently lagging behind. Consider adding more volume or frequency to improve it."
        } else {
            "Great balanced strength profile! Keep applying progressive overload."
        }

        return OverallStrengthProfile(
            bodyweightKg = bodyweightKg,
            liftStandards = liftStandards,
            overallTier = overallTier,
            totalBig4Kg = totalBig4Kg,
            percentile = percentile,
            weakestLift = weakestLift,
            strongestLift = strongestLift,
            coachingRecommendation = recommendation
        )
    }

    private fun calculatePercentile(overallTier: StrengthTier, totalTierValue: Int): Int {
        // Simple heuristic for percentile based on total tier value (max 16)
        // Untrained: 0-20%
        // Novice: 20-50%
        // Intermediate: 50-80%
        // Advanced: 80-95%
        // Elite: 95-99%
        val maxScore = 16.0
        val basePercentile = (totalTierValue / maxScore) * 100

        return when (overallTier) {
            StrengthTier.UNTRAINED -> basePercentile.coerceIn(0.0, 20.0).roundToInt()
            StrengthTier.NOVICE -> basePercentile.coerceIn(20.0, 50.0).roundToInt()
            StrengthTier.INTERMEDIATE -> basePercentile.coerceIn(50.0, 80.0).roundToInt()
            StrengthTier.ADVANCED -> basePercentile.coerceIn(80.0, 95.0).roundToInt()
            StrengthTier.ELITE -> basePercentile.coerceIn(95.0, 99.0).roundToInt()
        }
    }

    private fun createEmptyProfile(bodyweightKg: Double, message: String): OverallStrengthProfile {
        return OverallStrengthProfile(
            bodyweightKg = bodyweightKg,
            liftStandards = emptyList(),
            overallTier = StrengthTier.UNTRAINED,
            totalBig4Kg = 0.0,
            percentile = 0,
            weakestLift = null,
            strongestLift = null,
            coachingRecommendation = message
        )
    }
}
