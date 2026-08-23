package com.gymcoach.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * P0 regression: the workout lifecycle state machine added in v7.
 * These pin the parsing contract used by WorkoutRepositoryImpl mappers -
 * unknown/corrupt DB strings must degrade predictably, never crash.
 */
class WorkoutStatusTest {

    @Test
    fun `parses every canonical state name`() {
        WorkoutStatus.entries
            .filter { it != WorkoutStatus.NOT_STARTED } // NOT_STARTED is legacy-only, never returned
            .forEach { status ->
                assertEquals(status, WorkoutStatus.fromString(status.name, completed = false))
            }
    }

    @Test
    fun `null status falls back from completed flag`() {
        assertEquals(WorkoutStatus.COMPLETED, WorkoutStatus.fromString(null, completed = true))
        assertEquals(WorkoutStatus.ACTIVE, WorkoutStatus.fromString(null, completed = false))
    }

    @Test
    fun `unknown legacy string falls back from completed flag`() {
        // Pre-v7 rows have status='NOT_STARTED' default applied by ALTER TABLE,
        // so any non-canonical value is corruption - degrade, don't crash.
        assertEquals(WorkoutStatus.COMPLETED, WorkoutStatus.fromString("GARBAGE", completed = true))
        assertEquals(WorkoutStatus.ACTIVE, WorkoutStatus.fromString("GARBAGE", completed = false))
    }

    @Test
    fun `legacy NOT_STARTED rows map by completed flag`() {
        assertEquals(
            WorkoutStatus.ACTIVE,
            WorkoutStatus.fromString("NOT_STARTED", completed = false)
        )
        assertEquals(
            WorkoutStatus.COMPLETED,
            WorkoutStatus.fromString("NOT_STARTED", completed = true)
        )
    }
}
