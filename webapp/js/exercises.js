/**
 * GymCoach Exercise Library & Preset V-Taper Routine Generator (exercises.js)
 * Bundles Dumbbell + Bodyweight exercises with V-Taper relevance scores.
 */

export const ExerciseLibrary = [
  // LATS & BACK (Torso Width Drivers)
  {
    id: "db_one_arm_row",
    name: "Single-Arm Dumbbell Row",
    category: "back",
    equipment: ["dumbbell", "bench"],
    primaryMuscles: ["latissimus_dorsi"],
    secondaryMuscles: ["upper_back", "biceps", "rear_deltoid"],
    vTaperRelevance: 9,
    repRange: "8-12",
    rirTarget: "1-2",
    restSeconds: 90,
    setup: "Place one knee and hand on bench. Keep torso parallel to floor, spine neutral.",
    execution: "Pull dumbbell toward hip crease, driving elbow up and back. Squeeze lat at top, lower under control.",
    formCues: [
      "Drive elbow toward hip, not shoulder",
      "Keep shoulders square; avoid twisting torso",
      "Full stretch at the bottom"
    ],
    commonMistakes: [
      "Jerking body to lift weight",
      "Pulling weight straight up to chest instead of arc to hip"
    ],
    alternatives: ["db_bent_over_row", "inverted_row"]
  },
  {
    id: "db_bent_over_row",
    name: "Two-Arm Dumbbell Row",
    category: "back",
    equipment: ["dumbbell"],
    primaryMuscles: ["latissimus_dorsi", "upper_back"],
    secondaryMuscles: ["biceps", "rear_deltoid"],
    vTaperRelevance: 8,
    repRange: "8-12",
    rirTarget: "1-2",
    restSeconds: 90,
    setup: "Hinge at hips to ~45 degrees, knees soft, holding dumbbells with overhand or neutral grip.",
    execution: "Row dumbbells toward lower ribs, driving elbows backward.",
    formCues: [
      "Keep chest up, core braced",
      "Squeeze shoulder blades together at peak contraction"
    ],
    commonMistakes: [
      "Standing up too upright during sets",
      "Rounding the lower back"
    ],
    alternatives: ["db_one_arm_row", "chest_supported_db_row"]
  },
  {
    id: "inverted_row",
    name: "Inverted Bodyweight Row (Table / Door)",
    category: "back",
    equipment: ["bodyweight"],
    primaryMuscles: ["latissimus_dorsi", "upper_back"],
    secondaryMuscles: ["biceps", "rear_deltoid"],
    vTaperRelevance: 9,
    repRange: "8-15",
    rirTarget: "1-2",
    restSeconds: 90,
    setup: "Grasp sturdy table edge or broomstick on chairs. Straight body line, heels planted.",
    execution: "Pull chest up to table edge by driving elbows back.",
    formCues: [
      "Brace glutes and abs to maintain straight plank line",
      "Pause for 1 sec at top contact"
    ],
    commonMistakes: [
      "Sagging hips toward floor",
      "Leading with chin instead of chest"
    ],
    alternatives: ["db_one_arm_row", "pull_up"]
  },
  {
    id: "db_pullover",
    name: "Dumbbell Lat Pullover",
    category: "back",
    equipment: ["dumbbell", "bench"],
    primaryMuscles: ["latissimus_dorsi"],
    secondaryMuscles: ["upper_chest", "triceps"],
    vTaperRelevance: 8,
    repRange: "10-15",
    rirTarget: "1-2",
    restSeconds: 90,
    setup: "Lie across bench with upper back supported. Hold single dumbbell overhead with palms cupping inner weight plate.",
    execution: "Lower dumbbell behind head in arc while keeping elbows slightly bent until lat stretch, then pull back over chest.",
    formCues: [
      "Keep hips down to stretch lats",
      "Focus on elbow movement rather than hands"
    ],
    commonMistakes: [
      "Bending elbows excessively turning it into a tricep extension",
      "Arching lower back off bench"
    ],
    alternatives: ["db_one_arm_row"]
  },

  // LATERAL & REAR DELTOIDS (Shoulder Width & 3D Frame)
  {
    id: "db_lateral_raise",
    name: "Standing Dumbbell Lateral Raise",
    category: "shoulders",
    equipment: ["dumbbell"],
    primaryMuscles: ["lateral_deltoid"],
    secondaryMuscles: ["front_deltoid", "upper_back"],
    vTaperRelevance: 10,
    repRange: "12-18",
    rirTarget: "1-2",
    restSeconds: 60,
    setup: "Stand upright, soft knees, holding dumbbells at sides with pinkies slightly higher.",
    execution: "Raise dumbbells out to sides in a wide arc until parallel with shoulders.",
    formCues: [
      "Lead with elbows, keep shoulders down away from ears",
      "Slight forward lean (~10 deg) aligns side delt with gravity"
    ],
    commonMistakes: [
      "Shrugging traps during lift",
      "Swinging torso for momentum"
    ],
    alternatives: ["lean_away_db_lateral_raise", "db_upright_row"]
  },
  {
    id: "lean_away_db_lateral_raise",
    name: "Incline Lean-Away Lateral Raise",
    category: "shoulders",
    equipment: ["dumbbell"],
    primaryMuscles: ["lateral_deltoid"],
    secondaryMuscles: ["front_deltoid"],
    vTaperRelevance: 10,
    repRange: "12-15",
    rirTarget: "1-2",
    restSeconds: 60,
    setup: "Hold doorframe or pole, lean body away at 30 deg angle, holding DB in outer hand.",
    execution: "Raise dumbbell in arc to shoulder height, maintaining constant tension at bottom.",
    formCues: [
      "Keep constant tension through bottom half of ROM",
      "Controlled 2-second lowering phase"
    ],
    commonMistakes: [
      "Using momentum to swing out of bottom stretch"
    ],
    alternatives: ["db_lateral_raise"]
  },
  {
    id: "db_rear_delt_fly",
    name: "Bent-Over DB Rear Delt Fly",
    category: "shoulders",
    equipment: ["dumbbell"],
    primaryMuscles: ["rear_deltoid"],
    secondaryMuscles: ["upper_back"],
    vTaperRelevance: 9,
    repRange: "12-15",
    rirTarget: "1-2",
    restSeconds: 60,
    setup: "Hinge forward at hips until torso is near parallel to floor. Hold DBs under chest.",
    execution: "Raise arms out to sides with soft elbow bend, squeezing rear delts at top.",
    formCues: [
      "Think of sweeping arms wide, not pulling shoulder blades together",
      "Keep neck in neutral alignment looking at floor"
    ],
    commonMistakes: [
      "Squeezing traps instead of isolating rear delts",
      "Using heavy weight and bouncing torso"
    ],
    alternatives: ["db_y_raise"]
  },

  // UPPER CHEST (Clavicular Head Support)
  {
    id: "incline_db_press",
    name: "Incline Dumbbell Chest Press",
    category: "chest",
    equipment: ["dumbbell", "bench"],
    primaryMuscles: ["upper_chest"],
    secondaryMuscles: ["front_deltoid", "triceps"],
    vTaperRelevance: 9,
    repRange: "8-12",
    rirTarget: "1-2",
    restSeconds: 90,
    setup: "Set bench to 30-45 degree incline. Feet planted, dumbbells at shoulder level.",
    execution: "Press dumbbells up and slightly inward over clavicles until arms extend.",
    formCues: [
      "Set incline to 30 deg for optimal upper chest vs front delt bias",
      "Tuck elbows ~45 degrees from torso"
    ],
    commonMistakes: [
      "Incline too steep (>60 deg), shifting load to shoulders",
      "Flaring elbows straight out to sides"
    ],
    alternatives: ["feet_elevated_pushup", "db_bench_press"]
  },
  {
    id: "feet_elevated_pushup",
    name: "Decline / Feet-Elevated Push-up",
    category: "chest",
    equipment: ["bodyweight", "bench"],
    primaryMuscles: ["upper_chest"],
    secondaryMuscles: ["front_deltoid", "triceps", "core"],
    vTaperRelevance: 9,
    repRange: "10-20",
    rirTarget: "1-2",
    restSeconds: 90,
    setup: "Place toes on chair/bench, hands slightly wider than shoulder width on floor.",
    execution: "Lower chest toward floor maintaining tight plank, press up explosively.",
    formCues: [
      "Maintain rigid core line from head to ankles",
      "Touch chest lightly to floor"
    ],
    commonMistakes: [
      "Arching lower back",
      "Cutting range of motion short"
    ],
    alternatives: ["incline_db_press", "push_up"]
  },
  {
    id: "push_up",
    name: "Standard Bodyweight Push-up",
    category: "chest",
    equipment: ["bodyweight"],
    primaryMuscles: ["mid_chest"],
    secondaryMuscles: ["front_deltoid", "triceps", "core"],
    vTaperRelevance: 7,
    repRange: "10-25",
    rirTarget: "1-2",
    restSeconds: 60,
    setup: "Hands shoulder-width on floor, fingers spread, body in straight plank.",
    execution: "Lower body until chest is 1 inch off floor, press up firmly.",
    formCues: [
      "Elbows at 45 degree angle",
      "Brace glutes and abs throughout"
    ],
    commonMistakes: [
      "Sagging hips",
      "Flaring elbows to 90 degrees"
    ],
    alternatives: ["feet_elevated_pushup", "db_bench_press"]
  },

  // ARMS & CORE
  {
    id: "db_bicep_curl",
    name: "Dumbbell Incline Bicep Curl",
    category: "arms",
    equipment: ["dumbbell", "bench"],
    primaryMuscles: ["biceps"],
    secondaryMuscles: ["forearms"],
    vTaperRelevance: 6,
    repRange: "10-14",
    rirTarget: "1-2",
    restSeconds: 60,
    setup: "Seated on 45 deg incline bench, arms hanging straight down behind torso.",
    execution: "Curl dumbbells up keeping upper arms stationary to emphasize bicep long head.",
    formCues: ["Keep elbows pinned back", "Supinate wrists at top"],
    commonMistakes: ["Swinging elbows forward"],
    alternatives: ["db_hammer_curl"]
  },
  {
    id: "overhead_db_tricep_ext",
    name: "Overhead Dumbbell Tricep Extension",
    category: "arms",
    equipment: ["dumbbell"],
    primaryMuscles: ["triceps"],
    secondaryMuscles: ["forearms"],
    vTaperRelevance: 6,
    repRange: "10-15",
    rirTarget: "1-2",
    restSeconds: 60,
    setup: "Stand or sit holding single DB overhead with both hands under top plate.",
    execution: "Lower DB behind head by flexing elbows, extend arms back to top.",
    formCues: ["Keep elbows pointing forward, not flaring out"],
    commonMistakes: ["Arching lower back excessively"],
    alternatives: ["db_skullcrusher"]
  },
  {
    id: "hanging_leg_raise",
    name: "Reverse Crunch / Hanging Knee Raise",
    category: "core",
    equipment: ["bodyweight"],
    primaryMuscles: ["abs"],
    secondaryMuscles: ["hip_flexors", "obliques"],
    vTaperRelevance: 8,
    repRange: "12-20",
    rirTarget: "1-2",
    restSeconds: 60,
    setup: "Lie on floor or hang from pull-up bar. Knees slightly bent.",
    execution: "Curl pelvis toward ribcage lifting knees up to chest.",
    formCues: ["Focus on curling lower spine, not just swinging legs"],
    commonMistakes: ["Using momentum and arching back"],
    alternatives: ["plank"]
  },
  {
    id: "plank",
    name: "Forearm Plank (Transverse Abdominis)",
    category: "core",
    equipment: ["bodyweight"],
    primaryMuscles: ["deep_core"],
    secondaryMuscles: ["abs", "glutes"],
    vTaperRelevance: 8,
    repRange: "45-75s",
    rirTarget: "1-2",
    restSeconds: 60,
    setup: "Forearms on floor, elbow under shoulder, feet together.",
    execution: "Hold rigid plank position while pulling navel toward spine.",
    formCues: ["Squeeze glutes and brace abdominal wall"],
    commonMistakes: ["Hips sagging or hiking high"],
    alternatives: ["hanging_leg_raise"]
  }
];

