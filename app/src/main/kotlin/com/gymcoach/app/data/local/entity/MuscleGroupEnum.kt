package com.gymcoach.app.data.local.entity

enum class MuscleGroupEnum(val displayName: String) {
    // Chest
    UPPER_CHEST("Upper Chest"),
    LOWER_CHEST("Lower Chest"),
    INNER_CHEST("Inner Chest"),
    // Back
    LATS("Lats"),
    UPPER_BACK("Upper Back"),
    LOWER_BACK("Lower Back"),
    RHOMBOIDS("Rhomboids"),
    TERES_MAJOR("Teres Major"),
    // Shoulders
    FRONT_DELT("Front Delt"),
    LATERAL_DELT("Lateral Delt"),
    REAR_DELT("Rear Delt"),
    ROTATOR_CUFF("Rotator Cuff"),
    // Arms
    BICEPS_LONG("Biceps Long Head"),
    BICEPS_SHORT("Biceps Short Head"),
    BRACHIALIS("Brachialis"),
    BRACHIORADIALIS("Brachioradialis"),
    TRICEPS_LONG("Triceps Long Head"),
    TRICEPS_LATERAL("Triceps Lateral Head"),
    TRICEPS_MEDIAL("Triceps Medial Head"),
    FOREARMS("Forearms"),
    // Core
    RECTUS_ABDOMINIS("Rectus Abdominis"),
    OBLIQUES("Obliques"),
    TRANSVERSE_ABDOMINIS("Transverse Abdominis"),
    SERRATUS("Serratus Anterior"),
    // Legs
    QUADS("Quadriceps"),
    HAMSTRINGS("Hamstrings"),
    GLUTES_MAX("Gluteus Maximus"),
    GLUTES_MED("Gluteus Medius"),
    CALVES_GASTROCNEMIUS("Gastrocnemius"),
    CALVES_SOLEUS("Soleus"),
    HIP_FLEXORS("Hip Flexors"),
    ADDUCTORS("Adductors"),
    ABDUCTORS("Abductors"),
    // Traps
    UPPER_TRAPS("Upper Traps"),
    MIDDLE_TRAPS("Middle Traps"),
    LOWER_TRAPS("Lower Traps")
}
