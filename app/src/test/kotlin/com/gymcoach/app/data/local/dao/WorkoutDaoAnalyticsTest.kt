package com.gymcoach.app.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymcoach.app.data.local.database.GymCoachDatabase
import com.gymcoach.app.data.local.entity.ExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutEntity
import com.gymcoach.app.data.local.entity.WorkoutExerciseEntity
import com.gymcoach.app.data.local.entity.WorkoutSetEntity
import com.gymcoach.app.test.ArchAwareRobolectricTestRunner
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

/**
 * Real-SQL tests for the ANALYTICS D1 (monthly volume bucketing) and D2
 * (completed-workout/completed-set/NORMAL-set filters) defects.
 *
 * Runs against a real in-memory Room/SQLite database (no mocks) so the SQL
 * itself is executed and pinned. D1 regression: the buggy base grouped by
 * `strftime('%Y-%m', w.date)` on millis, which yields NULL for every row and
 * collapses all months into a single bucket. D2 regression: missing
 * w.completed/ws.completed/setType filters let "would-win" sets leak into
 * latest/best-volume/personal-record results.
 */
@RunWith(ArchAwareRobolectricTestRunner::class)
class WorkoutDaoAnalyticsTest {

    private lateinit var db: GymCoachDatabase
    private lateinit var dao: WorkoutDao
    private lateinit var exerciseDao: ExerciseDao

    private val exerciseId = 1L

    private companion object {
        const val NORMAL = 0
        const val WARMUP = 1
        const val DROP = 2
        const val FAILURE = 3
    }

