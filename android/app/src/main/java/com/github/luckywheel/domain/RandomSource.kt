package com.github.luckywheel.domain

import java.security.SecureRandom

/** Source of uniform random integers, injectable so outcome logic can be tested. */
interface RandomSource {
    /** Uniform value in `0 until bound`. */
    fun nextInt(bound: Int): Int
}

class SecureRandomSource(private val random: SecureRandom = SecureRandom()) : RandomSource {
    override fun nextInt(bound: Int): Int = random.nextInt(bound)
}
