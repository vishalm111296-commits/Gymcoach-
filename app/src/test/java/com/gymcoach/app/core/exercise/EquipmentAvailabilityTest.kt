package com.gymcoach.app.core.exercise

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EquipmentAvailabilityTest {

    private val availability = EquipmentAvailability()

    @Test
    fun dumbbellBodyweightProfile_excludesUnsupportedEquipment() {
        assertTrue(availability.isAvailable("dumbbell", "dumbbell_bodyweight"))
        assertTrue(availability.isAvailable("bodyweight", "dumbbell_bodyweight"))
        assertTrue(availability.isAvailable("floor", "dumbbell_bodyweight"))
        assertFalse(availability.isAvailable("bench", "dumbbell_bodyweight"))
        assertFalse(availability.isAvailable("resistance band", "dumbbell_bodyweight"))
        assertFalse(availability.isAvailable("pull-up bar", "dumbbell_bodyweight"))
        assertFalse(availability.isAvailable("kettlebell", "dumbbell_bodyweight"))
    }

    @Test
    fun compoundRequirements_requireEveryItem() {
        assertFalse(availability.isLimited("dumbbell, bodyweight", "dumbbell_bodyweight"))
        assertTrue(availability.isLimited("dumbbell, bench", "dumbbell_bodyweight"))
        assertTrue(availability.isLimited("dumbbell + resistance band", "dumbbell_bodyweight"))
    }

    @Test
    fun genericHomeProfile_remainsBroaderThanStrictProfile() {
        assertTrue(availability.isAvailable("resistance band", "home"))
        assertTrue(availability.isAvailable("pull-up bar", "home"))
        assertFalse(availability.isAvailable("bench", "home"))
    }
}
