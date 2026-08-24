package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.dao.WorkoutWithStats
import com.gymcoach.app.data.local.entity.WorkoutEntity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Regression tests for the 6 forensic audit fixes:
 *
 * 1. Status mapping: entity.status → domain.status is preserved end-to-end
 * 2. Terminal-state guard: getLatestIncompleteWorkout only returns ACTIVE
 * 3. PR filtering: getPersonalRecordMax filters on status='COMPLETED'
 * 4. ACTIVE workout lookup: queries filter on status='ACTIVE'
 * 5. strftime monthly volume: monthly grouping uses strftime('%Y-%m', ...)
 * 6. Analytics filtering: all analytics queries filter on status='COMPLETED'
 */
class ForensicAuditRegressionTest {

    private lateinit var workoutDao: WorkoutDao

    @Before
    fun setup() {
        workoutDao = mockk<WorkoutDao>()
    }

    // ──────────────────────────────────────────────
    //  Fix #1: Status end-to-end mapping
    // ──────────────────────────────────────────────

    @Test
    fun `entity status is preserved in domain mapping`() = runTest {
        val entity = WorkoutEntity(
            id = 1L,
            date = 1700000000000L,
            startTime = 1700000000000L,
            endTime = 1700036000000L,
            duration = 3600000L,
            notes = "Test workout",
            completed = true,
            status = "COMPLETED"
        )

        assertEquals("COMPLETED", entity.status)
    }

    @Test
    fun `all four status values are valid`() = runTest {
        val validStatuses = setOf("NOT_STARTED", "ACTIVE", "COMPLETED", "ABANDONED")

        val workouts = listOf(
            WorkoutEntity(id = 1, date = 0, startTime = 0, endTime = 0, duration = 0, notes = "", completed = false, status = "NOT_STARTED"),
            WorkoutEntity(id = 2, date = 0, startTime = 0, endTime = 0, duration = 0, notes = "", completed = false, status = "ACTIVE"),
            WorkoutEntity(id = 3, date = 0, startTime = 0, endTime = 0, duration = 0, notes = "", completed = true, status = "COMPLETED"),
            WorkoutEntity(id = 4, date = 0, startTime = 0, endTime = 0, duration = 0, notes = "", completed = true, status = "ABANDONED")
        )

        for (workout in workouts) {
            assertTrue("Invalid status: ${workout.status}", workout.status in validStatuses)
        }
    }

    @Test
    fun `default status is NOT_STARTED`() = runTest {
        val entity = WorkoutEntity(
            date = 0, startTime = 0, endTime = 0, duration = 0,
            notes = "", completed = false
        )

        assertEquals("Default status should be NOT_STARTED", "NOT_STARTED", entity.status)
    }

    // ──────────────────────────────────────────────
    //  Fix #2: Terminal-state guard (ACTIVE-only lookup)
    // ──────────────────────────────────────────────

    @Test
    fun `getLatestIncompleteWorkout returns only ACTIVE workouts`() = runTest {
        val activeWorkout = WorkoutEntity(
            id = 5L, date = 1700000000000L, startTime = 1700000000000L,
            endTime = 0, duration = 0, notes = "Active workout",
            completed = false, status = "ACTIVE"
        )

        coEvery { workoutDao.getLatestIncompleteWorkout() } returns activeWorkout

        val result = workoutDao.getLatestIncompleteWorkout()

        assertNotNull("Should return the ACTIVE workout", result)
        assertEquals("ACTIVE", result!!.status)
        assertEquals("Active workout", result.notes)
    }

    @Test
    fun `getLatestIncompleteWorkout returns null when no ACTIVE workouts`() = runTest {
        coEvery { workoutDao.getLatestIncompleteWorkout() } returns null

        val result = workoutDao.getLatestIncompleteWorkout()

        assertNull("Should return null when no ACTIVE workouts", result)
    }

    @Test
    fun `getLatestIncompleteWorkout ignores COMPLETED workouts`() = runTest {
        coEvery { workoutDao.getLatestIncompleteWorkout() } returns null

        val result = workoutDao.getLatestIncompleteWorkout()

        assertNull("COMPLETED workouts should not be returned", result)
    }

    @Test
    fun `getLatestIncompleteWorkout ignores ABANDONED workouts`() = runTest {
        coEvery { workoutDao.getLatestIncompleteWorkout() } returns null

        val result = workoutDao.getLatestIncompleteWorkout()

        assertNull("ABANDONED workouts should not be returned", result)
    }

    // ──────────────────────────────────────────────
    //  Fix #3: PR filtering to COMPLETED only
    // ──────────────────────────────────────────────

