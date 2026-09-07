package com.gymcoach.app.domain.model

enum class BodyRegion(val displayName: String) {
    UPPER_BODY("Upper Body"),
    LOWER_BODY("Lower Body"),
    TRUNK("Trunk")
}

enum class CanonicalMuscle(
    val id: String,
    val displayName: String,
    val bodyRegion: BodyRegion,
    val aliases: List<String> = emptyList()
) {
    LATISSIMUS_DORSI("latissimus_dorsi", "Lats", BodyRegion.UPPER_BODY, listOf("lats", "lat", "back")),
    UPPER_BACK("upper_back", "Upper Back", BodyRegion.UPPER_BODY, listOf("traps", "rhomboids", "upper back")),
    REAR_DELTOID("rear_deltoid", "Rear Deltoid", BodyRegion.UPPER_BODY, listOf("rear delt", "rear delts", "posterior deltoid")),
    LATERAL_DELTOID("lateral_deltoid", "Side Deltoid", BodyRegion.UPPER_BODY, listOf("side delt", "lateral delt", "side delts", "lateral deltoids")),
    FRONT_DELTOID("front_deltoid", "Front Deltoid", BodyRegion.UPPER_BODY, listOf("front delt", "anterior deltoid")),
    UPPER_CHEST("upper_chest", "Upper Chest", BodyRegion.UPPER_BODY, listOf("clavicular chest", "upper chest")),
    MID_CHEST("mid_chest", "Mid Chest", BodyRegion.UPPER_BODY, listOf("chest", "sternal chest")),
    LOWER_CHEST("lower_chest", "Lower Chest", BodyRegion.UPPER_BODY, listOf("abdominal chest", "lower chest")),
    BICEPS("biceps", "Biceps", BodyRegion.UPPER_BODY, listOf("bicep", "biceps")),
    TRICEPS("triceps", "Triceps", BodyRegion.UPPER_BODY, listOf("tricep", "triceps")),
    QUADRICEPS("quadriceps", "Quadriceps", BodyRegion.LOWER_BODY, listOf("quads", "quad", "quadriceps")),
    HAMSTRINGS("hamstrings", "Hamstrings", BodyRegion.LOWER_BODY, listOf("hamstring", "hamstrings")),
    GLUTES("glutes", "Glutes", BodyRegion.LOWER_BODY, listOf("glute", "glutes")),
    CALVES("calves", "Calves", BodyRegion.LOWER_BODY, listOf("calf", "calves")),
    ABS("abs", "Abs", BodyRegion.TRUNK, listOf("core", "abdominals")),
    OBLIQUES("obliques", "Obliques", BodyRegion.TRUNK, listOf("oblique")),
    DEEP_CORE("deep_core", "Deep Core", BodyRegion.TRUNK, listOf("transverse abdominis"));

    companion object {
        fun fromIdOrAlias(input: String): CanonicalMuscle? {
            val trimmed = input.trim().lowercase()
            val token = trimmed.replace(" ", "_").replace("-", "_")
            return entries.firstOrNull { muscle ->
                muscle.id == token ||
                muscle.displayName.lowercase() == trimmed ||
                muscle.aliases.any { alias -> alias.lowercase() == trimmed || alias.replace(" ", "_") == token }
            }
        }
    }
}
