package com.gymcoach.app.core.ml

import kotlin.math.acos
import kotlin.math.sqrt
import kotlin.math.toDegrees

// ── Types ────────────────────────────────────────────────

data class NormalizedLandmark(val x: Float, val y: Float, val z: Float)

data class AnalysisResult(
    val repCount: Int,
    val formFeedback: String,
    val angle: Double,
    val currentPhase: RepPhase,
    val confidence: Double
)

enum class RepPhase { UP, DOWN, HOLD }

data class Pose(
    /** All 33 MediaPipe landmarks, normalized 0..1. Index 0 = nose. */
    val landmarks: List<NormalizedLandmark>,
    /** Per-landmark visibility scores from MediaPipe (0..1). Empty if unavailable. */
    val visibility: List<Float> = emptyList()
)

enum class ExerciseType {
    BICEP_CURL, SQUAT, PUSH_UP, SHOULDER_PRESS, LATERAL_RAISE,
    BENT_OVER_ROW, PLANK, DEADLIFT, BENCH_PRESS
}

// ── Per-exercise configuration ───────────────────────────

data class ExerciseConfig(
    val downThreshold: Double,
    val upThreshold: Double,
    val minConfidence: Double = 0.5,
    val validAngleRange: ClosedRange<Double> = 0.0..180.0,
    val isTimeBased: Boolean = false,
    val holdDurationMs: Long = 0
)

// ── Reusable state machine ───────────────────────────────

enum class MovementState { VALID, INVALID, RECOVERING }

data class MovementValidation(
    val state: MovementState,
    val feedback: String
)

// ── Analyzer ─────────────────────────────────────────────

