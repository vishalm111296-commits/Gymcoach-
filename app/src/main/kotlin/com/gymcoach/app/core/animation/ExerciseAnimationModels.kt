package com.gymcoach.app.core.animation

import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Biomechanical phases of an exercise repetition.
 */
enum class AnimationPhase(val displayName: String) {
    SETUP("Setup"),
    START("Start"),
    ECCENTRIC("Eccentric"),
    BOTTOM("Bottom"),
    CONCENTRIC("Concentric"),
    END("Lockout / End")
}

/**
 * Perspective from which the skeletal model is viewed.
 */
enum class ViewPerspective {
    SIDE,
    FRONT
}

/**
 * Normalized 2D joint coordinate within [0.0..1.0].
 * (0,0) is top-left, (1,1) is bottom-right.
 */
data class JointPoint(
    val x: Float,
    val y: Float
)

/**
 * Equipment rendering geometry associated with a keyframe.
 */
data class EquipmentGeometry(
    val type: String, // "barbell", "dumbbell", "cable", "bench", "machine", "none"
    val points: List<JointPoint> = emptyList()
)

/**
 * Biomechanical specification for measuring joint angles (e.g. knee flexion, elbow angle).
 */
data class JointAngleSpec(
    val label: String,
    val pointA: String,
    val centerPoint: String,
    val pointB: String
)

/**
 * Computed angle readout for display.
 */
data class JointAngleReadout(
    val label: String,
    val centerCoord: JointPoint,
    val angleDegrees: Float
)

/**
 * A single discrete keyframe of a skeletal movement.
 */
data class SkeletalKeyframe(
    val progress: Float, // Normalized progress in cycle: 0.0f to 1.0f
    val phase: AnimationPhase,
    val joints: Map<String, JointPoint>,
    val equipment: EquipmentGeometry? = null,
    val activeMuscles: List<String> = emptyList(),
    val cue: String = ""
)

/**
 * Complete data-driven animation definition for an exercise.
 */
