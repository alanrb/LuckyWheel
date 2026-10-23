package com.github.luckywheel.domain

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.math.ceil

object PinPolicy {
    const val LENGTH = 4
    const val MAX_ATTEMPTS = 5
    const val LOCKOUT_MS = 30_000L
}

/** Salted PBKDF2 hashing so the PIN is never stored in plain text (ST-03). */
class PinHasher(
    private val iterations: Int = 60_000,
    private val random: SecureRandom = SecureRandom(),
) {
    fun newSalt(): String = ByteArray(16).also(random::nextBytes).toHex()

    fun hash(pin: String, salt: String): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt.hexToBytes(), iterations, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded.toHex()
    }

    fun verify(pin: String, salt: String, hash: String): Boolean =
        MessageDigest.isEqual(hash(pin, salt).toByteArray(), hash.toByteArray())

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
    private fun String.hexToBytes() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

/** Wrong-PIN counter and lock (ST-04). */
data class Lockout(val failedAttempts: Int, val lockedUntilMs: Long)

fun Lockout.afterFailure(nowMs: Long): Lockout {
    val failures = failedAttempts + 1
    return if (failures >= PinPolicy.MAX_ATTEMPTS) {
        Lockout(0, nowMs + PinPolicy.LOCKOUT_MS)
    } else {
        copy(failedAttempts = failures)
    }
}

/** Whole seconds left on the lock, rounded up; 0 when not locked. */
fun Lockout.remainingSeconds(nowMs: Long): Int {
    val left = lockedUntilMs - nowMs
    return if (left <= 0) 0 else ceil(left / 1000.0).toInt().coerceAtLeast(1)
}
