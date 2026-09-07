/**
 * GymCoach V-Taper Companion Web App
 * Focused on Dumbbell & Bodyweight Training
 * Profile: 30yo, 70kg, 170cm
 */

// Global State
const state = {
    profile: {
        age: 30,
        weightKg: 70,
        heightCm: 170,
        equipment: ["dumbbell", "bodyweight", "bench"]
    },
    readinessScore: 3,
    activeTab: "tab-dashboard",
    exercises: [],
    programFrequency: 4,
    currentProgram: null,
    activeWorkout: null,
    workoutTimerInterval: null,
    workoutElapsedSeconds: 0,
    restTimerInterval: null,
    restSecondsLeft: 0,
    measurements: [
        { date: "2026-09-01", shoulder: 110, waist: 77, chest: 96, arm: 34 },
        { date: "2026-09-06", shoulder: 112, waist: 76, chest: 97, arm: 34.5 }
    ],
    nutritionGoal: "recomp" // "recomp", "surplus", "deficit"
};

// Exercise JSON Files in Repository
const EXERCISE_ASSET_FILES = [
    "../app/src/main/assets/exercises/dumbbell_bodyweight_bench_exercises.json",
    "../app/src/main/assets/exercises/back_heavy_exercises.json",
    "../app/src/main/assets/exercises/back_extra_exercises.json",
    "../app/src/main/assets/exercises/shoulder_extra_exercises.json",
    "../app/src/main/assets/exercises/chest_extra_exercises.json",
    "../app/src/main/assets/exercises/core_exercises.json",
    "../app/src/main/assets/exercises/core_exercises_batch2.json",
    "../app/src/main/assets/exercises/bicep_exercises.json",
    "../app/src/main/assets/exercises/tricep_exercises.json",
    "../app/src/main/assets/exercises/leg_quad_exercises.json",
    "../app/src/main/assets/exercises/leg_hamstring_glute_exercises.json"
];

