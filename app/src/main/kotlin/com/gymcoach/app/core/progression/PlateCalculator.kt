package com.gymcoach.app.core.progression

/**
 * High-precision barbell plate calculator for standard Olympic plates (kg & lbs).
 */
object PlateCalculator {

    data class PlateBreakdown(
        val totalWeight: Double,
        val barWeight: Double,
        val weightPerSide: Double,
        val platesPerSide: List<PlateCount>,
        val remainder: Double
    )

    data class PlateCount(
        val plateWeight: Double,
        val count: Int,
        val hexColor: Long
    )

    // Standard Olympic plate colors (IWF standards)
    val STANDARD_METRIC_PLATES = listOf(
        25.0 to 0xFFD32F2F, // Red
        20.0 to 0xFF1976D2, // Blue
        15.0 to 0xFFFBC02D, // Yellow
        10.0 to 0xFF388E3C, // Green
        5.0 to 0xFFFFFFFF,  // White
        2.5 to 0xFF212121,  // Black
        1.25 to 0xFF9E9E9E  // Chrome/Silver
    )

    // Standard Olympic plate colors (lbs standards: 45, 35, 25, 10, 5, 2.5)
    val STANDARD_IMPERIAL_PLATES = listOf(
        45.0 to 0xFF1976D2, // Blue (45 lbs)
        35.0 to 0xFFFBC02D, // Yellow (35 lbs)
        25.0 to 0xFF388E3C, // Green (25 lbs)
        10.0 to 0xFF212121, // Black (10 lbs)
        5.0 to 0xFFFFFFFF,  // White (5 lbs)
        2.5 to 0xFF9E9E9E   // Chrome/Silver (2.5 lbs)
    )

    /**
     * Calculates the plates needed on each side of the barbell.
     */
    fun calculatePlates(
        targetWeight: Double,
        barWeight: Double = 20.0,
        availablePlates: List<Pair<Double, Long>> = STANDARD_METRIC_PLATES
    ): PlateBreakdown {
        if (targetWeight <= barWeight) {
            return PlateBreakdown(
                totalWeight = targetWeight,
                barWeight = barWeight,
                weightPerSide = 0.0,
                platesPerSide = emptyList(),
                remainder = 0.0
            )
        }

        var remainingWeightPerSide = (targetWeight - barWeight) / 2.0
        val platesList = mutableListOf<PlateCount>()

        val sortedPlates = availablePlates.sortedByDescending { it.first }

        for ((plateWeight, color) in sortedPlates) {
            if (remainingWeightPerSide >= plateWeight) {
                val count = (remainingWeightPerSide / plateWeight).toInt()
                if (count > 0) {
                    platesList.add(PlateCount(plateWeight, count, color))
                    remainingWeightPerSide -= count * plateWeight
                    // Round to avoid floating point precision issues
                    remainingWeightPerSide = Math.round(remainingWeightPerSide * 100.0) / 100.0
                }
            }
        }

        return PlateBreakdown(
            totalWeight = targetWeight,
            barWeight = barWeight,
            weightPerSide = (targetWeight - barWeight) / 2.0,
            platesPerSide = platesList,
            remainder = remainingWeightPerSide * 2.0 // total unachievable weight
        )
    }
}
