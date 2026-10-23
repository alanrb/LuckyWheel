package com.github.luckywheel.data

import com.github.luckywheel.domain.Clock

object SystemClock : Clock {
    override fun nowMs(): Long = System.currentTimeMillis()
    override fun elapsedMs(): Long = android.os.SystemClock.elapsedRealtime()
}
