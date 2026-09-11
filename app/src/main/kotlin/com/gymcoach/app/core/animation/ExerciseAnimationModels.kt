package com.gymcoach.app.core.animation

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
 * A single discrete keyframe of a skeletal movement.
 */
data class SkeletalKeyframe(
    val progress: Float, // Normalized progress in cycle: 0.0f to 1.0f
    val phase: AnimationPhase,
    val joints: Map<String, JointPoint>,
    val equipment: EquipmentGeometry? = null,
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
    val description: String = ""
) {
    init {
        require(keyframes.isNotEmpty()) { "ExerciseAnimationDefinition must contain at least one keyframe." }
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
                cue = single.cue
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

        // Smooth cosine easing for natural human motion
        val eased = (1.0f - kotlin.math.cos(localT * Math.PI.toFloat())) / 2.0f

        val interpolatedJoints = mutableMapOf<String, JointPoint>()
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

        return InterpolatedFrame(
            progress = clamped,
            phase = currentPhase,
            joints = interpolatedJoints,
            equipment = interpolatedEquipment,
            cue = currentCue
        )
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
    val cue: String
)
