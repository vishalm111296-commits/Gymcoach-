package com.gymcoach.app.core.animation

import org.json.JSONArray
import org.json.JSONObject

/**
 * Robust, zero-dependency parser for exercise animation definitions.
 */
object AnimationParser {

    fun parseSingle(jsonString: String): ExerciseAnimationDefinition? {
        if (jsonString.isBlank()) return null
        return try {
            parseDefinition(JSONObject(jsonString))
        } catch (e: Exception) {
            null
        }
    }

    fun parseList(jsonString: String): List<ExerciseAnimationDefinition> {
        if (jsonString.isBlank()) return emptyList()
        return try {
            val array = JSONArray(jsonString)
            val list = mutableListOf<ExerciseAnimationDefinition>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                parseDefinition(obj)?.let { list.add(it) }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseDefinition(obj: JSONObject): ExerciseAnimationDefinition? {
        val exerciseId = obj.optString("exerciseId", "").ifBlank {
            obj.optString("id", "")
        }
        val exerciseName = obj.optString("exerciseName", "").ifBlank {
            obj.optString("name", "")
        }
        if (exerciseId.isBlank() || exerciseName.isBlank()) return null

        val perspective = try {
            ViewPerspective.valueOf(obj.optString("perspective", "SIDE").uppercase())
        } catch (e: Exception) {
            ViewPerspective.SIDE
        }
        val durationMs = obj.optLong("durationMs", 3200L).coerceAtLeast(500L)
        val description = obj.optString("description", "")

        val kfArray = obj.optJSONArray("keyframes") ?: return null
        val keyframes = mutableListOf<SkeletalKeyframe>()

        for (i in 0 until kfArray.length()) {
            val kfObj = kfArray.optJSONObject(i) ?: continue
            val progress = kfObj.optDouble("progress", i.toDouble() / (kfArray.length() - 1).coerceAtLeast(1)).toFloat()
            val phaseStr = kfObj.optString("phase", "START").uppercase()
            val phase = try {
                AnimationPhase.valueOf(phaseStr)
            } catch (e: Exception) {
                when {
                    phaseStr.contains("SETUP") -> AnimationPhase.SETUP
                    phaseStr.contains("ECC") -> AnimationPhase.ECCENTRIC
                    phaseStr.contains("BOT") -> AnimationPhase.BOTTOM
                    phaseStr.contains("CON") -> AnimationPhase.CONCENTRIC
                    phaseStr.contains("END") || phaseStr.contains("LOCK") -> AnimationPhase.END
                    else -> AnimationPhase.START
                }
            }
            val cue = kfObj.optString("cue", "")

            val jointsObj = kfObj.optJSONObject("joints") ?: continue
            val joints = mutableMapOf<String, JointPoint>()
            val keys = jointsObj.keys()
            while (keys.hasNext()) {
                val jointName = keys.next()
                val coordsArray = jointsObj.optJSONArray(jointName)
                val coordsObj = jointsObj.optJSONObject(jointName)
                if (coordsArray != null && coordsArray.length() >= 2) {
                    val x = coordsArray.optDouble(0, 0.5).toFloat().coerceIn(0.0f, 1.0f)
                    val y = coordsArray.optDouble(1, 0.5).toFloat().coerceIn(0.0f, 1.0f)
                    joints[jointName] = JointPoint(x, y)
                } else if (coordsObj != null) {
                    val x = coordsObj.optDouble("x", 0.5).toFloat().coerceIn(0.0f, 1.0f)
                    val y = coordsObj.optDouble("y", 0.5).toFloat().coerceIn(0.0f, 1.0f)
                    joints[jointName] = JointPoint(x, y)
                }
            }

            var equipment: EquipmentGeometry? = null
            val eqObj = kfObj.optJSONObject("equipment")
            if (eqObj != null) {
                val eqType = eqObj.optString("type", "none")
                val ptArray = eqObj.optJSONArray("points")
                val eqPoints = mutableListOf<JointPoint>()
                if (ptArray != null) {
                    for (p in 0 until ptArray.length()) {
                        val pt = ptArray.optJSONArray(p)
                        if (pt != null && pt.length() >= 2) {
                            eqPoints.add(
                                JointPoint(
                                    x = pt.optDouble(0, 0.5).toFloat().coerceIn(0.0f, 1.0f),
                                    y = pt.optDouble(1, 0.5).toFloat().coerceIn(0.0f, 1.0f)
                                )
                            )
                        }
                    }
                }
                equipment = EquipmentGeometry(type = eqType, points = eqPoints)
            }

            if (joints.isNotEmpty()) {
                keyframes.add(
                    SkeletalKeyframe(
                        progress = progress.coerceIn(0.0f, 1.0f),
                        phase = phase,
                        joints = joints,
                        equipment = equipment,
                        cue = cue
                    )
                )
            }
        }

        if (keyframes.isEmpty()) return null
        keyframes.sortBy { it.progress }

        return ExerciseAnimationDefinition(
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            perspective = perspective,
            durationMs = durationMs,
            keyframes = keyframes,
            description = description
        )
    }
}
