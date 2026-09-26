package com.gymcoach.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignSystemTest {

    @Test
    fun testColorTokensAreDefined() {
        // Assert all Stitch colors are defined and not unspecified
        val colors = listOf(
            GymCoachColors.SurfaceDeep,
            GymCoachColors.SurfaceCard,
            GymCoachColors.SurfaceElevated,
            GymCoachColors.CyanAccent,
            GymCoachColors.AmberAccent,
            GymCoachColors.Emerald,
            GymCoachColors.BorderSubtle,
            GymCoachColors.TextPrimary,
            GymCoachColors.TextSecondary,
            GymCoachColors.TextDisabled,
            GymCoachColors.ErrorRed,
            GymCoachColors.WarningAmber
        )

        for (color in colors) {
            assertTrue("Color must not be unspecified", color != Color.Unspecified)
            // Color has 4 components: alpha, red, green, blue.
            // When color != Color.Unspecified it is valid.
        }
    }

    @Test
    fun testSpacingTokensArePositive() {
        val spacing = CustomGymCoachSpacing()

        assertTrue("xs should be positive", spacing.xs > 0.dp)
        assertTrue("sm should be positive", spacing.sm > 0.dp)
        assertTrue("md should be positive", spacing.md > 0.dp)
        assertTrue("lg should be positive", spacing.lg > 0.dp)
        assertTrue("xl should be positive", spacing.xl > 0.dp)
        assertTrue("xxl should be positive", spacing.xxl > 0.dp)
    }
}
