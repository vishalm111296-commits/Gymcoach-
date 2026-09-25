package com.gymcoach.app.core.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EquipmentAvailabilityTest {

    private lateinit var equipmentAvailability: EquipmentAvailability

    @Before
    fun setUp() {
        equipmentAvailability = EquipmentAvailability()
    }

    @Test
    fun `getAvailableEquipment returns comprehensive sets for access tiers`() {
        val gymSet = equipmentAvailability.getAvailableEquipment("gym")
        assertTrue(gymSet.contains("barbell"))
        assertTrue(gymSet.contains("dumbbell"))
        assertTrue(gymSet.contains("cable"))
        assertTrue(gymSet.contains("leg press"))
        assertTrue(gymSet.contains("bodyweight"))
        assertTrue(gymSet.size >= 30)

        val homeSet = equipmentAvailability.getAvailableEquipment("home")
        assertTrue(homeSet.contains("dumbbell"))
        assertTrue(homeSet.contains("kettlebell"))
        assertTrue(homeSet.contains("pull-up bar"))
        assertTrue(homeSet.contains("resistance band"))
        assertTrue(homeSet.contains("bodyweight"))
        assertFalse("Home does not include heavy commercial leg press", homeSet.contains("leg press"))
        assertFalse("Home does not include barbell by default", homeSet.contains("barbell"))

        val customSet = equipmentAvailability.getAvailableEquipment("custom")
        assertEquals(setOf("bodyweight"), customSet)

        val unknownSet = equipmentAvailability.getAvailableEquipment("hotel_room")
        assertEquals(setOf("bodyweight"), unknownSet)

        // Case-insensitivity
        assertEquals(gymSet, equipmentAvailability.getAvailableEquipment("GYM"))
        assertEquals(homeSet, equipmentAvailability.getAvailableEquipment("Home"))
    }

    @Test
    fun `isAvailable validates item access and guarantees universal bodyweight availability`() {
        // Bodyweight is universally available across all tiers
        assertTrue(equipmentAvailability.isAvailable("bodyweight", "gym"))
        assertTrue(equipmentAvailability.isAvailable("bodyweight", "home"))
        assertTrue(equipmentAvailability.isAvailable("bodyweight", "custom"))
        assertTrue(equipmentAvailability.isAvailable("bodyweight", "unrecognized_location"))

        // Gym tier availability
        assertTrue(equipmentAvailability.isAvailable("barbell", "gym"))
        assertTrue(equipmentAvailability.isAvailable("smith machine", "gym"))
        assertFalse(equipmentAvailability.isAvailable("anti_gravity_chamber", "gym"))

        // Home tier availability
        assertTrue(equipmentAvailability.isAvailable("dumbbell", "home"))
        assertTrue(equipmentAvailability.isAvailable("doorway pull-up bar", "home"))
        assertFalse(equipmentAvailability.isAvailable("leg press", "home"))
        assertFalse(equipmentAvailability.isAvailable("cable crossover", "home"))

        // Custom tier availability
        assertFalse(equipmentAvailability.isAvailable("dumbbell", "custom"))
    }

    @Test
    fun `isLimited returns false for empty or blank equipment`() {
        assertFalse(equipmentAvailability.isLimited("", "home"))
        assertFalse(equipmentAvailability.isLimited("   ", "custom"))
        assertFalse(equipmentAvailability.isLimited("", "gym"))
    }

    @Test
    fun `isLimited returns false when all compound equipment tokens are satisfied`() {
        // All items available in gym
        assertFalse(equipmentAvailability.isLimited("barbell,bench", "gym"))
        assertFalse(equipmentAvailability.isLimited("dumbbell,incline bench", "gym"))
        assertFalse(equipmentAvailability.isLimited("cable,bench", "gym"))

        // Legacy plus separator support
        assertFalse(equipmentAvailability.isLimited("barbell+bench", "gym"))

        // All items available in home
        assertFalse(equipmentAvailability.isLimited("dumbbell,bench", "home"))
        assertFalse(equipmentAvailability.isLimited("kettlebell,floor", "home"))
    }

    @Test
    fun `isLimited returns true when any required token is missing in user tier`() {
        // Barbell is missing in home
        assertTrue(equipmentAvailability.isLimited("barbell", "home"))
        assertTrue(equipmentAvailability.isLimited("barbell,bench", "home"))
        assertTrue(equipmentAvailability.isLimited("bench+barbell", "home"))

        // Leg press is missing in home and custom
        assertTrue(equipmentAvailability.isLimited("leg press", "home"))
        assertTrue(equipmentAvailability.isLimited("leg press", "custom"))

        // Dumbbell is missing in custom
        assertTrue(equipmentAvailability.isLimited("dumbbell", "custom"))
    }

    @Test
    fun `isLimited ignores bodyweight tokens inside compound equipment`() {
        // In home, pull-up bar is available and bodyweight is ignored -> not limited
        assertFalse(equipmentAvailability.isLimited("bodyweight,pull-up bar", "home"))
        assertFalse(equipmentAvailability.isLimited("pull-up bar+bodyweight", "home"))

        // In custom, pull-up bar is missing -> limited
        assertTrue(equipmentAvailability.isLimited("bodyweight,pull-up bar", "custom"))

        // Pure bodyweight is never limited anywhere
        assertFalse(equipmentAvailability.isLimited("bodyweight", "custom"))
        assertFalse(equipmentAvailability.isLimited("bodyweight,bodyweight", "custom"))
    }

    @Test
    fun `isLimited handles whitespace, empty tokens and case variations cleanly`() {
        // Extra spaces and mixed case
        assertFalse(equipmentAvailability.isLimited("  Dumbbell ,  Bench  ", "home"))
        assertFalse(equipmentAvailability.isLimited("BARBELL + BENCH", "gym"))

        // Consecutive commas or trailing commas
        assertFalse(equipmentAvailability.isLimited("dumbbell,,bench,", "home"))
    }
}
