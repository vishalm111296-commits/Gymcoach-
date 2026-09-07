/**
 * GymCoach Progression & Autoregulation Engine (progression.js)
 * Implements double progression and RIR-based autoregulation rules with scientific literature citations.
 */

export const ProgressionEngine = {
  /**
   * Evaluates exercise history and suggests next workout targets.
   * @param {Object} exerciseLog - Completed exercise object with sets
   * @param {string} repRangeStr - Target rep range (e.g. "8-12")
   * @returns {Object} Recommendation object with loadAdjustment, repAdjustment, and rationale
   */
  evaluateProgression(exerciseLog, repRangeStr = "8-12") {
    if (!exerciseLog || !exerciseLog.sets || exerciseLog.sets.length === 0) {
      return {
        recommendation: "Establish baseline weight",
        action: "MAINTAIN",
        rationale: "No previous set performance found. Select a weight allowing target reps with 2 RIR."
      };
    }

    const completedSets = exerciseLog.sets.filter(s => s.completed);
    if (completedSets.length === 0) {
      return {
        recommendation: "Complete target sets",
        action: "MAINTAIN",
        rationale: "Finish all planned sets to evaluate progression."
      };
    }

    const rangeParts = repRangeStr.split('-').map(p => parseInt(p.trim()));
    const minReps = rangeParts[0] || 8;
    const maxReps = rangeParts[1] || 12;

    const allHitMax = completedSets.every(s => s.reps >= maxReps);
    const anyBelowMin = completedSets.some(s => s.reps < minReps);
    const averageRir = completedSets.reduce((acc, s) => acc + (s.rir !== undefined ? s.rir : 2), 0) / completedSets.length;

    // Rule 1: Double Progression - Increase Weight
    if (allHitMax && averageRir >= 1.0) {
      return {
        recommendation: "+1.0 to +2.0 kg",
        action: "INCREASE_WEIGHT",
        badge: "Progression Ready 🚀",
        rationale: `Achieved top of rep range (${maxReps} reps) across all sets with ~${averageRir.toFixed(1)} RIR. Increase load next session (Schoenfeld et al. 2021).`
      };
    }

    // Rule 2: Regress or Deload if undershooting
    if (anyBelowMin || averageRir === 0) {
      return {
        recommendation: "Maintain load, focus on form & control",
        action: "MAINTAIN_OR_LIGHTEN",
        badge: "Consolidate Load 🎯",
        rationale: "Fell below target minimum reps or hit 0 RIR prematurely. Maintain current weight until all sets fall cleanly inside target rep range."
      };
    }

    // Rule 3: Add 1 Rep
    return {
      recommendation: "+1 Rep on Set 1",
      action: "ADD_REP",
      badge: "Add 1 Rep 📈",
      rationale: `Currently performing well inside ${repRangeStr} range. Add 1 rep on set 1 next session before raising load (Helms et al. 2016).`
    };
  }
};
