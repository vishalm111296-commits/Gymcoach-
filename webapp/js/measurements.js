/**
 * GymCoach V-Taper Measurement Tracker (measurements.js)
 * Calculates Shoulder-to-Waist ratio and categorizes physical V-taper frame development.
 */

export const MeasurementsTracker = {
  calculateRatio(shouldersCm, waistCm) {
    const s = parseFloat(shouldersCm);
    const w = parseFloat(waistCm);
    if (!s || !w || w <= 0) return 0;
    return parseFloat((s / w).toFixed(3));
  },

  getRatioCategory(ratio) {
    if (ratio >= 1.55) {
      return { label: "Elite V-Taper Frame", color: "badge-accent", text: "Exceptional shoulder breadth relative to waist. Ideal Golden Ratio range." };
    } else if (ratio >= 1.45) {
      return { label: "Strong V-Shape", color: "badge-accent", text: "Well-developed lateral delts and lat width create a distinct athletic V taper." };
    } else if (ratio >= 1.35) {
      return { label: "Moderate V-Shape", color: "badge-primary", text: "Good foundation. Continued lat and side delt focus will further widen upper frame." };
    } else {
      return { label: "Baseline Frame", color: "badge-primary", text: "Initial baseline. Focus on upper back/lat hypertrophy and maintaining a lean waist." };
    }
  }
};
