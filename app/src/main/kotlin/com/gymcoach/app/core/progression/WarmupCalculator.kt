package com.gymcoach.app.core.progression

import kotlin.math.round

/**
 * Scientific warm-up protocol calculator for resistance training.
 * Computes progressive ramp-up sets based on target working set weight.
 */
object WarmupCalculator {

    data class WarmupSetProtocol(
        val setNumber: Int,
        val percentage: Double,
        val weight: Double,
        val reps: Int,
        val restSeconds: Int,
        val purpose: String
    )

    data class WarmupPlan(
        val workingWeight: Double,
        val barWeight: Double,
        val sets: List<WarmupSetProtocol>,
        val estimatedDurationMinutes: Int
    )

    /**
     * Calculates progressive warm-up sets based on standard scientific strength training protocols:
     * - Empty Bar / Activation: 10 reps (warm synovial fluid, groove motor pattern)
     * - 50% Working Weight: 5 reps (low neural fatigue, establish bar speed)
     * - 70% Working Weight: 3 reps (primer set)
     * - 85% Working Weight: 1 rep (CNS potentiation, heavy feel without metabolic fatigue)
     * - 92% Working Weight: 1 rep (optional potentiation for heavy loads >= 140kg)
     */
    fun calculateWarmupPlan(
        workingWeight: Double,
        barWeight: Double = 20.0,
        plateStep: Double = 2.5
    ): WarmupPlan {
        if (workingWeight <= barWeight) {
            return WarmupPlan(
                workingWeight = workingWeight,
                barWeight = barWeight,
                sets = listOf(
                    WarmupSetProtocol(
                        setNumber = 1,
                        percentage = 1.0,
                        weight = barWeight,
                        reps = 10,
                        restSeconds = 45,
                        purpose = "Barbell activation & mobility"
                    )
                ),
                estimatedDurationMinutes = 2
            )
        }

        val sets = mutableListOf<WarmupSetProtocol>()
        var setIndex = 1

        // 1. Empty Bar Set
        sets.add(
            WarmupSetProtocol(
                setNumber = setIndex++,
                percentage = round((barWeight / workingWeight) * 100.0) / 100.0,
                weight = barWeight,
                reps = 10,
                restSeconds = 45,
                purpose = "Joint lubrication & motor pattern"
            )
        )

        // 2. 50% Set
        val weight50 = roundToStep(workingWeight * 0.50, barWeight, plateStep)
        if (weight50 > barWeight) {
            sets.add(
                WarmupSetProtocol(
                    setNumber = setIndex++,
                    percentage = 0.50,
                    weight = weight50,
                    reps = 5,
                    restSeconds = 60,
                    purpose = "Speed & positional discipline"
                )
            )
        }

        // 3. 70% Set
        val weight70 = roundToStep(workingWeight * 0.70, barWeight, plateStep)
        if (weight70 > weight50 && weight70 < workingWeight) {
            sets.add(
                WarmupSetProtocol(
                    setNumber = setIndex++,
                    percentage = 0.70,
                    weight = weight70,
                    reps = 3,
                    restSeconds = 90,
                    purpose = "Neural ramp & bar acceleration"
                )
            )
        }

        // 4. 85% Set (if working load is sufficiently above 70%)
        val weight85 = roundToStep(workingWeight * 0.85, barWeight, plateStep)
        if (weight85 > weight70 && weight85 < workingWeight) {
            sets.add(
                WarmupSetProtocol(
                    setNumber = setIndex++,
                    percentage = 0.85,
                    weight = weight85,
                    reps = 1,
                    restSeconds = 120,
                    purpose = "CNS potentiation"
                )
            )
        }

        // 5. 92% Heavy Potentiation Set (only for heavy working weights >= 140kg)
        if (workingWeight >= 140.0) {
            val weight92 = roundToStep(workingWeight * 0.92, barWeight, plateStep)
            if (weight92 > weight85 && weight92 < workingWeight) {
                sets.add(
                    WarmupSetProtocol(
                        setNumber = setIndex,
                        percentage = 0.92,
                        weight = weight92,
                        reps = 1,
                        restSeconds = 150,
                        purpose = "Post-activation potentiation"
                    )
                )
            }
        }

        val totalRestSeconds = sets.sumOf { it.restSeconds }
        val durationMinutes = ((totalRestSeconds + (sets.size * 20)) / 60.0).let { kotlin.math.ceil(it).toInt() }

        return WarmupPlan(
            workingWeight = workingWeight,
            barWeight = barWeight,
            sets = sets,
            estimatedDurationMinutes = durationMinutes
        )
    }

    /**
     * Helper to round a target weight to the nearest achievable plate step (e.g. 2.5kg pairs).
     */
    fun roundToStep(weight: Double, minWeight: Double, step: Double): Double {
        if (weight <= minWeight) return minWeight
        val added = weight - minWeight
        val roundedAdded = round(added / step) * step
        return minWeight + roundedAdded
    }
}
