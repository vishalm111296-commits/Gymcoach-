package com.gymcoach.app.core.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExerciseAssetJsonValidationTest {

    private fun resolveAssetsDir(): File {
        val path1 = File("app/src/main/assets/exercises")
        if (path1.exists() && path1.isDirectory) return path1
        val path2 = File("src/main/assets/exercises")
        if (path2.exists() && path2.isDirectory) return path2
        throw IllegalStateException("Assets directory exercises not found in expected paths")
    }

    @Test
    fun `all exercise asset files exist and parse valid json with unique IDs`() {
        val assetsDir = resolveAssetsDir()
        assertTrue("Assets directory must exist", assetsDir.exists() && assetsDir.isDirectory)

        val jsonFiles = assetsDir.listFiles { _, name ->
            name.endsWith(".json") && name != "exercise_substitutions.json" && name != "muscle_taxonomy.json"
        } ?: emptyArray()

        assertTrue("Exercise asset files should be found", jsonFiles.isNotEmpty())

        val idRegex = """"id"\s*:\s*"([^"]+)"""".toRegex()
        val nameRegex = """"name"\s*:\s*"([^"]+)"""".toRegex()
        val categoryRegex = """"category"\s*:\s*"([^"]+)"""".toRegex()

        val seenIds = mutableMapOf<String, String>()
        var totalExercises = 0

        for (file in jsonFiles) {
            val content = file.readText()
            assertTrue("File ${file.name} must not be empty", content.isNotBlank())

            val idMatches = idRegex.findAll(content).map { it.groupValues[1] }.toList()
            val nameMatches = nameRegex.findAll(content).map { it.groupValues[1] }.toList()
            val categoryMatches = categoryRegex.findAll(content).map { it.groupValues[1] }.toList()

            assertTrue("File ${file.name} must contain exercises", idMatches.isNotEmpty())
            assertEquals("File ${file.name} id and name counts should match", idMatches.size, nameMatches.size)
            assertEquals("File ${file.name} id and category counts should match", idMatches.size, categoryMatches.size)

            for (id in idMatches) {
                if (id in seenIds) {
                    throw AssertionError("Duplicate exercise ID '$id' found in ${file.name} (first seen in ${seenIds[id]})")
                }
                seenIds[id] = file.name
                totalExercises++
            }
        }

        assertEquals("Total exercise count should equal unique ID count", totalExercises, seenIds.size)
        assertTrue("Should have at least 100 seeded exercises", seenIds.size >= 100)
    }
}
