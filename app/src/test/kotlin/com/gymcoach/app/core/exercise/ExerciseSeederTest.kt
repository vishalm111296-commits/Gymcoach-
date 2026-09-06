package com.gymcoach.app.core.exercise

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.gymcoach.app.data.local.dao.ExerciseSubstitutionDao
import com.gymcoach.app.data.local.database.GymCoachDatabase
import com.gymcoach.app.data.local.entity.ExerciseSubstitutionEntity
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext

class ExerciseSeederTest {

    private lateinit var db: GymCoachDatabase
    private lateinit var substitutionDao: ExerciseSubstitutionDao
    private lateinit var context: Context
    private lateinit var assets: AssetManager
    private lateinit var seeder: ExerciseSeeder

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0

        db = mockk(relaxed = true)
        substitutionDao = mockk(relaxed = true)
        context = mockk(relaxed = true)
        assets = mockk(relaxed = true)

        every { db.exerciseSubstitutionDao() } returns substitutionDao
        every { context.assets } returns assets

        seeder = ExerciseSeeder(db, context)
    }

    @Test
    fun `seedSubstitutions uses batch insertAll`() = runTest {
        val arrayJson = mockk<JSONArray>()
        val item1 = mockk<JSONObject>()
        val item2 = mockk<JSONObject>()

        mockkConstructor(JSONObject::class)
        every { anyConstructed<JSONObject>().keys() } returns listOf("barbell_bench_press").iterator()
        every { anyConstructed<JSONObject>().getJSONArray("barbell_bench_press") } returns arrayJson

        every { arrayJson.length() } returns 2
        every { arrayJson.getJSONObject(0) } returns item1
        every { arrayJson.getJSONObject(1) } returns item2

        every { item1.getString("substitute_id") } returns "dumbbell_bench_press"
        every { item1.optString("reason") } returns "Equipment variation"

        every { item2.getString("substitute_id") } returns "push_up"
        every { item2.optString("reason") } returns "Bodyweight option"

        every { assets.open("exercises/exercise_substitutions.json") } returns ByteArrayInputStream("{}".toByteArray())

        val exerciseIds = mapOf(
            "barbell_bench_press" to 101L,
            "dumbbell_bench_press" to 102L,
            "push_up" to 103L
        )

        val method = ExerciseSeeder::class.java.declaredMethods.first { it.name == "seedSubstitutions" }
        method.isAccessible = true

        val dummyContinuation = Continuation<Unit>(EmptyCoroutineContext) {}
        method.invoke(seeder, exerciseIds, dummyContinuation)

        val slot = slot<List<ExerciseSubstitutionEntity>>()
        coVerify(exactly = 1) { substitutionDao.insertAll(capture(slot)) }

        val captured = slot.captured
        assertEquals(2, captured.size)
        assertEquals(101L, captured[0].originalExerciseId)
        assertEquals(102L, captured[0].substituteExerciseId)
        assertEquals("Equipment variation", captured[0].reason)

        assertEquals(101L, captured[1].originalExerciseId)
        assertEquals(103L, captured[1].substituteExerciseId)
        assertEquals("Bodyweight option", captured[1].reason)
    }
}
