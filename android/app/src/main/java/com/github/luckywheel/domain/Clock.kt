package com.github.luckywheel.domain

/** Time source, injectable for tests. */
interface Clock {
    /** Wall-clock time, used for the PIN lock that must survive restarts. */
    fun nowMs(): Long

    /** Monotonic time since boot, used to time spins across rotation and backgrounding. */
    fun elapsedMs(): Long
}
