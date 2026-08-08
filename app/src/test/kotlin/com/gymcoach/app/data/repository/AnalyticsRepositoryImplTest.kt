package com.gymcoach.app.data.repository

import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.dao.DateVolume
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class AnalyticsRepositoryImplTest {

    private val dao = mockk<WorkoutDao>()

    private fun millis(y: Int, m: Int, d: Int): Long {
        val c = Calendar.getInstance().apply {
            set(y, m - 1, d, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    @Test
    fun `getVolumeHistory maps DAO rows to date-volume pairs`() = runTest {
        val t1 = millis(2026, 8, 1)
        val t2 = millis(2026, 8, 3)
        coEvery { dao.getAllWorkoutVolumes() } returns listOf(
            DateVolume(date = t1, volume = 100.0),
            DateVolume(date = t2, volume = 250.5)
        )

        val repo = AnalyticsRepositoryImpl(dao)
        val history = repo.getVolumeHistory()

        assertEquals(2, history.size)
        assertEquals(100.0, history[0].second, 0.001)
        assertEquals(250.5, history[1].second, 0.001)
        assertEquals(t1, history[0].first.time)
    }

    @Test
    fun `getWeeklySummary groups all dates into their Monday week`() = runTest {
        // 2026-08-05 is a Wednesday, 2026-08-08 is a Saturday: same week (Monday 2026-08-03)
        val wed = millis(2026, 8, 5)
        val sat = millis(2026, 8, 8)
        // 2026-08-10 is the following Monday
        val nextMon = millis(2026, 8, 10)
        coEvery { dao.getAllWorkoutVolumes() } returns listOf(
            DateVolume(date = wed, volume = 100.0),
            DateVolume(date = sat, volume = 50.0),
            DateVolume(date = nextMon, volume = 200.0)
        )

        val repo = AnalyticsRepositoryImpl(dao)
        val weekly = repo.getWeeklySummary()

        assertEquals(2, weekly.size)
        assertEquals(150.0, weekly[0].second, 0.001) // Wed + Sat bucket
        assertEquals(200.0, weekly[1].second, 0.001) // next Monday bucket
    }
}
