package com.gymcoach.app.domain.model

enum class UnitSystem { METRIC, IMPERIAL }

data class Plate(val weight: Double, val count: Int)

data class PlateCalculation(
    val targetWeight: Double,
    val barWeight: Double,
    val plates: List<Plate>
)
