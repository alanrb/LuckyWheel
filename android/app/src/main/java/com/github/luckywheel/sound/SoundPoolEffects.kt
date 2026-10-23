package com.github.luckywheel.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import com.github.luckywheel.R
import com.github.luckywheel.domain.ThemeId

/** Drops ticks that come faster than the ear can separate them (prototype: 28 ms). */
class TickThrottle(private val minIntervalMs: Long = 28) {
    private var last = Long.MIN_VALUE

    fun tryAcquire(nowMs: Long): Boolean {
        if (last != Long.MIN_VALUE && nowMs - last < minIntervalMs) return false
        last = nowMs
        return true
    }
}

/** Bundled, generated effects (tools/generate_sounds.py) played through the media volume (SD-01). */
class SoundPoolEffects(context: Context) : SoundEffects {
    private val pool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val ticks = ThemeId.entries.associateWith { pool.load(context, tickRes(it), 1) }
    private val wins = ThemeId.entries.associateWith { pool.load(context, winRes(it), 1) }
    private val lose = pool.load(context, R.raw.lose, 1)
    private val throttle = TickThrottle()

    override fun tick(theme: ThemeId) {
        if (throttle.tryAcquire(SystemClock.uptimeMillis())) play(ticks.getValue(theme), priority = 1)
    }

    override fun win(theme: ThemeId) = play(wins.getValue(theme), priority = 2)

    override fun lose() = play(lose, priority = 2)

    private fun play(id: Int, priority: Int) {
        pool.play(id, 1f, 1f, priority, 0, 1f)
    }

    private fun tickRes(theme: ThemeId) = when (theme) {
        ThemeId.DEFAULT -> R.raw.tick_default
        ThemeId.NOEL -> R.raw.tick_noel
        ThemeId.TET -> R.raw.tick_tet
        ThemeId.TRUNG_THU -> R.raw.tick_trung_thu
        ThemeId.LOVE -> R.raw.tick_love
        ThemeId.HALLOWEEN -> R.raw.tick_halloween
    }

    private fun winRes(theme: ThemeId) = when (theme) {
        ThemeId.DEFAULT -> R.raw.win_default
        ThemeId.NOEL -> R.raw.win_noel
        ThemeId.TET -> R.raw.win_tet
        ThemeId.TRUNG_THU -> R.raw.win_trung_thu
        ThemeId.LOVE -> R.raw.win_love
        ThemeId.HALLOWEEN -> R.raw.win_halloween
    }
}
