package com.gymcoach.app.presentation.history

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.model.Workout
import com.gymcoach.app.domain.model.WorkoutExercise
import com.gymcoach.app.domain.model.WorkoutExerciseWithSets
import com.gymcoach.app.domain.model.WorkoutSet
import com.gymcoach.app.domain.model.WorkoutWithDetails
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant

class WorkoutHistoryShareTest {

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager

    @Before
    fun setUp() {
        mockkStatic(Intent::class)
        mockkStatic(android.widget.Toast::class)

        context = mockk(relaxed = true)
        packageManager = mockk(relaxed = true)
        every { context.packageManager } returns packageManager

        val mockToast = mockk<android.widget.Toast>(relaxed = true)
        every { android.widget.Toast.makeText(any(), any<CharSequence>(), any()) } returns mockToast
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createDummyWorkout(): WorkoutWithDetails {
        val now = Instant.now()
        val workout = Workout(
            id = 1L,
            date = now,
            startTime = now,
            endTime = now.plusSeconds(3600),
            duration = 3600L,
            notes = "Great workout!",
            completed = true
        )
        val exercise = Exercise(
            id = 1L,
            name = "Bench Press",
            description = "Chest press",
            muscleGroup = "Chest",
            equipment = "Barbell",
            difficulty = "Intermediate"
        )
        val workoutExercise = WorkoutExercise(id = 1L, workoutId = 1L, exerciseId = 1L, orderIndex = 0)
        val sets = listOf(
            WorkoutSet(id = 1L, workoutExerciseId = 1L, setNumber = 1, weight = 100.0, reps = 10, rpe = 8.0, restSeconds = 90, completed = true)
        )
        val exerciseWithSets = WorkoutExerciseWithSets(workoutExercise, exercise, sets)
        return WorkoutWithDetails(workout, listOf(exerciseWithSets))
    }

    @Test
    fun `shareWorkoutSummary launches startActivity when intent resolves`() {
        val dummyWorkout = createDummyWorkout()
        val mockSendIntent = mockk<Intent>(relaxed = true)
        val mockChooserIntent = mockk<Intent>(relaxed = true)
        val dummyComponent = mockk<ComponentName>()
        var launched = false

        every { mockSendIntent.resolveActivity(packageManager) } returns dummyComponent
        every { Intent.createChooser(mockSendIntent, "Share Workout") } returns mockChooserIntent
        every { mockChooserIntent.resolveActivity(packageManager) } returns dummyComponent

        shareWorkoutSummary(
            context = context,
            workout = dummyWorkout,
            intentFactory = { mockSendIntent },
            intentLauncher = { launched = true }
        )

        assert(launched)
    }

    @Test
    fun `shareWorkoutSummary displays toast when intent cannot resolve`() {
        val dummyWorkout = createDummyWorkout()
        val mockSendIntent = mockk<Intent>(relaxed = true)
        val mockChooserIntent = mockk<Intent>(relaxed = true)
        var launched = false

        every { mockSendIntent.resolveActivity(packageManager) } returns null
        every { Intent.createChooser(mockSendIntent, "Share Workout") } returns mockChooserIntent
        every { mockChooserIntent.resolveActivity(packageManager) } returns null

        shareWorkoutSummary(
            context = context,
            workout = dummyWorkout,
            intentFactory = { mockSendIntent },
            intentLauncher = { launched = true }
        )

        assert(!launched)
        verify { android.widget.Toast.makeText(context, "No app available to handle share action", any()) }
    }

    @Test
    fun `shareWorkoutSummary handles exception gracefully when startActivity fails`() {
        val dummyWorkout = createDummyWorkout()
        val mockSendIntent = mockk<Intent>(relaxed = true)
        val mockChooserIntent = mockk<Intent>(relaxed = true)
        val dummyComponent = mockk<ComponentName>()

        every { mockSendIntent.resolveActivity(packageManager) } returns dummyComponent
        every { Intent.createChooser(mockSendIntent, "Share Workout") } returns mockChooserIntent
        every { mockChooserIntent.resolveActivity(packageManager) } returns dummyComponent

        shareWorkoutSummary(
            context = context,
            workout = dummyWorkout,
            intentFactory = { mockSendIntent },
            intentLauncher = { throw SecurityException("Permission denied") }
        )

        verify { android.widget.Toast.makeText(context, "Unable to share workout summary", any()) }
    }
}
