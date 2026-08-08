package com.gymcoach.app.core.util

import com.gymcoach.app.domain.model.Plate
import com.gymcoach.app.domain.model.PlateCalculation

object PlateCalculator {
    fun calculate(
        targetWeight: Double,
        barWeight: Double,
        inventory: List<Plate>
    ): PlateCalculation? {
        val weightToLoad = (targetWeight - barWeight) / 2
        if (weightToLoad < 0) return null
        if (weightToLoad == 0.0) return PlateCalculation(targetWeight, barWeight, emptyList())

        val sortedInventory = inventory.filter { it.weight > 0 }.sortedByDescending { it.weight }
        val resultPlates = mutableListOf<Plate>()
        var remaining = weightToLoad

        for (plate in sortedInventory) {
            val count = (remaining / plate.weight).toInt().coerceAtMost(plate.count)
            if (count > 0) {
                resultPlates.add(Plate(plate.weight, count))
                remaining -= count * plate.weight
            }
        }

        return if (remaining == 0.0) PlateCalculation(targetWeight, barWeight, resultPlates) else null
    }
}
