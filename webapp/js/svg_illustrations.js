/**
 * GymCoach SVG Motion Illustration Library (svg_illustrations.js)
 * Generates lightweight, looping vector motion graphics demonstrating proper movement paths.
 */

export const SVGIllustrations = {
  getIllustration(exerciseId, category = 'general') {
    const key = (exerciseId || '').toLowerCase();

    if (key.includes('lateral') || key.includes('upright_row') || key.includes('y_raise')) {
      return this._lateralRaiseSVG();
    }
    if (key.includes('row') || key.includes('pullover') || key.includes('lat')) {
      return this._dumbbellRowSVG();
    }
    if (key.includes('push_up') || key.includes('pushup') || key.includes('dip')) {
      return this._pushUpSVG();
    }
    if (key.includes('press') || key.includes('fly')) {
      return this._pressSVG();
    }
    if (key.includes('curl')) {
      return this._bicepCurlSVG();
    }
    if (key.includes('tricep') || key.includes('extension')) {
      return this._tricepExtensionSVG();
    }
    if (key.includes('squat') || key.includes('lunge') || key.includes('rdl') || key.includes('deadlift')) {
      return this._lowerBodySVG();
    }

    return this._genericMotionSVG();
  },

  _lateralRaiseSVG() {
    return `
      <svg viewBox="0 0 200 160" width="100%" height="100%" xmlns="http://www.w3.org/2000/svg">
        <style>
          .arm-left { animation: raiseLeft 2.5s ease-in-out infinite alternate; transform-origin: 85px 55px; }
          .arm-right { animation: raiseRight 2.5s ease-in-out infinite alternate; transform-origin: 115px 55px; }
          @keyframes raiseLeft { 0% { transform: rotate(0deg); } 100% { transform: rotate(70deg); } }
          @keyframes raiseRight { 0% { transform: rotate(0deg); } 100% { transform: rotate(-70deg); } }
          .highlight { stroke: #38bdf8; stroke-width: 4; fill: none; }
          .motion-path { stroke: #38bdf8; stroke-dasharray: 3,3; fill: none; opacity: 0.6; }
        </style>

        <!-- Head & Torso -->
        <circle cx="100" cy="30" r="12" fill="#94a3b8" />
        <path d="M 85 55 L 115 55 L 110 110 L 90 110 Z" fill="#64748b" />
        <!-- V-Taper Highlight (Lats & Delts) -->
        <path d="M 85 55 L 115 55 L 100 95 Z" fill="rgba(56, 189, 248, 0.2)" />

        <!-- Left Arm & Dumbbell -->
        <g class="arm-left">
          <line x1="85" y1="55" x2="85" y2="100" stroke="#f8fafc" stroke-width="8" stroke-linecap="round" />
          <rect x="77" y="96" width="16" height="10" rx="3" fill="#38bdf8" />
        </g>

        <!-- Right Arm & Dumbbell -->
        <g class="arm-right">
          <line x1="115" y1="55" x2="115" y2="100" stroke="#f8fafc" stroke-width="8" stroke-linecap="round" />
          <rect x="107" y="96" width="16" height="10" rx="3" fill="#38bdf8" />
        </g>

        <!-- Motion Arcs -->
        <path d="M 85 100 A 45 45 0 0 1 45 60" class="motion-path" />
        <path d="M 115 100 A 45 45 0 0 0 155 60" class="motion-path" />
        <text x="100" y="145" text-anchor="middle" fill="#38bdf8" font-size="11" font-weight="bold">Lateral Delt Abduction Arc</text>
      </svg>
    `;
  },

  _dumbbellRowSVG() {
    return `
      <svg viewBox="0 0 200 160" width="100%" height="100%" xmlns="http://www.w3.org/2000/svg">
        <style>
          .row-arm { animation: pullRow 2.2s ease-in-out infinite alternate; }
          @keyframes pullRow {
            0% { transform: translate(0, 20px); }
            100% { transform: translate(-15px, -15px); }
          }
          .lat-muscle { fill: rgba(16, 185, 129, 0.3); stroke: #10b981; stroke-width: 1.5; }
        </style>

        <!-- Bench -->
        <rect x="30" y="100" width="140" height="8" rx="2" fill="#475569" />

        <!-- Torso Bent Over -->
        <path d="M 60 70 L 130 70" stroke="#94a3b8" stroke-width="14" stroke-linecap="round" />
        <circle cx="138" cy="65" r="10" fill="#94a3b8" />

        <!-- Target Lat Muscle Highlight -->
        <path d="M 80 65 Q 105 75 115 65 Z" class="lat-muscle" />

        <!-- Pulling Arm & Weight -->
        <g class="row-arm">
          <path d="M 100 70 L 100 115" stroke="#f8fafc" stroke-width="8" stroke-linecap="round" />
          <rect x="92" y="110" width="16" height="12" rx="3" fill="#10b981" />
        </g>

        <text x="100" y="145" text-anchor="middle" fill="#10b981" font-size="11" font-weight="bold">Lat Pull Trajectory (Elbow to Hip)</text>
      </svg>
    `;
  },

  _pushUpSVG() {
    return `
      <svg viewBox="0 0 200 160" width="100%" height="100%" xmlns="http://www.w3.org/2000/svg">
        <style>
          .pushup-body { animation: pushMotion 2.2s ease-in-out infinite alternate; transform-origin: 160px 115px; }
          @keyframes pushMotion {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(18deg); }
          }
        </style>

        <!-- Ground Line -->
        <line x1="20" y1="120" x2="180" y2="120" stroke="#475569" stroke-width="3" />

        <g class="pushup-body">
          <!-- Body Plank -->
          <line x1="40" y1="70" x2="160" y2="115" stroke="#f8fafc" stroke-width="12" stroke-linecap="round" />
          <circle cx="30" cy="65" r="10" fill="#94a3b8" />
          <!-- Upper Chest Highlight -->
          <circle cx="55" cy="75" r="8" fill="rgba(56, 189, 248, 0.4)" />
          <!-- Arms -->
          <path d="M 50 75 L 50 120" stroke="#38bdf8" stroke-width="6" stroke-linecap="round" />
        </g>

        <text x="100" y="145" text-anchor="middle" fill="#38bdf8" font-size="11" font-weight="bold">Core Braced Body Line Push</text>
      </svg>
    `;
  },

  _pressSVG() {
    return `
      <svg viewBox="0 0 200 160" width="100%" height="100%" xmlns="http://www.w3.org/2000/svg">
        <style>
          .press-arm-l { animation: pressL 2s ease-in-out infinite alternate; }
          .press-arm-r { animation: pressR 2s ease-in-out infinite alternate; }
          @keyframes pressL { 0% { transform: translateY(20px); } 100% { transform: translateY(-10px); } }
          @keyframes pressR { 0% { transform: translateY(20px); } 100% { transform: translateY(-10px); } }
        </style>

        <!-- Torso -->
        <circle cx="100" cy="75" r="12" fill="#94a3b8" />
        <rect x="85" y="87" width="30" height="40" rx="6" fill="#64748b" />

        <!-- Upper Chest Clavicular Highlight -->
        <path d="M 85 87 Q 100 95 115 87 Q 100 80 85 87 Z" fill="#38bdf8" />

        <!-- Pressing Left Arm -->
        <g class="press-arm-l">
          <line x1="80" y1="92" x2="60" y2="92" stroke="#f8fafc" stroke-width="7" stroke-linecap="round" />
          <rect x="52" y="85" width="14" height="14" rx="3" fill="#38bdf8" />
        </g>

        <!-- Pressing Right Arm -->
        <g class="press-arm-r">
          <line x1="120" y1="92" x2="140" y2="92" stroke="#f8fafc" stroke-width="7" stroke-linecap="round" />
          <rect x="134" y="85" width="14" height="14" rx="3" fill="#38bdf8" />
        </g>

        <text x="100" y="145" text-anchor="middle" fill="#38bdf8" font-size="11" font-weight="bold">Upper Chest Clavicular Convergence</text>
      </svg>
    `;
  },

  _bicepCurlSVG() {
    return `
      <svg viewBox="0 0 200 160" width="100%" height="100%" xmlns="http://www.w3.org/2000/svg">
        <style>
          .curl-forearm { animation: curlMotion 2s ease-in-out infinite alternate; transform-origin: 100px 85px; }
          @keyframes curlMotion { 0% { transform: rotate(0deg); } 100% { transform: rotate(-110deg); } }
        </style>

        <circle cx="100" cy="35" r="10" fill="#94a3b8" />
        <line x1="100" y1="45" x2="100" y2="85" stroke="#64748b" stroke-width="12" stroke-linecap="round" />

        <g class="curl-forearm">
          <line x1="100" y1="85" x2="100" y2="125" stroke="#f8fafc" stroke-width="7" stroke-linecap="round" />
          <circle cx="100" cy="128" r="8" fill="#10b981" />
        </g>

        <text x="100" y="145" text-anchor="middle" fill="#10b981" font-size="11" font-weight="bold">Controlled Eccentric Bicep Curl</text>
      </svg>
    `;
  },

  _tricepExtensionSVG() {
    return `
      <svg viewBox="0 0 200 160" width="100%" height="100%" xmlns="http://www.w3.org/2000/svg">
        <style>
          .tri-forearm { animation: triMotion 2s ease-in-out infinite alternate; transform-origin: 100px 65px; }
          @keyframes triMotion { 0% { transform: rotate(-120deg); } 100% { transform: rotate(0deg); } }
        </style>

        <circle cx="100" cy="30" r="10" fill="#94a3b8" />
        <!-- Upper arm fixed upwards -->
        <line x1="100" y1="40" x2="100" y2="65" stroke="#38bdf8" stroke-width="8" stroke-linecap="round" />

        <g class="tri-forearm">
          <line x1="100" y1="65" x2="100" y2="105" stroke="#f8fafc" stroke-width="7" stroke-linecap="round" />
          <rect x="93" y="100" width="14" height="12" rx="3" fill="#38bdf8" />
        </g>

        <text x="100" y="145" text-anchor="middle" fill="#38bdf8" font-size="11" font-weight="bold">Overhead Long-Head Extension</text>
      </svg>
    `;
  },

  _lowerBodySVG() {
    return `
      <svg viewBox="0 0 200 160" width="100%" height="100%" xmlns="http://www.w3.org/2000/svg">
        <style>
          .squat-legs { animation: squatMotion 2.2s ease-in-out infinite alternate; transform-origin: 100px 60px; }
          @keyframes squatMotion { 0% { transform: scaleY(1); } 100% { transform: scaleY(0.75); } }
        </style>

        <g class="squat-legs">
          <circle cx="100" cy="30" r="10" fill="#94a3b8" />
          <line x1="100" y1="40" x2="100" y2="80" stroke="#64748b" stroke-width="12" stroke-linecap="round" />
          <path d="M 100 80 L 80 125 M 100 80 L 120 125" stroke="#f8fafc" stroke-width="8" stroke-linecap="round" />
        </g>

        <text x="100" y="145" text-anchor="middle" fill="#f8fafc" font-size="11" font-weight="bold">Controlled Knee & Hip Hinge</text>
      </svg>
    `;
  },

  _genericMotionSVG() {
    return `
      <svg viewBox="0 0 200 160" width="100%" height="100%" xmlns="http://www.w3.org/2000/svg">
        <circle cx="100" cy="50" r="16" fill="#38bdf8" opacity="0.3" />
        <circle cx="100" cy="50" r="10" fill="#38bdf8" />
        <path d="M 85 75 L 115 75 L 105 115 L 95 115 Z" fill="#64748b" />
        <text x="100" y="145" text-anchor="middle" fill="#94a3b8" font-size="11">GymCoach Anatomic Visual</text>
      </svg>
    `;
  }
};
