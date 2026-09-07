/**
 * GymCoach Workout Logging Engine & Rest Timer (workout.js)
 * Mid-workout logging, previous-performance prefilling, auto-rest timer, and session completion.
 */

import { store } from './store.js';
import { getExerciseById, ExerciseLibrary } from './exercises.js';

export class WorkoutEngine {
  constructor() {
    this.activeWorkout = null;
    this.timerInterval = null;
    this.restTimerSeconds = 0;
    this.restTimerMax = 0;
    this.restInterval = null;
    this.clockInterval = null;
  }

  init() {
    const draft = store.getActiveWorkout();
    if (draft) {
      this.activeWorkout = draft;
      this._startClock();
    }
  }

  startRoutine(routine, dayIndex = 0) {
    const day = routine.days[dayIndex] || routine.days[0];
    const previousLogs = store.getWorkoutLogs();

    const exercises = day.exercises.map(item => {
      const exDetail = getExerciseById(item.exerciseId) || { name: item.exerciseId };
      const lastPerf = this._getLastPerformance(item.exerciseId, previousLogs);

      const sets = [];
      for (let i = 0; i < (item.targetSets || 3); i++) {
        const prevSet = lastPerf && lastPerf.sets && lastPerf.sets[i];
        sets.push({
          setNumber: i + 1,
          weightKg: prevSet ? prevSet.weightKg : (item.defaultWeightKg || 12.5),
          reps: prevSet ? prevSet.reps : parseInt(item.repRange || 10),
          rir: item.targetRir || 2,
          completed: false,
          previousDisplay: prevSet ? `${prevSet.weightKg}kg × ${prevSet.reps}` : 'First time'
        });
      }

      return {
        exerciseId: item.exerciseId,
        exerciseName: exDetail.name,
        repRange: item.repRange || '8-12',
        restSeconds: exDetail.restSeconds || 90,
        sets
      };
    });

    this.activeWorkout = {
      id: 'wsession_' + Date.now(),
      routineId: routine.id,
      routineName: routine.name,
      dayName: day.dayName,
      startedAt: new Date().toISOString(),
      elapsedSeconds: 0,
      exercises
    };

    store.saveActiveWorkout(this.activeWorkout);
    this._startClock();
    return this.activeWorkout;
  }

  startBlankWorkout() {
    this.activeWorkout = {
      id: 'wsession_' + Date.now(),
      routineId: 'custom',
      routineName: 'Custom V-Taper Session',
      dayName: 'Impromptu Workout',
      startedAt: new Date().toISOString(),
      elapsedSeconds: 0,
      exercises: []
    };

    store.saveActiveWorkout(this.activeWorkout);
    this._startClock();
    return this.activeWorkout;
  }

  _startClock() {
    if (this.clockInterval) clearInterval(this.clockInterval);
    this.clockInterval = setInterval(() => {
      if (this.activeWorkout) {
        this.activeWorkout.elapsedSeconds = (this.activeWorkout.elapsedSeconds || 0) + 1;
        store.saveActiveWorkout(this.activeWorkout);

        // Dispatch custom clock tick event
        window.dispatchEvent(new CustomEvent('gc_workout_tick', { detail: this.activeWorkout }));
      }
    }, 1000);
  }

  stopClock() {
    if (this.clockInterval) {
      clearInterval(this.clockInterval);
      this.clockInterval = null;
    }
  }

  toggleSetComplete(exerciseIndex, setIndex, weightKg, reps, rir) {
    if (!this.activeWorkout) return;

    const ex = this.activeWorkout.exercises[exerciseIndex];
    if (!ex) return;

    const set = ex.sets[setIndex];
    if (!set) return;

    set.weightKg = parseFloat(weightKg) || set.weightKg;
    set.reps = parseInt(reps) || set.reps;
    set.rir = parseInt(rir) !== undefined ? parseInt(rir) : set.rir;
    set.completed = !set.completed;

    store.saveActiveWorkout(this.activeWorkout);

    // Trigger Rest Timer if set was just marked completed
    if (set.completed) {
      this.startRestTimer(ex.restSeconds || 90);
    }

    return set;
  }

