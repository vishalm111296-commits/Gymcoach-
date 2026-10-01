package com.gymcoach.app.core.timer

import com.gymcoach.app.domain.model.SetType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class RestPresetsTest(
    private val setType: SetType,
    private val rpe: Double,
    private val expectedRestTime: Int,
    private val description: String
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{3} - SetType: {0}, RPE: {1} -> Expected Rest: {2}s")
        fun data(): Collection<Array<Any>> {
            return listOf(
                // Warmup
                arrayOf(SetType.WARMUP, 5.0, 60, "Warmup set always returns 60s regardless of RPE"),
                arrayOf(SetType.WARMUP, 10.0, 60, "Warmup set always returns 60s regardless of RPE"),

                // Drop
                arrayOf(SetType.DROP, 8.0, 30, "Drop set always returns 30s regardless of RPE"),
                arrayOf(SetType.DROP, 10.0, 30, "Drop set always returns 30s regardless of RPE"),

                // Failure
                arrayOf(SetType.FAILURE, 9.0, 120, "Failure set always returns 120s regardless of RPE"),
                arrayOf(SetType.FAILURE, 10.0, 120, "Failure set always returns 120s regardless of RPE"),

                // Normal
                arrayOf(SetType.NORMAL, 9.5, 180, "Normal set with RPE >= 9.0 returns 180s"),
                arrayOf(SetType.NORMAL, 9.0, 180, "Normal set with RPE >= 9.0 returns 180s"),
                arrayOf(SetType.NORMAL, 8.5, 120, "Normal set with RPE >= 7.5 returns 120s"),
                arrayOf(SetType.NORMAL, 7.5, 120, "Normal set with RPE >= 7.5 returns 120s"),
                arrayOf(SetType.NORMAL, 6.5, 90, "Normal set with RPE >= 6.0 returns 90s"),
                arrayOf(SetType.NORMAL, 6.0, 90, "Normal set with RPE >= 6.0 returns 90s"),
                arrayOf(SetType.NORMAL, 5.5, 60, "Normal set with RPE < 6.0 returns 60s"),
                arrayOf(SetType.NORMAL, 0.0, 60, "Normal set with RPE < 6.0 returns 60s")
            )
        }
    }

    @Test
    fun testRecommendedRestTime() {
        val actualRestTime = RestPresets.recommended(setType, rpe)
        assertEquals("Failed for: $description", expectedRestTime, actualRestTime)
    }
}