    private class FixtureSet(
        val weight: Double,
        val reps: Int,
        val setType: Int = NORMAL,
        val completed: Boolean = true
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.workoutDao()
        exerciseDao = db.exerciseDao()
        runTest {
            exerciseDao.insertAllExercises(
                listOf(
                    ExerciseEntity(id = exerciseId, name = "TestPress", description = "", muscleGroup = "", equipment = "", difficulty = ""),
                    ExerciseEntity(id = 2L, name = "TestSquat", description = "", muscleGroup = "", equipment = "", difficulty = "")
                )
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** Builds a timestamp in the HOST timezone (Calendar.getInstance()), never a hardcoded epoch literal. */
    private fun date(y: Int, month: Int, d: Int, hour: Int, min: Int): Long =
        Calendar.getInstance().apply {
            set(y, month, d, hour, min, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private suspend fun insertWorkoutWithSets(
        dateMillis: Long,
        workoutCompleted: Boolean,
        sets: List<FixtureSet>,
        exerciseId: Long = this.exerciseId
    ): Long {
        val workoutId = dao.insertWorkout(
            WorkoutEntity(
                date = dateMillis,
                startTime = dateMillis,
                endTime = dateMillis,
                duration = 3600L,
                notes = "fixture",
                completed = workoutCompleted
            )
        )
        val workoutExerciseId = dao.insertWorkoutExercise(
            WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exerciseId, orderIndex = 0)
        )
        sets.forEachIndexed { index, s ->
            dao.insertWorkoutSet(
                WorkoutSetEntity(
                    workoutExerciseId = workoutExerciseId,
                    setNumber = index + 1,
                    weight = s.weight,
                    reps = s.reps,
                    rpe = 7.0,
                    restSeconds = 60,
                    completed = s.completed,
                    setType = s.setType
                )
            )
        }
        return workoutId
    }

    private fun assertMonth(dateMillis: Long, year: Int, month: Int) {
        val c = Calendar.getInstance().apply { timeInMillis = dateMillis }
        assertEquals(year, c.get(Calendar.YEAR))
        assertEquals(month, c.get(Calendar.MONTH))
    }

    // ---- D1: getMonthlyVolumes ----

    @Test
    fun `getMonthlyVolumes groups completed workouts into distinct real months with correct sums`() = runTest {
        // Mid-month dates avoid the UTC-vs-local month-boundary window entirely.
        insertWorkoutWithSets(date(2026, Calendar.MAY, 15, 12, 0), true, listOf(FixtureSet(60.0, 10))) // May: 600
        insertWorkoutWithSets(date(2026, Calendar.JUNE, 10, 12, 0), true, listOf(FixtureSet(100.0, 5))) // Jun: 500
        insertWorkoutWithSets(date(2026, Calendar.JUNE, 20, 12, 0), true, listOf(FixtureSet(30.0, 5))) // Jun: 150
        // Incomplete workout must NOT contribute (w.completed = 1); without the filter it would
        // add 999*10 = 9990 to the May bucket.
        insertWorkoutWithSets(date(2026, Calendar.MAY, 20, 12, 0), false, listOf(FixtureSet(999.0, 10)))

        val volumes = dao.getMonthlyVolumes()

        // The D1 bug collapsed every month into ONE bucket; there must be exactly two.
        assertEquals(2, volumes.size)
        assertEquals(600.0, volumes[0].volume, 0.001)
        assertEquals(650.0, volumes[1].volume, 0.001)
        assertMonth(volumes[0].date, 2026, Calendar.MAY)
        assertMonth(volumes[1].date, 2026, Calendar.JUNE)
        assertTrue(volumes[0].date < volumes[1].date)
    }

    // ---- D2: set filters on analytics queries ----

    @Test
    fun `getLatestSetForExercise only returns the qualifying normal completed set`() = runTest {
        val qualifying = date(2026, Calendar.MAY, 1, 10, 0)
        insertWorkoutWithSets(qualifying, true, listOf(FixtureSet(80.0, 8)))
        // Would win by recency (later date) if w.completed=1 were absent:
        insertWorkoutWithSets(date(2026, Calendar.MAY, 2, 10, 0), false, listOf(FixtureSet(100.0, 8)))
        // Would win by recency if ws.completed=1 were absent:
        insertWorkoutWithSets(date(2026, Calendar.MAY, 3, 10, 0), true, listOf(FixtureSet(120.0, 8, completed = false)))
        // Would win by recency if setType=0 were absent:
        insertWorkoutWithSets(date(2026, Calendar.MAY, 4, 10, 0), true, listOf(FixtureSet(140.0, 8, setType = WARMUP)))
        insertWorkoutWithSets(date(2026, Calendar.MAY, 5, 10, 0), true, listOf(FixtureSet(160.0, 8, setType = DROP)))
        insertWorkoutWithSets(date(2026, Calendar.MAY, 6, 10, 0), true, listOf(FixtureSet(180.0, 8, setType = FAILURE)))

        val latest = dao.getLatestSetForExercise(exerciseId)

        assertNotNull(latest)
        assertEquals(80.0, latest!!.weight, 0.001)
    }

    @Test
    fun `getBestVolumeSetForExercise picks the qualifying set over higher-volume filtered sets`() = runTest {
        insertWorkoutWithSets(date(2026, Calendar.MAY, 1, 10, 0), true, listOf(FixtureSet(80.0, 10))) // volume 800
        // Each of these has a higher weight*reps and would win if its filter were absent:
        insertWorkoutWithSets(date(2026, Calendar.MAY, 2, 10, 0), false, listOf(FixtureSet(90.0, 10))) // 900, incomplete workout
        insertWorkoutWithSets(date(2026, Calendar.MAY, 3, 10, 0), true, listOf(FixtureSet(100.0, 10, completed = false))) // 1000, uncompleted set
        insertWorkoutWithSets(date(2026, Calendar.MAY, 4, 10, 0), true, listOf(FixtureSet(110.0, 10, setType = WARMUP))) // 1100
        insertWorkoutWithSets(date(2026, Calendar.MAY, 5, 10, 0), true, listOf(FixtureSet(120.0, 10, setType = DROP))) // 1200
        insertWorkoutWithSets(date(2026, Calendar.MAY, 6, 10, 0), true, listOf(FixtureSet(130.0, 10, setType = FAILURE))) // 1300

        val best = dao.getBestVolumeSetForExercise(exerciseId)

        assertNotNull(best)
        assertEquals(80.0, best!!.weight, 0.001)
    }

    @Test
    fun `getAllPersonalRecords only counts qualifying sets and groups per exercise`() = runTest {
        insertWorkoutWithSets(date(2026, Calendar.MAY, 1, 10, 0), true, listOf(FixtureSet(80.0, 10)), exerciseId = 1L)
        // Higher weights that would win if any filter were absent:
        insertWorkoutWithSets(date(2026, Calendar.MAY, 2, 10, 0), false, listOf(FixtureSet(90.0, 10)), exerciseId = 1L)
        insertWorkoutWithSets(date(2026, Calendar.MAY, 3, 10, 0), true, listOf(FixtureSet(100.0, 10, completed = false)), exerciseId = 1L)
        insertWorkoutWithSets(date(2026, Calendar.MAY, 4, 10, 0), true, listOf(FixtureSet(110.0, 10, setType = WARMUP)), exerciseId = 1L)
        insertWorkoutWithSets(date(2026, Calendar.MAY, 5, 10, 0), true, listOf(FixtureSet(120.0, 10, setType = DROP)), exerciseId = 1L)
        // A second exercise with a higher qualifying max weight, and one with NO qualifying sets at all.
        insertWorkoutWithSets(date(2026, Calendar.MAY, 1, 11, 0), true, listOf(FixtureSet(200.0, 5)), exerciseId = 2L)
        insertWorkoutWithSets(date(2026, Calendar.MAY, 6, 10, 0), false, listOf(FixtureSet(500.0, 5)), exerciseId = 2L)

        val records = dao.getAllPersonalRecords()

        assertEquals(2, records.size)
        assertEquals("TestSquat", records[0].name) // ORDER BY maxWeight DESC
        assertEquals(200.0, records[0].maxWeight, 0.001)
        assertEquals("TestPress", records[1].name)
        assertEquals(80.0, records[1].maxWeight, 0.001)
    }
}
