package com.gymcoach.app.core.animation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * High-performance vector skeletal renderer for exercise animations.
 * Renders normalized joint coordinates on a Compose DrawScope with zero allocations per frame.
 */
object SkeletalRenderer {

    fun drawSkeleton(
        drawScope: DrawScope,
        frame: InterpolatedFrame,
        perspective: ViewPerspective,
        primaryColor: Color,
        jointColor: Color,
        equipmentColor: Color,
        floorColor: Color = Color.Gray.copy(alpha = 0.35f)
    ) = with(drawScope) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)

        val boneStroke = minDim * 0.022f
        val thinStroke = minDim * 0.014f
        val jointRadius = minDim * 0.018f
        val headRadius = minDim * 0.052f

        fun pt(name: String): Offset? {
            val j = frame.joints[name] ?: return null
            return Offset(j.x * w, j.y * h)
        }

        // Draw subtle floor baseline
        val floorY = h * 0.94f
        drawLine(
            color = floorColor,
            start = Offset(w * 0.05f, floorY),
            end = Offset(w * 0.95f, floorY),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )

        // Draw equipment like bench or rack base first (background)
        frame.equipment?.let { eq ->
            if (eq.type == "bench" && eq.points.size >= 2) {
                val p1 = Offset(eq.points[0].x * w, eq.points[0].y * h)
                val p2 = Offset(eq.points[1].x * w, eq.points[1].y * h)
                // Bench pad
                drawLine(
                    color = equipmentColor.copy(alpha = 0.6f),
                    start = p1,
                    end = p2,
                    strokeWidth = boneStroke * 1.5f,
                    cap = StrokeCap.Round
                )
                // Bench support legs
                drawLine(
                    color = equipmentColor.copy(alpha = 0.4f),
                    start = p1,
                    end = Offset(p1.x, floorY),
                    strokeWidth = thinStroke
                )
                drawLine(
                    color = equipmentColor.copy(alpha = 0.4f),
                    start = p2,
                    end = Offset(p2.x, floorY),
                    strokeWidth = thinStroke
                )
            }
        }

        fun drawLimb(startName: String, midName: String, endName: String, limbColor: Color, strokeWidth: Float) {
            val pStart = pt(startName) ?: return
            val pMid = pt(midName)
            val pEnd = pt(endName)

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

        if (perspective == ViewPerspective.SIDE) {
            // Far leg
            drawLimb("hip_far", "knee_far", "ankle_far", farColor, boneStroke)
            pt("ankle_far")?.let { a ->
                pt("foot_far")?.let { f ->
                    drawLine(farColor, a, f, boneStroke, cap = StrokeCap.Round)
                }
            }

            // Far arm
            drawLimb("shoulder_far", "elbow_far", "wrist_far", farColor, boneStroke * 0.9f)

            // Torso / Spine
            val neck = pt("neck") ?: pt("shoulder") ?: pt("shoulder_near")
            val hip = pt("hip") ?: pt("hip_near")
            if (neck != null && hip != null) {
                drawLine(nearColor, neck, hip, boneStroke * 1.3f, cap = StrokeCap.Round)
            }

            // Head
            val head = pt("head") ?: neck?.let { Offset(it.x, it.y - headRadius * 1.4f) }
            if (head != null) {
                drawCircle(color = nearColor, radius = headRadius, center = head)
                drawCircle(color = jointColor, radius = headRadius, center = head, style = Stroke(width = 2.5f))
            }

            // Near leg
            val hipNear = pt("hip_near") ?: pt("hip")
            val kneeNear = pt("knee_near") ?: pt("knee")
            val ankleNear = pt("ankle_near") ?: pt("ankle")
            if (hipNear != null && kneeNear != null) {
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

            // Near arm
            val shoulderNear = pt("shoulder_near") ?: pt("shoulder")
            val elbowNear = pt("elbow_near") ?: pt("elbow")
            val wristNear = pt("wrist_near") ?: pt("wrist")
            if (shoulderNear != null && elbowNear != null) {
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
                drawLine(nearColor, midChest, midPelvis, boneStroke * 1.3f, cap = StrokeCap.Round)
            }

            // Clavicle line
            val sLeft = pt("shoulder_left") ?: pt("shoulder")
            val sRight = pt("shoulder_right")
            if (sLeft != null && sRight != null) {
                drawLine(nearColor, sLeft, sRight, boneStroke, cap = StrokeCap.Round)
            }

            // Head
            val head = pt("head") ?: neck?.let { Offset(it.x, it.y - headRadius * 1.3f) }
            if (head != null) {
                drawCircle(color = nearColor, radius = headRadius, center = head)
                drawCircle(color = jointColor, radius = headRadius, center = head, style = Stroke(width = 2.5f))
            }

            // Left arm & Right arm
            drawLimb("shoulder_left", "elbow_left", "wrist_left", nearColor, boneStroke * 0.95f)
            drawLimb("shoulder_right", "elbow_right", "wrist_right", nearColor, boneStroke * 0.95f)

            // Left leg & Right leg
            drawLimb("hip_left", "knee_left", "ankle_left", nearColor, boneStroke)
            drawLimb("hip_right", "knee_right", "ankle_right", nearColor, boneStroke)

            pt("ankle_left")?.let { a -> pt("foot_left")?.let { f -> drawLine(nearColor, a, f, boneStroke, cap = StrokeCap.Round) } }
            pt("ankle_right")?.let { a -> pt("foot_right")?.let { f -> drawLine(nearColor, a, f, boneStroke, cap = StrokeCap.Round) } }
        }

        // Equipment foreground rendering (Barbell, Dumbbells, Cables)
        frame.equipment?.let { eq ->
            when (eq.type) {
                "barbell" -> {
                    if (eq.points.size >= 2) {
                        val bStart = Offset(eq.points[0].x * w, eq.points[0].y * h)
                        val bEnd = Offset(eq.points[1].x * w, eq.points[1].y * h)
                        // Shaft
                        drawLine(
                            color = equipmentColor,
                            start = bStart,
                            end = bEnd,
                            strokeWidth = minDim * 0.02f,
                            cap = StrokeCap.Round
                        )
                        // Outer weight plates
                        val plateRadius = minDim * 0.055f
                        val plateThickness = minDim * 0.025f
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
                        // Side-view barbell: circular bar cross-section + disc plate
                        val center = Offset(eq.points[0].x * w, eq.points[0].y * h)
                        drawCircle(color = equipmentColor.copy(alpha = 0.85f), radius = minDim * 0.06f, center = center)
                        drawCircle(color = jointColor, radius = minDim * 0.02f, center = center)
                    }
                }
                "dumbbell" -> {
                    for (p in eq.points) {
                        val center = Offset(p.x * w, p.y * h)
                        val dbWidth = minDim * 0.045f
                        val dbHeight = minDim * 0.035f
                        // Dumbbell bells
                        drawCircle(color = equipmentColor, radius = dbHeight * 0.6f, center = Offset(center.x - dbWidth * 0.5f, center.y))
                        drawCircle(color = equipmentColor, radius = dbHeight * 0.6f, center = Offset(center.x + dbWidth * 0.5f, center.y))
                        drawLine(
                            color = equipmentColor,
                            start = Offset(center.x - dbWidth * 0.5f, center.y),
                            end = Offset(center.x + dbWidth * 0.5f, center.y),
                            strokeWidth = thinStroke
                        )
                    }
                }
                "cable" -> {
                    if (eq.points.size >= 2) {
                        val pulley = Offset(eq.points[0].x * w, eq.points[0].y * h)
                        val handle = Offset(eq.points[1].x * w, eq.points[1].y * h)
                        // Pulley wheel
                        drawCircle(color = equipmentColor, radius = minDim * 0.025f, center = pulley, style = Stroke(width = 3f))
                        // Cable wire
                        drawLine(
                            color = equipmentColor.copy(alpha = 0.7f),
                            start = pulley,
                            end = handle,
                            strokeWidth = 3f
                        )
                        // Handle bar
                        drawLine(
                            color = equipmentColor,
                            start = Offset(handle.x - 12f, handle.y),
                            end = Offset(handle.x + 12f, handle.y),
                            strokeWidth = boneStroke * 0.8f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}
