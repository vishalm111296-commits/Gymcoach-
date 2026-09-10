package com.gymcoach.app.domain.model

enum class CanonicalMuscle(
    val id: String,
    val displayName: String,
    val targetCategory: String
) {
    LATISSIMUS_DORSI("latissimus_dorsi", "Lats", "Lats"),
    LATERAL_DELTOID("lateral_deltoid", "Lateral Deltoid", "Lateral Deltoid"),
    REAR_DELTOID("rear_deltoid", "Rear Deltoid", "Rear Deltoid"),
    UPPER_BACK("upper_back", "Upper Back", "Upper Back"),
    UPPER_CHEST("upper_chest", "Upper Chest", "Upper Chest"),
    CHEST("chest", "Chest", "Chest"),
    MID_CHEST("mid_chest", "Mid Chest", "Chest"),
    LOWER_CHEST("lower_chest", "Lower Chest", "Chest"),
    BICEPS("biceps", "Biceps", "Biceps"),
    BRACHIALIS("brachialis", "Brachialis", "Biceps"),
    TRICEPS("triceps", "Triceps", "Triceps"),
    QUADRICEPS("quadriceps", "Quadriceps", "Quadriceps"),
    HAMSTRINGS("hamstrings", "Hamstrings", "Hamstrings"),
    GLUTES("glutes", "Glutes", "Glutes"),
    CALVES("calves", "Calves", "Calves"),
    CORE("core", "Core", "Core"),
    ABS("abs", "Abs", "Core"),
    OBLIQUES("obliques", "Obliques", "Core"),
    DEEP_CORE("deep_core", "Deep Core", "Core"),
    FRONT_DELTOID("front_deltoid", "Front Deltoid", "Front Deltoid"),
    LOWER_BACK("lower_back", "Lower Back", "Upper Back"),
    FOREARMS("forearms", "Forearms", "Biceps"),
    ADDUCTORS("adductors", "Adductors", "Glutes"),
    HIP_FLEXORS("hip_flexors", "Hip Flexors", "Core");

    companion object {
        private val idMap = entries.associateBy { it.id.lowercase() }

        fun fromIdOrName(rawName: String): CanonicalMuscle? {
            val normalized = rawName.trim().lowercase().replace(" ", "_")
            idMap[normalized]?.let { return it }

            return when (normalized) {
                "lats", "latissimus", "latissimusdorsi" -> LATISSIMUS_DORSI
                "side_deltoid", "lateral_delt", "side_delt" -> LATERAL_DELTOID
                "rear_delt" -> REAR_DELTOID
                "upper_chest_clavicular" -> UPPER_CHEST
                "bicep" -> BICEPS
                "tricep" -> TRICEPS
                "quadricep", "quad", "quads" -> QUADRICEPS
                "hamstring" -> HAMSTRINGS
                "glute" -> GLUTES
                "calf" -> CALVES
                "erector_spinae" -> LOWER_BACK
                else -> null
            }
        }
    }
}
