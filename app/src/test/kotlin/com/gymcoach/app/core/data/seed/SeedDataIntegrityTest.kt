package com.gymcoach.app.core.data.seed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0 structural contract for the exercise seed corpus.
 * 70 exercises (Chest 10, Shoulders 10, Back 11, Legs 18, Arms 11, Core 10)
 * per Agent-C audit. Future edits that break these invariants fail CI.
 */
class SeedDataIntegrityTest {

    private val allExercises: List<SeedExercise> =
        SeedDataChestShoulders.CHEST +
            SeedDataChestShoulders.SHOULDERS +
            SeedDataBack.BACK +
            SeedDataLegs.LEGS +
            SeedDataArmsCore.ARMS +
            SeedDataArmsCore.CORE

    private val validEquipmentTokens = setOf("Dumbbell", "Bodyweight", "Flat Bench")
    private val validMuscleNames = SeedReference.MUSCLES.map { it.name }.toSet()

    @Test
    fun `corpus contains exactly 70 exercises`() {
        assertEquals(70, allExercises.size)
    }

    @Test
    fun `category counts match audited breakdown`() {
        val byGroup = allExercises.groupBy { it.muscleGroup }.mapValues { it.value.size }
        assertEquals(10, byGroup["Chest"])
        assertEquals(10, byGroup["Shoulders"])
        assertEquals(11, byGroup["Back"])
        assertEquals(18, byGroup["Legs"])
        assertEquals(11, byGroup["Arms"])
        assertEquals(10, byGroup["Core"])
    }

    @Test
    fun `exercise names are globally unique`() {
        val dupes = allExercises.groupBy { it.name }.filterValues { it.size > 1 }
        assertTrue("Duplicates: ${dupes.keys}", dupes.isEmpty())
    }

    @Test
    fun `equipment uses only user-owned tokens`() {
        // Inventory = dumbbells + bodyweight + flat bench ONLY. Any other
        // token could let generators recommend impossible exercises.
        val bad = allExercises.filter { ex ->
            ex.equipment.split("+").map { it.trim() }.any { it !in validEquipmentTokens }
        }
        assertTrue("Bad equipment: ${bad.map { it.name }}", bad.isEmpty())
    }

    @Test
    fun `every muscle reference resolves to taxonomy`() {
        // Dangling refs are silently dropped at seed time - make it loud.
        val dangling = allExercises.flatMap { ex ->
            ex.muscles.filter { it.name !in validMuscleNames }.map { ex.name to it.name }
        }
        assertTrue("Dangling refs: $dangling", dangling.isEmpty())
    }

    @Test
    fun `every exercise has exactly one primary mover`() {
        val bad = allExercises.filter { ex ->
            ex.muscles.count { it.role == "primary" } != 1
        }
        assertTrue("Primary-count != 1: ${bad.map { it.name }}", bad.isEmpty())
    }
}
