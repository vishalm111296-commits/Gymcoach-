package com.gymcoach.app.core.progression

import kotlin.math.round

/**
 * 1-Repetition Maximum (1RM) and Strength Standards Progression Engine.
 * Evaluates estimated 1RM using leading scientific formulas (Epley, Brzycki, Lombardi, Mayhew, Wathen)
 * and generates percentage-based training loads (e.g., hypertrophy, strength, power).
 */
object OneRepMaxCalculator {

    enum class Formula {
        EPLEY,
        BRZYCKI,
        LOMBARDI,
        MAYHEW,
        WATHEN,
        AVERAGE
    }

    data class TrainingZone(
        val percentage: Int,
        val weight: Double,
        val repRange: String,
        val trainingGoal: String
    )

    data class OneRepMaxProfile(
        val weight: Double,
        val reps: Int,
        val epley1RM: Double,
        val brzycki1RM: Double,
        val lombardi1RM: Double,
        val mayhew1RM: Double,
        val wathen1RM: Double,
        val average1RM: Double,
        val zones: List<TrainingZone>
    )

    /**
     * Standard training intensity percentages used in periodized strength training.
     */
    val STANDARD_ZONES = listOf(
        Triple(95, "1-2", "Maximal Strength & Neural Drive"),
        Triple(90, "3-4", "Heavy Strength"),
        Triple(85, "5-6", "Strength & High-Threshold Motor Units"),
        Triple(80, "7-8", "Strength-Hypertrophy"),
        Triple(75, "9-10", "Hypertrophy Zone"),
        Triple(70, "11-12", "Hypertrophy & Muscular Endurance"),
        Triple(65, "13-15", "Endurance & Conditioning"),
        Triple(60, "16-20", "Active Recovery / Technique")
    )

    /**
     * Calculate 1RM using Epley formula: w * (1 + r / 30)
     */
    fun epley(weight: Double, reps: Int): Double {
        if (weight <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return weight
        return weight * (1.0 + reps.coerceAtMost(15) / 30.0)
    }

    /**
     * Calculate 1RM using Brzycki formula: w * (36 / (37 - r))
     */
    fun brzycki(weight: Double, reps: Int): Double {
        if (weight <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return weight
        val clampedReps = reps.coerceIn(1, 15)
        return weight * (36.0 / (37.0 - clampedReps))
    }

    /**
     * Calculate 1RM using Lombardi formula: w * (r ^ 0.10)
     */
    fun lombardi(weight: Double, reps: Int): Double {
        if (weight <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return weight
        return weight * Math.pow(reps.coerceAtMost(15).toDouble(), 0.10)
    }

    /**
     * Calculate 1RM using Mayhew et al. formula: (100 * w) / (52.2 + (41.9 * e^(-0.055 * r)))
     */
    fun mayhew(weight: Double, reps: Int): Double {
        if (weight <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return weight
        val clampedReps = reps.coerceAtMost(15).toDouble()
        val denom = 52.2 + (41.9 * Math.exp(-0.055 * clampedReps))
        return (100.0 * weight) / denom
    }

    /**
     * Calculate 1RM using Wathen formula: (100 * w) / (48.8 + (53.8 * e^(-0.075 * r)))
     */
    fun wathen(weight: Double, reps: Int): Double {
        if (weight <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return weight
        val clampedReps = reps.coerceAtMost(15).toDouble()
        val denom = 48.8 + (53.8 * Math.exp(-0.075 * clampedReps))
        return (100.0 * weight) / denom
    }

    /**
     * Generate complete 1RM profile with cross-formula analysis and training zones.
     */
    fun calculateProfile(
        weight: Double,
        reps: Int,
        plateStep: Double = 2.5
    ): OneRepMaxProfile {
        if (weight <= 0.0 || reps <= 0) {
            return OneRepMaxProfile(
                weight = 0.0,
                reps = 0,
                epley1RM = 0.0,
                brzycki1RM = 0.0,
                lombardi1RM = 0.0,
                mayhew1RM = 0.0,
                wathen1RM = 0.0,
                average1RM = 0.0,
                zones = emptyList()
            )
        }

        val e = round(epley(weight, reps) * 10.0) / 10.0
        val b = round(brzycki(weight, reps) * 10.0) / 10.0
        val l = round(lombardi(weight, reps) * 10.0) / 10.0
        val m = round(mayhew(weight, reps) * 10.0) / 10.0
        val w = round(wathen(weight, reps) * 10.0) / 10.0
        val avg = round(((e + b + l + m + w) / 5.0) * 10.0) / 10.0

        val base1RM = avg

        val zones = STANDARD_ZONES.map { (pct, repRange, goal) ->
            val targetWeight = base1RM * (pct / 100.0)
            val roundedWeight = round(targetWeight / plateStep) * plateStep
            TrainingZone(
                percentage = pct,
                weight = roundedWeight,
                repRange = repRange,
                trainingGoal = goal
            )
        }

        return OneRepMaxProfile(
            weight = weight,
            reps = reps,
            epley1RM = e,
            brzycki1RM = b,
            lombardi1RM = l,
            mayhew1RM = m,
            wathen1RM = w,
            average1RM = avg,
            zones = zones
        )
    }
}
