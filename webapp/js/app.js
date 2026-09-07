/**
 * GymCoach WebApp Main Application Controller (app.js)
 * Binds DOM event listeners, navigation tabs, exercise library, workout modal, and backup handlers.
 */

import { store } from './store.js';
import { ExerciseLibrary, PresetRoutines, getExerciseById, filterExercises } from './exercises.js';
import { SVGIllustrations } from './svg_illustrations.js';
import { workoutEngine } from './workout.js';
import { ProgressionEngine } from './progression.js';
import { MeasurementsTracker } from './measurements.js';
import { renderEvidencePanel } from './evidence.js';

document.addEventListener('DOMContentLoaded', () => {
  // Initialize Engine & UI
  workoutEngine.init();
  initTheme();
  initNavigation();
  initWorkoutTab();
  initExerciseLibrary();
  initHistoryTab();
  initMeasurementsTab();
  initEvidenceTab();
  initBackupRestore();
  initEventListeners();

  checkActiveWorkoutBanner();
});

// --- Theme Toggling ---
function initTheme() {
  const btnTheme = document.getElementById('btn-theme-toggle');
  const savedTheme = localStorage.getItem('gc_theme') || 'dark';
  document.documentElement.setAttribute('data-theme', savedTheme);
  btnTheme.textContent = savedTheme === 'dark' ? '🌙' : '☀️';

  btnTheme.addEventListener('click', () => {
    const current = document.documentElement.getAttribute('data-theme');
    const next = current === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', next);
    localStorage.setItem('gc_theme', next);
    btnTheme.textContent = next === 'dark' ? '🌙' : '☀️';
  });
}

// --- Navigation Tabs ---
function initNavigation() {
  const navItems = document.querySelectorAll('.bottom-nav .nav-item');
  const tabPanes = document.querySelectorAll('.tab-pane');

  navItems.forEach(item => {
    item.addEventListener('click', () => {
      const targetTab = item.getAttribute('data-tab');
      navItems.forEach(i => i.classList.remove('active'));
      tabPanes.forEach(p => p.classList.remove('active'));

      item.classList.add('active');
      const pane = document.getElementById(targetTab);
      if (pane) pane.classList.add('active');

      // Refresh dynamic tabs
      if (targetTab === 'tab-progress') renderHistory();
      if (targetTab === 'tab-measurements') renderMeasurements();
    });
  });
}

// --- Workout Tab & Routines ---
function initWorkoutTab() {
  const routinesList = document.getElementById('routines-list');
  const filterPills = document.querySelectorAll('#split-filter-pills .pill');

  function renderRoutines(splitType = '4-day') {
    const filtered = PresetRoutines.filter(r => r.splitType === splitType);
    routinesList.innerHTML = filtered.map(r => `
      <div class="routine-card">
        <div>
          <h4>${r.name}</h4>
          <p class="text-sm muted-text">${r.description}</p>
          <div class="routine-tags">
            <span class="badge badge-primary">${r.days.length} Days / Wk</span>
            <span class="badge badge-accent">DB + BW Only</span>
          </div>
        </div>
        <button class="btn btn-primary btn-sm btn-block btn-start-routine" data-routine-id="${r.id}">
          Start Routine Day
        </button>
      </div>
    `).join('');

    // Event listeners for routine buttons
    routinesList.querySelectorAll('.btn-start-routine').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const rId = e.target.getAttribute('data-routine-id');
        const routine = PresetRoutines.find(r => r.id === rId);
        if (routine) {
          workoutEngine.startRoutine(routine, 0);
          openWorkoutModal();
        }
      });
    });
  }

  filterPills.forEach(pill => {
    pill.addEventListener('click', () => {
      filterPills.forEach(p => p.classList.remove('active'));
      pill.classList.add('active');
      renderRoutines(pill.getAttribute('data-split'));
    });
  });

  renderRoutines('4-day');

  // Blank Workout Button
  document.getElementById('btn-start-blank-workout').addEventListener('click', () => {
    workoutEngine.startBlankWorkout();
    openWorkoutModal();
  });

  // Resume Workout Banner Button
  document.getElementById('btn-resume-workout').addEventListener('click', () => {
    openWorkoutModal();
  });
}