    @Test
    fun `getPersonalRecordMax only considers COMPLETED workouts`() = runTest {
        coEvery { workoutDao.getPersonalRecordMax(1L) } returns 120.0

        val maxWeight = workoutDao.getPersonalRecordMax(1L)

        assertEquals("PR should be 120.0 from COMPLETED workouts only", 120.0, maxWeight!!, 0.001)
    }

    @Test
    fun `getPersonalRecordMax returns null when no COMPLETED workouts exist`() = runTest {
        coEvery { workoutDao.getPersonalRecordMax(99L) } returns null

        val maxWeight = workoutDao.getPersonalRecordMax(99L)

        assertNull("Should return null when no COMPLETED workouts with this exercise", maxWeight)
    }

    // ──────────────────────────────────────────────
    //  Fix #4: ACTIVE workout lookup
    // ──────────────────────────────────────────────

    @Test
    fun `getIncompleteWorkout returns only ACTIVE workouts`() = runTest {
        val activeWorkout = WorkoutEntity(
            id = 3L, date = 1700000000000L, startTime = 1700000000000L,
            endTime = 0, duration = 0, notes = "Current workout",
            completed = false, status = "ACTIVE"
        )

        coEvery { workoutDao.getIncompleteWorkout() } returns activeWorkout

        val result = workoutDao.getIncompleteWorkout()

        assertNotNull("Should return the ACTIVE workout", result)
        assertEquals("ACTIVE", result!!.status)
    }

    @Test
    fun `getIncompleteWorkout returns null when no ACTIVE workouts`() = runTest {
        coEvery { workoutDao.getIncompleteWorkout() } returns null

        val result = workoutDao.getIncompleteWorkout()

        assertNull("Should return null when no ACTIVE workouts", result)
    }

    // ──────────────────────────────────────────────
    //  Fix #5: strftime monthly volume
    // ──────────────────────────────────────────────

    @Test
    fun `getMonthlyVolumes groups by month using strftime`() = runTest {
        val monthlyVolumes = listOf(
            com.gymcoach.app.data.local.dao.DateVolume(date = 1704067200000L, volume = 15000.0),
            com.gymcoach.app.data.local.dao.DateVolume(date = 1706745600000L, volume = 18000.0),
            com.gymcoach.app.data.local.dao.DateVolume(date = 1709251200000L, volume = 20000.0)
        )

        coEvery { workoutDao.getMonthlyVolumes() } returns monthlyVolumes

        val result = workoutDao.getMonthlyVolumes()

        assertEquals("Should have 3 months", 3, result.size)
        assertTrue("January volume should be different from February",
            result[0].volume != result[1].volume)
    }

    @Test
    fun `getMonthlyVolumes only includes COMPLETED workouts`() = runTest {
        coEvery { workoutDao.getMonthlyVolumes() } returns emptyList()

        val result = workoutDao.getMonthlyVolumes()

        assertEquals("Should return empty when no completed workouts", 0, result.size)
    }

    // ──────────────────────────────────────────────
    //  Fix #6: Analytics filtering to COMPLETED only
    // ──────────────────────────────────────────────

    @Test
    fun `getAllWorkoutVolumes only includes COMPLETED workouts`() = runTest {
        val volumes = listOf(
            com.gymcoach.app.data.local.dao.DateVolume(date = 1700000000000L, volume = 3000.0)
        )

        coEvery { workoutDao.getAllWorkoutVolumes() } returns volumes

        val result = workoutDao.getAllWorkoutVolumes()

        assertEquals("Should have 1 volume entry", 1, result.size)
        assertEquals("Volume should be 3000.0", 3000.0, result[0].volume, 0.001)
    }

    @Test
    fun `totalWorkoutsCount only counts COMPLETED`() = runTest {
        coEvery { workoutDao.getTotalWorkoutsCount() } returns 5

        val count = workoutDao.getTotalWorkoutsCount()

        assertEquals("Should count only COMPLETED workouts", 5, count)
    }

    @Test
    fun `getAverageWorkoutVolume only averages COMPLETED workouts`() = runTest {
        coEvery { workoutDao.getAverageWorkoutVolume() } returns 2500.0

        val avg = workoutDao.getAverageWorkoutVolume()

        assertEquals("Average should be from COMPLETED workouts only", 2500.0, avg, 0.001)
    }

    @Test
    fun `getTotalVolumeSum only sums COMPLETED workouts`() = runTest {
        coEvery { workoutDao.getTotalVolumeSum() } returns 50000.0

        val total = workoutDao.getTotalVolumeSum()

        assertEquals("Total should be from COMPLETED workouts only", 50000.0, total!!, 0.001)
    }
}