class FormAnalyzer(
    private val exerciseType: ExerciseType,
    private val config: ExerciseConfig = ExerciseConfig(
        downThreshold = 0.0,
        upThreshold = 1.0,
        minConfidence = 0.5,
        validAngleRange = 0.0..180.0,
        isTimeBased = false,
        holdDurationMs = 0
    )
) {
    private var repCount = 0
    private var lastPhase: RepPhase? = null
    private var lastAngle: Double = 0.0
    private var lastValidAngle: Double = 0.0
    private val history = ArrayDeque<Double>(5)

    // Plank time tracking
    private var plankHoldStartMs: Long = 0L
    private var plankHoldCompleted: Boolean = false

    // Invalid movement tracking
    private var consecutiveInvalidCount = 0
    private val maxConsecutiveInvalidBeforeReset = 10

    // Confidence tracking
    private var lowConfidenceFrames = 0
    private val lowConfidenceThreshold = 3

    companion object {
        // MediaPipe landmark indices
        // shoulder=11, elbow=13, wrist=15  (left)  |  12, 14, 16 (right)
        // hip=23, knee=25, ankle=27 (left)  |  24, 26, 28 (right)

        fun defaultFor(type: ExerciseType): ExerciseConfig = when (type) {
            ExerciseType.BICEP_CURL -> ExerciseConfig(
                downThreshold = 150.0, upThreshold = 50.0, minConfidence = 0.5
            )
            ExerciseType.SQUAT -> ExerciseConfig(
                downThreshold = 100.0, upThreshold = 160.0, minConfidence = 0.5
            )
            ExerciseType.PUSH_UP -> ExerciseConfig(
                downThreshold = 90.0, upThreshold = 160.0, minConfidence = 0.5
            )
            ExerciseType.SHOULDER_PRESS -> ExerciseConfig(
                downThreshold = 80.0, upThreshold = 170.0, minConfidence = 0.5
            )
            ExerciseType.LATERAL_RAISE -> ExerciseConfig(
                downThreshold = 80.0, upThreshold = 160.0, minConfidence = 0.5
            )
            ExerciseType.BENT_OVER_ROW -> ExerciseConfig(
                downThreshold = 90.0, upThreshold = 160.0, minConfidence = 0.5
            )
            ExerciseType.PLANK -> ExerciseConfig(
                downThreshold = 170.0, upThreshold = 170.0,
                minConfidence = 0.4, isTimeBased = true, holdDurationMs = 30000
            )
            ExerciseType.DEADLIFT -> ExerciseConfig(
                downThreshold = 100.0, upThreshold = 160.0, minConfidence = 0.5
            )
            ExerciseType.BENCH_PRESS -> ExerciseConfig(
                downThreshold = 80.0, upThreshold = 160.0, minConfidence = 0.5
            )
        }
    }

    // ── Core math ────────────────────────────────────────

    private fun angle(a: NormalizedLandmark, b: NormalizedLandmark, c: NormalizedLandmark): Double {
        val ab = doubleArrayOf((a.x - b.x).toDouble(), (a.y - b.y).toDouble())
        val cb = doubleArrayOf((c.x - b.x).toDouble(), (c.y - b.y).toDouble())
        val dot = ab[0] * cb[0] + ab[1] * cb[1]
        val magAB = sqrt(ab[0] * ab[0] + ab[1] * ab[1])
        val magCB = sqrt(cb[0] * cb[0] + cb[1] * cb[1])
        if (magAB < 1e-6 || magCB < 1e-6) return -1.0
        return kotlin.math.toDegrees(acos((dot / (magAB * magCB)).coerceIn(-1.0, 1.0)))
    }

    private fun safeLandmark(pose: Pose, index: Int): NormalizedLandmark {
        return pose.landmarks.getOrElse(index) { NormalizedLandmark(0f, 0f, 0f) }
    }

    private fun landmarkVisibility(pose: Pose, index: Int): Float {
        return pose.visibility.getOrElse(index) { 1.0f }
    }

    private fun allLandmarksVisible(pose: Pose, indices: List<Int>): Boolean {
        return indices.all { landmarkVisibility(pose, it) >= config.minConfidence }
    }

    private fun averageConfidence(pose: Pose, indices: List<Int>): Double {
        if (pose.visibility.isEmpty()) return 1.0
        val confidences = indices.map { landmarkVisibility(pose, it).toDouble() }
        return confidences.average()
    }

    // ── Angle helpers per exercise ───────────────────────

    private fun elbowAngle(pose: Pose): Double {
        val shoulder = safeLandmark(pose, 12)
        val elbow    = safeLandmark(pose, 14)
        val wrist    = safeLandmark(pose, 16)
        return angle(shoulder, elbow, wrist)
    }

    private fun hipKneeAnkleAngle(pose: Pose): Double {
        val hip    = safeLandmark(pose, 24)
        val knee   = safeLandmark(pose, 26)
        val ankle  = safeLandmark(pose, 28)
        return angle(hip, knee, ankle)
    }

    private fun pushUpAngle(pose: Pose): Double = elbowAngle(pose)

    private fun shoulderPressElbowAngle(pose: Pose): Double = elbowAngle(pose)

    private fun lateralRaiseElbowAngle(pose: Pose): Double = elbowAngle(pose)

    private fun bentOverRowElbowAngle(pose: Pose): Double = elbowAngle(pose)

    private fun benchPressElbowAngle(pose: Pose): Double = elbowAngle(pose)

    private fun hipTrunkAngle(pose: Pose): Double {
        val shoulder = safeLandmark(pose, 12)
        val hip      = safeLandmark(pose, 24)
        val knee     = safeLandmark(pose, 26)
        return angle(shoulder, hip, knee)
    }

    private fun plankBodyAngle(pose: Pose): Double {
        val shoulder = safeLandmark(pose, 12)
        val hip      = safeLandmark(pose, 24)
        val ankle    = safeLandmark(pose, 28)
        return angle(shoulder, hip, ankle)
    }

    private fun deadliftHipKneeAnkle(pose: Pose): Double {
        val hip    = safeLandmark(pose, 24)
        val knee   = safeLandmark(pose, 26)
        val ankle  = safeLandmark(pose, 28)
        return angle(hip, knee, ankle)
    }

    private fun deadliftShoulderHip(pose: Pose): Double {
        val shoulder = safeLandmark(pose, 12)
        val hip      = safeLandmark(pose, 24)
        val knee     = safeLandmark(pose, 26)
        return angle(shoulder, hip, knee)
    }

    // ── Movement validation ──────────────────────────────

    private fun validateMovement(angle: Double): MovementValidation {
        val inRange = angle in config.validAngleRange
        return when {
            angle < 0 -> MovementValidation(MovementState.INVALID, "Landmarks not detected")
            !inRange -> MovementValidation(MovementState.INVALID, "Angle out of expected range")
            else -> MovementValidation(MovementState.VALID, "")
        }
    }

    // ── Feedback per exercise ────────────────────────────

    private fun getFeedback(angle: Double, type: ExerciseType): String = when (type) {
        ExerciseType.BICEP_CURL -> when {
            angle > 170 -> "Extend arm more"
            angle < 30  -> "Full range of motion"
            else        -> "Good rep"
        }
        ExerciseType.SQUAT -> when {
            angle < 90  -> "Good depth"
            angle < 120 -> "Go lower"
            else        -> "Start squat"
        }
        ExerciseType.PUSH_UP -> when {
            angle < 90  -> "Good depth"
            angle < 150 -> "Lower your chest"
            else        -> "Keep body straight"
        }
        ExerciseType.SHOULDER_PRESS -> when {
            angle < 90  -> "Press weight up"
            angle < 150 -> "Almost there"
            else        -> "Start press"
        }
        ExerciseType.LATERAL_RAISE -> when {
            angle < 90  -> "Raise arms to shoulder height"
            angle < 150 -> "Lower with control"
            else        -> "Start lateral raise"
        }
        ExerciseType.BENT_OVER_ROW -> when {
            angle < 90  -> "Pull to your torso"
            angle < 150 -> "Extend arms forward"
            else        -> "Start bent-over row"
        }
        ExerciseType.PLANK -> when {
            angle > 175 -> "Good plank position"
            angle > 160 -> "Squeeze core, keep straight"
            else        -> "Lower hips or raise up"
        }
        ExerciseType.DEADLIFT -> when {
            angle < 90  -> "Good hinge position"
            angle < 130 -> "Push hips back"
            else        -> "Start deadlift"
        }
        ExerciseType.BENCH_PRESS -> when {
            angle < 90  -> "Lower the bar"
            angle < 150 -> "Press up"
            else        -> "Start bench press"
        }
    }

    // ── Angle dispatcher ─────────────────────────────────

    private fun getAngle(pose: Pose): Double = when (exerciseType) {
        ExerciseType.BICEP_CURL     -> elbowAngle(pose)
        ExerciseType.SQUAT          -> hipKneeAnkleAngle(pose)
        ExerciseType.PUSH_UP        -> pushUpAngle(pose)
        ExerciseType.SHOULDER_PRESS -> shoulderPressElbowAngle(pose)
        ExerciseType.LATERAL_RAISE  -> lateralRaiseElbowAngle(pose)
        ExerciseType.BENT_OVER_ROW  -> bentOverRowElbowAngle(pose)
        ExerciseType.PLANK          -> plankBodyAngle(pose)
        ExerciseType.DEADLIFT       -> deadliftHipKneeAnkle(pose)
        ExerciseType.BENCH_PRESS    -> benchPressElbowAngle(pose)
    }

    // ── Plank time-based logic ───────────────────────────

    private fun updatePlankState(angle: Double, currentTimeMs: Long): Pair<Int, String> {
        val inPosition = angle > config.downThreshold
        if (inPosition) {
            if (plankHoldStartMs == 0L) {
                plankHoldStartMs = currentTimeMs
            }
            val elapsed = currentTimeMs - plankHoldStartMs
            if (elapsed >= config.holdDurationMs && !plankHoldCompleted) {
                plankHoldCompleted = true
                plankHoldStartMs = 0L
                return Pair(repCount + 1, "Plank hold complete")
            }
            val secondsRemaining = (config.holdDurationMs - elapsed) / 1000
            return Pair(repCount, "Hold for ${secondsRemaining}s")
        } else {
            plankHoldStartMs = 0L
            return Pair(repCount, "Get into plank position")
        }
    }

    // ── Public API ───────────────────────────────────────

    fun analyze(pose: Pose, currentTimeMs: Long = 0L): AnalysisResult? = synchronized(this) {
        // Landmark visibility check
        val requiredLandmarks = when (exerciseType) {
            ExerciseType.BICEP_CURL, ExerciseType.SHOULDER_PRESS,
            ExerciseType.LATERAL_RAISE, ExerciseType.BENT_OVER_ROW,
            ExerciseType.PUSH_UP, ExerciseType.BENCH_PRESS -> listOf(12, 14, 16)
            ExerciseType.SQUAT, ExerciseType.DEADLIFT -> listOf(24, 26, 28)
            ExerciseType.PLANK -> listOf(12, 24, 28)
        }

        if (!allLandmarksVisible(pose, requiredLandmarks)) {
            lowConfidenceFrames++
            if (lowConfidenceFrames >= lowConfidenceThreshold) {
                reset()
                lowConfidenceFrames = 0
            }
            return null
        }
        lowConfidenceFrames = 0

        val rawAngle = getAngle(pose)

        // Invalid movement detection
        val validation = validateMovement(rawAngle)
        if (validation.state == MovementState.INVALID) {
            consecutiveInvalidCount++
            if (consecutiveInvalidCount >= maxConsecutiveInvalidBeforeReset) {
                reset()
                consecutiveInvalidCount = 0
            }
            return AnalysisResult(
                repCount = repCount,
                formFeedback = validation.feedback,
                angle = rawAngle,
                currentPhase = lastPhase ?: RepPhase.UP,
                confidence = averageConfidence(pose, requiredLandmarks)
            )
        }
        consecutiveInvalidCount = 0
        lastValidAngle = rawAngle

        // Confidence score
        val confidence = averageConfidence(pose, requiredLandmarks)

        // Plank time-based handling
        if (config.isTimeBased && exerciseType == ExerciseType.PLANK) {
            val (count, feedback) = updatePlankState(rawAngle, currentTimeMs)
            return AnalysisResult(
                repCount = count,
                formFeedback = feedback,
                angle = rawAngle,
                currentPhase = RepPhase.HOLD,
                confidence = confidence
            )
        }

        // Standard angle-based phase detection
        history.addLast(rawAngle)
        if (history.size > 5) history.removeFirst()
        val smoothed = history.average()

        val phase = if (smoothed < config.upThreshold) RepPhase.UP
                     else if (smoothed > config.downThreshold) RepPhase.DOWN
                     else lastPhase ?: RepPhase.DOWN

        if (lastPhase == RepPhase.DOWN && phase == RepPhase.UP) {
            repCount++
        }

        lastPhase = phase
        lastAngle = smoothed

        val feedback = getFeedback(rawAngle, exerciseType)

        return AnalysisResult(
            repCount = repCount,
            formFeedback = feedback,
            angle = smoothed,
            currentPhase = phase,
            confidence = confidence
        )
    }

    fun reset() = synchronized(this) {
        repCount = 0
        lastPhase = null
        lastAngle = 0.0
        lastValidAngle = 0.0
        history.clear()
        plankHoldStartMs = 0L
        plankHoldCompleted = false
        consecutiveInvalidCount = 0
        lowConfidenceFrames = 0
    }

    fun close() {
        // FormAnalyzer currently manages local state history and counters.
        // No heavy native MediaPipe resources are held here (currently handled in ViewModel/Activity),
        // but this method exists to support future resource management.
    }
}