package com.gymcoach.app.domain.model

data class SupersetGroup(
    val id: String, // e.g., "SS_1"
    val label: String, // e.g., "Superset A"
    val exerciseIndices: List<Int>, // e.g. [0, 1]
    val transitionRestSeconds: Int = 30, // short rest between exercises in the pair
    val roundRestSeconds: Int = 90 // full rest after both exercises in the pair complete a round
) {
    fun getNextExerciseIndex(currentExerciseIndex: Int): Int? {
        val pos = exerciseIndices.indexOf(currentExerciseIndex)
        if (pos == -1) return null
        return if (pos + 1 < exerciseIndices.size) exerciseIndices[pos + 1] else exerciseIndices.firstOrNull()
    }

    fun isEndOfRound(currentExerciseIndex: Int): Boolean {
        return exerciseIndices.lastOrNull() == currentExerciseIndex
    }

    fun getRecommendedRestSeconds(currentExerciseIndex: Int): Int {
        return if (isEndOfRound(currentExerciseIndex)) roundRestSeconds else transitionRestSeconds
    }
}