data class ExerciseAnimationDefinition(
    val exerciseId: String,
    val exerciseName: String,
    val perspective: ViewPerspective = ViewPerspective.SIDE,
    val durationMs: Long = 3200L,
    val keyframes: List<SkeletalKeyframe>,
    val angleSpecs: List<JointAngleSpec> = emptyList(),
    val primaryMuscle: String = "",
    val description: String = ""
) {
    init {
        require(keyframes.isNotEmpty()) { "ExerciseAnimationDefinition must contain at least one keyframe." }
    }

    /**
     * Pre-calculated full trajectory path for equipment / movement endpoint (e.g. barbell path).
     */
    val trajectoryPath: List<JointPoint> by lazy {
        keyframes.mapNotNull { kf ->
            // Try equipment point first, then wrist/ankle
            kf.equipment?.points?.firstOrNull()
                ?: kf.joints["wrist"]
                ?: kf.joints["wrist_near"]
                ?: kf.joints["ankle"]
                ?: kf.joints["ankle_near"]
        }
    }

    /**
     * Interpolates skeletal joints and equipment at an arbitrary progress point [0.0..1.0].
     */
    fun interpolateAt(progress: Float): InterpolatedFrame {
        val clamped = progress.coerceIn(0.0f, 1.0f)
        if (keyframes.size == 1) {
            val single = keyframes.first()
            return InterpolatedFrame(
                progress = clamped,
                phase = single.phase,
                joints = single.joints,
                equipment = single.equipment,
                activeMuscles = single.activeMuscles,
                cue = single.cue,
                angleReadouts = computeAngles(single.joints)
            )
        }

        // Find surrounding keyframes
        var prev = keyframes.first()
        var next = keyframes.last()

        for (i in 0 until keyframes.size - 1) {
            val kfA = keyframes[i]
            val kfB = keyframes[i + 1]
            if (clamped in kfA.progress..kfB.progress) {
                prev = kfA
                next = kfB
                break
            }
        }

        val range = next.progress - prev.progress
        val localT = if (range <= 0.0001f) 0.0f else ((clamped - prev.progress) / range).coerceIn(0.0f, 1.0f)

        // Smooth cosine easing
        val eased = if (keyframes.size == 2) {
            (1.0f - kotlin.math.cos(localT * Math.PI.toFloat())) / 2.0f
        } else {
            // Apply slight cubic hermite smoothstep for local segment transitions
            localT * localT * (3.0f - 2.0f * localT)
        }

        val interpolatedJoints = HashMap<String, JointPoint>(prev.joints.size)
        for ((name, p1) in prev.joints) {
            val p2 = next.joints[name] ?: p1
            val ix = p1.x + (p2.x - p1.x) * eased
            val iy = p1.y + (p2.y - p1.y) * eased
            interpolatedJoints[name] = JointPoint(ix, iy)
        }

        val prevEq = prev.equipment
        val nextEq = next.equipment
        val interpolatedEquipment = if (prevEq != null && nextEq != null && prevEq.type == nextEq.type) {
            val eqPoints = prevEq.points.zip(nextEq.points) { ptA, ptB ->
                JointPoint(
                    x = ptA.x + (ptB.x - ptA.x) * eased,
                    y = ptA.y + (ptB.y - ptA.y) * eased
                )
            }
            EquipmentGeometry(type = prevEq.type, points = eqPoints)
        } else {
            prevEq ?: nextEq
        }

        val currentPhase = if (localT < 0.5f) prev.phase else next.phase
        val currentCue = if (localT < 0.5f) prev.cue else next.cue
        val currentMuscles = if (localT < 0.5f) prev.activeMuscles else next.activeMuscles

        return InterpolatedFrame(
            progress = clamped,
            phase = currentPhase,
            joints = interpolatedJoints,
            equipment = interpolatedEquipment,
            activeMuscles = currentMuscles,
            cue = currentCue,
            angleReadouts = computeAngles(interpolatedJoints)
        )
    }

    private fun computeAngles(joints: Map<String, JointPoint>): List<JointAngleReadout> {
        if (angleSpecs.isEmpty()) return emptyList()
        val readouts = mutableListOf<JointAngleReadout>()
        for (spec in angleSpecs) {
            val pA = joints[spec.pointA] ?: joints["${spec.pointA}_near"] ?: continue
            val pCenter = joints[spec.centerPoint] ?: joints["${spec.centerPoint}_near"] ?: continue
            val pB = joints[spec.pointB] ?: joints["${spec.pointB}_near"] ?: continue

            val angle = calculateAngle(pA, pCenter, pB)
            readouts.add(
                JointAngleReadout(
                    label = spec.label,
                    centerCoord = pCenter,
                    angleDegrees = angle
                )
            )
        }
        return readouts
    }

    companion object {
        fun calculateAngle(a: JointPoint, center: JointPoint, b: JointPoint): Float {
            val v1x = a.x - center.x
            val v1y = a.y - center.y
            val v2x = b.x - center.x
            val v2y = b.y - center.y

            val dot = v1x * v2x + v1y * v2y
            val mag1 = sqrt(v1x * v1x + v1y * v1y)
            val mag2 = sqrt(v2x * v2x + v2y * v2y)

            if (mag1 < 0.0001f || mag2 < 0.0001f) return 0.0f
            val cosTheta = (dot / (mag1 * mag2)).coerceIn(-1.0f, 1.0f)
            return (acos(cosTheta) * 180.0f / Math.PI.toFloat())
        }
    }
}

/**
 * Result of interpolating an animation definition at a specific point in time.
 */
data class InterpolatedFrame(
    val progress: Float,
    val phase: AnimationPhase,
    val joints: Map<String, JointPoint>,
    val equipment: EquipmentGeometry?,
    val activeMuscles: List<String> = emptyList(),
    val cue: String,
    val angleReadouts: List<JointAngleReadout> = emptyList()
)