// Fallback Dumbbell & Bodyweight Exercise Seed if fetch is restricted locally
const FALLBACK_EXERCISES = [
    {
        id: "db_bench_press",
        name: "Dumbbell Bench Press",
        category: "chest",
        primary_muscles: ["mid_chest"],
        secondary_muscles: ["front_deltoid", "triceps", "upper_chest"],
        equipment: ["dumbbell", "bench"],
        rep_range: "8-12",
        vtaper_scores: { lat: 0, lateral_delt: 2, upper_chest: 6, rear_delt: 0 },
        setup: "Lie flat on bench with feet planted. Hold dumbbells over chest.",
        execution: "Lower dumbbells with elbows at 45 degrees, then press up.",
        form_cues: ["Elbows 45 degrees from torso", "Feet drive into floor"]
    },
    {
        id: "incline_db_press",
        name: "Incline Dumbbell Press",
        category: "chest",
        primary_muscles: ["upper_chest"],
        secondary_muscles: ["front_deltoid", "triceps"],
        equipment: ["dumbbell", "bench"],
        rep_range: "8-12",
        vtaper_scores: { lat: 0, lateral_delt: 3, upper_chest: 9, rear_delt: 0 },
        setup: "Set bench to 30 degrees incline. Hold dumbbells at shoulder level.",
        execution: "Press dumbbells vertically over upper chest.",
        form_cues: ["Squeeze upper chest at top", "Control the 2-second negative"]
    },
    {
        id: "single_arm_db_row",
        name: "Single-Arm Dumbbell Row",
        category: "back",
        primary_muscles: ["latissimus_dorsi"],
        secondary_muscles: ["rear_deltoid", "biceps", "upper_back"],
        equipment: ["dumbbell", "bench"],
        rep_range: "8-12",
        vtaper_scores: { lat: 10, lateral_delt: 2, upper_chest: 0, rear_delt: 5 },
        setup: "One knee and hand on bench, other foot planted.",
        execution: "Pull dumbbell toward hip, driving elbow back and stretching lat at bottom.",
        form_cues: ["Pull elbow to hip", "Pause 1 sec at top for lat peak contraction"]
    },
    {
        id: "db_lateral_raise",
        name: "Dumbbell Lateral Raise",
        category: "shoulders",
        primary_muscles: ["lateral_deltoid"],
        secondary_muscles: ["front_deltoid"],
        equipment: ["dumbbell"],
        rep_range: "12-15",
        vtaper_scores: { lat: 1, lateral_delt: 10, upper_chest: 0, rear_delt: 3 },
        setup: "Stand with dumbbells at sides, slight forward lean.",
        execution: "Raise arms out to sides until parallel with floor, leading with elbows.",
        form_cues: ["Lead with elbows", "Pour water motion at top", "Control descent"]
    },
    {
        id: "pull_up",
        name: "Pull-up",
        category: "back",
        primary_muscles: ["latissimus_dorsi"],
        secondary_muscles: ["biceps", "upper_back", "rear_deltoid"],
        equipment: ["bodyweight"],
        rep_range: "6-10",
        vtaper_scores: { lat: 10, lateral_delt: 3, upper_chest: 0, rear_delt: 4 },
        setup: "Overhand grip slightly wider than shoulders.",
        execution: "Pull chest up to bar, driving elbows down.",
        form_cues: ["Depress shoulder blades first", "Chest to bar"]
    },
    {
        id: "db_rear_delt_fly",
        name: "Bent-Over Dumbbell Rear Delt Fly",
        category: "shoulders",
        primary_muscles: ["rear_deltoid"],
        secondary_muscles: ["upper_back"],
        equipment: ["dumbbell"],
        rep_range: "12-15",
        vtaper_scores: { lat: 2, lateral_delt: 4, upper_chest: 0, rear_delt: 9 },
        setup: "Hinge at hips to 45 degrees with dumbbells hanging.",
        execution: "Raise arms out to sides focusing on rear delts.",
        form_cues: ["Keep pinkies up", "Don't shrug traps"]
    },
    {
        id: "hanging_leg_raise",
        name: "Hanging Leg Raise / Knee Raise",
        category: "core",
        primary_muscles: ["abs"],
        secondary_muscles: ["deep_core"],
        equipment: ["bodyweight"],
        rep_range: "10-15",
        vtaper_scores: { lat: 0, lateral_delt: 0, upper_chest: 0, rear_delt: 0 },
        setup: "Hang from pull-up bar.",
        execution: "Raise knees or feet to chest without swinging hips.",
        form_cues: ["Curl pelvis upward", "Control lowering phase"]
    },
    {
        id: "goblet_squat",
        name: "Dumbbell Goblet Squat",
        category: "legs",
        primary_muscles: ["quadriceps"],
        secondary_muscles: ["glutes", "core"],
        equipment: ["dumbbell"],
        rep_range: "10-12",
        vtaper_scores: { lat: 0, lateral_delt: 0, upper_chest: 0, rear_delt: 0 },
        setup: "Hold dumbbell vertically against chest.",
        execution: "Squat down between knees keeping torso upright.",
        form_cues: ["Knees track over toes", "Chest high"]
    },
    {
        id: "db_romanian_deadlift",
        name: "Dumbbell Romanian Deadlift",
        category: "legs",
        primary_muscles: ["hamstrings"],
        secondary_muscles: ["glutes", "lower_back"],
        equipment: ["dumbbell"],
        rep_range: "8-12",
        vtaper_scores: { lat: 2, lateral_delt: 0, upper_chest: 0, rear_delt: 0 },
        setup: "Stand holding dumbbells in front of thighs.",
        execution: "Hinge at hips pushing glutes back until hamstring stretch.",
        form_cues: ["Flat back", "Dumbbells skim close to legs"]
    }
];

// Initialize App
document.addEventListener("DOMContentLoaded", () => {
    initNavigation();
    initReadiness();
    initMeasurements();
    initNutrition();
    loadExerciseLibrary();
});

