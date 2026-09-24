package com.gymcoach.app.core.animation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AnimationSystemTest {

    private fun resolveAssetsFile(fileName: String): File {
        val path1 = File("app/src/main/assets/$fileName")
        if (path1.exists()) return path1
        val path2 = File("src/main/assets/$fileName")
        if (path2.exists()) return path2
        throw IllegalStateException("Assets file $fileName not found in expected paths")
    }

    @Test
    fun testParseListFromAssetFile() {
        val assetFile = resolveAssetsFile("animations/exercise_animations.json")
        assertTrue("Asset file must exist", assetFile.exists())
        val json = assetFile.readText()
        val definitions = AnimationParser.parseList(json)

        assertEquals("Must parse 20 high-value exercise animations", 20, definitions.size)

        for (def in definitions) {
            assertTrue("Exercise ID must not be blank", def.exerciseId.isNotBlank())
            assertTrue("Exercise Name must not be blank", def.exerciseName.isNotBlank())
            assertTrue("DurationMs must be positive", def.durationMs >= 1000)
            assertTrue("Must have at least 2 keyframes", def.keyframes.size >= 2)

            for (kf in def.keyframes) {
                assertTrue("Progress must be in range [0.0, 1.0]", kf.progress in 0.0f..1.0f)
                assertTrue("Joints must not be empty", kf.joints.isNotEmpty())
                for ((jointName, point) in kf.joints) {
                    assertTrue("Joint $jointName X must be within [0, 1]", point.x in 0.0f..1.0f)
                    assertTrue("Joint $jointName Y must be within [0, 1]", point.y in 0.0f..1.0f)
                }
            }
        }
    }

    @Test
    fun testInterpolationEndpoints() {
        val kf0 = SkeletalKeyframe(
            progress = 0.0f,
            phase = AnimationPhase.START,
            cue = "Starting position",
            joints = mapOf("hand" to JointPoint(0.2f, 0.2f))
        )
        val kf1 = SkeletalKeyframe(
            progress = 1.0f,
            phase = AnimationPhase.END,
            cue = "Ending position",
            joints = mapOf("hand" to JointPoint(0.8f, 0.8f))
        )
        val def = ExerciseAnimationDefinition(
            exerciseId = "test_ex",
            exerciseName = "Test Exercise",
            perspective = ViewPerspective.SIDE,
            durationMs = 1000,
            keyframes = listOf(kf0, kf1)
        )

        val frameAt0 = def.interpolateAt(0.0f)
        assertEquals(0.2f, frameAt0.joints["hand"]?.x ?: 0f, 0.001f)
        assertEquals(0.2f, frameAt0.joints["hand"]?.y ?: 0f, 0.001f)
        assertEquals(AnimationPhase.START, frameAt0.phase)
        assertEquals("Starting position", frameAt0.cue)

        val frameAt1 = def.interpolateAt(1.0f)
        assertEquals(0.8f, frameAt1.joints["hand"]?.x ?: 0f, 0.001f)
        assertEquals(0.8f, frameAt1.joints["hand"]?.y ?: 0f, 0.001f)
        assertEquals(AnimationPhase.END, frameAt1.phase)
        assertEquals("Ending position", frameAt1.cue)

        val frameAtMid = def.interpolateAt(0.5f)
        assertEquals(0.5f, frameAtMid.joints["hand"]?.x ?: 0f, 0.01f)
        assertEquals(0.5f, frameAtMid.joints["hand"]?.y ?: 0f, 0.01f)

        // Test non-linear cosine easing at 0.25f progress
        // Eased localT = (1 - cos(0.25 * PI)) / 2 ≈ 0.1464466
        // Expected X = 0.2 + (0.8 - 0.2) * 0.1464466 ≈ 0.287868
        val frameAtQuarter = def.interpolateAt(0.25f)
        val quarterX = frameAtQuarter.joints["hand"]?.x ?: 0f
        assertEquals(0.2878f, quarterX, 0.01f)
        assertTrue("Cosine eased X at 0.25 ($quarterX) must be strictly less than linear 0.35", quarterX < 0.35f)
    }

    @Test
    fun testMultiKeyframeContinuousProgression() {
        val kf0 = SkeletalKeyframe(
            progress = 0.0f,
            phase = AnimationPhase.START,
            cue = "Start",
            joints = mapOf("hand" to JointPoint(0.0f, 0.0f))
        )
        val kf1 = SkeletalKeyframe(
            progress = 0.5f,
            phase = AnimationPhase.ECCENTRIC,
            cue = "Mid",
            joints = mapOf("hand" to JointPoint(0.5f, 0.5f))
        )
        val kf2 = SkeletalKeyframe(
            progress = 1.0f,
            phase = AnimationPhase.END,
            cue = "End",
            joints = mapOf("hand" to JointPoint(1.0f, 1.0f))
        )
        val def = ExerciseAnimationDefinition(
            exerciseId = "multi_keyframe_test",
            exerciseName = "Multi Keyframe Test",
            perspective = ViewPerspective.SIDE,
            durationMs = 1000,
            keyframes = listOf(kf0, kf1, kf2)
        )

        // For >=3 keyframes, interpolation is continuous linear per segment
        // At progress 0.25f (midway between 0.0 and 0.5), localT = 0.5, expected X = 0.25f
        val frameAtQuarter = def.interpolateAt(0.25f)
        assertEquals(0.25f, frameAtQuarter.joints["hand"]?.x ?: 0f, 0.001f)
        assertEquals(0.25f, frameAtQuarter.joints["hand"]?.y ?: 0f, 0.001f)
    }

    @Test
    fun testInvalidJsonGracefulRecovery() {
        val emptyResult = AnimationParser.parseList("")
        assertTrue(emptyResult.isEmpty())

        val invalidJsonResult = AnimationParser.parseList("{ not valid json }")
        assertTrue(invalidJsonResult.isEmpty())

        val singleDef = AnimationParser.parseSingle("""
            {
                "exerciseId": "single_test",
                "exerciseName": "Single Test",
                "perspective": "front",
                "durationMs": 2000,
                "keyframes": [
                    {
                        "progress": 0.0,
                        "phase": "setup",
                        "joints": { "head": { "x": 0.5, "y": 0.2 } }
                    },
                    {
                        "progress": 1.0,
                        "phase": "end",
                        "joints": { "head": { "x": 0.5, "y": 0.2 } }
                    }
                ]
            }
        """.trimIndent())

        assertNotNull(singleDef)
        assertEquals("single_test", singleDef?.exerciseId)
        assertEquals(ViewPerspective.FRONT, singleDef?.perspective)
    }

    @Test
    fun testAll20DefinitionsInterpolationAtRequiredProgressPoints() {
        val assetFile = resolveAssetsFile("animations/exercise_animations.json")
        val definitions = AnimationParser.parseList(assetFile.readText())
        assertEquals(20, definitions.size)

        val progressPoints = floatArrayOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f)

        for (def in definitions) {
            for (p in progressPoints) {
                val interpolated = def.interpolateAt(p)
                assertNotNull("Interpolated frame must not be null for ${def.exerciseId} at progress $p", interpolated)
                assertTrue("Interpolated frame joints must not be empty for ${def.exerciseId} at $p", interpolated.joints.isNotEmpty())

                for ((jointName, pt) in interpolated.joints) {
                    assertFalse("Joint $jointName X must not be NaN for ${def.exerciseId} at $p", pt.x.isNaN())
                    assertFalse("Joint $jointName Y must not be NaN for ${def.exerciseId} at $p", pt.y.isNaN())
                    assertFalse("Joint $jointName X must not be Infinite for ${def.exerciseId} at $p", pt.x.isInfinite())
                    assertFalse("Joint $jointName Y must not be Infinite for ${def.exerciseId} at $p", pt.y.isInfinite())
                    assertTrue("Joint $jointName X (${pt.x}) must be in [0.0, 1.0] for ${def.exerciseId} at $p", pt.x in 0.0f..1.0f)
                    assertTrue("Joint $jointName Y (${pt.y}) must be in [0.0, 1.0] for ${def.exerciseId} at $p", pt.y in 0.0f..1.0f)
                }
            }
        }
    }

    @Test
    fun testJointAngleComputation() {
        // Test right angle (90 degrees)
        val pA = JointPoint(0.5f, 0.2f) // above
        val center = JointPoint(0.5f, 0.5f) // center
        val pB = JointPoint(0.8f, 0.5f) // right
        val angle90 = ExerciseAnimationDefinition.calculateAngle(pA, center, pB)
        assertEquals(90.0f, angle90, 0.5f)

        // Test straight line (180 degrees)
        val pC = JointPoint(0.5f, 0.8f) // below
        val angle180 = ExerciseAnimationDefinition.calculateAngle(pA, center, pC)
        assertEquals(180.0f, angle180, 0.5f)

        // Test zero length safety
        val angleZero = ExerciseAnimationDefinition.calculateAngle(center, center, pB)
        assertEquals(0.0f, angleZero, 0.001f)
    }

    @Test
    fun testBiomechanicalAngleReadoutsAndTrajectories() {
        val kf0 = SkeletalKeyframe(
            progress = 0.0f,
            phase = AnimationPhase.START,
            cue = "Top",
            joints = mapOf(
                "hip" to JointPoint(0.5f, 0.5f),
                "knee" to JointPoint(0.5f, 0.7f),
                "ankle" to JointPoint(0.5f, 0.9f)
            ),
            equipment = EquipmentGeometry("barbell", listOf(JointPoint(0.5f, 0.3f))),
            activeMuscles = listOf("quads", "glutes")
        )
        val kf1 = SkeletalKeyframe(
            progress = 1.0f,
            phase = AnimationPhase.BOTTOM,
            cue = "Deep Squat",
            joints = mapOf(
                "hip" to JointPoint(0.4f, 0.7f),
                "knee" to JointPoint(0.6f, 0.7f),
                "ankle" to JointPoint(0.5f, 0.9f)
            ),
            equipment = EquipmentGeometry("barbell", listOf(JointPoint(0.5f, 0.6f))),
            activeMuscles = listOf("quads", "glutes", "hamstrings")
        )

        val def = ExerciseAnimationDefinition(
            exerciseId = "squat_test",
            exerciseName = "Barbell Squat",
            perspective = ViewPerspective.SIDE,
            durationMs = 2000,
            keyframes = listOf(kf0, kf1),
            angleSpecs = listOf(
                JointAngleSpec(label = "Knee Angle", pointA = "hip", centerPoint = "knee", pointB = "ankle")
            ),
            primaryMuscle = "quads"
        )

        // Test trajectory path
        assertEquals(2, def.trajectoryPath.size)
        assertEquals(0.5f, def.trajectoryPath[0].x, 0.001f)
        assertEquals(0.3f, def.trajectoryPath[0].y, 0.001f)
        assertEquals(0.5f, def.trajectoryPath[1].x, 0.001f)
        assertEquals(0.6f, def.trajectoryPath[1].y, 0.001f)

        // Test interpolated frame angles
        val frame0 = def.interpolateAt(0.0f)
        assertEquals(1, frame0.angleReadouts.size)
        assertEquals("Knee Angle", frame0.angleReadouts[0].label)
        assertEquals(180.0f, frame0.angleReadouts[0].angleDegrees, 1.0f)
        assertEquals(listOf("quads", "glutes"), frame0.activeMuscles)

        val frame1 = def.interpolateAt(1.0f)
        assertEquals(1, frame1.angleReadouts.size)
        assertTrue("Knee angle at bottom must be flexed (< 120 deg)", frame1.angleReadouts[0].angleDegrees < 120.0f)
        assertEquals(listOf("quads", "glutes", "hamstrings"), frame1.activeMuscles)
    }
}
