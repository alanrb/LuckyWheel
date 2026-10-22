package io.github.alanrb.luckywheel.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class PinSecurityTest {

    private val hasher = PinHasher(iterations = 1_000)

    @Test
    fun verify_correctAndWrong() {
        val salt = hasher.newSalt()
        val hash = hasher.hash("1234", salt)
        assertTrue(hasher.verify("1234", salt, hash))
        assertFalse(hasher.verify("1235", salt, hash))
        assertFalse(hash.contains("1234"))
    }

    @Test
    fun hash_differsPerSalt() {
        val a = hasher.newSalt()
        val b = hasher.newSalt()
        assertNotEquals(a, b)
        assertNotEquals(hasher.hash("0000", a), hasher.hash("0000", b))
    }

    @Test
    fun fifthFailure_locks30s() {
        var lock = Lockout(0, 0)
        repeat(4) { lock = lock.afterFailure(nowMs = 1_000) }
        assertEquals(Lockout(4, 0), lock)
        assertEquals(0, lock.remainingSeconds(1_000))
        lock = lock.afterFailure(nowMs = 1_000)
        assertEquals(Lockout(0, 31_000), lock)
    }

    @Test
    fun remainingSeconds_ceil() {
        val lock = Lockout(0, 31_000)
        assertEquals(30, lock.remainingSeconds(1_000))
        assertEquals(1, lock.remainingSeconds(30_999))
        assertEquals(2, lock.remainingSeconds(29_500))
        assertEquals(0, lock.remainingSeconds(31_000))
    }

    @Test
    fun hash_worksInNonLatinDigitLocale() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            val salt = hasher.newSalt()
            assertTrue(salt.all { it in '0'..'9' || it in 'a'..'f' })
            assertTrue(hasher.verify("4321", salt, hasher.hash("4321", salt)))
        } finally {
            Locale.setDefault(saved)
        }
    }
}
