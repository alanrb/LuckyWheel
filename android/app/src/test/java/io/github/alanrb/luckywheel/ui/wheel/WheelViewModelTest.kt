package io.github.alanrb.luckywheel.ui.wheel

import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.alanrb.luckywheel.data.AppDatabase
import io.github.alanrb.luckywheel.data.FakeClock
import io.github.alanrb.luckywheel.data.PrizeRepository
import io.github.alanrb.luckywheel.data.SettingsRepository
import io.github.alanrb.luckywheel.data.TestDataStore
import io.github.alanrb.luckywheel.data.seedPrizes
import io.github.alanrb.luckywheel.domain.ScriptedRandomSource
import io.github.alanrb.luckywheel.domain.sliceIndexUnderPointer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WheelViewModelTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var db: AppDatabase
    private lateinit var prizes: PrizeRepository
    private lateinit var ds: TestDataStore
    private lateinit var settings: SettingsRepository
    private val clock = FakeClock()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prizes = PrizeRepository(db)
        ds = TestDataStore(tmp.newFile("s.preferences_pb").also { it.delete() })
        settings = SettingsRepository(ds.store)
        runBlocking { prizes.seedIfEmpty(seedPrizes("Chúc may mắn lần sau", "Giảm 10%", "Cà phê sữa đá", "Ly giữ nhiệt")) }
    }

    @After
    fun tearDown() {
        db.close()
        ds.close()
        Dispatchers.resetMain()
    }

    private fun vm(random: ScriptedRandomSource, saved: SavedStateHandle = SavedStateHandle(), process: Long = 1L) =
        WheelViewModel(prizes, settings, random, clock, saved, processToken = process).also { v -> await { v.state.value.loaded } }

    /** Polls until [condition] holds; flows from Room and DataStore emit on their own threads. */
    private fun await(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "condition not met in time" }
            Thread.sleep(10)
        }
    }

    private fun tumblerQuantity() = runBlocking { prizes.prizes.first().single { it.name == "Ly giữ nhiệt" }.quantity }

    // Seed weights 5000/3000/1500/500: drawn 9500 picks "Ly giữ nhiệt" (index 3); 500_000 = slice centre.
    private fun tumblerDraw() = ScriptedRandomSource(9500, 500_000)

    @Test
    fun button_startsOnePlan_secondTapIgnored() {
        val random = tumblerDraw()
        val v = vm(random)
        v.spinWithButton()
        v.spinWithButton()
        v.spinWithFlick(direction = -1, strength = 1.0, fromRotation = 0.0)
        val phase = v.state.value.phase as SpinPhase.Spinning
        assertEquals(3, phase.plan.outcomeIndex)
        assertEquals(2, random.calls)
        assertEquals(5250L, phase.plan.durationMs)
    }

    @Test
    fun finish_revealsPlannedPrize_andDecrementsOnce() {
        val v = vm(tumblerDraw())
        v.spinWithButton()
        v.onSpinAnimationFinished()
        v.onSpinAnimationFinished()
        await { v.state.value.phase is SpinPhase.Revealed }
        val revealed = v.state.value.phase as SpinPhase.Revealed
        assertEquals("Ly giữ nhiệt", revealed.name)
        assertTrue(revealed.isReal)
        await { tumblerQuantity() == 9 }
        Thread.sleep(100)
        assertEquals(9, tumblerQuantity())
    }

    @Test
    fun recreatedInSameProcess_revealsSamePrize() {
        val saved = SavedStateHandle()
        val first = vm(tumblerDraw(), saved)
        first.spinWithButton()
        val plan = (first.state.value.phase as SpinPhase.Spinning).plan

        // The ViewModel is rebuilt from saved state while the process lives on.
        val restored = SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) })
        val second = vm(ScriptedRandomSource(), restored)
        val phase = second.state.value.phase as SpinPhase.Spinning
        assertEquals(plan, phase.plan)
        second.onSpinAnimationFinished()
        await { second.state.value.phase is SpinPhase.Revealed }
        assertEquals("Ly giữ nhiệt", (second.state.value.phase as SpinPhase.Revealed).name)
        await { tumblerQuantity() == 9 }
    }

    @Test
    fun outOfPrizes_spinIgnored() {
        runBlocking {
            prizes.replaceAll(prizes.prizes.first().map { if (it.isFallback) it else it.copy(quantity = 0) })
        }
        val random = tumblerDraw()
        val v = vm(random)
        await { !v.state.value.spinAllowed && v.state.value.slices.size == 1 }
        v.spinWithButton()
        assertEquals(SpinPhase.Idle, v.state.value.phase)
        assertEquals(0, random.calls)
    }

    @Test
    fun dismiss_returnsToIdle_andRestsAtPlannedAngle() {
        val v = vm(tumblerDraw())
        v.spinWithButton()
        v.onSpinAnimationFinished()
        await { v.state.value.phase is SpinPhase.Revealed }
        v.dismissResult()
        assertEquals(SpinPhase.Idle, v.state.value.phase)
        val rest = v.state.value.restRotation
        assertTrue(rest >= 0.0 && rest < 360.0)
        assertEquals(3, sliceIndexUnderPointer(rest, 4))
    }

    @Test
    fun limitedPrizeExhaustedBetweenPlanAndReveal_revealsFallback() {
        val v = vm(tumblerDraw())
        v.spinWithButton()
        runBlocking {
            prizes.replaceAll(prizes.prizes.first().map { if (it.name == "Ly giữ nhiệt") it.copy(quantity = 0) else it })
        }
        v.onSpinAnimationFinished()
        await { v.state.value.phase is SpinPhase.Revealed }
        val revealed = v.state.value.phase as SpinPhase.Revealed
        assertEquals("Chúc may mắn lần sau", revealed.name)
        assertFalse(revealed.isReal)
        assertEquals(0, tumblerQuantity())
    }

    @Test
    fun fallbackOutcome_isNotReal() {
        val v = vm(ScriptedRandomSource(0, 500_000))
        v.spinWithButton()
        v.onSpinAnimationFinished()
        await { v.state.value.phase is SpinPhase.Revealed }
        assertFalse((v.state.value.phase as SpinPhase.Revealed).isReal)
    }

    @Test
    fun processKilledMidSpin_spinDiscarded_quantityUnchanged() {
        // PRD §5.4 / §11: a spin interrupted by process death is never revealed.
        val saved = SavedStateHandle()
        vm(tumblerDraw(), saved, process = 1L).spinWithButton()
        val restored = SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) })
        val afterRestart = vm(ScriptedRandomSource(), restored, process = 2L)
        assertEquals(SpinPhase.Idle, afterRestart.state.value.phase)
        afterRestart.onSpinAnimationFinished()
        Thread.sleep(200)
        assertEquals(SpinPhase.Idle, afterRestart.state.value.phase)
        assertEquals(10, tumblerQuantity())
        assertFalse(restored.contains("spin.prizeId"))
    }
}
