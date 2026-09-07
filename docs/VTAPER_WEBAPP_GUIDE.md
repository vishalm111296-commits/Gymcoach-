# GymCoach — V-Taper Dumbbell & Bodyweight Companion Web Application

## Overview

The **GymCoach V-Taper Companion Web Application** (`web/`) is an offline-ready, mobile-first web app specifically designed for users focused on developing a broader-looking V-taper physique using **dumbbells and bodyweight only**.

It leverages the repository's native exercise assets located under `app/src/main/assets/exercises/` with zero data duplication, and provides a streamlined live workout logging experience, V-taper Golden Ratio tracking, readiness-based program adjustments, and evidence-based nutrition recommendations.

---

## Targeted User Profile

- **Age**: 30 years old
- **Weight**: 70 kg
- **Height**: 170 cm
- **Equipment**: Dumbbells + Bodyweight (+ Flat/Incline Bench)
- **Goal**: Maximize visual V-taper physique (Golden Shoulder-to-Waist Ratio ~1.618)

---

## Evidence-Based V-Taper Principles

### 1. Latissimus Dorsi (Lat Width)
- **Primary Driver**: Visual upper-body width.
- **Dumbbell / Bodyweight Exercise**: Single-Arm Dumbbell Rows (pulling to hip) & Bodyweight Pull-ups / Inverted Rows.
- **Evidence**: Meta-analyses show full stretch under load and pulling along the lat muscle fibers maximally stimulate hypertrophy.

### 2. Lateral Deltoid (3D Shoulder Cap)
- **Primary Driver**: Shoulder width extension beyond the torso frame.
- **Dumbbell Exercise**: High-frequency (2–3x/week) Dumbbell Lateral Raises.
- **Evidence**: Lateral deltoid fibers are most responsive to moderate-to-high rep ranges (10–15 reps) at RPE 7.5–9 with controlled eccentric negatives.

### 3. Upper Back & Posture (Scapular Retraction)
- **Primary Driver**: Erect posture, chest expansion, and upper back thickness.
- **Dumbbell Exercise**: Bent-Over Rear Delt Flyes & Dumbbell Incline Rows.

### 4. Upper Chest (Incline Shelf)
- **Primary Driver**: Fills out the upper torso beneath the collarbone.
- **Dumbbell Exercise**: Incline Dumbbell Press (30° Incline).

### 5. Waist Management & Deep Core Taper
- **Primary Driver**: Keeping the waist tight without hypertrophy of lateral obliques.
- **Bodyweight Exercise**: Hanging Leg Raises, Planks, Stomach Vacuums.

---

## Web Application Architecture

The web app is structured as a self-contained static PWA requiring no server pipeline or build step:

```
web/
├── index.html   # Mobile-first app shell & tab navigation
├── styles.css   # Dark theme CSS variables & responsive layout
└── app.js       # Exercise loader, V-Taper program generator, workout logger & nutrition calculator
```

### Key Features
1. **Dynamic Exercise Loader**: Parses asset JSON files directly from `app/src/main/assets/exercises/` with strict equipment filtering (`dumbbell`, `bodyweight`, `bench`).
2. **V-Taper Program Split Generator**: Automatically generates 3-Day, 4-Day, or 5-Day splits hyper-targeting V-taper muscle groups.
3. **Daily Readiness Scoring**: Scales workout set volume (2..4 sets) and target RPE (7.0..8.0) based on user readiness (1–5).
4. **Live Workout Logger**: Logs sets, reps, weight, and calculates estimated 1RM (e1RM via Epley formula) in real-time.
5. **Audio/Visual Rest Timer**: Built-in 90-second rest timer with +30s extension and Web Audio API audio cue.
6. **V-Taper Body Measurement Tracker**: Logs Shoulder-to-Waist ratio and compares against the Adonis Golden Ratio (1.618).
7. **Nutrition Calculator**: Calculates Mifflin-St Jeor BMR/TDEE (2,250 kcal maintenance) and protein targets (154g/day) for Recomp, Surplus (+250 kcal), and Deficit (-350 kcal) modes.

---

## GitHub Pages Deployment Instructions

To host this companion web application on GitHub Pages:

1. Go to your repository settings on GitHub: `https://github.com/vishalm111296-commits/Gymcoach-/settings/pages`
2. Under **Build and deployment**:
   - **Source**: Select `Deploy from a branch`
   - **Branch**: Select `main` (or your feature branch)
   - **Folder**: Select `/web` (or `/ (root)` if serving via root redirect)
3. Click **Save**.
4. Access the web app at `https://vishalm111296-commits.github.io/Gymcoach-/web/`.
