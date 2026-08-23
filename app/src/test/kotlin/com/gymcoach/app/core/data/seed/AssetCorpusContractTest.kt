package com.gymcoach.app.core.data.seed

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * P0 contract for the LIVE exercise corpus - the JSON assets that
 * core.exercise.ExerciseSeeder actually ships and inserts.
 *
 * The earlier SeedDataIntegrityTest/SeedHonestyTest validated the legacy
 * Kotlin seed objects under core.data.seed, which are dead code on main;
 * these tests target the real data so an audit edit can never drift away
 * from what users actually get.
 *
 * NOTE on counts: the exact corpus total is printed by `corpus loads and
 * has audited magnitude` so CI logs record it; assertions pin a floor
 * rather than a fabricated constant.
 */
class AssetCorpusContractTest {

    private fun assetDir(): File {
        var dir = File("").absoluteFile
        repeat(6) {
            val candidate = File(dir, "src/main/assets/exercises")
            if (candidate.isDirectory) return candidate
            dir = dir.parentFile ?: return@repeat
        }
        error("assets/exercises not found from working dir ${File("").absolutePath}")
    }

    private fun load(name: String) = JSONObject(File(assetDir(), name).readText())
    private fun loadArray(name: String) = JSONArray(File(assetDir(), name).readText())

    private val exercises: List<JSONObject> by lazy {
        loadArray("dumbbell_bodyweight_bench_exercises.json").let { arr ->
            (0 until arr.length()).map { arr.getJSONObject(it) }
        }
    }
    private val taxonomy: JSONObject by lazy { load("muscle_taxonomy.json") }

    private fun validMuscleIds(): Set<String> {
        val ids = mutableSetOf<String>()
        val groups = taxonomy.getJSONArray("muscles")
        for (g in 0 until groups.length()) {
            val group = groups.getJSONObject(g)
            ids += group.getString("id")
            val subs = group.optJSONArray("subdivisions") ?: continue
            for (s in 0 until subs.length()) ids += subs.getJSONObject(s).getString("id")
        }
        return ids
    }

    // ---- Corpus integrity ----

    @Test
    fun `corpus loads and has audited magnitude`() {
        assertTrue(
            "Corpus suspiciously small: ${exercises.size}",
            exercises.size >= 60
        )
        println("[asset-corpus] actual exercise count = ${exercises.size}")
    }

    @Test
    fun `exercise names are globally unique`() {
        val dupes = exercises.groupBy { it.getString("name") }.filterValues { it.size > 1 }
        assertEquals("Duplicate names: ${dupes.keys}", emptyMap<String, List<JSONObject>>(), dupes)
    }

    @Test
    fun `exercise ids are present and unique`() {
        val ids = exercises.map { it.getString("id") }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `equipment stays inside user inventory`() {
        val owned = setOf("dumbbell", "bench", "bodyweight")
        val bad = exercises.flatMap { ex ->
            ex.optJSONArray("equipment")?.let { a ->
                (0 until a.length()).map { a.getJSONObject(it).getString("name") }
                    .filter { it.lowercase() !in owned }
                    .map { ex.getString("name") to it }
            } ?: emptyList()
        }
        assertTrue("Non-inventory equipment: $bad", bad.isEmpty())
    }

    @Test
    fun `every muscle reference resolves to taxonomy`() {
        val valid = validMuscleIds()
        val dangling = exercises.flatMap { ex ->
            val roles = listOfNotNull(
                ex.optJSONArray("primary_muscles"),
                ex.optJSONArray("secondary_muscles")
            )
            roles.flatMap { a -> (0 until a.length()).map { a.getString(it) } }
                .filter { it !in valid }
                .map { ex.getString("name") to it }
        }
        assertTrue("Dangling muscle refs: $dangling", dangling.isEmpty())
    }

    @Test
    fun `every exercise declares exactly one or zero primary movers`() {
        val multi = exercises.filter {
            (it.optJSONArray("primary_muscles")?.length() ?: 0) > 1
        }
        assertTrue("Multiple primaries: ${multi.map { it.getString("name") }}", multi.isEmpty())
    }

    // ---- Honesty ----

    @Test
    fun `presses never claim lat contribution`() {
        val offenders = exercises.filter { ex ->
            val pattern = ex.optString("movement_pattern")
            pattern.contains("push") &&
                (ex.optJSONObject("vtaper_scores")?.optInt("lat", 0) ?: 0) > 0
        }
        assertTrue("Lat score on press: ${offenders.map { it.getString("name") }}", offenders.isEmpty())
    }

    @Test
    fun `rep ranges are machine-parseable`() {
        val regex = Regex("^\\d+(-\\d+)?(s)?( (hold|per side|steps))?$")
        val bad = exercises.mapNotNull { ex ->
            val range = ex.optString("rep_range").trim()
            if (range.isEmpty() || !regex.matches(range)) ex.getString("name") to range else null
        }
        assertTrue("Unparseable rep ranges: $bad", bad.isEmpty())
    }

    @Test
    fun `movement patterns are declared`() {
        val blank = exercises.filter { it.optString("movement_pattern").isBlank() }
        assertTrue("Blank movement patterns: ${blank.map { it.getString("name") }}", blank.isEmpty())
    }

    // ---- Substitutions graph ----

    @Test
    fun `substitution targets resolve to real exercise ids`() {
        val ids = exercises.map { it.getString("id") }.toSet()
        val subs = load("exercise_substitutions.json")
        val dangling = mutableListOf<Pair<String, String>>()
        val keys = subs.keys()
        while (keys.hasNext()) {
            val original = keys.next()
            if (original !in ids) dangling += original to "<original>"
            val arr = subs.getJSONArray(original)
            for (i in 0 until arr.length()) {
                val subId = arr.getJSONObject(i).getString("substitute_id")
                if (subId !in ids) dangling += original to subId
            }
        }
        assertTrue("Dangling substitutions: $dangling", dangling.isEmpty())
    }
}
