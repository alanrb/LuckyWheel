package io.github.alanrb.luckywheel.data

import io.github.alanrb.luckywheel.domain.Clock

object SystemClock : Clock {
    override fun nowMs(): Long = System.currentTimeMillis()
    override fun elapsedMs(): Long = android.os.SystemClock.elapsedRealtime()
}
