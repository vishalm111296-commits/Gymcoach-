package com.gymcoach.app.core.exercise

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.gymcoach.app.data.local.database.GymCoachDatabase
import com.gymcoach.app.data.local.entity.EquipmentEntity
import com.gymcoach.app.data.local.entity.ExerciseAliasEntity
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.ExerciseEquipmentEntity
import com.gymcoach.app.data.local.entity.ExerciseMuscleEntity
import com.gymcoach.app.data.local.entity.ExerciseSubstitutionEntity
import com.gymcoach.app.data.local.entity.MuscleEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseSeeder @Inject constructor(
    private val db: GymCoachDatabase,
    @ApplicationContext private val context: Context
) {
    suspend fun seedIfNeeded() = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getInt(KEY_SEED_VERSION, 0) < SEED_VERSION) {
            seed()
            prefs.edit().putInt(KEY_SEED_VERSION, SEED_VERSION).apply()
        }
    }

    private suspend fun seed() = db.withTransaction {
        val muscleIds = seedMuscles()
        val equipmentIds = seedEquipment()
        val exerciseIds = seedExercises(muscleIds, equipmentIds)
        seedSubstitutions(exerciseIds)
        rebuildSearchIndex()
    }

    private fun asset(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    private fun strings(array: JSONArray?): List<String> =
        array?.let { a -> List(a.length()) { i -> a.getString(i) } } ?: emptyList()

    private suspend fun seedMuscles(): Map<String, Long> {
        val ids = mutableMapOf<String, Long>()
        val groups = JSONObject(asset(TAXONOMY_FILE)).getJSONArray("muscles")
        for (g in 0 until groups.length()) {
            val group = groups.getJSONObject(g)
            val groupId = db.muscleDao().insert(
                MuscleEntity(name = group.getString("id"), displayName = group.getString("name"))
            )
            ids[group.getString("id")] = groupId
            val subs = group.optJSONArray("subdivisions") ?: continue
            for (s in 0 until subs.length()) {
                val sub = subs.getJSONObject(s)
                if (sub.getString("id") in ids) continue
                ids[sub.getString("id")] = db.muscleDao().insert(
                    MuscleEntity(
                        name = sub.getString("id"),
                        displayName = sub.getString("name"),
                        parentMuscleId = groupId,
                        bodyRegion = group.getString("id")
                    )
                )
            }
        }
        return ids
    }

    private suspend fun seedEquipment(): Map<String, Long> {
        val equipmentMap = mutableMapOf<String, String>()
        for (file in ALL_EXERCISE_FILES) {
            val root = JSONArray(asset(file))
            for (i in 0 until root.length()) {
                val e = root.getJSONObject(i)
                strings(e.optJSONArray("equipment")).forEach { eq ->
                    if (eq !in equipmentMap) {
                        equipmentMap[eq] = eq.replace("_", " ").replaceFirstChar { it.uppercase() }
                    }
                }
            }
        }
        return equipmentMap.associate { (name, display) ->
            name to db.equipmentDao().insert(EquipmentEntity(name = name, displayName = display, category = name))
        }
    }


    private suspend fun seedExercises(
        muscleIds: Map<String, Long>, equipmentIds: Map<String, Long>
    ): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        for (file in ALL_EXERCISE_FILES) {
            try {
                val root = JSONArray(asset(file))
                for (i in 0 until root.length()) {
                    val e = root.getJSONObject(i)
                    val id = e.getString("id")
                    if (id in result) continue
                    val primary = strings(e.optJSONArray("primary_muscles"))
                    val secondary = strings(e.optJSONArray("secondary_muscles"))
                    val scores = e.optJSONObject("vtaper_scores")
                    val relevance = e.optInt("v_taper_relevance", 0)
                    fun vtaper(key: String, muscle: String): Int =
                        scores?.optInt(key, 0)?.takeIf { it > 0 }
                            ?: if (muscle in primary) relevance else 0
                    val tags = buildList {
                        if (e.optBoolean("compound")) add("compound")
                        if (e.optBoolean("unilateral")) add("unilateral")
                    }
                    val equipment = strings(e.optJSONArray("equipment"))
                    val rowId = db.exerciseDao().insert(
                        ExerciseEntity(
                            name = e.getString("name"),
                            description = e.optString("description"),
                            muscleGroup = e.getString("category"),
                            equipment = equipment.joinToString(","),
                            difficulty = e.getString("difficulty"),
                            secondaryMuscles = secondary.joinToString(", "),
                            instructions = e.optString("execution"),
                            tips = strings(e.optJSONArray("form_cues")).joinToString("; "),
                            commonMistakes = strings(e.optJSONArray("common_mistakes")).joinToString("; "),
                            safetyNotes = strings(e.optJSONArray("safety_notes")).joinToString("; "),
                            recommendedRepRange = e.optString("rep_range"),
                            recommendedRestTime = "${e.optInt("rest_seconds", 60)}s",
                            category = e.getString("category"),
                            tags = tags.joinToString(","),
                            movementPattern = e.optString("movement_pattern"),
                            setupInstructions = e.optString("setup"),
                            executionInstructions = e.optString("execution"),
                            breathingInstructions = e.optString("breathing"),
                            vtaperLat = vtaper("lat", "latissimus_dorsi"),
                            vtaperLateralDelt = vtaper("lateral_delt", "lateral_deltoid"),
                            vtaperUpperChest = vtaper("upper_chest", "upper_chest"),
                            vtaperRearDelt = vtaper("rear_delt", "rear_deltoid")
                        )
                    )
                    result[id] = rowId
                    db.exerciseAliasDao().insertAll(
                        strings(e.optJSONArray("aliases")).map {
                            ExerciseAliasEntity(exerciseId = rowId, alias = it.lowercase())
                        }
                    )
                    db.exerciseMuscleDao().insertAll(
                        (primary.map { it to "primary" } + secondary.map { it to "secondary" })
                            .mapNotNull { (muscle, role) ->
                                muscleIds[muscle]?.let {
                                    ExerciseMuscleEntity(exerciseId = rowId, muscleId = it, role = role)
                                }
                            }
                    )
                    db.exerciseEquipmentDao().insertAll(
                        equipment.mapNotNull { eq ->
                            equipmentIds[eq]?.let {
                                ExerciseEquipmentEntity(exerciseId = rowId, equipmentId = it)
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to seed exercise file $file", e)
            }
        }
        Log.i(TAG, "Seeded ${result.size} exercises from ${ALL_EXERCISE_FILES.size} asset files")
        return result
    }

    private suspend fun seedSubstitutions(exerciseIds: Map<String, Long>) {
        try {
            val root = JSONObject(asset(SUBSTITUTIONS_FILE))
            val keys = root.keys()
            while (keys.hasNext()) {
                val original = keys.next()
                val originalRow = exerciseIds[original] ?: continue
                val substitutes = root.getJSONArray(original)
                for (i in 0 until substitutes.length()) {
                    val s = substitutes.getJSONObject(i)
                    val subRow = exerciseIds[s.getString("substitute_id")] ?: continue
                    db.exerciseSubstitutionDao().insert(
                        ExerciseSubstitutionEntity(
                            originalExerciseId = originalRow,
                            substituteExerciseId = subRow,
                            reason = s.optString("reason")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to seed substitutions", e)
        }
    }

    private fun rebuildSearchIndex() {
        db.openHelper.writableDatabase.execSQL("INSERT INTO exercise_fts(exercise_fts) VALUES('rebuild')")
    }

    companion object {
        private const val TAG = "ExerciseSeeder"
        const val SEED_VERSION = 2
        private const val PREFS_NAME = "gymcoach_seed"
        private const val KEY_SEED_VERSION = "exercise_seed_version"
        private const val SUBSTITUTIONS_FILE = "exercises/exercise_substitutions.json"
        private const val TAXONOMY_FILE = "exercises/muscle_taxonomy.json"
        private val ALL_EXERCISE_FILES = listOf(
            "exercises/dumbbell_bodyweight_bench_exercises.json",
            "exercises/bicep_exercises.json",
            "exercises/tricep_exercises.json",
            "exercises/leg_quad_exercises.json",
            "exercises/leg_hamstring_glute_exercises.json",
            "exercises/leg_glute_extra_exercises.json",
            "exercises/calf_exercises.json",
            "exercises/core_exercises.json",
            "exercises/core_exercises_batch2.json",
            "exercises/full_body_exercises.json",
            "exercises/back_extra_exercises.json",
            "exercises/back_heavy_exercises.json",
            "exercises/shoulder_extra_exercises.json",
            "exercises/chest_extra_exercises.json"
        )
    }
}
