package io.github.alanrb.luckywheel.data

import io.github.alanrb.luckywheel.domain.PinHasher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PinManagerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var ds: TestDataStore
    private val clock = FakeClock()
    private val hasher = PinHasher(iterations = 1_000)
    private lateinit var pins: PinManager

    @Before
    fun setUp() {
        ds = TestDataStore(tmp.newFile("pin.preferences_pb").also { it.delete() })
        pins = PinManager(ds.store, hasher, clock)
    }

    @After
    fun tearDown() = ds.close()

    @Test
    fun noPin_initially() = runTest {
        assertFalse(pins.hasPin.first())
    }

    @Test
    fun create_thenCheckOk() = runTest {
        pins.createPin("2468")
        assertTrue(pins.hasPin.first())
        assertEquals(PinCheck.Ok, pins.check("2468"))
    }

    @Test
    fun wrong_reportsAttemptsLeft() = runTest {
        pins.createPin("2468")
        assertEquals(PinCheck.Wrong(4), pins.check("0000"))
        assertEquals(PinCheck.Wrong(3), pins.check("0000"))
        assertEquals(PinCheck.Wrong(2), pins.check("0000"))
        assertEquals(PinCheck.Wrong(1), pins.check("0000"))
    }

    @Test
    fun correctPin_resetsAttempts() = runTest {
        pins.createPin("2468")
        pins.check("0000")
        pins.check("0000")
        assertEquals(PinCheck.Ok, pins.check("2468"))
        assertEquals(PinCheck.Wrong(4), pins.check("0000"))
    }

    @Test
    fun fifthWrong_locks_andCheckReturnsLockedEvenIfCorrect() = runTest {
        pins.createPin("2468")
        repeat(4) { pins.check("0000") }
        assertEquals(PinCheck.Locked(30), pins.check("0000"))
        clock.advance(10_000)
        assertEquals(PinCheck.Locked(20), pins.check("2468"))
        assertEquals(20, pins.lockedSeconds())
    }

    @Test
    fun lock_persistsAcrossNewInstance() = runTest {
        pins.createPin("2468")
        repeat(5) { pins.check("0000") }
        val restarted = PinManager(ds.restart(), hasher, clock)
        assertEquals(PinCheck.Locked(30), restarted.check("2468"))
    }

    @Test
    fun lockExpires_after30s() = runTest {
        pins.createPin("2468")
        repeat(5) { pins.check("0000") }
        clock.advance(30_000)
        assertEquals(0, pins.lockedSeconds())
        assertEquals(PinCheck.Ok, pins.check("2468"))
    }

    @Test
    fun pinStoredHashedNotPlain() = runTest {
        pins.createPin("2468")
        val raw = ds.store.data.first().asMap().values.joinToString("|")
        assertFalse(raw.contains("2468"))
    }

    @Test
    fun changePin_requiresMatchAndReplaces() = runTest {
        pins.createPin("2468")
        assertTrue(pins.matchesCurrent("2468"))
        assertFalse(pins.matchesCurrent("1111"))
        pins.changePin("1357")
        assertEquals(PinCheck.Ok, pins.check("1357"))
        assertEquals(PinCheck.Wrong(4), pins.check("2468"))
    }
}
