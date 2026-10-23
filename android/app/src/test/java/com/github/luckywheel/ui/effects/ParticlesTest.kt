package com.github.luckywheel.ui.effects

import com.github.luckywheel.ui.themes.DecorationKind
import com.github.luckywheel.ui.themes.WinEffectKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ParticlesTest {

    private val w = 2560f
    private val h = 1600f
    private val unit = 2f

    @Test
    fun decorations_atMostAbout60Particles() {
        DecorationKind.entries.forEach { kind ->
            val system = DecorationSystem(kind, w, h, unit, Random(1))
            assertTrue("$kind has ${system.particles.size}", system.particles.size in 1..60)
        }
    }

    @Test
    fun decorations_stayAroundTheScreenOverTime() {
        DecorationKind.entries.forEach { kind ->
            val system = DecorationSystem(kind, w, h, unit, Random(2))
            repeat(60 * 120) { system.update(1f / 60) }
            system.particles.forEach { p ->
                assertTrue("$kind x=${p.x}", p.x in -100f * unit..w + 100f * unit)
                assertTrue("$kind y=${p.y}", p.y in -100f * unit..h + 100f * unit)
            }
        }
    }

    @Test
    fun winEffects_lastBetween2And4Seconds() {
        WinEffectKind.entries.forEach { kind ->
            val fx = WinEffectSystem.win(kind, w, h, unit, palette = listOf(0xFFFF0000.toInt()), random = Random(3))
            repeat(120) { fx.update(1f / 60) }
            assertFalse("$kind ended before 2 s", fx.isFinished)
            repeat(120) { fx.update(1f / 60) }
            assertTrue("$kind still running after 4 s", fx.isFinished)
        }
    }

    @Test
    fun loseEffect_softerAndShorter() {
        val fx = WinEffectSystem.lose(w, h, unit, glow = 0xFFFFD54F.toInt(), random = Random(4))
        assertTrue(fx.particles.size <= 30)
        repeat((2.5f * 60).toInt()) { fx.update(1f / 60) }
        assertTrue(fx.isFinished)
    }
}
