package com.gymcoach.app.core.animation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun testProgressBoundaryClamping() {
        val kf0 = SkeletalKeyframe(
            progress = 0.0f,
            phase = AnimationPhase.START,
            cue = "Start",
            joints = mapOf("wrist" to JointPoint(0.1f, 0.2f))
        )
        val kf1 = SkeletalKeyframe(
            progress = 1.0f,
            phase = AnimationPhase.END,
            cue = "End",
            joints = mapOf("wrist" to JointPoint(0.9f, 0.8f))
        )
        val def = ExerciseAnimationDefinition(
            exerciseId = "clamp_test",
            exerciseName = "Clamp Test",
            keyframes = listOf(kf0, kf1)
        )

        val negativeProgress = def.interpolateAt(-0.5f)
        assertEquals(0.0f, negativeProgress.progress, 0.0001f)
        assertEquals(0.1f, negativeProgress.joints["wrist"]?.x ?: 0f, 0.001f)
        assertEquals(0.2f, negativeProgress.joints["wrist"]?.y ?: 0f, 0.001f)
        assertEquals(AnimationPhase.START, negativeProgress.phase)

        val excessiveProgress = def.interpolateAt(2.0f)
        assertEquals(1.0f, excessiveProgress.progress, 0.0001f)
        assertEquals(0.9f, excessiveProgress.joints["wrist"]?.x ?: 0f, 0.001f)
        assertEquals(0.8f, excessiveProgress.joints["wrist"]?.y ?: 0f, 0.001f)
        assertEquals(AnimationPhase.END, excessiveProgress.phase)
    }

    @Test
    fun testSingleKeyframeDefinitionInterpolation() {
        val singleKf = SkeletalKeyframe(
            progress = 0.0f,
            phase = AnimationPhase.SETUP,
            cue = "Hold Position",
            joints = mapOf("hip" to JointPoint(0.5f, 0.5f)),
            equipment = EquipmentGeometry("kettlebell", listOf(JointPoint(0.5f, 0.4f))),
            activeMuscles = listOf("core")
        )
        val def = ExerciseAnimationDefinition(
            exerciseId = "single_kf",
            exerciseName = "Plank Hold",
            keyframes = listOf(singleKf),
            angleSpecs = listOf(
                JointAngleSpec("Straight", "hip", "hip", "hip")
            )
        )

        val frame = def.interpolateAt(0.5f)
        assertEquals(0.5f, frame.progress, 0.0001f)
        assertEquals(AnimationPhase.SETUP, frame.phase)
        assertEquals("Hold Position", frame.cue)
        assertEquals(0.5f, frame.joints["hip"]?.x ?: 0f, 0.001f)
        assertEquals("kettlebell", frame.equipment?.type)
        assertEquals(1, frame.angleReadouts.size)
    }

    @Test
    fun testEquipmentGeometryInterpolationAndFallbacks() {
        // Matching equipment type interpolates points
        val kf0 = SkeletalKeyframe(
            progress = 0.0f,
            phase = AnimationPhase.START,
            joints = emptyMap(),
            equipment = EquipmentGeometry("barbell", listOf(JointPoint(0.2f, 0.2f)))
        )
        val kf1 = SkeletalKeyframe(
            progress = 1.0f,
            phase = AnimationPhase.END,
            joints = emptyMap(),
            equipment = EquipmentGeometry("barbell", listOf(JointPoint(0.8f, 0.8f)))
        )
        val defMatch = ExerciseAnimationDefinition(
            exerciseId = "barbell_match",
            exerciseName = "Barbell Match",
            keyframes = listOf(kf0, kf1)
        )
        val midMatch = defMatch.interpolateAt(0.5f)
        assertNotNull(midMatch.equipment)
        assertEquals("barbell", midMatch.equipment?.type)
        assertEquals(0.5f, midMatch.equipment?.points?.firstOrNull()?.x ?: 0f, 0.01f)

        // Differing equipment types falls back without crashing
        val kfDiff = SkeletalKeyframe(
            progress = 1.0f,
            phase = AnimationPhase.END,
            joints = emptyMap(),
            equipment = EquipmentGeometry("dumbbell", listOf(JointPoint(0.8f, 0.8f)))
        )
        val defDiff = ExerciseAnimationDefinition(
            exerciseId = "eq_diff",
            exerciseName = "Eq Diff",
            keyframes = listOf(kf0, kfDiff)
        )
        val midDiff = defDiff.interpolateAt(0.5f)
        assertNotNull(midDiff.equipment)
        assertTrue(midDiff.equipment?.type == "barbell" || midDiff.equipment?.type == "dumbbell")
    }

    @Test
    fun testTrajectoryPathPriorityHierarchy() {
        // Priority 1: equipment point
        val def1 = ExerciseAnimationDefinition(
            exerciseId = "t1",
            exerciseName = "Trajectory 1",
            keyframes = listOf(
                SkeletalKeyframe(
                    progress = 0.0f,
                    phase = AnimationPhase.START,
                    joints = mapOf(
                        "wrist" to JointPoint(0.2f, 0.2f),
                        "wrist_near" to JointPoint(0.3f, 0.3f),
                        "ankle" to JointPoint(0.4f, 0.4f)
                    ),
                    equipment = EquipmentGeometry("barbell", listOf(JointPoint(0.1f, 0.1f)))
                )
            )
        )
        assertEquals(0.1f, def1.trajectoryPath[0].x, 0.001f)

        // Priority 2: wrist (no equipment)
        val def2 = ExerciseAnimationDefinition(
            exerciseId = "t2",
            exerciseName = "Trajectory 2",
            keyframes = listOf(
                SkeletalKeyframe(
                    progress = 0.0f,
                    phase = AnimationPhase.START,
                    joints = mapOf(
                        "wrist" to JointPoint(0.2f, 0.2f),
                        "wrist_near" to JointPoint(0.3f, 0.3f),
                        "ankle" to JointPoint(0.4f, 0.4f)
                    )
                )
            )
        )
        assertEquals(0.2f, def2.trajectoryPath[0].x, 0.001f)

        // Priority 3: wrist_near (no wrist, no equipment)
        val def3 = ExerciseAnimationDefinition(
            exerciseId = "t3",
            exerciseName = "Trajectory 3",
            keyframes = listOf(
                SkeletalKeyframe(
                    progress = 0.0f,
                    phase = AnimationPhase.START,
                    joints = mapOf(
                        "wrist_near" to JointPoint(0.3f, 0.3f),
                        "ankle" to JointPoint(0.4f, 0.4f)
                    )
                )
            )
        )
        assertEquals(0.3f, def3.trajectoryPath[0].x, 0.001f)

        // Priority 4: ankle
        val def4 = ExerciseAnimationDefinition(
            exerciseId = "t4",
            exerciseName = "Trajectory 4",
            keyframes = listOf(
                SkeletalKeyframe(
                    progress = 0.0f,
                    phase = AnimationPhase.START,
                    joints = mapOf(
                        "ankle" to JointPoint(0.4f, 0.4f)
                    )
                )
            )
        )
        assertEquals(0.4f, def4.trajectoryPath[0].x, 0.001f)

        // Priority 5: ankle_near
        val def5 = ExerciseAnimationDefinition(
            exerciseId = "t5",
            exerciseName = "Trajectory 5",
            keyframes = listOf(
                SkeletalKeyframe(
                    progress = 0.0f,
                    phase = AnimationPhase.START,
                    joints = mapOf(
                        "ankle_near" to JointPoint(0.5f, 0.5f)
                    )
                )
            )
        )
        assertEquals(0.5f, def5.trajectoryPath[0].x, 0.001f)
    }

    @Test
    fun testJointAngleNearSuffixFallback() {
        val kf = SkeletalKeyframe(
            progress = 0.0f,
            phase = AnimationPhase.START,
            joints = mapOf(
                "shoulder_near" to JointPoint(0.5f, 0.2f),
                "elbow_near" to JointPoint(0.5f, 0.5f),
                "wrist_near" to JointPoint(0.8f, 0.5f)
            )
        )
        val def = ExerciseAnimationDefinition(
            exerciseId = "near_fallback_test",
            exerciseName = "Near Fallback Test",
            keyframes = listOf(kf),
            angleSpecs = listOf(
                JointAngleSpec("Elbow Flexion", "shoulder", "elbow", "wrist")
            )
        )

        val frame = def.interpolateAt(0.0f)
        assertEquals(1, frame.angleReadouts.size)
        assertEquals("Elbow Flexion", frame.angleReadouts[0].label)
        assertEquals(90.0f, frame.angleReadouts[0].angleDegrees, 0.5f)
    }

    @Test
    fun testAngleCalculation_AcuteRightObtuse() {
        val center = JointPoint(0.5f, 0.5f)
        val pUp = JointPoint(0.5f, 0.0f)

        // 45 degrees acute
        val p45 = JointPoint(1.0f, 0.0f)
        val acuteAngle = ExerciseAnimationDefinition.calculateAngle(pUp, center, p45)
        assertEquals(45.0f, acuteAngle, 0.5f)

        // 90 degrees right angle
        val pRight = JointPoint(1.0f, 0.5f)
        val rightAngle = ExerciseAnimationDefinition.calculateAngle(pUp, center, pRight)
        assertEquals(90.0f, rightAngle, 0.5f)

        // 135 degrees obtuse
        val pObtuse = JointPoint(1.0f, 1.0f)
        val obtuseAngle = ExerciseAnimationDefinition.calculateAngle(pUp, center, pObtuse)
        assertEquals(135.0f, obtuseAngle, 0.5f)
    }

    @Test
    fun testTrajectoryPathPrioritizesMovingJointForStaticEquipment() {
        // Bench: static pads, trajectory should track wrist movement
        val kfBench1 = SkeletalKeyframe(
            progress = 0.0f,
            phase = AnimationPhase.START,
            joints = mapOf("wrist" to JointPoint(0.42f, 0.20f)),
            equipment = EquipmentGeometry("bench", listOf(JointPoint(0.22f, 0.62f), JointPoint(0.66f, 0.62f)))
        )
        val kfBench2 = SkeletalKeyframe(
            progress = 1.0f,
            phase = AnimationPhase.BOTTOM,
            joints = mapOf("wrist" to JointPoint(0.42f, 0.52f)),
            equipment = EquipmentGeometry("bench", listOf(JointPoint(0.22f, 0.62f), JointPoint(0.66f, 0.62f)))
        )
        val benchDef = ExerciseAnimationDefinition(
            exerciseId = "bench_press",
            exerciseName = "Bench Press",
            keyframes = listOf(kfBench1, kfBench2)
        )
        assertEquals(2, benchDef.trajectoryPath.size)
        assertEquals(0.20f, benchDef.trajectoryPath[0].y, 0.001f)
        assertEquals(0.52f, benchDef.trajectoryPath[1].y, 0.001f)

        // Pull-Up: static bar, trajectory should track neck / head displacement
        val kfPull1 = SkeletalKeyframe(
            progress = 0.0f,
            phase = AnimationPhase.START,
            joints = mapOf("neck" to JointPoint(0.50f, 0.35f)),
            equipment = EquipmentGeometry("pullup_bar", listOf(JointPoint(0.50f, 0.15f)))
        )
        val kfPull2 = SkeletalKeyframe(
            progress = 1.0f,
            phase = AnimationPhase.BOTTOM,
            joints = mapOf("neck" to JointPoint(0.50f, 0.18f)),
            equipment = EquipmentGeometry("pullup_bar", listOf(JointPoint(0.50f, 0.15f)))
        )
        val pullDef = ExerciseAnimationDefinition(
            exerciseId = "pull_up",
            exerciseName = "Pull-Up",
            keyframes = listOf(kfPull1, kfPull2)
        )
        assertEquals(2, pullDef.trajectoryPath.size)
        assertEquals(0.35f, pullDef.trajectoryPath[0].y, 0.001f)
        assertEquals(0.18f, pullDef.trajectoryPath[1].y, 0.001f)
    }

    @Test
    fun testEnrichedAnimationsCoverage() {
        val assetFile = resolveAssetsFile("animations/exercise_animations.json")
        val json = assetFile.readText()
        val definitions = AnimationParser.parseList(json)

        assertEquals(20, definitions.size)

        val benchmarkSet = setOf(
            "barbell_squat", "bench_press", "deadlift", "barbell_row",
            "biceps_curl", "lateral_raise", "push_up", "pull_up"
        )

        for (def in definitions) {
            assertTrue("Every exercise must have angleSpecs: ${def.exerciseId}", def.angleSpecs.isNotEmpty())
            for (kf in def.keyframes) {
                assertTrue("Every keyframe must have activeMuscles: ${def.exerciseId}", kf.activeMuscles.isNotEmpty())
            }
            if (def.exerciseId in benchmarkSet) {
                assertTrue("Benchmark exercise ${def.exerciseId} must have >= 5 keyframes", def.keyframes.size >= 5)
            }
        }

        val benchPress = definitions.first { it.exerciseId == "bench_press" }
        for (kf in benchPress.keyframes) {
            assertNotNull(kf.equipment)
            assertEquals("bench", kf.equipment?.type)
            assertEquals("Bench press equipment must have 3 points (pads + barbell at wrist)", 3, kf.equipment?.points?.size)
        }
    }

    @Test
    fun testAnimationControllerStateTransitionsComprehensive() {
        val def = ExerciseAnimationDefinition(
            exerciseId = "ctrl_test",
            exerciseName = "Control Test",
            durationMs = 1000,
            keyframes = listOf(
                SkeletalKeyframe(0.0f, AnimationPhase.START, mapOf("head" to JointPoint(0f, 0f))),
                SkeletalKeyframe(0.5f, AnimationPhase.BOTTOM, mapOf("head" to JointPoint(0.5f, 0.5f))),
                SkeletalKeyframe(1.0f, AnimationPhase.END, mapOf("head" to JointPoint(1f, 1f)))
            )
        )
        val controller = AnimationController(def, initialAutoPlay = false)

        assertFalse(controller.isPlaying)
        assertEquals(0.0f, controller.progress, 0.001f)

        controller.play()
        assertTrue(controller.isPlaying)
        assertFalse(controller.isStepMode)

        controller.pause()
        assertFalse(controller.isPlaying)

        controller.seekTo(0.25f)
        assertEquals(0.25f, controller.progress, 0.001f)

        controller.setPlaybackSpeed(1.5f)
        assertEquals(1.5f, controller.speed, 0.001f)

        controller.setLoop(false)
        assertFalse(controller.isLooping)

        controller.nextStep()
        assertFalse("Should pause on step", controller.isPlaying)
        assertTrue(controller.isStepMode)
        assertEquals(0.5f, controller.progress, 0.001f)

        controller.previousStep()
        assertEquals(0.0f, controller.progress, 0.001f)

        controller.replay()
        assertTrue(controller.isPlaying)
        assertEquals(0.0f, controller.progress, 0.001f)
    }

    @Test
    fun testParserErrorHandling_InvalidJsonMissingData() {
        val invalidDef = AnimationParser.parseSingle("{ invalid_json:")
        assertNull(invalidDef)

        val noKeyframesDef = AnimationParser.parseSingle("""
            { "exerciseId": "test", "exerciseName": "Test" }
        """.trimIndent())
        assertNull(noKeyframesDef)

        val emptyJointsDef = AnimationParser.parseSingle("""
            {
                "exerciseId": "test2", 
                "exerciseName": "Test 2",
                "keyframes": [ { "progress": 0.0, "joints": {} } ]
            }
        """.trimIndent())
        assertNull(emptyJointsDef)
    }
}
