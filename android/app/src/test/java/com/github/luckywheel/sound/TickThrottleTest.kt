package com.github.luckywheel.sound

import org.junit.Assert.assertEquals
import org.junit.Test

class TickThrottleTest {

    @Test
    fun ticksCloserThan28ms_dropped() {
        val throttle = TickThrottle(minIntervalMs = 28)
        val played = listOf(0L, 10L, 27L, 28L, 40L, 56L, 100L).filter(throttle::tryAcquire)
        assertEquals(listOf(0L, 28L, 56L, 100L), played)
    }
}