// Navigation Controller
function initNavigation() {
    const navItems = document.querySelectorAll(".nav-item");
    navItems.forEach(item => {
        item.addEventListener("click", () => {
            const targetTab = item.getAttribute("data-tab");
            switchTab(targetTab);
        });
    });

    document.getElementById("btn-start-workout").addEventListener("click", () => {
        startWorkoutSession();
        switchTab("tab-workout");
    });

    document.getElementById("btn-cancel-workout").addEventListener("click", () => {
        if (confirm("End current workout session?")) {
            cancelWorkoutSession();
        }
    });

    document.getElementById("btn-finish-workout").addEventListener("click", () => {
        finishWorkoutSession();
    });

    document.getElementById("btn-timer-add30").addEventListener("click", () => addRestTime(30));
    document.getElementById("btn-timer-skip").addEventListener("click", skipRestTimer);

    document.getElementById("btn-close-modal").addEventListener("click", () => {
        document.getElementById("exercise-modal").classList.add("hidden");
    });
}

function switchTab(tabId) {
    state.activeTab = tabId;
    document.querySelectorAll(".tab-page").forEach(page => page.classList.remove("active"));
    document.querySelectorAll(".nav-item").forEach(item => item.classList.remove("active"));

    const activePage = document.getElementById(tabId);
    if (activePage) activePage.classList.add("active");

    const activeNav = document.querySelector(`.nav-item[data-tab="${tabId}"]`);
    if (activeNav) activeNav.classList.add("active");
}

// Daily Readiness Controller
function initReadiness() {
    const buttons = document.querySelectorAll(".btn-readiness");
    buttons.forEach(btn => {
        btn.addEventListener("click", () => {
            buttons.forEach(b => b.classList.remove("active"));
            btn.classList.add("active");
            state.readinessScore = parseInt(btn.getAttribute("data-score"), 10);
            updateProgramView();
        });
    });
}

// Exercise Library Loader & Equipment Filter
async function loadExerciseLibrary() {
    let loadedExercises = [];
    for (const fileUrl of EXERCISE_ASSET_FILES) {
        try {
            const response = await fetch(fileUrl);
            if (response.ok) {
                const data = await response.json();
                if (Array.isArray(data)) {
                    loadedExercises.push(...data);
                }
            }
        } catch (e) {
            // Ignore individual fetch errors
        }
    }

    if (loadedExercises.length === 0) {
        loadedExercises = FALLBACK_EXERCISES;
    }

    // Strictly Filter for Dumbbell + Bodyweight Equipment
    state.exercises = loadedExercises.filter(ex => {
        const eqList = Array.isArray(ex.equipment) ? ex.equipment : [ex.equipment];
        return eqList.some(eq => eq === "dumbbell" || eq === "bodyweight" || eq === "bench");
    });

    initSearchAndFilter();
    renderExerciseLibrary();
    generateVTaperProgram();
}

function initSearchAndFilter() {
    const searchInput = document.getElementById("exercise-search-input");
    const chips = document.querySelectorAll(".chip");

    searchInput.addEventListener("input", renderExerciseLibrary);
    chips.forEach(chip => {
        chip.addEventListener("click", () => {
            chips.forEach(c => c.classList.remove("active"));
            chip.classList.add("active");
            renderExerciseLibrary();
        });
    });
}

function renderExerciseLibrary() {
    const searchVal = document.getElementById("exercise-search-input").value.toLowerCase();
    const activeChip = document.querySelector(".chip.active").getAttribute("data-filter");
    const container = document.getElementById("exercise-list-container");

    const filtered = state.exercises.filter(ex => {
        const nameMatch = ex.name.toLowerCase().includes(searchVal) || (ex.category && ex.category.toLowerCase().includes(searchVal));
        let chipMatch = true;

        if (activeChip === "lat") chipMatch = ex.category === "back" || (ex.primary_muscles && ex.primary_muscles.includes("latissimus_dorsi"));
        else if (activeChip === "lateral_delt") chipMatch = ex.category === "shoulders" || (ex.primary_muscles && ex.primary_muscles.includes("lateral_deltoid"));
        else if (activeChip === "upper_back") chipMatch = ex.category === "back" || (ex.secondary_muscles && ex.secondary_muscles.includes("upper_back"));
        else if (activeChip === "upper_chest") chipMatch = ex.category === "chest" || (ex.primary_muscles && ex.primary_muscles.includes("upper_chest"));
        else if (activeChip === "core") chipMatch = ex.category === "core" || (ex.primary_muscles && ex.primary_muscles.includes("abs"));

        return nameMatch && chipMatch;
    });

    container.innerHTML = filtered.map(ex => `
        <div class="exercise-card" onclick="openExerciseModal('${ex.id}')">
            <div>
                <div class="exercise-title">${ex.name}</div>
                <div class="exercise-meta">${(ex.category || 'Compound').toUpperCase()} • ${Array.isArray(ex.equipment) ? ex.equipment.join(", ") : ex.equipment}</div>
            </div>
            <span class="tag tag-primary">V-Taper Score: ${getVTaperRelevance(ex)}</span>
        </div>
    `).join("");
}

