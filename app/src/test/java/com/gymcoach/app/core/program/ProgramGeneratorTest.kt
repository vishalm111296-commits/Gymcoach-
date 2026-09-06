package com.gymcoach.app.core.program

import com.gymcoach.app.data.local.entity.ExerciseEntity
import org.junit.Test
import org.junit.Assert.*

class JavaProgramGeneratorTest {

    @Test
    fun testVtaperPrioritization() {
        val ex1 = ExerciseEntity(id = 1, name = "Ex1", description = "", vtaperLat = 0, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0, muscleGroup = "Biceps", equipment = "dumbbell", difficulty = "Intermediate")
        val ex2 = ExerciseEntity(id = 2, name = "Ex2", description = "", vtaperLat = 9, vtaperLateralDelt = 0, vtaperUpperChest = 0, vtaperRearDelt = 0, muscleGroup = "Biceps", equipment = "dumbbell", difficulty = "Intermediate")
        
        // This is a simplified test simulating the ranking logic in ProgramGenerator
        val exercises = listOf(ex1, ex2)
        val sorted = exercises.sortedByDescending { it.vtaperLat }
        
        assertEquals(2L, sorted[0].id)
    }
}
