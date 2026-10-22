package io.github.alanrb.luckywheel.ui.wheel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WheelLayoutTest {

    @Test
    fun tabletLandscape_stackedLikeDesign() {
        val noLogo = wheelLayout(1280f, 800f, hasLogo = false)
        val logo = wheelLayout(1280f, 800f, hasLogo = true)
        assertFalse(noLogo.split)
        assertFalse(logo.split)
        assertEquals(1f, noLogo.unit, 0.001f)
        // Design: 590 px without a logo, 480 px with one.
        assertTrue(noLogo.wheelDp in 570f..620f)
        assertTrue(logo.wheelDp in 460f..500f)
    }

    @Test
    fun tabletPortrait_stackedWheelUsesWidth() {
        val layout = wheelLayout(800f, 1280f, hasLogo = true)
        assertFalse(layout.split)
        assertTrue(layout.wheelDp <= 800f - 48f)
        assertTrue(layout.wheelDp > 650f)
    }

    @Test
    fun phoneLandscape_buttonBesideWheel() {
        val layout = wheelLayout(915f, 412f, hasLogo = true)
        assertTrue(layout.split)
        assertTrue(layout.wheelDp <= 412f)
    }

    @Test
    fun buttonNeverBelowTouchTarget() {
        val layout = wheelLayout(412f, 915f, hasLogo = false)
        assertTrue(layout.buttonHeightDp >= 56f)
    }

    @Test
    fun everythingFitsVertically_whenStacked() {
        for ((w, h) in listOf(1280f to 800f, 800f to 1280f, 412f to 915f, 1340f to 800f, 1024f to 768f)) {
            for (logo in listOf(false, true)) {
                val l = wheelLayout(w, h, logo)
                if (!l.split) {
                    val used = l.paddingTopDp + l.paddingBottomDp + (if (logo) l.logoHeightDp + l.gapDp else 0f) +
                        l.wheelDp + l.gapDp + l.buttonHeightDp
                    assertTrue("$w x $h logo=$logo uses $used", used <= h)
                }
            }
        }
    }
}