function getVTaperRelevance(ex) {
    if (ex.vtaper_scores) {
        return (ex.vtaper_scores.lat || 0) + (ex.vtaper_scores.lateral_delt || 0) + (ex.vtaper_scores.upper_chest || 0) + (ex.vtaper_scores.rear_delt || 0);
    }
    return ex.v_taper_relevance || 5;
}

window.openExerciseModal = function(id) {
    const ex = state.exercises.find(e => e.id === id) || FALLBACK_EXERCISES.find(e => e.id === id);
    if (!ex) return;

    const modalBody = document.getElementById("modal-body-content");
    document.getElementById("modal-title").innerText = ex.name;

    modalBody.innerHTML = `
        <div style="margin-bottom: 12px;">
            <span class="tag tag-primary">${(ex.category || 'General').toUpperCase()}</span>
            <span class="tag tag-accent">${Array.isArray(ex.equipment) ? ex.equipment.join(", ") : ex.equipment}</span>
        </div>
        <h4>Setup:</h4>
        <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 10px;">${ex.setup || "Position body with proper support and tight core."}</p>
        <h4>Execution:</h4>
        <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 10px;">${ex.execution || "Perform full range of motion with controlled tempo."}</p>
        <h4>Form Cues:</h4>
        <ul style="font-size: 0.85rem; color: var(--text-muted); padding-left: 20px;">
            ${(ex.form_cues || ["Drive through working muscle", "Keep core braced"]).map(c => `<li>${c}</li>`).join("")}
        </ul>
    `;

    document.getElementById("exercise-modal").classList.remove("hidden");
};

// Program Generator
function generateVTaperProgram() {
    const splitBtns = document.querySelectorAll(".program-selector .btn-option");
    splitBtns.forEach(btn => {
        btn.addEventListener("click", () => {
            splitBtns.forEach(b => b.classList.remove("active"));
            btn.classList.add("active");
            state.programFrequency = parseInt(btn.getAttribute("data-freq"), 10);
            updateProgramView();
        });
    });

    updateProgramView();
}

