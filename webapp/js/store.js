/**
 * GymCoach Local Data Persistence Layer (store.js)
 * Manages localStorage / in-memory fallback, export/import JSON, and state getters/setters.
 */

class Store {
  constructor() {
    this.memoryStorage = {};
    this.isLocalStorageAvailable = this._checkLocalStorage();
    this._initDefaults();
  }

  _checkLocalStorage() {
    try {
      if (typeof window === 'undefined' || !window.localStorage) return false;
      const testKey = '__gymcoach_test__';
      window.localStorage.setItem(testKey, testKey);
      window.localStorage.removeItem(testKey);
      return true;
    } catch (e) {
      return false;
    }
  }

  getItem(key) {
    if (this.isLocalStorageAvailable) {
      try {
        const val = window.localStorage.getItem(key);
        return val ? JSON.parse(val) : null;
      } catch (e) {
        console.error(`Error reading ${key} from localStorage`, e);
      }
    }
    return this.memoryStorage[key] || null;
  }

  setItem(key, value) {
    if (this.isLocalStorageAvailable) {
      try {
        window.localStorage.setItem(key, JSON.stringify(value));
        return;
      } catch (e) {
        console.error(`Error writing ${key} to localStorage`, e);
      }
    }
    this.memoryStorage[key] = value;
  }

  removeItem(key) {
    if (this.isLocalStorageAvailable) {
      try {
        window.localStorage.removeItem(key);
        return;
      } catch (e) {
        console.error(`Error removing ${key} from localStorage`, e);
      }
    }
    delete this.memoryStorage[key];
  }

  _initDefaults() {
    // Default User Profile for V-taper User (30yo, 70kg, 170cm)
    if (!this.getItem('gc_profile')) {
      this.setItem('gc_profile', {
        age: 30,
        weightKg: 70.0,
        heightCm: 170.0,
        equipment: ['dumbbell', 'bodyweight'],
        selectedSplit: '4-day',
        createdAt: new Date().toISOString()
      });
    }

    if (!this.getItem('gc_workout_logs')) {
      this.setItem('gc_workout_logs', []);
    }

    if (!this.getItem('gc_body_measurements')) {
      // Pre-seed initial benchmark baseline
      this.setItem('gc_body_measurements', [
        {
          id: 'm_baseline',
          date: new Date().toISOString().split('T')[0],
          shouldersCm: 112.5,
          waistCm: 80.0,
          chestCm: 96.0,
          weightKg: 70.0,
          ratio: 1.406
        }
      ]);
    }
  }

  // --- Profile Methods ---
  getUserProfile() {
    return this.getItem('gc_profile');
  }

  saveUserProfile(profile) {
    this.setItem('gc_profile', profile);
  }

  // --- Workout Log Methods ---
  getWorkoutLogs() {
    return this.getItem('gc_workout_logs') || [];
  }

  saveWorkoutLog(log) {
    const logs = this.getWorkoutLogs();
    if (!log.id) log.id = 'wlog_' + Date.now();
    if (!log.completedAt) log.completedAt = new Date().toISOString();
    logs.unshift(log);
    this.setItem('gc_workout_logs', logs);
    return log;
  }

  deleteWorkoutLog(id) {
    const logs = this.getWorkoutLogs().filter(l => l.id !== id);
    this.setItem('gc_workout_logs', logs);
  }

  // --- Active Workout Draft ---
  getActiveWorkout() {
    return this.getItem('gc_active_workout');
  }

  saveActiveWorkout(draft) {
    this.setItem('gc_active_workout', draft);
  }

  clearActiveWorkout() {
    this.removeItem('gc_active_workout');
  }

  // --- Body Measurements ---
  getBodyMeasurements() {
    return this.getItem('gc_body_measurements') || [];
  }

  saveBodyMeasurement(m) {
    const list = this.getBodyMeasurements();
    const shoulders = parseFloat(m.shouldersCm);
    const waist = parseFloat(m.waistCm);
    const ratio = waist > 0 ? (shoulders / waist) : 0;

    const entry = {
      id: m.id || ('m_' + Date.now()),
      date: m.date || new Date().toISOString().split('T')[0],
      shouldersCm: shoulders,
      waistCm: waist,
      chestCm: parseFloat(m.chestCm) || 0,
      weightKg: parseFloat(m.weightKg) || 70.0,
      ratio: parseFloat(ratio.toFixed(3))
    };

    list.unshift(entry);
    this.setItem('gc_body_measurements', list);
    return entry;
  }

  deleteBodyMeasurement(id) {
    const list = this.getBodyMeasurements().filter(m => m.id !== id);
    this.setItem('gc_body_measurements', list);
  }

  // --- Backup & Restore ---
  exportDataJSON() {
    const data = {
      profile: this.getUserProfile(),
      workoutLogs: this.getWorkoutLogs(),
      bodyMeasurements: this.getBodyMeasurements(),
      activeWorkout: this.getActiveWorkout(),
      exportedAt: new Date().toISOString()
    };
    return JSON.stringify(data, null, 2);
  }

  importDataJSON(jsonStr) {
    try {
      const data = JSON.parse(jsonStr);
      if (data.profile) this.setItem('gc_profile', data.profile);
      if (Array.isArray(data.workoutLogs)) this.setItem('gc_workout_logs', data.workoutLogs);
      if (Array.isArray(data.bodyMeasurements)) this.setItem('gc_body_measurements', data.bodyMeasurements);
      if (data.activeWorkout) this.setItem('gc_active_workout', data.activeWorkout);
      return true;
    } catch (e) {
      console.error('Failed to import JSON data', e);
      return false;
    }
  }

  clearAllData() {
    this.removeItem('gc_profile');
    this.removeItem('gc_workout_logs');
    this.removeItem('gc_body_measurements');
    this.removeItem('gc_active_workout');
    this._initDefaults();
  }
}

export const store = new Store();
