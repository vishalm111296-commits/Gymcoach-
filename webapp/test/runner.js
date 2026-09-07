/**
 * GymCoach WebApp Automated Unit Test Suite (runner.js)
 * Executes assertions for exercise dataset, routines, progression logic, measurements, and persistence.
 */

import assert from 'node:assert';
import { ExerciseLibrary, PresetRoutines, filterExercises, getExerciseById } from '../js/exercises.js';
import { ProgressionEngine } from '../js/progression.js';
import { MeasurementsTracker } from '../js/measurements.js';
import { store } from '../js/store.js';

let passed = 0;
let failed = 0;

function test(name, fn) {
  try {
    fn();
    console.log(`  ✓ PASS: ${name}`);
    passed++;
  } catch (err) {
    console.error(`  ✕ FAIL: ${name}`);
    console.error(err);
    failed++;
  }
}

console.log('====================================================');
console.log(' GymCoach WebApp Automated Test Suite Verification');
console.log('====================================================\n');

// --- 1. EXERCISE DATASET & EQUIPMENT CONSTRAINTS ---
console.log('--- 1. Exercise Dataset & Equipment Constraints ---');

test('All exercises in ExerciseLibrary must use ONLY dumbbell or bodyweight equipment', () => {
  const allowed = ['dumbbell', 'bodyweight', 'bench', 'mat', 'pullup_bar', 'dip_station'];
  for (const ex of ExerciseLibrary) {
    for (const eq of ex.equipment) {
      assert.strictEqual(
        allowed.includes(eq),
        true,
        `Exercise ${ex.id} contains forbidden equipment: ${eq}`
      );
    }
  }
});

test('Filter exercises by category correctly returns target muscle subset', () => {
  const backExs = filterExercises({ category: 'back' });
  assert.ok(backExs.length > 0, 'Back exercises should not be empty');
  assert.ok(backExs.every(e => e.category === 'back'));
});

test('Preset routines contain valid exercise IDs and target sets', () => {
  for (const routine of PresetRoutines) {
    assert.ok(routine.days.length >= 3, `Routine ${routine.id} must have at least 3 days`);
    for (const day of routine.days) {
      for (const item of day.exercises) {
        const ex = getExerciseById(item.exerciseId);
        assert.ok(ex, `Exercise ${item.exerciseId} in routine ${routine.id} must exist in ExerciseLibrary`);
        assert.ok(item.targetSets > 0, `Target sets for ${item.exerciseId} must be > 0`);
      }
    }
  }
});

// --- 2. PROGRESSION & AUTOREGULATION ENGINE ---
console.log('\n--- 2. Progression & Autoregulation Engine ---');

test('Recommend INCREASE_WEIGHT when hitting top rep range across all sets with >=1 RIR', () => {
  const exerciseLog = {
    exerciseId: 'db_one_arm_row',
    sets: [
      { completed: true, weightKg: 15, reps: 12, rir: 2 },
      { completed: true, weightKg: 15, reps: 12, rir: 2 },
      { completed: true, weightKg: 15, reps: 12, rir: 1 }
    ]
  };
  const evalResult = ProgressionEngine.evaluateProgression(exerciseLog, '8-12');
  assert.strictEqual(evalResult.action, 'INCREASE_WEIGHT');
  assert.ok(evalResult.recommendation.includes('+1.0'));
});

test('Recommend MAINTAIN_OR_LIGHTEN when reps fall below target minimum', () => {
  const exerciseLog = {
    exerciseId: 'incline_db_press',
    sets: [
      { completed: true, weightKg: 20, reps: 6, rir: 0 },
      { completed: true, weightKg: 20, reps: 5, rir: 0 }
    ]
  };
  const evalResult = ProgressionEngine.evaluateProgression(exerciseLog, '8-12');
  assert.strictEqual(evalResult.action, 'MAINTAIN_OR_LIGHTEN');
});

test('Recommend ADD_REP when inside rep range without hitting top target', () => {
  const exerciseLog = {
    exerciseId: 'db_lateral_raise',
    sets: [
      { completed: true, weightKg: 8, reps: 10, rir: 2 },
      { completed: true, weightKg: 8, reps: 10, rir: 2 }
    ]
  };
  const evalResult = ProgressionEngine.evaluateProgression(exerciseLog, '8-12');
  assert.strictEqual(evalResult.action, 'ADD_REP');
});

// --- 3. V-TAPER MEASUREMENTS & RATIO CALCULATION ---
console.log('\n--- 3. V-Taper Measurements & Ratio Calculation ---');

test('Calculate Shoulder-to-Waist ratio accurately', () => {
  const ratio = MeasurementsTracker.calculateRatio(112.5, 80.0);
  assert.strictEqual(ratio, 1.406);
});

test('Categorize ratio into correct V-taper frame labels', () => {
  const eliteCat = MeasurementsTracker.getRatioCategory(1.60);
  assert.strictEqual(eliteCat.label, 'Elite V-Taper Frame');

  const strongCat = MeasurementsTracker.getRatioCategory(1.48);
  assert.strictEqual(strongCat.label, 'Strong V-Shape');

  const modCat = MeasurementsTracker.getRatioCategory(1.38);
  assert.strictEqual(modCat.label, 'Moderate V-Shape');
});

// --- 4. DATA PERSISTENCE & STORE BACKUP ---
console.log('\n--- 4. Data Persistence & Store Backup ---');

test('Store fallback memory operations read and write correctly', () => {
  const profile = store.getUserProfile();
  assert.strictEqual(profile.weightKg, 70.0);
  assert.strictEqual(profile.heightCm, 170.0);

  const testMeasurement = {
    shouldersCm: 115.0,
    waistCm: 79.0,
    chestCm: 98.0,
    weightKg: 70.0
  };
  const saved = store.saveBodyMeasurement(testMeasurement);
  assert.ok(saved.id);
  assert.strictEqual(saved.ratio, 1.456);
});

test('Export JSON and Import JSON backup cycle works cleanly', () => {
  const jsonStr = store.exportDataJSON();
  assert.ok(jsonStr.length > 50);

  const importedOk = store.importDataJSON(jsonStr);
  assert.strictEqual(importedOk, true);
});

console.log('\n====================================================');
console.log(` Summary: ${passed} Passed, ${failed} Failed`);
console.log('====================================================\n');

if (failed > 0) {
  process.exit(1);
} else {
  process.exit(0);
}