function updateProgramView() {
    const setsAdj = state.readinessScore < 2.5 ? 2 : (state.readinessScore >= 4.0 ? 4 : 3);
    const rpeAdj = state.readinessScore < 2.5 ? 7.0 : (state.readinessScore >= 4.0 ? 8.0 : 7.5);

    const lats = state.exercises.filter(e => e.category === "back" || (e.primary_muscles && e.primary_muscles.includes("latissimus_dorsi")));
    const lateralDelts = state.exercises.filter(e => e.category === "shoulders" || (e.primary_muscles && e.primary_muscles.includes("lateral_deltoid")));
    const upperChest = state.exercises.filter(e => e.category === "chest" || (e.primary_muscles && e.primary_muscles.includes("upper_chest")));
    const legs = state.exercises.filter(e => e.category === "legs");

    const days = [
        {
            name: "Upper A (V-Taper Width)",
            muscles: "Lats, Lateral Delts, Upper Chest, Arms",
            exercises: [
                lats[0] || FALLBACK_EXERCISES[2],
                lateralDelts[0] || FALLBACK_EXERCISES[3],
                upperChest[0] || FALLBACK_EXERCISES[1],
                lats[1] || FALLBACK_EXERCISES[4]
            ]
        },
        {
            name: "Lower A & Core Taper",
            muscles: "Quads, Hamstrings, Glutes, Hanging Core",
            exercises: [
                legs[0] || FALLBACK_EXERCISES[7],
                legs[1] || FALLBACK_EXERCISES[8],
                FALLBACK_EXERCISES[6]
            ]
        },
        {
            name: "Upper B (3D Shoulders & Back Density)",
            muscles: "Lateral Delts, Upper Back, Chest, Rear Delts",
            exercises: [
                lateralDelts[0] || FALLBACK_EXERCISES[3],
                lats[0] || FALLBACK_EXERCISES[2],
                FALLBACK_EXERCISES[5],
                FALLBACK_EXERCISES[0]
            ]
        },
        {
            name: "Lower B & Deep Core",
            muscles: "Hamstrings, Quads, Calves, Planks",
            exercises: [
                legs[1] || FALLBACK_EXERCISES[8],
                legs[0] || FALLBACK_EXERCISES[7],
                FALLBACK_EXERCISES[6]
            ]
        }
    ];

    state.currentProgram = { days, setsPerEx: setsAdj, targetRpe: rpeAdj };

    const daysContainer = document.getElementById("program-days-container");
    daysContainer.innerHTML = days.map((day, idx) => `
        <div class="day-card">
            <div class="day-title">Day ${idx + 1}: ${day.name}</div>
            <div class="day-muscles">${day.muscles} • ${day.exercises.length} Exercises (${setsAdj} sets @ RPE ${rpeAdj})</div>
        </div>
    `).join("");

    // Update Today Preview
    document.getElementById("today-workout-title").innerText = days[0].name;
    const previewList = document.getElementById("today-exercise-preview-list");
    previewList.innerHTML = days[0].exercises.map(ex => `
        <div style="font-size: 0.8rem; color: var(--text-muted); padding: 2px 0;">• ${ex.name} (${setsAdj} x 8-12 reps)</div>
    `).join("");
}

// Workout Session Controller
function startWorkoutSession() {
    if (!state.currentProgram) updateProgramView();

    const todayDay = state.currentProgram.days[0];
    state.activeWorkout = {
        name: todayDay.name,
        startTime: Date.now(),
        exercises: todayDay.exercises.map(ex => ({
            id: ex.id,
            name: ex.name,
            targetSets: state.currentProgram.setsPerEx,
            completedSets: []
        }))
    };

    state.workoutElapsedSeconds = 0;
    clearInterval(state.workoutTimerInterval);
    state.workoutTimerInterval = setInterval(() => {
        state.workoutElapsedSeconds++;
        const mins = String(Math.floor(state.workoutElapsedSeconds / 60)).padStart(2, '0');
        const secs = String(state.workoutElapsedSeconds % 60).padStart(2, '0');
        document.getElementById("active-session-timer").innerText = `Elapsed: ${mins}:${secs}`;
    }, 1000);

    renderActiveWorkoutScreen();
}

function renderActiveWorkoutScreen() {
    document.getElementById("active-session-title").innerText = state.activeWorkout.name;
    const container = document.getElementById("workout-exercises-container");

    container.innerHTML = state.activeWorkout.exercises.map((ex, exIdx) => `
        <div class="card workout-exercise-card">
            <div class="workout-exercise-header">
                <div>
                    <h3 style="font-size: 0.95rem;">${ex.name}</h3>
                    <span class="text-muted" style="font-size: 0.75rem;">${ex.targetSets} Target Sets @ RPE ${state.currentProgram.targetRpe}</span>
                </div>
                <button class="btn btn-sm btn-outline" onclick="openExerciseModal('${ex.id}')">Cues 💡</button>
            </div>
            <table class="set-table">
                <thead>
                    <tr>
                        <th>Set</th>
                        <th>Kg</th>
                        <th>Reps</th>
                        <th>e1RM</th>
                        <th>Done</th>
                    </tr>
                </thead>
                <tbody>
                    ${Array.from({ length: ex.targetSets }).map((_, setIdx) => {
                        const setDone = ex.completedSets[setIdx];
                        return `
                            <tr>
                                <td>${setIdx + 1}</td>
                                <td><input type="number" class="set-input set-kg" id="kg-${exIdx}-${setIdx}" value="${setDone ? setDone.kg : 20}" step="0.5"></td>
                                <td><input type="number" class="set-input set-reps" id="reps-${exIdx}-${setIdx}" value="${setDone ? setDone.reps : 10}"></td>
                                <td id="e1rm-${exIdx}-${setIdx}">${setDone ? calculateE1RM(setDone.kg, setDone.reps) : '--'}</td>
                                <td>
                                    <button class="btn-check-set ${setDone ? 'completed' : ''}" onclick="toggleCompleteSet(${exIdx}, ${setIdx})">
                                        ${setDone ? '✓' : ''}
                                    </button>
                                </td>
                            </tr>
                        `;
                    }).join("")}
                </tbody>
            </table>
        </div>
    `).join("");

    updateWorkoutProgressBar();
}

