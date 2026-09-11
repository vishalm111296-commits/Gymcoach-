package com.gymcoach.app.core.animation

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val animationById = ConcurrentHashMap<String, ExerciseAnimationDefinition>()
    private val animationByName = ConcurrentHashMap<String, ExerciseAnimationDefinition>()
    @Volatile private var isLoaded = false

    suspend fun getAnimation(exerciseId: String = "", exerciseName: String = ""): ExerciseAnimationDefinition? {
        ensureLoaded()
        val normalizedId = exerciseId.trim().lowercase()
        if (normalizedId.isNotBlank()) {
            animationById[normalizedId]?.let { return it }
        }

        val normalizedName = normalize(exerciseName)
        if (normalizedName.isNotBlank()) {
            animationByName[normalizedName]?.let { return it }
            // Try fuzzy/substring match
            for ((key, def) in animationByName) {
                if (key.contains(normalizedName) || normalizedName.contains(key)) {
                    return def
                }
            }
        }
        return null
    }

    suspend fun getAllAnimations(): List<ExerciseAnimationDefinition> {
        ensureLoaded()
        return animationById.values.toList()
    }

    suspend fun hasAnimation(exerciseName: String): Boolean {
        return getAnimation(exerciseName = exerciseName) != null
    }

    private suspend fun ensureLoaded() {
        if (isLoaded) return
        withContext(Dispatchers.IO) {
            synchronized(this@AnimationRepository) {
                if (isLoaded) return@withContext
                loadFromAssets()
                isLoaded = true
            }
        }
    }

    fun loadFromAssets() {
        try {
            val assetManager = context.assets
            val files = assetManager.list("animations") ?: emptyArray()
            for (file in files) {
                if (file.endsWith(".json")) {
                    val content = assetManager.open("animations/$file").bufferedReader().use { it.readText() }
                    val defs = AnimationParser.parseList(content)
                    for (def in defs) {
                        registerDefinition(def)
                    }
                }
            }
        } catch (e: Exception) {
            // Keep app operational offline even if an asset is temporarily inaccessible
        }
    }

    fun registerDefinition(def: ExerciseAnimationDefinition) {
        val idKey = def.exerciseId.trim().lowercase()
        animationById[idKey] = def
        val nameKey = normalize(def.exerciseName)
        animationByName[nameKey] = def
    }

    private fun normalize(str: String): String {
        return str.trim().lowercase()
            .replace("-", " ")
            .replace("_", " ")
            .replace(Regex("[^a-z0-9 ]"), "")
            .replace(Regex("\\s+"), " ")
    }
}