  addSetToExercise(exerciseIndex) {
    if (!this.activeWorkout) return;
    const ex = this.activeWorkout.exercises[exerciseIndex];
    if (!ex) return;

    const lastSet = ex.sets[ex.sets.length - 1];
    ex.sets.push({
      setNumber: ex.sets.length + 1,
      weightKg: lastSet ? lastSet.weightKg : 10,
      reps: lastSet ? lastSet.reps : 10,
      rir: lastSet ? lastSet.rir : 2,
      completed: false,
      previousDisplay: lastSet ? `${lastSet.weightKg}kg × ${lastSet.reps}` : 'New set'
    });

    store.saveActiveWorkout(this.activeWorkout);
  }

  addExerciseToWorkout(exerciseId) {
    if (!this.activeWorkout) return;
    const exDetail = getExerciseById(exerciseId);
    if (!exDetail) return;

    const previousLogs = store.getWorkoutLogs();
    const lastPerf = this._getLastPerformance(exerciseId, previousLogs);

    const sets = [1, 2, 3].map(i => {
      const prevSet = lastPerf && lastPerf.sets && lastPerf.sets[i - 1];
      return {
        setNumber: i,
        weightKg: prevSet ? prevSet.weightKg : 10,
        reps: prevSet ? prevSet.reps : 10,
        rir: 2,
        completed: false,
        previousDisplay: prevSet ? `${prevSet.weightKg}kg × ${prevSet.reps}` : 'First time'
      };
    });

    this.activeWorkout.exercises.push({
      exerciseId,
      exerciseName: exDetail.name,
      repRange: exDetail.repRange || '8-12',
      restSeconds: exDetail.restSeconds || 90,
      sets
    });

    store.saveActiveWorkout(this.activeWorkout);
  }

  startRestTimer(seconds) {
    this.stopRestTimer();
    this.restTimerSeconds = seconds;
    this.restTimerMax = seconds;

    window.dispatchEvent(new CustomEvent('gc_rest_timer_start', {
      detail: { remaining: this.restTimerSeconds, total: this.restTimerMax }
    }));

    this.restInterval = setInterval(() => {
      this.restTimerSeconds--;

      window.dispatchEvent(new CustomEvent('gc_rest_timer_tick', {
        detail: { remaining: this.restTimerSeconds, total: this.restTimerMax }
      }));

      if (this.restTimerSeconds <= 0) {
        this.stopRestTimer();
        this._playBeepSound();
        window.dispatchEvent(new CustomEvent('gc_rest_timer_complete'));
      }
    }, 1000);
  }

  addRestTime(additionalSeconds) {
    this.restTimerSeconds += additionalSeconds;
    this.restTimerMax += additionalSeconds;
    window.dispatchEvent(new CustomEvent('gc_rest_timer_tick', {
      detail: { remaining: this.restTimerSeconds, total: this.restTimerMax }
    }));
  }

  stopRestTimer() {
    if (this.restInterval) {
      clearInterval(this.restInterval);
      this.restInterval = null;
    }
    this.restTimerSeconds = 0;
  }

  _playBeepSound() {
    try {
      const ctx = new (window.AudioContext || window.webkitAudioContext)();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(880, ctx.currentTime); // A5 pitch
      gain.gain.setValueAtTime(0.3, ctx.currentTime);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start();
      osc.stop(ctx.currentTime + 0.4);
    } catch (e) {
      console.log('AudioContext beep fallback', e);
    }
  }

  finishWorkout() {
    if (!this.activeWorkout) return null;

    this.stopClock();
    this.stopRestTimer();

    // Calculate session summaries
    let totalVolume = 0;
    let completedSetsCount = 0;

    this.activeWorkout.exercises.forEach(ex => {
      ex.sets.forEach(s => {
        if (s.completed) {
          completedSetsCount++;
          totalVolume += (s.weightKg * s.reps);
        }
      });
    });

    const finishedLog = {
      ...this.activeWorkout,
      completedAt: new Date().toISOString(),
      totalVolumeKg: Math.round(totalVolume),
      completedSetsCount
    };

    store.saveWorkoutLog(finishedLog);
    store.clearActiveWorkout();
    this.activeWorkout = null;

    return finishedLog;
  }

  cancelWorkout() {
    this.stopClock();
    this.stopRestTimer();
    store.clearActiveWorkout();
    this.activeWorkout = null;
  }

  _getLastPerformance(exerciseId, logs) {
    for (const log of logs) {
      if (!log.exercises) continue;
      const match = log.exercises.find(e => e.exerciseId === exerciseId);
      if (match) {
        return match;
      }
    }
    return null;
  }
}

export const workoutEngine = new WorkoutEngine();
