package com.gymcoach.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignSystemTest {

    @Test
    fun testAllColorTokensAreDefined() {
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
            GymCoachColors.WarningAmber,
            GymCoachColors.PureDark,
            GymCoachColors.SurfaceCardElevated,
            GymCoachColors.SurfaceInput,
            GymCoachColors.BorderLight,
            GymCoachColors.Primary,
            GymCoachColors.PrimaryLight,
            GymCoachColors.PrimaryText,
            GymCoachColors.PrimaryDark,
            GymCoachColors.PrimaryGlow,
            GymCoachColors.GoldAccent,
            GymCoachColors.TextMuted,
            GymCoachColors.Success,
            GymCoachColors.SuccessBg,
            GymCoachColors.Warning,
            GymCoachColors.WarningBg,
            GymCoachColors.Danger,
            GymCoachColors.DangerBg,
            GymCoachColors.PhaseSetup,
            GymCoachColors.PhaseEccentric,
            GymCoachColors.PhaseBottom,
            GymCoachColors.PhaseConcentric,
            GymCoachColors.PhaseEnd
        )

        for (color in colors) {
            assertTrue("Color must not be unspecified", color != Color.Unspecified)
            assertTrue("Color alpha must be positive", color.alpha > 0f)
        }
    }

    @Test
    fun testGradientsAreDefined() {
        assertNotNull("PrimaryGradient should not be null", GymCoachColors.PrimaryGradient)
        assertNotNull("SurfaceCardGradient should not be null", GymCoachColors.SurfaceCardGradient)
    }

    @Test
    fun testGymCoachSpacingScale() {
        assertTrue("xxs should be > 0.dp", GymCoachSpacing.xxs > 0.dp)
        assertTrue("xs should be > xxs", GymCoachSpacing.xs > GymCoachSpacing.xxs)
        assertTrue("sm should be > xs", GymCoachSpacing.sm > GymCoachSpacing.xs)
        assertTrue("md should be > sm", GymCoachSpacing.md > GymCoachSpacing.sm)
        assertTrue("lg should be > md", GymCoachSpacing.lg > GymCoachSpacing.md)
        assertTrue("xl should be > lg", GymCoachSpacing.xl > GymCoachSpacing.lg)
        assertTrue("xxl should be > xl", GymCoachSpacing.xxl > GymCoachSpacing.xl)
        assertTrue("xxxl should be > xxl", GymCoachSpacing.xxxl > GymCoachSpacing.xxl)
        assertTrue("huge should be > xxxl", GymCoachSpacing.huge > GymCoachSpacing.xxxl)
        assertTrue("colossal should be > huge", GymCoachSpacing.colossal > GymCoachSpacing.huge)

        assertEquals(2.dp, GymCoachSpacing.xxs)
        assertEquals(4.dp, GymCoachSpacing.xs)
        assertEquals(8.dp, GymCoachSpacing.sm)
        assertEquals(12.dp, GymCoachSpacing.md)
        assertEquals(16.dp, GymCoachSpacing.lg)
        assertEquals(20.dp, GymCoachSpacing.xl)
        assertEquals(24.dp, GymCoachSpacing.xxl)
        assertEquals(32.dp, GymCoachSpacing.xxxl)
        assertEquals(40.dp, GymCoachSpacing.huge)
        assertEquals(48.dp, GymCoachSpacing.colossal)
    }

    @Test
    fun testCustomSpacingTokensArePositive() {
        val spacing = CustomGymCoachSpacing()
        assertTrue("xs should be positive", spacing.xs > 0.dp)
        assertTrue("sm should be positive", spacing.sm > 0.dp)
        assertTrue("md should be positive", spacing.md > 0.dp)
        assertTrue("lg should be positive", spacing.lg > 0.dp)
        assertTrue("xl should be positive", spacing.xl > 0.dp)
        assertTrue("xxl should be positive", spacing.xxl > 0.dp)
    }

    @Test
    fun testGymCoachShapesAreDefined() {
        assertNotNull(GymCoachShapes.xs)
        assertNotNull(GymCoachShapes.sm)
        assertNotNull(GymCoachShapes.md)
        assertNotNull(GymCoachShapes.lg)
        assertNotNull(GymCoachShapes.xl)
        assertNotNull(GymCoachShapes.Card)
        assertNotNull(GymCoachShapes.container)
        assertNotNull(GymCoachShapes.pill)
        assertNotNull(GymCoachShapes.input)
        assertNotNull(GymCoachShapes.chip)
    }

    @Test
    fun testGymCoachBordersAreDefined() {
        assertEquals(1.dp, GymCoachBorders.subtle.width)
        assertEquals(GymCoachColors.BorderSubtle, GymCoachBorders.subtle.brush.let {
            (it as androidx.compose.ui.graphics.SolidColor).value
        })
        assertEquals(1.dp, GymCoachBorders.light.width)
        assertEquals(1.5.dp, GymCoachBorders.primary.width)
        assertEquals(1.dp, GymCoachBorders.success.width)
        assertEquals(GymCoachBorders.subtle, GymCoachBorders.subtleBorder())
    }

    @Test
    fun testGymCoachMotionDurations() {
        assertTrue(GymCoachMotion.durationFast < GymCoachMotion.durationMedium)
        assertTrue(GymCoachMotion.durationMedium < GymCoachMotion.durationSlow)
        assertEquals(150, GymCoachMotion.durationFast)
        assertEquals(250, GymCoachMotion.durationMedium)
        assertEquals(400, GymCoachMotion.durationSlow)
    }
}
