package io.github.alanrb.luckywheel.ui.pin

import io.github.alanrb.luckywheel.data.FakeClock
import io.github.alanrb.luckywheel.data.PinManager
import io.github.alanrb.luckywheel.data.TestDataStore
import io.github.alanrb.luckywheel.domain.PinHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class PinViewModelsTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var ds: TestDataStore
    private val clock = FakeClock()
    private lateinit var pins: PinManager

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        ds = TestDataStore(tmp.newFile("p.preferences_pb").also { it.delete() })
        pins = PinManager(ds.store, PinHasher(iterations = 1_000), clock)
    }

    @After
    fun tearDown() {
        ds.close()
        Dispatchers.resetMain()
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "condition not met in time" }
            Thread.sleep(10)
        }
    }

    private fun CreatePinViewModel.type(pin: String) = pin.forEach { press(PinKey.Digit(it.digitToInt())) }
    private fun PinEntryViewModel.type(pin: String) = pin.forEach { press(PinKey.Digit(it.digitToInt())) }

    @Test
    fun createPin_twoMatchingEntries_storesPin() {
        val vm = CreatePinViewModel(pins)
        vm.type("2580")
        await { vm.state.value.step == 1 }
        vm.type("2580")
        await { vm.state.value.done }
        assertTrue(runBlocking { pins.hasPin.first() })
    }

    @Test
    fun createPin_mismatch_restarts() {
        val vm = CreatePinViewModel(pins)
        vm.type("2580")
        await { vm.state.value.step == 1 }
        vm.type("2581")
        await { vm.state.value.mismatch }
        assertEquals(0, vm.state.value.step)
        assertEquals("", vm.state.value.input)
        assertFalse(runBlocking { pins.hasPin.first() })
    }

    @Test
    fun delete_removesLastDigit() {
        val vm = CreatePinViewModel(pins)
        vm.press(PinKey.Digit(1))
        vm.press(PinKey.Digit(2))
        vm.press(PinKey.Delete)
        assertEquals("1", vm.state.value.input)
    }

    @Test
    fun entry_wrongThenRight() {
        runBlocking { pins.createPin("2580") }
        val vm = PinEntryViewModel(pins)
        vm.type("0000")
        await { vm.state.value.attemptsLeft == 4 }
        assertFalse(vm.state.value.unlocked)
        vm.type("2580")
        await { vm.state.value.unlocked }
    }

    @Test
    fun entry_fiveWrong_locksKeypad() {
        runBlocking { pins.createPin("2580") }
        val vm = PinEntryViewModel(pins)
        repeat(5) {
            vm.type("0000")
            await { !vm.state.value.busy }
        }
        await { vm.state.value.lockedSeconds == 30 }
        vm.type("2580")
        Thread.sleep(300)
        assertFalse(vm.state.value.unlocked)
        assertEquals("", vm.state.value.input)
    }
}