function checkActiveWorkoutBanner() {
  const banner = document.getElementById('active-workout-banner');
  const stats = document.getElementById('active-routine-stats');
  const routineName = document.getElementById('active-routine-name');

  if (workoutEngine.activeWorkout) {
    banner.classList.remove('hidden');
    routineName.textContent = workoutEngine.activeWorkout.routineName;
    const completedSets = workoutEngine.activeWorkout.exercises.flatMap(e => e.sets).filter(s => s.completed).length;
    stats.textContent = `${workoutEngine.activeWorkout.exercises.length} Exercises | ${completedSets} Sets Logged`;
  } else {
    banner.classList.add('hidden');
  }
}

// --- Exercise Library ---
function initExerciseLibrary() {
  const searchInput = document.getElementById('exercise-search');
  const categoryPills = document.querySelectorAll('.filter-pills-scroll .filter-pill');
  const grid = document.getElementById('exercise-grid');

  let currentCategory = 'all';
  let currentSearch = '';

  function renderLibrary() {
    const exercises = filterExercises({ category: currentCategory, search: currentSearch });
    grid.innerHTML = exercises.map(ex => `
      <div class="exercise-card" data-id="${ex.id}">
        <div class="exercise-card-header">
          <span class="exercise-title">${ex.name}</span>
          <span class="vtaper-score-pill">V-Taper ★ ${ex.vTaperRelevance}/10</span>
        </div>
        <p class="text-sm muted-text">${ex.setup.substring(0, 75)}...</p>
        <div class="routine-tags">
          <span class="badge badge-primary">${ex.category.toUpperCase()}</span>
          <span class="badge badge-accent">${ex.repRange} Reps</span>
        </div>
      </div>
    `).join('');

    grid.querySelectorAll('.exercise-card').forEach(card => {
      card.addEventListener('click', () => {
        openExerciseDetailModal(card.getAttribute('data-id'));
      });
    });
  }

  searchInput.addEventListener('input', (e) => {
    currentSearch = e.target.value;
    renderLibrary();
  });

  categoryPills.forEach(pill => {
    pill.addEventListener('click', () => {
      categoryPills.forEach(p => p.classList.remove('active'));
      pill.classList.add('active');
      currentCategory = pill.getAttribute('data-category');
      renderLibrary();
    });
  });

  renderLibrary();
}

// --- Exercise Detail Modal ---
function openExerciseDetailModal(exerciseId) {
  const ex = getExerciseById(exerciseId);
  if (!ex) return;

  const modal = document.getElementById('exercise-detail-modal');
  const title = document.getElementById('exercise-detail-title');
  const body = document.getElementById('exercise-detail-body');

  title.textContent = ex.name;

  const svgHTML = SVGIllustrations.getIllustration(ex.id, ex.category);

  body.innerHTML = `
    <div class="svg-motion-container">
      ${svgHTML}
    </div>
    <div style="margin-bottom: 12px;">
      <span class="badge badge-primary">Target: ${ex.primaryMuscles.join(', ')}</span>
      <span class="badge badge-accent">V-Taper Relevance: ${ex.vTaperRelevance}/10</span>
    </div>
    <div style="margin-bottom: 12px;">
      <h4>Setup</h4>
      <p class="text-sm muted-text">${ex.setup}</p>
    </div>
    <div style="margin-bottom: 12px;">
      <h4>Execution & Form Cues</h4>
      <ul style="padding-left: 18px; font-size: 0.85rem; color: var(--text-secondary);">
        ${ex.formCues.map(c => `<li>${c}</li>`).join('')}
      </ul>
    </div>
    <div style="margin-bottom: 12px;">
      <h4>Common Mistakes</h4>
      <ul style="padding-left: 18px; font-size: 0.85rem; color: var(--danger-color);">
        ${ex.commonMistakes.map(m => `<li>${m}</li>`).join('')}
      </ul>
    </div>
  `;

  modal.classList.remove('hidden');
}

