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
        val seenNames = mutableMapOf<String, String>()
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

            for (name in nameMatches) {
                if (name in seenNames) {
                    throw AssertionError("Duplicate exercise name '$name' found in ${file.name} (first seen in ${seenNames[name]})")
                }
                seenNames[name] = file.name
            }
        }

        assertEquals("Total exercise count should equal unique ID count", totalExercises, seenIds.size)
        assertEquals("Total exercise count should equal unique name count", totalExercises, seenNames.size)
        assertTrue("Should have at least 100 seeded exercises", seenIds.size >= 100)
    }

    @Test
    fun `exercise substitutions json has valid schema and referential integrity against exercise IDs`() {
        val assetsDir = resolveAssetsDir()
        val subsFile = File(assetsDir, "exercise_substitutions.json")
        assertTrue("exercise_substitutions.json must exist", subsFile.exists())

        val jsonFiles = assetsDir.listFiles { _, name ->
            name.endsWith(".json") && name != "exercise_substitutions.json" && name != "muscle_taxonomy.json"
        } ?: emptyArray()

        val idRegex = """"id"\s*:\s*"([^"]+)"""".toRegex()
        val validExerciseIds = mutableSetOf<String>()
        for (file in jsonFiles) {
            val content = file.readText()
            validExerciseIds.addAll(idRegex.findAll(content).map { it.groupValues[1] })
        }
        assertTrue("Valid exercise IDs must be populated", validExerciseIds.isNotEmpty())

        val content = subsFile.readText()
        assertTrue("Substitutions content must not be blank", content.isNotBlank())
        val json = org.json.JSONObject(content)
        val keys = json.keys()
        var keyCount = 0

        while (keys.hasNext()) {
            val exerciseId = keys.next()
            keyCount++
            assertTrue("Substitution key '$exerciseId' must exist in exercise catalog", validExerciseIds.contains(exerciseId))

            val subsArray = json.getJSONArray(exerciseId)
            assertTrue("Substitution list for '$exerciseId' must not be empty", subsArray.length() > 0)

            val seenSubstituteIds = mutableSetOf<String>()
            for (i in 0 until subsArray.length()) {
                val subObj = subsArray.getJSONObject(i)
                assertTrue("Substitution entry must have substitute_id", subObj.has("substitute_id"))
                assertTrue("Substitution entry must have reason", subObj.has("reason"))

                val substituteId = subObj.getString("substitute_id")
                val reason = subObj.getString("reason")

                assertTrue("substitute_id must not be blank in '$exerciseId'", substituteId.isNotBlank())
                assertTrue("reason must not be blank in '$exerciseId' -> '$substituteId'", reason.isNotBlank())
                assertTrue("Exercise '$exerciseId' must not substitute itself", substituteId != exerciseId)
                assertTrue("substitute_id '$substituteId' must exist in exercise catalog", validExerciseIds.contains(substituteId))
                assertTrue("Duplicate substitute '$substituteId' found for '$exerciseId'", seenSubstituteIds.add(substituteId))
            }
        }

        assertEquals("Must validate exactly 28 seeded substitution mappings", 28, keyCount)
    }

    @Test
    fun `muscle taxonomy json has valid schema and unique subdivision IDs`() {
        val assetsDir = resolveAssetsDir()
        val taxFile = File(assetsDir, "muscle_taxonomy.json")
        assertTrue("muscle_taxonomy.json must exist", taxFile.exists())

        val content = taxFile.readText()
        assertTrue("muscle_taxonomy.json must not be blank", content.isNotBlank())
        val json = org.json.JSONObject(content)

        assertTrue("Must contain 'muscles' array", json.has("muscles"))
        val muscles = json.getJSONArray("muscles")
        assertTrue("Muscles array must not be empty", muscles.length() > 0)

        val seenMuscleIds = mutableSetOf<String>()
        val seenMuscleNames = mutableSetOf<String>()
        val seenSubdivisionIds = mutableSetOf<String>()

        for (i in 0 until muscles.length()) {
            val m = muscles.getJSONObject(i)
            val muscleId = m.getString("id")
            val muscleName = m.getString("name")

            assertTrue("Muscle ID must not be blank", muscleId.isNotBlank())
            assertTrue("Muscle name must not be blank", muscleName.isNotBlank())
            assertTrue("Duplicate muscle ID '$muscleId'", seenMuscleIds.add(muscleId))
            assertTrue("Duplicate muscle name '$muscleName'", seenMuscleNames.add(muscleName))

            if (m.has("subdivisions")) {
                val subs = m.getJSONArray("subdivisions")
                for (j in 0 until subs.length()) {
                    val s = subs.getJSONObject(j)
                    val subId = s.getString("id")
                    val subName = s.getString("name")

                    assertTrue("Subdivision ID must not be blank in $muscleId", subId.isNotBlank())
                    assertTrue("Subdivision name must not be blank in $muscleId", subName.isNotBlank())
                    assertTrue("Globally duplicate subdivision ID '$subId'", seenSubdivisionIds.add(subId))
                }
            }
        }

        assertTrue("Should have at least 5 muscle groups", seenMuscleIds.size >= 5)
        assertTrue("Should have at least 15 subdivisions", seenSubdivisionIds.size >= 15)
    }
}
