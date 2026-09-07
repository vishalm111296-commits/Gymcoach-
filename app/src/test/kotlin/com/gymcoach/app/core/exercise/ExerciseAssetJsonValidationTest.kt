package com.gymcoach.app.core.exercise

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExerciseAssetJsonValidationTest {

    private val assetDir: File = File("app/src/main/assets/exercises").takeIf { it.exists() }
        ?: File("src/main/assets/exercises")

    private val exerciseFiles = listOf(
        "dumbbell_bodyweight_bench_exercises.json",
        "bicep_exercises.json",
        "tricep_exercises.json",
        "leg_quad_exercises.json",
        "leg_hamstring_glute_exercises.json",
        "leg_glute_extra_exercises.json",
        "calf_exercises.json",
        "core_exercises.json",
        "core_exercises_batch2.json",
        "full_body_exercises.json",
        "back_extra_exercises.json",
        "back_heavy_exercises.json",
        "shoulder_extra_exercises.json",
        "chest_extra_exercises.json"
    )

    @Test
    fun `muscle taxonomy file exists and contains muscles`() {
        val taxonomyFile = File(assetDir, "muscle_taxonomy.json")
        assertTrue("Taxonomy file must exist at ${taxonomyFile.absolutePath}", taxonomyFile.exists())
        val text = taxonomyFile.readText()
        assertTrue("Taxonomy file must contain 'muscles'", text.contains("\"muscles\""))
    }

    @Test
    fun `all exercise asset files exist and contain unique exercise IDs`() {
        val seenIds = mutableSetOf<String>()
        val idRegex = Regex(""""id"\s*:\s*"([^"]+)"""")
        var totalExercises = 0

        for (fileName in exerciseFiles) {
            val file = File(assetDir, fileName)
            assertTrue("Asset file $fileName must exist", file.exists())

            val text = file.readText()
            val matches = idRegex.findAll(text).map { it.groupValues[1] }.toList()
            assertTrue("File $fileName must contain exercises", matches.isNotEmpty())

            for (id in matches) {
                assertTrue("Duplicate exercise ID '$id' found in $fileName", seenIds.add(id))
                totalExercises++
            }
        }

        assertTrue("Expected at least 50 seeded exercises, found $totalExercises", totalExercises >= 50)
    }
}
