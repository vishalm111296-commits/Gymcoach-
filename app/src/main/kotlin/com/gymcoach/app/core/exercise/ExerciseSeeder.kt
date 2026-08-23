package com.gymcoach.app.core.exercise

import android.content.Context
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

/**
 * Seeds the curated dumbbell/bodyweight/bench exercise library from JSON assets.
 *
 * Runs once per SEED_VERSION: first launch, or again whenever the bundled
 * catalog is upgraded (bump SEED_VERSION and re-ship the assets).
 * Version tracked in SharedPreferences; all inserts happen inside one transaction.
 */
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

    /** muscle_taxonomy.json -> muscles table (groups + subdivisions, self-referencing parent). */
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
                if (sub.getString("id") in ids) continue // rear_deltoid shared by back+shoulders
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

    /** Home-equipment set available to this curated library. */
    private suspend fun seedEquipment(): Map<String, Long> =
        listOf(
            "dumbbell" to "Dumbbell",
            "bench" to "Flat Bench",
            "bodyweight" to "Bodyweight"
        ).associate { (name, display) ->
            name to db.equipmentDao().insert(
                EquipmentEntity(name = name, displayName = display, category = name)
            )
        }

    /** Exercises + aliases + muscle relations + equipment relations. */
    private suspend fun seedExercises(
        muscleIds: Map<String, Long>,
        equipmentIds: Map<String, Long>
    ): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        val root = JSONArray(asset(EXERCISES_FILE))
        for (i in 0 until root.length()) {
            val e = root.getJSONObject(i)
            val primary = strings(e.optJSONArray("primary_muscles"))
            val secondary = strings(e.optJSONArray("secondary_muscles"))
            val scores = e.optJSONObject("vtaper_scores")
            val relevance = e.optInt("v_taper_relevance", 0)

            // Granular V-taper score wins; else fall back to overall relevance if the
            // muscle is a primary mover.
            fun vtaper(scoreKey: String, muscle: String): Int =
                scores?.optInt(scoreKey, 0)?.takeIf { it > 0 }
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
            result[e.getString("id")] = rowId

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
        return result
    }

    /** exercise_substitutions.json -> exercise_substitutions rows (string IDs resolved to row IDs). */
    private suspend fun seedSubstitutions(exerciseIds: Map<String, Long>) {
        val root = JSONObject(asset(SUBSTITUTIONS_FILE))
        val keys = root.keys()
        while (keys.hasNext()) {
            val original = keys.next()
            val originalRow = exerciseIds[original] ?: continue
            val substitutes = root.getJSONArray(original)
            for (i in 0 until substitutes.length()) {
                val s = substitutes.getJSONObject(i)
                val substituteRow = exerciseIds[s.getString("substitute_id")] ?: continue
                db.exerciseSubstitutionDao().insert(
                    ExerciseSubstitutionEntity(
                        originalExerciseId = originalRow,
                        substituteExerciseId = substituteRow,
                        reason = s.optString("reason")
                    )
                )
            }
        }
    }

    /** External-content FTS4 index must be rebuilt after bulk exercise inserts. */
    private fun rebuildSearchIndex() {
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO exercise_fts(exercise_fts) VALUES('rebuild')"
        )
    }

    companion object {
        /** Bump when shipped exercise assets change; triggers a re-seed. */
        const val SEED_VERSION = 1

        private const val PREFS_NAME = "gymcoach_seed"
        private const val KEY_SEED_VERSION = "exercise_seed_version"
        private const val EXERCISES_FILE = "exercises/dumbbell_bodyweight_bench_exercises.json"
        private const val SUBSTITUTIONS_FILE = "exercises/exercise_substitutions.json"
        private const val TAXONOMY_FILE = "exercises/muscle_taxonomy.json"
    }
}