// Preset V-Taper Routines
export const PresetRoutines = [
  {
    id: "split_4day_vtaper",
    name: "4-Day V-Taper Upper/Lower Split",
    splitType: "4-day",
    description: "Optimal weekly frequency (2x/week) for Lats, Side Delts, and Upper Chest using DB & Bodyweight.",
    days: [
      {
        dayName: "Day 1: Upper A (Lat & Side Delt Focus)",
        exercises: [
          { exerciseId: "db_one_arm_row", targetSets: 4, repRange: "8-12", targetRir: 2 },
          { exerciseId: "db_lateral_raise", targetSets: 4, repRange: "12-15", targetRir: 1 },
          { exerciseId: "incline_db_press", targetSets: 3, repRange: "8-12", targetRir: 2 },
          { exerciseId: "db_rear_delt_fly", targetSets: 3, repRange: "12-15", targetRir: 1 },
          { exerciseId: "db_bicep_curl", targetSets: 3, repRange: "10-12", targetRir: 2 }
        ]
      },
      {
        dayName: "Day 2: Lower A & Deep Core",
        exercises: [
          { exerciseId: "hanging_leg_raise", targetSets: 3, repRange: "12-15", targetRir: 2 },
          { exerciseId: "plank", targetSets: 3, repRange: "60s", targetRir: 2 }
        ]
      },
      {
        dayName: "Day 3: Upper B (Upper Chest & Shoulder Focus)",
        exercises: [
          { exerciseId: "feet_elevated_pushup", targetSets: 4, repRange: "10-15", targetRir: 2 },
          { exerciseId: "lean_away_db_lateral_raise", targetSets: 4, repRange: "12-15", targetRir: 1 },
          { exerciseId: "inverted_row", targetSets: 4, repRange: "8-12", targetRir: 2 },
          { exerciseId: "db_pullover", targetSets: 3, repRange: "10-14", targetRir: 2 },
          { exerciseId: "overhead_db_tricep_ext", targetSets: 3, repRange: "10-14", targetRir: 2 }
        ]
      },
      {
        dayName: "Day 4: Lower B & Abs",
        exercises: [
          { exerciseId: "hanging_leg_raise", targetSets: 3, repRange: "12-15", targetRir: 2 },
          { exerciseId: "plank", targetSets: 3, repRange: "60s", targetRir: 2 }
        ]
      }
    ]
  },
  {
    id: "split_3day_vtaper",
    name: "3-Day Full Body V-Taper Hypertrophy",
    splitType: "3-day",
    description: "Compact 3-day schedule ideal for busy routines, focusing on high-efficiency V-taper compounds.",
    days: [
      {
        dayName: "Workout A (Lats & Chest)",
        exercises: [
          { exerciseId: "db_one_arm_row", targetSets: 4, repRange: "8-12", targetRir: 2 },
          { exerciseId: "incline_db_press", targetSets: 4, repRange: "8-12", targetRir: 2 },
          { exerciseId: "db_lateral_raise", targetSets: 4, repRange: "12-18", targetRir: 1 },
          { exerciseId: "hanging_leg_raise", targetSets: 3, repRange: "12-15", targetRir: 2 }
        ]
      },
      {
        dayName: "Workout B (Shoulders & Upper Back)",
        exercises: [
          { exerciseId: "inverted_row", targetSets: 4, repRange: "8-12", targetRir: 2 },
          { exerciseId: "feet_elevated_pushup", targetSets: 3, repRange: "10-15", targetRir: 2 },
          { exerciseId: "lean_away_db_lateral_raise", targetSets: 4, repRange: "12-15", targetRir: 1 },
          { exerciseId: "db_rear_delt_fly", targetSets: 3, repRange: "12-15", targetRir: 1 }
        ]
      },
      {
        dayName: "Workout C (V-Taper Finisher)",
        exercises: [
          { exerciseId: "db_bent_over_row", targetSets: 4, repRange: "8-12", targetRir: 2 },
          { exerciseId: "db_pullover", targetSets: 3, repRange: "10-14", targetRir: 2 },
          { exerciseId: "db_lateral_raise", targetSets: 4, repRange: "12-15", targetRir: 1 },
          { exerciseId: "plank", targetSets: 3, repRange: "60s", targetRir: 2 }
        ]
      }
    ]
  },
  {
    id: "split_5day_vtaper",
    name: "5-Day V-Taper High-Frequency Specialization",
    splitType: "5-day",
    description: "Dedicated specialization split maximizing lateral delt and lat volume across 5 training sessions.",
    days: [
      {
        dayName: "Day 1: Lats & Upper Back",
        exercises: [
          { exerciseId: "db_one_arm_row", targetSets: 4, repRange: "8-12", targetRir: 2 },
          { exerciseId: "inverted_row", targetSets: 4, repRange: "8-12", targetRir: 2 },
          { exerciseId: "db_pullover", targetSets: 3, repRange: "10-14", targetRir: 2 }
        ]
      },
      {
        dayName: "Day 2: Lateral & Rear Delt Specialization",
        exercises: [
          { exerciseId: "db_lateral_raise", targetSets: 5, repRange: "12-18", targetRir: 1 },
          { exerciseId: "lean_away_db_lateral_raise", targetSets: 4, repRange: "12-15", targetRir: 1 },
          { exerciseId: "db_rear_delt_fly", targetSets: 4, repRange: "12-15", targetRir: 1 }
        ]
      },
      {
        dayName: "Day 3: Upper Chest & Core",
        exercises: [
          { exerciseId: "incline_db_press", targetSets: 4, repRange: "8-12", targetRir: 2 },
          { exerciseId: "feet_elevated_pushup", targetSets: 4, repRange: "10-15", targetRir: 2 },
          { exerciseId: "hanging_leg_raise", targetSets: 3, repRange: "12-15", targetRir: 2 }
        ]
      },
      {
        dayName: "Day 4: Side Delt & Arm Hypertrophy",
        exercises: [
          { exerciseId: "db_lateral_raise", targetSets: 4, repRange: "12-15", targetRir: 1 },
          { exerciseId: "db_bicep_curl", targetSets: 4, repRange: "10-12", targetRir: 2 },
          { exerciseId: "overhead_db_tricep_ext", targetSets: 4, repRange: "10-14", targetRir: 2 }
        ]
      },
      {
        dayName: "Day 5: Full Upper V-Taper Re-stimulus",
        exercises: [
          { exerciseId: "db_bent_over_row", targetSets: 4, repRange: "8-12", targetRir: 2 },
          { exerciseId: "push_up", targetSets: 3, repRange: "12-20", targetRir: 2 },
          { exerciseId: "plank", targetSets: 3, repRange: "60s", targetRir: 2 }
        ]
      }
    ]
  }
];

export function getExerciseById(id) {
  return ExerciseLibrary.find(e => e.id === id) || null;
}

export function filterExercises({ category = 'all', search = '' } = {}) {
  return ExerciseLibrary.filter(ex => {
    const matchesCat = category === 'all' || ex.category === category;
    const searchLower = search.toLowerCase().trim();
    const matchesSearch = !searchLower ||
      ex.name.toLowerCase().includes(searchLower) ||
      ex.primaryMuscles.some(m => m.toLowerCase().includes(searchLower));
    return matchesCat && matchesSearch;
  });
}
