package com.gymcoach.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymcoach.app.data.local.dao.WorkoutDao
import com.gymcoach.app.data.local.database.GymCoachDatabase
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutEntity
import com.gymcoach.app.data.local.entity.WorkoutExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import com.gymcoach.app.test.ArchAwareRobolectricTestRunner
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.Date

/**
 * Real-SQL tests for the ANALYTICS D3 (today boundary), D4 (Monday week
 * boundary, no Sunday forward-roll) and D5 (1st-of-month boundary) defects,
 * plus the weekly-summary bucket logic.
 *
 * Constructs [AnalyticsRepositoryImpl] over a real in-memory Room/SQLite
 * database so the count queries and the Calendar boundary helpers are exercised
 * together against real data. All dates are built in the HOST timezone with
 * `Calendar.getInstance()`; the repository clones the passed-in `now`, so
 * boundaries are computed in the same timezone as the fixtures.
 */
@RunWith(ArchAwareRobolectricTestRunner::class)
class AnalyticsRepositoryImplRoomTest {

    private lateinit var db: GymCoachDatabase
    private lateinit var workoutDao: WorkoutDao
    private lateinit var repo: AnalyticsRepositoryImpl

    private val exerciseId = 1L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        workoutDao = db.workoutDao()
        repo = AnalyticsRepositoryImpl(workoutDao)
        runTest {
            db.exerciseDao().insertAllExercises(
                listOf(ExerciseEntity(id = exerciseId, name = "TestPress", description = "", muscleGroup = "", equipment = "", difficulty = ""))
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** Builds a Calendar in the HOST timezone at local midnight-zeroed millis. */
    private fun calendar(y: Int, month: Int, d: Int, hour: Int, min: Int): Calendar =
        Calendar.getInstance().apply {
            set(y, month, d, hour, min, 0)
            set(Calendar.MILLISECOND, 0)
        }

    private fun date(y: Int, month: Int, d: Int, hour: Int, min: Int): Long =
        calendar(y, month, d, hour, min).timeInMillis

    /** Inserts a single completed workout (one NORMAL, completed set) at the given date. */
    private suspend fun insertCompletedWorkout(dateMillis: Long, weight: Double, reps: Int) {
        val workoutId = workoutDao.insertWorkout(
            WorkoutEntity(
                date = dateMillis,
                startTime = dateMillis,
                endTime = dateMillis,
                duration = 3600L,
                notes = "fixture",
                completed = true
            )
        )
        val workoutExerciseId = workoutDao.insertWorkoutExercise(
            WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exerciseId, orderIndex = 0)
        )
        workoutDao.insertWorkoutSet(
            WorkoutSetEntity(
                workoutExerciseId = workoutExerciseId,
                setNumber = 1,
                weight = weight,
                reps = reps,
                rpe = 7.0,
                restSeconds = 60,
                completed = true,
                setType = 0
            )
        )
    }

    // ---- D3: today boundary ----

    @Test
    fun `getWorkoutCounts counts a workout today but not yesterday`() = runTest {
        val now = calendar(2026, Calendar.JUNE, 15, 12, 0) // Monday noon
        insertCompletedWorkout(date(2026, Calendar.JUNE, 15, 9, 0), 60.0, 10) // today, before now
        insertCompletedWorkout(date(2026, Calendar.JUNE, 14, 20, 0), 70.0, 10) // yesterday

        val counts = repo.getWorkoutCounts(now)

        assertEquals(2, counts.total)
        assertEquals(1, counts.today) // only the today workout passes date >= todayStart
    }

    // ---- D4: week boundary (Monday at local midnight) ----

    @Test
    fun `getWorkoutCounts week includes this Monday but excludes previous Sunday`() = runTest {
        val now = calendar(2026, Calendar.JUNE, 17, 12, 0) // Wednesday
        assertEquals(Calendar.WEDNESDAY, now.get(Calendar.DAY_OF_WEEK)) // fixture sanity check
        insertCompletedWorkout(date(2026, Calendar.JUNE, 15, 8, 0), 60.0, 10) // this week's Monday
        insertCompletedWorkout(date(2026, Calendar.JUNE, 14, 20, 0), 70.0, 10) // previous week's Sunday

        val counts = repo.getWorkoutCounts(now)

        assertEquals(2, counts.total)
        assertEquals(1, counts.week) // Monday only; the Sunday sits before the Monday-midnight boundary
    }

    @Test
    fun `getWorkoutCounts on a Sunday does not roll the week forward`() = runTest {
        val now = calendar(2026, Calendar.JUNE, 21, 12, 0) // Sunday
        assertEquals(Calendar.SUNDAY, now.get(Calendar.DAY_OF_WEEK)) // fixture sanity check
        insertCompletedWorkout(date(2026, Calendar.JUNE, 20, 18, 0), 60.0, 10) // yesterday (Saturday, same week)
        insertCompletedWorkout(date(2026, Calendar.JUNE, 13, 18, 0), 70.0, 10) // 8 days before (previous week)

        val counts = repo.getWorkoutCounts(now)

        // Buggy `set(DAY_OF_WEEK, MONDAY)` on a Sunday rolls FORWARD to next Monday and returns 0.
        assertEquals(1, counts.week)
        assertEquals(0, counts.today) // Saturday is before Sunday midnight
    }

    // ---- D5: month boundary (1st at local midnight) ----

    @Test
    fun `getWorkoutCounts month includes the 1st but excludes the previous month`() = runTest {
        val now = calendar(2026, Calendar.JUNE, 10, 12, 0) // Wednesday June 10
        insertCompletedWorkout(date(2026, Calendar.JUNE, 1, 0, 30), 60.0, 10) // 1st at 00:30 local
        insertCompletedWorkout(date(2026, Calendar.MAY, 31, 23, 0), 70.0, 10) // end of previous month

        val counts = repo.getWorkoutCounts(now)

        assertEquals(2, counts.total)
        assertEquals(1, counts.month) // June 1st qualifies, May 31st does not
        assertEquals(0, counts.today) // neither workout is on June 10
    }

    // ---- Weekly summary (same Monday-midnight bucketing) ----

    @Test
    fun `getWeeklySummary groups each week into a single Monday-midnight bucket`() = runTest {
        // Prior week: Monday June 8.
        insertCompletedWorkout(date(2026, Calendar.JUNE, 8, 12, 0), 30.0, 10) // 300
        // Current week (Mon Jun 15 .. Sun Jun 21).
        insertCompletedWorkout(date(2026, Calendar.JUNE, 15, 12, 0), 10.0, 10) // 100
        insertCompletedWorkout(date(2026, Calendar.JUNE, 16, 12, 0), 15.0, 10) // 150
        insertCompletedWorkout(date(2026, Calendar.JUNE, 21, 12, 0), 20.0, 10) // 200, Sunday -> same week bucket

        val weekly = repo.getWeeklySummary()

        assertEquals(2, weekly.size)
        assertEquals(300.0, weekly[0].second, 0.001) // prior week alone
        assertEquals(450.0, weekly[1].second, 0.001) // Mon + Tue + Sun of current week
        assertMondayMidnight(weekly[0].first)
        assertMondayMidnight(weekly[1].first)
    }

    private fun assertMondayMidnight(value: Date) {
        val c = Calendar.getInstance().apply { time = value }
        assertEquals(Calendar.MONDAY, c.get(Calendar.DAY_OF_WEEK))
        assertEquals(0, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, c.get(Calendar.MINUTE))
    }
}