// --- Workout Modal & Logger ---
function openWorkoutModal() {
  if (!workoutEngine.activeWorkout) return;
  const modal = document.getElementById('workout-modal');
  modal.classList.remove('hidden');
  renderActiveWorkoutUI();
}

function renderActiveWorkoutUI() {
  const active = workoutEngine.activeWorkout;
  if (!active) return;

  document.getElementById('workout-modal-title').textContent = active.routineName;
  const container = document.getElementById('active-workout-exercises');

  container.innerHTML = active.exercises.map((ex, exIdx) => {
    // Evaluate double progression recommendation from previous history
    const prevLogs = store.getWorkoutLogs();
    const prevExLog = prevLogs.flatMap(l => l.exercises || []).find(e => e.exerciseId === ex.exerciseId);
    const progEval = ProgressionEngine.evaluateProgression(prevExLog, ex.repRange);

    return `
      <div class="card" style="margin-bottom: 14px;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
          <div>
            <h4 style="font-size: 1.05rem;">${ex.exerciseName}</h4>
            <span class="text-sm muted-text">Target: ${ex.repRange} reps | Rest: ${ex.restSeconds}s</span>
          </div>
          <button class="btn btn-xs btn-outline btn-show-ex-detail" data-ex-id="${ex.exerciseId}">Cues</button>
        </div>

        <!-- Progression Tip Banner -->
        <div style="background-color: var(--accent-light); padding: 6px 10px; border-radius: 6px; font-size: 0.75rem; margin-bottom: 10px;">
          <strong>${progEval.badge || 'Target Target'}</strong>: ${progEval.rationale}
        </div>

        <!-- Sets Table -->
        <table class="data-table">
          <thead>
            <tr>
              <th>Set</th>
              <th>Previous</th>
              <th>kg</th>
              <th>Reps</th>
              <th>Done</th>
            </tr>
          </thead>
          <tbody>
            ${ex.sets.map((s, setIdx) => `
              <tr>
                <td><strong>${s.setNumber}</strong></td>
                <td class="text-sm muted-text">${s.previousDisplay}</td>
                <td>
                  <input type="number" step="0.5" class="form-input set-input-weight"
                    data-ex-idx="${exIdx}" data-set-idx="${setIdx}" value="${s.weightKg}"
                    style="width: 60px; min-height: 36px; padding: 4px;">
                </td>
                <td>
                  <input type="number" class="form-input set-input-reps"
                    data-ex-idx="${exIdx}" data-set-idx="${setIdx}" value="${s.reps}"
                    style="width: 55px; min-height: 36px; padding: 4px;">
                </td>
                <td>
                  <button class="btn btn-sm ${s.completed ? 'btn-success' : 'btn-outline'} btn-toggle-set"
                    data-ex-idx="${exIdx}" data-set-idx="${setIdx}" style="min-width: 44px;">
                    ${s.completed ? '✓' : '—'}
                  </button>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
        <button class="btn btn-xs btn-secondary btn-add-set" data-ex-idx="${exIdx}" style="margin-top: 8px;">
          ➕ Add Set
        </button>
      </div>
    `;
  }).join('');

  // Event Listeners inside Workout Modal
  container.querySelectorAll('.btn-show-ex-detail').forEach(btn => {
    btn.addEventListener('click', (e) => {
      openExerciseDetailModal(e.target.getAttribute('data-ex-id'));
    });
  });

  container.querySelectorAll('.btn-toggle-set').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const exIdx = parseInt(e.target.getAttribute('data-ex-idx'));
      const setIdx = parseInt(e.target.getAttribute('data-set-idx'));
      const weightEl = container.querySelector(`.set-input-weight[data-ex-idx="${exIdx}"][data-set-idx="${setIdx}"]`);
      const repsEl = container.querySelector(`.set-input-reps[data-ex-idx="${exIdx}"][data-set-idx="${setIdx}"]`);

      workoutEngine.toggleSetComplete(exIdx, setIdx, weightEl.value, repsEl.value, 2);
      renderActiveWorkoutUI();
      checkActiveWorkoutBanner();
    });
  });

  container.querySelectorAll('.btn-add-set').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const exIdx = parseInt(e.target.getAttribute('data-ex-idx'));
      workoutEngine.addSetToExercise(exIdx);
      renderActiveWorkoutUI();
    });
  });
}

// --- History & Stats Tab ---
function initHistoryTab() {
  renderHistory();
}

function renderHistory() {
  const logs = store.getWorkoutLogs();
  const list = document.getElementById('history-list');

  document.getElementById('stat-total-workouts').textContent = logs.length;
  const totalSets = logs.reduce((acc, l) => acc + (l.completedSetsCount || 0), 0);
  document.getElementById('stat-weekly-volume').textContent = totalSets;

  if (logs.length === 0) {
    list.innerHTML = `<p class="muted-text text-sm">No completed workouts yet. Start a session from the Workout tab!</p>`;
    return;
  }

  list.innerHTML = logs.map(l => `
    <div class="card" style="margin-bottom: 10px;">
      <div style="display: flex; justify-content: space-between; align-items: flex-start;">
        <div>
          <h4>${l.routineName}</h4>
          <span class="text-sm muted-text">${new Date(l.completedAt).toLocaleDateString()} at ${new Date(l.completedAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</span>
        </div>
        <span class="badge badge-accent">${l.completedSetsCount} Sets | ${l.totalVolumeKg} kg</span>
      </div>
    </div>
  `).join('');
}

// --- Measurements Tab ---
function initMeasurementsTab() {
  const form = document.getElementById('form-measurement');
  form.addEventListener('submit', (e) => {
    e.preventDefault();
    const shoulders = document.getElementById('m-shoulders').value;
    const waist = document.getElementById('m-waist').value;
    const chest = document.getElementById('m-chest').value;
    const weight = document.getElementById('m-weight').value;

    store.saveBodyMeasurement({
      shouldersCm: shoulders,
      waistCm: waist,
      chestCm: chest,
      weightKg: weight
    });

    form.reset();
    document.getElementById('m-weight').value = "70.0";
    renderMeasurements();
  });

  renderMeasurements();
}

function renderMeasurements() {
  const list = store.getBodyMeasurements();
  const tbody = document.getElementById('measurement-history-body');

  if (list.length > 0) {
    const latest = list[0];
    document.getElementById('current-sw-ratio').textContent = latest.ratio.toFixed(2);
    const cat = MeasurementsTracker.getRatioCategory(latest.ratio);
    const badge = document.getElementById('ratio-category-badge');
    badge.textContent = cat.label;
    badge.className = `badge ${cat.color}`;
    document.getElementById('ratio-description').textContent = cat.text;
  }

  tbody.innerHTML = list.map(m => `
    <tr>
      <td>${m.date}</td>
      <td>${m.shouldersCm} cm</td>
      <td>${m.waistCm} cm</td>
      <td><strong>${m.ratio.toFixed(2)}</strong></td>
      <td>
        <button class="btn btn-xs btn-danger-outline btn-delete-measurement" data-id="${m.id}">✕</button>
      </td>
    </tr>
  `).join('');

  tbody.querySelectorAll('.btn-delete-measurement').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const id = e.target.getAttribute('data-id');
      store.deleteBodyMeasurement(id);
      renderMeasurements();
    });
  });
}

// --- Evidence Tab ---
function initEvidenceTab() {
  renderEvidencePanel(document.getElementById('evidence-panel-container'));
}

// --- Backup & Restore Modal ---
function initBackupRestore() {
  const modal = document.getElementById('backup-modal');
  document.getElementById('btn-backup').addEventListener('click', () => modal.classList.remove('hidden'));
  document.getElementById('btn-close-backup-modal').addEventListener('click', () => modal.classList.add('hidden'));

  document.getElementById('btn-export-json').addEventListener('click', () => {
    const jsonStr = store.exportDataJSON();
    const blob = new Blob([jsonStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `gymcoach_vtaper_backup_${new Date().toISOString().split('T')[0]}.json`;
    a.click();
    URL.revokeObjectURL(url);
  });

  document.getElementById('input-import-json').addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (event) => {
      const ok = store.importDataJSON(event.target.result);
      if (ok) {
        alert('Data imported successfully!');
        location.reload();
      } else {
        alert('Failed to parse backup JSON file.');
      }
    };
    reader.readAsText(file);
  });

  document.getElementById('btn-clear-data').addEventListener('click', () => {
    if (confirm('Are you sure you want to reset all local workout data and measurements?')) {
      store.clearAllData();
      location.reload();
    }
  });
}

// --- Global Event Listeners & Timers ---
function initEventListeners() {
  // Modal Close Buttons
  document.getElementById('btn-close-workout-modal').addEventListener('click', () => {
    document.getElementById('workout-modal').classList.add('hidden');
  });

  document.getElementById('btn-close-exercise-modal').addEventListener('click', () => {
    document.getElementById('exercise-detail-modal').classList.add('hidden');
  });

  // Finish Workout Button
  document.getElementById('btn-finish-workout').addEventListener('click', () => {
    if (confirm('Finish workout and save session log?')) {
      workoutEngine.finishWorkout();
      document.getElementById('workout-modal').classList.add('hidden');
      checkActiveWorkoutBanner();
      renderHistory();
    }
  });

  // Cancel Workout Button
  document.getElementById('btn-cancel-workout').addEventListener('click', () => {
    if (confirm('Discard active workout session?')) {
      workoutEngine.cancelWorkout();
      document.getElementById('workout-modal').classList.add('hidden');
      checkActiveWorkoutBanner();
    }
  });

  // Rest Timer Custom Events
  const restWidget = document.getElementById('rest-timer-widget');
  const restTimeEl = document.getElementById('rest-timer-time');
  const restProgressEl = document.getElementById('rest-timer-progress');

  window.addEventListener('gc_rest_timer_start', (e) => {
    restWidget.classList.remove('hidden');
    updateRestTimerUI(e.detail.remaining, e.detail.total);
  });

  window.addEventListener('gc_rest_timer_tick', (e) => {
    updateRestTimerUI(e.detail.remaining, e.detail.total);
  });

  window.addEventListener('gc_rest_timer_complete', () => {
    restWidget.classList.add('hidden');
  });

  function updateRestTimerUI(remaining, total) {
    const mins = Math.floor(remaining / 60);
    const secs = remaining % 60;
    restTimeEl.textContent = `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    const pct = Math.max(0, (remaining / total) * 100);
    restProgressEl.style.width = `${pct}%`;
  }

  document.querySelectorAll('#rest-timer-widget button[data-add-seconds]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const secs = parseInt(e.target.getAttribute('data-add-seconds'));
      workoutEngine.addRestTime(secs);
    });
  });

  document.getElementById('btn-skip-timer').addEventListener('click', () => {
    workoutEngine.stopRestTimer();
    restWidget.classList.add('hidden');
  });

  // Workout Clock Tick
  window.addEventListener('gc_workout_tick', (e) => {
    const totalSecs = e.detail.elapsedSeconds || 0;
    const mins = Math.floor(totalSecs / 60);
    const secs = totalSecs % 60;
    document.getElementById('workout-timer-clock').textContent =
      `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  });
}
