/**
 * GymCoach Evidence Base & Literature Citations (evidence.js)
 * Transparently separates Scientific Evidence, Expert Practice, Community Opinion, and Inferences.
 */

export const EvidenceBase = [
  {
    tier: "scientific",
    tag: "TIER 1: SCIENTIFIC EVIDENCE (Peer-Reviewed Literature & Meta-Analyses)",
    cssClass: "tier-science",
    title: "Latissimus Dorsi & Lateral Delt Hypertrophy Mechanisms",
    citations: [
      "Morton RW et al. (2018). 'A systematic review, meta-analysis and meta-regression of the effect of protein supplementation on resistance training-induced gains in muscle mass and strength in healthy adults.' British Journal of Sports Medicine, 52(6):376-384. [Finding: 1.6-2.2 g/kg/day total protein optimizes hypertrophy].",
      "Schoenfeld BJ et al. (2017). 'Dose-response relationship between weekly resistance training volume and increases in muscle mass: A systematic review and meta-analysis.' Journal of Sports Sciences, 35(11):1073-1082. [Finding: 10+ sets per muscle group weekly yields superior hypertrophy vs <10 sets].",
      "Escamilla RF et al. (2009). 'An electromyographic analysis of shoulder muscle activation during shoulder pressing and lateral raise exercises.' Journal of Applied Biomechanics. [Finding: Dumbbell Lateral Raises produce isolated peak EMG activation of the lateral head of deltoid].",
      "Helms ER et al. (2016). 'Application of the Rating of Perceived Exertion Scale Based on Repetitions in Reserve in Resistance Training.' Strategic Strength & Conditioning Journal. [Finding: RIR 1-3 produces hyper-trophic equivalence to absolute failure with reduced recovery cost]."
    ]
  },
  {
    tier: "practice",
    tag: "TIER 2: EXPERT PRACTICE (Elite Coaching Consensus)",
    cssClass: "tier-practice",
    title: "Exercise Selection & Incline/Angle Adjustments for Equipment Constraints",
    citations: [
      "Chest-Supported / Bent-Over DB Rows eliminate lower back spinal shear when heavy barbells are absent, allowing maximum lat drive (Helms, Israetel).",
      "Incline DB Press set to 30 degrees isolates the clavicular head of pectoralis major without over-recruiting anterior deltoid.",
      "Incline Lean-Away Lateral Raises maintain constant tension at the bottom stretch of the side delt, which standard standing raises lack."
    ]
  },
  {
    tier: "community",
    tag: "TIER 3: COMMUNITY OPINION & PRACTICAL HYPOTHESES",
    cssClass: "tier-community",
    title: "Home Gym & Bodyweight Equipment Adaptations",
    citations: [
      "Inverted Rows executed under a heavy table or doorframe broomstick setup offer a highly accessible bodyweight horizontal pull equivalent to barbell rows.",
      "Decline / Feet-Elevated Push-ups provide high upper-chest stimulus without requiring heavy bench press equipment."
    ]
  },
  {
    tier: "inference",
    tag: "TIER 4: OUR ALGORITHMIC INFERENCES & MODEL EXTRAPOLATIONS",
    cssClass: "tier-inference",
    title: "V-Taper Mathematical Ratio & Progression Scaling",
    citations: [
      "Shoulder-to-Waist Ratio (S/W = Shoulder Circumference / Waist Circumference) serves as a quantitative progress metric representing upper torso breadth.",
      "Rest timer defaults (60s for isolation raises, 90s for DB compound rows/presses) balance intra-set ATP-PC recovery with time-efficient workout duration."
    ]
  }
];

export function renderEvidencePanel(containerEl) {
  if (!containerEl) return;
  containerEl.innerHTML = EvidenceBase.map(item => `
    <div class="card evidence-tier-card">
      <span class="tier-tag ${item.cssClass}">${item.tag}</span>
      <h4>${item.title}</h4>
      <ul style="padding-left: 18px; margin-top: 8px; font-size: 0.85rem; color: var(--text-secondary);">
        ${item.citations.map(c => `<li style="margin-bottom: 6px;">${c}</li>`).join('')}
      </ul>
    </div>
  `).join('');
}
