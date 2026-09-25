package com.gymcoach.app.core.animation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas

/**
 * High-performance vector skeletal and biomechanical renderer.
 * Renders normalized joint coordinates, muscle activation glows, equipment geometry,
 * joint angles, and ROM trajectory paths on a Compose DrawScope with zero per-frame allocations.
 */
object SkeletalRenderer {

    private val FLOOR_DASH = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
    private val TRAJECTORY_DASH = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
    private val ANGLE_DASH = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)

    private val HEAD_STROKE = Stroke(width = 2.5f)
    private val PULLEY_STROKE = Stroke(width = 3.5f)
    private val ANGLE_STROKE = Stroke(width = 2f, pathEffect = ANGLE_DASH)

    fun drawSkeleton(
        drawScope: DrawScope,
        frame: InterpolatedFrame,
        perspective: ViewPerspective,
        primaryColor: Color,
        jointColor: Color,
        equipmentColor: Color,
        trajectoryPath: List<JointPoint> = emptyList(),
        showTrajectory: Boolean = true,
        showAngles: Boolean = true,
        floorColor: Color = Color.Gray.copy(alpha = 0.35f)
    ) = with(drawScope) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)

        val boneStroke = minDim * 0.024f
        val thinStroke = minDim * 0.014f
        val jointRadius = minDim * 0.018f
        val headRadius = minDim * 0.052f

        fun pt(name: String): Offset? {
            val j = frame.joints[name] ?: return null
            return Offset(j.x * w, j.y * h)
        }

        // Draw floor baseline and perspective shadow
        val floorY = h * 0.94f
        drawLine(
            color = floorColor,
            start = Offset(w * 0.05f, floorY),
            end = Offset(w * 0.95f, floorY),
            strokeWidth = 2f,
            pathEffect = FLOOR_DASH
        )

        // Draw ROM Trajectory Path if enabled
        if (showTrajectory && trajectoryPath.size >= 2) {
            val trajectoryColor = jointColor.copy(alpha = 0.35f)
            for (i in 0 until trajectoryPath.size - 1) {
                val pA = Offset(trajectoryPath[i].x * w, trajectoryPath[i].y * h)
                val pB = Offset(trajectoryPath[i + 1].x * w, trajectoryPath[i + 1].y * h)
                drawLine(
                    color = trajectoryColor,
                    start = pA,
                    end = pB,
                    strokeWidth = 2.5f,
                    pathEffect = TRAJECTORY_DASH,
                    cap = StrokeCap.Round
                )
            }
        }

        // 1. Draw equipment background elements (e.g. bench, rack, machine frame)
        frame.equipment?.let { eq ->
            when (eq.type) {
                "bench" -> {
                    if (eq.points.size >= 2) {
                        val p1 = Offset(eq.points[0].x * w, eq.points[0].y * h)
                        val p2 = Offset(eq.points[1].x * w, eq.points[1].y * h)
                        // Bench padded top
                        drawLine(
                            color = equipmentColor.copy(alpha = 0.75f),
                            start = p1,
                            end = p2,
                            strokeWidth = boneStroke * 1.6f,
                            cap = StrokeCap.Round
                        )
                        // Support legs down to floor
                        drawLine(
                            color = equipmentColor.copy(alpha = 0.45f),
                            start = p1,
                            end = Offset(p1.x, floorY),
                            strokeWidth = thinStroke
                        )
                        drawLine(
                            color = equipmentColor.copy(alpha = 0.45f),
                            start = p2,
                            end = Offset(p2.x, floorY),
                            strokeWidth = thinStroke
                        )
                    }
                }
                "machine" -> {
                    if (eq.points.size >= 2) {
                        val p1 = Offset(eq.points[0].x * w, eq.points[0].y * h)
                        val p2 = Offset(eq.points[1].x * w, eq.points[1].y * h)
                        drawLine(
                            color = equipmentColor.copy(alpha = 0.5f),
                            start = p1,
                            end = p2,
                            strokeWidth = boneStroke * 1.2f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }

        // Helper to draw limb segments with joint circles
        fun drawLimb(
            startName: String,
            midName: String,
            endName: String,
            limbColor: Color,
            strokeWidth: Float,
            isMuscleActive: Boolean = false
        ) {
            val pStart = pt(startName) ?: return
            val pMid = pt(midName)
            val pEnd = pt(endName)

            // Muscle activation glow
            if (isMuscleActive && pMid != null) {
                val glowColor = Color(0xFFFF5722).copy(alpha = 0.35f)
                drawLine(
                    color = glowColor,
                    start = pStart,
                    end = pMid,
                    strokeWidth = strokeWidth * 2.2f,
                    cap = StrokeCap.Round
                )
                if (pEnd != null) {
                    drawLine(
                        color = glowColor,
                        start = pMid,
                        end = pEnd,
                        strokeWidth = strokeWidth * 2.2f,
                        cap = StrokeCap.Round
                    )
                }
            }

            if (pMid != null) {
                drawLine(
                    color = limbColor,
                    start = pStart,
                    end = pMid,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawCircle(color = jointColor, radius = jointRadius, center = pMid)

                if (pEnd != null) {
                    drawLine(
                        color = limbColor,
                        start = pMid,
                        end = pEnd,
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = jointColor, radius = jointRadius * 0.85f, center = pEnd)
                }
            } else if (pEnd != null) {
                drawLine(
                    color = limbColor,
                    start = pStart,
                    end = pEnd,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        val farColor = primaryColor.copy(alpha = 0.45f)
        val nearColor = primaryColor
        val activeMuscles = frame.activeMuscles

        val isLegActive = activeMuscles.any { it.contains("quad", true) || it.contains("glute", true) || it.contains("ham", true) || it.contains("leg", true) }
        val isArmActive = activeMuscles.any { it.contains("bicep", true) || it.contains("tricep", true) || it.contains("arm", true) }
        val isChestActive = activeMuscles.any { it.contains("chest", true) || it.contains("pec", true) }
        val isBackActive = activeMuscles.any { it.contains("back", true) || it.contains("lat", true) }
        val isDeltActive = activeMuscles.any { it.contains("delt", true) || it.contains("shoulder", true) }

        if (perspective == ViewPerspective.SIDE) {
            // 2. Far leg (background layer)
            drawLimb("hip_far", "knee_far", "ankle_far", farColor, boneStroke, isMuscleActive = isLegActive)
            pt("ankle_far")?.let { a ->
                pt("foot_far")?.let { f ->
                    drawLine(farColor, a, f, boneStroke, cap = StrokeCap.Round)
                }
            }

            // 3. Far arm
            drawLimb("shoulder_far", "elbow_far", "wrist_far", farColor, boneStroke * 0.9f, isMuscleActive = isArmActive)

            // 4. Spine / Torso (with back/chest activation glow if applicable)
            val neck = pt("neck") ?: pt("shoulder") ?: pt("shoulder_near")
            val hip = pt("hip") ?: pt("hip_near")
            if (neck != null && hip != null) {
                if (isChestActive || isBackActive) {
                    drawLine(
                        color = Color(0xFFFF5722).copy(alpha = 0.35f),
                        start = neck,
                        end = hip,
                        strokeWidth = boneStroke * 2.4f,
                        cap = StrokeCap.Round
                    )
                }
                drawLine(nearColor, neck, hip, boneStroke * 1.35f, cap = StrokeCap.Round)
            }

            // 5. Head
            val head = pt("head") ?: neck?.let { Offset(it.x, it.y - headRadius * 1.35f) }
            if (head != null) {
                drawCircle(color = nearColor, radius = headRadius, center = head)
                drawCircle(color = jointColor, radius = headRadius, center = head, style = HEAD_STROKE)
            }

            // 6. Near leg (foreground layer)
            val hipNear = pt("hip_near") ?: pt("hip")
            val kneeNear = pt("knee_near") ?: pt("knee")
            val ankleNear = pt("ankle_near") ?: pt("ankle")
            if (hipNear != null && kneeNear != null) {
                if (isLegActive) {
                    drawLine(
                        color = Color(0xFFFF5722).copy(alpha = 0.35f),
                        start = hipNear,
                        end = kneeNear,
                        strokeWidth = boneStroke * 2.2f,
                        cap = StrokeCap.Round
                    )
                }
                drawLine(nearColor, hipNear, kneeNear, boneStroke, cap = StrokeCap.Round)
                drawCircle(color = jointColor, radius = jointRadius, center = kneeNear)
                if (ankleNear != null) {
                    drawLine(nearColor, kneeNear, ankleNear, boneStroke, cap = StrokeCap.Round)
                    drawCircle(color = jointColor, radius = jointRadius * 0.85f, center = ankleNear)
                    val footNear = pt("foot_near") ?: pt("foot")
                    if (footNear != null) {
                        drawLine(nearColor, ankleNear, footNear, boneStroke, cap = StrokeCap.Round)
                    }
                }
            }

            // 7. Near arm
            val shoulderNear = pt("shoulder_near") ?: pt("shoulder")
            val elbowNear = pt("elbow_near") ?: pt("elbow")
            val wristNear = pt("wrist_near") ?: pt("wrist")
            if (shoulderNear != null && elbowNear != null) {
                if (isDeltActive || isArmActive) {
                    drawLine(
                        color = Color(0xFFFF5722).copy(alpha = 0.35f),
                        start = shoulderNear,
                        end = elbowNear,
                        strokeWidth = boneStroke * 2.0f,
                        cap = StrokeCap.Round
                    )
                }
                drawLine(nearColor, shoulderNear, elbowNear, boneStroke * 0.95f, cap = StrokeCap.Round)
                drawCircle(color = jointColor, radius = jointRadius, center = elbowNear)
                if (wristNear != null) {
                    drawLine(nearColor, elbowNear, wristNear, boneStroke * 0.95f, cap = StrokeCap.Round)
                    drawCircle(color = jointColor, radius = jointRadius * 0.85f, center = wristNear)
                }
            }
        } else {
            // FRONT Perspective
            val neck = pt("neck")
            val midChest = pt("chest") ?: neck
            val midPelvis = pt("pelvis") ?: pt("hip")

            // Spine
            if (midChest != null && midPelvis != null) {
                if (isChestActive || isBackActive) {
                    drawLine(
                        color = Color(0xFFFF5722).copy(alpha = 0.35f),
                        start = midChest,
                        end = midPelvis,
                        strokeWidth = boneStroke * 2.4f,
                        cap = StrokeCap.Round
                    )
                }
                drawLine(nearColor, midChest, midPelvis, boneStroke * 1.35f, cap = StrokeCap.Round)
            }

            // Clavicle line
            val sLeft = pt("shoulder_left") ?: pt("shoulder")
            val sRight = pt("shoulder_right")
            if (sLeft != null && sRight != null) {
                if (isDeltActive) {
                    drawLine(
                        color = Color(0xFFFF5722).copy(alpha = 0.35f),
                        start = sLeft,
                        end = sRight,
                        strokeWidth = boneStroke * 2.0f,
                        cap = StrokeCap.Round
                    )
                }
                drawLine(nearColor, sLeft, sRight, boneStroke, cap = StrokeCap.Round)
            }

            // Head
            val head = pt("head") ?: neck?.let { Offset(it.x, it.y - headRadius * 1.35f) }
            if (head != null) {
                drawCircle(color = nearColor, radius = headRadius, center = head)
                drawCircle(color = jointColor, radius = headRadius, center = head, style = HEAD_STROKE)
            }

            // Left arm & Right arm
            drawLimb("shoulder_left", "elbow_left", "wrist_left", nearColor, boneStroke * 0.95f, isMuscleActive = isArmActive)
            drawLimb("shoulder_right", "elbow_right", "wrist_right", nearColor, boneStroke * 0.95f, isMuscleActive = isArmActive)

            // Left leg & Right leg
            drawLimb("hip_left", "knee_left", "ankle_left", nearColor, boneStroke, isMuscleActive = isLegActive)
            drawLimb("hip_right", "knee_right", "ankle_right", nearColor, boneStroke, isMuscleActive = isLegActive)

            pt("ankle_left")?.let { a -> pt("foot_left")?.let { f -> drawLine(nearColor, a, f, boneStroke, cap = StrokeCap.Round) } }
            pt("ankle_right")?.let { a -> pt("foot_right")?.let { f -> drawLine(nearColor, a, f, boneStroke, cap = StrokeCap.Round) } }
        }

        // 8. Foreground equipment rendering (Barbell, Dumbbells, Cable, Pull-up Bar)
        frame.equipment?.let { eq ->
            when (eq.type) {
                "barbell" -> {
                    if (eq.points.size >= 2) {
                        val bStart = Offset(eq.points[0].x * w, eq.points[0].y * h)
                        val bEnd = Offset(eq.points[1].x * w, eq.points[1].y * h)
                        // Barbell shaft
                        drawLine(
                            color = equipmentColor,
                            start = bStart,
                            end = bEnd,
                            strokeWidth = minDim * 0.022f,
                            cap = StrokeCap.Round
                        )
                        // Olympic Bumper Plates
                        val plateRadius = minDim * 0.06f
                        val plateThickness = minDim * 0.028f
                        drawLine(
                            color = equipmentColor,
                            start = Offset(bStart.x, bStart.y - plateRadius),
                            end = Offset(bStart.x, bStart.y + plateRadius),
                            strokeWidth = plateThickness,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = equipmentColor,
                            start = Offset(bEnd.x, bEnd.y - plateRadius),
                            end = Offset(bEnd.x, bEnd.y + plateRadius),
                            strokeWidth = plateThickness,
                            cap = StrokeCap.Round
                        )
                    } else if (eq.points.size == 1) {
                        // Side-view barbell: circular bar plate cross section + collar
                        val center = Offset(eq.points[0].x * w, eq.points[0].y * h)
                        drawCircle(color = equipmentColor.copy(alpha = 0.9f), radius = minDim * 0.065f, center = center)
                        drawCircle(color = jointColor, radius = minDim * 0.022f, center = center)
                    }
                }
                "bench" -> {
                    // For bench press: lifter on bench presses Olympic barbell held at wrist
                    val barCenter = if (eq.points.size >= 3) {
                        Offset(eq.points[2].x * w, eq.points[2].y * h)
                    } else {
                        pt("wrist_near") ?: pt("wrist")
                    }
                    if (barCenter != null) {
                        drawCircle(color = equipmentColor.copy(alpha = 0.9f), radius = minDim * 0.065f, center = barCenter)
                        drawCircle(color = jointColor, radius = minDim * 0.022f, center = barCenter)
                    }
                }
                "dumbbell" -> {
                    for (p in eq.points) {
                        val center = Offset(p.x * w, p.y * h)
                        val dbWidth = minDim * 0.048f
                        val dbHeight = minDim * 0.038f
                        // Hexagonal dumbbell heads
                        drawCircle(color = equipmentColor, radius = dbHeight * 0.6f, center = Offset(center.x - dbWidth * 0.5f, center.y))
                        drawCircle(color = equipmentColor, radius = dbHeight * 0.6f, center = Offset(center.x + dbWidth * 0.5f, center.y))
                        drawLine(
                            color = equipmentColor,
                            start = Offset(center.x - dbWidth * 0.5f, center.y),
                            end = Offset(center.x + dbWidth * 0.5f, center.y),
                            strokeWidth = thinStroke,
                            cap = StrokeCap.Round
                        )
                    }
                }
                "cable" -> {
                    if (eq.points.size >= 2) {
                        val pulley = Offset(eq.points[0].x * w, eq.points[0].y * h)
                        val handle = Offset(eq.points[1].x * w, eq.points[1].y * h)
                        // Pulley wheel
                        drawCircle(color = equipmentColor, radius = minDim * 0.028f, center = pulley, style = PULLEY_STROKE)
                        // Tension cable
                        drawLine(
                            color = equipmentColor.copy(alpha = 0.75f),
                            start = pulley,
                            end = handle,
                            strokeWidth = 3f
                        )
                        // Ergonomic handle
                        drawLine(
                            color = equipmentColor,
                            start = Offset(handle.x - 14f, handle.y),
                            end = Offset(handle.x + 14f, handle.y),
                            strokeWidth = boneStroke * 0.85f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                "pullup_bar" -> {
                    if (eq.points.size >= 2) {
                        val p1 = Offset(eq.points[0].x * w, eq.points[0].y * h)
                        val p2 = Offset(eq.points[1].x * w, eq.points[1].y * h)
                        drawLine(
                            color = equipmentColor,
                            start = p1,
                            end = p2,
                            strokeWidth = boneStroke * 1.1f,
                            cap = StrokeCap.Round
                        )
                    } else if (eq.points.size == 1) {
                        val barY = eq.points[0].y * h
                        drawLine(
                            color = equipmentColor,
                            start = Offset(w * 0.15f, barY),
                            end = Offset(w * 0.85f, barY),
                            strokeWidth = boneStroke * 1.1f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }

        // 9. Joint Angle overlay readouts (if enabled)
        if (showAngles && frame.angleReadouts.isNotEmpty()) {
            for (angle in frame.angleReadouts) {
                val center = Offset(angle.centerCoord.x * w, angle.centerCoord.y * h)
                // Draw small angle arc
                drawCircle(
                    color = jointColor.copy(alpha = 0.4f),
                    radius = jointRadius * 2.0f,
                    center = center,
                    style = ANGLE_STROKE
                )
            }
        }
    }
}