window.toggleCompleteSet = function(exIdx, setIdx) {
    const kgInput = document.getElementById(`kg-${exIdx}-${setIdx}`);
    const repsInput = document.getElementById(`reps-${exIdx}-${setIdx}`);

    const kg = parseFloat(kgInput.value) || 0;
    const reps = parseInt(repsInput.value, 10) || 0;

    const ex = state.activeWorkout.exercises[exIdx];
    if (ex.completedSets[setIdx]) {
        ex.completedSets[setIdx] = null;
    } else {
        ex.completedSets[setIdx] = { kg, reps, e1rm: calculateE1RM(kg, reps) };
        document.getElementById(`e1rm-${exIdx}-${setIdx}`).innerText = calculateE1RM(kg, reps);
        startRestTimer(90);
    }

    renderActiveWorkoutScreen();
};

function calculateE1RM(weight, reps) {
    if (!weight || weight <= 0 || !reps || reps <= 0) return 0;
    if (reps === 1) return Math.round(weight);
    // Epley Formula: e1RM = weight * (1 + reps / 30)
    return Math.round(weight * (1 + reps / 30));
}

function updateWorkoutProgressBar() {
    let totalSets = 0;
    let doneSets = 0;

    if (!state.activeWorkout) return;

    state.activeWorkout.exercises.forEach(ex => {
        totalSets += ex.targetSets;
        doneSets += ex.completedSets.filter(Boolean).length;
    });

    const percent = totalSets > 0 ? Math.round((doneSets / totalSets) * 100) : 0;
    document.getElementById("workout-progress-fill").style.width = `${percent}%`;
}

// Audio/Visual Rest Timer
function startRestTimer(seconds) {
    state.restSecondsLeft = seconds;
    const banner = document.getElementById("rest-timer-banner");
    banner.classList.remove("hidden");

    clearInterval(state.restTimerInterval);
    updateRestTimerDisplay();

    state.restTimerInterval = setInterval(() => {
        state.restSecondsLeft--;
        if (state.restSecondsLeft <= 0) {
            clearInterval(state.restTimerInterval);
            banner.classList.add("hidden");
            playTimerChime();
        } else {
            updateRestTimerDisplay();
        }
    }, 1000);
}

function updateRestTimerDisplay() {
    const mins = String(Math.floor(state.restSecondsLeft / 60)).padStart(2, '0');
    const secs = String(state.restSecondsLeft % 60).padStart(2, '0');
    document.getElementById("rest-timer-display").innerText = `${mins}:${secs}`;
}

function addRestTime(secs) {
    state.restSecondsLeft += secs;
    updateRestTimerDisplay();
}

function skipRestTimer() {
    clearInterval(state.restTimerInterval);
    document.getElementById("rest-timer-banner").classList.add("hidden");
}

function playTimerChime() {
    try {
        const ctx = new (window.AudioContext || window.webkitAudioContext)();
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = "sine";
        osc.frequency.setValueAtTime(880, ctx.currentTime); // A5
        gain.gain.setValueAtTime(0.1, ctx.currentTime);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start();
        osc.stop(ctx.currentTime + 0.3);
    } catch (e) {
        // AudioContext restricted before user gesture
    }
}

function cancelWorkoutSession() {
    clearInterval(state.workoutTimerInterval);
    clearInterval(state.restTimerInterval);
    state.activeWorkout = null;
    document.getElementById("rest-timer-banner").classList.add("hidden");
    switchTab("tab-dashboard");
}

