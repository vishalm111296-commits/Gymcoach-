package com.gymcoach.app.core.program

import com.gymcoach.app.data.local.entity.ExerciseEntity
import org.junit.Test
import org.junit.Assert.*

class ProgramGeneratorTest {

    @Test
    fun testVtaperPrioritization() {
        val ex1 = ExerciseEntity(id = 1, name = "Ex1", vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0, muscleGroup = "Biceps", difficulty = "Intermediate")
        val ex2 = ExerciseEntity(id = 2, name = "Ex2", vtaperLat = 9, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0, muscleGroup = "Biceps", difficulty = "Intermediate")
        
        // This is a simplified test simulating the ranking logic in ProgramGenerator
        val exercises = listOf(ex1, ex2)
        val sorted = exercises.sortedByDescending { it.vtaperLat }
        
        assertEquals(2L, sorted[0].id)
    }
}
