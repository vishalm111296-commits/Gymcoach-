package com.gymcoach.app.core.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Validates all exercise JSON asset files semantically without relying on Android org.json stubs.
 */
class ExerciseAssetJsonValidationTest {

    private fun resolveAssetsDir(): File {
        val path1 = File("app/src/main/assets/exercises")
        if (path1.exists() && path1.isDirectory) return path1
        val path2 = File("src/main/assets/exercises")
        if (path2.exists() && path2.isDirectory) return path2
        throw IllegalStateException("Assets directory exercises not found in expected paths")
    }

    @Test
    fun `all exercise assets, taxonomy, and substitutions are semantically valid and cross-referenced`() {
        val assetsDir = resolveAssetsDir()
        val taxonomyFile = File(assetsDir, "muscle_taxonomy.json")
        val substitutionsFile = File(assetsDir, "exercise_substitutions.json")

        assertTrue("muscle_taxonomy.json must exist", taxonomyFile.exists())
        assertTrue("exercise_substitutions.json must exist", substitutionsFile.exists())

        // 1. Validate taxonomy
        val taxonomyContent = taxonomyFile.readText()
        val validMuscleIds = parseTaxonomyMuscleIds(taxonomyContent)
        assertTrue("Taxonomy must contain muscle IDs", validMuscleIds.isNotEmpty())

        // 2. Validate exercise files
        val jsonFiles = assetsDir.listFiles { _, name ->
            name.endsWith(".json") && name != "exercise_substitutions.json" && name != "muscle_taxonomy.json"
        } ?: emptyArray()

        assertTrue("Exercise asset files must be present", jsonFiles.isNotEmpty())

        val validCategories = setOf("chest", "back", "shoulders", "biceps", "triceps", "legs", "core", "full_body", "arms", "calves")
        val validDifficulties = setOf("beginner", "intermediate", "advanced")

        val allExerciseIds = mutableMapOf<String, String>()
        var totalExercises = 0

        for (file in jsonFiles) {
            val content = file.readText()
            assertTrue("File ${file.name} must not be empty", content.isNotBlank())

            val exercises = parseExerciseList(content)
            assertTrue("File ${file.name} must contain exercises", exercises.isNotEmpty())

            for (ex in exercises) {
                val id = ex["id"] as? String
                val name = ex["name"] as? String
                val category = (ex["category"] as? String)?.lowercase()
                val difficulty = (ex["difficulty"] as? String)?.lowercase()

                assertNotNull("Exercise in ${file.name} must have ID", id)
                assertTrue("Exercise $id in ${file.name} name cannot be blank", !name.isNullOrBlank())
                assertTrue("Exercise $id in ${file.name} invalid category '$category'", category.isNullOrBlank() || category in validCategories)
                assertTrue("Exercise $id in ${file.name} invalid difficulty '$difficulty'", difficulty.isNullOrBlank() || difficulty in validDifficulties)

                if (id!! in allExerciseIds) {
                    throw AssertionError("Duplicate exercise ID '$id' found in ${file.name} (first seen in ${allExerciseIds[id]})")
                }
                allExerciseIds[id] = file.name
                totalExercises++

                // Validate muscles against taxonomy
                val primaryMuscles = ex["primary_muscles"] as? List<*> ?: emptyList<Any>()
                val secondaryMuscles = ex["secondary_muscles"] as? List<*> ?: emptyList<Any>()

                for (pm in primaryMuscles) {
                    val m = pm as? String
                    assertTrue("Primary muscle '$m' in exercise '$id' (${file.name}) not found in muscle_taxonomy.json", m in validMuscleIds)
                }
                for (sm in secondaryMuscles) {
                    val m = sm as? String
                    assertTrue("Secondary muscle '$m' in exercise '$id' (${file.name}) not found in muscle_taxonomy.json", m in validMuscleIds)
                }
            }
        }

        assertEquals("Total exercise count should equal unique ID count", totalExercises, allExerciseIds.size)
        assertTrue("Should have at least 100 seeded exercises", allExerciseIds.size >= 100)

        // 3. Validate substitutions cross-references
        val substitutionsContent = substitutionsFile.readText()
        val substitutionRefs = parseSubstitutionReferences(substitutionsContent)

        for ((origId, subIds) in substitutionRefs) {
            assertTrue("Substitution original ID '$origId' not found in exercise database", origId in allExerciseIds)
            for (subId in subIds) {
                assertTrue("Substitution target ID '$subId' for '$origId' not found in exercise database", subId in allExerciseIds)
            }
        }
    }

    private fun parseTaxonomyMuscleIds(json: String): Set<String> {
        val ids = mutableSetOf<String>()
        val idRegex = """"id"\s*:\s*"([^"]+)"""".toRegex()
        idRegex.findAll(json).forEach { ids.add(it.groupValues[1]) }
        return ids
    }

    private fun parseExerciseList(json: String): List<Map<String, Any?>> {
        val exercises = mutableListOf<Map<String, Any?>>()

        val idRegex = """"id"\s*:\s*"([^"]+)"""".toRegex()
        val nameRegex = """"name"\s*:\s*"([^"]+)"""".toRegex()
        val categoryRegex = """"category"\s*:\s*"([^"]+)"""".toRegex()
        val difficultyRegex = """"difficulty"\s*:\s*"([^"]+)"""".toRegex()
        val primaryRegex = """"primary_muscles"\s*:\s*\[([^\]]*)\]""".toRegex()
        val secondaryRegex = """"secondary_muscles"\s*:\s*\[([^\]]*)\]""".toRegex()
        val strValueRegex = """"([^"]+)"""".toRegex()

        val allIds = idRegex.findAll(json).map { it.groupValues[1] }.toList()

        for (exId in allIds) {
            val exBlockMatch = """\{[^{}]*"id"\s*:\s*"$exId"[^{}]*\}""".toRegex(RegexOption.DOT_MATCHES_ALL).find(json)
            val block = exBlockMatch?.value ?: ""

            val name = nameRegex.find(block)?.groupValues?.get(1) ?: exId
            val category = categoryRegex.find(block)?.groupValues?.get(1) ?: ""
            val difficulty = difficultyRegex.find(block)?.groupValues?.get(1) ?: ""

            val pmStr = primaryRegex.find(block)?.groupValues?.get(1) ?: ""
            val primaryMuscles = strValueRegex.findAll(pmStr).map { it.groupValues[1] }.toList()

            val smStr = secondaryRegex.find(block)?.groupValues?.get(1) ?: ""
            val secondaryMuscles = strValueRegex.findAll(smStr).map { it.groupValues[1] }.toList()

            exercises.add(
                mapOf(
                    "id" to exId,
                    "name" to name,
                    "category" to category,
                    "difficulty" to difficulty,
                    "primary_muscles" to primaryMuscles,
                    "secondary_muscles" to secondaryMuscles
                )
            )
        }
        return exercises
    }

    private fun parseSubstitutionReferences(json: String): Map<String, List<String>> {
        val refs = mutableMapOf<String, List<String>>()
        val keyRegex = """"([a-zA-Z0-9_]+)"\s*:\s*\[""".toRegex()
        val subIdRegex = """"substitute_id"\s*:\s*"([^"]+)"""".toRegex()

        val blocks = json.split("],")
        for (block in blocks) {
            val origId = keyRegex.find(block)?.groupValues?.get(1) ?: continue
            val subIds = subIdRegex.findAll(block).map { it.groupValues[1] }.toList()
            refs[origId] = subIds
        }
        return refs
    }
}