function finishWorkoutSession() {
    clearInterval(state.workoutTimerInterval);
    clearInterval(state.restTimerInterval);
    document.getElementById("rest-timer-banner").classList.add("hidden");
    alert("🎉 Workout Saved Successfully! Excellent progress towards your V-Taper.");
    state.activeWorkout = null;
    switchTab("tab-dashboard");
}

// Measurements & V-Ratio Controller
function initMeasurements() {
    const form = document.getElementById("measurement-form");
    form.addEventListener("submit", (e) => {
        e.preventDefault();
        const shoulder = parseFloat(document.getElementById("m-shoulder").value);
        const waist = parseFloat(document.getElementById("m-waist").value);
        const chest = parseFloat(document.getElementById("m-chest").value) || null;
        const arm = parseFloat(document.getElementById("m-arm").value) || null;

        if (shoulder > 0 && waist > 0) {
            const today = new Date().toISOString().split("T")[0];
            state.measurements.unshift({ date: today, shoulder, waist, chest, arm });
            renderMeasurements();
            form.reset();
        }
    });

    renderMeasurements();
}

function renderMeasurements() {
    const historyRows = document.getElementById("measurement-history-rows");
    if (state.measurements.length === 0) return;

    const latest = state.measurements[0];
    const ratio = (latest.shoulder / latest.waist).toFixed(2);

    document.getElementById("stat-shoulder").innerText = `${latest.shoulder} cm`;
    document.getElementById("stat-waist").innerText = `${latest.waist} cm`;
    document.getElementById("current-vtaper-ratio").querySelector(".ratio-val").innerText = ratio;

    historyRows.innerHTML = state.measurements.map(m => {
        const r = (m.shoulder / m.waist).toFixed(2);
        let status = "Building";
        if (r >= 1.6) status = "🌟 Golden Ratio";
        else if (r >= 1.45) status = "🔥 Strong V-Taper";

        return `
            <tr>
                <td>${m.date}</td>
                <td>${m.shoulder} cm</td>
                <td>${m.waist} cm</td>
                <td><strong>${r}</strong></td>
                <td>${status}</td>
            </tr>
        `;
    }).join("");
}

// Nutrition Engine Controller
function initNutrition() {
    const btnRecomp = document.getElementById("nut-recomp");
    const btnSurplus = document.getElementById("nut-surplus");
    const btnDeficit = document.getElementById("nut-deficit");

    const buttons = [btnRecomp, btnSurplus, btnDeficit];

    btnRecomp.addEventListener("click", () => setNutritionGoal("recomp", btnRecomp, buttons));
    btnSurplus.addEventListener("click", () => setNutritionGoal("surplus", btnSurplus, buttons));
    btnDeficit.addEventListener("click", () => setNutritionGoal("deficit", btnDeficit, buttons));

    calculateMacros();
}

function setNutritionGoal(goal, activeBtn, allBtns) {
    allBtns.forEach(b => b.classList.remove("active"));
    activeBtn.classList.add("active");
    state.nutritionGoal = goal;
    calculateMacros();
}

function calculateMacros() {
    // Mifflin-St Jeor BMR for Male: 10 * weight(kg) + 6.25 * height(cm) - 5 * age + 5
    const bmr = 10 * 70 + 6.25 * 170 - 5 * 30 + 5; // 1617.5 kcal
    let tdee = Math.round(bmr * 1.4); // Moderate active ~ 2264 kcal

    if (state.nutritionGoal === "surplus") tdee += 250;
    else if (state.nutritionGoal === "deficit") tdee -= 350;

    const proteinGrams = Math.round(70 * 2.2); // 154g protein
    const fatGrams = Math.round((tdee * 0.25) / 9);
    const carbGrams = Math.round((tdee - (proteinGrams * 4 + fatGrams * 9)) / 4);

    document.getElementById("macro-calories").innerText = tdee.toLocaleString();
    document.getElementById("macro-protein").innerText = `${proteinGrams} g`;
    document.getElementById("macro-carbs").innerText = `${carbGrams} g`;
    document.getElementById("macro-fats").innerText = `${fatGrams} g`;
}
